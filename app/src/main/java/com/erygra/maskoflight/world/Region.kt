package com.erygra.maskoflight.world

import androidx.compose.ui.graphics.Color
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.enemy.EnemyType
import com.erygra.maskoflight.engine.ParticleType
import com.erygra.maskoflight.player.AbilityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.*
import kotlin.math.min

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * Region System — Mask of Light (Erygra Universe)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * نظام شامل لإدارة المناطق في عالم إريغرا:
 * - 7 مناطق متصلة مع خصائص بيئية فريدة
 * - نظام Graph للتنقل بين المناطق
 * - شروط فتح المناطق
 * - إعدادات بصرية وصوتية لكل منطقة
 * - نظام اكتشاف المناطق
 * - إحصائيات وتقدم
 *
 * المناطق:
 * 1. Ashen Sprawl — المدينة الرمادية (نار، رماد، صناعة)
 * 2. Veiled Archives — الأرشيفات المحجوبة (معرفة، غبار، صمت)
 * 3. Hollowed Archipelago — الأرخبيل المجوف (رياح، حبال، جسور)
 * 4. Glassfjord Cliffs — منحدرات الزجاج (ثلج، بلور، انعكاس)
 * 5. Sunken Clockworks — الساعات الغارقة (مياه، تروس، ميكانيكا)
 * 6. Blackroot Moorlands — مستنقعات الجذور (نباتات، سموم، ضباب)
 * 7. Luminous Chasm — الهاوية المضيئة (ضوء، فراغ، ذكريات)
 *
 * @author Erygra Development Team
 * @version 2.0.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════════
// Region Enums
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * أنواع المناطق في العالم
 */
enum class RegionType {
    ASHEN_SPRAWL,           // المدينة الرمادية — منطقة البداية
    VEILED_ARCHIVES,        // الأرشيفات المحجوبة — مكتبة قديمة
    HOLLOWED_ARCHIPELAGO,   // الأرخبيل المجوف — جزر معلقة
    GLASSFJORD_CLIFFS,      // منحدرات الزجاج — جبال جليدية بلورية
    SUNKEN_CLOCKWORKS,      // الساعات الغارقة — مدينة آلية غارقة
    BLACKROOT_MOORLANDS,    // مستنقعات الجذور — أراضي سامة
    LUMINOUS_CHASM          // الهاوية المضيئة — منطقة نهائية
}

/**
 * أنواع الطقس في المناطق
 */
enum class WeatherType {
    CLEAR,          // صافٍ
    ASH_FALL,       // سقوط الرماد
    HEAT_WAVES,     // موجات حر
    DUST,           // غبار
    WIND,           // رياح
    LIGHT_RAIN,     // مطر خفيف
    SNOW,           // ثلج
    ICE_STORM,      // عاصفة جليدية
    STEAM,          // بخار
    FOG,            // ضباب
    DRIZZLE,        // رذاذ
    GLOWING_PARTICLES  // جسيمات مضيئة
}

// ═══════════════════════════════════════════════════════════════════════════════
// UI & State models
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * أوضاع الإضاءة
 */
enum class LightingMode {
    BRIGHT,     // مشرق
    DIM,        // خافت
    DARK,       // مظلم
    DYNAMIC     // ديناميكي (يتغير)
}

/**
 * أنواع الاتصال بين المناطق
 */
enum class ConnectionType {
    DOOR,       // باب
    GATE,       // بوابة
    LADDER,     // سلم
    ROPE,       // حبل
    TELEPORT,   // انتقال فوري
    FERRY,      // عبارة
    BRIDGE      // جسر
}

// ═══════════════════════════════════════════════════════════════════════════════
// Unlock Conditions
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * شروط فتح المناطق أو الاتصالات
 */
sealed class UnlockCondition {
    /**
     * مستوى اللاعب
     */
    data class PlayerLevel(val level: Int) : UnlockCondition()
    
    /**
     * إكمال مهمة
     */
    data class QuestCompleted(val questId: String) : UnlockCondition()
    
    /**
     * هزيمة زعيم
     */
    data class BossDefeated(val bossType: EnemyType) : UnlockCondition()
    
    /**
     * امتلاك عنصر
     */
    data class ItemOwned(val itemId: String) : UnlockCondition()
    
    /**
     * فتح قدرة
     */
    data class AbilityUnlocked(val abilityType: AbilityType) : UnlockCondition()
    
    /**
     * جمع شظايا ذاكرة
     */
    data class MemoryFragmentsCollected(val count: Int) : UnlockCondition()
    
    /**
     * الحصول على القناع
     */
    object MaskObtained : UnlockCondition()
    
    /**
     * فتح منطقة أخرى
     */
    data class RegionUnlocked(val region: RegionType) : UnlockCondition()
    
    /**
     * اكتشاف نسبة معينة من منطقة
     */
    data class RegionDiscoveryPercentage(val region: RegionType, val percentage: Float) : UnlockCondition()
    
    /**
     * امتلاك عملة معينة
     */
    data class CurrencyAmount(val amount: Int) : UnlockCondition()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Region Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * لوحة الألوان الخاصة بمنطقة
 */
data class RegionColorPalette(
    val primary: Color,      // اللون الرئيسي
    val secondary: Color,    // اللون الثانوي
    val tertiary: Color,     // اللون الثالث
    val background: Color,   // لون الخلفية
    val foreground: Color,   // لون المنصات والأجسام
    val highlight: Color,    // لون التمييز (للأشياء التفاعلية)
    val danger: Color        // لون الخطر
)

/**
 * نمط الطقس في منطقة
 */
data class WeatherPattern(
    val type: WeatherType,
    val probability: Float,      // احتمال الحدوث (0.0-1.0)
    val durationMinMs: Long,      // الحد الأدنى للمدة
    val durationMaxMs: Long,      // الحد الأقصى للمدة
    val transitionTimeMs: Long,   // وقت الانتقال
    val intensity: Float          // الشدة (0.0-1.0)
)

/**
 * إعدادات البيئة الطبيعية
 */
data class BiomeConfig(
    val region: RegionType,
    
    // فيزياء:
    val gravityModifier: Float = 1.0f,          // معدل الجاذبية (1.0 = عادي)
    val airResistanceModifier: Float = 1.0f,    // معدل مقاومة الهواء
    val waterLevel: Float? = null,              // مستوى الماء (إن وجد)
    
    // تأثيرات بيئية:
    val particleEffects: List<ParticleType> = emptyList(),
    val particleSpawnRate: Float = 1.0f,        // معدل ظهور الجسيمات
    val lightingMode: LightingMode = LightingMode.BRIGHT,
    
    // معدلات اللعب:
    val visibilityRange: Float = 1000f,         // مدى الرؤية
    val jumpHeightModifier: Float = 1.0f,       // معدل ارتفاع القفز
    val movementSpeedModifier: Float = 1.0f,    // معدل سرعة الحركة
    
    // مخاطر:
    val hasLava: Boolean = false,
    val hasPoisonWater: Boolean = false,
    val hasElectricity: Boolean = false,
    val hasFallingDebris: Boolean = false,
    
    // خصائص إضافية:
    val hasWind: Boolean = false,
    val windStrength: Float = 0f,
    val windDirectionDegrees: Float = 0f,
    val hasCurrents: Boolean = false,
    val currentStrength: Float = 0f
)

/**
 * اتصال بين منطقتين
 */
data class RegionConnection(
    val fromRegion: RegionType,
    val toRegion: RegionType,
    val connectionType: ConnectionType,
    val transitionPoint: Pair<Float, Float>,    // نقطة الانتقال (x, y)
    val isOneWay: Boolean = false,              // هل الاتصال في اتجاه واحد؟
    val unlockCondition: UnlockCondition? = null,
    val isDiscovered: Boolean = false,          // هل تم اكتشاف هذا الاتصال؟
    val travelTimeMs: Long = 3000L,             // وقت السفر (للرسوم المتحركة)
    val travelCost: Int = 0                     // تكلفة السفر (عملة)
)

/**
 * بيانات منطقة كاملة
 */
data class RegionData(
    val type: RegionType,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    
    // إعدادات بصرية:
    val colorPalette: RegionColorPalette,
    val ambientLight: Color,
    val fogColor: Color,
    val fogDensity: Float,                      // كثافة الضباب (0.0-1.0)
    val skyboxTexture: String,
    
    // بيئة:
    val defaultWeather: WeatherType,
    val weatherPatterns: List<WeatherPattern>,
    val hazards: List<HazardType>,
    val musicTheme: String,
    val ambientSounds: List<String>,
    
    // لعب:
    val recommendedLevel: Int,
    val difficultyMultiplier: Float,            // معدل الصعوبة (1.0 = عادي)
    val enemyTypes: List<EnemyType>,
    val bossType: EnemyType? = null,
    
    // الاتصال:
    val connectedRegions: List<RegionType>,
    val entrancePoints: List<Pair<Float, Float>>,   // نقاط الدخول
    val exitPoints: List<Pair<Float, Float>>,       // نقاط الخروج
    
    // الفتح:
    val isUnlockedByDefault: Boolean = false,
    val unlockConditions: List<UnlockCondition> = emptyList(),
    
    // الشخصيات والمتاجر:
    val npcs: List<String> = emptyList(),
    val shops: List<String> = emptyList(),
    
    // القابلة للجمع:
    val totalMemoryFragments: Int,
    val totalLoreObjects: Int,
    val totalSanctuaries: Int,
    val totalSecrets: Int,
    
    // القصة:
    val lore: String,
    val loreArabic: String,
    
    // الحجم:
    val widthTiles: Int,
    val heightTiles: Int,
    val totalAreas: Int                         // عدد المناطق الفرعية
)

/**
 * حالة اكتشاف منطقة
 */
data class RegionDiscovery(
    val region: RegionType,
    val isUnlocked: Boolean = false,
    val isDiscovered: Boolean = false,
    val discoveredAreas: Set<String> = emptySet(),      // IDs المناطق المكتشفة
    val totalAreas: Int = 0,
    val collectedFragments: Int = 0,
    val totalFragments: Int = 0,
    val foundLoreObjects: Int = 0,
    val totalLoreObjects: Int = 0,
    val activatedSanctuaries: Int = 0,
    val totalSanctuaries: Int = 0,
    val foundSecrets: Int = 0,
    val totalSecrets: Int = 0,
    val defeatedBoss: Boolean = false,
    val completedQuests: Set<String> = emptySet(),
    val firstVisitTime: Long? = null,
    val totalTimeSpentMs: Long = 0L
) {
    /**
     * نسبة الاكتشاف (0.0-1.0)
     */
    val discoveryPercentage: Float
        get() = if (totalAreas > 0) discoveredAreas.size.toFloat() / totalAreas else 0f
    
    /**
     * نسبة الإكمال الكلي (0.0-1.0)
     */
    val completionPercentage: Float
        get() {
            val fragmentsPercent = if (totalFragments > 0) collectedFragments.toFloat() / totalFragments else 0f
            val lorePercent = if (totalLoreObjects > 0) foundLoreObjects.toFloat() / totalLoreObjects else 0f
            val sanctuariesPercent = if (totalSanctuaries > 0) activatedSanctuaries.toFloat() / totalSanctuaries else 0f
            val secretsPercent = if (totalSecrets > 0) foundSecrets.toFloat() / totalSecrets else 0f
            val bossPercent = if (defeatedBoss) 1f else 0f
            
            return (discoveryPercentage + fragmentsPercent + lorePercent + sanctuariesPercent + 
                    secretsPercent + bossPercent) / 6f
        }
}

/**
 * حالة مدير المناطق (للحفظ/التحميل)
 */
data class RegionManagerState(
    val currentRegion: RegionType,
    val discoveries: Map<RegionType, RegionDiscovery>,
    val unlockedConnections: Set<Pair<RegionType, RegionType>>,
    val visitHistory: List<Pair<RegionType, Long>>,    // (region, timestamp)
    val totalRegionsUnlocked: Int,
    val totalRegionsCompleted: Int
)

// ═══════════════════════════════════════════════════════════════════════════════
// Region Database
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات المناطق — تعريف جميع المناطق السبع
 */
object RegionDatabase {
    
