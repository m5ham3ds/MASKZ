package com.erygra.maskoflight.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.erygra.maskoflight.player.ItemType
import com.erygra.maskoflight.player.Rarity

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * PlayerEntity - كيان بيانات اللاعب
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * كيان Room لحفظ جميع بيانات اللاعب في قاعدة البيانات المحلية
 * 
 * Room entity for persisting all player data in local database
 * 
 * البيانات المحفوظة / Stored Data:
 * - معلومات اللاعب الأساسية (Basic info)
 * - الإحصائيات والمستوى (Stats & level)
 * - المخزون والعتاد (Inventory & equipment)
 * - القدرات والمهارات (Abilities & skills)
 * - التقدم في العالم (World progress)
 * - الإنجازات والعملات (Achievements & currencies)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Entity(
    tableName = "players",
    indices = [
        Index(value = ["playerId"], unique = true),
        Index(value = ["playerName"]),
        Index(value = ["level"]),
        Index(value = ["lastSaveTime"])
    ]
)
data class PlayerEntity(
    // ═══════════════════════════════════════════════════════════════════════
    // Primary Key & Basic Info
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * معرف اللاعب الفريد (Primary Key)
     * Unique player identifier
     */
    @PrimaryKey
    @ColumnInfo(name = "playerId")
    val playerId: String,

    /**
     * اسم اللاعب
     * Player name
     */
    @ColumnInfo(name = "playerName")
    val playerName: String,

    /**
     * المستوى الحالي
     * Current level
     */
    @ColumnInfo(name = "level")
    val level: Int = 1,

    /**
     * نقاط الخبرة الحالية
     * Current experience points
     */
    @ColumnInfo(name = "currentXP")
    val currentXP: Int = 0,

    /**
     * نقاط الخبرة المطلوبة للمستوى التالي
     * XP required for next level
     */
    @ColumnInfo(name = "xpToNextLevel")
    val xpToNextLevel: Int = 100,

    // ═══════════════════════════════════════════════════════════════════════
    // Combat Stats - إحصائيات القتال
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * نقاط الصحة الحالية
     * Current health points
     */
    @ColumnInfo(name = "currentHealth")
    val currentHealth: Float = 100f,

    /**
     * الحد الأقصى لنقاط الصحة
     * Maximum health points
     */
    @ColumnInfo(name = "maxHealth")
    val maxHealth: Float = 100f,

    /**
     * نقاط الطاقة الحالية
     * Current energy points
     */
    @ColumnInfo(name = "currentEnergy")
    val currentEnergy: Float = 100f,

    /**
     * الحد الأقصى لنقاط الطاقة
     * Maximum energy points
     */
    @ColumnInfo(name = "maxEnergy")
    val maxEnergy: Float = 100f,

    /**
     * معدل استعادة الطاقة في الثانية
     * Energy regeneration rate per second
     */
    @ColumnInfo(name = "energyRegen")
    val energyRegen: Float = 5f,

    /**
     * قوة الهجوم الأساسية
     * Base attack power
     */
    @ColumnInfo(name = "attackPower")
    val attackPower: Float = 10f,

    /**
     * قوة الدفاع
     * Defense power
     */
    @ColumnInfo(name = "defense")
    val defense: Float = 5f,

    /**
     * فرصة الضربة الحرجة (0-100)
     * Critical hit chance (0-100)
     */
    @ColumnInfo(name = "criticalChance")
    val criticalChance: Float = 5f,

    /**
     * ضرر الضربة الحرجة (مضاعف)
     * Critical damage multiplier
     */
    @ColumnInfo(name = "criticalDamage")
    val criticalDamage: Float = 1.5f,

    /**
     * سرعة الهجوم
     * Attack speed
     */
    @ColumnInfo(name = "attackSpeed")
    val attackSpeed: Float = 1f,

    /**
     * فرصة التفادي (0-100)
     * Dodge chance (0-100)
     */
    @ColumnInfo(name = "dodgeChance")
    val dodgeChance: Float = 10f,

    // ═══════════════════════════════════════════════════════════════════════
    // Movement & Physics - الحركة والفيزياء
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * سرعة الحركة
     * Movement speed
     */
    @ColumnInfo(name = "moveSpeed")
    val moveSpeed: Float = 300f,

    /**
     * قوة القفز
     * Jump force
     */
    @ColumnInfo(name = "jumpForce")
    val jumpForce: Float = 600f,

    /**
     * عدد القفزات المتاحة
     * Number of available jumps
     */
    @ColumnInfo(name = "maxJumps")
    val maxJumps: Int = 1,

    /**
     * سرعة الاندفاع
     * Dash speed
     */
    @ColumnInfo(name = "dashSpeed")
    val dashSpeed: Float = 500f,

    /**
     * مدة الاندفاع (ثانية)
     * Dash duration (seconds)
     */
    @ColumnInfo(name = "dashDuration")
    val dashDuration: Float = 0.2f,

    /**
     * مدة الانتظار بين الاندفاعات (ثانية)
     * Dash cooldown (seconds)
     */
    @ColumnInfo(name = "dashCooldown")
    val dashCooldown: Float = 1f,

    // ═══════════════════════════════════════════════════════════════════════
    // Position & State - الموقع والحالة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الموقع X الحالي في العالم
     * Current X position in world
     */
    @ColumnInfo(name = "positionX")
    val positionX: Float = 0f,

    /**
     * الموقع Y الحالي في العالم
     * Current Y position in world
     */
    @ColumnInfo(name = "positionY")
    val positionY: Float = 0f,

    /**
     * المنطقة الحالية
     * Current region
     */
    @ColumnInfo(name = "currentRegion")
    val currentRegion: String = "forgotten_valley",

    /**
     * آخر نقطة حفظ (Sanctuary)
     * Last save point (sanctuary)
     */
    @ColumnInfo(name = "lastSanctuary")
    val lastSanctuary: String = "sanctuary_entrance",

    /**
     * اتجاه النظر (يمين = true، يسار = false)
     * Facing direction (right = true, left = false)
     */
    @ColumnInfo(name = "facingRight")
    val facingRight: Boolean = true,

    // ═══════════════════════════════════════════════════════════════════════
    // Abilities & Skills - القدرات والمهارات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * القدرات المفتوحة (أسماء فقط)
     * Unlocked abilities (names only)
     */
    @ColumnInfo(name = "unlockedAbilities")
    val unlockedAbilities: List<String> = emptyList(),

    /**
     * القدرات المجهزة (slots)
     * Equipped abilities (slots)
     */
    @ColumnInfo(name = "equippedAbilities")
    val equippedAbilities: Map<String, String> = emptyMap(),

    /**
     * مستويات المهارات
     * Skill levels
     */
    @ColumnInfo(name = "skillLevels")
    val skillLevels: Map<String, Int> = emptyMap(),

    /**
     * نقاط المهارات المتاحة
     * Available skill points
     */
    @ColumnInfo(name = "skillPoints")
    val skillPoints: Int = 0,

    /**
     * شجرة المهارات المفتوحة
     * Unlocked skill tree nodes
     */
    @ColumnInfo(name = "unlockedSkillNodes")
    val unlockedSkillNodes: List<String> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Inventory & Equipment - المخزون والعتاد
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * العناصر في المخزون (itemId -> quantity)
     * Items in inventory (itemId -> quantity)
     */
    @ColumnInfo(name = "inventoryItems")
    val inventoryItems: Map<String, Int> = emptyMap(),

    /**
     * الحد الأقصى لمساحة المخزون
     * Maximum inventory slots
     */
    @ColumnInfo(name = "maxInventorySlots")
    val maxInventorySlots: Int = 20,

    /**
     * العتاد المجهز (slot -> itemId)
     * Equipped gear (slot -> itemId)
     */
    @ColumnInfo(name = "equippedGear")
    val equippedGear: Map<String, String> = emptyMap(),

    /**
     * القناع المجهز الحالي
     * Currently equipped mask
     */
    @ColumnInfo(name = "currentMask")
    val currentMask: String? = null,

    /**
     * الأقنعة المفتوحة
     * Unlocked masks
     */
    @ColumnInfo(name = "unlockedMasks")
    val unlockedMasks: List<String> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Currencies & Resources - العملات والموارد
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الذهب
     * Gold currency
     */
    @ColumnInfo(name = "gold")
    val gold: Int = 0,

    /**
     * الجواهر (العملة المميزة)
     * Gems (premium currency)
     */
    @ColumnInfo(name = "gems")
    val gems: Int = 0,

    /**
     * شظايا النور (مورد خاص)
     * Light fragments (special resource)
     */
    @ColumnInfo(name = "lightFragments")
    val lightFragments: Int = 0,

    /**
     * جوهر الظلام (مورد خاص)
     * Dark essence (special resource)
     */
    @ColumnInfo(name = "darkEssence")
    val darkEssence: Int = 0,

    /**
     * موارد إضافية (resourceId -> quantity)
     * Additional resources
     */
    @ColumnInfo(name = "resources")
    val resources: Map<String, Int> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // World Progress - تقدم العالم
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المناطق المفتوحة
     * Unlocked regions
     */
    @ColumnInfo(name = "unlockedRegions")
    val unlockedRegions: List<String> = listOf("forgotten_valley"),

    /**
     * الملاذات المكتشفة
     * Discovered sanctuaries
     */
    @ColumnInfo(name = "discoveredSanctuaries")
    val discoveredSanctuaries: List<String> = listOf("sanctuary_entrance"),

    /**
     * نقاط النقل السريع المفتوحة
     * Unlocked fast travel points
     */
    @ColumnInfo(name = "unlockedWaypoints")
    val unlockedWaypoints: List<String> = emptyList(),

    /**
     * الخرائط المكتشفة (regionId -> percentage)
     * Discovered maps (regionId -> percentage)
     */
    @ColumnInfo(name = "mapDiscovery")
    val mapDiscovery: Map<String, Int> = emptyMap(),

    /**
     * الأعداء المهزومين (enemyId -> count)
     * Defeated enemies (enemyId -> count)
     */
    @ColumnInfo(name = "defeatedEnemies")
    val defeatedEnemies: Map<String, Int> = emptyMap(),

    /**
     * الزعماء المهزومين
     * Defeated bosses
     */
    @ColumnInfo(name = "defeatedBosses")
    val defeatedBosses: List<String> = emptyList(),

    /**
     * الأحداث العالمية المكتملة
     * Completed world events
     */
    @ColumnInfo(name = "completedEvents")
    val completedEvents: List<String> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Quests & Achievements - المهام والإنجازات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المهام النشطة
     * Active quests
     */
    @ColumnInfo(name = "activeQuests")
    val activeQuests: List<String> = emptyList(),

    /**
     * المهام المكتملة
     * Completed quests
     */
    @ColumnInfo(name = "completedQuests")
    val completedQuests: List<String> = emptyList(),

    /**
     * المهام الفاشلة
     * Failed quests
     */
    @ColumnInfo(name = "failedQuests")
    val failedQuests: List<String> = emptyList(),

    /**
     * تقدم المهام (questId -> progress data)
     * Quest progress
     */
    @ColumnInfo(name = "questProgress")
    val questProgress: Map<String, String> = emptyMap(),

    /**
     * الإنجازات المفتوحة
     * Unlocked achievements
     */
    @ColumnInfo(name = "achievements")
    val achievements: List<String> = emptyList(),

    /**
     * تقدم الإنجازات (achievementId -> progress)
     * Achievement progress
     */
    @ColumnInfo(name = "achievementProgress")
    val achievementProgress: Map<String, Int> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Statistics - الإحصائيات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إجمالي وقت اللعب (بالثواني)
     * Total play time (seconds)
     */
    @ColumnInfo(name = "totalPlayTime")
    val totalPlayTime: Long = 0L,

    /**
     * عدد الوفيات
     * Death count
     */
    @ColumnInfo(name = "deathCount")
    val deathCount: Int = 0,

    /**
     * إجمالي الضرر المُلحق
     * Total damage dealt
     */
    @ColumnInfo(name = "totalDamageDealt")
    val totalDamageDealt: Long = 0L,

    /**
     * إجمالي الضرر المُستقبل
     * Total damage taken
     */
    @ColumnInfo(name = "totalDamageTaken")
    val totalDamageTaken: Long = 0L,

    /**
     * المسافة المقطوعة (بالبكسل)
     * Distance traveled (pixels)
     */
    @ColumnInfo(name = "distanceTraveled")
    val distanceTraveled: Long = 0L,

    /**
     * عدد القفزات
     * Jump count
     */
    @ColumnInfo(name = "jumpCount")
    val jumpCount: Int = 0,

    /**
     * عدد الاندفاعات
     * Dash count
     */
    @ColumnInfo(name = "dashCount")
    val dashCount: Int = 0,

    /**
     * إحصائيات إضافية
     * Additional statistics
     */
    @ColumnInfo(name = "statistics")
    val statistics: Map<String, Int> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Settings & Preferences - الإعدادات والتفضيلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * صعوبة اللعبة
     * Game difficulty
     */
    @ColumnInfo(name = "difficulty")
    val difficulty: String = "normal",

    /**
     * اللغة المفضلة
     * Preferred language
     */
    @ColumnInfo(name = "language")
    val language: String = "ar",

    /**
     * إعدادات التحكم
     * Control settings
     */
    @ColumnInfo(name = "controlSettings")
    val controlSettings: Map<String, String> = emptyMap(),

    /**
     * إعدادات العرض
     * Display settings
     */
    @ColumnInfo(name = "displaySettings")
    val displaySettings: Map<String, Boolean> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Timestamps - الطوابع الزمنية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * وقت إنشاء الحساب
     * Account creation time
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * آخر وقت حفظ
     * Last save time
     */
    @ColumnInfo(name = "lastSaveTime")
    val lastSaveTime: Long = System.currentTimeMillis(),

    /**
     * آخر تسجيل دخول
     * Last login time
     */
    @ColumnInfo(name = "lastLoginTime")
    val lastLoginTime: Long = System.currentTimeMillis(),

    /**
     * آخر مزامنة سحابية
     * Last cloud sync time
     */
    @ColumnInfo(name = "lastCloudSync")
    val lastCloudSync: Long? = null,

    // ═══════════════════════════════════════════════════════════════════════
    // Cloud & Sync - السحابة والمزامنة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * معرف الحساب السحابي
     * Cloud account ID
     */
    @ColumnInfo(name = "cloudAccountId")
    val cloudAccountId: String? = null,

    /**
     * حالة المزامنة
     * Sync status
     */
    @ColumnInfo(name = "syncStatus")
    val syncStatus: String = "local",

    /**
     * رقم إصدار الحفظ
     * Save version number
     */
    @ColumnInfo(name = "saveVersion")
    val saveVersion: Int = 1,

    /**
     * بيانات مخصصة إضافية (JSON)
     * Additional custom data (JSON)
     */
    @ColumnInfo(name = "customData")
    val customData: Map<String, String> = emptyMap()
) {
    /**
     * التحقق من صحة البيانات
     * Validate data integrity
     * 
     * @return true إذا كانت البيانات صحيحة / true if data is valid
     */
    fun isValid(): Boolean {
        return playerId.isNotBlank() &&
                playerName.isNotBlank() &&
                level > 0 &&
                currentHealth >= 0 &&
                maxHealth > 0 &&
                currentEnergy >= 0 &&
                maxEnergy > 0
    }

    /**
     * الحصول على نسبة الصحة (0-1)
     * Get health percentage (0-1)
     */
    fun getHealthPercentage(): Float {
        return if (maxHealth > 0) currentHealth / maxHealth else 0f
    }

    /**
     * الحصول على نسبة الطاقة (0-1)
     * Get energy percentage (0-1)
     */
    fun getEnergyPercentage(): Float {
        return if (maxEnergy > 0) currentEnergy / maxEnergy else 0f
    }

    /**
     * الحصول على نسبة التقدم للمستوى التالي (0-1)
     * Get level progress percentage (0-1)
     */
    fun getLevelProgressPercentage(): Float {
        return if (xpToNextLevel > 0) currentXP.toFloat() / xpToNextLevel else 0f
    }

    /**
     * حساب القوة الإجمالية للاعب
     * Calculate total player power
     */
    fun calculatePowerLevel(): Int {
        return (level * 100) +
                (attackPower * 10).toInt() +
                (defense * 10).toInt() +
                (maxHealth * 0.5f).toInt() +
                (unlockedAbilities.size * 50) +
                (skillLevels.values.sum() * 20)
    }

    /**
     * التحقق من إمكانية رفع المستوى
     * Check if can level up
     */
    fun canLevelUp(): Boolean {
        return currentXP >= xpToNextLevel
    }

    /**
     * عدد العناصر في المخزون
     * Count items in inventory
     */
    fun getInventoryItemCount(): Int {
        return inventoryItems.values.sum()
    }

    /**
     * التحقق من امتلاء المخزون
     * Check if inventory is full
     */
    fun isInventoryFull(): Boolean {
        return getInventoryItemCount() >= maxInventorySlots
    }

    /**
     * تحويل إلى سلسلة نصية للتصحيح
     * Convert to debug string
     */
    override fun toString(): String {
        return "PlayerEntity(id='$playerId', name='$playerName', level=$level, " +
                "health=$currentHealth/$maxHealth, energy=$currentEnergy/$maxEnergy, " +
                "region='$currentRegion', power=${calculatePowerLevel()})"
    }
}