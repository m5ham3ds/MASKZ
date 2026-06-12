package com.erygra.maskoflight.quest

import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * QuestDatabase.kt - قاعدة بيانات المهام
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - مجموعة شاملة من المهام المعرفة
 * - نظام تحميل المهام
 * - إدارة نسخ المهام
 * 
 * المكونات الرئيسية:
 * - Quest definitions
 * - Quest chains
 * - Quest categories
 * - Dynamic quest loading
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class QuestDatabase {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * خريطة المهام
     */
    private val quests = mutableMapOf<String, QuestNode>()
    
    /**
     * سلاسل المهام
     */
    private val questChains = mutableMapOf<String, List<String>>()
    
    /**
     * المهام حسب الفئة
     */
    private val questsByCategory = mutableMapOf<String, MutableList<String>>()
    
    // ════════════════════════════════════════════════════════════════════════════
    // Registration
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل مهمة جديدة
     *
     * @param quest المهمة
     */
    fun registerQuest(quest: QuestNode) {
        quests[quest.id] = quest
        
        // تسجيل حسب الفئة
        questsByCategory.getOrPut(quest.category) { mutableListOf() }.add(quest.id)
        
        // تسجيل السلسلة
        if (quest.chainId != null) {
            questChains.getOrPut(quest.chainId) { emptyList() }
        }
        
        Timber.d("Quest registered: ${quest.id}")
    }
    
    /**
     * تسجيل عدة مهام
     *
     * @param questList قائمة المهام
     */
    fun registerQuests(questList: List<QuestNode>) {
        questList.forEach { registerQuest(it) }
        Timber.d("${questList.size} quests registered")
    }
    
    /**
     * إلغاء تسجيل مهمة
     *
     * @param questId معرف المهمة
     */
    fun unregisterQuest(questId: String) {
        val quest = quests.remove(questId) ?: return
        
        questsByCategory[quest.category]?.remove(questId)
        Timber.d("Quest unregistered: $questId")
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Quest Query
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على مهمة بمعرفها
     *
     * @param questId معرف المهمة
     */
    fun getQuest(questId: String): QuestNode? {
        return quests[questId]
    }
    
    /**
     * الحصول على جميع المهام
     */
    fun getAllQuests(): List<QuestNode> {
        return quests.values.toList()
    }
    
    /**
     * الحصول على المهام حسب الفئة
     *
     * @param category الفئة
     */
    fun getQuestsByCategory(category: String): List<QuestNode> {
        return questsByCategory[category]
            ?.mapNotNull { quests[it] }
            ?: emptyList()
    }
    
    /**
     * الحصول على المهام حسب النوع
     *
     * @param type النوع
     */
    fun getQuestsByType(type: QuestType): List<QuestNode> {
        return quests.values.filter { it.type == type }
    }
    
    /**
     * الحصول على المهام حسب المستوى
     *
     * @param level المستوى
     * @param tolerance التسامح
     */
    fun getQuestsByLevel(level: Int, tolerance: Int = 2): List<QuestNode> {
        return quests.values.filter {
            it.level in (level - tolerance)..(level + tolerance)
        }
    }
    
    /**
     * الحصول على المهام حسب الموقع
     *
     * @param location الموقع
     */
    fun getQuestsByLocation(location: String): List<QuestNode> {
        return quests.values.filter { it.location == location }
    }
    
    /**
     * البحث عن المهام
     *
     * @param query نص البحث
     */
    fun searchQuests(query: String): List<QuestNode> {
        val lowerQuery = query.lowercase()
        return quests.values.filter {
            it.title.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery) ||
            it.category.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * الحصول على سلسلة مهام
     *
     * @param chainId معرف السلسلة
     */
    fun getQuestChain(chainId: String): List<QuestNode> {
        return quests.values
            .filter { it.chainId == chainId }
            .sortedBy { it.chainOrder }
    }
    
    /**
     * هل هناك مهمة
     *
     * @param questId معرف المهمة
     */
    fun hasQuest(questId: String): Boolean {
        return quests.containsKey(questId)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Statistics
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إحصائيات قاعدة البيانات
     */
    fun getStatistics(): QuestDatabaseStats {
        val totalQuests = quests.size
        val mainQuests = quests.values.count { it.type == QuestType.MAIN }
        val sideQuests = quests.values.count { it.type == QuestType.SIDE }
        val dailyQuests = quests.values.count { it.type == QuestType.DAILY }
        val weeklyQuests = quests.values.count { it.type == QuestType.WEEKLY }
        val chainQuests = quests.values.count { it.chainId != null }
        val optionalQuests = quests.values.count { it.optional }
        
        val categories = questsByCategory.keys.toList()
        val levelRange = if (quests.isNotEmpty()) {
            val levels = quests.values.map { it.level }
            levels.minOrNull() to levels.maxOrNull()
        } else {
            null to null
        }
        
        val totalChains = questChains.size
        val averageObjectivesPerQuest = if (quests.isNotEmpty()) {
            quests.values.sumOf { it.objectives.size } / quests.size.toFloat()
        } else {
            0f
        }
        
        return QuestDatabaseStats(
            totalQuests = totalQuests,
            mainQuests = mainQuests,
            sideQuests = sideQuests,
            dailyQuests = dailyQuests,
            weeklyQuests = weeklyQuests,
            chainQuests = chainQuests,
            optionalQuests = optionalQuests,
            categories = categories,
            minLevel = levelRange.first,
            maxLevel = levelRange.second,
            totalChains = totalChains,
            averageObjectivesPerQuest = averageObjectivesPerQuest
        )
    }
    
    /**
     * عدد المهام الكلي
     */
    fun getQuestCount(): Int {
        return quests.size
    }
    
    /**
     * عدد الفئات
     */
    fun getCategoryCount(): Int {
        return questsByCategory.size
    }
    
    /**
     * عدد السلاسل
     */
    fun getChainCount(): Int {
        return questChains.size
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Export/Import
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تصدير جميع المهام
     */
    fun exportAllQuests(): Map<String, QuestNode> {
        return quests.toMap()
    }
    
    /**
     * استيراد المهام
     *
     * @param questsMap خريطة المهام
     */
    fun importQuests(questsMap: Map<String, QuestNode>) {
        questsMap.forEach { (_, quest) ->
            registerQuest(quest)
        }
        Timber.d("${questsMap.size} quests imported")
    }
    
    /**
     * مسح قاعدة البيانات
     */
    fun clear() {
        quests.clear()
        questChains.clear()
        questsByCategory.clear()
        Timber.d("Quest database cleared")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * إحصائيات قاعدة بيانات المهام
 */
data class QuestDatabaseStats(
    val totalQuests: Int,
    val mainQuests: Int,
    val sideQuests: Int,
    val dailyQuests: Int,
    val weeklyQuests: Int,
    val chainQuests: Int,
    val optionalQuests: Int,
    val categories: List<String>,
    val minLevel: Int?,
    val maxLevel: Int?,
    val totalChains: Int,
    val averageObjectivesPerQuest: Float
) {
    /**
     * نسبة المهام الرئيسية
     */
    val mainQuestPercentage: Float
        get() = if (totalQuests > 0) (mainQuests / totalQuests.toFloat()) * 100 else 0f
    
    /**
     * نسبة المهام الجانبية
     */
    val sideQuestPercentage: Float
        get() = if (totalQuests > 0) (sideQuests / totalQuests.toFloat()) * 100 else 0f
}

/**
 * Factory للمهام المعروفة
 */
object KnownQuests {
    
    // سلسلة المقدمة
    val tutorialChain = listOf(
        quest {
            id("tutorial_1")
            title("Awakening")
            description("Wake up and understand your purpose")
            category("Tutorial")
            level(1)
            location("sanctuary")
            giver("Guardian")
            type(QuestType.MAIN)
            objectives(listOf(
                objective {
                    id("objective_1")
                    title("Investigate the sanctuary")
                    description("Explore your surroundings")
                    type(ObjectiveType.EXPLORE_LOCATION)
                    targetValue(1)
                    data("locationId", "sanctuary")
                }
            ))
            rewards(QuestRewards(
                experience = 100L,
                gold = 0L,
                items = listOf("starter_weapon"),
                achievements = listOf("awakening")
            ))
            chainId("tutorial_chain")
            chainOrder(1)
        },
        quest {
            id("tutorial_2")
            title("First Steps")
            description("Learn the basics of combat")
            category("Tutorial")
            level(1)
            location("training_ground")
            giver("Trainer")
            type(QuestType.MAIN)
            prerequisites(listOf("tutorial_1"))
            objectives(listOf(
                objective {
                    id("objective_2")
                    title("Defeat training dummies")
                    description("Practice your combat skills")
                    type(ObjectiveType.DEFEAT_ENEMIES)
                    targetValue(3)
                    data("enemyId", "training_dummy")
                }
            ))
            rewards(QuestRewards(
                experience = 150L,
                gold = 50L,
                items = listOf("leather_armor"),
                achievements = listOf("first_combat")
            ))
            chainId("tutorial_chain")
            chainOrder(2)
        }
    )
    
    /**
     * الحصول على جميع المهام المعروفة
     */
    fun getAllKnownQuests(): List<QuestNode> {
        return tutorialChain
    }
}