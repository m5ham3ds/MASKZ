package com.erygra.maskoflight.save

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.math.abs

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * AutoSaveSystem.kt - نظام الحفظ التلقائي
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - حفظ تلقائي بناءً على الوقت
 * - حفظ عند أحداث محددة
 * - إدارة الحفوظات التلقائية
 * - تحكم ذكي بتكرار الحفظ
 * 
 * المكونات الرئيسية:
 * - Timer-based auto-save
 * - Event-triggered saves
 * - Smart save management
 * - Save history
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class AutoSaveSystem(
    private val context: Context,
    private val saveManager: SaveManager
) {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * هل نظام الحفظ التلقائي فعال
     */
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    /**
     * حالة الحفظ التلقائي الحالية
     */
    private val _autoSaveStatus = MutableStateFlow<AutoSaveStatus>(AutoSaveStatus.IDLE)
    val autoSaveStatus: StateFlow<AutoSaveStatus> = _autoSaveStatus.asStateFlow()
    
    /**
     * عدد الحفوظات التلقائية
     */
    private val _autoSaveCount = MutableStateFlow(0)
    val autoSaveCount: StateFlow<Int> = _autoSaveCount.asStateFlow()
    
    /**
     * آخر وقت حفظ تلقائي
     */
    private val _lastAutoSaveTime = MutableStateFlow<Long?>(null)
    val lastAutoSaveTime: StateFlow<Long?> = _lastAutoSaveTime.asStateFlow()
    
    /**
     * سجل الحفوظات التلقائية
     */
    private val _saveHistory = MutableStateFlow<List<AutoSaveRecord>>(emptyList())
    val saveHistory: StateFlow<List<AutoSaveRecord>> = _saveHistory.asStateFlow()
    
    /**
     * Coroutine scope
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Job للحفظ المجدول
     */
    private var autoSaveJob: Job? = null
    
    // ════════════════════════════════════════════════════════════════════════════
    // Configuration
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الفترة الزمنية للحفظ التلقائي (ميلي ثانية)
     */
    private var autoSaveInterval = DEFAULT_AUTO_SAVE_INTERVAL
    
    /**
     * الحد الأدنى من التغيير المطلوب للحفظ
     */
    private var minChangeThreshold = DEFAULT_MIN_CHANGE_THRESHOLD
    
    /**
     * آخر بيانات محفوظة
     */
    private var lastSavedData: SaveData? = null
    
    /**
     * عدد الحفوظات التلقائية المتزامنة
     */
    private var maxAutoSaves = DEFAULT_MAX_AUTO_SAVES
    
    // ════════════════════════════════════════════════════════════════════════════
    // Initialization
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء نظام الحفظ التلقائي
     */
    fun start() {
        if (_isEnabled.value) {
            Timber.d("AutoSaveSystem started")
            startAutoSaveTimer()
        }
    }
    
    /**
     * إيقاف نظام الحفظ التلقائي
     */
    fun stop() {
        autoSaveJob?.cancel()
        Timber.d("AutoSaveSystem stopped")
    }
    
    /**
     * تفعيل/تعطيل النظام
     *
     * @param enabled تفعيل
     */
    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        if (enabled) {
            start()
        } else {
            stop()
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Configuration Methods
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تعيين الفترة الزمنية للحفظ التلقائي
     *
     * @param interval الفترة (ميلي ثانية)
     */
    fun setAutoSaveInterval(interval: Long) {
        require(interval > 0) { "Interval must be positive" }
        autoSaveInterval = interval
        
        // إعادة تشغيل المؤقت
        if (_isEnabled.value) {
            stop()
            start()
        }
    }
    
    /**
     * تعيين الحد الأدنى للتغيير المطلوب
     *
     * @param threshold الحد الأدنى
     */
    fun setMinChangeThreshold(threshold: Int) {
        minChangeThreshold = threshold
    }
    
    /**
     * تعيين الحد الأقصى للحفوظات التلقائية
     *
     * @param maxSaves الحد الأقصى
     */
    fun setMaxAutoSaves(maxSaves: Int) {
        maxAutoSaves = maxSaves
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Auto Save Logic
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء المؤقت للحفظ التلقائي
     */
    private fun startAutoSaveTimer() {
        autoSaveJob = scope.launch {
            while (isActive) {
                delay(autoSaveInterval)
                
                if (_isEnabled.value) {
                    // يتم استدعاء الحفظ من خلال performAutoSave()
                }
            }
        }
    }
    
    /**
     * تنفيذ الحفظ التلقائي
     *
     * @param currentData البيانات الحالية
     * @param reason سبب الحفظ
     */
    suspend fun performAutoSave(
        currentData: SaveData,
        reason: AutoSaveReason = AutoSaveReason.TIMER
    ): Boolean {
        if (!_isEnabled.value) return false
        
        // التحقق من وجود تغييرات كافية
        if (!hasSignificantChanges(currentData)) {
            Timber.d("No significant changes, skipping auto-save")
            return false
        }
        
        _autoSaveStatus.value = AutoSaveStatus.SAVING
        
        return try {
            // البحث عن خانة حفظ تلقائية
            val autoSaveSlot = findAutoSaveSlot()
            
            if (autoSaveSlot >= 0) {
                val success = saveManager.saveGame(autoSaveSlot, currentData)
                
                if (success) {
                    lastSavedData = currentData
                    _lastAutoSaveTime.value = System.currentTimeMillis()
                    _autoSaveCount.value += 1
                    
                    // إضافة لسجل الحفوظات
                    addToHistory(AutoSaveRecord(
                        timestamp = System.currentTimeMillis(),
                        slot = autoSaveSlot,
                        reason = reason,
                        playerLevel = currentData.playerLevel,
                        location = currentData.lastLocation
                    ))
                    
                    _autoSaveStatus.value = AutoSaveStatus.SUCCESS
                    Timber.d("Auto-save successful (reason: $reason)")
                } else {
                    _autoSaveStatus.value = AutoSaveStatus.FAILED
                    Timber.e("Auto-save failed")
                }
                
                return success
            } else {
                Timber.w("No auto-save slot available")
                _autoSaveStatus.value = AutoSaveStatus.FAILED
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in auto-save")
            _autoSaveStatus.value = AutoSaveStatus.FAILED
            false
        }
    }
    
    /**
     * التحقق من وجود تغييرات كافية
     *
     * @param currentData البيانات الحالية
     */
    private fun hasSignificantChanges(currentData: SaveData): Boolean {
        if (lastSavedData == null) return true
        
        val lastData = lastSavedData ?: return true
        
        // حساب عدد التغييرات
        var changes = 0
        
        // تغييرات الإحصائيات
        if (lastData.statistics.totalKills != currentData.statistics.totalKills) changes++
        if (lastData.statistics.totalDeaths != currentData.statistics.totalDeaths) changes++
        if (lastData.statistics.secretsFound != currentData.statistics.secretsFound) changes++
        
        // تغييرات المهام
        if (lastData.questData.completedQuests.size != currentData.questData.completedQuests.size) {
            changes += abs(
                lastData.questData.completedQuests.size - 
                currentData.questData.completedQuests.size
            )
        }
        
        // تغييرات الحقيبة
        if (lastData.inventory.items.size != currentData.inventory.items.size) {
            changes += abs(lastData.inventory.items.size - currentData.inventory.items.size)
        }
        
        // تغييرات الموقع
        if (lastData.lastLocation != currentData.lastLocation) changes++
        
        return changes >= minChangeThreshold
    }
    
    /**
     * البحث عن خانة حفظ تلقائية
     */
    private fun findAutoSaveSlot(): Int {
        // استخدام خانة مخصصة للحفظ التلقائي (إن وجدت)
        // أو إعادة استخدام أقدم خانة حفظ
        return 0 // استخدام الخانة الأولى للحفظ التلقائي
    }
    
    /**
     * إضافة سجل للحفوظات التلقائية
     *
     * @param record السجل
     */
    private fun addToHistory(record: AutoSaveRecord) {
        var history = _saveHistory.value.toMutableList()
        history.add(record)
        
        // الاحتفاظ بآخر N حفظ
        if (history.size > maxAutoSaves) {
            history = history.takeLast(maxAutoSaves).toMutableList()
        }
        
        _saveHistory.value = history
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Statistics and History
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إحصائيات الحفظ التلقائي
     */
    fun getStatistics(): AutoSaveStatistics {
        val history = _saveHistory.value
        
        val totalAutoSaves = history.size
        val averageSaveInterval = if (history.size > 1) {
            val timeDiffs = history.zipWithNext { a, b -> b.timestamp - a.timestamp }
            timeDiffs.average().toLong()
        } else {
            0L
        }
        
        val byReason = history.groupingBy { it.reason }.eachCount()
        
        return AutoSaveStatistics(
            totalAutoSaves = totalAutoSaves,
            lastAutoSaveTime = _lastAutoSaveTime.value,
            averageSaveInterval = averageSaveInterval,
            savesByReason = byReason
        )
    }
    
    /**
     * مسح السجل
     */
    fun clearHistory() {
        _saveHistory.value = emptyList()
        _autoSaveCount.value = 0
        Timber.d("Auto-save history cleared")
    }
    
    /**
     * الحصول على آخر حفظ تلقائي
     */
    fun getLatestAutoSave(): AutoSaveRecord? {
        return _saveHistory.value.lastOrNull()
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Cleanup
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stop()
        scope.cancel()
        Timber.d("AutoSaveSystem cleaned up")
    }
    
    companion object {
        /**
         * الفترة الافتراضية للحفظ التلقائي (5 دقائق)
         */
        const val DEFAULT_AUTO_SAVE_INTERVAL = 5 * 60 * 1000L
        
        /**
         * الحد الأدنى الافتراضي للتغيير (3 تغييرات)
         */
        const val DEFAULT_MIN_CHANGE_THRESHOLD = 3
        
        /**
         * الحد الأقصى الافتراضي للحفوظات التلقائية (10)
         */
        const val DEFAULT_MAX_AUTO_SAVES = 10
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Enums and Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * حالة الحفظ التلقائي
 */
enum class AutoSaveStatus {
    IDLE,
    SAVING,
    SUCCESS,
    FAILED
}

/**
 * سبب الحفظ التلقائي
 */
enum class AutoSaveReason {
    TIMER,              // بناءً على المؤقت
    LEVEL_UP,           // رفع مستوى
    BOSS_DEFEATED,      // هزيمة زعيم
    LOCATION_CHANGE,    // تغيير الموقع
    QUEST_COMPLETE,     // إكمال مهمة
    MANUAL              // يدوي
}

/**
 * سجل الحفظ التلقائي
 *
 * @property timestamp وقت الحفظ
 * @property slot خانة الحفظ
 * @property reason السبب
 * @property playerLevel مستوى اللاعب
 * @property location الموقع
 */
data class AutoSaveRecord(
    val timestamp: Long,
    val slot: Int,
    val reason: AutoSaveReason,
    val playerLevel: Int,
    val location: String
)

/**
 * إحصائيات الحفظ التلقائي
 *
 * @property totalAutoSaves إجمالي الحفوظات التلقائية
 * @property lastAutoSaveTime آخر وقت حفظ تلقائي
 * @property averageSaveInterval متوسط الفترة بين الحفوظات
 * @property savesByReason الحفوظات حسب السبب
 */
data class AutoSaveStatistics(
    val totalAutoSaves: Int,
    val lastAutoSaveTime: Long?,
    val averageSaveInterval: Long,
    val savesByReason: Map<AutoSaveReason, Int>
)