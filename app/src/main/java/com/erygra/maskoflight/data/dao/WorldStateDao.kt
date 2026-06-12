package com.erygra.maskoflight.data.dao

import androidx.room.*
import com.erygra.maskoflight.data.entities.WorldProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * WorldStateDao - واجهة الوصول لبيانات حالة العالم
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Data Access Object لإدارة عمليات قاعدة البيانات المتعلقة بحالة العالم وتقدم اللاعب
 * 
 * Data Access Object for managing world state and player progress database operations
 * 
 * العمليات المدعومة / Supported Operations:
 * - تتبع التقدم في العالم (World progress tracking)
 * - إدارة المناطق (Region management)
 * - الأحداث العالمية (World events)
 * - الإحصائيات والتحليلات (Statistics & analytics)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Dao
interface WorldStateDao {

    // ═══════════════════════════════════════════════════════════════════════
    // Insert Operations - عمليات الإدراج
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إدراج حالة عالم جديدة
     * Insert new world state
     * 
     * @param worldProgress بيانات تقدم العالم / World progress data
     * @return معرف السطر المُدرج / Inserted row ID
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWorldProgress(worldProgress: WorldProgressEntity): Long

    /**
     * إدراج أو استبدال حالة عالم
     * Insert or replace world state
     * 
     * @param worldProgress بيانات تقدم العالم / World progress data
     * @return معرف السطر / Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceWorldProgress(worldProgress: WorldProgressEntity): Long

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations - عمليات التحديث
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث حالة العالم
     * Update world state
     * 
     * @param worldProgress بيانات تقدم العالم المحدثة / Updated world progress data
     * @return عدد الصفوف المحدثة / Number of updated rows
     */
    @Update
    suspend fun updateWorldProgress(worldProgress: WorldProgressEntity): Int

