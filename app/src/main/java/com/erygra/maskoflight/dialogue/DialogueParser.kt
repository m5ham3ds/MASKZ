package com.erygra.maskoflight.dialogue

import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * DialogueParser.kt - محلل ومعالج نصوص الحوار
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - تحليل نصوص الحوار
 * - دعم المتغيرات والعلامات
 * - معالجة التنسيق والألوان
 * - استبدال النصوص الديناميكية
 * 
 * المكونات الرئيسية:
 * - Text parsing
 * - Variable substitution
 * - Tag processing
 * - Text formatting
 * - Dynamic content
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class DialogueParser {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * قاموس العلامات
     */
    private val tagHandlers = mutableMapOf<String, (String, DialogueSystem) -> String>()
    
    /**
     * قاموس المعالجات المخصصة
     */
    private val customProcessors = mutableMapOf<String, (String) -> String>()
    
    init {
        setupDefaultTags()
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Initialization
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إعداد العلامات الافتراضية
     */
    private fun setupDefaultTags() {
        // علامات اللون
        registerTag("color") { content, _ -> content }
        registerTag("red") { content, _ -> "<color=#FF0000>$content</color>" }
        registerTag("green") { content, _ -> "<color=#00FF00>$content</color>" }
        registerTag("blue") { content, _ -> "<color=#0000FF>$content</color>" }
        registerTag("yellow") { content, _ -> "<color=#FFFF00>$content</color>" }
        registerTag("gold") { content, _ -> "<color=#FFD700>$content</color>" }
        
        // علامات التنسيق
        registerTag("b") { content, _ -> "<b>$content</b>" }
        registerTag("i") { content, _ -> "<i>$content</i>" }
        registerTag("u") { content, _ -> "<u>$content</u>" }
        registerTag("strike") { content, _ -> "<s>$content</s>" }
        
        // علامات خاصة
        registerTag("name") { _, dialogue -> dialogue.getVariableAsString("playerName", "Player") }
        registerTag("npc") { _, dialogue -> dialogue.currentNPC.value ?: "NPC" }
        registerTag("break") { _, _ -> "\n" }
        registerTag("pause") { _, _ -> " " }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Tag Registration
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل علامة جديدة
     *
     * @param tagName اسم العلامة
     * @param handler معالج العلامة
     */
    fun registerTag(
        tagName: String,
        handler: (String, DialogueSystem) -> String
    ) {
        tagHandlers[tagName] = handler
        Timber.d("Dialogue tag registered: $tagName")
    }
    
    /**
     * تسجيل معالج مخصص
     *
     * @param processorName اسم المعالج
     * @param processor الدالة
     */
    fun registerCustomProcessor(
        processorName: String,
        processor: (String) -> String
    ) {
        customProcessors[processorName] = processor
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Text Processing
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * معالجة نص الحوار
     *
     * @param text النص الأصلي
     * @param dialogue نظام الحوار
     */
    fun processText(text: String, dialogue: DialogueSystem): String {
        var processedText = text
        
        // استبدال المتغيرات
        processedText = substituteVariables(processedText, dialogue)
        
        // معالجة العلامات
        processedText = processTags(processedText, dialogue)
        
        // معالجة الأوامر الخاصة
        processedText = processSpecialCommands(processedText, dialogue)
        
        return processedText.trim()
    }
    
    /**
     * استبدال المتغيرات
     *
     * @param text النص
     * @param dialogue نظام الحوار
     */
    private fun substituteVariables(text: String, dialogue: DialogueSystem): String {
        var result = text
        
        // البحث عن المتغيرات بصيغة ${variableName}
        val variablePattern = """\$\{([a-zA-Z_][a-zA-Z0-9_]*)\}""".toRegex()
        
        variablePattern.findAll(text).forEach { match ->
            val variableName = match.groupValues[1]
            val variableValue = dialogue.getVariable(variableName)?.toString() ?: match.value
            result = result.replace(match.value, variableValue)
        }
        
        return result
    }
    
    /**
     * معالجة العلامات
     *
     * @param text النص
     * @param dialogue نظام الحوار
     */
    private fun processTags(text: String, dialogue: DialogueSystem): String {
        var result = text
        
        // البحث عن العلامات بصيغة [tagName:content]
        val tagPattern = """\[([a-zA-Z_][a-zA-Z0-9_]*):([^\]]*)\]""".toRegex()
        
        tagPattern.findAll(text).forEach { match ->
            val tagName = match.groupValues[1]
            val content = match.groupValues[2]
            
            val handler = tagHandlers[tagName]
            if (handler != null) {
                val replacement = handler(content, dialogue)
                result = result.replace(match.value, replacement)
            }
        }
        
        // معالجة العلامات بدون محتوى [tagName]
        val simpleTagPattern = """\[([a-zA-Z_][a-zA-Z0-9_]*)\]""".toRegex()
        
        simpleTagPattern.findAll(text).forEach { match ->
            val tagName = match.groupValues[1]
            val handler = tagHandlers[tagName]
            if (handler != null) {
                val replacement = handler("", dialogue)
                result = result.replace(match.value, replacement)
            }
        }
        
        return result
    }
    
    /**
     * معالجة الأوامر الخاصة
     *
     * @param text النص
     * @param dialogue نظام الحوار
     */
    private fun processSpecialCommands(text: String, dialogue: DialogueSystem): String {
        var result = text
        
        // البحث عن الأوامر بصيغة #command:parameter
        val commandPattern = """#([a-zA-Z_][a-zA-Z0-9_]*):([^\n]+)""".toRegex()
        
        commandPattern.findAll(text).forEach { match ->
            val command = match.groupValues[1]
            val parameter = match.groupValues[2]
            
            when (command) {
                "add_variable" -> {
                    val parts = parameter.split("=")
                    if (parts.size == 2) {
                        dialogue.setVariable(parts[0].trim(), parts[1].trim())
                    }
                }
                "call_processor" -> {
                    val processor = customProcessors[parameter]
                    if (processor != null) {
                        result = result.replace(match.value, processor(parameter))
                    }
                }
            }
        }
        
        return result
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Text Analysis
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * استخراج المتغيرات من النص
     *
     * @param text النص
     */
    fun extractVariables(text: String): List<String> {
        val variablePattern = """\$\{([a-zA-Z_][a-zA-Z0-9_]*)\}""".toRegex()
        return variablePattern.findAll(text)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }
    
    /**
     * استخراج العلامات من النص
     *
     * @param text النص
     */
    fun extractTags(text: String): List<String> {
        val tagPattern = """\[([a-zA-Z_][a-zA-Z0-9_]*)[:\]]""".toRegex()
        return tagPattern.findAll(text)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }
    
    /**
     * التحقق من صحة النص
     *
     * @param text النص
     */
    fun validateText(text: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // التحقق من الأقواس المتطابقة
        var bracketCount = 0
        text.forEach { char ->
            when (char) {
                '[' -> bracketCount++
                ']' -> bracketCount--
            }
            if (bracketCount < 0) {
                errors.add("Mismatched brackets")
                bracketCount = 0
            }
        }
        if (bracketCount != 0) {
            errors.add("Unclosed brackets")
        }
        
        // التحقق من المتغيرات المستخدمة
        extractVariables(text).forEach { variable ->
            if (variable.isEmpty()) {
                errors.add("Empty variable name")
            }
        }
        
        // التحقق من العلامات المستخدمة
        extractTags(text).forEach { tag ->
            if (!tagHandlers.containsKey(tag)) {
                warnings.add("Unknown tag: $tag")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Text Formatting
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تنظيف النص من العلامات
     *
     * @param text النص
     */
    fun stripTags(text: String): String {
        var result = text
        
        // إزالة علامات HTML
        result = result.replace("""<[^>]*>""".toRegex(), "")
        
        // إزالة العلامات المخصصة
        result = result.replace("""\[[^\]]*\]""".toRegex(), "")
        
        return result
    }
    
    /**
     * قص النص لطول معين
     *
     * @param text النص
     * @param maxLength الطول الأقصى
     * @param ellipsis النقاط في النهاية
     */
    fun truncateText(text: String, maxLength: Int, ellipsis: String = "..."): String {
        if (text.length <= maxLength) return text
        
        val cleanText = stripTags(text)
        return if (cleanText.length <= maxLength) {
            cleanText
        } else {
            cleanText.substring(0, maxLength - ellipsis.length) + ellipsis
        }
    }
    
    /**
     * حساب طول النص الفعلي (بدون علامات)
     *
     * @param text النص
     */
    fun getTextLength(text: String): Int {
        return stripTags(text).length
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نتيجة التحقق من النص
 *
 * @property isValid هل النص صحيح
 * @property errors قائمة الأخطاء
 * @property warnings قائمة التحذيرات
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)