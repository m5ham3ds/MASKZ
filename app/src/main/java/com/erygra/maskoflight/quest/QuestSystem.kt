package com.erygra.maskoflight.quest

import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * QuestSystem.kt - نظام المهام الرئيسي
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة دورة حياة المهام (Create, Accept, Progress, Complete, Reward)
 * - تتبع حالة المهام للاعب
 * - إدارة سلاسل المهام (Quest Chains)
 * - نظام الأهداف والمكافآت
 * 
 * المكونات الرئيسية:
 * - Quest lifecycle management
 * - Active quests tracking
 * - Objectives progression
 * - Reward distribution
 * - Quest history
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class QuestSystem {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * جميع المهام المتاحة
     */
    private val _allQuests = MutableStateFlow<Map<String, QuestNode>>(emptyMap())
    val allQuests: StateFlow<Map<String, QuestNode>> = _allQuests.asStateFlow()
    
    /**
     * المهام النشطة
     */
    private val _activeQuests = MutableStateFlow<List<QuestNode>>(emptyList())
    val activeQuests: StateFlow<List<QuestNode>> = _activeQuests.asStateFlow()
    
    /**
     * المهام المكتملة
     */
    private val _completedQuests = MutableStateFlow<Set<String>>(emptySet())
    val completedQuests: StateFlow<Set<String>> = _completedQuests.asStateFlow()
    
    /**
     * المهام المرفوضة
     */
    private val _rejectedQuests = MutableStateFlow<Set<String>>(emptySet())
    val rejectedQuests: StateFlow<Set<String>> = _rejectedQuests.asStateFlow()
    
    /**
     * حالة المهام الحالية
     */
    private val _questsProgress = MutableStateFlow<Map<String, QuestProgress>>(emptyMap())
    val questsProgress: StateFlow<Map<String, QuestProgress>> = _questsProgress.asStateFlow()
    
    /**
     * المهام في الانتظار (Ready for completion)
     */
    private val _pendingQuests = MutableStateFlow<List<String>>(emptyList())
    val pendingQuests: StateFlow<List<String>> = _pendingQuests.asStateFlow()
    
    /**
     * إجمالي المهام المكتملة
     */
    val totalCompletedQuests: StateFlow<Int>
        get() = _completedQuests.asStateFlow().run {
            MutableStateFlow(value.size).asStateFlow()
        }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Initialization
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تهيئة نظام المهام
     *
     * @param quests خريطة المهام
     */
    fun initialize(quests: Map<String, QuestNode>) {
        _allQuests.value = quests
        Timber.d("QuestSystem initialized with ${quests.size} quests")
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Quest Acceptance
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * قبول مهمة
     *
     * @param questId معرف المهمة
     * @return true إذا تم قبولها بنجاح
     */
    fun acceptQuest(questId: String): Boolean {
        val quest = _allQuests.value[questId] ?: run {
            Timber.w("Quest not found: $questId")
            return false
        }
        
        // التحقق من الشروط
        if (!canAcceptQuest(questId)) {
            Timber.w("Cannot accept quest: $questId")
            return false
        }
        
        // إنشاء تقدم جديد
        val progress = QuestProgress(
            questId = questId,
            objectives = quest.objectives.associate { it.id to 0 },
            startedAt = System.currentTimeMillis(),
            status = QuestStatus.ACTIVE
        )
        
        // إضافة للمهام النشطة
        _activeQuests.value = _activeQuests.value + quest
        _questsProgress.value = _questsProgress.value + (questId to progress)
        
        // إرسال حدث
        EventBus.emit(GameEvent.QuestAccepted(questId))
        
        Timber.d("Quest accepted: $questId")
        return true
    }
    
    /**
     * التحقق من إمكانية قبول مهمة
     *
     * @param questId معرف المهمة
     */
    fun canAcceptQuest(questId: String): Boolean {
        val quest = _allQuests.value[questId] ?: return false
        
        // التحقق من الحالة
        if (_completedQuests.value.contains(questId)) return false
        if (_activeQuests.value.any { it.id == questId }) return false
        
        // التحقق من المتطلبات
        val requiredQuests = quest.prerequisites
        return requiredQuests.all { _completedQuests.value.contains(it) }
    }
    
    /**
     * رفض مهمة
     *
     * @param questId معرف المهمة
     */
    fun rejectQuest(questId: String): Boolean {
        if (!_activeQuests.value.any { it.id == questId }) {
            return false
        }
        
        _activeQuests.value = _activeQuests.value.filter { it.id != questId }
        _questsProgress.value = _questsProgress.value - questId
        _rejectedQuests.value = _rejectedQuests.value + questId
        
        EventBus.emit(GameEvent.QuestRejected(questId))
        Timber.d("Quest rejected: $questId")
        
        return true
    }
    
    /**
     * التخلي عن مهمة نشطة
     *
     * @param questId معرف المهمة
     */
    fun abandonQuest(questId: String): Boolean {
        if (!_activeQuests.value.any { it.id == questId }) {
            return false
        }
        
        _activeQuests.value = _activeQuests.value.filter { it.id != questId }
        _questsProgress.value = _questsProgress.value - questId
        
        EventBus.emit(GameEvent.QuestAbandoned(questId))
        Timber.d("Quest abandoned: $questId")
        
        return true
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Objective Progress
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تحديث تقدم هدف
     *
     * @param questId معرف المهمة
     * @param objectiveId معرف الهدف
     * @param progress التقدم (يضاف للقيمة الحالية)
     */
    fun updateObjectiveProgress(
        questId: String,
        objectiveId: String,
        progress: Int
    ): Boolean {
        val questProgress = _questsProgress.value[questId] ?: return false
        val currentProgress = questProgress.objectives[objectiveId] ?: return false
        
        val quest = _allQuests.value[questId] ?: return false
        val objective = quest.objectives.find { it.id == objectiveId } ?: return false
        
        val newProgress = (currentProgress + progress).coerceAtMost(objective.targetValue)
        
        // تحديث التقدم
        val updatedObjectives = questProgress.objectives.toMutableMap()
        updatedObjectives[objectiveId] = newProgress
        
        val updatedProgress = questProgress.copy(objectives = updatedObjectives)
        _questsProgress.value = _questsProgress.value + (questId to updatedProgress)
        
        // التحقق من إكمال الهدف
        if (newProgress >= objective.targetValue) {
            EventBus.emit(GameEvent.ObjectiveCompleted(questId, objectiveId))
            Timber.d("Objective completed: $questId - $objectiveId")
        } else {
            EventBus.emit(GameEvent.ObjectiveProgress(questId, objectiveId, newProgress))
        }
        
        // التحقق من إكمال جميع الأهداف
        if (areAllObjectivesCompleted(questId)) {
            markQuestAsPending(questId)
        }
        
        return true
    }
    
    /**
     * تعيين تقدم هدف مباشرة
     *
     * @param questId معرف المهمة
     * @param objectiveId معرف الهدف
     * @param value القيمة الجديدة
     */
    fun setObjectiveProgress(
        questId: String,
        objectiveId: String,
        value: Int
    ): Boolean {
        val questProgress = _questsProgress.value[questId] ?: return false
        val quest = _allQuests.value[questId] ?: return false
        val objective = quest.objectives.find { it.id == objectiveId } ?: return false
        
        val newProgress = value.coerceIn(0, objective.targetValue)
        val currentProgress = questProgress.objectives[objectiveId] ?: 0
        
        if (newProgress == currentProgress) return true
        
        // تحديث التقدم
        val updatedObjectives = questProgress.objectives.toMutableMap()
        updatedObjectives[objectiveId] = newProgress
        
        val updatedProgress = questProgress.copy(objectives = updatedObjectives)
        _questsProgress.value = _questsProgress.value + (questId to updatedProgress)
        
        EventBus.emit(GameEvent.ObjectiveProgress(questId, objectiveId, newProgress))
        
        // التحقق من إكمال جميع الأهداف
        if (areAllObjectivesCompleted(questId)) {
            markQuestAsPending(questId)
        }
        
        return true
    }
    
    /**
     * الحصول على تقدم هدف محدد
     *
     * @param questId معرف المهمة
     * @param objectiveId معرف الهدف
     */
    fun getObjectiveProgress(questId: String, objectiveId: String): Int {
        return _questsProgress.value[questId]?.objectives?.get(objectiveId) ?: 0
    }
    
    /**
     * الحصول على نسبة إكمال مهمة
     *
     * @param questId معرف المهمة
     */
    fun getQuestCompletion(questId: String): Float {
        val quest = _allQuests.value[questId] ?: return 0f
        val progress = _questsProgress.value[questId] ?: return 0f
        
        if (quest.objectives.isEmpty()) return 100f
        
        val totalObjectives = quest.objectives.size
        val completedObjectives = quest.objectives.count { objective ->
            val currentProgress = progress.objectives[objective.id] ?: 0
            currentProgress >= objective.targetValue
        }
        
        return (completedObjectives / totalObjectives.toFloat()) * 100f
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Quest Completion
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من إكمال جميع الأهداف
     *
     * @param questId معرف المهمة
     */
    private fun areAllObjectivesCompleted(questId: String): Boolean {
        val quest = _allQuests.value[questId] ?: return false
        val progress = _questsProgress.value[questId] ?: return false
        
        return quest.objectives.all { objective ->
            val currentProgress = progress.objectives[objective.id] ?: 0
            currentProgress >= objective.targetValue
        }
    }
    
    /**
     * تحديد مهمة كـ pending (جاهزة للإكمال)
     *
     * @param questId معرف المهمة
     */
    private fun markQuestAsPending(questId: String) {
        val quest = _allQuests.value[questId] ?: return
        val progress = _questsProgress.value[questId] ?: return
        
        if (progress.status != QuestStatus.PENDING) {
            val updatedProgress = progress.copy(status = QuestStatus.PENDING)
            _questsProgress.value = _questsProgress.value + (questId to updatedProgress)
            
            _pendingQuests.value = _pendingQuests.value + questId
            
            EventBus.emit(GameEvent.QuestReady(questId))
            Timber.d("Quest marked as pending: $questId")
        }
    }
    
    /**
     * إكمال مهمة
     *
     * @param questId معرف المهمة
     * @return المكافآت الممنوحة
     */
    fun completeQuest(questId: String): QuestRewards? {
        val quest = _allQuests.value[questId] ?: return null
        val progress = _questsProgress.value[questId] ?: return null
        
        // التحقق من أن جميع الأهداف مكتملة
        if (!areAllObjectivesCompleted(questId)) {
            Timber.w("Quest objectives not completed: $questId")
            return null
        }
        
        // إزالة من النشطة
        _activeQuests.value = _activeQuests.value.filter { it.id != questId }
        _pendingQuests.value = _pendingQuests.value.filter { it != questId }
        
        // إضافة للمكتملة
        _completedQuests.value = _completedQuests.value + questId
        
        // تحديث حالة التقدم
        val completedProgress = progress.copy(
            status = QuestStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        _questsProgress.value = _questsProgress.value + (questId to completedProgress)
        
        // إرسال حدث
        EventBus.emit(GameEvent.QuestCompleted(questId, quest.rewards.xp, quest.rewards.coins))
        
        Timber.d("Quest completed: $questId")
        
        return quest.rewards
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Quest Query
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على مهمة محددة
     *
     * @param questId معرف المهمة
     */
    fun getQuest(questId: String): QuestNode? {
        return _allQuests.value[questId]
    }
    
    /**
     * الحصول على تقدم مهمة محددة
     *
     * @param questId معرف المهمة
     */
    fun getQuestProgress(questId: String): QuestProgress? {
        return _questsProgress.value[questId]
    }
    
    /**
     * الحصول على المهام المتاحة
     */
    fun getAvailableQuests(): List<QuestNode> {
        return _allQuests.value.values.filter { quest ->
            canAcceptQuest(quest.id)
        }
    }
    
    /**
     * الحصول على مهام حسب الفئة
     *
     * @param category الفئة
     */
    fun getQuestsByCategory(category: String): List<QuestNode> {
        return _activeQuests.value.filter { it.category == category }
    }
    
    /**
     * الحصول على المهام حسب الموقع
     *
     * @param location الموقع
     */
    fun getQuestsByLocation(location: String): List<QuestNode> {
        return _activeQuests.value.filter { it.location == location }
    }
    
    /**
     * هل المهمة مكتملة
     *
     * @param questId معرف المهمة
     */
    fun isQuestCompleted(questId: String): Boolean {
        return _completedQuests.value.contains(questId)
    }
    
    /**
     * هل المهمة نشطة
     *
     * @param questId معرف المهمة
     */
    fun isQuestActive(questId: String): Boolean {
        return _activeQuests.value.any { it.id == questId }
    }
    
    /**
     * هل المهمة في الانتظار
     *
     * @param questId معرف المهمة
     */
    fun isQuestPending(questId: String): Boolean {
        return _pendingQuests.value.contains(questId)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Quest Chains
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على سلسلة مهام
     *
     * @param chainId معرف السلسلة
     */
    fun getQuestChain(chainId: String): List<QuestNode> {
        return _allQuests.value.values
            .filter { it.chainId == chainId }
            .sortedBy { it.chainOrder }
    }
    
    /**
     * الحصول على المهمة التالية في السلسلة
     *
     * @param questId معرف المهمة الحالية
     */
    fun getNextQuestInChain(questId: String): QuestNode? {
        val currentQuest = _allQuests.value[questId] ?: return null
        if (currentQuest.chainId == null) return null
        
        val chain = getQuestChain(currentQuest.chainId)
        val currentIndex = chain.indexOfFirst { it.id == questId }
        
        return if (currentIndex >= 0 && currentIndex < chain.size - 1) {
            chain[currentIndex + 1]
        } else {
            null
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Statistics
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إحصائيات المهام
     */
    fun getQuestStats(): QuestStats {
        val total = _allQuests.value.size
        val completed = _completedQuests.value.size
        val active = _activeQuests.value.size
        val available = getAvailableQuests().size
        
        return QuestStats(
            totalQuests = total,
            completedQuests = completed,
            activeQuests = active,
            availableQuests = available,
            completionPercentage = if (total > 0) (completed / total.toFloat()) * 100 else 0f
        )
    }
    
    /**
     * حساب إجمالي المكافآت
     *
     * @param questIds قائمة معرفات المهام
     */
    fun calculateTotalRewards(questIds: List<String>): QuestRewards {
        var totalExp = 0L
        var totalGold = 0L
        val items = mutableListOf<String>()
        val achievements = mutableListOf<String>()
        
        questIds.forEach { questId ->
            val quest = _allQuests.value[questId] ?: return@forEach
            val rewards = quest.rewards
            
            totalExp += rewards.experience
            totalGold += rewards.gold
            items.addAll(rewards.items)
            achievements.addAll(rewards.achievements)
        }
        
        return QuestRewards(
            experience = totalExp,
            gold = totalGold,
            items = items,
            achievements = achievements
        )
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save/Load
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ حالة النظام
     */
    fun saveState(): QuestSystemState {
        return QuestSystemState(
            activeQuests = _activeQuests.value.map { it.id },
            completedQuests = _completedQuests.value,
            rejectedQuests = _rejectedQuests.value,
            questsProgress = _questsProgress.value
        )
    }
    
    /**
     * استعادة حالة النظام
     *
     * @param state الحالة المحفوظة
     */
    fun loadState(state: QuestSystemState) {
        val activeQuestIds = state.activeQuests.toSet()
        val newActiveQuests = _allQuests.value.values.filter { activeQuestIds.contains(it.id) }
        
        _activeQuests.value = newActiveQuests
        _completedQuests.value = state.completedQuests
        _rejectedQuests.value = state.rejectedQuests
        _questsProgress.value = state.questsProgress
        
        Timber.d("QuestSystem state loaded")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * حالة المهمة
 */
enum class QuestStatus {
    AVAILABLE, ACTIVE, PENDING, COMPLETED, FAILED, ABANDONED
}

/**
 * تقدم المهمة
 *
 * @property questId معرف المهمة
 * @property objectives خريطة تقدم الأهداف
 * @property startedAt وقت البدء
 * @property completedAt وقت الإكمال
 * @property status حالة المهمة
 */
data class QuestProgress(
    val questId: String,
    val objectives: Map<String, Int>,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: QuestStatus = QuestStatus.ACTIVE
)

/**
 * إحصائيات المهام
 *
 * @property totalQuests إجمالي المهام
 * @property completedQuests المهام المكتملة
 * @property activeQuests المهام النشطة
 * @property availableQuests المهام المتاحة
 * @property completionPercentage نسبة الإكمال
 */
data class QuestStats(
    val totalQuests: Int,
    val completedQuests: Int,
    val activeQuests: Int,
    val availableQuests: Int,
    val completionPercentage: Float
)

/**
 * حالة نظام المهام (للحفظ)
 */
data class QuestSystemState(
    val activeQuests: List<String>,
    val completedQuests: Set<String>,
    val rejectedQuests: Set<String>,
    val questsProgress: Map<String, QuestProgress>
)