    /**
     * جميع المناطق معرّفة بالكامل
     */
    val regions: Map<RegionType, RegionData> = mapOf(
        
        // ═══════════════════════════════════════════════════════════════════════════
        // 1. ASHEN SPRAWL — المدينة الرمادية
        // ═══════════════════════════════════════════════════════════════════════════
        RegionType.ASHEN_SPRAWL to RegionData(
            type = RegionType.ASHEN_SPRAWL,
            name = "Ashen Sprawl",
            nameArabic = "الامتداد الرمادي",
            description = "A vast industrial city buried in ash and smoke, where scavengers " +
                    "hunt for fragments of memory among the ruins.",
            descriptionArabic = "مدينة صناعية شاسعة مدفونة في الرماد والدخان، حيث يبحث الزبالون " +
                    "عن شظايا الذاكرة بين الأنقاض.",
            
            colorPalette = RegionColorPalette(
                primary = Color(0xFFD32F2F),        // أحمر داكن
                secondary = Color(0xFFFF6F00),      // برتقالي
                tertiary = Color(0xFF424242),       // رمادي داكن
                background = Color(0xFF212121),     // أسود رمادي
                foreground = Color(0xFF616161),     // رمادي
                highlight = Color(0xFFFF9800),      // برتقالي فاتح
                danger = Color(0xFFFF5722)          // برتقالي محمر
            ),
            
            ambientLight = Color(0xFF8D6E63),
            fogColor = Color(0xFF5D4037),
            fogDensity = 0.6f,
            skyboxTexture = "ashen_sky",
            
            defaultWeather = WeatherType.ASH_FALL,
            weatherPatterns = listOf(
                WeatherPattern(
                    type = WeatherType.ASH_FALL,
                    probability = 0.7f,
                    durationMinMs = 120000L,
                    durationMaxMs = 300000L,
                    transitionTimeMs = 10000L,
                    intensity = 0.8f
                ),
                WeatherPattern(
                    type = WeatherType.HEAT_WAVES,
                    probability = 0.3f,
                    durationMinMs = 60000L,
                    durationMaxMs = 180000L,
                    transitionTimeMs = 5000L,
                    intensity = 0.6f
                )
            ),
            
            hazards = listOf(HazardType.FIRE, HazardType.LAVA),
            musicTheme = "ashen_sprawl_theme",
            ambientSounds = listOf("ash_wind", "distant_machinery", "forge_hammers", "crackling_fire"),
            
            recommendedLevel = 1,
            difficultyMultiplier = 1.0f,
            enemyTypes = listOf(
                EnemyType.SCRAB_SCAVENGER,
                EnemyType.ASHWARDEN,
                EnemyType.RAG_CHILDREN,
                EnemyType.COUNCIL_ENFORCER
            ),
            bossType = EnemyType.PYRE_HARROW,
            
            connectedRegions = listOf(RegionType.VEILED_ARCHIVES, RegionType.HOLLOWED_ARCHIPELAGO),
            entrancePoints = listOf(
                Pair(10f, 50f),     // Main gate
                Pair(80f, 30f),     // Side entrance
                Pair(45f, 70f)      // Underground tunnel
            ),
            exitPoints = listOf(
                Pair(150f, 50f),    // To Archives
                Pair(50f, 10f)      // To Archipelago
            ),
            
            isUnlockedByDefault = true,
            unlockConditions = emptyList(),
            
            npcs = listOf("Edda", "Marrek", "Soren"),
            shops = listOf("Port_Of_Ash_Shop", "Forge_Quarter_Shop"),
            
            totalMemoryFragments = 15,
            totalLoreObjects = 8,
            totalSanctuaries = 8,
            totalSecrets = 5,
            
            lore = "Once a thriving industrial hub, Ashen Sprawl fell into decay after " +
                    "the Memory Purge. Now, its citizens cling to scraps of the past, " +
                    "trading memories like currency in the shadow of the Council's rule.",
            loreArabic = "كانت ذات يوم مركزاً صناعياً مزدهراً، سقط الامتداد الرمادي في الاضمحلال " +
                    "بعد تطهير الذاكرة. الآن، يتشبث سكانها ببقايا الماضي، يتاجرون بالذكريات " +
                    "كعملة في ظل حكم المجلس.",
            
            widthTiles = 200,
            heightTiles = 100,
            totalAreas = 25
        ),
        
        // ═══════════════════════════════════════════════════════════════════════════
        // 2. VEILED ARCHIVES — الأرشيفات المحجوبة
        // ═══════════════════════════════════════════════════════════════════════════
        RegionType.VEILED_ARCHIVES to RegionData(
            type = RegionType.VEILED_ARCHIVES,
            name = "Veiled Archives",
            nameArabic = "الأرشيفات المحجوبة",
            description = "An endless library of forgotten knowledge, where shelves shift " +
                    "and books whisper secrets to those who listen.",
            descriptionArabic = "مكتبة لا نهائية من المعرفة المنسية، حيث تتحرك الرفوف وتهمس " +
                    "الكتب بأسرار لمن يستمع.",
            
            colorPalette = RegionColorPalette(
                primary = Color(0xFF1976D2),        // أزرق
                secondary = Color(0xFF90CAF9),      // أزرق فاتح
                tertiary = Color(0xFF424242),       // رمادي
                background = Color(0xFF263238),     // رمادي أزرق داكن
                foreground = Color(0xFF546E7A),     // رمادي أزرق
                highlight = Color(0xFF64B5F6),      // أزرق سماوي
                danger = Color(0xFF37474F)          // رمادي داكن
            ),
            
            ambientLight = Color(0xFF607D8B),
            fogColor = Color(0xFF455A64),
            fogDensity = 0.4f,
            skyboxTexture = "archive_ceiling",
            
            defaultWeather = WeatherType.DUST,
            weatherPatterns = listOf(
                WeatherPattern(
                    type = WeatherType.DUST,
                    probability = 0.8f,
                    durationMinMs = 180000L,
                    durationMaxMs = 600000L,
                    transitionTimeMs = 15000L,
                    intensity = 0.5f
                )
            ),
            
            hazards = listOf(HazardType.FALLING_BOOKS, HazardType.INK_POOLS),
            musicTheme = "veiled_archives_theme",
            ambientSounds = listOf("page_turning", "whispers", "clock_ticking", "distant_footsteps"),
            
            recommendedLevel = 5,
            difficultyMultiplier = 1.2f,
            enemyTypes = listOf(
                EnemyType.PAGE_SCRAPER,
                EnemyType.VAULT_SENTINEL,
                EnemyType.ECHO_SHADE,
                EnemyType.LEDGER_WARDEN
            ),
            bossType = EnemyType.THE_INDEXER,
            
            connectedRegions = listOf(RegionType.ASHEN_SPRAWL, RegionType.GLASSFJORD_CLIFFS),
            entrancePoints = listOf(
                Pair(20f, 60f),     // From Ashen Sprawl
                Pair(170f, 40f)     // Hidden entrance
            ),
            exitPoints = listOf(
                Pair(180f, 60f)     // To Glassfjord
            ),
            
            isUnlockedByDefault = false,
            unlockConditions = listOf(
                UnlockCondition.PlayerLevel(3),
                UnlockCondition.ItemOwned("archive_key")
            ),
            
            npcs = listOf("Lysa", "The_Silent_Scribe"),
            shops = listOf("Vault_Wharf_Shop"),
            
            totalMemoryFragments = 20,
            totalLoreObjects = 12,
            totalSanctuaries = 6,
            totalSecrets = 8,
            
            lore = "The Archives hold all that was lost in the Purge—names, stories, histories. " +
                    "But the Regent guards this knowledge jealously, using it to control " +
                    "who remembers and who is forgotten.",
            loreArabic = "تحتفظ الأرشيفات بكل ما ضاع في التطهير — أسماء، قصص، تواريخ. " +
                    "لكن الوصي يحرس هذه المعرفة بغيرة، يستخدمها للتحكم في من يتذكر ومن يُنسى.",
            
            widthTiles = 220,
            heightTiles = 120,
            totalAreas = 30
        ),
        
        // ═══════════════════════════════════════════════════════════════════════════
        // 3. HOLLOWED ARCHIPELAGO — الأرخبيل المجوف
        // ═══════════════════════════════════════════════════════════════════════════
        RegionType.HOLLOWED_ARCHIPELAGO to RegionData(
            type = RegionType.HOLLOWED_ARCHIPELAGO,
            name = "Hollowed Archipelago",
            nameArabic = "الأرخبيل المجوف",
            description = "Floating islands connected by rope bridges and gondolas, where sky " +
                    "pirates trade stolen memories and the wind carries forgotten songs.",
            descriptionArabic = "جزر عائمة متصلة بجسور حبلية وجندولات، حيث يتاجر قراصنة السماء " +
                    "بالذكريات المسروقة وتحمل الرياح أغاني منسية.",
            
            colorPalette = RegionColorPalette(
                primary = Color(0xFF0288D1),        // أزرق سماوي
                secondary = Color(0xFF81D4FA),      // أزرق فاتح جداً
                tertiary = Color(0xFFFDD835),       // أصفر ذهبي
                background = Color(0xFFE1F5FE),     // أزرق شاحب
                foreground = Color(0xFF4FC3F7),     // أزرق متوسط
                highlight = Color(0xFFFFEB3B),      // أصفر
                danger = Color(0xFF01579B)          // أزرق داكن جداً
            ),
            
            ambientLight = Color(0xFF80DEEA),
            fogColor = Color(0xFFB2EBF2),
            fogDensity = 0.2f,
            skyboxTexture = "sky_islands",
            
            defaultWeather = WeatherType.WIND,
            weatherPatterns = listOf(
                WeatherPattern(
                    type = WeatherType.WIND,
                    probability = 0.6f,
                    durationMinMs = 150000L,
                    durationMaxMs = 400000L,
                    transitionTimeMs = 8000L,
                    intensity = 0.7f
                ),
                WeatherPattern(
                    type = WeatherType.LIGHT_RAIN,
                    probability = 0.4f,
                    durationMinMs = 90000L,
                    durationMaxMs = 200000L,
                    transitionTimeMs = 12000L,
                    intensity = 0.4f
                )
            ),
            
            hazards = listOf(HazardType.FALLING, HazardType.ROPE_TRAPS),
            musicTheme = "archipelago_theme",
            ambientSounds = listOf("wind_gusts", "rope_creaking", "distant_bells", "seagulls"),
            
            recommendedLevel = 8,
            difficultyMultiplier = 1.3f,
            enemyTypes = listOf(
                EnemyType.ROPE_CROAKER,
                EnemyType.DRIFT_KNIGHT,
                EnemyType.BARGAIN_PIRATE,
                EnemyType.SKY_SCAVENGER
            ),
            bossType = EnemyType.BRIDGEMASTER,
            
            connectedRegions = listOf(RegionType.ASHEN_SPRAWL, RegionType.SUNKEN_CLOCKWORKS),
            entrancePoints = listOf(
                Pair(30f, 80f),     // From below (Ashen Sprawl)
                Pair(100f, 20f)     // Airship dock
            ),
            exitPoints = listOf(
                Pair(180f, 70f)     // To Clockworks
            ),
            
            isUnlockedByDefault = false,
            unlockConditions = listOf(
                UnlockCondition.PlayerLevel(6),
                UnlockCondition.AbilityUnlocked(AbilityType.ROPE_SWING)
            ),
            
            npcs = listOf("Rook", "Harbor_Ledger", "Captain_Drift"),
            shops = listOf("Harbor_Reach_Shop", "Sky_Market"),
            
            totalMemoryFragments = 18,
            totalLoreObjects = 10,
            totalSanctuaries = 7,
            totalSecrets = 9,
            
            lore = "Once a trade nexus, the Archipelago was hollowed out by war and greed. " +
                    "Now, it serves as a haven for those who refuse to forget, hiding " +
                    "their memories from the Council's reach.",
            loreArabic = "كان ذات يوم مركزاً تجارياً، أُفرغ الأرخبيل بالحرب والطمع. الآن، " +
                    "يعمل كملاذ لأولئك الذين يرفضون النسيان، يخفون ذكرياتهم من متناول المجلس.",
            
            widthTiles = 250,
            heightTiles = 150,
            totalAreas = 35
        ),
        
        // ═══════════════════════════════════════════════════════════════════════════
        // 4. GLASSFJORD CLIFFS — منحدرات الزجاج
        // ═══════════════════════════════════════════════════════════════════════════
        RegionType.GLASSFJORD_CLIFFS to RegionData(
            type = RegionType.GLASSFJORD_CLIFFS,
            name = "Glassfjord Cliffs",
            nameArabic = "منحدرات الزجاج",
            description = "Frozen mountains of crystalline glass that reflect and distort reality, " +
                    "where echoes of the past shimmer in every surface.",
            descriptionArabic = "جبال متجمدة من الزجاج البلوري تعكس وتشوه الواقع، حيث تتلألأ " +
                    "أصداء الماضي في كل سطح.",
            
            colorPalette = RegionColorPalette(
                primary = Color(0xFF00BCD4),        // سماوي
                secondary = Color(0xFF80DEEA),      // سماوي فاتح
                tertiary = Color(0xFFE0F7FA),       // أبيض مزرق
                background = Color(0xFFE1F5FE),     // أزرق شاحب جداً
                foreground = Color(0xFF4DD0E1),     // سماوي متوسط
                highlight = Color(0xFF00E5FF),      // سماوي مشع
                danger = Color(0xFF006064)          // سماوي داكن جداً
            ),
            
            ambientLight = Color(0xFFB2EBF2),
            fogColor = Color(0xFFE0F2F1),
            fogDensity = 0.3f,
            skyboxTexture = "glass_mountains",
            
            defaultWeather = WeatherType.SNOW,
            weatherPatterns = listOf(
                WeatherPattern(
                    type = WeatherType.SNOW,
                    probability = 0.5f,
                    durationMinMs = 200000L,
                    durationMaxMs = 500000L,
                    transitionTimeMs = 20000L,
                    intensity = 0.6f
                ),
                WeatherPattern(
                    type = WeatherType.ICE_STORM,
                    probability = 0.3f,
                    durationMinMs = 100000L,
                    durationMaxMs = 250000L,
                    transitionTimeMs = 15000L,
                    intensity = 0.8f
                ),
                WeatherPattern(
                    type = WeatherType.CLEAR,
                    probability = 0.2f,
                    durationMinMs = 300000L,
                    durationMaxMs = 600000L,
                    transitionTimeMs = 10000L,
                    intensity = 1.0f
                )
            ),
            
            hazards = listOf(HazardType.ICE, HazardType.FREEZING_WATER),
            musicTheme = "glassfjord_theme",
            ambientSounds = listOf("ice_cracking", "wind_howling", "crystal_chimes", "aurora"),
            
            recommendedLevel = 12,
            difficultyMultiplier = 1.5f,
            enemyTypes = listOf(
                EnemyType.SHARDLING,
                EnemyType.GLASS_HOUND,
                EnemyType.REFLECTOR,
                EnemyType.MIRROR_SENTINEL
            ),
            bossType = EnemyType.FRACTURED_COLOSSUS,
            
            connectedRegions = listOf(RegionType.VEILED_ARCHIVES, RegionType.LUMINOUS_CHASM),
            entrancePoints = listOf(
                Pair(15f, 100f),    // From Archives
                Pair(120f, 30f)     // Mountain path
            ),
            exitPoints = listOf(
                Pair(200f, 80f)     // To Chasm
            ),
            
            isUnlockedByDefault = false,
            unlockConditions = listOf(
                UnlockCondition.PlayerLevel(10),
                UnlockCondition.BossDefeated(EnemyType.THE_INDEXER)
            ),
            
            npcs = listOf("Shardcart_Seller", "Ice_Hermit"),
            shops = listOf("Frozen_Basin_Shop"),
            
            totalMemoryFragments = 22,
            totalLoreObjects = 15,
            totalSanctuaries = 6,
            totalSecrets = 12,
            
            lore = "The Glassfjord was formed when the first Memory Purge shattered the world's " +
                    "collective consciousness. Its reflections show not what is, but what " +
                    "could have been—a haunting reminder of lost possibilities.",
            loreArabic = "تشكلت منحدرات الزجاج عندما حطم أول تطهير للذاكرة الوعي الجماعي للعالم. " +
                    "انعكاساتها لا تُظهر ما هو كائن، بل ما كان يمكن أن يكون — تذكير مؤلم " +
                    "بالإمكانيات المفقودة.",
            
            widthTiles = 280,
            heightTiles = 180,
            totalAreas = 40
        ),
        
        // ═══════════════════════════════════════════════════════════════════════════
        // 5. SUNKEN CLOCKWORKS — الساعات الغارقة
        // ═══════════════════════════════════════════════════════════════════════════
        RegionType.SUNKEN_CLOCKWORKS to RegionData(
            type = RegionType.SUNKEN_CLOCKWORKS,
            name = "Sunken Clockworks",
            nameArabic = "الساعات الغارقة",
            description = "A drowned mechanical city where gears still turn beneath the water, " +
                    "measuring time that no longer exists.",
            descriptionArabic = "مدينة آلية غارقة حيث لا تزال التروس تدور تحت الماء، تقيس " +
                    "وقتاً لم يعد موجوداً.",
            
            colorPalette = RegionColorPalette(
                primary = Color(0xFF558B2F),        // أخضر برونزي
                secondary = Color(0xFF9CCC65),      // أخضر فاتح
                tertiary = Color(0xFF8D6E63),       // بني صدئ
                background = Color(0xFF263238),     // رمادي أخضر داكن
                foreground = Color(0xFF455A64),     // رمادي مزرق
                highlight = Color(0xFFFFB74D),      // برتقالي نحاسي
                danger = Color(0xFF6A1B9A)          // بنفسجي (كهرباء)
            ),
            
            ambientLight = Color(0xFF546E7A),
            fogColor = Color(0xFF37474F),
            fogDensity = 0.5f,
            skyboxTexture = "underwater_gears",
            
            defaultWeather = WeatherType.STEAM,
            weatherPatterns = listOf(
                WeatherPattern(
                    type = WeatherType.STEAM,
                    probability = 0.7f,
                    durationMinMs = 180000L,
                    durationMaxMs = 450000L,
                    transitionTimeMs = 12000L,
                    intensity = 0.7f
                )
            ),
            
            hazards = listOf(HazardType.WATER, HazardType.ELECTRICITY, HazardType.GEARS),
            musicTheme = "clockworks_theme",
            ambientSounds = listOf("ticking", "water_dripping", "gears_grinding", "steam_hissing"),
            
            recommendedLevel = 15,
            difficultyMultiplier = 1.6f,
            enemyTypes = listOf(
                EnemyType.GEARFOLK,
                EnemyType.FLOOD_WRAITH,
                EnemyType.VALVE_SPIDER,
                EnemyType.BRASS_JUGGERNAUT
            ),
            bossType = EnemyType.GIDEON_REMNANT,
            
            connectedRegions = listOf(RegionType.HOLLOWED_ARCHIPELAGO, RegionType.BLACKROOT_MOORLANDS),
            entrancePoints = listOf(
                Pair(40f, 20f),     // From Archipelago (descend)
                Pair(150f, 50f)     // Underwater tunnel
            ),
            exitPoints = listOf(
                Pair(220f, 90f)     // To Moorlands
            ),
            
            isUnlockedByDefault = false,
            unlockConditions = listOf(
                UnlockCondition.PlayerLevel(13),
                UnlockCondition.AbilityUnlocked(AbilityType.WATER_BREATHING),
                UnlockCondition.ItemOwned("clockwork_key")
            ),
            
            npcs = listOf("Gideon", "Cogscribe", "Rust_Merchant"),
            shops = listOf("Canal_Platform_Shop", "Gear_Bazaar"),
            
            totalMemoryFragments = 25,
            totalLoreObjects = 18,
            totalSanctuaries = 5,
            totalSecrets = 15,
            
            lore = "The Clockworks once regulated all of Erygra's time, ensuring memories were " +
                    "cataloged and preserved. When it sank, so did the integrity of the timeline—" +
                    "now, past and present bleed together in its flooded halls.",
            loreArabic = "كانت الساعات ذات يوم تنظم كل زمن إريغرا، تضمن تصنيف الذكريات والحفاظ " +
                    "عليها. عندما غرقت، غرقت معها سلامة الخط الزمني — الآن، الماضي والحاضر " +
                    "يمتزجان في قاعاتها المغمورة.",
            
            widthTiles = 300,
            heightTiles = 200,
            totalAreas = 45
        ),
        
        // ═══════════════════════════════════════════════════════════════════════════
        // 6. BLACKROOT MOORLANDS — مستنقعات الجذور
        // ═══════════════════════════════════════════════════════════════════════════
        RegionType.BLACKROOT_MOORLANDS to RegionData(
            type = RegionType.BLACKROOT_MOORLANDS,
            name = "Blackroot Moorlands",
            nameArabic = "مستنقعات الجذور السوداء",
            description = "A toxic marshland where corrupted roots writhe and the air itself " +
                    "seems to forget those who breathe it.",
            descriptionArabic = "أرض مستنقعية سامة حيث تتلوى الجذور الفاسدة والهواء نفسه يبدو " +
                    "أنه ينسى أولئك الذين يتنفسونه.",
            
            colorPalette = RegionColorPalette(
                primary = Color(0xFF4E342E),        // بني داكن
                secondary = Color(0xFF558B2F),      // أخضر طحلبي
                tertiary = Color(0xFF6A1B9A),       // بنفسجي سام
                background = Color(0xFF1B5E20),     // أخضر داكن جداً
                foreground = Color(0xFF33691E),     // أخضر غامق
                highlight = Color(0xFF9C27B0),      // بنفسجي
                danger = Color(0xFF7B1FA2)          // بنفسجي غامق
            ),
            
            ambientLight = Color(0xFF4E342E),
            fogColor = Color(0xFF37474F),
            fogDensity = 0.7f,
            skyboxTexture = "murky_sky",
            
            defaultWeather = WeatherType.FOG,
            weatherPatterns = listOf(
                WeatherPattern(
                    type = WeatherType.FOG,
                    probability = 0.8f,
                    durationMinMs = 300000L,
                    durationMaxMs = 900000L,
                    transitionTimeMs = 25000L,
                    intensity = 0.9f
                ),
                WeatherPattern(
                    type = WeatherType.DRIZZLE,
                    probability = 0.2f,
                    durationMinMs = 150000L,
                    durationMaxMs = 350000L,
                    transitionTimeMs = 15000L,
                    intensity = 0.5f
                )
            ),
            
            hazards = listOf(HazardType.POISON, HazardType.THORNS, HazardType.MUD),
            musicTheme = "moorlands_theme",
            ambientSounds = listOf("frogs_croaking", "insects_buzzing", "swamp_bubbles", "eerie_wind"),
            
            recommendedLevel = 18,
            difficultyMultiplier = 1.7f,
            enemyTypes = listOf(
                EnemyType.ROOTCRAWLER,
                EnemyType.BOG_SIREN,
                EnemyType.HOLLOW_HERDER,
                EnemyType.NIGHT_STITCHER_ECHO
            ),
            bossType = EnemyType.ROOT_TITAN,
            
            connectedRegions = listOf(RegionType.SUNKEN_CLOCKWORKS, RegionType.LUMINOUS_CHASM),
            entrancePoints = listOf(
                Pair(30f, 110f),    // From Clockworks
                Pair(180f, 60f)     // Hidden passage
            ),
            exitPoints = listOf(
                Pair(270f, 100f)    // To Chasm
            ),
            
            isUnlockedByDefault = false,
            unlockConditions = listOf(
                UnlockCondition.PlayerLevel(16),
                UnlockCondition.QuestCompleted("gideon_rescue"),
                UnlockCondition.ItemOwned("root_immunity_charm")
            ),
            
            npcs = listOf("Maera", "Root_Map_Hermit", "Bog_Witch"),
            shops = listOf("Bog_Pool_Shop"),
            
            totalMemoryFragments = 28,
            totalLoreObjects = 20,
            totalSanctuaries = 6,
            totalSecrets = 18,
            
            lore = "The Moorlands grow from the collective grief of Erygra. Every forgotten sorrow " +
                    "feeds the roots, twisting them into monuments of pain. Maera stitches the " +
                    "wounded here, fighting to preserve what little humanity remains.",
            loreArabic = "تنمو المستنقعات من الحزن الجماعي لإريغرا. كل حزن منسي يغذي الجذور، " +
                    "يلفها إلى نصب من الألم. ماييرا تخيط الجرحى هنا، تقاتل للحفاظ على ما " +
                    "تبقى من إنسانية.",
            
            widthTiles = 320,
            heightTiles = 220,
            totalAreas = 50
        ),
        
        // ═══════════════════════════════════════════════════════════════════════════
        // 7. LUMINOUS CHASM — الهاوية المضيئة
        // ═══════════════════════════════════════════════════════════════════════════
        RegionType.LUMINOUS_CHASM to RegionData(
            type = RegionType.LUMINOUS_CHASM,
            name = "Luminous Chasm",
            nameArabic = "الهاوية المضيئة",
            description = "The final abyss where all lost memories gather, glowing with the light " +
                    "of countless forgotten souls. Here, the boundary between self and void dissolves.",
            descriptionArabic = "الهاوية الأخيرة حيث تتجمع كل الذكريات المفقودة، تتوهج بنور " +
                    "أرواح لا تعد ولا تحصى منسية. هنا، الحد بين الذات والفراغ ينحل.",
            
            colorPalette = RegionColorPalette(
                primary = Color(0xFFFDD835),        // ذهبي
                secondary = Color(0xFF9C27B0),      // بنفسجي
                tertiary = Color(0xFFE1BEE7),       // بنفسجي فاتح
                background = Color(0xFF1A237E),     // أزرق داكن جداً (void)
                foreground = Color(0xFF512DA8),     // بنفسجي داكن
                highlight = Color(0xFFFFEA00),      // أصفر مشع
                danger = Color(0xFF000000)          // أسود (الفراغ)
            ),
            
            ambientLight = Color(0xFFFFD54F),
            fogColor = Color(0xFF9575CD),
            fogDensity = 0.3f,
            skyboxTexture = "void_stars",
            
            defaultWeather = WeatherType.GLOWING_PARTICLES,
            weatherPatterns = listOf(
                WeatherPattern(
                    type = WeatherType.GLOWING_PARTICLES,
                    probability = 1.0f,
                    durationMinMs = 600000L,
                    durationMaxMs = 1800000L,
                    transitionTimeMs = 30000L,
                    intensity = 1.0f
                )
            ),
            
            hazards = listOf(HazardType.VOID, HazardType.LIGHT_BEAMS),
            musicTheme = "luminous_chasm_theme",
            ambientSounds = listOf("ethereal_choir", "memory_whispers", "void_hum", "light_pulse"),
            
            recommendedLevel = 22,
            difficultyMultiplier = 2.0f,
            enemyTypes = listOf(
                EnemyType.GLOW_WISP,
                EnemyType.MEMORY_LEECH,
                EnemyType.CAVE_STALKER,
                EnemyType.BIOLUME_SENTINEL
            ),
            bossType = EnemyType.LUMINAR_HOST,
            
            connectedRegions = listOf(
                RegionType.GLASSFJORD_CLIFFS,
                RegionType.BLACKROOT_MOORLANDS
            ),
            entrancePoints = listOf(
                Pair(50f, 150f),    // From Glassfjord
                Pair(100f, 50f)     // From Moorlands
            ),
            exitPoints = emptyList(),   // No exit — final region
            
            isUnlockedByDefault = false,
            unlockConditions = listOf(
                UnlockCondition.PlayerLevel(20),
                UnlockCondition.MemoryFragmentsCollected(100),
                UnlockCondition.BossDefeated(EnemyType.ROOT_TITAN),
                UnlockCondition.MaskObtained
            ),
            
            npcs = listOf("Biolume_Cartographer", "The_Luminar", "Echo_Of_Self"),
            shops = listOf("Glow_Pond_Shop"),
            
            totalMemoryFragments = 35,
            totalLoreObjects = 25,
            totalSanctuaries = 4,
            totalSecrets = 25,
            
            lore = "The Chasm is the heart of Erygra's collective unconscious—a place where all " +
                    "forgotten things exist in perpetual twilight. To enter is to risk losing " +
                    "yourself entirely, becoming just another glow in the darkness. Only those " +
                    "who remember who they are can hope to return.",
            loreArabic = "الهاوية هي قلب اللاوعي الجماعي لإريغرا — مكان حيث توجد كل الأشياء " +
                    "المنسية في شفق دائم. الدخول يعني المخاطرة بفقدان نفسك بالكامل، تصبح " +
                    "مجرد وهج آخر في الظلام. فقط أولئك الذين يتذكرون من هم يمكنهم الأمل في العودة.",
            
            widthTiles = 350,
            heightTiles = 250,
            totalAreas = 60
        )
    )
    
