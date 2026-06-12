package com.erygra.maskoflight.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * WorldProgressEntity - كيان تقدم العالم
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * كيان Room لحفظ تقدم اللاعب في العالم والمناطق
 * 
 * Room entity for persisting world and region progress
 * 
 * البيانات المحفوظة / Stored Data:
 * - المناطق المفتوحة والمكتشفة
 * - الملاذات والنقاط المرجعية
 * - الأحداث العالمية
 * - الأعداء والزعماء المهزومين
 * - اكتشاف الخريطة
 * - الحالات والمتغيرات العالمية
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Entity(
    tableName = "world_progress",
    indices = [
        Index(value = ["playerId"], unique = true),
        Index(value = ["currentRegion"]),
        Index(value = ["totalDiscoveryPercentage"]),
        Index(value = ["lastUpdated"])
    ]
)
data class WorldProgressEntity(
    // ═══════════════════════════════════════════════════════════════════════
    // Primary Key & Player Info
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * معرف اللاعب (Primary Key)
     * Player identifier (Primary Key)
     */
    @PrimaryKey
    @ColumnInfo(name = "playerId")
    val playerId: String,

    /**
     * المنطقة الحالية
     * Current region
     */
    @ColumnInfo(name = "currentRegion")
    val currentRegion: String = "forgotten_valley",

    /**
     * آخر موقع معروف
     * Last known position
     */
    @ColumnInfo(name = "lastPosition")
    val lastPosition: Map<String, Float> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Regions - المناطق
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المناطق المفتوحة
     * Unlocked regions
     */
    @ColumnInfo(name = "unlockedRegions")
    val unlockedRegions: List<String> = listOf("forgotten_valley"),

    /**
     * المناطق المكتشفة
     * Discovered regions
     */
    @ColumnInfo(name = "discoveredRegions")
    val discoveredRegions: List<String> = listOf("forgotten_valley"),

    /**
     * المناطق المكتملة (100%)
     * Completed regions (100%)
     */
    @ColumnInfo(name = "completedRegions")
    val completedRegions: List<String> = emptyList(),

    /**
     * نسبة اكتشاف كل منطقة (regionId -> percentage)
     * Discovery percentage per region
     */
    @ColumnInfo(name = "regionDiscovery")
    val regionDiscovery: Map<String, Int> = emptyMap(),

    /**
     * وقت أول زيارة لكل منطقة
     * First visit time per region
     */
    @ColumnInfo(name = "regionFirstVisit")
    val regionFirstVisit: Map<String, Long> = emptyMap(),

    /**
     * عدد زيارات كل منطقة
     * Visit count per region
     */
    @ColumnInfo(name = "regionVisitCount")
    val regionVisitCount: Map<String, Int> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Sanctuaries - الملاذات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الملاذات المكتشفة
     * Discovered sanctuaries
     */
    @ColumnInfo(name = "discoveredSanctuaries")
    val discoveredSanctuaries: List<String> = listOf("sanctuary_entrance"),

    /**
     * الملاذات المفعّلة (يمكن النقل إليها)
     * Activated sanctuaries (can teleport to)
     */
    @ColumnInfo(name = "activatedSanctuaries")
    val activatedSanctuaries: List<String> = listOf("sanctuary_entrance"),

    /**
     * آخر ملاذ تم زيارته
     * Last visited sanctuary
     */
    @ColumnInfo(name = "lastSanctuary")
    val lastSanctuary: String = "sanctuary_entrance",

    /**
     * إحصائيات الملاذات (sanctuaryId -> stats)
     * Sanctuary statistics
     */
    @ColumnInfo(name = "sanctuaryStats")
    val sanctuaryStats: Map<String, Map<String, Int>> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Waypoints & Fast Travel - نقاط النقل السريع
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * نقاط النقل السريع المفتوحة
     * Unlocked fast travel waypoints
     */
    @ColumnInfo(name = "unlockedWaypoints")
    val unlockedWaypoints: List<String> = emptyList(),

    /**
     * عدد استخدامات النقل السريع
     * Fast travel usage count
     */
    @ColumnInfo(name = "fastTravelCount")
    val fastTravelCount: Int = 0,

    /**
     * آخر نقطة نقل سريع مستخدمة
     * Last used waypoint
     */
    @ColumnInfo(name = "lastWaypoint")
    val lastWaypoint: String? = null,

    // ═══════════════════════════════════════════════════════════════════════
    // Enemies & Bosses - الأعداء والزعماء
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الأعداء المهزومين (enemyId -> count)
     * Defeated enemies
     */
    @ColumnInfo(name = "defeatedEnemies")
    val defeatedEnemies: Map<String, Int> = emptyMap(),

    /**
     * إجمالي الأعداء المهزومين
     * Total enemies defeated
     */
    @ColumnInfo(name = "totalEnemiesDefeated")
    val totalEnemiesDefeated: Int = 0,

    /**
     * الزعماء المهزومين
     * Defeated bosses
     */
    @ColumnInfo(name = "defeatedBosses")
    val defeatedBosses: List<String> = emptyList(),

    /**
     * أوقات هزيمة الزعماء
     * Boss defeat times
     */
    @ColumnInfo(name = "bossDefeatTimes")
    val bossDefeatTimes: Map<String, Long> = emptyMap(),

    /**
     * عدد محاولات الزعماء
     * Boss attempt counts
     */
    @ColumnInfo(name = "bossAttempts")
    val bossAttempts: Map<String, Int> = emptyMap(),

    /**
     * أفضل أوقات هزيمة الزعماء (ثواني)
     * Best boss clear times (seconds)
     */
    @ColumnInfo(name = "bossBestTimes")
    val bossBestTimes: Map<String, Float> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // World Events - الأحداث العالمية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الأحداث العالمية النشطة
     * Active world events
     */
    @ColumnInfo(name = "activeEvents")
    val activeEvents: List<String> = emptyList(),

    /**
     * الأحداث المكتملة
     * Completed events
     */
    @ColumnInfo(name = "completedEvents")
    val completedEvents: List<String> = emptyList(),

    /**
     * تقدم الأحداث (eventId -> progress)
     * Event progress
     */
    @ColumnInfo(name = "eventProgress")
    val eventProgress: Map<String, Int> = emptyMap(),

    /**
     * أوقات إكمال الأحداث
     * Event completion times
     */
    @ColumnInfo(name = "eventCompletionTimes")
    val eventCompletionTimes: Map<String, Long> = emptyMap(),

    /**
     * مكافآت الأحداث المُستلمة
     * Claimed event rewards
     */
    @ColumnInfo(name = "claimedEventRewards")
    val claimedEventRewards: List<String> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Map Discovery - اكتشاف الخريطة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * البلاطات المكتشفة في الخريطة (tileId)
     * Discovered map tiles
     */
    @ColumnInfo(name = "discoveredMapTiles")
    val discoveredMapTiles: List<String> = emptyList(),

    /**
     * نسبة الاكتشاف الإجمالية (0-100)
     * Total discovery percentage (0-100)
     */
    @ColumnInfo(name = "totalDiscoveryPercentage")
    val totalDiscoveryPercentage: Int = 0,

    /**
     * نقاط الاهتمام المكتشفة (POI)
     * Discovered points of interest
     */
    @ColumnInfo(name = "discoveredPOIs")
    val discoveredPOIs: List<String> = emptyList(),

    /**
     * الأماكن السرية المكتشفة
     * Discovered secret locations
     */
    @ColumnInfo(name = "discoveredSecrets")
    val discoveredSecrets: List<String> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Collectibles - المقتنيات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المقتنيات التي تم جمعها (collectibleId)
     * Collected collectibles
     */
    @ColumnInfo(name = "collectedItems")
    val collectedItems: List<String> = emptyList(),

    /**
     * الكنوز المفتوحة (chestId)
     * Opened treasure chests
     */
    @ColumnInfo(name = "openedChests")
    val openedChests: List<String> = emptyList(),

    /**
     * الأسرار المكتشفة (secretId)
     * Discovered secrets
     */
    @ColumnInfo(name = "foundSecrets")
    val foundSecrets: List<String> = emptyList(),

    /**
     * اللور (القصص) المكتشفة
     * Discovered lore entries
     */
    @ColumnInfo(name = "discoveredLore")
    val discoveredLore: List<String> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Hazards & Obstacles - المخاطر والعقبات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المخاطر المُعطّلة (hazardId)
     * Disabled hazards
     */
    @ColumnInfo(name = "disabledHazards")
    val disabledHazards: List<String> = emptyList(),

    /**
     * العقبات التي تم تجاوزها
     * Cleared obstacles
     */
    @ColumnInfo(name = "clearedObstacles")
    val clearedObstacles: List<String> = emptyList(),

    /**
     * المسارات المفتوحة
     * Unlocked paths
     */
    @ColumnInfo(name = "unlockedPaths")
    val unlockedPaths: List<String> = emptyList(),

    /**
     * الأبواب المفتوحة
     * Opened doors
     */
    @ColumnInfo(name = "openedDoors")
    val openedDoors: List<String> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // World State - حالة العالم
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المتغيرات العالمية (variableId -> value)
     * Global variables
     */
    @ColumnInfo(name = "worldVariables")
    val worldVariables: Map<String, Any> = emptyMap(),

    /**
     * الأعلام العالمية (flags)
     * World flags
     */
    @ColumnInfo(name = "worldFlags")
    val worldFlags: Map<String, Boolean> = emptyMap(),

    /**
     * الحالات العالمية النشطة
     * Active world states
     */
    @ColumnInfo(name = "activeStates")
    val activeStates: List<String> = emptyList(),

    /**
     * التغييرات الدائمة في العالم
     * Permanent world changes
     */
    @ColumnInfo(name = "permanentChanges")
    val permanentChanges: List<Map<String, Any>> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Weather & Time - الطقس والوقت
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الطقس الحالي في كل منطقة
     * Current weather per region
     */
    @ColumnInfo(name = "regionWeather")
    val regionWeather: Map<String, String> = emptyMap(),

    /**
     * الوقت في اللعبة (in-game time)
     * In-game time
     */
    @ColumnInfo(name = "gameTime")
    val gameTime: Long = 0L,

    /**
     * دورة النهار/الليل (day/night cycle)
     * Day/night cycle phase
     */
    @ColumnInfo(name = "timeOfDay")
    val timeOfDay: String = "day",

    // ═══════════════════════════════════════════════════════════════════════
    // Statistics - الإحصائيات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المسافة المقطوعة في كل منطقة
     * Distance traveled per region
     */
    @ColumnInfo(name = "regionDistances")
    val regionDistances: Map<String, Long> = emptyMap(),

    /**
     * إجمالي المسافة المقطوعة
     * Total distance traveled
     */
    @ColumnInfo(name = "totalDistance")
    val totalDistance: Long = 0L,

    /**
     * الوقت المقضي في كل منطقة (ثواني)
     * Time spent per region (seconds)
     */
    @ColumnInfo(name = "regionPlayTime")
    val regionPlayTime: Map<String, Long> = emptyMap(),

    /**
     * عدد الوفيات في كل منطقة
     * Death count per region
     */
    @ColumnInfo(name = "regionDeaths")
    val regionDeaths: Map<String, Int> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Additional Data - بيانات إضافية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * بيانات مخصصة
     * Custom data
     */
    @ColumnInfo(name = "customData")
    val customData: Map<String, String> = emptyMap(),

    /**
     * وقت الإنشاء
     * Creation time
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * آخر تحديث
     * Last update time
     */
    @ColumnInfo(name = "lastUpdated")
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * الحصول على عدد المناطق المفتوحة
     * Get unlocked regions count
     */
    fun getUnlockedRegionsCount(): Int = unlockedRegions.size

    /**
     * الحصول على عدد المناطق المكتشفة
     * Get discovered regions count
     */
    fun getDiscoveredRegionsCount(): Int = discoveredRegions.size

    /**
     * الحصول على عدد المناطق المكتملة
     * Get completed regions count
     */
    fun getCompletedRegionsCount(): Int = completedRegions.size

    /**
     * التحقق من فتح منطقة
     * Check if region is unlocked
     */
    fun isRegionUnlocked(regionId: String): Boolean {
        return regionId in unlockedRegions
    }

    /**
     * التحقق من اكتشاف منطقة
     * Check if region is discovered
     */
    fun isRegionDiscovered(regionId: String): Boolean {
        return regionId in discoveredRegions
    }

    /**
     * التحقق من إكمال منطقة
     * Check if region is completed
     */
    fun isRegionCompleted(regionId: String): Boolean {
        return regionId in completedRegions
    }

    /**
     * الحصول على نسبة اكتشاف منطقة
     * Get region discovery percentage
     */
    fun getRegionDiscoveryPercentage(regionId: String): Int {
        return regionDiscovery[regionId] ?: 0
    }

    /**
     * الحصول على عدد الملاذات المكتشفة
     * Get discovered sanctuaries count
     */
    fun getDiscoveredSanctuariesCount(): Int = discoveredSanctuaries.size

    /**
     * الحصول على عدد الملاذات المفعّلة
     * Get activated sanctuaries count
     */
    fun getActivatedSanctuariesCount(): Int = activatedSanctuaries.size

    /**
     * التحقق من تفعيل ملاذ
     * Check if sanctuary is activated
     */
    fun isSanctuaryActivated(sanctuaryId: String): Boolean {
        return sanctuaryId in activatedSanctuaries
    }

    /**
     * الحصول على عدد الزعماء المهزومين
     * Get defeated bosses count
     */
    fun getDefeatedBossesCount(): Int = defeatedBosses.size

    /**
     * التحقق من هزيمة زعيم
     * Check if boss is defeated
     */
    fun isBossDefeated(bossId: String): Boolean {
        return bossId in defeatedBosses
    }

    /**
     * الحصول على عدد محاولات زعيم
     * Get boss attempt count
     */
    fun getBossAttempts(bossId: String): Int {
        return bossAttempts[bossId] ?: 0
    }

    /**
     * الحصول على أفضل وقت لهزيمة زعيم
     * Get boss best clear time
     */
    fun getBossBestTime(bossId: String): Float? {
        return bossBestTimes[bossId]
    }

    /**
     * الحصول على عدد الأحداث المكتملة
     * Get completed events count
     */
    fun getCompletedEventsCount(): Int = completedEvents.size

    /**
     * التحقق من إكمال حدث
     * Check if event is completed
     */
    fun isEventCompleted(eventId: String): Boolean {
        return eventId in completedEvents
    }

    /**
     * الحصول على تقدم حدث
     * Get event progress
     */
    fun getEventProgress(eventId: String): Int {
        return eventProgress[eventId] ?: 0
    }

    /**
     * الحصول على عدد الأسرار المكتشفة
     * Get discovered secrets count
     */
    fun getDiscoveredSecretsCount(): Int = discoveredSecrets.size

    /**
     * الحصول على عدد المقتنيات
     * Get collectibles count
     */
    fun getCollectiblesCount(): Int = collectedItems.size

    /**
     * حساب نسبة الإكمال الإجمالية
     * Calculate overall completion percentage
     */
    fun calculateOverallCompletion(): Float {
        val factors = listOf(
            totalDiscoveryPercentage.toFloat(),
            (getCompletedRegionsCount().toFloat() / maxOf(unlockedRegions.size, 1)) * 100f,
            (defeatedBosses.size.toFloat() / 10f) * 100f, // افتراض 10 زعماء
            (completedEvents.size.toFloat() / 20f) * 100f // افتراض 20 حدث
        )
        return factors.average().toFloat()
    }

    /**
     * تحويل إلى سلسلة نصية للتصحيح
     * Convert to debug string
     */
    override fun toString(): String {
        return "WorldProgressEntity(player='$playerId', region='$currentRegion', " +
                "discovery=$totalDiscoveryPercentage%, bosses=${defeatedBosses.size}, " +
                "completion=${calculateOverallCompletion()}%)"
    }
}