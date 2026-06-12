package com.erygra.maskoflight.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.player.AbilityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * Sanctuary System — Mask of Light (Erygra Universe)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * نظام الملاجئ (نقاط التفتيش):
 * - 4 أنواع ملاجئ (Standard, Hidden, Event, Ruined)
 * - 42 ملجأ موزع على 7 مناطق
 * - نظام حفظ/تحميل تلقائي ويدوي
 * - استشفاء كامل (HP, Energy)
 * - Fast Travel بين الملاجئ المفعّلة
 * - طقوس تطهير (تقليل FM)
 * - إعادة تعبئة أدوات محدودة
 * - تجار محليون (يظهرون أحياناً)
 * - نظام اكتشاف وفتح
 * - تكامل مع RegionManager و TransportSystem
 *
 * التوزيع:
 * - Ashen Sprawl: 8 (4 Standard, 3 Hidden, 1 Event)
 * - Veiled Archives: 6 (3 Standard, 3 Hidden, 0 Event)
 * - Hollowed Archipelago: 7 (3 Standard, 4 Hidden, 0 Event)
 * - Glassfjord Cliffs: 6 (2 Standard, 4 Hidden, 0 Event)
 * - Sunken Clockworks: 5 (3 Standard, 2 Hidden, 0 Event)
 * - Blackroot Moorlands: 6 (3 Standard, 3 Hidden, 0 Event)
 * - Luminous Chasm: 4 (2 Standard, 2 Hidden, 0 Event)
 *
 * @author Erygra Development Team
 * @version 2.0.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════════
// Sanctuary Enums
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * أنواع الملاجئ
 */
enum class SanctuaryType {
    STANDARD,       // ملجأ قياسي (ظاهر نسبياً)
    HIDDEN,         // ملجأ مخفي (يحتاج استكشاف/لغز/guardian)
    EVENT,          // ملجأ حدث (يظهر مؤقتاً)
    RUINED          // ملجأ مدمر (في المناطق القاحلة، يُكتشف بالاستكشاف)
}

/**
 * حالة الملجأ
 */
enum class SanctuaryStatus {
    UNDISCOVERED,   // غير مكتشف
    DISCOVERED,     // مكتشف لكن غير مفعّل
    ACTIVE,         // نشط ومتاح
    INACTIVE,       // غير نشط (حدث انتهى)
    CORRUPTED,      // فاسد (حدث قصصي)
    RESTORED        // مُصلح (بعد الفساد)
}

/**
 * نوع الطقس
 */
enum class RitualType {
    CLEANSE_FM,             // تطهير FM
    RESTORE_MEMORY,         // استعادة ذكرى
    UPGRADE_ABILITY,        // ترقية قدرة
    COMMUNE_WITH_PAST,      // التواصل مع الماضي (قصصي)
    SANCTIFY_WEAPON,        // تقديس سلاح
    BLESS_ARMOR,            // مباركة درع
    ENHANCE_FRAGMENT        // تعزيز شظية
}

/**
 * نوع التاجر المحلي
 */
