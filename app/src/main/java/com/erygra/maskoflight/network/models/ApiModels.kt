package com.erygra.maskoflight.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * ApiModels.kt - نماذج البيانات للـ API
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - يحتوي على جميع نماذج البيانات المستخدمة في API calls
 * - نماذج للاعب، الحفظ السحابي، لوحة المتصدرين
 * - نماذج الإحصائيات والإنجازات
 * 
 * المكونات الرئيسية:
 * - نماذج المستخدم والمصادقة
 * - نماذج الحفظ السحابي
 * - نماذج لوحة المتصدرين
 * - نماذج الإحصائيات والإنجازات
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

// ════════════════════════════════════════════════════════════════════════════════
// نماذج المستخدم والمصادقة
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نموذج بيانات تسجيل الدخول
 *
 * @property email البريد الإلكتروني
 * @property password كلمة المرور
 * @property deviceId معرف الجهاز
 */
@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email")
    val email: String,
    
    @Json(name = "password")
    val password: String,
    
    @Json(name = "device_id")
    val deviceId: String
)

/**
 * نموذج استجابة تسجيل الدخول
 *
 * @property userId معرف المستخدم
 * @property token رمز المصادقة
 * @property refreshToken رمز التحديث
 * @property expiresAt وقت انتهاء الرمز
 * @property user بيانات المستخدم
 */
@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "user_id")
    val userId: String,
    
    @Json(name = "token")
    val token: String,
    
    @Json(name = "refresh_token")
    val refreshToken: String,
    
    @Json(name = "expires_at")
    val expiresAt: Long,
    
    @Json(name = "user")
    val user: ApiUser
)

/**
 * نموذج بيانات المستخدم من API
 *
 * @property id معرف المستخدم
 * @property email البريد الإلكتروني
 * @property username اسم المستخدم
 * @property displayName الاسم المعروض
 * @property avatarUrl رابط الصورة الشخصية
 * @property level المستوى
 * @property createdAt تاريخ الإنشاء
 * @property lastLoginAt آخر تسجيل دخول
 */
@JsonClass(generateAdapter = true)
data class ApiUser(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "email")
    val email: String,
    
    @Json(name = "username")
    val username: String,
    
    @Json(name = "display_name")
    val displayName: String,
    
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    
    @Json(name = "level")
    val level: Int = 1,
    
    @Json(name = "created_at")
    val createdAt: Long,
    
    @Json(name = "last_login_at")
    val lastLoginAt: Long
)

// ════════════════════════════════════════════════════════════════════════════════
// نماذج الحفظ السحابي
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نموذج بيانات الحفظ السحابي
 *
 * @property saveId معرف الحفظ
 * @property userId معرف المستخدم
 * @property slotId رقم الخانة (0-2)
 * @property saveData بيانات الحفظ (JSON string)
 * @property checksum للتحقق من سلامة البيانات
 * @property version إصدار البيانات
 * @property playtime وقت اللعب بالثواني
 * @property lastSaveLocation آخر موقع حفظ
 * @property createdAt تاريخ الإنشاء
 * @property updatedAt تاريخ آخر تحديث
 */
@JsonClass(generateAdapter = true)
data class CloudSaveData(
    @Json(name = "save_id")
    val saveId: String,
    
    @Json(name = "user_id")
    val userId: String,
    
    @Json(name = "slot_id")
    val slotId: Int,
    
    @Json(name = "save_data")
    val saveData: String,
    
    @Json(name = "checksum")
    val checksum: String,
    
    @Json(name = "version")
    val version: Int,
    
    @Json(name = "playtime")
    val playtime: Long,
    
    @Json(name = "last_save_location")
    val lastSaveLocation: String,
    
    @Json(name = "created_at")
    val createdAt: Long,
    
    @Json(name = "updated_at")
    val updatedAt: Long
)

/**
 * طلب رفع حفظ سحابي
 *
 * @property slotId رقم الخانة
 * @property saveData بيانات الحفظ
 * @property checksum للتحقق
 * @property version إصدار البيانات
 * @property playtime وقت اللعب
 * @property lastSaveLocation آخر موقع
 */
@JsonClass(generateAdapter = true)
data class UploadSaveRequest(
    @Json(name = "slot_id")
    val slotId: Int,
    
    @Json(name = "save_data")
    val saveData: String,
    
    @Json(name = "checksum")
    val checksum: String,
    
    @Json(name = "version")
    val version: Int,
    
    @Json(name = "playtime")
    val playtime: Long,
    
    @Json(name = "last_save_location")
    val lastSaveLocation: String
)

/**
 * قائمة الحفوظات السحابية
 *
 * @property saves قائمة الحفوظات
 */
@JsonClass(generateAdapter = true)
data class CloudSavesList(
    @Json(name = "saves")
    val saves: List<CloudSaveData>
)

