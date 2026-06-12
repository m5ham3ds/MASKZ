package com.erygra.maskoflight.data.dao

import androidx.room.*
import com.erygra.maskoflight.data.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PlayerDao - واجهة الوصول لبيانات اللاعب
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Data Access Object لإدارة عمليات قاعدة البيانات المتعلقة باللاعب
 * 
 * Data Access Object for managing player database operations
 * 
 * العمليات المدعومة / Supported Operations:
 * - الإدراج والتحديث (Insert & Update)
 * - الاستعلام والبحث (Query & Search)
 * - الحذف (Delete)
 * - المراقبة بـ Flow (Observe with Flow)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
data class LevelAndXP(val level: Int, val currentXP: Int, val xpToNextLevel: Int)
data class HealthAndEnergy(val currentHealth: Float, val maxHealth: Float, val currentEnergy: Float, val maxEnergy: Float)
data class PlayerPositionData(val positionX: Float, val positionY: Float, val currentRegion: String)
data class PlayerCurrencies(val gold: Int, val gems: Int, val lightFragments: Int, val darkEssence: Int)
data class PlayerStatistics(val totalPlayTime: Long, val deathCount: Int, val totalDamageDealt: Long, val totalDamageTaken: Long, val distanceTraveled: Long, val jumpCount: Int, val dashCount: Int)

@Dao
interface PlayerDao {

    // ═══════════════════════════════════════════════════════════════════════
    // Insert Operations - عمليات الإدراج
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إدراج لاعب جديد
     * Insert new player
     * 
     * @param player بيانات اللاعب / Player data
     * @return معرف السطر المُدرج / Inserted row ID
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlayer(player: PlayerEntity): Long

    /**
     * إدراج أو استبدال لاعب
     * Insert or replace player
     * 
     * @param player بيانات اللاعب / Player data
     * @return معرف السطر / Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplacePlayer(player: PlayerEntity): Long

    /**
     * إدراج عدة لاعبين (للاستيراد)
     * Insert multiple players (for import)
     * 
     * @param players قائمة اللاعبين / Players list
     * @return قائمة معرفات الصفوف / Row IDs list
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayers(players: List<PlayerEntity>): List<Long>

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations - عمليات التحديث
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث بيانات لاعب
     * Update player data
     * 
     * @param player بيانات اللاعب المحدثة / Updated player data
     * @return عدد الصفوف المحدثة / Number of updated rows
     */
    @Update
    suspend fun updatePlayer(player: PlayerEntity): Int

    /**
     * تحديث عدة لاعبين
     * Update multiple players
     * 
     * @param players قائمة اللاعبين / Players list
     * @return عدد الصفوف المحدثة / Number of updated rows
     */
    @Update
    suspend fun updatePlayers(players: List<PlayerEntity>): Int

