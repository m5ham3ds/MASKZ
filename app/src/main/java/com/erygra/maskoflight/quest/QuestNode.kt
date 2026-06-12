package com.erygra.maskoflight.quest

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * QuestNode.kt - عقدة المهمة (البيانات الثابتة)
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - تمثيل بيانات المهمة الثابتة
 * - المعلومات الأساسية، الأهداف، المكافآت
 * - الشروط المسبقة والسلاسل
 * 
 * المكونات الرئيسية:
 * - Quest metadata
 * - Objectives
 * - Rewards
 * - Prerequisites
 * - Quest chains
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

/**
 * عقدة المهمة (البيانات الثابتة)
 *
 * @property id معرف المهمة الفريد
 * @property title عنوان المهمة
 * @property description وصف المهمة
 * @property category فئة المهمة
 * @property level مستوى المهمة
 * @property location موقع المهمة
 * @property giver المعطي (NPC)
 * @property giverPortrait صورة المعطي
 * @property objectives أهداف المهمة
 * @property rewards المكافآت
 * @property prerequisites المتطلبات السابقة
 * @property type نوع المهمة
 * @property difficulty الصعوبة
 * @property timeLimit حد زمني (ميلي ثانية)
 * @property chainId معرف سلسلة المهام
 * @property chainOrder ترتيب المهمة في السلسلة
 * @property optional هل المهمة اختيارية
 * @property repeatable هل يمكن تكرار المهمة
 * @property failureConditions شروط الفشل
 * @property tags علامات المهمة
 * @property description_long الوصف الطويل
 */
data class QuestNode(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val level: Int,
    val location: String,
    val giver: String,
    val giverPortrait: String? = null,
    val objectives: List<QuestObjective>,
    val rewards: QuestRewards,
    val prerequisites: List<String> = emptyList(),
    val type: QuestType = QuestType.MAIN,
    val difficulty: QuestDifficulty = QuestDifficulty.NORMAL,
    val timeLimit: Long? = null,
    val chainId: String? = null,
    val chainOrder: Int = 0,
    val optional: Boolean = false,
    val repeatable: Boolean = false,
    val failureConditions: List<FailureCondition> = emptyList(),
    val tags: List<String> = emptyList(),
    val description_long: String? = null
) {
    /**
     * هل المهمة بها حد زمني
     */
    val hasTimeLimit: Boolean get() = timeLimit != null
    
    /**
     * هل المهمة مهمة رئيسية
     */
    val isMainQuest: Boolean get() = type == QuestType.MAIN
    
    /**
     * هل المهمة مهمة جانبية
     */
    val isSideQuest: Boolean get() = type == QuestType.SIDE
    
    /**
     * هل المهمة مهمة جماعية
     */
    val isGroupQuest: Boolean get() = type == QuestType.GROUP
    
    /**
     * هل المهمة مهمة مجموعة
     */
    val isChainQuest: Boolean get() = chainId != null
    
    /**
     * عدد الأهداف
     */
    val objectiveCount: Int get() = objectives.size
}

/**
 * أنواع المهام
 */
enum class QuestType {
    MAIN,      // مهمة رئيسية
    SIDE,      // مهمة جانبية
    DAILY,     // مهمة يومية
    WEEKLY,    // مهمة أسبوعية
    EVENT,     // مهمة حدث
    GUILD,     // مهمة نقابة
    GROUP,     // مهمة جماعية
    DUNGEON    // مهمة زنزانة
}

/**
 * صعوبة المهمة
 */
enum class QuestDifficulty(val multiplier: Float) {
    EASY(0.5f),
    NORMAL(1.0f),
    HARD(1.5f),
    VERY_HARD(2.0f),
    IMPOSSIBLE(3.0f)
}

/**
 * شرط الفشل
 *
 * @property id معرف الشرط
 * @property description وصف الشرط
 * @property condition دالة التحقق
 */
data class FailureCondition(
    val id: String,
    val description: String,
    val condition: () -> Boolean
)