enum class LocalMerchantType {
    GENERAL,        // عام (استهلاكيات أساسية)
    WEAPONSMITH,    // صانع أسلحة
    ARMORSMITH,     // صانع دروع
    ALCHEMIST,      // خيميائي
    CARTOGRAPHER,   // رسام خرائط
    MEMORY_TRADER,  // تاجر ذكريات
    WANDERER        // متجول (عشوائي)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Sanctuary Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ملجأ
 */
data class Sanctuary(
    val id: String,
    val name: String,
    val nameArabic: String,
    val type: SanctuaryType,
    val region: RegionType,
    val position: Offset,
    val status: SanctuaryStatus = SanctuaryStatus.UNDISCOVERED,
    
    // الاكتشاف:
    val isVisible: Boolean = true,               // هل ظاهر أم مخفي تماماً؟
    val discoveryRadius: Float = 8f,             // نطاق الاكتشاف التلقائي
    val discoveryHint: String = "",              // دليل للاكتشاف
    val discoveryHintArabic: String = "",
    
    // الفتح:
    val unlockConditions: List<UnlockCondition> = emptyList(),
    val hasGuardian: Boolean = false,            // هل يحرسه عدو؟
    val guardianEnemyId: String? = null,
    val requiresPuzzle: Boolean = false,         // هل يحتاج حل لغز؟
    val puzzleId: String? = null,
    
    // الوظائف:
    val canSave: Boolean = true,
    val canHeal: Boolean = true,
    val canRefill: Boolean = true,              // إعادة تعبئة أدوات
    val canFastTravel: Boolean = true,
    val canPerformRituals: Boolean = true,
    val availableRituals: List<RitualType> = emptyList(),
    
    // التاجر:
    val hasMerchant: Boolean = false,
    val merchantType: LocalMerchantType? = null,
    val merchantSpawnChance: Float = 0.3f,      // احتمال ظهور التاجر
    val currentMerchantPresent: Boolean = false,
    
    // Fast Travel:
    val fastTravelCost: Int = 0,                // تكلفة Fast Travel (0 = مجاني)
    val fastTravelDistance: Float = 0f,         // المسافة (للحساب)
    
    // المظهر:
    val iconColor: Color = Color(0xFFFFD54F),
    val glowIntensity: Float = 1.0f,
    val ambientSound: String = "sanctuary_hum",
    val description: String = "",
    val descriptionArabic: String = "",
    val lore: String = "",
    val loreArabic: String = "",
    
    // الإحصائيات:
    val totalVisits: Int = 0,
    val totalSaves: Int = 0,
    val totalHeals: Int = 0,
    val totalRituals: Int = 0,
    val firstDiscoveredAt: Long? = null,
    val lastVisitedAt: Long? = null
) {
    /**
     * هل الملجأ متاح للاستخدام؟
     */
    fun isAvailable(): Boolean =
        status == SanctuaryStatus.ACTIVE || status == SanctuaryStatus.RESTORED
    
    /**
     * هل يمكن Fast Travel منه؟
     */
    fun canFastTravelFrom(): Boolean =
        isAvailable() && canFastTravel
}

/**
 * طقس ملجأ
 */
data class SanctuaryRitual(
    val type: RitualType,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    
    // التكلفة:
    val coinCost: Int = 0,
    val mfCost: Int = 0,
    val itemRequirements: Map<String, Int> = emptyMap(),  // itemId -> quantity
    
    // التأثير:
    val fmReduction: Int = 0,              // تقليل FM
    val hpRestore: Float = 0f,             // استعادة HP
    val energyRestore: Float = 0f,         // استعادة Energy
    val statBoosts: Map<String, Float> = emptyMap(),  // مؤقت
    val boostDurationMs: Long = 0L,
    
    // القصة:
    val triggersDialogue: Boolean = false,
    val dialogueId: String? = null,
    val triggersMemory: Boolean = false,
    val memoryId: String? = null,
    
    // القيود:
    val maxUsesPerVisit: Int = 1,
    val cooldownMs: Long = 0L,
    val requiredLevel: Int = 1,
    val requiredFM: Int = 0                // حد أدنى من FM للاستخدام
)

/**
 * تاجر محلي
 */
data class LocalMerchant(
    val id: String,
    val name: String,
    val nameArabic: String,
    val type: LocalMerchantType,
    val sanctuaryId: String,
    val inventory: List<String> = emptyList(),  // itemIds
    val priceMultiplier: Float = 1.0f,         // معدل السعر
    val dialogue: String = "",
    val dialogueArabic: String = "",
    val appearedAt: Long = System.currentTimeMillis(),
    val staysUntil: Long? = null               // null = يبقى للأبد
) {
    /**
     * هل التاجر لا يزال موجوداً؟
     */
    fun isPresent(): Boolean =
        staysUntil == null || System.currentTimeMillis() < staysUntil
}

/**
 * زيارة ملجأ
 */
data class SanctuaryVisit(
    val sanctuaryId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionsTaken: List<String> = emptyList(),  // "save", "heal", "ritual_cleanse_fm", etc.
    val ritualsPerformed: List<RitualType> = emptyList(),
    val merchantInteracted: Boolean = false,
    val itemsPurchased: Int = 0
)

/**
 * حالة نظام الملاجئ (للحفظ/التحميل)
 */
data class SanctuarySystemState(
    val sanctuaries: Map<String, Sanctuary>,
    val discoveredSanctuaries: Set<String>,
    val activeSanctuaries: Set<String>,
    val currentSanctuaryId: String? = null,
    val visitHistory: List<SanctuaryVisit>,
    val activeMerchants: List<LocalMerchant>,
    val ritualCooldowns: Map<String, Long>,      // ritualId -> nextAvailableTime
    val totalSanctuariesFound: Int,
    val totalRitualsPerformed: Int,
    val lastSaveLocation: String? = null,
    val lastSaveTime: Long? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// Sanctuary Database
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات الملاجئ
 */
object SanctuaryDatabase {
    
    /**
     * إنشاء جميع الملاجئ (42 ملجأ)
     */
    fun createDefaultSanctuaries(): Map<String, Sanctuary> = mapOf(
        
        // ═══════════════════════════════════════════════════════════════════════
        // ASHEN SPRAWL — 8 Sanctuaries (4 Standard, 3 Hidden, 1 Event)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Standard:
        "ashen_port_beacon" to Sanctuary(
            id = "ashen_port_beacon",
            name = "Port Beacon",
            nameArabic = "منارة الميناء",
            type = SanctuaryType.STANDARD,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(20f, 40f),
            status = SanctuaryStatus.ACTIVE,  // أول ملجأ مفعّل
            isVisible = true,
            description = "A lighthouse turned sanctuary, its beam a reminder of safer times.",
            descriptionArabic = "منارة تحولت إلى ملاذ، شعاعها تذكير بأوقات أكثر أماناً.",
            hasMerchant = true,
            merchantType = LocalMerchantType.GENERAL,
            merchantSpawnChance = 0.5f,
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.RESTORE_MEMORY)
        ),
        
        "ashen_forge_shrine" to Sanctuary(
            id = "ashen_forge_shrine",
            name = "Forge Quarter Shrine",
            nameArabic = "ضريح حي الحدادة",
            type = SanctuaryType.STANDARD,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(60f, 70f),
            isVisible = true,
            description = "A small shrine among the forges, cooled by prayer.",
            descriptionArabic = "ضريح صغير بين المطارق، تبرده الصلاة.",
            hasMerchant = true,
            merchantType = LocalMerchantType.WEAPONSMITH,
            merchantSpawnChance = 0.4f,
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.SANCTIFY_WEAPON)
        ),
        
        "ashen_market_sanctuary" to Sanctuary(
            id = "ashen_market_sanctuary",
            name = "Market Square Sanctuary",
            nameArabic = "ملاذ ساحة السوق",
            type = SanctuaryType.STANDARD,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(45f, 55f),
            isVisible = true,
            description = "A quiet corner in the bustling market.",
            descriptionArabic = "ركن هادئ في السوق المزدحم.",
            hasMerchant = true,
            merchantType = LocalMerchantType.ALCHEMIST,
            merchantSpawnChance = 0.6f,
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        "ashen_council_chapel" to Sanctuary(
            id = "ashen_council_chapel",
            name = "Council Chapel",
            nameArabic = "كنيسة المجلس",
            type = SanctuaryType.STANDARD,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(80f, 50f),
            unlockConditions = listOf(
                UnlockCondition.QuestCompleted("infiltrate_council")
            ),
            description = "The Council's own sanctuary—ironic, given what they've done.",
            descriptionArabic = "ملاذ المجلس نفسه — ساخر، بالنظر إلى ما فعلوه.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST
            )
        ),
        
        // Hidden:
        "ashen_ash_pit_shrine" to Sanctuary(
            id = "ashen_ash_pit_shrine",
            name = "Ash Pit Shrine",
            nameArabic = "ضريح حفرة الرماد",
            type = SanctuaryType.HIDDEN,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(30f, 85f),
            isVisible = false,
            discoveryHint = "Listen for the hum beneath the ash.",
            discoveryHintArabic = "استمع للطنين تحت الرماد.",
            requiresPuzzle = true,
            puzzleId = "ash_pit_sequence",
            description = "Hidden beneath layers of ash, a forgotten sanctuary.",
            descriptionArabic = "مخفي تحت طبقات الرماد، ملاذ منسي.",
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.ENHANCE_FRAGMENT)
        ),
        
        "ashen_foundry_depths" to Sanctuary(
            id = "ashen_foundry_depths",
            name = "Foundry Depths Sanctuary",
            nameArabic = "ملاذ أعماق السبك",
            type = SanctuaryType.HIDDEN,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(70f, 90f),
            isVisible = false,
            hasGuardian = true,
            guardianEnemyId = "foundry_sentinel",
            description = "A sanctuary guarded by a molten sentinel.",
            descriptionArabic = "ملاذ يحرسه حارس منصهر.",
            availableRituals = listOf(RitualType.SANCTIFY_WEAPON)
        ),
        
