package com.erygra.maskoflight.dialogue

import timber.log.Timber
import kotlin.math.pow

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * ConditionEvaluator.kt - محيم تقييم الشروط
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - تقييم شروط معقدة
 * - نظام العمليات المنطقية
 * - دعم الدوال المخصصة
 * - تخزين مؤقت للنتائج
 * 
 * المكونات الرئيسية:
 * - Complex condition evaluation
 * - Expression parsing
 * - Logical operations
 * - Custom functions
 * - Result caching
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class ConditionEvaluator {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الدوال المخصصة
     */
    private val customFunctions = mutableMapOf<String, (List<Any>) -> Any>()
    
    /**
     * تخزين مؤقت للنتائج
     */
    private val resultCache = mutableMapOf<String, Any>()
    
    /**
     * تفعيل التخزين المؤقت
     */
    var cacheEnabled = true
    
    // ════════════════════════════════════════════════════════════════════════════
    // Initialization
    // ════════════════════════════════════════════════════════════════════════════
    
    init {
        registerDefaultFunctions()
    }
    
    /**
     * تسجيل الدوال الافتراضية
     */
    private fun registerDefaultFunctions() {
        // دوال حسابية
        registerFunction("abs") { args ->
            when (val value = args.getOrNull(0)) {
                is Number -> kotlin.math.abs(value.toDouble())
                else -> 0.0
            }
        }
        
        registerFunction("min") { args ->
            args.filterIsInstance<Number>().map { it.toDouble() }.minOrNull() ?: 0.0
        }
        
        registerFunction("max") { args ->
            args.filterIsInstance<Number>().map { it.toDouble() }.maxOrNull() ?: 0.0
        }
        
        registerFunction("sqrt") { args ->
            when (val value = args.getOrNull(0)) {
                is Number -> kotlin.math.sqrt(value.toDouble())
                else -> 0.0
            }
        }
        
        registerFunction("pow") { args ->
            val base = (args.getOrNull(0) as? Number)?.toDouble() ?: 0.0
            val exponent = (args.getOrNull(1) as? Number)?.toDouble() ?: 0.0
            base.pow(exponent)
        }
        
        // دوال منطقية
        registerFunction("and") { args ->
            args.all { it is Boolean && it }
        }
        
        registerFunction("or") { args ->
            args.any { it is Boolean && it }
        }
        
        registerFunction("not") { args ->
            when (val value = args.getOrNull(0)) {
                is Boolean -> !value
                else -> false
            }
        }
        
        // دوال نصية
        registerFunction("length") { args ->
            (args.getOrNull(0) as? String)?.length ?: 0
        }
        
        registerFunction("uppercase") { args ->
            (args.getOrNull(0) as? String)?.uppercase() ?: ""
        }
        
        registerFunction("lowercase") { args ->
            (args.getOrNull(0) as? String)?.lowercase() ?: ""
        }
        
        registerFunction("contains") { args ->
            val text = args.getOrNull(0) as? String ?: return@registerFunction false
            val substring = args.getOrNull(1) as? String ?: return@registerFunction false
            text.contains(substring)
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Function Registration
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل دالة مخصصة
     *
     * @param name اسم الدالة
     * @param function الدالة
     */
    fun registerFunction(name: String, function: (List<Any>) -> Any) {
        customFunctions[name] = function
        Timber.d("Custom function registered: $name")
    }
    
    /**
     * إلغاء تسجيل دالة
     *
     * @param name اسم الدالة
     */
    fun unregisterFunction(name: String) {
        customFunctions.remove(name)
    }
    
    /**
     * هل الدالة موجودة
     *
     * @param name اسم الدالة
     */
    fun hasFunction(name: String): Boolean {
        return customFunctions.containsKey(name)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Expression Evaluation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تقييم تعبير
     *
     * @param expression التعبير
     * @param variables المتغيرات
     */
    fun evaluate(expression: String, variables: Map<String, Any> = emptyMap()): Any {
        // التحقق من التخزين المؤقت
        if (cacheEnabled) {
            val cacheKey = expression
            val cached = resultCache[cacheKey]
            if (cached != null) {
                return cached
            }
        }
        
        try {
            val result = evaluateExpression(expression, variables)
            
            // تخزين النتيجة
            if (cacheEnabled) {
                resultCache[expression] = result
            }
            
            return result
        } catch (e: Exception) {
            Timber.e(e, "Error evaluating expression: $expression")
            return false
        }
    }
    
    /**
     * تقييم تعبير (تنفيذ داخلي)
     */
    private fun evaluateExpression(expression: String, variables: Map<String, Any>): Any {
        val trimmed = expression.trim()
        
        // التحقق من القيم الثابتة
        return when {
            trimmed.equals("true", ignoreCase = true) -> true
            trimmed.equals("false", ignoreCase = true) -> false
            trimmed.toIntOrNull() != null -> trimmed.toInt()
            trimmed.toDoubleOrNull() != null -> trimmed.toDouble()
            trimmed.startsWith("\"") && trimmed.endsWith("\"") -> {
                trimmed.substring(1, trimmed.length - 1)
            }
            // المتغيرات
            variables.containsKey(trimmed) -> variables[trimmed] ?: false
            // الدوال
            trimmed.contains("(") -> evaluateFunction(trimmed, variables)
            // العمليات المنطقية
            trimmed.contains("&&") -> evaluateLogicalAnd(trimmed, variables)
            trimmed.contains("||") -> evaluateLogicalOr(trimmed, variables)
            trimmed.contains("==") -> evaluateEquality(trimmed, variables, true)
            trimmed.contains("!=") -> evaluateEquality(trimmed, variables, false)
            trimmed.contains(">") && !trimmed.contains(">=") -> evaluateGreater(trimmed, variables, false)
            trimmed.contains(">=") -> evaluateGreater(trimmed, variables, true)
            trimmed.contains("<") && !trimmed.contains("<=") -> evaluateLess(trimmed, variables, false)
            trimmed.contains("<=") -> evaluateLess(trimmed, variables, true)
            trimmed.contains("!") -> evaluateNot(trimmed, variables)
            else -> false
        }
    }
    
    /**
     * تقييم دالة
     */
    private fun evaluateFunction(expression: String, variables: Map<String, Any>): Any {
        val functionPattern = """(\w+)\((.*)\)""".toRegex()
        val match = functionPattern.find(expression) ?: return false
        
        val functionName = match.groupValues[1]
        val argsString = match.groupValues[2]
        
        val function = customFunctions[functionName] ?: return false
        
        val args = argsString.split(",").map { arg ->
            evaluateExpression(arg.trim(), variables)
        }
        
        return function(args)
    }
    
    /**
     * تقييم AND المنطقية
     */
    private fun evaluateLogicalAnd(expression: String, variables: Map<String, Any>): Boolean {
        val parts = expression.split("&&").map { it.trim() }
        return parts.all { evaluateExpression(it, variables) as? Boolean ?: false }
    }
    
    /**
     * تقييم OR المنطقية
     */
    private fun evaluateLogicalOr(expression: String, variables: Map<String, Any>): Boolean {
        val parts = expression.split("||").map { it.trim() }
        return parts.any { evaluateExpression(it, variables) as? Boolean ?: false }
    }
    
    /**
     * تقييم المساواة
     */
    private fun evaluateEquality(
        expression: String,
        variables: Map<String, Any>,
        isEqual: Boolean
    ): Boolean {
        val operator = if (isEqual) "==" else "!="
        val parts = expression.split(operator).map { it.trim() }
        
        if (parts.size != 2) return false
        
        val left = evaluateExpression(parts[0], variables)
        val right = evaluateExpression(parts[1], variables)
        
        return if (isEqual) left == right else left != right
    }
    
    /**
     * تقييم المقارنة الأكبر
     */
    private fun evaluateGreater(
        expression: String,
        variables: Map<String, Any>,
        isEqual: Boolean
    ): Boolean {
        val operator = if (isEqual) ">=" else ">"
        val parts = expression.split(operator).map { it.trim() }
        
        if (parts.size != 2) return false
        
        val left = (evaluateExpression(parts[0], variables) as? Number)?.toDouble() ?: return false
        val right = (evaluateExpression(parts[1], variables) as? Number)?.toDouble() ?: return false
        
        return if (isEqual) left >= right else left > right
    }
    
    /**
     * تقييم المقارنة الأصغر
     */
    private fun evaluateLess(
        expression: String,
        variables: Map<String, Any>,
        isEqual: Boolean
    ): Boolean {
        val operator = if (isEqual) "<=" else "<"
        val parts = expression.split(operator).map { it.trim() }
        
        if (parts.size != 2) return false
        
        val left = (evaluateExpression(parts[0], variables) as? Number)?.toDouble() ?: return false
        val right = (evaluateExpression(parts[1], variables) as? Number)?.toDouble() ?: return false
        
        return if (isEqual) left <= right else left < right
    }
    
    /**
     * تقييم NOT المنطقية
     */
    private fun evaluateNot(expression: String, variables: Map<String, Any>): Boolean {
        val value = expression.substring(1).trim()
        return !(evaluateExpression(value, variables) as? Boolean ?: false)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Cache Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * مسح التخزين المؤقت
     */
    fun clearCache() {
        resultCache.clear()
    }
    
    /**
     * مسح عنصر من التخزين المؤقت
     *
     * @param key المفتاح
     */
    fun removeCacheEntry(key: String) {
        resultCache.remove(key)
    }
    
    /**
     * حجم التخزين المؤقت
     */
    fun getCacheSize(): Int {
        return resultCache.size
    }
}