/**
 * مكافآت المهمة
 *
 * @property experience خبرة العودة
 * @property gold ذهب العودة
 * @property items أشياء العودة
 * @property achievements إنجازات العودة
 * @property reputation سمعة العودة
 */
data class QuestRewards(
    val experience: Long = 0L,
    val gold: Long = 0L,
    val items: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
    val reputation: Map<String, Int> = emptyMap()
) {
    /**
     * هل هناك مكافآت
     */
    fun hasRewards(): Boolean {
        return experience > 0 || gold > 0 || items.isNotEmpty() || 
               achievements.isNotEmpty() || reputation.isNotEmpty()
    }
}

/**
 * Builder للمهام (لسهولة الإنشاء)
 */
class QuestBuilder {
    private var id: String = ""
    private var title: String = ""
    private var description: String = ""
    private var category: String = ""
    private var level: Int = 1
    private var location: String = ""
    private var giver: String = ""
    private var giverPortrait: String? = null
    private val objectives = mutableListOf<QuestObjective>()
    private var rewards = QuestRewards()
    private val prerequisites = mutableListOf<String>()
    private var type: QuestType = QuestType.MAIN
    private var difficulty: QuestDifficulty = QuestDifficulty.NORMAL
    private var timeLimit: Long? = null
    private var chainId: String? = null
    private var chainOrder: Int = 0
    private var optional: Boolean = false
    private var repeatable: Boolean = false
    private val failureConditions = mutableListOf<FailureCondition>()
    private val tags = mutableListOf<String>()
    private var description_long: String? = null
    
    fun id(value: String) = apply { id = value }
    fun title(value: String) = apply { title = value }
    fun description(value: String) = apply { description = value }
    fun description_long(value: String) = apply { description_long = value }
    fun category(value: String) = apply { category = value }
    fun level(value: Int) = apply { level = value }
    fun location(value: String) = apply { location = value }
    fun giver(value: String) = apply { giver = value }
    fun giverPortrait(value: String) = apply { giverPortrait = value }
    fun objective(value: QuestObjective) = apply { objectives.add(value) }
    fun objectives(value: List<QuestObjective>) = apply { objectives.addAll(value) }
    fun rewards(value: QuestRewards) = apply { rewards = value }
    fun prerequisite(value: String) = apply { prerequisites.add(value) }
    fun prerequisites(value: List<String>) = apply { prerequisites.addAll(value) }
    fun type(value: QuestType) = apply { type = value }
    fun difficulty(value: QuestDifficulty) = apply { difficulty = value }
    fun timeLimit(value: Long) = apply { timeLimit = value }
    fun chainId(value: String) = apply { chainId = value }
    fun chainOrder(value: Int) = apply { chainOrder = value }
    fun optional(value: Boolean) = apply { optional = value }
    fun repeatable(value: Boolean) = apply { repeatable = value }
    fun failureCondition(value: FailureCondition) = apply { failureConditions.add(value) }
    fun failureConditions(value: List<FailureCondition>) = apply { failureConditions.addAll(value) }
    fun tag(value: String) = apply { tags.add(value) }
    fun tags(value: List<String>) = apply { tags.addAll(value) }
    
    fun build(): QuestNode {
        require(id.isNotEmpty()) { "Quest ID is required" }
        require(title.isNotEmpty()) { "Quest title is required" }
        require(description.isNotEmpty()) { "Quest description is required" }
        require(objectives.isNotEmpty()) { "At least one objective is required" }
        
        return QuestNode(
            id = id,
            title = title,
            description = description,
            category = category,
            level = level,
            location = location,
            giver = giver,
            giverPortrait = giverPortrait,
            objectives = objectives,
            rewards = rewards,
            prerequisites = prerequisites,
            type = type,
            difficulty = difficulty,
            timeLimit = timeLimit,
            chainId = chainId,
            chainOrder = chainOrder,
            optional = optional,
            repeatable = repeatable,
            failureConditions = failureConditions,
            tags = tags,
            description_long = description_long
        )
    }
}

/**
 * Helper function للإنشاء
 */
fun quest(block: QuestBuilder.() -> Unit): QuestNode {
    return QuestBuilder().apply(block).build()
}