        "ashen_shadow_alcove" to Sanctuary(
            id = "ashen_shadow_alcove",
            name = "Shadow Alcove",
            nameArabic = "حجرة الظل",
            type = SanctuaryType.HIDDEN,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(50f, 35f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.AbilityUnlocked(AbilityType.SHADOW_STEP)
            ),
            description = "Only the shadows know this place.",
            descriptionArabic = "فقط الظلال تعرف هذا المكان.",
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.UPGRADE_ABILITY)
        ),
        
        // Event:
        "ashen_memory_storm_sanctuary" to Sanctuary(
            id = "ashen_memory_storm_sanctuary",
            name = "Storm's Eye Sanctuary",
            nameArabic = "ملاذ عين العاصفة",
            type = SanctuaryType.EVENT,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(65f, 45f),
            status = SanctuaryStatus.INACTIVE,  // يظهر فقط أثناء Memory Storm
            isVisible = true,
            description = "A sanctuary that appears only during Memory Storms.",
            descriptionArabic = "ملاذ يظهر فقط أثناء عواصف الذاكرة.",
            canFastTravel = false,
            availableRituals = listOf(
                RitualType.RESTORE_MEMORY,
                RitualType.ENHANCE_FRAGMENT
            )
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // VEILED ARCHIVES — 6 Sanctuaries (3 Standard, 3 Hidden)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Standard:
        "archive_reading_alcove" to Sanctuary(
            id = "archive_reading_alcove",
            name = "Reading Alcove Sanctuary",
            nameArabic = "ملاذ حجرة القراءة",
            type = SanctuaryType.STANDARD,
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(30f, 50f),
            isVisible = true,
            description = "A quiet reading nook, preserved by silence.",
            descriptionArabic = "ركن قراءة هادئ، محفوظ بالصمت.",
            hasMerchant = true,
            merchantType = LocalMerchantType.CARTOGRAPHER,
            merchantSpawnChance = 0.5f,
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.COMMUNE_WITH_PAST)
        ),
        
        "archive_vault_rest" to Sanctuary(
            id = "archive_vault_rest",
            name = "Vault Rest",
            nameArabic = "راحة القبو",
            type = SanctuaryType.STANDARD,
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(90f, 30f),
            isVisible = true,
            description = "A secure chamber among the vaults.",
            descriptionArabic = "غرفة آمنة بين الأقبية.",
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        "archive_scribes_chapel" to Sanctuary(
            id = "archive_scribes_chapel",
            name = "Scribe's Chapel",
            nameArabic = "كنيسة الكاتب",
            type = SanctuaryType.STANDARD,
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(120f, 60f),
            isVisible = true,
            description = "Where scribes once prayed for clarity.",
            descriptionArabic = "حيث كان الكتبة يصلون من أجل الوضوح.",
            hasMerchant = true,
            merchantType = LocalMerchantType.MEMORY_TRADER,
            merchantSpawnChance = 0.3f,
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST
            )
        ),
        
        // Hidden:
        "archive_forbidden_section" to Sanctuary(
            id = "archive_forbidden_section",
            name = "Forbidden Section Sanctuary",
            nameArabic = "ملاذ القسم المحرّم",
            type = SanctuaryType.HIDDEN,
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(150f, 90f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.ItemOwned("forbidden_key")
            ),
            description = "Beyond the locked shelves, a sanctuary of dangerous knowledge.",
            descriptionArabic = "وراء الرفوف المقفلة، ملاذ للمعرفة الخطرة.",
            availableRituals = listOf(
                RitualType.RESTORE_MEMORY,
                RitualType.UPGRADE_ABILITY
            )
        ),
        
        "archive_indexers_lair" to Sanctuary(
            id = "archive_indexers_lair",
            name = "Indexer's Former Lair",
            nameArabic = "عرين المفهرس السابق",
            type = SanctuaryType.HIDDEN,
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(170f, 100f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.BossDefeated(com.erygra.maskoflight.enemy.EnemyType.THE_INDEXER)
            ),
            description = "The Indexer's lair, now purified.",
            descriptionArabic = "عرين المفهرس، مُطهّر الآن.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.ENHANCE_FRAGMENT
            )
        ),
        
        "archive_lost_ledger" to Sanctuary(
            id = "archive_lost_ledger",
            name = "Lost Ledger Sanctuary",
            nameArabic = "ملاذ السجل المفقود",
            type = SanctuaryType.HIDDEN,
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(60f, 80f),
            isVisible = false,
            requiresPuzzle = true,
            puzzleId = "ledger_sequence",
            description = "A sanctuary hidden within a massive ledger.",
            descriptionArabic = "ملاذ مخفي داخل سجل ضخم.",
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // HOLLOWED ARCHIPELAGO — 7 Sanctuaries (3 Standard, 4 Hidden)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Standard:
        "archipelago_sky_platform" to Sanctuary(
            id = "archipelago_sky_platform",
            name = "Sky Platform Sanctuary",
            nameArabic = "ملاذ منصة السماء",
            type = SanctuaryType.STANDARD,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(40f, 30f),
            isVisible = true,
            description = "A platform suspended by ropes and faith.",
            descriptionArabic = "منصة معلقة بالحبال والإيمان.",
            hasMerchant = true,
            merchantType = LocalMerchantType.GENERAL,
            merchantSpawnChance = 0.5f,
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        "archipelago_harbor_chapel" to Sanctuary(
            id = "archipelago_harbor_chapel",
            name = "Harbor Chapel",
            nameArabic = "كنيسة المرفأ",
            type = SanctuaryType.STANDARD,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(25f, 70f),
            isVisible = true,
            description = "A chapel overlooking the harbor.",
            descriptionArabic = "كنيسة تطل على المرفأ.",
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.BLESS_ARMOR)
        ),
        
        "archipelago_traders_rest" to Sanctuary(
            id = "archipelago_traders_rest",
            name = "Trader's Rest",
            nameArabic = "راحة التاجر",
            type = SanctuaryType.STANDARD,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(100f, 50f),
            isVisible = true,
            description = "Where sky traders rest between journeys.",
            descriptionArabic = "حيث يستريح تجار السماء بين الرحلات.",
            hasMerchant = true,
            merchantType = LocalMerchantType.WANDERER,
            merchantSpawnChance = 0.7f,
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        // Hidden:
        "archipelago_wind_shrine" to Sanctuary(
            id = "archipelago_wind_shrine",
            name = "Wind Shrine",
            nameArabic = "ضريح الرياح",
            type = SanctuaryType.HIDDEN,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(15f, 15f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.AbilityUnlocked(AbilityType.AIR_DASH)
            ),
            description = "A shrine at the highest point, touched by winds.",
            descriptionArabic = "ضريح في أعلى نقطة، تلمسه الرياح.",
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.UPGRADE_ABILITY)
        ),
        
        "archipelago_rooks_hideout" to Sanctuary(
            id = "archipelago_rooks_hideout",
            name = "Rook's Hidden Cache",
            nameArabic = "مخبأ روك الخفي",
            type = SanctuaryType.HIDDEN,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(180f, 65f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.QuestCompleted("gain_rook_trust")
            ),
            description = "Rook's secret sanctuary, shared only with allies.",
            descriptionArabic = "ملاذ روك السري، يُشارَك فقط مع الحلفاء.",
            hasMerchant = true,
            merchantType = LocalMerchantType.GENERAL,
            merchantSpawnChance = 1.0f,
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.SANCTIFY_WEAPON)
        ),
        
        "archipelago_rope_nest" to Sanctuary(
            id = "archipelago_rope_nest",
            name = "Rope Nest Sanctuary",
            nameArabic = "ملاذ عش الحبال",
            type = SanctuaryType.HIDDEN,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(70f, 20f),
            isVisible = false,
            requiresPuzzle = true,
            puzzleId = "rope_swing_puzzle",
            description = "A nest woven from ropes, high above.",
            descriptionArabic = "عش منسوج من الحبال، عالياً في الأعلى.",
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        "archipelago_bridge_shrine" to Sanctuary(
            id = "archipelago_bridge_shrine",
            name = "Broken Bridge Shrine",
            nameArabic = "ضريح الجسر المكسور",
            type = SanctuaryType.HIDDEN,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(140f, 85f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.BossDefeated(com.erygra.maskoflight.enemy.EnemyType.BRIDGEMASTER)
            ),
            description = "A shrine at the site of the Bridgemaster's defeat.",
            descriptionArabic = "ضريح في موقع هزيمة سيد الجسور.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY
            )
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // GLASSFJORD CLIFFS — 6 Sanctuaries (2 Standard, 4 Hidden)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Standard:
        "glass_crystal_cavern" to Sanctuary(
            id = "glass_crystal_cavern",
            name = "Crystal Cavern Sanctuary",
            nameArabic = "ملاذ كهف البلور",
            type = SanctuaryType.STANDARD,
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(50f, 60f),
            isVisible = true,
            description = "A cavern of crystalline beauty, warm despite the cold.",
            descriptionArabic = "كهف من جمال بلوري، دافئ رغم البرد.",
            hasMerchant = true,
            merchantType = LocalMerchantType.ALCHEMIST,
            merchantSpawnChance = 0.4f,
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.ENHANCE_FRAGMENT)
        ),
        
        "glass_frozen_basin" to Sanctuary(
            id = "glass_frozen_basin",
            name = "Frozen Basin Sanctuary",
            nameArabic = "ملاذ الحوض المتجمد",
            type = SanctuaryType.STANDARD,
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(156f, 48f),
            isVisible = true,
            description = "A sanctuary carved into ice, reflecting countless memories.",
            descriptionArabic = "ملاذ محفور في الجليد، يعكس ذكريات لا تعد.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST
            )
        ),
        
        // Hidden:
        "glass_mirror_chamber" to Sanctuary(
            id = "glass_mirror_chamber",
            name = "Mirror Chamber",
            nameArabic = "غرفة المرايا",
            type = SanctuaryType.HIDDEN,
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(100f, 30f),
            isVisible = false,
            requiresPuzzle = true,
            puzzleId = "mirror_reflection_puzzle",
            description = "A chamber of mirrors showing what could have been.",
            descriptionArabic = "غرفة من المرايا تُظهر ما كان يمكن أن يكون.",
            availableRituals = listOf(
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST
            )
        ),
        
        "glass_aurora_shrine" to Sanctuary(
            id = "glass_aurora_shrine",
            name = "Aurora Shrine",
            nameArabic = "ضريح الشفق",
            type = SanctuaryType.HIDDEN,
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(200f, 70f),
            isVisible = false,
            discoveryHint = "Follow the aurora's glow to its source.",
            discoveryHintArabic = "اتبع توهج الشفق إلى مصدره.",
            description = "Beneath the aurora, a sanctuary of pure light.",
            descriptionArabic = "تحت الشفق، ملاذ من نور صاف.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.UPGRADE_ABILITY,
                RitualType.ENHANCE_FRAGMENT
            )
        ),
        
        "glass_colossus_rest" to Sanctuary(
            id = "glass_colossus_rest",
            name = "Colossus Rest",
            nameArabic = "راحة العملاق",
            type = SanctuaryType.HIDDEN,
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(210f, 85f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.BossDefeated(com.erygra.maskoflight.enemy.EnemyType.FRACTURED_COLOSSUS)
            ),
            description = "Where the Colossus fell, a sanctuary rose.",
            descriptionArabic = "حيث سقط العملاق، نهض ملاذ.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.SANCTIFY_WEAPON
            )
        ),
        
        "glass_ice_heart" to Sanctuary(
            id = "glass_ice_heart",
            name = "Ice Heart Sanctuary",
            nameArabic = "ملاذ قلب الجليد",
            type = SanctuaryType.HIDDEN,
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(130f, 20f),
            isVisible = false,
            hasGuardian = true,
            guardianEnemyId = "ice_guardian",
            description = "A sanctuary at the mountain's frozen core.",
            descriptionArabic = "ملاذ في قلب الجبل المتجمد.",
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.BLESS_ARMOR)
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // SUNKEN CLOCKWORKS — 5 Sanctuaries (3 Standard, 2 Hidden)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Standard:
        "clockwork_dry_chamber" to Sanctuary(
            id = "clockwork_dry_chamber",
            name = "Dry Chamber Sanctuary",
            nameArabic = "ملاذ الغرفة الجافة",
            type = SanctuaryType.STANDARD,
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(70f, 100f),
            isVisible = true,
            description = "A rare dry chamber, protected from the floods.",
            descriptionArabic = "غرفة جافة نادرة، محمية من الفيضانات.",
            hasMerchant = true,
            merchantType = LocalMerchantType.GENERAL,
            merchantSpawnChance = 0.5f,
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        "clockwork_canal_rest" to Sanctuary(
            id = "clockwork_canal_rest",
            name = "Canal Platform Sanctuary",
            nameArabic = "ملاذ منصة القناة",
            type = SanctuaryType.STANDARD,
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(220f, 88f),
            isVisible = true,
            description = "A platform above the flowing canals.",
            descriptionArabic = "منصة فوق القنوات المتدفقة.",
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        "clockwork_gear_shrine" to Sanctuary(
            id = "clockwork_gear_shrine",
            name = "Gear Shrine",
            nameArabic = "ضريح التروس",
            type = SanctuaryType.STANDARD,
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(180f, 110f),
            isVisible = true,
            description = "A shrine among the turning gears.",
            descriptionArabic = "ضريح بين التروس الدوارة.",
            hasMerchant = true,
            merchantType = LocalMerchantType.WEAPONSMITH,
            merchantSpawnChance = 0.3f,
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.SANCTIFY_WEAPON)
        ),
        
        // Hidden:
        "clockwork_gideons_refuge" to Sanctuary(
            id = "clockwork_gideons_refuge",
            name = "Gideon's Refuge",
            nameArabic = "ملجأ جدعون",
            type = SanctuaryType.HIDDEN,
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(240f, 130f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.QuestCompleted("gideon_rescue")
            ),
            description = "Gideon's secret workshop, now a sanctuary.",
            descriptionArabic = "ورشة جدعون السرية، الآن ملاذ.",
            hasMerchant = true,
            merchantType = LocalMerchantType.ALCHEMIST,
            merchantSpawnChance = 1.0f,
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.UPGRADE_ABILITY
            )
        ),
        
        "clockwork_time_vault" to Sanctuary(
            id = "clockwork_time_vault",
            name = "Time Vault Sanctuary",
            nameArabic = "ملاذ قبو الزمن",
            type = SanctuaryType.HIDDEN,
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(260f, 95f),
            isVisible = false,
            requiresPuzzle = true,
            puzzleId = "clockwork_timing_puzzle",
            description = "A vault where time itself is kept.",
            descriptionArabic = "قبو حيث يُحفظ الزمن نفسه.",
            availableRituals = listOf(
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST,
                RitualType.ENHANCE_FRAGMENT
            )
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // BLACKROOT MOORLANDS — 6 Sanctuaries (3 Standard, 3 Hidden)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Standard:
        "moor_root_hollow" to Sanctuary(
            id = "moor_root_hollow",
            name = "Root Hollow Sanctuary",
            nameArabic = "ملاذ جوف الجذر",
            type = SanctuaryType.STANDARD,
            region = RegionType.BLACKROOT_MOORLANDS,
            position = Offset(100f, 150f),
            isVisible = true,
            description = "A hollow carved into a massive root.",
            descriptionArabic = "جوف محفور في جذر ضخم.",
            hasMerchant = true,
            merchantType = LocalMerchantType.ALCHEMIST,
            merchantSpawnChance = 0.6f,
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.BLESS_ARMOR)
        ),
        
        "moor_bog_shrine" to Sanctuary(
            id = "moor_bog_shrine",
            name = "Bog Shrine",
            nameArabic = "ضريح المستنقع",
            type = SanctuaryType.STANDARD,
            region = RegionType.BLACKROOT_MOORLANDS,
            position = Offset(274f, 136f),
            isVisible = true,
            description = "A shrine on solid ground, rare in the bog.",
            descriptionArabic = "ضريح على أرض صلبة، نادر في المستنقع.",
            availableRituals = listOf(RitualType.CLEANSE_FM)
        ),
        
        "moor_maeras_sanctuary" to Sanctuary(
            id = "moor_maeras_sanctuary",
            name = "Maera's Sanctuary",
            nameArabic = "ملاذ ماييرا",
            type = SanctuaryType.STANDARD,
            region = RegionType.BLACKROOT_MOORLANDS,
            position = Offset(250f, 170f),
            isVisible = true,
            unlockConditions = listOf(
                UnlockCondition.QuestCompleted("find_maera")
            ),
            description = "Where Maera stitches the wounded and broken.",
            descriptionArabic = "حيث تخيط ماييرا الجرحى والمكسورين.",
            hasMerchant = true,
            merchantType = LocalMerchantType.GENERAL,
            merchantSpawnChance = 1.0f,
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.BLESS_ARMOR
            )
        ),
        
        // Hidden:
        "moor_poison_grove" to Sanctuary(
            id = "moor_poison_grove",
            name = "Poison Grove Sanctuary",
            nameArabic = "ملاذ بستان السموم",
            type = SanctuaryType.HIDDEN,
            region = RegionType.BLACKROOT_MOORLANDS,
            position = Offset(150f, 180f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.ItemOwned("antidote_charm")
            ),
            description = "A sanctuary immune to the grove's poison.",
            descriptionArabic = "ملاذ محصّن ضد سموم البستان.",
            availableRituals = listOf(RitualType.CLEANSE_FM, RitualType.ENHANCE_FRAGMENT)
        ),
        
        "moor_night_stitchers_shrine" to Sanctuary(
            id = "moor_night_stitchers_shrine",
            name = "Night-Stitcher's Shrine",
            nameArabic = "ضريح الخياط الليلي",
            type = SanctuaryType.HIDDEN,
            region = RegionType.BLACKROOT_MOORLANDS,
            position = Offset(200f, 200f),
            isVisible = false,
            hasGuardian = true,
            guardianEnemyId = "night_stitcher_echo",
            description = "A shrine guarded by echoes of the past.",
            descriptionArabic = "ضريح محروس بأصداء الماضي.",
            availableRituals = listOf(
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST
            )
        ),
        
        "moor_root_titan_heart" to Sanctuary(
            id = "moor_root_titan_heart",
            name = "Root Titan's Heart",
            nameArabic = "قلب عملاق الجذور",
            type = SanctuaryType.HIDDEN,
            region = RegionType.BLACKROOT_MOORLANDS,
            position = Offset(280f, 165f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.BossDefeated(com.erygra.maskoflight.enemy.EnemyType.ROOT_TITAN)
            ),
            description = "The Titan's heart, now purified and still.",
            descriptionArabic = "قلب العملاق، مُطهّر وساكن الآن.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.UPGRADE_ABILITY
            )
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // LUMINOUS CHASM — 4 Sanctuaries (2 Standard, 2 Hidden)
        // ═══════════════════════════════════════════════════════════════════════
        
        // Standard:
        "chasm_light_nexus" to Sanctuary(
            id = "chasm_light_nexus",
            name = "Light Nexus Sanctuary",
            nameArabic = "ملاذ نقطة الضوء",
            type = SanctuaryType.STANDARD,
            region = RegionType.LUMINOUS_CHASM,
            position = Offset(150f, 200f),
            isVisible = true,
            description = "A nexus of converging light, warm and eternal.",
            descriptionArabic = "نقطة تلاقي الضوء، دافئة وأبدية.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST,
                RitualType.ENHANCE_FRAGMENT
            )
        ),
        
        "chasm_glow_pond" to Sanctuary(
            id = "chasm_glow_pond",
            name = "Glow Pond Sanctuary",
            nameArabic = "ملاذ بركة الوهج",
            type = SanctuaryType.STANDARD,
            region = RegionType.LUMINOUS_CHASM,
            position = Offset(325f, 200f),
            isVisible = true,
            description = "A pond that glows with lost memories.",
            descriptionArabic = "بركة تتوهج بالذكريات المفقودة.",
            hasMerchant = true,
            merchantType = LocalMerchantType.MEMORY_TRADER,
            merchantSpawnChance = 0.8f,
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY
            )
        ),
        
        // Hidden:
        "chasm_void_edge" to Sanctuary(
            id = "chasm_void_edge",
            name = "Void's Edge Sanctuary",
            nameArabic = "ملاذ حافة الفراغ",
            type = SanctuaryType.HIDDEN,
            region = RegionType.LUMINOUS_CHASM,
            position = Offset(250f, 250f),
            isVisible = false,
            discoveryHint = "Where light meets void, a sanctuary stands.",
            discoveryHintArabic = "حيث يلتقي الضوء بالفراغ، يقف ملاذ.",
            description = "At the edge of everything, a final sanctuary.",
            descriptionArabic = "عند حافة كل شيء، ملاذ أخير.",
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST,
                RitualType.UPGRADE_ABILITY,
                RitualType.ENHANCE_FRAGMENT
            )
        ),
        
        "chasm_echo_sanctum" to Sanctuary(
            id = "chasm_echo_sanctum",
            name = "Echo Sanctum",
            nameArabic = "قدس الصدى",
            type = SanctuaryType.HIDDEN,
            region = RegionType.LUMINOUS_CHASM,
            position = Offset(350f, 240f),
            isVisible = false,
            unlockConditions = listOf(
                UnlockCondition.BossDefeated(com.erygra.maskoflight.enemy.EnemyType.LUMINAR_HOST)
            ),
            description = "The final sanctuary, where all echoes converge.",
            descriptionArabic = "الملاذ الأخير، حيث تتلاقى كل الأصداء.",
            canFastTravel = false,  // النهاية — لا fast travel
            availableRituals = listOf(
                RitualType.CLEANSE_FM,
                RitualType.RESTORE_MEMORY,
                RitualType.COMMUNE_WITH_PAST
            )
        )
    )
    
    /**
     * الحصول على ملجأ
     */
    fun getSanctuary(id: String): Sanctuary? =
        createDefaultSanctuaries()[id]
    
    /**
     * الحصول على ملاجئ منطقة
     */
    fun getSanctuariesInRegion(region: RegionType): List<Sanctuary> =
        createDefaultSanctuaries().values.filter { it.region == region }
    
    /**
     * الحصول على ملاجئ حسب النوع
     */
    fun getSanctuariesByType(type: SanctuaryType): List<Sanctuary> =
        createDefaultSanctuaries().values.filter { it.type == type }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Ritual Database
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات الطقوس
 */
