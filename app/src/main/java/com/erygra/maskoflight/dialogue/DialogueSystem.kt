package com.erygra.maskoflight.dialogue

import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * DialogueSystem.kt - نظام الحوار الرئيسي
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة حوارات الشخصيات (NPCs)
 * - نظام الاختيارات المتفرعة (Branching Dialogue)
 * - تتبع حالة الحوار
 * - نظام المتغيرات والشروط
 * 
 * المكونات الرئيسية:
 * - Dialogue tree management
 * - State tracking
 * - Choice handling
 * - Condition evaluation
 * - Variable system
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class DialogueSystem {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحوار الحالي
     */
    private val _currentDialogue = MutableStateFlow<DialogueNode?>(null)
    val currentDialogue: StateFlow<DialogueNode?> = _currentDialogue.asStateFlow()
    
    /**
     * الشخصية الحالية
     */
    private val _currentNPC = MutableStateFlow<String?>(null)
    val currentNPC: StateFlow<String?> = _currentNPC.asStateFlow()
    
    /**
     * الخيارات المتاحة
     */
    private val _availableChoices = MutableStateFlow<List<DialogueChoice>>(emptyList())
    val availableChoices: StateFlow<List<DialogueChoice>> = _availableChoices.asStateFlow()
    
    /**
     * هل يوجد حوار نشط
     */
    private val _isDialogueActive = MutableStateFlow(false)
    val isDialogueActive: StateFlow<Boolean> = _isDialogueActive.asStateFlow()
    
    /**
     * تاريخ الحوارات
     */
    private val _dialogueHistory = MutableStateFlow<List<DialogueHistoryEntry>>(emptyList())
    val dialogueHistory: StateFlow<List<DialogueHistoryEntry>> = _dialogueHistory.asStateFlow()
    
    /**
     * متغيرات الحوار
     */
    private val _dialogueVariables = MutableStateFlow<Map<String, Any>>(emptyMap())
    val dialogueVariables: StateFlow<Map<String, Any>> = _dialogueVariables.asStateFlow()
    
    /**
     * حالات الحوار المحفوظة
     */
    private val _savedDialogueStates = mutableMapOf<String, DialogueState>()
    
    /**
     * خيط الحوار الحالي
     */
    private var currentDialogueThread: DialogueThread? = null
    
    // ════════════════════════════════════════════════════════════════════════════
    // Dialogue Initialization
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء حوار
     *
     * @param npcId معرف الشخصية
     * @param dialogueTree شجرة الحوار
     * @param context سياق الحوار
     */
    fun startDialogue(
        npcId: String,
        dialogueTree: DialogueTree,
        context: DialogueContext? = null
    ): Boolean {
        try {
            _currentNPC.value = npcId
            _isDialogueActive.value = true
            
            // إنشاء خيط الحوار
            currentDialogueThread = DialogueThread(npcId, dialogueTree, context)
            
            // الانتقال للعقدة الأولى
            val startNode = dialogueTree.getStartNode()
            if (startNode != null) {
                displayNode(startNode)
                EventBus.emit(GameEvent.DialogueStarted(npcId))
                Timber.d("Dialogue started with NPC: $npcId")
                return true
            } else {
                Timber.w("No start node found in dialogue tree")
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting dialogue")
            return false
        }
    }
    
    /**
     * إنهاء الحوار
     */
    fun endDialogue() {
        val npcId = _currentNPC.value
        
        _isDialogueActive.value = false
        _currentDialogue.value = null
        _currentNPC.value = null
        _availableChoices.value = emptyList()
        currentDialogueThread = null
        
        if (npcId != null) {
            EventBus.emit(GameEvent.DialogueEnded(npcId))
            Timber.d("Dialogue ended with NPC: $npcId")
        }
    }
    
    /**
     * عرض عقدة حوار
     *
     * @param node العقدة
     */
    private fun displayNode(node: DialogueNode) {
        _currentDialogue.value = node
        
        // جمع الخيارات المتاحة
        val availableChoices = node.choices.filter { choice ->
            evaluateCondition(choice.condition)
        }
        
        _availableChoices.value = availableChoices
        
        // إضافة للسجل
        addToHistory(node, _currentNPC.value ?: "")
        
        // تنفيذ الإجراءات
        node.onDisplay?.invoke(this)
    }
    
    /**
     * إضافة عقدة لسجل الحوار
     *
     * @param node العقدة
     * @param npcId معرف الشخصية
     */
    private fun addToHistory(node: DialogueNode, npcId: String) {
        val entry = DialogueHistoryEntry(
            nodeId = node.id,
            npcId = npcId,
            text = node.text,
            timestamp = System.currentTimeMillis(),
            speaker = if (node.speaker.isEmpty()) "NPC" else node.speaker
        )
        
        _dialogueHistory.value = _dialogueHistory.value + entry
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Choice Selection
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * اختيار خيار
     *
     * @param choiceIndex فهرس الخيار
     */
    fun selectChoice(choiceIndex: Int): Boolean {
        val choices = _availableChoices.value
        if (choiceIndex !in choices.indices) return false
        
        val choice = choices[choiceIndex]
        val npcId = _currentNPC.value ?: "unknown"
        
        val success = selectChoice(choice)
        if (success) {
            EventBus.emit(GameEvent.DialogueChoiceMade(npcId, choiceIndex, choice.text))
        }
        return success
    }

    /**
     * اختيار خيار مباشرة
     *
     * @param choice الخيار
     */
    fun selectChoice(choice: DialogueChoice): Boolean {
        try {
            val currentNode = _currentDialogue.value ?: return false
            val thread = currentDialogueThread ?: return false
            val tree = thread.dialogueTree
            
            // تنفيذ إجراء الخيار
            choice.onSelect?.invoke(this)
            
            // إضافة الخيار للسجل
            _dialogueHistory.value = _dialogueHistory.value + DialogueHistoryEntry(
                nodeId = "choice_${choice.id}",
                npcId = _currentNPC.value ?: "",
                text = choice.text,
                timestamp = System.currentTimeMillis(),
                speaker = "Player"
            )
            
            // الانتقال للعقدة التالية
            val nextNode = tree.getNode(choice.nextNodeId)
            if (nextNode != null) {
                displayNode(nextNode)
                return true
            } else {
                Timber.w("Next node not found: ${choice.nextNodeId}")
                endDialogue()
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error selecting choice")
            return false
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Variable Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تعيين متغير
     *
     * @param key المفتاح
     * @param value القيمة
     */
    fun setVariable(key: String, value: Any) {
        _dialogueVariables.value = _dialogueVariables.value.toMutableMap().apply {
            put(key, value)
        }
    }
    
    /**
     * الحصول على متغير
     *
     * @param key المفتاح
     */
    fun getVariable(key: String): Any? {
        return _dialogueVariables.value[key]
    }
    
    /**
     * الحصول على متغير كـ String
     *
     * @param key المفتاح
     * @param default القيمة الافتراضية
     */
    fun getVariableAsString(key: String, default: String = ""): String {
        return (getVariable(key) as? String) ?: default
    }
    
    /**
     * الحصول على متغير كـ Int
     *
     * @param key المفتاح
     * @param default القيمة الافتراضية
     */
    fun getVariableAsInt(key: String, default: Int = 0): Int {
        return when (val value = getVariable(key)) {
            is Int -> value
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
    }
    
    /**
     * الحصول على متغير كـ Boolean
     *
     * @param key المفتاح
     * @param default القيمة الافتراضية
     */
    fun getVariableAsBoolean(key: String, default: Boolean = false): Boolean {
        return when (val value = getVariable(key)) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> default
        }
    }
    
    /**
     * إضافة رقم لمتغير
     *
     * @param key المفتاح
     * @param amount المبلغ
     */
    fun addToVariable(key: String, amount: Int) {
        val current = getVariableAsInt(key, 0)
        setVariable(key, current + amount)
    }
    
    /**
     * التحقق من وجود متغير
     *
     * @param key المفتاح
     */
    fun hasVariable(key: String): Boolean {
        return _dialogueVariables.value.containsKey(key)
    }
    
    /**
     * حذف متغير
     *
     * @param key المفتاح
     */
    fun removeVariable(key: String) {
        _dialogueVariables.value = _dialogueVariables.value.toMutableMap().apply {
            remove(key)
        }
    }
    
    /**
     * مسح جميع المتغيرات
     */
    fun clearVariables() {
        _dialogueVariables.value = emptyMap()
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Condition Evaluation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تقييم شرط
     *
     * @param condition الشرط
     */
    fun evaluateCondition(condition: DialogueCondition?): Boolean {
        if (condition == null) return true
        
        return when (condition.type) {
            ConditionType.ALWAYS_TRUE -> true
            ConditionType.ALWAYS_FALSE -> false
            ConditionType.VARIABLE_EQUALS -> {
                val value = getVariable(condition.key)
                value == condition.value
            }
            ConditionType.VARIABLE_NOT_EQUALS -> {
                val value = getVariable(condition.key)
                value != condition.value
            }
            ConditionType.VARIABLE_GREATER_THAN -> {
                val value = getVariableAsInt(condition.key, 0)
                val target = (condition.value as? Int) ?: 0
                value > target
            }
            ConditionType.VARIABLE_LESS_THAN -> {
                val value = getVariableAsInt(condition.key, 0)
                val target = (condition.value as? Int) ?: 0
                value < target
            }
            ConditionType.CUSTOM -> {
                condition.customEvaluator?.invoke(this) ?: true
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // State Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ حالة الحوار
     *
     * @param stateId معرف الحالة
     */
    fun saveDialogueState(stateId: String) {
        val state = DialogueState(
            currentNodeId = _currentDialogue.value?.id,
            npcId = _currentNPC.value,
            variables = _dialogueVariables.value,
            history = _dialogueHistory.value,
            timestamp = System.currentTimeMillis()
        )
        
        _savedDialogueStates[stateId] = state
        Timber.d("Dialogue state saved: $stateId")
    }
    
    /**
     * استعادة حالة الحوار
     *
     * @param stateId معرف الحالة
     */
    fun loadDialogueState(stateId: String): Boolean {
        val state = _savedDialogueStates[stateId] ?: return false
        
        _dialogueVariables.value = state.variables
        _dialogueHistory.value = state.history
        _currentNPC.value = state.npcId
        
        Timber.d("Dialogue state loaded: $stateId")
        return true
    }
    
    /**
     * حذف حالة محفوظة
     *
     * @param stateId معرف الحالة
     */
    fun deleteDialogueState(stateId: String) {
        _savedDialogueStates.remove(stateId)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Dialogue History
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على سجل الحوار الكامل
     */
    fun getFullHistory(): List<DialogueHistoryEntry> {
        return _dialogueHistory.value
    }
    
    /**
     * الحصول على آخر عنصر في السجل
     */
    fun getLastHistoryEntry(): DialogueHistoryEntry? {
        return _dialogueHistory.value.lastOrNull()
    }
    
    /**
     * مسح السجل
     */
    fun clearHistory() {
        _dialogueHistory.value = emptyList()
    }
    
    /**
     * إعادة تعيين الحوار
     */
    fun reset() {
        endDialogue()
        clearVariables()
        clearHistory()
        _savedDialogueStates.clear()
        Timber.d("Dialogue system reset")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نوع الشرط
 */
enum class ConditionType {
    ALWAYS_TRUE,
    ALWAYS_FALSE,
    VARIABLE_EQUALS,
    VARIABLE_NOT_EQUALS,
    VARIABLE_GREATER_THAN,
    VARIABLE_LESS_THAN,
    CUSTOM
}

/**
 * شرط الحوار
 *
 * @property type نوع الشرط
 * @property key مفتاح المتغير
 * @property value القيمة المقارنة
 * @property customEvaluator دالة تقييم مخصصة
 */
data class DialogueCondition(
    val type: ConditionType,
    val key: String = "",
    val value: Any? = null,
    val customEvaluator: ((DialogueSystem) -> Boolean)? = null
)

/**
 * عقدة الحوار
 *
 * @property id معرف العقدة الفريد
 * @property speaker المتحدث
 * @property text نص الحوار
 * @property choices الخيارات المتاحة
 * @property onDisplay إجراء عند عرض العقدة
 */
data class DialogueNode(
    val id: String,
    val speaker: String = "",
    val text: String,
    val choices: List<DialogueChoice> = emptyList(),
    val onDisplay: ((DialogueSystem) -> Unit)? = null
)

/**
 * خيار الحوار
 *
 * @property id معرف الخيار
 * @property text نص الخيار
 * @property nextNodeId معرف العقدة التالية
 * @property condition شرط ظهور الخيار
 * @property onSelect إجراء عند اختيار الخيار
 */
data class DialogueChoice(
    val id: String,
    val text: String,
    val nextNodeId: String,
    val condition: DialogueCondition? = null,
    val onSelect: ((DialogueSystem) -> Unit)? = null
)

/**
 * سياق الحوار
 *
 * @property playerName اسم اللاعب
 * @property questId معرف المهمة
 * @property context سياق إضافي
 */
data class DialogueContext(
    val playerName: String = "Player",
    val questId: String? = null,
    val context: Map<String, Any> = emptyMap()
)

/**
 * خيط الحوار
 *
 * @property npcId معرف الشخصية
 * @property dialogueTree شجرة الحوار
 * @property context السياق
 */
data class DialogueThread(
    val npcId: String,
    val dialogueTree: DialogueTree,
    val context: DialogueContext? = null
)

/**
 * سجل الحوار
 *
 * @property nodeId معرف العقدة
 * @property npcId معرف الشخصية
 * @property text النص
 * @property timestamp الطابع الزمني
 * @property speaker المتحدث
 */
data class DialogueHistoryEntry(
    val nodeId: String,
    val npcId: String,
    val text: String,
    val timestamp: Long,
    val speaker: String
)

/**
 * حالة الحوار (للحفظ)
 *
 * @property currentNodeId معرف العقدة الحالية
 * @property npcId معرف الشخصية
 * @property variables المتغيرات
 * @property history السجل
 * @property timestamp الطابع الزمني
 */
data class DialogueState(
    val currentNodeId: String?,
    val npcId: String?,
    val variables: Map<String, Any>,
    val history: List<DialogueHistoryEntry>,
    val timestamp: Long
)