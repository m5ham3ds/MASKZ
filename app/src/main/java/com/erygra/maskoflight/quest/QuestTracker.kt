package com.erygra.maskoflight.quest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * QuestTracker.kt - متتبع المهام والمرشد
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - تتبع المهام النشطة
 * - نظام الهدف الحالي
 * - التنبيهات والتذكيرات
 * - إحصائيات المتابعة
 * 
 * المكونات الرئيسية:
 * - Quest tracking
 * - Current objective management
 * - Notifications system
 * - Progress markers
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class QuestTracker {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * المهمة المتابعة حالياً
     */
    private val _trackedQuest = MutableStateFlow<String?>(null)
    val trackedQuest: StateFlow<String?> = _trackedQuest.asStateFlow()
    
    /**
     * الهدف الحالي للمهمة المتابعة
     */
    private val _currentObjective = MutableStateFlow<String?>(null)
    val currentObjective: StateFlow<String?> = _currentObjective.asStateFlow()
    
    /**
     * المهام المضافة للمتابعة
     */
    private val _markedQuests = MutableStateFlow<Set<String>>(emptySet())
    val markedQuests: StateFlow<Set<String>> = _markedQuests.asStateFlow()
    
    /**
     * تنبيهات المتابعة
     */
    private val _trackingNotifications = MutableStateFlow<List<TrackingNotification>>(emptyList())
    val trackingNotifications: StateFlow<List<TrackingNotification>> = _trackingNotifications.asStateFlow()
    
    /**
     * مؤشرات التقدم
     */
    private val _progressMarkers = MutableStateFlow<Map<String, QuestMarker>>(emptyMap())
    val progressMarkers: StateFlow<Map<String, QuestMarker>> = _progressMarkers.asStateFlow()
    
    /**
     * إحصائيات المتابعة الحالية
     */
    private val _trackerStats = MutableStateFlow(TrackerStats())
    val trackerStats: StateFlow<TrackerStats> = _trackerStats.asStateFlow()
    
    /**
     * وقت بدء المتابعة
     */
    private var trackingStartTime: Long = 0
    
    // ════════════════════════════════════════════════════════════════════════════
    // Quest Tracking
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء متابعة مهمة
     *
     * @param questId معرف المهمة
     */
    fun trackQuest(questId: String) {
        _trackedQuest.value = questId
        trackingStartTime = System.currentTimeMillis()
        
        // اختيار أول هدف غير مكتمل
        selectFirstIncompleteObjective(questId)
        
        Timber.d("Quest tracking started: $questId")
    }
    
    /**
     * إيقاف متابعة المهمة الحالية
     */
    fun stopTracking() {
        val questId = _trackedQuest.value
        _trackedQuest.value = null
        _currentObjective.value = null
        
        if (questId != null) {
            val duration = System.currentTimeMillis() - trackingStartTime
            addNotification(
                questId,
                "Tracking stopped",
                TrackingNotificationType.INFO,
                duration
            )
        }
        
        Timber.d("Quest tracking stopped")
    }
    
    /**
     * التبديل لمهمة أخرى
     *
     * @param newQuestId معرف المهمة الجديدة
     */
    fun switchTracking(newQuestId: String) {
        stopTracking()
        trackQuest(newQuestId)
    }
    
    /**
     * هل هناك مهمة متابعة
     */
    fun isTracking(): Boolean {
        return _trackedQuest.value != null
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Objective Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * اختيار أول هدف غير مكتمل
     *
     * @param questId معرف المهمة
     */
    fun selectFirstIncompleteObjective(questId: String) {
        // هذا يحتاج للوصول لنظام المهام
        // يتم تنفيذه من خلال QuestSystem
        Timber.d("First incomplete objective selected for: $questId")
    }
    
    /**
     * تعيين الهدف الحالي
     *
     * @param objectiveId معرف الهدف
     */
    fun setCurrentObjective(objectiveId: String) {
        _currentObjective.value = objectiveId
        Timber.d("Current objective set: $objectiveId")
    }
    
    /**
     * الانتقال للهدف التالي
     *
     * @param questSystem نظام المهام
     */
    fun moveToNextObjective(questSystem: QuestSystem) {
        val questId = _trackedQuest.value ?: return
        val quest = questSystem.getQuest(questId) ?: return
        val progress = questSystem.getQuestProgress(questId) ?: return
        
        // البحث عن أول هدف غير مكتمل
        val nextObjective = quest.objectives.find { objective ->
            val currentProgress = progress.objectives[objective.id] ?: 0
            currentProgress < objective.targetValue
        }
        
        if (nextObjective != null) {
            setCurrentObjective(nextObjective.id)
            addNotification(
                questId,
                "Objective updated: ${nextObjective.title}",
                TrackingNotificationType.OBJECTIVE_CHANGE
            )
        }
    }
    
    // ════════════════════════════════════════════════// Marked Quests
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة مهمة للمتابعة العلامات
     *
     * @param questId معرف المهمة
     */
    fun markQuest(questId: String) {
        _markedQuests.value = _markedQuests.value + questId
        Timber.d("Quest marked: $questId")
    }
    
    /**
     * إزالة مهمة من المتابعة العلامات
     *
     * @param questId معرف المهمة
     */
    fun unmarkQuest(questId: String) {
        _markedQuests.value = _markedQuests.value - questId
        Timber.d("Quest unmarked: $questId")
    }
    
    /**
     * التبديل على وسم المهمة
     *
     * @param questId معرف المهمة
     */
    fun toggleMarkQuest(questId: String) {
        if (_markedQuests.value.contains(questId)) {
            unmarkQuest(questId)
        } else {
            markQuest(questId)
        }
    }
    
    /**
     * هل المهمة موسومة
     *
     * @param questId معرف المهمة
     */
    fun isQuestMarked(questId: String): Boolean {
        return _markedQuests.value.contains(questId)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Notifications
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة تنبيه
     *
     * @param questId معرف المهمة
     * @param message الرسالة
     * @param type نوع التنبيه
     * @param duration المدة (ميلي ثانية)
     */
    fun addNotification(
        questId: String,
        message: String,
        type: TrackingNotificationType,
        duration: Long = 0L
    ) {
        val notification = TrackingNotification(
            questId = questId,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            duration = duration
        )
        
        _trackingNotifications.value = _trackingNotifications.value + notification
        
        // الاحتفاظ بآخر 20 إخطار فقط
        if (_trackingNotifications.value.size > 20) {
            _trackingNotifications.value = _trackingNotifications.value.takeLast(20)
        }
    }
    
    /**
     * مسح جميع الإخطارات
     */
    fun clearNotifications() {
        _trackingNotifications.value = emptyList()
    }
    
    /**
     * مسح إخطارات مهمة محددة
     *
     * @param questId معرف المهمة
     */
    fun clearQuestNotifications(questId: String) {
        _trackingNotifications.value = _trackingNotifications.value.filter {
            it.questId != questId
        }
    }
    
    /**
     * الحصول على آخر إخطار
     */
    fun getLatestNotification(): TrackingNotification? {
        return _trackingNotifications.value.lastOrNull()
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Progress Markers
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة مؤشر تقدم
     *
     * @param questId معرف المهمة
     * @param marker المؤشر
     */
    fun addMarker(questId: String, marker: QuestMarker) {
        _progressMarkers.value = _progressMarkers.value + (questId to marker)
        Timber.d("Marker added for quest: $questId")
    }
    
    /**
     * إزالة مؤشر تقدم
     *
     * @param questId معرف المهمة
     */
    fun removeMarker(questId: String) {
        _progressMarkers.value = _progressMarkers.value - questId
        Timber.d("Marker removed for quest: $questId")
    }
    
    /**
     * الحصول على مؤشر تقدم
     *
     * @param questId معرف المهمة
     */
    fun getMarker(questId: String): QuestMarker? {
        return _progressMarkers.value[questId]
    }
    
    /**
     * تحديث مؤشر التقدم
     *
     * @param questId معرف المهمة
     * @param progress التقدم (0-100)
     */
    fun updateMarkerProgress(questId: String, progress: Float) {
        val marker = _progressMarkers.value[questId]?.copy(
            progress = progress.coerceIn(0f, 100f)
        ) ?: return
        
        _progressMarkers.value = _progressMarkers.value + (questId to marker)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Statistics
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تحديث إحصائيات المتابعة
     *
     * @param questSystem نظام المهام
     */
    fun updateStats(questSystem: QuestSystem) {
        val stats = questSystem.getQuestStats()
        val trackedQuestId = _trackedQuest.value
        var trackedQuestProgress = 0f
        
        if (trackedQuestId != null) {
            trackedQuestProgress = questSystem.getQuestCompletion(trackedQuestId)
        }
        
        _trackerStats.value = TrackerStats(
            totalActiveQuests = stats.activeQuests,
            totalMarkedQuests = _markedQuests.value.size,
            currentTrackedQuest = trackedQuestId,
            trackedQuestProgress = trackedQuestProgress,
            trackedQuestDuration = if (_trackedQuest.value != null) {
                System.currentTimeMillis() - trackingStartTime
            } else {
                0L
            },
            totalNotifications = _trackingNotifications.value.size
        )
    }
    
    /**
     * الحصول على إحصائيات المتابعة الحالية
     */
    fun getStats(): TrackerStats {
        return _trackerStats.value
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save/Load
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ حالة المتتبع
     */
    fun saveState(): TrackerState {
        return TrackerState(
            trackedQuest = _trackedQuest.value,
            currentObjective = _currentObjective.value,
            markedQuests = _markedQuests.value,
            progressMarkers = _progressMarkers.value
        )
    }
    
    /**
     * استعادة حالة المتتبع
     *
     * @param state الحالة المحفوظة
     */
    fun loadState(state: TrackerState) {
        _trackedQuest.value = state.trackedQuest
        _currentObjective.value = state.currentObjective
        _markedQuests.value = state.markedQuests
        _progressMarkers.value = state.progressMarkers
        
        if (state.trackedQuest != null) {
            trackingStartTime = System.currentTimeMillis()
        }
        
        Timber.d("TrackerState loaded")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نوع التنبيه
 */
enum class TrackingNotificationType {
    OBJECTIVE_PROGRESS,      // تقدم الهدف
    OBJECTIVE_COMPLETE,      // إتمام الهدف
    OBJECTIVE_CHANGE,        // تغيير الهدف
    QUEST_PROGRESS,          // تقدم المهمة
    QUEST_READY,             // المهمة جاهزة للإكمال
    MILESTONE,               // معلم
    WARNING,                 // تحذير
    INFO                     // معلومة
}

/**
 * تنبيه المتابعة
 *
 * @property questId معرف المهمة
 * @property message الرسالة
 * @property type النوع
 * @property timestamp الطابع الزمني
 * @property duration المدة
 */
data class TrackingNotification(
    val questId: String,
    val message: String,
    val type: TrackingNotificationType,
    val timestamp: Long,
    val duration: Long = 0L
)

/**
 * مؤشر الهدف
 *
 * @property questId معرف المهمة
 * @property locationX موقع X
 * @property locationY موقع Y
 * @property progress التقدم (0-100)
 * @property active نشط أم لا
 */
data class QuestMarker(
    val questId: String,
    val locationX: Float,
    val locationY: Float,
    val progress: Float = 0f,
    val active: Boolean = true
)

/**
 * إحصائيات المتتبع
 *
 * @property totalActiveQuests إجمالي المهام النشطة
 * @property totalMarkedQuests إجمالي المهام الموسومة
 * @property currentTrackedQuest المهمة المتابعة حالياً
 * @property trackedQuestProgress تقدم المهمة المتابعة
 * @property trackedQuestDuration مدة المتابعة
 * @property totalNotifications إجمالي الإخطارات
 */
data class TrackerStats(
    val totalActiveQuests: Int = 0,
    val totalMarkedQuests: Int = 0,
    val currentTrackedQuest: String? = null,
    val trackedQuestProgress: Float = 0f,
    val trackedQuestDuration: Long = 0L,
    val totalNotifications: Int = 0
)

/**
 * حالة المتتبع (للحفظ)
 */
data class TrackerState(
    val trackedQuest: String?,
    val currentObjective: String?,
    val markedQuests: Set<String>,
    val progressMarkers: Map<String, QuestMarker>
)