    /**
     * تحديث المستوى والخبرة
     * Update level and experience
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param level المستوى الجديد / New level
     * @param currentXP الخبرة الحالية / Current XP
     * @param xpToNextLevel الخبرة للمستوى التالي / XP to next level
     */
    @Query("""
        UPDATE players 
        SET level = :level, 
            currentXP = :currentXP, 
            xpToNextLevel = :xpToNextLevel,
            lastSaveTime = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateLevelAndXP(
        playerId: String,
        level: Int,
        currentXP: Int,
        xpToNextLevel: Int,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الصحة والطاقة
     * Update health and energy
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param health الصحة الحالية / Current health
     * @param energy الطاقة الحالية / Current energy
     */
    @Query("""
        UPDATE players 
        SET currentHealth = :health, 
            currentEnergy = :energy,
            lastSaveTime = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateHealthAndEnergy(
        playerId: String,
        health: Float,
        energy: Float,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الموقع
     * Update position
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param positionX موقع X / X position
     * @param positionY موقع Y / Y position
     * @param currentRegion المنطقة الحالية / Current region
     */
    @Query("""
        UPDATE players 
        SET positionX = :positionX, 
            positionY = :positionY,
            currentRegion = :currentRegion,
            lastSaveTime = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updatePosition(
        playerId: String,
        positionX: Float,
        positionY: Float,
        currentRegion: String,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث العملات
     * Update currencies
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param gold الذهب / Gold
     * @param gems الجواهر / Gems
     * @param lightFragments شظايا النور / Light fragments
     * @param darkEssence جوهر الظلام / Dark essence
     */
    @Query("""
        UPDATE players 
        SET gold = :gold,
            gems = :gems,
            lightFragments = :lightFragments,
            darkEssence = :darkEssence,
            lastSaveTime = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateCurrencies(
        playerId: String,
        gold: Int,
        gems: Int,
        lightFragments: Int,
        darkEssence: Int,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الإحصائيات
     * Update statistics
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param totalPlayTime إجمالي وقت اللعب / Total play time
     * @param deathCount عدد الوفيات / Death count
     * @param totalDamageDealt إجمالي الضرر المُلحق / Total damage dealt
     * @param totalDamageTaken إجمالي الضرر المُستقبل / Total damage taken
     */
    @Query("""
        UPDATE players 
        SET totalPlayTime = :totalPlayTime,
            deathCount = :deathCount,
            totalDamageDealt = :totalDamageDealt,
            totalDamageTaken = :totalDamageTaken,
            lastSaveTime = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateStatistics(
        playerId: String,
        totalPlayTime: Long,
        deathCount: Int,
        totalDamageDealt: Long,
        totalDamageTaken: Long,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث وقت آخر حفظ
     * Update last save time
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param saveTime وقت الحفظ / Save time
     */
    @Query("UPDATE players SET lastSaveTime = :saveTime WHERE playerId = :playerId")
    suspend fun updateLastSaveTime(playerId: String, saveTime: Long = System.currentTimeMillis()): Int

    /**
     * تحديث وقت آخر تسجيل دخول
     * Update last login time
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param loginTime وقت التسجيل / Login time
     */
    @Query("UPDATE players SET lastLoginTime = :loginTime WHERE playerId = :playerId")
    suspend fun updateLastLoginTime(playerId: String, loginTime: Long = System.currentTimeMillis()): Int

    /**
     * تحديث حالة المزامنة السحابية
     * Update cloud sync status
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param syncStatus حالة المزامنة / Sync status
     * @param lastCloudSync وقت آخر مزامنة / Last cloud sync time
     */
    @Query("""
        UPDATE players 
        SET syncStatus = :syncStatus,
            lastCloudSync = :lastCloudSync
        WHERE playerId = :playerId
    """)
    suspend fun updateCloudSyncStatus(
        playerId: String,
        syncStatus: String,
        lastCloudSync: Long = System.currentTimeMillis()
    ): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Query Operations - عمليات الاستعلام
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على لاعب بمعرفه (مع مراقبة التغييرات)
     * Get player by ID (with change observation)
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Flow من بيانات اللاعب / Flow of player data
     */
    @Query("SELECT * FROM players WHERE playerId = :playerId")
    fun getPlayerFlow(playerId: String): Flow<PlayerEntity?>

    /**
     * الحصول على لاعب بمعرفه (استعلام مباشر)
     * Get player by ID (direct query)
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return بيانات اللاعب / Player data
     */
    @Query("SELECT * FROM players WHERE playerId = :playerId")
    suspend fun getPlayer(playerId: String): PlayerEntity?

    /**
     * الحصول على جميع اللاعبين
     * Get all players
     * 
     * @return قائمة اللاعبين / Players list
     */
    @Query("SELECT * FROM players ORDER BY lastSaveTime DESC")
    suspend fun getAllPlayers(): List<PlayerEntity>

    /**
     * الحصول على جميع اللاعبين (مع المراقبة)
     * Get all players (with observation)
     * 
     * @return Flow من قائمة اللاعبين / Flow of players list
     */
    @Query("SELECT * FROM players ORDER BY lastSaveTime DESC")
    fun getAllPlayersFlow(): Flow<List<PlayerEntity>>

    /**
     * البحث عن لاعبين بالاسم
     * Search players by name
     * 
     * @param searchQuery نص البحث / Search query
     * @return قائمة اللاعبين المطابقين / Matching players list
     */
    @Query("""
        SELECT * FROM players 
        WHERE playerName LIKE '%' || :searchQuery || '%'
        ORDER BY playerName ASC
    """)
    suspend fun searchPlayersByName(searchQuery: String): List<PlayerEntity>

    /**
     * الحصول على اللاعبين في منطقة معينة
     * Get players in specific region
     * 
     * @param region المنطقة / Region
     * @return قائمة اللاعبين / Players list
     */
    @Query("SELECT * FROM players WHERE currentRegion = :region")
    suspend fun getPlayersInRegion(region: String): List<PlayerEntity>

    /**
     * الحصول على اللاعبين حسب المستوى
     * Get players by level range
     * 
     * @param minLevel المستوى الأدنى / Minimum level
     * @param maxLevel المستوى الأقصى / Maximum level
     * @return قائمة اللاعبين / Players list
     */
    @Query("""
        SELECT * FROM players 
        WHERE level BETWEEN :minLevel AND :maxLevel
        ORDER BY level DESC
    """)
    suspend fun getPlayersByLevelRange(minLevel: Int, maxLevel: Int): List<PlayerEntity>

    /**
     * الحصول على أعلى لاعب من حيث المستوى
     * Get highest level player
     * 
     * @return بيانات اللاعب / Player data
     */
    @Query("SELECT * FROM players ORDER BY level DESC LIMIT 1")
    suspend fun getHighestLevelPlayer(): PlayerEntity?

    /**
     * فحص وجود لاعب
     * Check if player exists
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return true إذا كان موجود / true if exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM players WHERE playerId = :playerId)")
    suspend fun playerExists(playerId: String): Boolean

    /**
     * الحصول على عدد اللاعبين
     * Get player count
     * 
     * @return عدد اللاعبين / Player count
     */
    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int

    /**
     * الحصول على عدد اللاعبين (مع المراقبة)
     * Get player count (with observation)
     * 
     * @return Flow من عدد اللاعبين / Flow of player count
     */
    @Query("SELECT COUNT(*) FROM players")
    fun getPlayerCountFlow(): Flow<Int>

    // ═══════════════════════════════════════════════════════════════════════
    // Specific Data Queries - استعلامات بيانات محددة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على المستوى والخبرة فقط
     * Get level and XP only
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Map يحتوي على level, currentXP, xpToNextLevel
     */
    @Query("""
        SELECT level, currentXP, xpToNextLevel 
        FROM players 
        WHERE playerId = :playerId
    """)
    suspend fun getLevelAndXP(playerId: String): LevelAndXP?

    /**
     * الحصول على الصحة والطاقة فقط
     * Get health and energy only
     * 
     * @param playerId معرف اللاعب / Player ID
     */
    @Query("""
        SELECT currentHealth, maxHealth, currentEnergy, maxEnergy 
        FROM players 
        WHERE playerId = :playerId
    """)
    suspend fun getHealthAndEnergy(playerId: String): HealthAndEnergy?

    /**
     * الحصول على الموقع فقط
     * Get position only
     * 
     * @param playerId معرف اللاعب / Player ID
     */
    @Query("""
        SELECT positionX, positionY, currentRegion 
        FROM players 
        WHERE playerId = :playerId
    """)
    suspend fun getPosition(playerId: String): PlayerPositionData?

    /**
     * الحصول على العملات فقط
     * Get currencies only
     * 
     * @param playerId معرف اللاعب / Player ID
     */
    @Query("""
        SELECT gold, gems, lightFragments, darkEssence 
        FROM players 
        WHERE playerId = :playerId
    """)
    suspend fun getCurrencies(playerId: String): PlayerCurrencies?

    /**
     * الحصول على الإحصائيات فقط
     * Get statistics only
     * 
     * @param playerId معرف اللاعب / Player ID
     */
    @Query("""
        SELECT totalPlayTime, deathCount, totalDamageDealt, totalDamageTaken,
               distanceTraveled, jumpCount, dashCount
        FROM players 
        WHERE playerId = :playerId
    """)
    suspend fun getStatistics(playerId: String): PlayerStatistics?

    // ═══════════════════════════════════════════════════════════════════════
    // Delete Operations - عمليات الحذف
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حذف لاعب
     * Delete player
     * 
     * @param player بيانات اللاعب / Player data
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Delete
    suspend fun deletePlayer(player: PlayerEntity): Int

    /**
     * حذف لاعب بمعرفه
     * Delete player by ID
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM players WHERE playerId = :playerId")
    suspend fun deletePlayerById(playerId: String): Int

    /**
     * حذف جميع اللاعبين
     * Delete all players
     * 
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers(): Int

    /**
     * حذف اللاعبين القدامى (لم يتم تسجيل دخول منذ فترة)
     * Delete old players (no login for a while)
     * 
     * @param olderThan الوقت الأقدم من / Older than timestamp
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM players WHERE lastLoginTime < :olderThan")
    suspend fun deleteOldPlayers(olderThan: Long): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Batch Operations - العمليات الجماعية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث آخر حفظ لجميع اللاعبين
     * Update last save for all players
     * 
     * @param saveTime وقت الحفظ / Save time
     */
    @Query("UPDATE players SET lastSaveTime = :saveTime")
    suspend fun updateAllPlayersLastSave(saveTime: Long = System.currentTimeMillis()): Int

    /**
     * إعادة تعيين الصحة والطاقة لجميع اللاعبين
     * Reset health and energy for all players
     */
    @Query("""
        UPDATE players 
        SET currentHealth = maxHealth,
            currentEnergy = maxEnergy
    """)
    suspend fun resetAllPlayersHealthAndEnergy(): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Transaction Operations - عمليات المعاملات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حفظ كامل للاعب (معاملة واحدة)
     * Full player save (single transaction)
     * 
     * @param player بيانات اللاعب / Player data
     */
    @Transaction
    suspend fun savePlayerComplete(player: PlayerEntity) {
        val exists = playerExists(player.playerId)
        if (exists) {
            updatePlayer(player)
        } else {
            insertPlayer(player)
        }
        updateLastSaveTime(player.playerId)
    }

    /**
     * نسخ لاعب (إنشاء نسخة جديدة)
     * Clone player (create new copy)
     * 
     * @param sourcePlayerId معرف اللاعب المصدر / Source player ID
     * @param newPlayerId معرف اللاعب الجديد / New player ID
     * @param newPlayerName اسم اللاعب الجديد / New player name
     */
    @Transaction
    suspend fun clonePlayer(sourcePlayerId: String, newPlayerId: String, newPlayerName: String) {
        val sourcePlayer = getPlayer(sourcePlayerId)
        if (sourcePlayer != null) {
            val clonedPlayer = sourcePlayer.copy(
                playerId = newPlayerId,
                playerName = newPlayerName,
                createdAt = System.currentTimeMillis(),
                lastSaveTime = System.currentTimeMillis(),
                lastLoginTime = System.currentTimeMillis()
            )
            insertPlayer(clonedPlayer)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Analytics Queries - استعلامات التحليلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على متوسط مستوى اللاعبين
     * Get average player level
     * 
     * @return متوسط المستوى / Average level
     */
    @Query("SELECT AVG(level) FROM players")
    suspend fun getAveragePlayerLevel(): Float

    /**
     * الحصول على إجمالي وقت اللعب لجميع اللاعبين
     * Get total play time for all players
     * 
     * @return إجمالي الوقت / Total time
     */
    @Query("SELECT SUM(totalPlayTime) FROM players")
    suspend fun getTotalPlayTime(): Long

    /**
     * الحصول على إجمالي الذهب لجميع اللاعبين
     * Get total gold for all players
     * 
     * @return إجمالي الذهب / Total gold
     */
    @Query("SELECT SUM(gold) FROM players")
    suspend fun getTotalGold(): Long

    /**
     * الحصول على أكثر منطقة زيارة
     * Get most visited region
     * 
     * @return المنطقة الأكثر زيارة / Most visited region
     */
    @Query("""
        SELECT currentRegion, COUNT(*) as count 
        FROM players 
        GROUP BY currentRegion 
        ORDER BY count DESC 
        LIMIT 1
    """)
    @MapInfo(keyColumn = "currentRegion", valueColumn = "count")
    suspend fun getMostVisitedRegion(): Map<String, Int>

    /**
     * الحصول على توزيع المستويات
     * Get level distribution
     * 
     * @return خريطة المستوى -> العدد / Level -> Count map
     */
    @Query("""
        SELECT level, COUNT(*) as count 
        FROM players 
        GROUP BY level 
        ORDER BY level ASC
    """)
    @MapInfo(keyColumn = "level", valueColumn = "count")
    suspend fun getLevelDistribution(): Map<Int, Int>
}