    /**
     * الحصول على بيانات منطقة
     */
    fun getRegion(type: RegionType): RegionData? = regions[type]
    
    /**
     * الحصول على جميع المناطق كقائمة
     */
    fun getAllRegions(): List<RegionData> = regions.values.toList()
    
    /**
     * الحصول على المناطق حسب المستوى الموصى به
     */
    fun getRegionsByLevel(minLevel: Int, maxLevel: Int): List<RegionData> =
        regions.values.filter { it.recommendedLevel in minLevel..maxLevel }
    
    /**
     * الحصول على المناطق التي تحتوي على زعماء
     */
    fun getRegionsWithBoss(): List<RegionData> =
        regions.values.filter { it.bossType != null }
    
    /**
     * الحصول على منطقة بالاسم (إنجليزي أو عربي)
     */
    fun getRegionByName(name: String): RegionType? =
        regions.entries.firstOrNull { 
            it.value.name.equals(name, ignoreCase = true) || 
            it.value.nameArabic == name 
        }?.key
}

// ═══════════════════════════════════════════════════════════════════════════════
// Biome Configuration Database
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات إعدادات البيئة الطبيعية
 */
object BiomeConfigDatabase {
    
    val biomes: Map<RegionType, BiomeConfig> = mapOf(
        
        RegionType.ASHEN_SPRAWL to BiomeConfig(
            region = RegionType.ASHEN_SPRAWL,
            gravityModifier = 1.0f,
            airResistanceModifier = 1.1f,      // رياح ساخنة تبطئ قليلاً
            waterLevel = null,
            particleEffects = listOf(
                ParticleType.ASH,
                ParticleType.EMBER,
                ParticleType.SMOKE
            ),
            particleSpawnRate = 1.5f,
            lightingMode = LightingMode.DIM,
            visibilityRange = 800f,
            jumpHeightModifier = 1.0f,
            movementSpeedModifier = 0.95f,     // الحرارة تبطئ قليلاً
            hasLava = true,
            hasPoisonWater = false,
            hasElectricity = false,
            hasFallingDebris = true
        ),
        
        RegionType.VEILED_ARCHIVES to BiomeConfig(
            region = RegionType.VEILED_ARCHIVES,
            gravityModifier = 1.0f,
            airResistanceModifier = 1.0f,
            waterLevel = null,
            particleEffects = listOf(
                ParticleType.DUST,
                ParticleType.PAPER_SCRAP
            ),
            particleSpawnRate = 0.8f,
            lightingMode = LightingMode.DIM,
            visibilityRange = 700f,
            jumpHeightModifier = 1.0f,
            movementSpeedModifier = 1.0f,
            hasLava = false,
            hasPoisonWater = false,
            hasElectricity = false,
            hasFallingDebris = true            // كتب ساقطة
        ),
        
        RegionType.HOLLOWED_ARCHIPELAGO to BiomeConfig(
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            gravityModifier = 0.9f,            // جاذبية أقل (ارتفاع)
            airResistanceModifier = 1.3f,      // رياح قوية
            waterLevel = null,
            particleEffects = listOf(
                ParticleType.CLOUD,
                ParticleType.FEATHER,
                ParticleType.WIND_GUST
            ),
            particleSpawnRate = 1.2f,
            lightingMode = LightingMode.BRIGHT,
            visibilityRange = 1200f,
            jumpHeightModifier = 1.1f,         // قفز أعلى (رياح)
            movementSpeedModifier = 1.05f,     // حركة أسرع (رياح خلفية)
            hasLava = false,
            hasPoisonWater = false,
            hasElectricity = false,
            hasFallingDebris = false,
            hasWind = true,
            windStrength = 2.5f,
            windDirectionDegrees = 90f         // رياح شرقية
        ),
        
        RegionType.GLASSFJORD_CLIFFS to BiomeConfig(
            region = RegionType.GLASSFJORD_CLIFFS,
            gravityModifier = 1.0f,
            airResistanceModifier = 0.9f,
            waterLevel = 30f,
            particleEffects = listOf(
                ParticleType.SNOW,
                ParticleType.ICE_CRYSTAL,
                ParticleType.AURORA
            ),
            particleSpawnRate = 1.3f,
            lightingMode = LightingMode.BRIGHT,
            visibilityRange = 1000f,
            jumpHeightModifier = 1.0f,
            movementSpeedModifier = 0.85f,     // جليد زلق
            hasLava = false,
            hasPoisonWater = false,
            hasElectricity = false,
            hasFallingDebris = true            // جليد متساقط
        ),
        
        RegionType.SUNKEN_CLOCKWORKS to BiomeConfig(
            region = RegionType.SUNKEN_CLOCKWORKS,
            gravityModifier = 1.2f,            // جاذبية أقوى (تحت الماء)
            airResistanceModifier = 2.5f,      // مقاومة الماء
            waterLevel = 150f,
            particleEffects = listOf(
                ParticleType.BUBBLE,
                ParticleType.STEAM,
                ParticleType.OIL_DRIP
            ),
            particleSpawnRate = 1.0f,
            lightingMode = LightingMode.DARK,
            visibilityRange = 600f,
            jumpHeightModifier = 0.7f,         // قفز أقل (تحت الماء)
            movementSpeedModifier = 0.6f,      // حركة بطيئة (ماء)
            hasLava = false,
            hasPoisonWater = false,
            hasElectricity = true,
            hasFallingDebris = false,
            hasCurrents = true,
            currentStrength = 1.5f
        ),
        
        RegionType.BLACKROOT_MOORLANDS to BiomeConfig(
            region = RegionType.BLACKROOT_MOORLANDS,
            gravityModifier = 1.1f,
            airResistanceModifier = 1.2f,      // هواء كثيف
            waterLevel = 50f,
            particleEffects = listOf(
                ParticleType.FOG,
                ParticleType.SPORE,
                ParticleType.FIREFLY
            ),
            particleSpawnRate = 1.8f,
            lightingMode = LightingMode.DARK,
            visibilityRange = 500f,
            jumpHeightModifier = 0.95f,
            movementSpeedModifier = 0.8f,      // طين يبطئ
            hasLava = false,
            hasPoisonWater = true,
            hasElectricity = false,
            hasFallingDebris = false
        ),
        
        RegionType.LUMINOUS_CHASM to BiomeConfig(
            region = RegionType.LUMINOUS_CHASM,
            gravityModifier = 0.8f,            // جاذبية ضعيفة (void)
            airResistanceModifier = 0.7f,
            waterLevel = null,
            particleEffects = listOf(
                ParticleType.LIGHT_RAY,
                ParticleType.SPARKLE,
                ParticleType.VOID_PARTICLE
            ),
            particleSpawnRate = 2.0f,
            lightingMode = LightingMode.DYNAMIC,
            visibilityRange = 900f,
            jumpHeightModifier = 1.3f,         // قفز أعلى (void)
            movementSpeedModifier = 1.1f,
            hasLava = false,
            hasPoisonWater = false,
            hasElectricity = false,
            hasFallingDebris = false
        )
    )
    