    /**
     * تحديث المنطقة الحالية
     * Update current region
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param currentRegion المنطقة الحالية / Current region
     */
    @Query("""
        UPDATE world_progress 
        SET currentRegion = :currentRegion,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateCurrentRegion(
        playerId: String,
        currentRegion: String,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الموقع الأخير
     * Update last position
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param lastPosition الموقع الأخير / Last position
     */
    @Query("""
        UPDATE world_progress 
        SET lastPosition = :lastPosition,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateLastPosition(
        playerId: String,
        lastPosition: Map<String, Float>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث نسبة الاكتشاف الإجمالية
     * Update total discovery percentage
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param percentage نسبة الاكتشاف / Discovery percentage
     */
    @Query("""
        UPDATE world_progress 
        SET totalDiscoveryPercentage = :percentage,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateTotalDiscovery(
        playerId: String,
        percentage: Int,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث اكتشاف منطقة
     * Update region discovery
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param regionDiscovery خريطة اكتشاف المناطق / Region discovery map
     */
    @Query("""
        UPDATE world_progress 
        SET regionDiscovery = :regionDiscovery,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateRegionDiscovery(
        playerId: String,
        regionDiscovery: Map<String, Int>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * إضافة منطقة مفتوحة
     * Add unlocked region
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param unlockedRegions قائمة المناطق المفتوحة / Unlocked regions list
     */
    @Query("""
        UPDATE world_progress 
        SET unlockedRegions = :unlockedRegions,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateUnlockedRegions(
        playerId: String,
        unlockedRegions: List<String>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الملاذات المكتشفة
     * Update discovered sanctuaries
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param discoveredSanctuaries قائمة الملاذات المكتشفة / Discovered sanctuaries list
     */
    @Query("""
        UPDATE world_progress 
        SET discoveredSanctuaries = :discoveredSanctuaries,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateDiscoveredSanctuaries(
        playerId: String,
        discoveredSanctuaries: List<String>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث آخر ملاذ
     * Update last sanctuary
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param lastSanctuary آخر ملاذ / Last sanctuary
     */
    @Query("""
        UPDATE world_progress 
        SET lastSanctuary = :lastSanctuary,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateLastSanctuary(
        playerId: String,
        lastSanctuary: String,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الأعداء المهزومين
     * Update defeated enemies
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param defeatedEnemies خريطة الأعداء المهزومين / Defeated enemies map
     * @param totalEnemiesDefeated إجمالي الأعداء المهزومين / Total enemies defeated
     */
    @Query("""
        UPDATE world_progress 
        SET defeatedEnemies = :defeatedEnemies,
            totalEnemiesDefeated = :totalEnemiesDefeated,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateDefeatedEnemies(
        playerId: String,
        defeatedEnemies: Map<String, Int>,
        totalEnemiesDefeated: Int,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الزعماء المهزومين
     * Update defeated bosses
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param defeatedBosses قائمة الزعماء المهزومين / Defeated bosses list
     */
    @Query("""
        UPDATE world_progress 
        SET defeatedBosses = :defeatedBosses,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateDefeatedBosses(
        playerId: String,
        defeatedBosses: List<String>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الأحداث المكتملة
     * Update completed events
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param completedEvents قائمة الأحداث المكتملة / Completed events list
     */
    @Query("""
        UPDATE world_progress 
        SET completedEvents = :completedEvents,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateCompletedEvents(
        playerId: String,
        completedEvents: List<String>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث المتغيرات العالمية
     * Update world variables
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param worldVariables المتغيرات العالمية / World variables
     */
    @Query("""
        UPDATE world_progress 
        SET worldVariables = :worldVariables,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateWorldVariables(
        playerId: String,
        worldVariables: Map<String, Any>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث الأعلام العالمية
     * Update world flags
     * 
     * @param playerId معرف اللاعب / Player ID
     * @param worldFlags الأعلام العالمية / World flags
     */
    @Query("""
        UPDATE world_progress 
        SET worldFlags = :worldFlags,
            lastUpdated = :updateTime
        WHERE playerId = :playerId
    """)
    suspend fun updateWorldFlags(
        playerId: String,
        worldFlags: Map<String, Boolean>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Query Operations - عمليات الاستعلام
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على تقدم العالم (مع المراقبة)
     * Get world progress (with observation)
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return Flow من بيانات التقدم / Flow of progress data
     */
    @Query("SELECT * FROM world_progress WHERE playerId = :playerId")
    fun getWorldProgressFlow(playerId: String): Flow<WorldProgressEntity?>

    /**
     * الحصول على تقدم العالم (استعلام مباشر)
     * Get world progress (direct query)
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return بيانات التقدم / Progress data
     */
    @Query("SELECT * FROM world_progress WHERE playerId = :playerId")
    suspend fun getWorldProgress(playerId: String): WorldProgressEntity?

    /**
     * الحصول على جميع حالات العالم
     * Get all world states
     * 
     * @return قائمة حالات العالم / World states list
     */
    @Query("SELECT * FROM world_progress ORDER BY lastUpdated DESC")
    suspend fun getAllWorldProgress(): List<WorldProgressEntity>

    /**
     * الحصول على المنطقة الحالية
     * Get current region
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return المنطقة الحالية / Current region
     */
    @Query("SELECT currentRegion FROM world_progress WHERE playerId = :playerId")
    suspend fun getCurrentRegion(playerId: String): String?

    /**
     * الحصول على المناطق المفتوحة
     * Get unlocked regions
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return قائمة المناطق / Regions list
     */
    @Query("SELECT unlockedRegions FROM world_progress WHERE playerId = :playerId")
    suspend fun getUnlockedRegions(playerId: String): List<String>?

    /**
     * الحصول على الزعماء المهزومين
     * Get defeated bosses
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return قائمة الزعماء / Bosses list
     */
    @Query("SELECT defeatedBosses FROM world_progress WHERE playerId = :playerId")
    suspend fun getDefeatedBosses(playerId: String): List<String>?

    /**
     * الحصول على نسبة الاكتشاف
     * Get discovery percentage
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return نسبة الاكتشاف / Discovery percentage
     */
    @Query("SELECT totalDiscoveryPercentage FROM world_progress WHERE playerId = :playerId")
    suspend fun getDiscoveryPercentage(playerId: String): Int?

    /**
     * فحص وجود حالة عالم
     * Check if world state exists
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return true إذا كانت موجودة / true if exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM world_progress WHERE playerId = :playerId)")
    suspend fun worldProgressExists(playerId: String): Boolean

    /**
     * الحصول على عدد حالات العالم
     * Get world states count
     * 
     * @return عدد حالات العالم / World states count
     */
    @Query("SELECT COUNT(*) FROM world_progress")
    suspend fun getWorldProgressCount(): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Delete Operations - عمليات الحذف
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حذف حالة عالم
     * Delete world state
     * 
     * @param worldProgress بيانات التقدم / Progress data
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Delete
    suspend fun deleteWorldProgress(worldProgress: WorldProgressEntity): Int

    /**
     * حذف حالة عالم بمعرف اللاعب
     * Delete world state by player ID
     * 
     * @param playerId معرف اللاعب / Player ID
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM world_progress WHERE playerId = :playerId")
    suspend fun deleteWorldProgressByPlayerId(playerId: String): Int

    /**
     * حذف جميع حالات العالم
     * Delete all world states
     * 
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM world_progress")
    suspend fun deleteAllWorldProgress(): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Transaction Operations - عمليات المعاملات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حفظ كامل لحالة العالم (معاملة واحدة)
     * Full world state save (single transaction)
     * 
     * @param worldProgress بيانات التقدم / Progress data
     */
    @Transaction
    suspend fun saveWorldProgressComplete(worldProgress: WorldProgressEntity) {
        val exists = worldProgressExists(worldProgress.playerId)
        if (exists) {
            updateWorldProgress(worldProgress)
        } else {
            insertWorldProgress(worldProgress)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Analytics Queries - استعلامات التحليلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على متوسط نسبة الاكتشاف
     * Get average discovery percentage
     * 
     * @return متوسط الاكتشاف / Average discovery
     */
    @Query("SELECT AVG(totalDiscoveryPercentage) FROM world_progress")
    suspend fun getAverageDiscovery(): Float

    /**
     * الحصول على إجمالي الأعداء المهزومين
     * Get total enemies defeated
     * 
     * @return إجمالي الأعداء / Total enemies
     */
    @Query("SELECT SUM(totalEnemiesDefeated) FROM world_progress")
    suspend fun getTotalEnemiesDefeated(): Long

    /**
     * الحصول على أكثر منطقة زيارة
     * Get most visited region
     * 
     * @return المنطقة / Region
     */
    @Query("""
        SELECT currentRegion, COUNT(*) as count 
        FROM world_progress 
        GROUP BY currentRegion 
        ORDER BY count DESC 
        LIMIT 1
    """)
    @MapInfo(keyColumn = "currentRegion", valueColumn = "count")
    suspend fun getMostVisitedRegion(): Map<String, Int>

    /**
     * الحصول على إجمالي المسافة المقطوعة
     * Get total distance traveled
     * 
     * @return إجمالي المسافة / Total distance
     */
    @Query("SELECT SUM(totalDistance) FROM world_progress")
    suspend fun getTotalDistanceTraveled(): Long
}