object RitualDatabase {
    
    val rituals = mapOf(
        
        RitualType.CLEANSE_FM to SanctuaryRitual(
            type = RitualType.CLEANSE_FM,
            name = "Ritual of Cleansing",
            nameArabic = "طقس التطهير",
            description = "Reduce your Forgetfulness through meditation and offerings.",
            descriptionArabic = "قلّل نسيانك عبر التأمل والقرابين.",
            coinCost = 50,
            itemRequirements = mapOf("memory_essence" to 1),
            fmReduction = 5,
            maxUsesPerVisit = 3,
            cooldownMs = 0L
        ),
        
        RitualType.RESTORE_MEMORY to SanctuaryRitual(
            type = RitualType.RESTORE_MEMORY,
            name = "Ritual of Remembrance",
            nameArabic = "طقس التذكّر",
            description = "Restore a lost memory fragment through deep meditation.",
            descriptionArabic = "استعد شظية ذاكرة مفقودة عبر تأمل عميق.",
            mfCost = 5,
            coinCost = 200,
            fmReduction = 10,
            triggersMemory = true,
            maxUsesPerVisit = 1,
            cooldownMs = 3600000L,  // 1 hour
            requiredFM = 5
        ),
        
        RitualType.UPGRADE_ABILITY to SanctuaryRitual(
            type = RitualType.UPGRADE_ABILITY,
            name = "Ritual of Enhancement",
            nameArabic = "طقس التعزيز",
            description = "Channel memory fragments to enhance an ability.",
            descriptionArabic = "وجّه شظايا الذاكرة لتعزيز قدرة.",
            mfCost = 3,
            coinCost = 300,
            itemRequirements = mapOf("ability_catalyst" to 1),
            maxUsesPerVisit = 1,
            cooldownMs = 7200000L  // 2 hours
        ),
        
        RitualType.COMMUNE_WITH_PAST to SanctuaryRitual(
            type = RitualType.COMMUNE_WITH_PAST,
            name = "Ritual of Communion",
            nameArabic = "طقس التواصل",
            description = "Commune with echoes of the past for guidance.",
            descriptionArabic = "تواصل مع أصداء الماضي للإرشاد.",
            mfCost = 2,
            triggersDialogue = true,
            maxUsesPerVisit = 1,
            cooldownMs = 0L,
            requiredFM = 3
        ),
        
        RitualType.SANCTIFY_WEAPON to SanctuaryRitual(
            type = RitualType.SANCTIFY_WEAPON,
            name = "Ritual of Sanctification",
            nameArabic = "طقس التقديس",
            description = "Sanctify your weapon with holy light.",
            descriptionArabic = "قدّس سلاحك بالنور المقدس.",
            coinCost = 150,
            itemRequirements = mapOf("sanctifying_oil" to 1),
            statBoosts = mapOf("damage" to 1.2f),
            boostDurationMs = 1800000L,  // 30 minutes
            maxUsesPerVisit = 1,
            cooldownMs = 3600000L
        ),
        
        RitualType.BLESS_ARMOR to SanctuaryRitual(
            type = RitualType.BLESS_ARMOR,
            name = "Ritual of Protection",
            nameArabic = "طقس الحماية",
            description = "Bless your armor for enhanced protection.",
            descriptionArabic = "بارك درعك لحماية معززة.",
            coinCost = 150,
            itemRequirements = mapOf("blessing_incense" to 1),
            statBoosts = mapOf("defense" to 1.3f),
            boostDurationMs = 1800000L,
            maxUsesPerVisit = 1,
            cooldownMs = 3600000L
        ),
        
        RitualType.ENHANCE_FRAGMENT to SanctuaryRitual(
            type = RitualType.ENHANCE_FRAGMENT,
            name = "Ritual of Amplification",
            nameArabic = "طقس التضخيم",
            description = "Amplify the power of collected memory fragments.",
            descriptionArabic = "ضخّم قوة شظايا الذاكرة المجموعة.",
            mfCost = 10,
            coinCost = 500,
            itemRequirements = mapOf("amplifier_crystal" to 1),
            maxUsesPerVisit = 1,
            cooldownMs = 86400000L,  // 24 hours
            requiredLevel = 10
        )
    )
    