    /**
     * الحصول على إعدادات بيئة منطقة
     */
    fun getConfig(region: RegionType): BiomeConfig? = biomes[region]
}

// ═══════════════════════════════════════════════════════════════════════════════
// Region Connection Graph
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * رسم الاتصالات بين المناطق
 */
object RegionConnectionGraph {
    
    /**
     * جميع الاتصالات بين المناطق
     */
    private val connections = mutableListOf<RegionConnection>(
        
        // Ashen Sprawl ↔ Veiled Archives
        RegionConnection(
            fromRegion = RegionType.ASHEN_SPRAWL,
            toRegion = RegionType.VEILED_ARCHIVES,
            connectionType = ConnectionType.GATE,
            transitionPoint = Pair(150f, 50f),
            isOneWay = false,
            unlockCondition = UnlockCondition.PlayerLevel(3),
            travelTimeMs = 5000L,
            travelCost = 20
        ),
        
        // Ashen Sprawl ↔ Hollowed Archipelago
        RegionConnection(
            fromRegion = RegionType.ASHEN_SPRAWL,
            toRegion = RegionType.HOLLOWED_ARCHIPELAGO,
            connectionType = ConnectionType.FERRY,
            transitionPoint = Pair(50f, 10f),
            isOneWay = false,
            unlockCondition = UnlockCondition.ItemOwned("ferry_pass"),
            travelTimeMs = 8000L,
            travelCost = 50
        ),
        
        // Veiled Archives ↔ Glassfjord Cliffs
        RegionConnection(
            fromRegion = RegionType.VEILED_ARCHIVES,
            toRegion = RegionType.GLASSFJORD_CLIFFS,
            connectionType = ConnectionType.TELEPORT,
            transitionPoint = Pair(180f, 60f),
            isOneWay = false,
            unlockCondition = UnlockCondition.BossDefeated(EnemyType.THE_INDEXER),
            travelTimeMs = 2000L,
            travelCost = 100
        ),
        
        // Hollowed Archipelago ↔ Sunken Clockworks
        RegionConnection(
            fromRegion = RegionType.HOLLOWED_ARCHIPELAGO,
            toRegion = RegionType.SUNKEN_CLOCKWORKS,
            connectionType = ConnectionType.LADDER,
            transitionPoint = Pair(180f, 70f),
            isOneWay = false,
            unlockCondition = UnlockCondition.AbilityUnlocked(AbilityType.CLIMB),
            travelTimeMs = 6000L,
            travelCost = 30
        ),
        
        // Sunken Clockworks ↔ Blackroot Moorlands
        RegionConnection(
            fromRegion = RegionType.SUNKEN_CLOCKWORKS,
            toRegion = RegionType.BLACKROOT_MOORLANDS,
            connectionType = ConnectionType.DOOR,
            transitionPoint = Pair(220f, 90f),
            isOneWay = false,
            unlockCondition = UnlockCondition.ItemOwned("root_key"),
            travelTimeMs = 4000L,
            travelCost = 40
        ),
        
        // Glassfjord Cliffs ↔ Luminous Chasm
        RegionConnection(
            fromRegion = RegionType.GLASSFJORD_CLIFFS,
            toRegion = RegionType.LUMINOUS_CHASM,
            connectionType = ConnectionType.BRIDGE,
            transitionPoint = Pair(200f, 80f),
            isOneWay = false,
            unlockCondition = UnlockCondition.MemoryFragmentsCollected(100),
            travelTimeMs = 10000L,
            travelCost = 0  // حدث قصصي
        ),
        
        // Blackroot Moorlands ↔ Luminous Chasm
        RegionConnection(
            fromRegion = RegionType.BLACKROOT_MOORLANDS,
            toRegion = RegionType.LUMINOUS_CHASM,
            connectionType = ConnectionType.ROPE,
            transitionPoint = Pair(270f, 100f),
            isOneWay = false,
            unlockCondition = UnlockCondition.BossDefeated(EnemyType.ROOT_TITAN),
            travelTimeMs = 7000L,
            travelCost = 0  // حدث قصصي
        )
    )
    
