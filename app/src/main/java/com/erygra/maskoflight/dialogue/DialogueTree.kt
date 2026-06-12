package com.erygra.maskoflight.dialogue

import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * DialogueTree.kt - شجرة الحوار
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - بناء وإدارة شجرة الحوار
 * - البحث عن العقد
 * - التحقق من سلامة الشجرة
 * - تخزين العقد والعلاقات
 * 
 * المكونات الرئيسية:
 * - Node storage and retrieval
 * - Tree validation
 * - Path finding
 * - Tree analysis
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class DialogueTree {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * معرف الشجرة
     */
    var id: String = ""
    
    /**
     * اسم الشجرة
     */
    var name: String = ""
    
    /**
     * العقد في الشجرة
     */
    private val nodes = mutableMapOf<String, DialogueNode>()
    
    /**
     * معرف العقدة الأولى
     */
    private var startNodeId: String? = null
    
    // ════════════════════════════════════════════════════════════════════════════
    // Node Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة عقدة
     *
     * @param node العقدة
     */
    fun addNode(node: DialogueNode) {
        nodes[node.id] = node
        
        if (startNodeId == null) {
            startNodeId = node.id
        }
        
        Timber.d("Node added to dialogue tree: ${node.id}")
    }
    
    /**
     * إضافة عدة عقد
     *
     * @param nodeList قائمة العقد
     */
    fun addNodes(nodeList: List<DialogueNode>) {
        nodeList.forEach { addNode(it) }
    }
    
    /**
     * الحصول على عقدة
     *
     * @param nodeId معرف العقدة
     */
    fun getNode(nodeId: String): DialogueNode? {
        return nodes[nodeId]
    }
    
    /**
     * الحصول على العقدة الأولى
     */
    fun getStartNode(): DialogueNode? {
        return startNodeId?.let { nodes[it] }
    }
    
    /**
     * تعيين العقدة الأولى
     *
     * @param nodeId معرف العقدة
     */
    fun setStartNode(nodeId: String) {
        if (nodes.containsKey(nodeId)) {
            startNodeId = nodeId
            Timber.d("Start node set to: $nodeId")
        } else {
            Timber.w("Node not found: $nodeId")
        }
    }
    
    /**
     * حذف عقدة
     *
     * @param nodeId معرف العقدة
     */
    fun removeNode(nodeId: String) {
        nodes.remove(nodeId)
        if (startNodeId == nodeId) {
            startNodeId = null
        }
    }
    
    /**
     * الحصول على جميع العقد
     */
    fun getAllNodes(): List<DialogueNode> {
        return nodes.values.toList()
    }
    
    /**
     * عدد العقد
     */
    fun getNodeCount(): Int {
        return nodes.size
    }
    
    /**
     * هل توجد عقدة
     *
     * @param nodeId معرف العقدة
     */
    fun hasNode(nodeId: String): Boolean {
        return nodes.containsKey(nodeId)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Path Finding
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على الطريق بين عقدتين
     *
     * @param fromNodeId معرف العقدة المصدر
     * @param toNodeId معرف العقدة الهدف
     */
    fun findPath(fromNodeId: String, toNodeId: String): List<String>? {
        if (!nodes.containsKey(fromNodeId) || !nodes.containsKey(toNodeId)) {
            return null
        }
        
        val visited = mutableSetOf<String>()
        val path = mutableListOf<String>()
        
        if (dfs(fromNodeId, toNodeId, visited, path)) {
            return path
        }
        
        return null
    }
    
    /**
     * البحث بالعمق أولاً (DFS)
     */
    private fun dfs(
        current: String,
        target: String,
        visited: MutableSet<String>,
        path: MutableList<String>
    ): Boolean {
        if (current == target) {
            path.add(current)
            return true
        }
        
        visited.add(current)
        path.add(current)
        
        val currentNode = nodes[current] ?: return false
        
        for (choice in currentNode.choices) {
            if (!visited.contains(choice.nextNodeId)) {
                if (dfs(choice.nextNodeId, target, visited, path)) {
                    return true
                }
            }
        }
        
        path.removeAt(path.size - 1)
        return false
    }
    
    /**
     * الحصول على جميع المسارات من عقدة
     *
     * @param fromNodeId معرف العقدة
     */
    fun getPaths(fromNodeId: String): List<List<String>> {
        val paths = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val path = mutableListOf<String>()
        
        collectPaths(fromNodeId, visited, path, paths)
        
        return paths
    }
    
    /**
     * جمع جميع المسارات
     */
    private fun collectPaths(
        current: String,
        visited: MutableSet<String>,
        path: MutableList<String>,
        paths: MutableList<List<String>>
    ) {
        visited.add(current)
        path.add(current)
        
        val currentNode = nodes[current]
        
        if (currentNode?.choices.isNullOrEmpty()) {
            // عقدة نهائية
            paths.add(path.toList())
        } else {
            currentNode?.choices?.forEach { choice ->
                if (!visited.contains(choice.nextNodeId)) {
                    collectPaths(choice.nextNodeId, visited.toMutableSet(), path, paths)
                }
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Tree Validation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من سلامة الشجرة
     */
    fun validate(): TreeValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // التحقق من وجود عقدة أولى
        if (startNodeId == null) {
            errors.add("No start node defined")
        }
        
        // التحقق من العقد المعزولة
        val reachable = getReachableNodes()
        nodes.keys.forEach { nodeId ->
            if (!reachable.contains(nodeId)) {
                warnings.add("Unreachable node: $nodeId")
            }
        }
        
        // التحقق من الروابط المرجعية المفقودة
        nodes.values.forEach { node ->
            node.choices.forEach { choice ->
                if (!nodes.containsKey(choice.nextNodeId)) {
                    errors.add("Broken link in node ${node.id}: ${choice.nextNodeId}")
                }
            }
        }
        
        // التحقق من العقد النهائية (بدون خيارات)
        val endNodes = nodes.values.filter { it.choices.isEmpty() }
        if (endNodes.isEmpty()) {
            warnings.add("No end nodes found")
        }
        
        return TreeValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            nodeCount = nodes.size,
            endNodeCount = endNodes.size
        )
    }
    
    /**
     * الحصول على العقد التي يمكن الوصول إليها من العقدة الأولى
     */
    fun getReachableNodes(): Set<String> {
        if (startNodeId == null) return emptySet()
        
        val reachable = mutableSetOf<String>()
        val toVisit = mutableListOf(startNodeId!!)
        
        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeAt(0)
            if (reachable.contains(current)) continue
            
            reachable.add(current)
            
            val node = nodes[current] ?: continue
            node.choices.forEach { choice ->
                if (!reachable.contains(choice.nextNodeId)) {
                    toVisit.add(choice.nextNodeId)
                }
            }
        }
        
        return reachable
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Statistics
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إحصائيات الشجرة
     */
    fun getStatistics(): DialogueTreeStats {
        val nodeCount = nodes.size
        val endNodeCount = nodes.values.count { it.choices.isEmpty() }
        val totalChoices = nodes.values.sumOf { it.choices.size }
        val averageChoicesPerNode = if (nodeCount > 0) totalChoices / nodeCount.toFloat() else 0f
        val maxDepth = getMaxDepth()
        
        return DialogueTreeStats(
            nodeCount = nodeCount,
            endNodeCount = endNodeCount,
            totalChoices = totalChoices,
            averageChoicesPerNode = averageChoicesPerNode,
            maxDepth = maxDepth,
            reachableNodeCount = getReachableNodes().size
        )
    }
    
    /**
     * الحصول على أقصى عمق للشجرة
     */
    fun getMaxDepth(): Int {
        if (startNodeId == null) return 0
        
        val depths = mutableMapOf<String, Int>()
        calculateDepths(startNodeId!!, 0, depths)
        
        return depths.values.maxOrNull() ?: 0
    }
    
    /**
     * حساب أعماق العقد
     */
    private fun calculateDepths(
        nodeId: String,
        depth: Int,
        depths: MutableMap<String, Int>
    ) {
        if (depths.containsKey(nodeId) && depths[nodeId]!! >= depth) {
            return
        }
        
        depths[nodeId] = depth
        
        val node = nodes[nodeId] ?: return
        node.choices.forEach { choice ->
            calculateDepths(choice.nextNodeId, depth + 1, depths)
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Builder
    // ════════════════════════════════════════════════════════════════════════════
    
    companion object {
        /**
         * بناء شجرة حوار
         */
        fun builder(id: String = ""): DialogueTreeBuilder {
            return DialogueTreeBuilder(id)
        }
    }
}

/**
 * بناء شجرة الحوار
 */
class DialogueTreeBuilder(private val id: String = "") {
    private val tree = DialogueTree()
    private val nodesList = mutableListOf<DialogueNode>()
    
    init {
        tree.id = id
    }
    
    fun name(value: String) = apply { tree.name = value }
    
    fun addNode(node: DialogueNode) = apply { nodesList.add(node) }
    
    fun addNodes(nodes: List<DialogueNode>) = apply { nodesList.addAll(nodes) }
    
    fun startNode(nodeId: String) = apply { tree.setStartNode(nodeId) }
    
    fun build(): DialogueTree {
        nodesList.forEach { tree.addNode(it) }
        return tree
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نتيجة التحقق من الشجرة
 *
 * @property isValid هل الشجرة صحيحة
 * @property errors قائمة الأخطاء
 * @property warnings قائمة التحذيرات
 * @property nodeCount عدد العقد
 * @property endNodeCount عدد العقد النهائية
 */
data class TreeValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val nodeCount: Int,
    val endNodeCount: Int
)

/**
 * إحصائيات شجرة الحوار
 *
 * @property nodeCount عدد العقد
 * @property endNodeCount عدد العقد النهائية
 * @property totalChoices إجمالي الخيارات
 * @property averageChoicesPerNode متوسط الخيارات لكل عقدة
 * @property maxDepth أقصى عمق
 * @property reachableNodeCount عدد العقد التي يمكن الوصول إليها
 */
data class DialogueTreeStats(
    val nodeCount: Int,
    val endNodeCount: Int,
    val totalChoices: Int,
    val averageChoicesPerNode: Float,
    val maxDepth: Int,
    val reachableNodeCount: Int
)