    /**
     * الحصول على طقس
     */
    fun getRitual(type: RitualType): SanctuaryRitual? = rituals[type]
}

// ═══════════════════════════════════════════════════════════════════════════════
// Sanctuary System Manager
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مدير نظام الملاجئ
 */
class SanctuarySystemManager {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════
    
    private val _sanctuaries = MutableStateFlow(SanctuaryDatabase.createDefaultSanctuaries())
    val sanctuaries: StateFlow<Map<String, Sanctuary>> = _sanctuaries.asStateFlow()
    
    private val _discoveredSanctuaries = MutableStateFlow<Set<String>>(setOf("ashen_port_beacon"))
    val discoveredSanctuaries: StateFlow<Set<String>> = _discoveredSanctuaries.asStateFlow()
    
    private val _activeSanctuaries = MutableStateFlow<Set<String>>(setOf("ashen_port_beacon"))
    val activeSanctuaries: StateFlow<Set<String>> = _activeSanctuaries.asStateFlow()
    
    private val _currentSanctuary = MutableStateFlow<String?>(null)
    val currentSanctuary: StateFlow<String?> = _currentSanctuary.asStateFlow()
    
    private val _visitHistory = MutableStateFlow<List<SanctuaryVisit>>(emptyList())
    val visitHistory: StateFlow<List<SanctuaryVisit>> = _visitHistory.asStateFlow()
    