    /**
     * الحصول على جميع الاتصالات من منطقة
     */
    fun getConnectionsFrom(region: RegionType): List<RegionConnection> =
        connections.filter { it.fromRegion == region }
    
    /**
     * الحصول على اتصال محدد
     */
    fun getConnection(from: RegionType, to: RegionType): RegionConnection? =
        connections.firstOrNull { it.fromRegion == from && it.toRegion == to }
    
    /**
     * الحصول على جميع الاتصالات
     */
    fun getAllConnections(): List<RegionConnection> = connections.toList()
    
    /**
     * إضافة اتصال جديد (للمناطق الديناميكية)
     */
    fun addConnection(connection: RegionConnection) {
        if (!connections.any { it.fromRegion == connection.fromRegion && it.toRegion == connection.toRegion }) {
            connections.add(connection)
        }
    }
    
    /**
     * تحديث حالة اكتشاف اتصال
     */
    fun markConnectionDiscovered(from: RegionType, to: RegionType) {
        connections.indexOfFirst { it.fromRegion == from && it.toRegion == to }
            .takeIf { it != -1 }
            ?.let { index ->
                connections[index] = connections[index].copy(isDiscovered = true)
            }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Region Manager
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مدير المناطق — يدير التنقل والاكتشاف والحالة
 */
class RegionManager {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════
    
    private val _currentRegion = MutableStateFlow(RegionType.ASHEN_SPRAWL)
    val currentRegion: StateFlow<RegionType> = _currentRegion.asStateFlow()
    
    private val _discoveries = MutableStateFlow<Map<RegionType, RegionDiscovery>>(
        mapOf(
            RegionType.ASHEN_SPRAWL to RegionDiscovery(
                region = RegionType.ASHEN_SPRAWL,
                isUnlocked = true,
                isDiscovered = true,
                totalAreas = 25,
                totalFragments = 15,
                totalLoreObjects = 8,
                totalSanctuaries = 8,
                totalSecrets = 5,
                firstVisitTime = System.currentTimeMillis()
            )
        )
    )
    val discoveries: StateFlow<Map<RegionType, RegionDiscovery>> = _discoveries.asStateFlow()
    
    private val _visitHistory = MutableStateFlow<List<Pair<RegionType, Long>>>(
        listOf(Pair(RegionType.ASHEN_SPRAWL, System.currentTimeMillis()))
    )
    val visitHistory: StateFlow<List<Pair<RegionType, Long>>> = _visitHistory.asStateFlow()
    
    private var regionEnterTime = System.currentTimeMillis()
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Navigation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على المنطقة الحالية
     */
    fun getCurrentRegion(): RegionType = _currentRegion.value
    
    /**
     * الحصول على بيانات المنطقة الحالية
     */
    fun getCurrentRegionData(): RegionData? =
        RegionDatabase.getRegion(_currentRegion.value)
    
    /**
     * الحصول على إعدادات بيئة المنطقة الحالية
     */
    fun getCurrentBiomeConfig(): BiomeConfig? =
        BiomeConfigDatabase.getConfig(_currentRegion.value)
    
    /**
     * التحقق من إمكانية الدخول إلى منطقة
     */
    fun canEnterRegion(region: RegionType, playerLevel: Int, inventory: Set<String>, 
                      unlockedAbilities: Set<AbilityType>, completedQuests: Set<String>,
                      defeatedBosses: Set<EnemyType>, memoryFragments: Int,
                      hasMask: Boolean): Boolean {
        
        // إذا كانت المنطقة مفتوحة بالفعل
        if (_discoveries.value[region]?.isUnlocked == true) return true
        
        val regionData = RegionDatabase.getRegion(region) ?: return false
        
        // إذا كانت مفتوحة بشكل افتراضي
        if (regionData.isUnlockedByDefault) return true
        
        // التحقق من شروط الفتح
        return regionData.unlockConditions.all { condition ->
            checkUnlockCondition(
                condition, playerLevel, inventory, unlockedAbilities,
                completedQuests, defeatedBosses, memoryFragments, hasMask
            )
        }
    }
    
    /**
     * التحقق من شرط فتح واحد
     */
    private fun checkUnlockCondition(
        condition: UnlockCondition,
        playerLevel: Int,
        inventory: Set<String>,
        unlockedAbilities: Set<AbilityType>,
        completedQuests: Set<String>,
        defeatedBosses: Set<EnemyType>,
        memoryFragments: Int,
        hasMask: Boolean
    ): Boolean = when (condition) {
        is UnlockCondition.PlayerLevel -> playerLevel >= condition.level
        is UnlockCondition.QuestCompleted -> completedQuests.contains(condition.questId)
        is UnlockCondition.BossDefeated -> defeatedBosses.contains(condition.bossType)
        is UnlockCondition.ItemOwned -> inventory.contains(condition.itemId)
        is UnlockCondition.AbilityUnlocked -> unlockedAbilities.contains(condition.abilityType)
        is UnlockCondition.MemoryFragmentsCollected -> memoryFragments >= condition.count
        is UnlockCondition.MaskObtained -> hasMask
        is UnlockCondition.RegionUnlocked -> _discoveries.value[condition.region]?.isUnlocked == true
        is UnlockCondition.RegionDiscoveryPercentage -> {
            val discovery = _discoveries.value[condition.region]
            discovery != null && discovery.discoveryPercentage >= condition.percentage
        }
        is UnlockCondition.CurrencyAmount -> false  // يحتاج currency من PlayerState
    }
    
    /**
     * الانتقال إلى منطقة
     */
    fun transitionToRegion(region: RegionType, entryPoint: Pair<Float, Float>? = null) {
        if (region == _currentRegion.value) return
        
        // حفظ وقت الدخول السابق
        val timeInPreviousRegion = System.currentTimeMillis() - regionEnterTime
        _discoveries.update { current ->
            val previousRegion = _currentRegion.value
            val discovery = current[previousRegion] ?: return@update current
            current + (previousRegion to discovery.copy(
                totalTimeSpentMs = discovery.totalTimeSpentMs + timeInPreviousRegion
            ))
        }
        
        // الانتقال
        _currentRegion.value = region
        regionEnterTime = System.currentTimeMillis()
        
        // تحديث سجل الزيارات
        _visitHistory.update { it + Pair(region, System.currentTimeMillis()) }
        
        // تحديث الاكتشاف
        _discoveries.update { current ->
            val discovery = current[region] ?: RegionDiscovery(
                region = region,
                isUnlocked = true,
                isDiscovered = true,
                totalAreas = RegionDatabase.getRegion(region)?.totalAreas ?: 0,
                totalFragments = RegionDatabase.getRegion(region)?.totalMemoryFragments ?: 0,
                totalLoreObjects = RegionDatabase.getRegion(region)?.totalLoreObjects ?: 0,
                totalSanctuaries = RegionDatabase.getRegion(region)?.totalSanctuaries ?: 0,
                totalSecrets = RegionDatabase.getRegion(region)?.totalSecrets ?: 0,
                firstVisitTime = System.currentTimeMillis()
            )
            current + (region to discovery)
        }
        
        // إطلاق حدث الدخول
        onRegionEnter(region)
    }
    
    /**
     * الحصول على المناطق المتصلة بمنطقة
     */
    fun getConnectedRegions(region: RegionType = _currentRegion.value): List<RegionType> =
        RegionConnectionGraph.getConnectionsFrom(region).map { it.toRegion }
    
    /**
     * البحث عن مسار بين منطقتين (BFS)
     */
    fun findPath(fromRegion: RegionType, toRegion: RegionType): List<RegionConnection>? {
        if (fromRegion == toRegion) return emptyList()
        
        val visited = mutableSetOf<RegionType>()
        val queue: Queue<Pair<RegionType, List<RegionConnection>>> = LinkedList()
        queue.offer(Pair(fromRegion, emptyList()))
        
        while (queue.isNotEmpty()) {
            val (current, path) = queue.poll()
            if (current in visited) continue
            visited.add(current)
            
            val connections = RegionConnectionGraph.getConnectionsFrom(current)
            for (connection in connections) {
                if (connection.toRegion == toRegion) {
                    return path + connection
                }
                if (connection.toRegion !in visited) {
                    queue.offer(Pair(connection.toRegion, path + connection))
                }
            }
        }
        
        return null  // لا يوجد مسار
    }
    
    /**
     * الحصول على أقصر مسار (أقل عدد من الاتصالات)
     */
    fun getShortestPath(fromRegion: RegionType, toRegion: RegionType): List<RegionType>? {
        val connections = findPath(fromRegion, toRegion) ?: return null
        return listOf(fromRegion) + connections.map { it.toRegion }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Unlocking & Discovery
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * فتح منطقة
     */
    fun unlockRegion(region: RegionType) {
        _discoveries.update { current ->
            val discovery = current[region] ?: RegionDiscovery(
                region = region,
                totalAreas = RegionDatabase.getRegion(region)?.totalAreas ?: 0,
                totalFragments = RegionDatabase.getRegion(region)?.totalMemoryFragments ?: 0,
                totalLoreObjects = RegionDatabase.getRegion(region)?.totalLoreObjects ?: 0,
                totalSanctuaries = RegionDatabase.getRegion(region)?.totalSanctuaries ?: 0,
                totalSecrets = RegionDatabase.getRegion(region)?.totalSecrets ?: 0
            )
            current + (region to discovery.copy(isUnlocked = true))
        }
    }
    
    /**
     * التحقق من فتح منطقة
     */
    fun isRegionUnlocked(region: RegionType): Boolean =
        _discoveries.value[region]?.isUnlocked == true
    
    /**
     * وضع علامة على منطقة فرعية كمُكتشفة
     */
    fun markAreaDiscovered(region: RegionType, areaId: String) {
        _discoveries.update { current ->
            val discovery = current[region] ?: return@update current
            val newDiscoveredAreas = discovery.discoveredAreas + areaId
            current + (region to discovery.copy(discoveredAreas = newDiscoveredAreas))
        }
        
        onAreaDiscovered(region, areaId)
    }
    
    /**
     * جمع شظية ذاكرة
     */
    fun collectMemoryFragment(region: RegionType) {
        _discoveries.update { current ->
            val discovery = current[region] ?: return@update current
            val newCount = min(discovery.collectedFragments + 1, discovery.totalFragments)
            current + (region to discovery.copy(collectedFragments = newCount))
        }
    }
    
    /**
     * إيجاد شيء من القصة
     */
    fun findLoreObject(region: RegionType) {
        _discoveries.update { current ->
            val discovery = current[region] ?: return@update current
            val newCount = min(discovery.foundLoreObjects + 1, discovery.totalLoreObjects)
            current + (region to discovery.copy(foundLoreObjects = newCount))
        }
    }
    
    /**
     * تفعيل ملجأ
     */
    fun activateSanctuary(region: RegionType) {
        _discoveries.update { current ->
            val discovery = current[region] ?: return@update current
            val newCount = min(discovery.activatedSanctuaries + 1, discovery.totalSanctuaries)
            current + (region to discovery.copy(activatedSanctuaries = newCount))
        }
    }
    
    /**
     * إيجاد سر
     */
    fun findSecret(region: RegionType) {
        _discoveries.update { current ->
            val discovery = current[region] ?: return@update current
            val newCount = min(discovery.foundSecrets + 1, discovery.totalSecrets)
            current + (region to discovery.copy(foundSecrets = newCount))
        }
    }
    
    /**
     * هزيمة زعيم منطقة
     */
    fun defeatRegionBoss(region: RegionType) {
        _discoveries.update { current ->
            val discovery = current[region] ?: return@update current
            current + (region to discovery.copy(defeatedBoss = true))
        }
    }
    
    /**
     * إكمال مهمة في منطقة
     */
    fun completeQuestInRegion(region: RegionType, questId: String) {
        _discoveries.update { current ->
            val discovery = current[region] ?: return@update current
            val newQuests = discovery.completedQuests + questId
            current + (region to discovery.copy(completedQuests = newQuests))
        }
    }
    
    /**
     * الحصول على نسبة اكتشاف منطقة
     */
    fun getDiscoveryPercentage(region: RegionType): Float =
        _discoveries.value[region]?.discoveryPercentage ?: 0f
    
    /**
     * الحصول على نسبة الإكمال الكلي لمنطقة
     */
    fun getCompletionPercentage(region: RegionType): Float =
        _discoveries.value[region]?.completionPercentage ?: 0f
    
    /**
     * الحصول على عدد شظايا الذاكرة المجموعة
     */
    fun getCollectedFragments(region: RegionType): Int =
        _discoveries.value[region]?.collectedFragments ?: 0
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Data Access
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على بيانات منطقة
     */
    fun getRegionData(region: RegionType): RegionData? =
        RegionDatabase.getRegion(region)
    
    /**
     * الحصول على إعدادات بيئة منطقة
     */
    fun getBiomeConfig(region: RegionType): BiomeConfig? =
        BiomeConfigDatabase.getConfig(region)
    
    /**
     * الحصول على لوحة ألوان منطقة
     */
    fun getColorPalette(region: RegionType): RegionColorPalette? =
        RegionDatabase.getRegion(region)?.colorPalette
    
    /**
     * الحصول على حالة اكتشاف منطقة
     */
    fun getRegionDiscovery(region: RegionType): RegionDiscovery? =
        _discoveries.value[region]
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Events
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * حدث الدخول إلى منطقة
     */
    private fun onRegionEnter(region: RegionType) {
        // يمكن إطلاق أحداث اللعبة هنا (EventBus)
        println("Entered region: ${RegionDatabase.getRegion(region)?.name}")
    }
    
    /**
     * حدث الخروج من منطقة
     */
    private fun onRegionExit(region: RegionType) {
        // يمكن إطلاق أحداث اللعبة هنا
        println("Exited region: ${RegionDatabase.getRegion(region)?.name}")
    }
    
    /**
     * حدث اكتشاف منطقة فرعية
     */
    private fun onAreaDiscovered(region: RegionType, areaId: String) {
        // يمكن إطلاق أحداث اللعبة هنا
        println("Discovered area $areaId in ${RegionDatabase.getRegion(region)?.name}")
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Save/Load
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ حالة المدير
     */
    fun saveState(): RegionManagerState {
        // حفظ الوقت المتبقي في المنطقة الحالية
        val timeInCurrentRegion = System.currentTimeMillis() - regionEnterTime
        val updatedDiscoveries = _discoveries.value.toMutableMap()
        val currentDiscovery = updatedDiscoveries[_currentRegion.value]
        if (currentDiscovery != null) {
            updatedDiscoveries[_currentRegion.value] = currentDiscovery.copy(
                totalTimeSpentMs = currentDiscovery.totalTimeSpentMs + timeInCurrentRegion
            )
        }
        
        return RegionManagerState(
            currentRegion = _currentRegion.value,
            discoveries = updatedDiscoveries,
            unlockedConnections = RegionConnectionGraph.getAllConnections()
                .filter { it.isDiscovered }
                .map { Pair(it.fromRegion, it.toRegion) }
                .toSet(),
            visitHistory = _visitHistory.value,
            totalRegionsUnlocked = updatedDiscoveries.count { it.value.isUnlocked },
            totalRegionsCompleted = updatedDiscoveries.count { it.value.completionPercentage >= 0.95f }
        )
    }
    
    /**
     * تحميل حالة المدير
     */
    fun loadState(state: RegionManagerState) {
        _currentRegion.value = state.currentRegion
        _discoveries.value = state.discoveries
        _visitHistory.value = state.visitHistory
        regionEnterTime = System.currentTimeMillis()
        
        // تحديث حالة اكتشاف الاتصالات
        state.unlockedConnections.forEach { (from, to) ->
            RegionConnectionGraph.markConnectionDiscovered(from, to)
        }
    }
    
    /**
     * إعادة تعيين كل التقدم (للعبة جديدة)
     */
    fun resetAllProgress() {
        _currentRegion.value = RegionType.ASHEN_SPRAWL
        _discoveries.value = mapOf(
            RegionType.ASHEN_SPRAWL to RegionDiscovery(
                region = RegionType.ASHEN_SPRAWL,
                isUnlocked = true,
                isDiscovered = true,
                totalAreas = 25,
                totalFragments = 15,
                totalLoreObjects = 8,
                totalSanctuaries = 8,
                totalSecrets = 5,
                firstVisitTime = System.currentTimeMillis()
            )
        )
        _visitHistory.value = listOf(Pair(RegionType.ASHEN_SPRAWL, System.currentTimeMillis()))
        regionEnterTime = System.currentTimeMillis()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Helper Functions
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * الحصول على لون تلوين منطقة
 */
fun getRegionTintColor(region: RegionType): Color =
    RegionDatabase.getRegion(region)?.colorPalette?.primary ?: Color.White

/**
 * دمج ألوان منطقتين مع تقدم
 */
fun interpolateRegionColors(
    from: RegionType,
    to: RegionType,
    progress: Float
): RegionColorPalette {
    val fromPalette = RegionDatabase.getRegion(from)?.colorPalette 
        ?: return RegionDatabase.getRegion(to)?.colorPalette ?: RegionColorPalette(
            Color.White, Color.White, Color.White, Color.White, 
            Color.White, Color.White, Color.White
        )
    val toPalette = RegionDatabase.getRegion(to)?.colorPalette 
        ?: return fromPalette
    
    return RegionColorPalette(
        primary = androidx.compose.ui.graphics.lerp(fromPalette.primary, toPalette.primary, progress),
        secondary = androidx.compose.ui.graphics.lerp(fromPalette.secondary, toPalette.secondary, progress),
        tertiary = androidx.compose.ui.graphics.lerp(fromPalette.tertiary, toPalette.tertiary, progress),
        background = androidx.compose.ui.graphics.lerp(fromPalette.background, toPalette.background, progress),
        foreground = androidx.compose.ui.graphics.lerp(fromPalette.foreground, toPalette.foreground, progress),
        highlight = androidx.compose.ui.graphics.lerp(fromPalette.highlight, toPalette.highlight, progress),
        danger = androidx.compose.ui.graphics.lerp(fromPalette.danger, toPalette.danger, progress)
    )
}

/**
 * الحصول على منطقة بالاسم
 */
fun getRegionByName(name: String): RegionType? =
    RegionDatabase.getRegionByName(name)