// ════════════════════════════════════════════════════════════════════════════════
// نماذج لوحة المتصدرين
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نوع لوحة المتصدرين
 */
enum class LeaderboardType {
    @Json(name = "speedrun")
    SPEEDRUN,
    
    @Json(name = "score")
    SCORE,
    
    @Json(name = "completion")
    COMPLETION,
    
    @Json(name = "boss_time")
    BOSS_TIME
}

/**
 * نموذج سجل في لوحة المتصدرين
 *
 * @property rank الترتيب
 * @property userId معرف المستخدم
 * @property username اسم المستخدم
 * @property avatarUrl صورة المستخدم
 * @property score النقاط
 * @property time الوقت (للسرعة)
 * @property completionPercentage نسبة الإنجاز
 * @property metadata بيانات إضافية
 * @property achievedAt تاريخ التحقيق
 */
@JsonClass(generateAdapter = true)
data class LeaderboardEntry(
    @Json(name = "rank")
    val rank: Int,
    
    @Json(name = "user_id")
    val userId: String,
    
    @Json(name = "username")
    val username: String,
    
    @Json(name = "avatar_url")
    val avatarUrl: String? = null,
    
    @Json(name = "score")
    val score: Long,
    
    @Json(name = "time")
    val time: Long? = null,
    
    @Json(name = "completion_percentage")
    val completionPercentage: Float? = null,
    
    @Json(name = "metadata")
    val metadata: Map<String, Any>? = null,
    
    @Json(name = "achieved_at")
    val achievedAt: Long
)

/**
 * طلب رفع نتيجة للوحة المتصدرين
 *
 * @property type نوع اللوحة
 * @property score النقاط
 * @property time الوقت
 * @property completionPercentage نسبة الإنجاز
 * @property metadata بيانات إضافية
 */
@JsonClass(generateAdapter = true)
data class SubmitScoreRequest(
    @Json(name = "type")
    val type: LeaderboardType,
    
    @Json(name = "score")
    val score: Long,
    
    @Json(name = "time")
    val time: Long? = null,
    
    @Json(name = "completion_percentage")
    val completionPercentage: Float? = null,
    
    @Json(name = "metadata")
    val metadata: Map<String, Any>? = null
)

/**
 * استجابة لوحة المتصدرين
 *
 * @property entries السجلات
 * @property userEntry سجل المستخدم الحالي
 * @property totalEntries إجمالي السجلات
 */
@JsonClass(generateAdapter = true)
data class LeaderboardResponse(
    @Json(name = "entries")
    val entries: List<LeaderboardEntry>,
    
    @Json(name = "user_entry")
    val userEntry: LeaderboardEntry? = null,
    
    @Json(name = "total_entries")
    val totalEntries: Int
)

// ════════════════════════════════════════════════════════════════════════════════
// نماذج الإحصائيات والإنجازات
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نموذج إحصائيات اللاعب
 *
 * @property totalPlaytime إجمالي وقت اللعب
 * @property totalDeaths إجمالي الوفيات
 * @property totalKills إجمالي القتلى
 * @property bossesDefeated الزعماء المهزومون
 * @property areasExplored المناطق المستكشفة
 * @property itemsCollected الأشياء المجموعة
 * @property abilitiesUnlocked القدرات المفتوحة
 * @property questsCompleted المهام المكتملة
 * @property secretsFound الأسرار المكتشفة
 */
@JsonClass(generateAdapter = true)
data class PlayerStats(
    @Json(name = "total_playtime")
    val totalPlaytime: Long,
    
    @Json(name = "total_deaths")
    val totalDeaths: Int,
    
    @Json(name = "total_kills")
    val totalKills: Int,
    
    @Json(name = "bosses_defeated")
    val bossesDefeated: List<String>,
    
    @Json(name = "areas_explored")
    val areasExplored: List<String>,
    
    @Json(name = "items_collected")
    val itemsCollected: Int,
    
    @Json(name = "abilities_unlocked")
    val abilitiesUnlocked: List<String>,
    
    @Json(name = "quests_completed")
    val questsCompleted: Int,
    
    @Json(name = "secrets_found")
    val secretsFound: Int
)

/**
 * نموذج الإنجاز
 *
 * @property id معرف الإنجاز
 * @property name الاسم
 * @property description الوصف
 * @property iconUrl رابط الأيقونة
 * @property isUnlocked هل مفتوح
 * @property unlockedAt تاريخ الفتح
 * @property progress التقدم (0-100)
 * @property category الفئة
 * @property rarity الندرة
 * @property points النقاط
 */