    private val _activeMerchants = MutableStateFlow<List<LocalMerchant>>(emptyList())
    val activeMerchants: StateFlow<List<LocalMerchant>> = _activeMerchants.asStateFlow()
    
    private val _ritualCooldowns = MutableStateFlow<Map<String, Long>>(emptyMap())
    val ritualCooldowns: StateFlow<Map<String, Long>> = _ritualCooldowns.asStateFlow()
    
    private val _totalSanctuariesFound = MutableStateFlow(1)  // Port Beacon
    val totalSanctuariesFound: StateFlow<Int> = _totalSanctuariesFound.asStateFlow()
    
    private val _totalRitualsPerformed = MutableStateFlow(0)
    val totalRitualsPerformed: StateFlow<Int> = _totalRitualsPerformed.asStateFlow()
    
    private val _lastSaveLocation = MutableStateFlow<String?>("ashen_port_beacon")
    val lastSaveLocation: StateFlow<String?> = _lastSaveLocation.asStateFlow()
    
    private val _lastSaveTime = MutableStateFlow<Long?>(null)
    val lastSaveTime: StateFlow<Long?> = _lastSaveTime.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Discovery & Activation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * اكتشاف ملجأ (عند الاقتراب)
     */
    fun discoverSanctuary(sanctuaryId: String, playerPosition: Offset) {
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return
        if (_discoveredSanctuaries.value.contains(sanctuaryId)) return
        if (!sanctuary.isVisible) return  // لا يمكن اكتشافه تلقائياً
        
        // التحقق من المسافة
        val distance = kotlin.math.sqrt(
            (sanctuary.position.x - playerPosition.x) * (sanctuary.position.x - playerPosition.x) +
            (sanctuary.position.y - playerPosition.y) * (sanctuary.position.y - playerPosition.y)
        )
        
        if (distance <= sanctuary.discoveryRadius) {
            _discoveredSanctuaries.update { it + sanctuaryId }
            _sanctuaries.update { sanctuaries ->
                sanctuaries + (sanctuaryId to sanctuary.copy(
                    status = SanctuaryStatus.DISCOVERED,
                    firstDiscoveredAt = System.currentTimeMillis()
                ))
            }
            _totalSanctuariesFound.update { it + 1 }
        }
    }
    
