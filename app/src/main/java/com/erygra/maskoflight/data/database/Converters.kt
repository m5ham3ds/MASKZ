package com.erygra.maskoflight.data.database

import androidx.room.TypeConverter
import com.erygra.maskoflight.player.ItemType
import com.erygra.maskoflight.player.Rarity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * Converters - محولات الأنواع لقاعدة البيانات
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * محولات Room TypeConverter لتحويل الأنواع المعقدة إلى أنواع بسيطة
 * قابلة للتخزين في قاعدة البيانات
 * 
 * Room TypeConverters for converting complex types to simple types
 * that can be stored in the database
 * 
 * الأنواع المدعومة / Supported Types:
 * - List<String> ↔ String (JSON)
 * - Map<String, Any> ↔ String (JSON)
 * - Enums ↔ String
 * - Long ↔ Date representation
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
object Converters {

    /**
     * محلل JSON لتحويل الكائنات
     * JSON parser for object conversion
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    // ═══════════════════════════════════════════════════════════════════════
    // List Converters - محولات القوائم
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحويل قائمة النصوص إلى JSON
     * Convert string list to JSON
     * 
     * @param list قائمة النصوص / String list
     * @return نص JSON / JSON string
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return if (list == null || list.isEmpty()) {
            "[]"
        } else {
            json.encodeToString(list)
        }
    }

    /**
     * تحويل JSON إلى قائمة نصوص
     * Convert JSON to string list
     * 
     * @param value نص JSON / JSON string
     * @return قائمة النصوص / String list
     */
    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrBlank() || value == "[]") {
            emptyList()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * تحويل قائمة الأعداد الصحيحة إلى JSON
     * Convert integer list to JSON
     * 
     * @param list قائمة الأعداد / Integer list
     * @return نص JSON / JSON string
     */
    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        return if (list == null || list.isEmpty()) {
            "[]"
        } else {
            json.encodeToString(list)
        }
    }

    /**
     * تحويل JSON إلى قائمة أعداد صحيحة
     * Convert JSON to integer list
     * 
     * @param value نص JSON / JSON string
     * @return قائمة الأعداد / Integer list
     */
    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        return if (value.isNullOrBlank() || value == "[]") {
            emptyList()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Map Converters - محولات الخرائط
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحويل خريطة النص-النص إلى JSON
     * Convert string-string map to JSON
     * 
     * @param map الخريطة / Map
     * @return نص JSON / JSON string
     */
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        return if (map == null || map.isEmpty()) {
            "{}"
        } else {
            json.encodeToString(map)
        }
    }

    /**
     * تحويل JSON إلى خريطة نص-نص
     * Convert JSON to string-string map
     * 
     * @param value نص JSON / JSON string
     * @return الخريطة / Map
     */
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        return if (value.isNullOrBlank() || value == "{}") {
            emptyMap()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    /**
     * تحويل خريطة النص-عدد إلى JSON
     * Convert string-int map to JSON
     * 
     * @param map الخريطة / Map
     * @return نص JSON / JSON string
     */
    @TypeConverter
    fun fromStringIntMap(map: Map<String, Int>?): String {
        return if (map == null || map.isEmpty()) {
            "{}"
        } else {
            json.encodeToString(map)
        }
    }

    /**
     * تحويل JSON إلى خريطة نص-عدد
     * Convert JSON to string-int map
     * 
     * @param value نص JSON / JSON string
     * @return الخريطة / Map
     */
    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int> {
        return if (value.isNullOrBlank() || value == "{}") {
            emptyMap()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    @TypeConverter
    fun fromStringFloatMap(map: Map<String, Float>?): String {
        return if (map == null || map.isEmpty()) {
            "{}"
        } else {
            json.encodeToString(map)
        }
    }

    @TypeConverter
    fun toStringFloatMap(value: String?): Map<String, Float> {
        return if (value.isNullOrBlank() || value == "{}") {
            emptyMap()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    @TypeConverter
    fun fromStringLongMap(map: Map<String, Long>?): String {
        return if (map == null || map.isEmpty()) {
            "{}"
        } else {
            json.encodeToString(map)
        }
    }

    @TypeConverter
    fun toStringLongMap(value: String?): Map<String, Long> {
        return if (value.isNullOrBlank() || value == "{}") {
            emptyMap()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    @TypeConverter
    fun fromStringMapMapInt(map: Map<String, Map<String, Int>>?): String {
        return if (map == null || map.isEmpty()) {
            "{}"
        } else {
            json.encodeToString(map)
        }
    }

    @TypeConverter
    fun toStringMapMapInt(value: String?): Map<String, Map<String, Int>> {
        return if (value.isNullOrBlank() || value == "{}") {
            emptyMap()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    /**
     * تحويل خريطة النص-Boolean إلى JSON
     * Convert string-boolean map to JSON
     * 
     * @param map الخريطة / Map
     * @return نص JSON / JSON string
     */
    @TypeConverter
    fun fromStringBooleanMap(map: Map<String, Boolean>?): String {
        return if (map == null || map.isEmpty()) {
            "{}"
        } else {
            json.encodeToString(map)
        }
    }

    /**
     * تحويل JSON إلى خريطة نص-Boolean
     * Convert JSON to string-boolean map
     * 
     * @param value نص JSON / JSON string
     * @return الخريطة / Map
     */
    @TypeConverter
    fun toStringBooleanMap(value: String?): Map<String, Boolean> {
        return if (value.isNullOrBlank() || value == "{}") {
            emptyMap()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    /**
     * تحويل خريطة معقدة (String-Any) إلى JSON
     * Convert complex map (String-Any) to JSON
     * 
     * @param map الخريطة / Map
     * @return نص JSON / JSON string
     */
    @TypeConverter
    fun fromAnyMap(map: Map<String, Any>?): String {
        return if (map == null || map.isEmpty()) {
            "{}"
        } else {
            try {
                // تحويل Any إلى أنواع قابلة للتسلسل
                // Convert Any to serializable types
                val serializableMap = map.mapValues { (_, value) ->
                    when (value) {
                        is Int, is Long, is Float, is Double, is Boolean, is String -> value
                        is List<*> -> value
                        is Map<*, *> -> value
                        else -> value.toString()
                    }
                }
                json.encodeToString(serializableMap)
            } catch (e: Exception) {
                "{}"
            }
        }
    }

    /**
     * تحويل JSON إلى خريطة معقدة
     * Convert JSON to complex map
     * 
     * @param value نص JSON / JSON string
     * @return الخريطة / Map
     */
    @TypeConverter
    fun toAnyMap(value: String?): Map<String, Any> {
        return if (value.isNullOrBlank() || value == "{}") {
            emptyMap()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Enum Converters - محولات التعدادات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحويل ItemType إلى نص
     * Convert ItemType to string
     * 
     * @param type نوع العنصر / Item type
     * @return النص / String
     */
    @TypeConverter
    fun fromItemType(type: ItemType?): String {
        return type?.name ?: ItemType.CONSUMABLE.name
    }

    /**
     * تحويل النص إلى ItemType
     * Convert string to ItemType
     * 
     * @param value النص / String
     * @return نوع العنصر / Item type
     */
    @TypeConverter
    fun toItemType(value: String?): ItemType {
        return try {
            ItemType.valueOf(value ?: ItemType.CONSUMABLE.name)
        } catch (e: Exception) {
            ItemType.CONSUMABLE
        }
    }

    /**
     * تحويل Rarity إلى نص
     * Convert Rarity to string
     * 
     * @param rarity الندرة / Rarity
     * @return النص / String
     */
    @TypeConverter
    fun fromRarity(rarity: Rarity?): String {
        return rarity?.name ?: Rarity.COMMON.name
    }

    /**
     * تحويل النص إلى Rarity
     * Convert string to Rarity
     * 
     * @param value النص / String
     * @return الندرة / Rarity
     */
    @TypeConverter
    fun toRarity(value: String?): Rarity {
        return try {
            Rarity.valueOf(value ?: Rarity.COMMON.name)
        } catch (e: Exception) {
            Rarity.COMMON
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Complex Object Converters - محولات الكائنات المعقدة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحويل قائمة Map معقدة إلى JSON
     * Convert complex map list to JSON
     * 
     * @param list قائمة الخرائط / Map list
     * @return نص JSON / JSON string
     */
    @TypeConverter
    fun fromMapList(list: List<Map<String, Any>>?): String {
        return if (list == null || list.isEmpty()) {
            "[]"
        } else {
            try {
                json.encodeToString(list)
            } catch (e: Exception) {
                "[]"
            }
        }
    }

    /**
     * تحويل JSON إلى قائمة خرائط
     * Convert JSON to map list
     * 
     * @param value نص JSON / JSON string
     * @return قائمة الخرائط / Map list
     */
    @TypeConverter
    fun toMapList(value: String?): List<Map<String, Any>> {
        return if (value.isNullOrBlank() || value == "[]") {
            emptyList()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}