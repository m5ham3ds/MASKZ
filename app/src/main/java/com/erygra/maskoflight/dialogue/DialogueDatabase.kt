package com.erygra.maskoflight.dialogue

import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * DialogueDatabase.kt - قاعدة بيانات الحوارات
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - تخزين وإدارة شجرات الحوار
 * - البحث والاستعلامات
 * - إدارة الحوارات حسب الشخصيات
 * - نسخ احتياطية وتصدير
 * 
 * المكونات الرئيسية:
 * - Dialogue tree storage
 * - NPC management
 * - Search and queries
 * - Tree validation
 * - Export/Import
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class DialogueDatabase {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * شجرات الحوار حسب الشخصية
     */
    private val npcDialogues = mutableMapOf<String, MutableList<DialogueTree>>()
    
    /**
     * جميع شجرات الحوار
     */
    private val allDialogueTrees = mutableMapOf<String, DialogueTree>()
    
    /**
     * الشخصيات المعروفة
     */
    private val npcs = mutableMapOf<String, NPCInfo>()
    
    // ════════════════════════════════════════════════════════════════════════════
    // Dialogue Tree Registration
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل شجرة حوار
     *
     * @param npcId معرف الشخصية
     * @param tree شجرة الحوار
     */
    fun registerDialogueTree(npcId: String, tree: DialogueTree) {
        allDialogueTrees[tree.id] = tree
        npcDialogues.getOrPut(npcId) { mutableListOf() }.add(tree)
        
        Timber.d("Dialogue tree registered for NPC: $npcId (${tree.id})")
    }
    
    /**
     * تسجيل عدة شجرات حوار
     *
     * @param npcId معرف الشخصية
     * @param trees قائمة الأشجار
     */
    fun registerDialogueTrees(npcId: String, trees: List<DialogueTree>) {
        trees.forEach { registerDialogueTree(npcId, it) }
    }
    
    /**
     * إلغاء تسجيل شجرة حوار
     *
     * @param treeId معرف الشجرة
     */
    fun unregisterDialogueTree(treeId: String) {
        val tree = allDialogueTrees.remove(treeId) ?: return
        
        npcDialogues.forEach { (_, trees) ->
            trees.remove(tree)
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Dialogue Tree Query
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على شجرة حوار بمعرفها
     *
     * @param treeId معرف الشجرة
     */
    fun getDialogueTree(treeId: String): DialogueTree? {
        return allDialogueTrees[treeId]
    }
    
    /**
     * الحصول على حوارات شخصية محددة
     *
     * @param npcId معرف الشخصية
     */
    fun getNPCDialogues(npcId: String): List<DialogueTree> {
        return npcDialogues[npcId] ?: emptyList()
    }
    
    /**
     * الحصول على الحوار الأول لشخصية
     *
     * @param npcId معرف الشخصية
     */
    fun getDefaultDialogue(npcId: String): DialogueTree? {
        return getNPCDialogues(npcId).firstOrNull()
    }
    
    /**
     * البحث عن حوار بالاسم
     *
     * @param npcId معرف الشخصية
     * @param treeName اسم الشجرة
     */
    fun findDialogueByName(npcId: String, treeName: String): DialogueTree? {
        return getNPCDialogues(npcId).find { it.name == treeName }
    }
    
    /**
     * الحصول على جميع الأشجار
     */
    fun getAllDialogueTrees(): List<DialogueTree> {
        return allDialogueTrees.values.toList()
    }
    
    /**
     * عدد شجرات الحوار
     */
    fun getTreeCount(): Int {
        return allDialogueTrees.size
    }
    
    /**
     * عدد الشخصيات
     */
    fun getNPCCount(): Int {
        return npcDialogues.size
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // NPC Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل شخصية
     *
     * @param npcId معرف الشخصية
     * @param info معلومات الشخصية
     */
    fun registerNPC(npcId: String, info: NPCInfo) {
        npcs[npcId] = info
        npcDialogues.getOrPut(npcId) { mutableListOf() }
        
        Timber.d("NPC registered: $npcId")
    }
    
    /**
     * الحصول على معلومات الشخصية
     *
     * @param npcId معرف الشخصية
     */
    fun getNPCInfo(npcId: String): NPCInfo? {
        return npcs[npcId]
    }
    
    /**
     * الحصول على أسماء جميع الشخصيات
     */
    fun getAllNPCIds(): List<String> {
        return npcDialogues.keys.toList()
    }
    
    /**
     * هل الشخصية موجودة
     *
     * @param npcId معرف الشخصية
     */
    fun hasNPC(npcId: String): Boolean {
        return npcDialogues.containsKey(npcId)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Validation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من جميع شجرات الحوار
     */
    fun validateAllTrees(): DatabaseValidationResult {
        val results = mutableMapOf<String, TreeValidationResult>()
        var totalErrors = 0
        var totalWarnings = 0
        
        allDialogueTrees.forEach { (treeId, tree) ->
            val validation = tree.validate()
            results[treeId] = validation
            totalErrors += validation.errors.size
            totalWarnings += validation.warnings.size
        }
        
        return DatabaseValidationResult(
            isValid = totalErrors == 0,
            totalErrors = totalErrors,
            totalWarnings = totalWarnings,
            treeResults = results
        )
    }
    
    /**
     * التحقق من شجرة محددة
     *
     * @param treeId معرف الشجرة
     */
    fun validateTree(treeId: String): TreeValidationResult? {
        return allDialogueTrees[treeId]?.validate()
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Statistics
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إحصائيات قاعدة البيانات
     */
    fun getStatistics(): DatabaseStatistics {
        var totalNodes = 0
        var totalChoices = 0
        var totalEndNodes = 0
        
        allDialogueTrees.values.forEach { tree ->
            val stats = tree.getStatistics()
            totalNodes += stats.nodeCount
            totalChoices += stats.totalChoices
            totalEndNodes += stats.endNodeCount
        }
        
        return DatabaseStatistics(
            totalTrees = allDialogueTrees.size,
            totalNPCs = npcDialogues.size,
            totalNodes = totalNodes,
            totalChoices = totalChoices,
            totalEndNodes = totalEndNodes,
            averageNodesPerTree = if (allDialogueTrees.size > 0) {
                totalNodes / allDialogueTrees.size.toFloat()
            } else {
                0f
            },
            averageChoicesPerTree = if (allDialogueTrees.size > 0) {
                totalChoices / allDialogueTrees.size.toFloat()
            } else {
                0f
            }
        )
    }
    
    /**
     * الحصول على إحصائيات شخصية محددة
     *
     * @param npcId معرف الشخصية
     */
    fun getNPCStatistics(npcId: String): NPCStatistics? {
        val trees = getNPCDialogues(npcId)
        if (trees.isEmpty()) return null
        
        var totalNodes = 0
        var totalChoices = 0
        
        trees.forEach { tree ->
            val stats = tree.getStatistics()
            totalNodes += stats.nodeCount
            totalChoices += stats.totalChoices
        }
        
        return NPCStatistics(
            npcId = npcId,
            dialogueTreeCount = trees.size,
            totalNodes = totalNodes,
            totalChoices = totalChoices,
            averageNodesPerTree = if (trees.size > 0) {
                totalNodes / trees.size.toFloat()
            } else {
                0f
            }
        )
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Search and Filter
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * البحث عن حوارات بالاسم
     *
     * @param query نص البحث
     */
    fun searchByName(query: String): List<DialogueTree> {
        val lowerQuery = query.lowercase()
        return allDialogueTrees.values.filter { tree ->
            tree.name.lowercase().contains(lowerQuery) ||
            tree.id.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * الحصول على الحوارات حسب الحد الأدنى من العقد
     *
     * @param minNodes الحد الأدنى
     */
    fun getTreesByMinNodes(minNodes: Int): List<DialogueTree> {
        return allDialogueTrees.values.filter { tree ->
            tree.getNodeCount() >= minNodes
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Export/Import
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تصدير جميع الحوارات
     */
    fun exportAllDialogues(): Map<String, DialogueTree> {
        return allDialogueTrees.toMap()
    }
    
    /**
     * استيراد حوارات
     *
     * @param dialogues خريطة الحوارات
     */
    fun importDialogues(dialogues: Map<String, DialogueTree>) {
        dialogues.forEach { (_, tree) ->
            allDialogueTrees[tree.id] = tree
        }
        Timber.d("${dialogues.size} dialogue trees imported")
    }
    
    /**
     * مسح قاعدة البيانات
     */
    fun clear() {
        allDialogueTrees.clear()
        npcDialogues.clear()
        npcs.clear()
        Timber.d("Dialogue database cleared")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * معلومات الشخصية
 *
 * @property id معرف الشخصية
 * @property name اسم الشخصية
 * @property description وصف الشخصية
 * @property portrait صورة الشخصية
 * @property voice صوت الشخصية
 * @property personality شخصية الشخصية
 */
data class NPCInfo(
    val id: String,
    val name: String,
    val description: String = "",
    val portrait: String? = null,
    val voice: String? = null,
    val personality: String = "neutral"
)

/**
 * نتيجة التحقق من قاعدة البيانات
 *
 * @property isValid هل صحيحة
 * @property totalErrors إجمالي الأخطاء
 * @property totalWarnings إجمالي التحذيرات
 * @property treeResults نتائج الأشجار
 */
data class DatabaseValidationResult(
    val isValid: Boolean,
    val totalErrors: Int,
    val totalWarnings: Int,
    val treeResults: Map<String, TreeValidationResult>
)

/**
 * إحصائيات قاعدة البيانات
 *
 * @property totalTrees إجمالي الأشجار
 * @property totalNPCs إجمالي الشخصيات
 * @property totalNodes إجمالي العقد
 * @property totalChoices إجمالي الخيارات
 * @property totalEndNodes إجمالي العقد النهائية
 * @property averageNodesPerTree متوسط العقد لكل شجرة
 * @property averageChoicesPerTree متوسط الخيارات لكل شجرة
 */
data class DatabaseStatistics(
    val totalTrees: Int,
    val totalNPCs: Int,
    val totalNodes: Int,
    val totalChoices: Int,
    val totalEndNodes: Int,
    val averageNodesPerTree: Float,
    val averageChoicesPerTree: Float
)

/**
 * إحصائيات الشخصية
 *
 * @property npcId معرف الشخصية
 * @property dialogueTreeCount عدد أشجار الحوار
 * @property totalNodes إجمالي العقد
 * @property totalChoices إجمالي الخيارات
 * @property averageNodesPerTree متوسط العقد لكل شجرة
 */
data class NPCStatistics(
    val npcId: String,
    val dialogueTreeCount: Int,
    val totalNodes: Int,
    val totalChoices: Int,
    val averageNodesPerTree: Float
)