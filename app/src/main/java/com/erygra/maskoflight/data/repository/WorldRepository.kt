package com.erygra.maskoflight.data.repository

import com.erygra.maskoflight.data.dao.WorldStateDao
import com.erygra.maskoflight.data.entities.WorldProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * WorldRepository - مستودع بيانات العالم
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * طبقة المستودع لإدارة حالة العالم وتقدم اللاعب في المناطق
 * 
 * Repository layer for managing world state and player region progress
 * 
 * المسؤوليات / Responsibilities:
 * - إدارة تقدم العالم (World progress management)
 * - تتبع اكتشاف المناطق (Region discovery tracking)
 * - إدارة الأحداث العالمية (World events management)
 * - حفظ الحالات (State persistence)
 * - التخزين المؤقت (Caching)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Singleton
class WorldRepository @Inject constructor(
    private val worldStateDao: WorldStateDao
) {

    // ═══════════════════════════════════════════════════════════════════════
    // Cache - التخزين المؤقت
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * كاش حالة العالم الحالية
     * Current world state cache
     */
    private var cachedWorldProgress: WorldProgressEntity? = null

    /**
     * وقت آخر تحديث للكاش
     * Last cache update time
     */
    private var lastCacheUpdate: Long = 0L

    /**
     * مدة صلاحية الكاش (2 دقيقة)
     * Cache validity duration (2 minutes)
     */
    private val cacheValidityDuration = 2 * 60 * 1000L

    // ═══════════════════════════════════════════════════════════════════════
    // Create Operations - عمليات الإنشاء
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إنشاء حالة عالم جديدة
     * Create new world state
     * 
     * @param worldProgress بيانات تقدم العالم / World progress data
     * @return Result<Long> معرف الصف المُدرج / Inserted row ID
     */
    suspend fun createWorldProgress(worldProgress: WorldProgressEntity): Result<Long> = 
        withContext(Dispatchers.IO) {
            try {
                val rowId = worldStateDao.insertWorldProgress(worldProgress)
                
                // تحديث الكاش
                // Update cache
                cachedWorldProgress = worldProgress
                lastCacheUpdate = System.currentTimeMillis()

                Result.success(rowId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * إنشاء أو تحديث حالة عالم
     * Create or update world state
     * 
     * @param worldProgress بيانات تقدم العالم / World progress data
     * @return Result<Long> معرف الصف / Row ID
     */
    suspend fun createOrUpdateWorldProgress(worldProgress: WorldProgressEntity): Result<Long> = 
        withContext(Dispatchers.IO) {
            try {
                val rowId = worldStateDao.insertOrReplaceWorldProgress(worldProgress)
                
                // تحديث الكاش
                // Update cache
                cachedWorldProgress = worldProgress
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
     * الحصول على تقدم العالم (مع المراقبة)
     * Get world progress (with observation)
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Flow<WorldProgressEntity?> تدفق بيانات التقدم / Flow of progress data
     */
    fun getWorldProgressFlow(playerId: String): Flow<WorldProgressEntity?> {
        return worldStateDao.getWorldProgressFlow(playerId)
            .catch { e ->
                e.printStackTrace()
                emit(null)
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * الحصول على تقدم العالم (مع التخزين المؤقت)
     * Get world progress (with caching)
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param forceRefresh إجبار التحديث / Force refresh
     * @return Result<WorldProgressEntity?> بيانات التقدم / Progress data
     */
    suspend fun getWorldProgress(
        playerId: String,
        forceRefresh: Boolean = false
    ): Result<WorldProgressEntity?> = withContext(Dispatchers.IO) {
        try {
            // التحقق من الكاش
            // Check cache
            if (!forceRefresh && isCacheValid() && cachedWorldProgress?.playerId == playerId) {
                return@withContext Result.success(cachedWorldProgress)
            }

            // جلب من قاعدة البيانات
            // Fetch from database
            val worldProgress = worldStateDao.getWorldProgress(playerId)

            // تحديث الكاش
            // Update cache
            if (worldProgress != null) {
                cachedWorldProgress = worldProgress
                lastCacheUpdate = System.currentTimeMillis()
            }

            Result.success(worldProgress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على جميع حالات العالم
     * Get all world states
     * 
     * @return Result<List<WorldProgressEntity>> قائمة حالات العالم / World states list
     */
    suspend fun getAllWorldProgress(): Result<List<WorldProgressEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val allProgress = worldStateDao.getAllWorldProgress()
                Result.success(allProgress)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على المنطقة الحالية
     * Get current region
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<String?> المنطقة الحالية / Current region
     */
    suspend fun getCurrentRegion(playerId: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val region = worldStateDao.getCurrentRegion(playerId)
            Result.success(region)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على المناطق المفتوحة
     * Get unlocked regions
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<List<String>?> قائمة المناطق / Regions list
     */
    suspend fun getUnlockedRegions(playerId: String): Result<List<String>?> = 
        withContext(Dispatchers.IO) {
            try {
                val regions = worldStateDao.getUnlockedRegions(playerId)
                Result.success(regions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على الزعماء المهزومين
     * Get defeated bosses
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<List<String>?> قائمة الزعماء / Bosses list
     */
    suspend fun getDefeatedBosses(playerId: String): Result<List<String>?> = 
        withContext(Dispatchers.IO) {
            try {
                val bosses = worldStateDao.getDefeatedBosses(playerId)
                Result.success(bosses)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على نسبة الاكتشاف
     * Get discovery percentage
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<Int?> نسبة الاكتشاف / Discovery percentage
     */
    suspend fun getDiscoveryPercentage(playerId: String): Result<Int?> = 
        withContext(Dispatchers.IO) {
            try {
                val percentage = worldStateDao.getDiscoveryPercentage(playerId)
                Result.success(percentage)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * التحقق من وجود حالة عالم
     * Check if world state exists
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<Boolean> true إذا كانت موجودة / true if exists
     */
    suspend fun worldProgressExists(playerId: String): Result<Boolean> = 
        withContext(Dispatchers.IO) {
            try {
                val exists = worldStateDao.worldProgressExists(playerId)
                Result.success(exists)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations - عمليات التحديث
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث حالة العالم
     * Update world state
     * 
     * @param worldProgress بيانات التقدم المحدثة / Updated progress data
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateWorldProgress(worldProgress: WorldProgressEntity): Result<Int> = 
        withContext(Dispatchers.IO) {
            try {
                val updatedRows = worldStateDao.updateWorldProgress(worldProgress)
                
                // تحديث الكاش
                // Update cache
                if (updatedRows > 0 && cachedWorldProgress?.playerId == worldProgress.playerId) {
                    cachedWorldProgress = worldProgress
                    lastCacheUpdate = System.currentTimeMillis()
                }

                Result.success(updatedRows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * تحديث المنطقة الحالية
     * Update current region
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param currentRegion المنطقة الحالية / Current region
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateCurrentRegion(playerId: String, currentRegion: String): Result<Int> = 
        withContext(Dispatchers.IO) {
            try {
                val updatedRows = worldStateDao.updateCurrentRegion(playerId, currentRegion)
                invalidateCache()
                Result.success(updatedRows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * تحديث الموقع الأخير
     * Update last position
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param lastPosition الموقع الأخير / Last position
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateLastPosition(
        playerId: String,
        lastPosition: Map<String, Float>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = worldStateDao.updateLastPosition(playerId, lastPosition)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث نسبة الاكتشاف الإجمالية
     * Update total discovery percentage
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param percentage نسبة الاكتشاف / Discovery percentage
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateTotalDiscovery(playerId: String, percentage: Int): Result<Int> = 
        withContext(Dispatchers.IO) {
            try {
                val updatedRows = worldStateDao.updateTotalDiscovery(playerId, percentage)
                invalidateCache()
                Result.success(updatedRows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * تحديث المناطق المفتوحة
     * Update unlocked regions
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param unlockedRegions قائمة المناطق المفتوحة / Unlocked regions list
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateUnlockedRegions(
        playerId: String,
        unlockedRegions: List<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = worldStateDao.updateUnlockedRegions(playerId, unlockedRegions)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث الملاذات المكتشفة
     * Update discovered sanctuaries
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param discoveredSanctuaries قائمة الملاذات / Sanctuaries list
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateDiscoveredSanctuaries(
        playerId: String,
        discoveredSanctuaries: List<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = worldStateDao.updateDiscoveredSanctuaries(
                playerId, 
                discoveredSanctuaries
            )
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث الأعداء المهزومين
     * Update defeated enemies
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param defeatedEnemies خريطة الأعداء / Enemies map
     * @param totalEnemiesDefeated إجمالي الأعداء / Total enemies
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateDefeatedEnemies(
        playerId: String,
        defeatedEnemies: Map<String, Int>,
        totalEnemiesDefeated: Int
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = worldStateDao.updateDefeatedEnemies(
                playerId,
                defeatedEnemies,
                totalEnemiesDefeated
            )
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث الزعماء المهزومين
     * Update defeated bosses
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param defeatedBosses قائمة الزعماء / Bosses list
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateDefeatedBosses(
        playerId: String,
        defeatedBosses: List<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = worldStateDao.updateDefeatedBosses(playerId, defeatedBosses)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث الأحداث المكتملة
     * Update completed events
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param completedEvents قائمة الأحداث / Events list
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateCompletedEvents(
        playerId: String,
        completedEvents: List<String>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = worldStateDao.updateCompletedEvents(playerId, completedEvents)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث المتغيرات العالمية
     * Update world variables
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param worldVariables المتغيرات / Variables
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateWorldVariables(
        playerId: String,
        worldVariables: Map<String, Any>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = worldStateDao.updateWorldVariables(playerId, worldVariables)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث الأعلام العالمية
     * Update world flags
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param worldFlags الأعلام / Flags
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateWorldFlags(
        playerId: String,
        worldFlags: Map<String, Boolean>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = worldStateDao.updateWorldFlags(playerId, worldFlags)
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
     * حذف حالة عالم
     * Delete world state
     * 
     * @param worldProgress بيانات التقدم / Progress data
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteWorldProgress(worldProgress: WorldProgressEntity): Result<Int> = 
        withContext(Dispatchers.IO) {
            try {
                val deletedRows = worldStateDao.deleteWorldProgress(worldProgress)
                
                // مسح الكاش
                // Clear cache
                if (cachedWorldProgress?.playerId == worldProgress.playerId) {
                    cachedWorldProgress = null
                }

                Result.success(deletedRows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * حذف حالة عالم بمعرف اللاعب
     * Delete world state by player ID
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteWorldProgressByPlayerId(playerId: String): Result<Int> = 
        withContext(Dispatchers.IO) {
            try {
                val deletedRows = worldStateDao.deleteWorldProgressByPlayerId(playerId)
                
                // مسح الكاش
                // Clear cache
                if (cachedWorldProgress?.playerId == playerId) {
                    cachedWorldProgress = null
                }

                Result.success(deletedRows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // Complex Operations - العمليات المعقدة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حفظ كامل لحالة العالم
     * Full world state save
     * 
     * @param worldProgress بيانات التقدم / Progress data
     * @return Result<Unit> نجاح أو خطأ / Success or error
     */
    suspend fun saveWorldProgressComplete(worldProgress: WorldProgressEntity): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                worldStateDao.saveWorldProgressComplete(worldProgress)
                
                // تحديث الكاش
                // Update cache
                cachedWorldProgress = worldProgress
                lastCacheUpdate = System.currentTimeMillis()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // Analytics Operations - عمليات التحليلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على متوسط نسبة الاكتشاف
     * Get average discovery percentage
     * 
     * @return Result<Float> متوسط الاكتشاف / Average discovery
     */
    suspend fun getAverageDiscovery(): Result<Float> = withContext(Dispatchers.IO) {
        try {
            val average = worldStateDao.getAverageDiscovery()
            Result.success(average)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على إجمالي الأعداء المهزومين
     * Get total enemies defeated
     * 
     * @return Result<Long> إجمالي الأعداء / Total enemies
     */
    suspend fun getTotalEnemiesDefeated(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val total = worldStateDao.getTotalEnemiesDefeated()
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على إجمالي المسافة المقطوعة
     * Get total distance traveled
     * 
     * @return Result<Long> إجمالي المسافة / Total distance
     */
    suspend fun getTotalDistanceTraveled(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val total = worldStateDao.getTotalDistanceTraveled()
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على عدد حالات العالم
     * Get world states count
     * 
     * @return Result<Int> عدد حالات العالم / World states count
     */
    suspend fun getWorldProgressCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = worldStateDao.getWorldProgressCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cache Management - إدارة الكاش
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * التحقق من صلاحية الكاش
     * Check cache validity
     */
    private fun isCacheValid(): Boolean {
        return cachedWorldProgress != null &&
               (System.currentTimeMillis() - lastCacheUpdate) < cacheValidityDuration
    }

    /**
     * إبطال الكاش
     * Invalidate cache
     */
    private fun invalidateCache() {
        cachedWorldProgress = null
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
     * @param worldProgress بيانات التقدم / Progress data
     */
    fun updateCache(worldProgress: WorldProgressEntity) {
        cachedWorldProgress = worldProgress
        lastCacheUpdate = System.currentTimeMillis()
    }
}