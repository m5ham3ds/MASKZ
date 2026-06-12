package com.erygra.maskoflight.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * QuestEntity - كيان بيانات المهمة
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * كيان Room لحفظ بيانات المهام في قاعدة البيانات
 * 
 * Room entity for persisting quest data in database
 * 
 * أنواع المهام / Quest Types:
 * - main: مهمة رئيسية (Main quest)
 * - side: مهمة جانبية (Side quest)
 * - daily: مهمة يومية (Daily quest)
 * - weekly: مهمة أسبوعية (Weekly quest)
 * - event: مهمة حدث (Event quest)
 * - tutorial: مهمة تعليمية (Tutorial quest)
 * - bounty: مهمة مكافأة (Bounty quest)
 * 
 * حالات المهمة / Quest Status:
 * - locked: مقفلة (Locked)
 * - available: متاحة (Available)
 * - active: نشطة (Active)
 * - completed: مكتملة (Completed)
 * - failed: فاشلة (Failed)
 * - expired: منتهية (Expired)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Entity(
    tableName = "quests",
    indices = [
        Index(value = ["questId"], unique = true),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["region"]),
        Index(value = ["priority"])
    ]
)
data class QuestEntity(
    // ═══════════════════════════════════════════════════════════════════════
    // Primary Key & Basic Info
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * معرف المهمة الفريد
     * Unique quest identifier
     */
    @PrimaryKey
    @ColumnInfo(name = "questId")
    val questId: String,

    /**
     * الاسم بالعربية
     * Name in Arabic
     */
    @ColumnInfo(name = "nameAr")
    val nameAr: String,

    /**
     * الاسم بالإنجليزية
     * Name in English
     */
    @ColumnInfo(name = "nameEn")
    val nameEn: String,

    /**
     * الوصف بالعربية
     * Description in Arabic
     */
    @ColumnInfo(name = "descriptionAr")
    val descriptionAr: String,

    /**
     * الوصف بالإنجليزية
     * Description in English
     */
    @ColumnInfo(name = "descriptionEn")
    val descriptionEn: String,

    /**
     * نوع المهمة
     * Quest type
     */
    @ColumnInfo(name = "type")
    val type: String,

    /**
     * حالة المهمة
     * Quest status
     */
    @ColumnInfo(name = "status")
    val status: String = "locked",

    // ═══════════════════════════════════════════════════════════════════════
    // Requirements - المتطلبات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المتطلبات (key -> value)
     * Requirements (key -> value)
     * 
     * أمثلة / Examples:
     * - "minLevel" -> 5
     * - "requiredQuests" -> ["quest_001", "quest_002"]
     * - "requiredItems" -> ["item_key_001"]
     * - "requiredRegion" -> "forgotten_valley"
     */
    @ColumnInfo(name = "requirements")
    val requirements: Map<String, Any> = emptyMap(),

    /**
     * المستوى الأدنى المطلوب
     * Minimum required level
     */
    @ColumnInfo(name = "minLevel")
    val minLevel: Int = 1,

    /**
     * المستوى الأقصى المطلوب (للمهام ذات النطاق)
     * Maximum level (for level-ranged quests)
     */
    @ColumnInfo(name = "maxLevel")
    val maxLevel: Int? = null,

    /**
     * المهام المطلوب إكمالها أولاً
     * Required prerequisite quests
     */
    @ColumnInfo(name = "prerequisiteQuests")
    val prerequisiteQuests: List<String> = emptyList(),

    /**
     * العناصر المطلوبة للبدء
     * Required items to start
     */
    @ColumnInfo(name = "requiredItems")
    val requiredItems: Map<String, Int> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Objectives - الأهداف
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * قائمة الأهداف
     * List of objectives
     * 
     * كل هدف هو Map يحتوي على:
     * Each objective is a Map containing:
     * - type: نوع الهدف (kill, collect, reach, talk, etc.)
     * - target: الهدف المحدد
     * - required: العدد المطلوب
     * - progress: التقدم الحالي
     * - description: وصف الهدف
     */
    @ColumnInfo(name = "objectives")
    val objectives: List<Map<String, Any>> = emptyList(),

    /**
     * أهداف اختيارية (إضافية)
     * Optional objectives
     */
    @ColumnInfo(name = "optionalObjectives")
    val optionalObjectives: List<Map<String, Any>> = emptyList(),

    /**
     * أهداف سرية (مخفية)
     * Secret objectives
     */
    @ColumnInfo(name = "secretObjectives")
    val secretObjectives: List<Map<String, Any>> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Rewards - المكافآت
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المكافآت (key -> value)
     * Rewards (key -> value)
     * 
     * أمثلة / Examples:
     * - "xp" -> 500
     * - "gold" -> 100
     * - "items" -> ["item_001", "item_002"]
     * - "skillPoints" -> 1
     */
    @ColumnInfo(name = "rewards")
    val rewards: Map<String, Any> = emptyMap(),

    /**
     * المكافآت الاختيارية (يختار اللاعب واحدة)
     * Optional rewards (player chooses one)
     */
    @ColumnInfo(name = "optionalRewards")
    val optionalRewards: List<Map<String, Any>> = emptyList(),

    /**
     * مكافآت إضافية للأهداف الاختيارية
     * Bonus rewards for optional objectives
     */
    @ColumnInfo(name = "bonusRewards")
    val bonusRewards: Map<String, Any> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Location & Region - الموقع والمنطقة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * المنطقة المرتبطة بالمهمة
     * Quest region
     */
    @ColumnInfo(name = "region")
    val region: String,

    /**
     * نقطة البداية (X, Y)
     * Start location
     */
    @ColumnInfo(name = "startLocation")
    val startLocation: Map<String, Float> = emptyMap(),

    /**
     * مانح المهمة (NPC)
     * Quest giver NPC
     */
    @ColumnInfo(name = "questGiver")
    val questGiver: String? = null,

    /**
     * موقع مانح المهمة
     * Quest giver location
     */
    @ColumnInfo(name = "questGiverLocation")
    val questGiverLocation: Map<String, Float> = emptyMap(),

    /**
     * نقطة التسليم
     * Turn-in location
     */
    @ColumnInfo(name = "turnInLocation")
    val turnInLocation: Map<String, Float> = emptyMap(),

    /**
     * مستقبل المهمة (NPC)
     * Quest receiver NPC
     */
    @ColumnInfo(name = "questReceiver")
    val questReceiver: String? = null,

    // ═══════════════════════════════════════════════════════════════════════
    // Progress & Tracking - التقدم والتتبع
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * التقدم الإجمالي (0-100)
     * Overall progress (0-100)
     */
    @ColumnInfo(name = "progress")
    val progress: Int = 0,

    /**
     * الأهداف المكتملة
     * Completed objectives
     */
    @ColumnInfo(name = "completedObjectives")
    val completedObjectives: List<String> = emptyList(),

    /**
     * بيانات التقدم التفصيلية
     * Detailed progress data
     */
    @ColumnInfo(name = "progressData")
    val progressData: Map<String, Int> = emptyMap(),

    /**
     * المحاولات المتبقية (للمهام القابلة للفشل)
     * Remaining attempts (for fail-able quests)
     */
    @ColumnInfo(name = "attemptsRemaining")
    val attemptsRemaining: Int? = null,

    // ═══════════════════════════════════════════════════════════════════════
    // Time & Expiration - الوقت والانتهاء
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * وقت قبول المهمة
     * Quest accepted time
     */
    @ColumnInfo(name = "acceptedAt")
    val acceptedAt: Long? = null,

    /**
     * وقت إكمال المهمة
     * Quest completed time
     */
    @ColumnInfo(name = "completedAt")
    val completedAt: Long? = null,

    /**
     * وقت انتهاء المهمة (للمهام المحددة بوقت)
     * Quest expiration time (for timed quests)
     */
    @ColumnInfo(name = "expiresAt")
    val expiresAt: Long? = null,

    /**
     * مدة المهمة (بالثواني)
     * Quest duration (seconds)
     */
    @ColumnInfo(name = "duration")
    val duration: Int? = null,

    /**
     * الوقت المتبقي
     * Remaining time
     */
    @ColumnInfo(name = "timeRemaining")
    val timeRemaining: Int? = null,

    // ═══════════════════════════════════════════════════════════════════════
    // Quest Chain - سلسلة المهام
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * معرف السلسلة (إن وجدت)
     * Chain ID (if part of a chain)
     */
    @ColumnInfo(name = "chainId")
    val chainId: String? = null,

    /**
     * ترتيب المهمة في السلسلة
     * Order in chain
     */
    @ColumnInfo(name = "chainOrder")
    val chainOrder: Int? = null,

    /**
     * المهمة التالية في السلسلة
     * Next quest in chain
     */
    @ColumnInfo(name = "nextQuest")
    val nextQuest: String? = null,

    /**
     * المهمة السابقة في السلسلة
     * Previous quest in chain
     */
    @ColumnInfo(name = "previousQuest")
    val previousQuest: String? = null,

    // ═══════════════════════════════════════════════════════════════════════
    // Properties - الخصائص
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الأولوية (للترتيب في القائمة)
     * Priority (for list sorting)
     */
    @ColumnInfo(name = "priority")
    val priority: Int = 0,

    /**
     * هل المهمة قابلة للتكرار؟
     * Is quest repeatable?
     */
    @ColumnInfo(name = "isRepeatable")
    val isRepeatable: Boolean = false,

    /**
     * عدد مرات التكرار (للمهام القابلة للتكرار)
     * Repeat count (for repeatable quests)
     */
    @ColumnInfo(name = "repeatCount")
    val repeatCount: Int = 0,

    /**
     * الحد الأقصى للتكرار
     * Maximum repeats
     */
    @ColumnInfo(name = "maxRepeats")
    val maxRepeats: Int? = null,

    /**
     * هل المهمة قابلة للفشل؟
     * Is quest fail-able?
     */
    @ColumnInfo(name = "canFail")
    val canFail: Boolean = false,

    /**
     * هل المهمة مخفية؟
     * Is quest hidden?
     */
    @ColumnInfo(name = "isHidden")
    val isHidden: Boolean = false,

    /**
     * هل المهمة محددة بوقت؟
     * Is quest timed?
     */
    @ColumnInfo(name = "isTimed")
    val isTimed: Boolean = false,

    /**
     * هل يمكن التخلي عن المهمة؟
     * Can abandon quest?
     */
    @ColumnInfo(name = "canAbandon")
    val canAbandon: Boolean = true,

    /**
     * هل يمكن مشاركة المهمة؟
     * Is quest shareable?
     */
    @ColumnInfo(name = "isShareable")
    val isShareable: Boolean = false,

    // ═══════════════════════════════════════════════════════════════════════
    // Dialogue & Story - الحوار والقصة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * معرف حوار البداية
     * Start dialogue ID
     */
    @ColumnInfo(name = "startDialogue")
    val startDialogue: String? = null,

    /**
     * معرف حوار التقدم
     * Progress dialogue ID
     */
    @ColumnInfo(name = "progressDialogue")
    val progressDialogue: String? = null,

    /**
     * معرف حوار الإكمال
     * Completion dialogue ID
     */
    @ColumnInfo(name = "completionDialogue")
    val completionDialogue: String? = null,

    /**
     * معرف حوار الفشل
     * Failure dialogue ID
     */
    @ColumnInfo(name = "failureDialogue")
    val failureDialogue: String? = null,

    // ═══════════════════════════════════════════════════════════════════════
    // Additional Data - بيانات إضافية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الأيقونة
     * Icon identifier
     */
    @ColumnInfo(name = "icon")
    val icon: String? = null,

    /**
     * العلامات (للتصنيف والبحث)
     * Tags (for categorization and search)
     */
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

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
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * التحقق من إمكانية قبول المهمة
     * Check if quest can be accepted
     * 
     * @param playerLevel مستوى اللاعب / Player level
     * @param completedQuests المهام المكتملة / Completed quests
     * @return true إذا كانت متاحة / true if available
     */
    fun canAccept(playerLevel: Int, completedQuests: List<String>): Boolean {
        // فحص المستوى
        // Check level
        if (playerLevel < minLevel) return false
        if (maxLevel != null && playerLevel > maxLevel) return false

        // فحص المهام المطلوبة
        // Check prerequisite quests
        if (!prerequisiteQuests.all { it in completedQuests }) return false

        // فحص الحالة
        // Check status
        return status == "available"
    }

    /**
     * التحقق من اكتمال المهمة
     * Check if quest is completed
     */
    fun isCompleted(): Boolean {
        return status == "completed"
    }

    /**
     * التحقق من نشاط المهمة
     * Check if quest is active
     */
    fun isActive(): Boolean {
        return status == "active"
    }

    /**
     * التحقق من فشل المهمة
     * Check if quest is failed
     */
    fun isFailed(): Boolean {
        return status == "failed"
    }

    /**
     * التحقق من انتهاء المهمة
     * Check if quest is expired
     */
    fun isExpired(): Boolean {
        if (!isTimed || expiresAt == null) return false
        return System.currentTimeMillis() > expiresAt
    }

    /**
     * الحصول على نسبة التقدم (0-100)
     * Get progress percentage (0-100)
     */
    fun getProgressPercentage(): Int {
        return progress.coerceIn(0, 100)
    }

    /**
     * الحصول على عدد الأهداف المكتملة
     * Get completed objectives count
     */
    fun getCompletedObjectivesCount(): Int {
        return completedObjectives.size
    }

    /**
     * الحصول على عدد الأهداف الكلي
     * Get total objectives count
     */
    fun getTotalObjectivesCount(): Int {
        return objectives.size
    }

    /**
     * التحقق من اكتمال جميع الأهداف
     * Check if all objectives are completed
     */
    fun areAllObjectivesCompleted(): Boolean {
        return getCompletedObjectivesCount() == getTotalObjectivesCount()
    }

    /**
     * الحصول على الاسم حسب اللغة
     * Get name by language
     */
    fun getName(language: String = "ar"): String {
        return if (language == "ar") nameAr else nameEn
    }

    /**
     * الحصول على الوصف حسب اللغة
     * Get description by language
     */
    fun getDescription(language: String = "ar"): String {
        return if (language == "ar") descriptionAr else descriptionEn
    }

    /**
     * تحويل إلى سلسلة نصية للتصحيح
     * Convert to debug string
     */
    override fun toString(): String {
        return "QuestEntity(id='$questId', name='$nameAr', type='$type', " +
                "status='$status', progress=$progress%, objectives=${getCompletedObjectivesCount()}/${getTotalObjectivesCount()})"
    }
}