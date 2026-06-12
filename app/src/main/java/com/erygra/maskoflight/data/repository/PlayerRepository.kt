package com.erygra.maskoflight.data.repository

import com.erygra.maskoflight.data.dao.PlayerDao
import com.erygra.maskoflight.data.entities.PlayerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PlayerRepository - مستودع بيانات اللاعب
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * طبقة المستودع لإدارة بيانات اللاعب بين قاعدة البيانات المحلية والسحابة
 * 
 * Repository layer for managing player data between local database and cloud
 * 
 * المسؤوليات / Responsibilities:
 * - إدارة عمليات القراءة والكتابة (CRUD operations)
 * - التعامل مع الأخطاء (Error handling)
 * - التخزين المؤقت (Caching)
 * - المزامنة السحابية (Cloud sync)
 * - تحويل البيانات (Data transformation)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Singleton
class PlayerRepository @Inject constructor(
    private val playerDao: PlayerDao
) {

    // ═══════════════════════════════════════════════════════════════════════
    // Cache - التخزين المؤقت
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * كاش اللاعب الحالي في الذاكرة
     * Current player cache in memory
     */
    private var cachedPlayer: PlayerEntity? = null

    /**
     * وقت آخر تحديث للكاش
     * Last cache update time
     */
    private var lastCacheUpdate: Long = 0L

    /**
     * مدة صلاحية الكاش (5 دقائق)
     * Cache validity duration (5 minutes)
     */
    private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes

    // ═══════════════════════════════════════════════════════════════════════
    // Create Operations - عمليات الإنشاء
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إنشاء لاعب جديد
     * Create new player
     * 
     * @param player بيانات اللاعب / Player data
     * @return Result<Long> معرف الصف المُدرج أو خطأ / Inserted row ID or error
     */
    suspend fun createPlayer(player: PlayerEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // التحقق من عدم وجود لاعب بنفس المعرف
            // Check player doesn't exist
            if (playerDao.playerExists(player.playerId)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Player with ID ${player.playerId} already exists")
                )
            }

            // إدراج اللاعب
            // Insert player
            val rowId = playerDao.insertPlayer(player)

            // تحديث الكاش
            // Update cache
            cachedPlayer = player
            lastCacheUpdate = System.currentTimeMillis()

            Result.success(rowId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * إنشاء أو تحديث لاعب
     * Create or update player
     * 
     * @param player بيانات اللاعب / Player data
     * @return Result<Long> معرف الصف أو خطأ / Row ID or error
     */
    suspend fun createOrUpdatePlayer(player: PlayerEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val rowId = playerDao.insertOrReplacePlayer(player)
            
            // تحديث الكاش
            // Update cache
            cachedPlayer = player
            lastCacheUpdate = System.currentTimeMillis()

            Result.success(rowId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Read Operations - عمليات القراءة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على لاعب بمعرفه (مع المراقبة)
     * Get player by ID (with observation)
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Flow<PlayerEntity?> تدفق بيانات اللاعب / Flow of player data
     */
    fun getPlayerFlow(playerId: String): Flow<PlayerEntity?> {
        return playerDao.getPlayerFlow(playerId)
            .catch { e ->
                // تسجيل الخطأ
                // Log error
                e.printStackTrace()
                emit(null)
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * الحصول على لاعب بمعرفه (مع التخزين المؤقت)
     * Get player by ID (with caching)
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param forceRefresh إجبار التحديث / Force refresh
     * @return Result<PlayerEntity?> بيانات اللاعب أو خطأ / Player data or error
     */
    suspend fun getPlayer(
        playerId: String,
        forceRefresh: Boolean = false
    ): Result<PlayerEntity?> = withContext(Dispatchers.IO) {
        try {
            // التحقق من الكاش
            // Check cache
            if (!forceRefresh && isCacheValid() && cachedPlayer?.playerId == playerId) {
                return@withContext Result.success(cachedPlayer)
            }

            // جلب من قاعدة البيانات
            // Fetch from database
            val player = playerDao.getPlayer(playerId)

            // تحديث الكاش
            // Update cache
            if (player != null) {
                cachedPlayer = player
                lastCacheUpdate = System.currentTimeMillis()
            }

            Result.success(player)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على جميع اللاعبين
     * Get all players
     * 
     * @return Result<List<PlayerEntity>> قائمة اللاعبين أو خطأ / Players list or error
     */
    suspend fun getAllPlayers(): Result<List<PlayerEntity>> = withContext(Dispatchers.IO) {
        try {
            val players = playerDao.getAllPlayers()
            Result.success(players)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على جميع اللاعبين (مع المراقبة)
     * Get all players (with observation)
     * 
     * @return Flow<List<PlayerEntity>> تدفق قائمة اللاعبين / Flow of players list
     */
    fun getAllPlayersFlow(): Flow<List<PlayerEntity>> {
        return playerDao.getAllPlayersFlow()
            .catch { e ->
                e.printStackTrace()
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * البحث عن لاعبين بالاسم
     * Search players by name
     * 
     * @param searchQuery نص البحث / Search query
     * @return Result<List<PlayerEntity>> نتائج البحث أو خطأ / Search results or error
     */
    suspend fun searchPlayersByName(searchQuery: String): Result<List<PlayerEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val players = playerDao.searchPlayersByName(searchQuery)
                Result.success(players)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * التحقق من وجود لاعب
     * Check if player exists
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<Boolean> true إذا كان موجود / true if exists
     */
    suspend fun playerExists(playerId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = playerDao.playerExists(playerId)
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations - عمليات التحديث
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث بيانات لاعب
     * Update player data
     * 
     * @param player بيانات اللاعب المحدثة / Updated player data
     * @return Result<Int> عدد الصفوف المحدثة أو خطأ / Updated rows count or error
     */
    suspend fun updatePlayer(player: PlayerEntity): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = playerDao.updatePlayer(player)

            // تحديث الكاش
            // Update cache
            if (updatedRows > 0 && cachedPlayer?.playerId == player.playerId) {
                cachedPlayer = player
                lastCacheUpdate = System.currentTimeMillis()
            }

            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث المستوى والخبرة
     * Update level and XP
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param level المستوى الجديد / New level
     * @param currentXP الخبرة الحالية / Current XP
     * @param xpToNextLevel الخبرة للمستوى التالي / XP to next level
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateLevelAndXP(
        playerId: String,
        level: Int,
        currentXP: Int,
        xpToNextLevel: Int
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = playerDao.updateLevelAndXP(playerId, level, currentXP, xpToNextLevel)
            
            // إبطال الكاش
            // Invalidate cache
            invalidateCache()

            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث الصحة والطاقة
     * Update health and energy
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param health الصحة الحالية / Current health
     * @param energy الطاقة الحالية / Current energy
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateHealthAndEnergy(
        playerId: String,
        health: Float,
        energy: Float
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = playerDao.updateHealthAndEnergy(playerId, health, energy)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث الموقع
     * Update position
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param positionX موقع X / X position
     * @param positionY موقع Y / Y position
     * @param currentRegion المنطقة الحالية / Current region
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updatePosition(
        playerId: String,
        positionX: Float,
        positionY: Float,
        currentRegion: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = playerDao.updatePosition(playerId, positionX, positionY, currentRegion)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث العملات
     * Update currencies
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param gold الذهب / Gold
     * @param gems الجواهر / Gems
     * @param lightFragments شظايا النور / Light fragments
     * @param darkEssence جوهر الظلام / Dark essence
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateCurrencies(
        playerId: String,
        gold: Int,
        gems: Int,
        lightFragments: Int,
        darkEssence: Int
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = playerDao.updateCurrencies(
                playerId, gold, gems, lightFragments, darkEssence
            )
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث الإحصائيات
     * Update statistics
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param totalPlayTime إجمالي وقت اللعب / Total play time
     * @param deathCount عدد الوفيات / Death count
     * @param totalDamageDealt إجمالي الضرر المُلحق / Total damage dealt
     * @param totalDamageTaken إجمالي الضرر المُستقبل / Total damage taken
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateStatistics(
        playerId: String,
        totalPlayTime: Long,
        deathCount: Int,
        totalDamageDealt: Long,
        totalDamageTaken: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = playerDao.updateStatistics(
                playerId, totalPlayTime, deathCount, totalDamageDealt, totalDamageTaken
            )
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث وقت آخر حفظ
     * Update last save time
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateLastSaveTime(playerId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = playerDao.updateLastSaveTime(playerId)
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث حالة المزامنة السحابية
     * Update cloud sync status
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param syncStatus حالة المزامنة / Sync status
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateCloudSyncStatus(
        playerId: String,
        syncStatus: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = playerDao.updateCloudSyncStatus(playerId, syncStatus)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Delete Operations - عمليات الحذف
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حذف لاعب
     * Delete player
     * 
     * @param player بيانات اللاعب / Player data
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deletePlayer(player: PlayerEntity): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = playerDao.deletePlayer(player)

            // مسح الكاش
            // Clear cache
            if (cachedPlayer?.playerId == player.playerId) {
                cachedPlayer = null
            }

            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف لاعب بمعرفه
     * Delete player by ID
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deletePlayerById(playerId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = playerDao.deletePlayerById(playerId)

            // مسح الكاش
            // Clear cache
            if (cachedPlayer?.playerId == playerId) {
                cachedPlayer = null
            }

            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف جميع اللاعبين
     * Delete all players
     * 
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteAllPlayers(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = playerDao.deleteAllPlayers()
            
            // مسح الكاش
            // Clear cache
            cachedPlayer = null

            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Complex Operations - العمليات المعقدة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حفظ كامل للاعب
     * Full player save
     * 
     * @param player بيانات اللاعب / Player data
     * @return Result<Unit> نجاح أو خطأ / Success or error
     */
    suspend fun savePlayerComplete(player: PlayerEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            playerDao.savePlayerComplete(player)
            
            // تحديث الكاش
            // Update cache
            cachedPlayer = player
            lastCacheUpdate = System.currentTimeMillis()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * نسخ لاعب
     * Clone player
     * 
     * @param sourcePlayerId معرف اللاعب المصدر / Source player ID
     * @param newPlayerId معرف اللاعب الجديد / New player ID
     * @param newPlayerName اسم اللاعب الجديد / New player name
     * @return Result<Unit> نجاح أو خطأ / Success or error
     */
    suspend fun clonePlayer(
        sourcePlayerId: String,
        newPlayerId: String,
        newPlayerName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            playerDao.clonePlayer(sourcePlayerId, newPlayerId, newPlayerName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Analytics Operations - عمليات التحليلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على متوسط مستوى اللاعبين
     * Get average player level
     * 
     * @return Result<Float> متوسط المستوى / Average level
     */
    suspend fun getAveragePlayerLevel(): Result<Float> = withContext(Dispatchers.IO) {
        try {
            val average = playerDao.getAveragePlayerLevel()
            Result.success(average)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على إجمالي وقت اللعب
     * Get total play time
     * 
     * @return Result<Long> إجمالي الوقت / Total time
     */
    suspend fun getTotalPlayTime(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val total = playerDao.getTotalPlayTime()
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على عدد اللاعبين
     * Get player count
     * 
     * @return Result<Int> عدد اللاعبين / Player count
     */
    suspend fun getPlayerCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = playerDao.getPlayerCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على عدد اللاعبين (مع المراقبة)
     * Get player count (with observation)
     * 
     * @return Flow<Int> تدفق عدد اللاعبين / Flow of player count
     */
    fun getPlayerCountFlow(): Flow<Int> {
        return playerDao.getPlayerCountFlow()
            .catch { e ->
                e.printStackTrace()
                emit(0)
            }
            .flowOn(Dispatchers.IO)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cache Management - إدارة الكاش
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * التحقق من صلاحية الكاش
     * Check cache validity
     * 
     * @return true إذا كان الكاش صالح / true if cache is valid
     */
    private fun isCacheValid(): Boolean {
        return cachedPlayer != null && 
               (System.currentTimeMillis() - lastCacheUpdate) < cacheValidityDuration
    }

    /**
     * إبطال الكاش
     * Invalidate cache
     */
    private fun invalidateCache() {
        cachedPlayer = null
        lastCacheUpdate = 0L
    }

    /**
     * مسح الكاش يدوياً
     * Clear cache manually
     */
    fun clearCache() {
        invalidateCache()
    }

    /**
     * تحديث الكاش يدوياً
     * Update cache manually
     * 
     * @param player بيانات اللاعب / Player data
     */
    fun updateCache(player: PlayerEntity) {
        cachedPlayer = player
        lastCacheUpdate = System.currentTimeMillis()
    }
}