@JsonClass(generateAdapter = true)
data class Achievement(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "name")
    val name: String,
    
    @Json(name = "description")
    val description: String,
    
    @Json(name = "icon_url")
    val iconUrl: String,
    
    @Json(name = "is_unlocked")
    val isUnlocked: Boolean,
    
    @Json(name = "unlocked_at")
    val unlockedAt: Long? = null,
    
    @Json(name = "progress")
    val progress: Float,
    
    @Json(name = "category")
    val category: String,
    
    @Json(name = "rarity")
    val rarity: AchievementRarity,
    
    @Json(name = "points")
    val points: Int
)

/**
 * ندرة الإنجاز
 */
enum class AchievementRarity {
    @Json(name = "common")
    COMMON,
    
    @Json(name = "rare")
    RARE,
    
    @Json(name = "epic")
    EPIC,
    
    @Json(name = "legendary")
    LEGENDARY
}

/**
 * طلب فتح إنجاز
 *
 * @property achievementId معرف الإنجاز
 * @property timestamp وقت الفتح
 */
@JsonClass(generateAdapter = true)
data class UnlockAchievementRequest(
    @Json(name = "achievement_id")
    val achievementId: String,
    
    @Json(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * قائمة الإنجازات
 *
 * @property achievements الإنجازات
 * @property totalPoints إجمالي النقاط
 * @property unlockedCount عدد المفتوحة
 */
@JsonClass(generateAdapter = true)
data class AchievementsList(
    @Json(name = "achievements")
    val achievements: List<Achievement>,
    
    @Json(name = "total_points")
    val totalPoints: Int,
    
    @Json(name = "unlocked_count")
    val unlockedCount: Int
)

// ════════════════════════════════════════════════════════════════════════════════
// نماذج الأحداث والتحليلات
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نموذج حدث تحليلي
 *
 * @property eventName اسم الحدث
 * @property eventParams معاملات الحدث
 * @property timestamp الوقت
 * @property sessionId معرف الجلسة
 */
@JsonClass(generateAdapter = true)
data class AnalyticsEvent(
    @Json(name = "event_name")
    val eventName: String,
    
    @Json(name = "event_params")
    val eventParams: Map<String, Any>,
    
    @Json(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @Json(name = "session_id")
    val sessionId: String
)

/**
 * دفعة من الأحداث التحليلية
 *
 * @property events الأحداث
 * @property deviceInfo معلومات الجهاز
 */
@JsonClass(generateAdapter = true)
data class AnalyticsBatch(
    @Json(name = "events")
    val events: List<AnalyticsEvent>,
    
    @Json(name = "device_info")
    val deviceInfo: DeviceInfo
)

/**
 * معلومات الجهاز
 *
 * @property deviceId معرف الجهاز
 * @property platform المنصة
 * @property osVersion إصدار النظام
 * @property appVersion إصدار التطبيق
 * @property locale اللغة
 */
@JsonClass(generateAdapter = true)
data class DeviceInfo(
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "platform")
    val platform: String = "Android",
    
    @Json(name = "os_version")
    val osVersion: String,
    
    @Json(name = "app_version")
    val appVersion: String,
    
    @Json(name = "locale")
    val locale: String
)

// ════════════════════════════════════════════════════════════════════════════════
// نماذج الإعدادات السحابية
// ════════════════════════════════════════════════════════════════════════════════

/**
 * إعدادات اللاعب السحابية
 *
 * @property graphics إعدادات الرسوميات
 * @property audio إعدادات الصوت
 * @property controls إعدادات التحكم
 * @property language اللغة
 * @property accessibility إعدادات الوصول
 */
@JsonClass(generateAdapter = true)
data class CloudSettings(
    @Json(name = "graphics")
    val graphics: GraphicsSettings,
    
    @Json(name = "audio")
    val audio: AudioSettings,
    
    @Json(name = "controls")
    val controls: ControlSettings,
    
    @Json(name = "language")
    val language: String,
    
    @Json(name = "accessibility")
    val accessibility: AccessibilitySettings
)

@JsonClass(generateAdapter = true)
data class GraphicsSettings(
    @Json(name = "quality")
    val quality: String,
    
    @Json(name = "vsync")
    val vsync: Boolean,
    
    @Json(name = "particle_effects")
    val particleEffects: Boolean
)

@JsonClass(generateAdapter = true)
data class AudioSettings(
    @Json(name = "master_volume")
    val masterVolume: Float,
    
    @Json(name = "music_volume")
    val musicVolume: Float,
    
    @Json(name = "sfx_volume")
    val sfxVolume: Float
)

@JsonClass(generateAdapter = true)
data class ControlSettings(
    @Json(name = "vibration")
    val vibration: Boolean,
    
    @Json(name = "sensitivity")
    val sensitivity: Float
)

@JsonClass(generateAdapter = true)
data class AccessibilitySettings(
    @Json(name = "color_blind_mode")
    val colorBlindMode: String,
    
    @Json(name = "text_size")
    val textSize: Float
)