    /**
     * كشف ملجأ مخفي (عبر لغز/guardian/quest)
     */
    fun revealHiddenSanctuary(sanctuaryId: String) {
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return
        if (_discoveredSanctuaries.value.contains(sanctuaryId)) return
        
        _discoveredSanctuaries.update { it + sanctuaryId }
        _sanctuaries.update { sanctuaries ->
            sanctuaries + (sanctuaryId to sanctuary.copy(
                status = SanctuaryStatus.DISCOVERED,
                firstDiscoveredAt = System.currentTimeMillis()
            ))
        }
        _totalSanctuariesFound.update { it + 1 }
    }
    
    /**
     * تفعيل ملجأ
     */
    fun activateSanctuary(
        sanctuaryId: String,
        completedQuests: Set<String>,
        defeatedBosses: Set<com.erygra.maskoflight.enemy.EnemyType>,
        inventory: Set<String>,
        unlockedAbilities: Set<AbilityType>,
        playerLevel: Int,
        memoryFragments: Int
    ): Boolean {
        
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return false
        if (_activeSanctuaries.value.contains(sanctuaryId)) return false
        if (sanctuary.status != SanctuaryStatus.DISCOVERED) return false
        
        // التحقق من شروط الفتح
        val meetsConditions = sanctuary.unlockConditions.all { condition ->
            when (condition) {
                is UnlockCondition.QuestCompleted -> completedQuests.contains(condition.questId)
                is UnlockCondition.BossDefeated -> defeatedBosses.contains(condition.bossType)
                is UnlockCondition.ItemOwned -> inventory.contains(condition.itemId)
                is UnlockCondition.AbilityUnlocked -> unlockedAbilities.contains(condition.abilityType)
                is UnlockCondition.PlayerLevel -> playerLevel >= condition.level
                is UnlockCondition.MemoryFragmentsCollected -> memoryFragments >= condition.count
                else -> true
            }
        }
        
        if (!meetsConditions) return false
        
        _activeSanctuaries.update { it + sanctuaryId }
        _sanctuaries.update { sanctuaries ->
            sanctuaries + (sanctuaryId to sanctuary.copy(status = SanctuaryStatus.ACTIVE))
        }
        
        // تفعيل تاجر عشوائياً
        if (sanctuary.hasMerchant && kotlin.random.Random.nextFloat() < sanctuary.merchantSpawnChance) {
            spawnMerchant(sanctuaryId, sanctuary.merchantType!!)
        }
        
        return true
    }
    
    /**
     * دخول ملجأ
     */
    fun enterSanctuary(sanctuaryId: String) {
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return
        if (!sanctuary.isAvailable()) return
        
        _currentSanctuary.value = sanctuaryId
        
        // تحديث آخر زيارة
        _sanctuaries.update { sanctuaries ->
            sanctuaries + (sanctuaryId to sanctuary.copy(
                totalVisits = sanctuary.totalVisits + 1,
                lastVisitedAt = System.currentTimeMillis()
            ))
        }
    }
    
