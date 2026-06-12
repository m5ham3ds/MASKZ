package com.erygra.maskoflight.save

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import timber.log.Timber
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * SaveSerializer.kt - محول بيانات الحفظ
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - تحويل بيانات الحفظ من/إلى JSON
 * - ضغط البيانات
 * - التشفير (اختياري)
 * - التحقق من الصحة
 * 
 * المكونات الرئيسية:
 * - JSON serialization
 * - Data compression
 * - Encryption support
 * - Validation
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

object SaveSerializer {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Moshi instance لـ JSON
     */
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    /**
     * تفعيل الضغط
     */
    private var compressionEnabled = true
    
    /**
     * تفعيل التشفير
     */
    private var encryptionEnabled = false
    
    /**
     * معدل الضغط
     */
    private var compressionLevel = 6 // 1-9
    
    // ════════════════════════════════════════════════════════════════════════════
    // Configuration
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تعيين تفعيل الضغط
     *
     * @param enabled تفعيل الضغط
     */
    fun setCompressionEnabled(enabled: Boolean) {
        compressionEnabled = enabled
        Timber.d("Compression: ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * تعيين مستوى الضغط
     *
     * @param level المستوى (1-9)
     */
    fun setCompressionLevel(level: Int) {
        compressionLevel = level.coerceIn(1, 9)
        Timber.d("Compression level set to: $compressionLevel")
    }
    
    /**
     * تعيين تفعيل التشفير
     *
     * @param enabled تفعيل التشفير
     */
    fun setEncryptionEnabled(enabled: Boolean) {
        encryptionEnabled = enabled
        Timber.d("Encryption: ${if (enabled) "enabled" else "disabled"}")
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Serialization
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تحويل بيانات الحفظ إلى نص
     *
     * @param saveData بيانات الحفظ
     */
    fun serialize(saveData: SaveData): String {
        return try {
            // تحويل إلى JSON
            val adapter = moshi.adapter(SaveData::class.java)
            var json = adapter.toJson(saveData)
            
            Timber.d("Serialized save data (before compression): ${json.length} chars")
            
            // ضغط البيانات
            if (compressionEnabled) {
                json = compressData(json)
                Timber.d("Data compressed successfully")
            }
            
            // تشفير البيانات
            if (encryptionEnabled) {
                json = encryptData(json)
                Timber.d("Data encrypted successfully")
            }
            
            json
        } catch (e: Exception) {
            Timber.e(e, "Error serializing save data")
            ""
        }
    }
    
    /**
     * تحويل نص إلى بيانات حفظ
     *
     * @param data النص
     */
    fun deserialize(data: String): SaveData {
        return try {
            var json = data
            
            // فك التشفير
            if (encryptionEnabled) {
                json = decryptData(json)
                Timber.d("Data decrypted successfully")
            }
            
            // فك الضغط
            if (compressionEnabled) {
                json = decompressData(json)
                Timber.d("Data decompressed successfully")
            }
            
            // تحويل من JSON
            val adapter = moshi.adapter(SaveData::class.java)
            val saveData = adapter.fromJson(json)
            
            if (saveData != null) {
                Timber.d("Save data deserialized successfully")
                saveData
            } else {
                Timber.w("Failed to deserialize save data")
                SaveData()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deserializing save data")
            SaveData()
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Compression
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * ضغط البيانات
     *
     * @param data البيانات
     */
    private fun compressData(data: String): String {
        return try {
            val bytes = data.toByteArray(Charsets.UTF_8)
            val baos = ByteArrayOutputStream()
            
            GZIPOutputStream(baos, compressionLevel).use { gzip ->
                gzip.write(bytes)
            }
            
            val compressed = baos.toByteArray()
            val encoded = android.util.Base64.encodeToString(compressed, android.util.Base64.DEFAULT)
            
            Timber.d("Original: ${bytes.size} bytes, Compressed: ${compressed.size} bytes")
            
            encoded
        } catch (e: Exception) {
            Timber.e(e, "Error compressing data")
            data
        }
    }
    
    /**
     * فك ضغط البيانات
     *
     * @param data البيانات المضغوطة
     */
    private fun decompressData(data: String): String {
        return try {
            val compressed = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
            val bais = ByteArrayInputStream(compressed)
            
            val baos = ByteArrayOutputStream()
            GZIPInputStream(bais).use { gzip ->
                val buffer = ByteArray(1024)
                var len: Int
                while (gzip.read(buffer).also { len = it } != -1) {
                    baos.write(buffer, 0, len)
                }
            }
            
            String(baos.toByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Error decompressing data")
            data
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Encryption (Basic XOR - للتطوير فقط، استخدم مكتبة تشفير حقيقية في الإنتاج)
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تشفير البيانات
     *
     * @param data البيانات
     */
    private fun encryptData(data: String): String {
        return try {
            // استخدام مفتاح ثابت (يجب تغييره في الإنتاج)
            val key = "MaskOfLight2024"
            val bytes = data.toByteArray(Charsets.UTF_8)
            val encrypted = ByteArray(bytes.size)
            
            for (i in bytes.indices) {
                encrypted[i] = (bytes[i].toInt() xor key[i % key.length].code).toByte()
            }
            
            android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Timber.e(e, "Error encrypting data")
            data
        }
    }
    
    /**
     * فك تشفير البيانات
     *
     * @param data البيانات المشفرة
     */
    private fun decryptData(data: String): String {
        return try {
            val key = "MaskOfLight2024"
            val encrypted = android.util.Base64.decode(data, android.util.Base64.DEFAULT)
            val decrypted = ByteArray(encrypted.size)
            
            for (i in encrypted.indices) {
                decrypted[i] = (encrypted[i].toInt() xor key[i % key.length].code).toByte()
            }
            
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Error decrypting data")
            data
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Validation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من صحة بيانات الحفظ
     *
     * @param saveData بيانات الحفظ
     */
    fun validate(saveData: SaveData): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // التحقق من البيانات الأساسية
        if (saveData.playerData.playerName.isBlank()) {
            errors.add("Player name is empty")
        }
        
        if (saveData.playerData.level < 1) {
            errors.add("Player level must be at least 1")
        }
        
        if (saveData.playerData.maxHealth <= 0) {
            errors.add("Max health must be greater than 0")
        }
        
        if (saveData.playerData.health < 0 || saveData.playerData.health > saveData.playerData.maxHealth) {
            errors.add("Health is out of range")
        }
        
        // تحذيرات
        if (saveData.playtime < 0) {
            warnings.add("Playtime is negative")
        }
        
        if (saveData.statistics.totalDeaths < 0) {
            warnings.add("Total deaths is negative")
        }
        
        if (saveData.inventory.items.size > saveData.inventory.capacity) {
            warnings.add("Inventory exceeds capacity")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Statistics
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على حجم البيانات المسلسلة
     *
     * @param saveData بيانات الحفظ
     */
    fun getSerializedSize(saveData: SaveData): Long {
        return serialize(saveData).length.toLong()
    }
    
    /**
     * الحصول على معدل الضغط
     *
     * @param saveData بيانات الحفظ
     */
    fun getCompressionRatio(saveData: SaveData): Float {
        return try {
            val adapter = moshi.adapter(SaveData::class.java)
            val json = adapter.toJson(saveData)
            val originalSize = json.length.toLong()
            val compressedSize = serialize(saveData).length.toLong()
            
            if (originalSize > 0) {
                (1.0f - (compressedSize.toFloat() / originalSize.toFloat())) * 100f
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نتيجة التحقق
 *
 * @property isValid هل البيانات صحيحة
 * @property errors قائمة الأخطاء
 * @property warnings قائمة التحذيرات
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)