    /**
     * مغادرة ملجأ
     */
    fun exitSanctuary() {
        val sanctuaryId = _currentSanctuary.value
        if (sanctuaryId != null) {
            // تسجيل الزيارة
            _visitHistory.update { history ->
                (history + SanctuaryVisit(sanctuaryId)).takeLast(50)
            }
        }
        _currentSanctuary.value = null
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Functions (Heal, Save, Refill)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * استشفاء في ملجأ
     */
    fun heal(sanctuaryId: String): Boolean {
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return false
        if (!sanctuary.canHeal) return false
        if (_currentSanctuary.value != sanctuaryId) return false
        
        _sanctuaries.update { sanctuaries ->
            sanctuaries + (sanctuaryId to sanctuary.copy(totalHeals = sanctuary.totalHeals + 1))
        }
        
        return true
    }
    
    /**
     * حفظ في ملجأ
     */
    fun save(sanctuaryId: String): Boolean {
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return false
        if (!sanctuary.canSave) return false
        if (_currentSanctuary.value != sanctuaryId) return false
        
        _sanctuaries.update { sanctuaries ->
            sanctuaries + (sanctuaryId to sanctuary.copy(totalSaves = sanctuary.totalSaves + 1))
        }
        
        _lastSaveLocation.value = sanctuaryId
        _lastSaveTime.value = System.currentTimeMillis()
        
        return true
    }
    
    /**
     * إعادة تعبئة أدوات
     */
    fun refill(sanctuaryId: String): Boolean {
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return false
        if (!sanctuary.canRefill) return false
        if (_currentSanctuary.value != sanctuaryId) return false
        
        return true
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Rituals
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من إمكانية أداء طقس
     */
    fun canPerformRitual(
        sanctuaryId: String,
        ritualType: RitualType,
        currency: Int,
        mf: Int,
        inventory: Set<String>,
        playerLevel: Int,
        fm: Int
    ): Pair<Boolean, String> {
        
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return Pair(false, "Sanctuary not found")
        if (_currentSanctuary.value != sanctuaryId) return Pair(false, "Not at sanctuary")
        if (!sanctuary.canPerformRituals) return Pair(false, "Rituals not available")
        if (!sanctuary.availableRituals.contains(ritualType)) return Pair(false, "Ritual not available here")
        
        val ritual = RitualDatabase.getRitual(ritualType) ?: return Pair(false, "Ritual not found")
        
        // التحقق من المستوى
        if (playerLevel < ritual.requiredLevel) {
            return Pair(false, "Level ${ritual.requiredLevel} required")
        }
        
        // التحقق من FM
        if (fm < ritual.requiredFM) {
            return Pair(false, "Forgetfulness too low")
        }
        
        // التحقق من العملة
        if (currency < ritual.coinCost) {
            return Pair(false, "Insufficient coins")
        }
        
        // التحقق من MF
        if (mf < ritual.mfCost) {
            return Pair(false, "Insufficient memory fragments")
        }
        
        // التحقق من العناصر
        if (!ritual.itemRequirements.all { (itemId, quantity) ->
            inventory.contains(itemId)  // TODO: check quantity
        }) {
            return Pair(false, "Missing required items")
        }
        
        // التحقق من Cooldown
        val cooldownKey = "${sanctuaryId}_${ritualType.name}"
        val nextAvailable = _ritualCooldowns.value[cooldownKey] ?: 0L
        if (System.currentTimeMillis() < nextAvailable) {
            val remainingMs = nextAvailable - System.currentTimeMillis()
            return Pair(false, "On cooldown for ${remainingMs / 1000}s")
        }
        
        return Pair(true, "")
    }
    
    /**
     * أداء طقس
     */
    fun performRitual(sanctuaryId: String, ritualType: RitualType): Boolean {
        val sanctuary = _sanctuaries.value[sanctuaryId] ?: return false
        val ritual = RitualDatabase.getRitual(ritualType) ?: return false
        
        // تحديث الإحصائيات
        _sanctuaries.update { sanctuaries ->
            sanctuaries + (sanctuaryId to sanctuary.copy(
                totalRituals = sanctuary.totalRituals + 1
            ))
        }
        
        _totalRitualsPerformed.update { it + 1 }
        
        // تعيين Cooldown
        if (ritual.cooldownMs > 0) {
            val cooldownKey = "${sanctuaryId}_${ritualType.name}"
            _ritualCooldowns.update { cooldowns ->
                cooldowns + (cooldownKey to (System.currentTimeMillis() + ritual.cooldownMs))
            }
        }
        
        return true
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Fast Travel
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على ملاجئ Fast Travel المتاحة
     */
    fun getAvailableFastTravelDestinations(fromSanctuaryId: String): List<Sanctuary> {
        val fromSanctuary = _sanctuaries.value[fromSanctuaryId] ?: return emptyList()
        if (!fromSanctuary.canFastTravelFrom()) return emptyList()
        
        return _activeSanctuaries.value
            .mapNotNull { _sanctuaries.value[it] }
            .filter { it.canFastTravel && it.id != fromSanctuaryId }
    }
    
    /**
     * Fast Travel
     */
    fun fastTravel(fromSanctuaryId: String, toSanctuaryId: String, currency: Int): Boolean {
        val toSanctuary = _sanctuaries.value[toSanctuaryId] ?: return false
        
        // التحقق من التكلفة
        if (currency < toSanctuary.fastTravelCost) return false
        
        return true
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Merchants
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * إظهار تاجر
     */
    private fun spawnMerchant(sanctuaryId: String, type: LocalMerchantType) {
        val merchant = LocalMerchant(
            id = "merchant_${sanctuaryId}_${System.currentTimeMillis()}",
            name = "Local ${type.name} Merchant",
            nameArabic = "تاجر ${type.name} محلي",
            type = type,
            sanctuaryId = sanctuaryId,
            staysUntil = System.currentTimeMillis() + 3600000L  // 1 hour
        )
        
        _activeMerchants.update { it + merchant }
    }
    
    /**
     * الحصول على تاجر في ملجأ
     */
    fun getMerchantAtSanctuary(sanctuaryId: String): LocalMerchant? =
        _activeMerchants.value.firstOrNull { it.sanctuaryId == sanctuaryId && it.isPresent() }
    
    /**
     * تحديث التجار (إزالة المنتهين)
     */
    fun updateMerchants() {
        _activeMerchants.update { merchants ->
            merchants.filter { it.isPresent() }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على ملجأ
     */
    fun getSanctuary(id: String): Sanctuary? = _sanctuaries.value[id]
    
    /**
     * الحصول على ملاجئ منطقة
     */
    fun getSanctuariesInRegion(region: RegionType): List<Sanctuary> =
        _sanctuaries.value.values.filter { it.region == region }
    
    /**
     * الحصول على أقرب ملجأ نشط
     */
    fun getNearestActiveSanctuary(position: Offset, region: RegionType): Sanctuary? {
        val activeSanctuariesInRegion = _activeSanctuaries.value
            .mapNotNull { _sanctuaries.value[it] }
            .filter { it.region == region }
        
        return activeSanctuariesInRegion.minByOrNull { sanctuary ->
            val dx = sanctuary.position.x - position.x
            val dy = sanctuary.position.y - position.y
            kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Save/Load
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ الحالة
     */
    fun saveState(): SanctuarySystemState {
        return SanctuarySystemState(
            sanctuaries = _sanctuaries.value,
            discoveredSanctuaries = _discoveredSanctuaries.value,
            activeSanctuaries = _activeSanctuaries.value,
            currentSanctuaryId = _currentSanctuary.value,
            visitHistory = _visitHistory.value,
            activeMerchants = _activeMerchants.value,
            ritualCooldowns = _ritualCooldowns.value,
            totalSanctuariesFound = _totalSanctuariesFound.value,
            totalRitualsPerformed = _totalRitualsPerformed.value,
            lastSaveLocation = _lastSaveLocation.value,
            lastSaveTime = _lastSaveTime.value
        )
    }
    
    /**
     * تحميل الحالة
     */
    fun loadState(state: SanctuarySystemState) {
        _sanctuaries.value = state.sanctuaries
        _discoveredSanctuaries.value = state.discoveredSanctuaries
        _activeSanctuaries.value = state.activeSanctuaries
        _currentSanctuary.value = state.currentSanctuaryId
        _visitHistory.value = state.visitHistory
        _activeMerchants.value = state.activeMerchants
        _ritualCooldowns.value = state.ritualCooldowns
        _totalSanctuariesFound.value = state.totalSanctuariesFound
        _totalRitualsPerformed.value = state.totalRitualsPerformed
        _lastSaveLocation.value = state.lastSaveLocation
        _lastSaveTime.value = state.lastSaveTime
    }
    
    /**
     * إعادة تعيين
     */
    fun reset() {
        _sanctuaries.value = SanctuaryDatabase.createDefaultSanctuaries()
        _discoveredSanctuaries.value = setOf("ashen_port_beacon")
        _activeSanctuaries.value = setOf("ashen_port_beacon")
        _currentSanctuary.value = null
        _visitHistory.value = emptyList()
        _activeMerchants.value = emptyList()
        _ritualCooldowns.value = emptyMap()
        _totalSanctuariesFound.value = 1
        _totalRitualsPerformed.value = 0
        _lastSaveLocation.value = "ashen_port_beacon"
        _lastSaveTime.value = null
    }
}