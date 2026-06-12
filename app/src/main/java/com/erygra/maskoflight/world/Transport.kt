package com.erygra.maskoflight.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.player.AbilityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * Transport System — Mask of Light (Erygra Universe)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * نظام وسائل النقل المتعدد:
 * - 5 أنواع وسائل نقل (Ferry, Gondola, Clockwork Ferry, Sky Barge, Root Tunnel)
 * - نقاط محطات موزعة على المناطق
 * - نظام تذاكر وتصاريح
 * - جداول زمنية وتوفر ديناميكي
 * - رسوم استخدام متغيرة
 * - مهام فتح/إصلاح
 * - رسوم متحركة للسفر
 * - تكامل مع RegionManager و WorldMapManager
 *
 * الوسائل:
 * 1. River Ferry — عبارة نهرية (50 Coins / Season Pass 250)
 * 2. Rope-Gondola — جندولا حبلية (200 Coins + 3 Gears لفتح)
 * 3. Clockwork Ferry — عبارة آلية (300 Coins + Key Gear)
 * 4. Sky Barge — سفينة هوائية (1000 Coins + Escort Quest)
 * 5. Root Tunnel — نفق جذور (400 Coins أو Root Key)
 *
 * @author Erygra Development Team
 * @version 2.0.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════════
// Transport Enums
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * أنواع وسائل النقل
 */
enum class TransportType {
    RIVER_FERRY,        // عبارة نهرية
    ROPE_GONDOLA,       // جندولا حبلية
    CLOCKWORK_FERRY,    // عبارة آلية
    SKY_BARGE,          // سفينة هوائية
    ROOT_TUNNEL,        // نفق جذور
    BRIDGE,             // جسر (ثابت)
    TELEPORTER,         // انتقال فوري
    LADDER,             // سلم
    ROPE,               // حبل
    ELEVATOR            // مصعد
}

/**
 * حالة وسيلة النقل
 */
enum class TransportStatus {
    LOCKED,             // مقفلة (تحتاج فتح)
    UNDER_REPAIR,       // تحت الإصلاح
    AVAILABLE,          // متاحة
    IN_USE,             // قيد الاستخدام
    BROKEN,             // معطلة
    UNAVAILABLE,        // غير متاحة (جدول زمني)
    DESTROYED           // مدمرة (حدث قصصي)
}

/**
 * طريقة الدفع
 */
enum class PaymentMethod {
    COINS,              // عملة
    SEASON_PASS,        // تصريح موسمي
    ITEM_KEY,           // مفتاح/عنصر
    QUEST_COMPLETION,   // إكمال مهمة
    FREE                // مجاني
}

/**
 * سرعة السفر
 */
enum class TravelSpeed(val multiplier: Float) {
    VERY_SLOW(0.5f),
    SLOW(0.75f),
    NORMAL(1.0f),
    FAST(1.5f),
    VERY_FAST(2.0f),
    INSTANT(999f)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Transport Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * محطة نقل
 */
data class TransportStation(
    val id: String,
    val name: String,
    val nameArabic: String,
    val type: TransportType,
    val region: RegionType,
    val position: Offset,
    val status: TransportStatus = TransportStatus.LOCKED,
    
    // الاتصال:
    val connectedStationIds: List<String> = emptyList(),
    
    // التكلفة:
    val unlockCost: Int = 0,                    // تكلفة الفتح
    val unlockItems: List<String> = emptyList(), // عناصر مطلوبة للفتح
    val unlockQuest: String? = null,             // مهمة للفتح
    
    val rideCost: Int = 0,                       // تكلفة الركوب
    val ridePayment: PaymentMethod = PaymentMethod.COINS,
    val seasonPassCost: Int = 0,                 // تكلفة التصريح الموسمي
    
    // الصيانة:
    val maintenanceItemId: String? = null,       // عنصر الصيانة (Oil, Fuel, etc.)
    val maintenanceInterval: Int = 0,            // عدد الرحلات قبل الصيانة (0 = لا صيانة)
    val currentMaintenanceUses: Int = 0,
    
    // الجدول الزمني:
    val hasSchedule: Boolean = false,
    val availableHours: List<Pair<Int, Int>> = emptyList(), // (start hour, end hour)
    val unavailableDays: List<Int> = emptyList(), // أيام غير متاحة (0-6)
    
    // السفر:
    val travelSpeed: TravelSpeed = TravelSpeed.NORMAL,
    val capacityPerTrip: Int = 1,                // سعة الركاب (للمستقبل)
    
    // المظهر:
    val iconColor: Color = Color.White,
    val description: String = "",
    val descriptionArabic: String = "",
    
    // الإحصائيات:
    val totalTrips: Int = 0,
    val lastUsedTime: Long? = null,
    
    // الاكتشاف:
    val isDiscovered: Boolean = false,
    val discoveryRadius: Float = 5f
) {
    /**
     * هل المحطة متاحة الآن؟
     */
    fun isAvailableNow(currentHour: Int, currentDay: Int): Boolean {
        if (status != TransportStatus.AVAILABLE) return false
        if (!hasSchedule) return true
        if (unavailableDays.contains(currentDay)) return false
        return availableHours.any { (start, end) ->
            currentHour in start until end
        }
    }
    
    /**
     * هل تحتاج صيانة؟
     */
    fun needsMaintenance(): Boolean {
        if (maintenanceInterval == 0) return false
        return currentMaintenanceUses >= maintenanceInterval
    }
}

/**
 * مسار سفر
 */
data class TravelRoute(
    val fromStationId: String,
    val toStationId: String,
    val distance: Float,                         // المسافة
    val estimatedTimeMs: Long,                   // الوقت المقدر
    val isOneWay: Boolean = false,
    val requiredAbilities: List<AbilityType> = emptyList(),
    val hazards: List<String> = emptyList(),     // مخاطر في الطريق
    val scenicValue: Int = 0                     // قيمة المناظر (1-10)
)

/**
 * رحلة نشطة
 */
data class ActiveTrip(
    val tripId: String,
    val fromStation: TransportStation,
    val toStation: TransportStation,
    val route: TravelRoute,
    val startTime: Long = System.currentTimeMillis(),
    val estimatedArrivalTime: Long,
    val currentProgress: Float = 0f,             // 0.0-1.0
    val currentPosition: Offset = fromStation.position,
    val isSkippable: Boolean = true
) {
    /**
     * هل الرحلة مكتملة؟
     */
    val isComplete: Boolean
        get() = currentProgress >= 1.0f
    
    /**
     * الوقت المتبقي
     */
    fun getRemainingTimeMs(): Long {
        val now = System.currentTimeMillis()
        return max(0, estimatedArrivalTime - now)
    }
}

/**
 * تصريح موسمي
 */
data class SeasonPass(
    val id: String,
    val name: String,
    val nameArabic: String,
    val transportTypes: Set<TransportType>,      // الوسائل المشمولة
    val regions: Set<RegionType>,                // المناطق المشمولة
    val purchasePrice: Int,
    val expirationTime: Long? = null,            // null = لا ينتهي
    val purchasedAt: Long = System.currentTimeMillis(),
    val totalUsageCount: Int = 0
) {
    /**
     * هل التصريح ساري؟
     */
    fun isValid(): Boolean {
        if (expirationTime == null) return true
        return System.currentTimeMillis() < expirationTime
    }
    
    /**
     * هل يغطي نوع نقل؟
     */
    fun covers(type: TransportType): Boolean = transportTypes.contains(type)
    
    /**
     * هل يغطي منطقة؟
     */
    fun coversRegion(region: RegionType): Boolean = regions.contains(region)
}

/**
 * حدث نقل
 */
sealed class TransportEvent {
    data class StationDiscovered(val station: TransportStation, val time: Long) : TransportEvent()
    data class StationUnlocked(val station: TransportStation, val time: Long) : TransportEvent()
    data class StationRepaired(val station: TransportStation, val time: Long) : TransportEvent()
    data class TripStarted(val trip: ActiveTrip, val time: Long) : TransportEvent()
    data class TripCompleted(val trip: ActiveTrip, val time: Long) : TransportEvent()
    data class TripCancelled(val tripId: String, val reason: String, val time: Long) : TransportEvent()
    data class SeasonPassPurchased(val pass: SeasonPass, val time: Long) : TransportEvent()
    data class MaintenancePerformed(val stationId: String, val time: Long) : TransportEvent()
    data class StationBroken(val stationId: String, val reason: String, val time: Long) : TransportEvent()
}

/**
 * حالة نظام النقل (للحفظ/التحميل)
 */
data class TransportSystemState(
    val stations: Map<String, TransportStation>,
    val routes: List<TravelRoute>,
    val activeTrip: ActiveTrip? = null,
    val ownedSeasonPasses: List<SeasonPass>,
    val discoveredStations: Set<String>,
    val unlockedStations: Set<String>,
    val totalTripsCount: Int,
    val totalCoinsSpent: Int,
    val eventHistory: List<TransportEvent>
)

// ═══════════════════════════════════════════════════════════════════════════════
// Transport Station Database
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات محطات النقل
 */
object TransportStationDatabase {
    
    /**
     * جميع المحطات معرّفة مسبقاً
     */
    fun createDefaultStations(): Map<String, TransportStation> = mapOf(
        
        // ═══════════════════════════════════════════════════════════════════════
        // RIVER FERRY — عبارات نهرية
        // ═══════════════════════════════════════════════════════════════════════
        
        "ferry_ashen_port" to TransportStation(
            id = "ferry_ashen_port",
            name = "Port of Ash Ferry",
            nameArabic = "عبارة ميناء الرماد",
            type = TransportType.RIVER_FERRY,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(35f, 55f),
            status = TransportStatus.AVAILABLE,  // مفتوحة من البداية
            connectedStationIds = listOf("ferry_archive_wharf", "ferry_archipelago_harbor"),
            unlockCost = 0,
            rideCost = 5,
            ridePayment = PaymentMethod.COINS,
            seasonPassCost = 250,
            travelSpeed = TravelSpeed.SLOW,
            iconColor = Color(0xFF42A5F5),
            description = "A rusty ferry that connects the Port of Ash to other regions.",
            descriptionArabic = "عبارة صدئة تربط ميناء الرماد بمناطق أخرى.",
            isDiscovered = true
        ),
        
        "ferry_archive_wharf" to TransportStation(
            id = "ferry_archive_wharf",
            name = "Vault Wharf Ferry",
            nameArabic = "عبارة رصيف القبو",
            type = TransportType.RIVER_FERRY,
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(88f, 32f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("ferry_ashen_port"),
            unlockCost = 50,
            unlockItems = listOf("ferry_pass"),
            rideCost = 5,
            ridePayment = PaymentMethod.COINS,
            seasonPassCost = 250,
            travelSpeed = TravelSpeed.SLOW,
            iconColor = Color(0xFF42A5F5),
            description = "A quiet dock among the archives.",
            descriptionArabic = "رصيف هادئ بين الأرشيفات."
        ),
        
        "ferry_archipelago_harbor" to TransportStation(
            id = "ferry_archipelago_harbor",
            name = "Harbor Reach Ferry",
            nameArabic = "عبارة مدى المرفأ",
            type = TransportType.RIVER_FERRY,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(23f, 68f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("ferry_ashen_port"),
            unlockCost = 50,
            unlockItems = listOf("ferry_pass"),
            rideCost = 5,
            ridePayment = PaymentMethod.COINS,
            seasonPassCost = 250,
            travelSpeed = TravelSpeed.SLOW,
            iconColor = Color(0xFF42A5F5),
            description = "A sky-high dock with rope moorings.",
            descriptionArabic = "رصيف عالٍ في السماء مع حبال ربط."
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // ROPE-GONDOLA — جندولات حبلية
        // ═══════════════════════════════════════════════════════════════════════
        
        "gondola_archipelago_main" to TransportStation(
            id = "gondola_archipelago_main",
            name = "Main Island Gondola",
            nameArabic = "جندولا الجزيرة الرئيسية",
            type = TransportType.ROPE_GONDOLA,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(40f, 30f),
            status = TransportStatus.UNDER_REPAIR,
            connectedStationIds = listOf("gondola_archipelago_west", "gondola_archipelago_east"),
            unlockCost = 200,
            unlockItems = listOf("gear_small", "gear_small", "gear_small"),  // 3 Gears
            unlockQuest = "repair_gondola_system",
            rideCost = 0,  // مجاني بعد الفتح
            ridePayment = PaymentMethod.FREE,
            travelSpeed = TravelSpeed.NORMAL,
            iconColor = Color(0xFF26C6DA),
            description = "A rope-and-pulley gondola system. Needs repair.",
            descriptionArabic = "نظام جندولا بالحبال والبكرات. يحتاج إصلاحاً."
        ),
        
        "gondola_archipelago_west" to TransportStation(
            id = "gondola_archipelago_west",
            name = "West Platform Gondola",
            nameArabic = "جندولا المنصة الغربية",
            type = TransportType.ROPE_GONDOLA,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(10f, 35f),
            status = TransportStatus.UNDER_REPAIR,
            connectedStationIds = listOf("gondola_archipelago_main"),
            unlockCost = 0,  // تُفتح مع Main
            rideCost = 0,
            ridePayment = PaymentMethod.FREE,
            travelSpeed = TravelSpeed.NORMAL,
            iconColor = Color(0xFF26C6DA),
            description = "Western gondola station.",
            descriptionArabic = "محطة جندولا غربية."
        ),
        
        "gondola_archipelago_east" to TransportStation(
            id = "gondola_archipelago_east",
            name = "East Platform Gondola",
            nameArabic = "جندولا المنصة الشرقية",
            type = TransportType.ROPE_GONDOLA,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(70f, 35f),
            status = TransportStatus.UNDER_REPAIR,
            connectedStationIds = listOf("gondola_archipelago_main"),
            unlockCost = 0,  // تُفتح مع Main
            rideCost = 0,
            ridePayment = PaymentMethod.FREE,
            travelSpeed = TravelSpeed.NORMAL,
            iconColor = Color(0xFF26C6DA),
            description = "Eastern gondola station.",
            descriptionArabic = "محطة جندولا شرقية."
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // CLOCKWORK FERRY — عبارة آلية
        // ═══════════════════════════════════════════════════════════════════════
        
        "clockwork_main_canal" to TransportStation(
            id = "clockwork_main_canal",
            name = "Main Canal Ferry",
            nameArabic = "عبارة القناة الرئيسية",
            type = TransportType.CLOCKWORK_FERRY,
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(220f, 88f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("clockwork_north_dock", "clockwork_south_dock"),
            unlockCost = 300,
            unlockItems = listOf("key_gear"),
            rideCost = 0,
            ridePayment = PaymentMethod.ITEM_KEY,  // يستهلك Oil
            maintenanceItemId = "oil_can",
            maintenanceInterval = 3,  // كل 3 رحلات
            travelSpeed = TravelSpeed.FAST,
            iconColor = Color(0xFF558B2F),
            description = "An automated ferry powered by clockwork gears.",
            descriptionArabic = "عبارة آلية تعمل بتروس الساعات."
        ),
        
        "clockwork_north_dock" to TransportStation(
            id = "clockwork_north_dock",
            name = "Northern Dock",
            nameArabic = "الرصيف الشمالي",
            type = TransportType.CLOCKWORK_FERRY,
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(180f, 60f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("clockwork_main_canal"),
            unlockCost = 0,
            rideCost = 0,
            ridePayment = PaymentMethod.ITEM_KEY,
            maintenanceItemId = "oil_can",
            maintenanceInterval = 3,
            travelSpeed = TravelSpeed.FAST,
            iconColor = Color(0xFF558B2F),
            description = "Northern docking station.",
            descriptionArabic = "محطة رسو شمالية."
        ),
        
        "clockwork_south_dock" to TransportStation(
            id = "clockwork_south_dock",
            name = "Southern Dock",
            nameArabic = "الرصيف الجنوبي",
            type = TransportType.CLOCKWORK_FERRY,
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(250f, 110f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("clockwork_main_canal"),
            unlockCost = 0,
            rideCost = 0,
            ridePayment = PaymentMethod.ITEM_KEY,
            maintenanceItemId = "oil_can",
            maintenanceInterval = 3,
            travelSpeed = TravelSpeed.FAST,
            iconColor = Color(0xFF558B2F),
            description = "Southern docking station.",
            descriptionArabic = "محطة رسو جنوبية."
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // SKY BARGE — سفينة هوائية
        // ═══════════════════════════════════════════════════════════════════════
        
        "barge_ashen_platform" to TransportStation(
            id = "barge_ashen_platform",
            name = "Ashen Sky Platform",
            nameArabic = "منصة السماء الرمادية",
            type = TransportType.SKY_BARGE,
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(100f, 30f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("barge_glass_platform", "barge_archipelago_platform"),
            unlockCost = 1000,
            unlockQuest = "escort_sky_merchant",
            rideCost = 50,
            ridePayment = PaymentMethod.COINS,
            hasSchedule = true,
            availableHours = listOf(6 to 12, 14 to 20),  // 6am-12pm, 2pm-8pm
            travelSpeed = TravelSpeed.VERY_FAST,
            iconColor = Color(0xFF66BB6A),
            description = "A massive airship that travels between regions.",
            descriptionArabic = "سفينة هوائية ضخمة تسافر بين المناطق."
        ),
        
        "barge_glass_platform" to TransportStation(
            id = "barge_glass_platform",
            name = "Glassfjord Landing",
            nameArabic = "هبوط الزجاج",
            type = TransportType.SKY_BARGE,
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(156f, 48f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("barge_ashen_platform"),
            unlockCost = 0,
            rideCost = 50,
            ridePayment = PaymentMethod.COINS,
            hasSchedule = true,
            availableHours = listOf(6 to 12, 14 to 20),
            travelSpeed = TravelSpeed.VERY_FAST,
            iconColor = Color(0xFF66BB6A),
            description = "A frozen landing platform for the sky barge.",
            descriptionArabic = "منصة هبوط متجمدة للسفينة الهوائية."
        ),
        
        "barge_archipelago_platform" to TransportStation(
            id = "barge_archipelago_platform",
            name = "Archipelago Sky Dock",
            nameArabic = "رصيف سماء الأرخبيل",
            type = TransportType.SKY_BARGE,
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(100f, 20f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("barge_ashen_platform"),
            unlockCost = 0,
            rideCost = 50,
            ridePayment = PaymentMethod.COINS,
            hasSchedule = true,
            availableHours = listOf(6 to 12, 14 to 20),
            travelSpeed = TravelSpeed.VERY_FAST,
            iconColor = Color(0xFF66BB6A),
            description = "A high-altitude docking platform.",
            descriptionArabic = "منصة رسو عالية الارتفاع."
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // ROOT TUNNEL — أنفاق الجذور
        // ═══════════════════════════════════════════════════════════════════════
        
        "tunnel_moor_entrance" to TransportStation(
            id = "tunnel_moor_entrance",
            name = "Moorland Tunnel Entrance",
            nameArabic = "مدخل نفق المستنقعات",
            type = TransportType.ROOT_TUNNEL,
            region = RegionType.BLACKROOT_MOORLANDS,
            position = Offset(100f, 150f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("tunnel_chasm_exit"),
            unlockCost = 400,
            unlockItems = listOf("root_key"),  // أو شراء بـ 400 Coins
            rideCost = 0,  // مجاني بعد الفتح
            ridePayment = PaymentMethod.FREE,
            travelSpeed = TravelSpeed.NORMAL,
            iconColor = Color(0xFF5D4037),
            description = "A dark tunnel carved through ancient roots.",
            descriptionArabic = "نفق مظلم محفور عبر جذور قديمة."
        ),
        
        "tunnel_chasm_exit" to TransportStation(
            id = "tunnel_chasm_exit",
            name = "Chasm Tunnel Exit",
            nameArabic = "مخرج نفق الهاوية",
            type = TransportType.ROOT_TUNNEL,
            region = RegionType.LUMINOUS_CHASM,
            position = Offset(100f, 180f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("tunnel_moor_entrance"),
            unlockCost = 0,
            rideCost = 0,
            ridePayment = PaymentMethod.FREE,
            travelSpeed = TravelSpeed.NORMAL,
            iconColor = Color(0xFF5D4037),
            description = "The tunnel opens into the luminous void.",
            descriptionArabic = "ينفتح النفق على الفراغ المضيء."
        ),
        
        // ═══════════════════════════════════════════════════════════════════════
        // TELEPORTERS — انتقال فوري
        // ═══════════════════════════════════════════════════════════════════════
        
        "teleport_archive_to_glass" to TransportStation(
            id = "teleport_archive_to_glass",
            name = "Archive Teleporter",
            nameArabic = "ناقل الأرشيف",
            type = TransportType.TELEPORTER,
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(180f, 60f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("teleport_glass_to_archive"),
            unlockCost = 0,
            unlockQuest = "defeat_archival_regent",
            rideCost = 100,
            ridePayment = PaymentMethod.COINS,
            travelSpeed = TravelSpeed.INSTANT,
            iconColor = Color(0xFF7E57C2),
            description = "A magical portal powered by forgotten memories.",
            descriptionArabic = "بوابة سحرية تعمل بذكريات منسية."
        ),
        
        "teleport_glass_to_archive" to TransportStation(
            id = "teleport_glass_to_archive",
            name = "Glassfjord Teleporter",
            nameArabic = "ناقل الزجاج",
            type = TransportType.TELEPORTER,
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(200f, 80f),
            status = TransportStatus.LOCKED,
            connectedStationIds = listOf("teleport_archive_to_glass"),
            unlockCost = 0,
            rideCost = 100,
            ridePayment = PaymentMethod.COINS,
            travelSpeed = TravelSpeed.INSTANT,
            iconColor = Color(0xFF7E57C2),
            description = "A frozen portal amidst the glass.",
            descriptionArabic = "بوابة متجمدة وسط الزجاج."
        )
    )
    
    /**
     * الحصول على محطة
     */
    fun getStation(id: String): TransportStation? =
        createDefaultStations()[id]
    
    /**
     * الحصول على محطات منطقة
     */
    fun getStationsInRegion(region: RegionType): List<TransportStation> =
        createDefaultStations().values.filter { it.region == region }
    
    /**
     * الحصول على محطات حسب النوع
     */
    fun getStationsByType(type: TransportType): List<TransportStation> =
        createDefaultStations().values.filter { it.type == type }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Transport Route Database
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات المسارات
 */
object TransportRouteDatabase {
    
    /**
     * إنشاء المسارات الافتراضية
     */
    fun createDefaultRoutes(): List<TravelRoute> = listOf(
        
        // River Ferry routes:
        TravelRoute(
            fromStationId = "ferry_ashen_port",
            toStationId = "ferry_archive_wharf",
            distance = 80f,
            estimatedTimeMs = 8000L,
            scenicValue = 6
        ),
        TravelRoute(
            fromStationId = "ferry_archive_wharf",
            toStationId = "ferry_ashen_port",
            distance = 80f,
            estimatedTimeMs = 8000L,
            scenicValue = 6
        ),
        TravelRoute(
            fromStationId = "ferry_ashen_port",
            toStationId = "ferry_archipelago_harbor",
            distance = 60f,
            estimatedTimeMs = 6000L,
            scenicValue = 8
        ),
        TravelRoute(
            fromStationId = "ferry_archipelago_harbor",
            toStationId = "ferry_ashen_port",
            distance = 60f,
            estimatedTimeMs = 6000L,
            scenicValue = 8
        ),
        
        // Gondola routes:
        TravelRoute(
            fromStationId = "gondola_archipelago_main",
            toStationId = "gondola_archipelago_west",
            distance = 35f,
            estimatedTimeMs = 4000L,
            requiredAbilities = listOf(AbilityType.ROPE_SWING),
            scenicValue = 9
        ),
        TravelRoute(
            fromStationId = "gondola_archipelago_west",
            toStationId = "gondola_archipelago_main",
            distance = 35f,
            estimatedTimeMs = 4000L,
            requiredAbilities = listOf(AbilityType.ROPE_SWING),
            scenicValue = 9
        ),
        TravelRoute(
            fromStationId = "gondola_archipelago_main",
            toStationId = "gondola_archipelago_east",
            distance = 40f,
            estimatedTimeMs = 4500L,
            requiredAbilities = listOf(AbilityType.ROPE_SWING),
            scenicValue = 9
        ),
        TravelRoute(
            fromStationId = "gondola_archipelago_east",
            toStationId = "gondola_archipelago_main",
            distance = 40f,
            estimatedTimeMs = 4500L,
            requiredAbilities = listOf(AbilityType.ROPE_SWING),
            scenicValue = 9
        ),
        
        // Clockwork Ferry routes:
        TravelRoute(
            fromStationId = "clockwork_main_canal",
            toStationId = "clockwork_north_dock",
            distance = 50f,
            estimatedTimeMs = 3000L,
            hazards = listOf("electricity", "gears"),
            scenicValue = 7
        ),
        TravelRoute(
            fromStationId = "clockwork_north_dock",
            toStationId = "clockwork_main_canal",
            distance = 50f,
            estimatedTimeMs = 3000L,
            hazards = listOf("electricity", "gears"),
            scenicValue = 7
        ),
        TravelRoute(
            fromStationId = "clockwork_main_canal",
            toStationId = "clockwork_south_dock",
            distance = 55f,
            estimatedTimeMs = 3500L,
            hazards = listOf("electricity", "gears"),
            scenicValue = 7
        ),
        TravelRoute(
            fromStationId = "clockwork_south_dock",
            toStationId = "clockwork_main_canal",
            distance = 55f,
            estimatedTimeMs = 3500L,
            hazards = listOf("electricity", "gears"),
            scenicValue = 7
        ),
        
        // Sky Barge routes:
        TravelRoute(
            fromStationId = "barge_ashen_platform",
            toStationId = "barge_glass_platform",
            distance = 120f,
            estimatedTimeMs = 5000L,
            scenicValue = 10
        ),
        TravelRoute(
            fromStationId = "barge_glass_platform",
            toStationId = "barge_ashen_platform",
            distance = 120f,
            estimatedTimeMs = 5000L,
            scenicValue = 10
        ),
        TravelRoute(
            fromStationId = "barge_ashen_platform",
            toStationId = "barge_archipelago_platform",
            distance = 80f,
            estimatedTimeMs = 4000L,
            scenicValue = 9
        ),
        TravelRoute(
            fromStationId = "barge_archipelago_platform",
            toStationId = "barge_ashen_platform",
            distance = 80f,
            estimatedTimeMs = 4000L,
            scenicValue = 9
        ),
        
        // Root Tunnel routes:
        TravelRoute(
            fromStationId = "tunnel_moor_entrance",
            toStationId = "tunnel_chasm_exit",
            distance = 100f,
            estimatedTimeMs = 7000L,
            hazards = listOf("poison", "darkness"),
            scenicValue = 5
        ),
        TravelRoute(
            fromStationId = "tunnel_chasm_exit",
            toStationId = "tunnel_moor_entrance",
            distance = 100f,
            estimatedTimeMs = 7000L,
            hazards = listOf("poison", "darkness"),
            scenicValue = 5
        ),
        
        // Teleporter routes:
        TravelRoute(
            fromStationId = "teleport_archive_to_glass",
            toStationId = "teleport_glass_to_archive",
            distance = 0f,
            estimatedTimeMs = 500L,  // شبه فوري
            scenicValue = 10
        ),
        TravelRoute(
            fromStationId = "teleport_glass_to_archive",
            toStationId = "teleport_archive_to_glass",
            distance = 0f,
            estimatedTimeMs = 500L,
            scenicValue = 10
        )
    )
    
    /**
     * الحصول على مسار
     */
    fun getRoute(fromId: String, toId: String): TravelRoute? =
        createDefaultRoutes().firstOrNull { it.fromStationId == fromId && it.toStationId == toId }
    
    /**
     * الحصول على جميع المسارات من محطة
     */
    fun getRoutesFrom(stationId: String): List<TravelRoute> =
        createDefaultRoutes().filter { it.fromStationId == stationId }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Transport System Manager
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مدير نظام النقل
 */
class TransportSystemManager {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════
    
    private val _stations = MutableStateFlow(TransportStationDatabase.createDefaultStations())
    val stations: StateFlow<Map<String, TransportStation>> = _stations.asStateFlow()
    
    private val _routes = MutableStateFlow(TransportRouteDatabase.createDefaultRoutes())
    val routes: StateFlow<List<TravelRoute>> = _routes.asStateFlow()
    
    private val _activeTrip = MutableStateFlow<ActiveTrip?>(null)
    val activeTrip: StateFlow<ActiveTrip?> = _activeTrip.asStateFlow()
    
    private val _ownedSeasonPasses = MutableStateFlow<List<SeasonPass>>(emptyList())
    val ownedSeasonPasses: StateFlow<List<SeasonPass>> = _ownedSeasonPasses.asStateFlow()
    
    private val _discoveredStations = MutableStateFlow<Set<String>>(setOf("ferry_ashen_port"))
    val discoveredStations: StateFlow<Set<String>> = _discoveredStations.asStateFlow()
    
    private val _unlockedStations = MutableStateFlow<Set<String>>(setOf("ferry_ashen_port"))
    val unlockedStations: StateFlow<Set<String>> = _unlockedStations.asStateFlow()
    
    private val _totalTripsCount = MutableStateFlow(0)
    val totalTripsCount: StateFlow<Int> = _totalTripsCount.asStateFlow()
    
    private val _totalCoinsSpent = MutableStateFlow(0)
    val totalCoinsSpent: StateFlow<Int> = _totalCoinsSpent.asStateFlow()
    
    private val _eventHistory = MutableStateFlow<List<TransportEvent>>(emptyList())
    val eventHistory: StateFlow<List<TransportEvent>> = _eventHistory.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Station Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على محطة
     */
    fun getStation(id: String): TransportStation? = _stations.value[id]
    
    /**
     * الحصول على محطات منطقة
     */
    fun getStationsInRegion(region: RegionType): List<TransportStation> =
        _stations.value.values.filter { it.region == region }
    
    /**
     * اكتشاف محطة (عند الاقتراب)
     */
    fun discoverStation(stationId: String, playerPosition: Offset) {
        val station = _stations.value[stationId] ?: return
        if (_discoveredStations.value.contains(stationId)) return
        
        // التحقق من المسافة
        val distance = kotlin.math.sqrt(
            (station.position.x - playerPosition.x) * (station.position.x - playerPosition.x) +
            (station.position.y - playerPosition.y) * (station.position.y - playerPosition.y)
        )
        
        if (distance <= station.discoveryRadius) {
            _discoveredStations.update { it + stationId }
            addEvent(TransportEvent.StationDiscovered(station, System.currentTimeMillis()))
        }
    }
    
    /**
     * فتح محطة
     */
    fun unlockStation(stationId: String, currency: Int, inventory: Set<String>): Boolean {
        val station = _stations.value[stationId] ?: return false
        if (_unlockedStations.value.contains(stationId)) return false
        
        // التحقق من العملة
        if (currency < station.unlockCost) return false
        
        // التحقق من العناصر
        if (!station.unlockItems.all { inventory.contains(it) }) return false
        
        // الفتح
        _unlockedStations.update { it + stationId }
        _stations.update { stations ->
            stations + (stationId to station.copy(status = TransportStatus.AVAILABLE))
        }
        _totalCoinsSpent.update { it + station.unlockCost }
        
        addEvent(TransportEvent.StationUnlocked(station, System.currentTimeMillis()))
        
        return true
    }
    
    /**
     * إصلاح محطة
     */
    fun repairStation(stationId: String, inventory: Set<String>): Boolean {
        val station = _stations.value[stationId] ?: return false
        if (station.status != TransportStatus.UNDER_REPAIR) return false
        
        // التحقق من العناصر المطلوبة
        if (!station.unlockItems.all { inventory.contains(it) }) return false
        
        _stations.update { stations ->
            stations + (stationId to station.copy(status = TransportStatus.AVAILABLE))
        }
        
        addEvent(TransportEvent.StationRepaired(station, System.currentTimeMillis()))
        
        return true
    }
    
    /**
     * تنفيذ صيانة محطة
     */
    fun performMaintenance(stationId: String, itemId: String): Boolean {
        val station = _stations.value[stationId] ?: return false
        if (station.maintenanceItemId != itemId) return false
        if (!station.needsMaintenance()) return false
        
        _stations.update { stations ->
            stations + (stationId to station.copy(currentMaintenanceUses = 0))
        }
        
        addEvent(TransportEvent.MaintenancePerformed(stationId, System.currentTimeMillis()))
        
        return true
    }
    
    /**
     * كسر محطة (حدث)
     */
    fun breakStation(stationId: String, reason: String) {
        val station = _stations.value[stationId] ?: return
        
        _stations.update { stations ->
            stations + (stationId to station.copy(status = TransportStatus.BROKEN))
        }
        
        addEvent(TransportEvent.StationBroken(stationId, reason, System.currentTimeMillis()))
    }
    
    /**
     * التحقق من توفر محطة
     */
    fun isStationAvailable(stationId: String, currentHour: Int, currentDay: Int): Boolean {
        val station = _stations.value[stationId] ?: return false
        if (!_unlockedStations.value.contains(stationId)) return false
        return station.isAvailableNow(currentHour, currentDay)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Travel Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من إمكانية السفر
     */
    fun canTravel(
        fromStationId: String,
        toStationId: String,
        currency: Int,
        inventory: Set<String>,
        unlockedAbilities: Set<AbilityType>,
        currentHour: Int,
        currentDay: Int
    ): Pair<Boolean, String> {
        
        // التحقق من رحلة نشطة
        if (_activeTrip.value != null) {
            return Pair(false, "Already traveling")
        }
        
        // التحقق من المحطات
        val fromStation = _stations.value[fromStationId] 
            ?: return Pair(false, "Station not found")
        val toStation = _stations.value[toStationId] 
            ?: return Pair(false, "Station not found")
        
        // التحقق من الفتح
        if (!_unlockedStations.value.contains(fromStationId)) {
            return Pair(false, "Station locked")
        }
        if (!_unlockedStations.value.contains(toStationId)) {
            return Pair(false, "Destination locked")
        }
        
        // التحقق من التوفر
        if (!fromStation.isAvailableNow(currentHour, currentDay)) {
            return Pair(false, "Station unavailable")
        }
        
        // التحقق من الاتصال
        if (!fromStation.connectedStationIds.contains(toStationId)) {
            return Pair(false, "Stations not connected")
        }
        
        // التحقق من المسار
        val route = TransportRouteDatabase.getRoute(fromStationId, toStationId)
            ?: return Pair(false, "Route not found")
        
        // التحقق من القدرات المطلوبة
        if (!route.requiredAbilities.all { unlockedAbilities.contains(it) }) {
            return Pair(false, "Missing required abilities")
        }
        
        // التحقق من الدفع
        when (fromStation.ridePayment) {
            PaymentMethod.COINS -> {
                if (currency < fromStation.rideCost) {
                    return Pair(false, "Insufficient coins")
                }
            }
            PaymentMethod.SEASON_PASS -> {
                val hasValidPass = _ownedSeasonPasses.value.any { pass ->
                    pass.isValid() && 
                    pass.covers(fromStation.type) && 
                    pass.coversRegion(fromStation.region)
                }
                if (!hasValidPass && currency < fromStation.rideCost) {
                    return Pair(false, "No valid season pass or coins")
                }
            }
            PaymentMethod.ITEM_KEY -> {
                val requiredItem = fromStation.maintenanceItemId
                if (requiredItem != null && !inventory.contains(requiredItem)) {
                    return Pair(false, "Missing required item: $requiredItem")
                }
            }
            PaymentMethod.QUEST_COMPLETION -> {
                // يُفترض أن المهمة اكتملت إذا المحطة مفتوحة
            }
            PaymentMethod.FREE -> {
                // مجاني
            }
        }
        
        // التحقق من الصيانة
        if (fromStation.needsMaintenance()) {
            return Pair(false, "Station needs maintenance")
        }
        
        return Pair(true, "")
    }
    
    /**
     * بدء رحلة
     */
    fun startTrip(
        fromStationId: String,
        toStationId: String,
        payWithSeasonPass: Boolean = false
    ): Boolean {
        
        val fromStation = _stations.value[fromStationId] ?: return false
        val toStation = _stations.value[toStationId] ?: return false
        val route = TransportRouteDatabase.getRoute(fromStationId, toStationId) ?: return false
        
        // حساب الوقت المقدر
        val estimatedTime = (route.estimatedTimeMs / fromStation.travelSpeed.multiplier).toLong()
        
        // إنشاء الرحلة
        val trip = ActiveTrip(
            tripId = "trip_${System.currentTimeMillis()}",
            fromStation = fromStation,
            toStation = toStation,
            route = route,
            estimatedArrivalTime = System.currentTimeMillis() + estimatedTime,
            isSkippable = fromStation.type != TransportType.TELEPORTER
        )
        
        _activeTrip.value = trip
        
        // تحديث الاستخدام
        if (!payWithSeasonPass && fromStation.ridePayment == PaymentMethod.COINS) {
            _totalCoinsSpent.update { it + fromStation.rideCost }
        }
        
        // تحديث الصيانة
        if (fromStation.maintenanceInterval > 0) {
            _stations.update { stations ->
                stations + (fromStationId to fromStation.copy(
                    currentMaintenanceUses = fromStation.currentMaintenanceUses + 1,
                    totalTrips = fromStation.totalTrips + 1,
                    lastUsedTime = System.currentTimeMillis()
                ))
            }
        }
        
        _totalTripsCount.update { it + 1 }
        addEvent(TransportEvent.TripStarted(trip, System.currentTimeMillis()))
        
        return true
    }
    
    /**
     * تحديث رحلة نشطة
     */
    fun updateActiveTrip(deltaTimeMs: Long) {
        val trip = _activeTrip.value ?: return
        
        val elapsedTime = System.currentTimeMillis() - trip.startTime
        val totalTime = trip.estimatedArrivalTime - trip.startTime
        val newProgress = min(1f, elapsedTime.toFloat() / totalTime.toFloat())
        
        // تحديث الموقع (interpolate)
        val newPosition = Offset(
            x = trip.fromStation.position.x + (trip.toStation.position.x - trip.fromStation.position.x) * newProgress,
            y = trip.fromStation.position.y + (trip.toStation.position.y - trip.fromStation.position.y) * newProgress
        )
        
        _activeTrip.value = trip.copy(
            currentProgress = newProgress,
            currentPosition = newPosition
        )
        
        // إكمال تلقائي
        if (newProgress >= 1f) {
            completeTrip()
        }
    }
    
    /**
     * إكمال رحلة
     */
    fun completeTrip() {
        val trip = _activeTrip.value ?: return
        
        addEvent(TransportEvent.TripCompleted(trip, System.currentTimeMillis()))
        _activeTrip.value = null
    }
    
    /**
     * إلغاء رحلة
     */
    fun cancelTrip(reason: String = "User cancelled") {
        val trip = _activeTrip.value ?: return
        
        addEvent(TransportEvent.TripCancelled(trip.tripId, reason, System.currentTimeMillis()))
        _activeTrip.value = null
    }
    
    /**
     * تخطي رحلة
     */
    fun skipTrip(): Boolean {
        val trip = _activeTrip.value ?: return false
        if (!trip.isSkippable) return false
        
        completeTrip()
        return true
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Season Pass Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * شراء تصريح موسمي
     */
    fun purchaseSeasonPass(
        name: String,
        transportTypes: Set<TransportType>,
        regions: Set<RegionType>,
        price: Int,
        currency: Int,
        durationDays: Int? = null
    ): Boolean {
        
        if (currency < price) return false
        
        val expiration = durationDays?.let { 
            System.currentTimeMillis() + (it * 24 * 60 * 60 * 1000L)
        }
        
        val pass = SeasonPass(
            id = "pass_${System.currentTimeMillis()}",
            name = name,
            nameArabic = name,  // TODO: Arabic translation
            transportTypes = transportTypes,
            regions = regions,
            purchasePrice = price,
            expirationTime = expiration
        )
        
        _ownedSeasonPasses.update { it + pass }
        _totalCoinsSpent.update { it + price }
        
        addEvent(TransportEvent.SeasonPassPurchased(pass, System.currentTimeMillis()))
        
        return true
    }
    
    /**
     * التحقق من وجود تصريح ساري
     */
    fun hasValidSeasonPass(type: TransportType, region: RegionType): Boolean =
        _ownedSeasonPasses.value.any { pass ->
            pass.isValid() && pass.covers(type) && pass.coversRegion(region)
        }
    
    /**
     * الحصول على التصاريح السارية
     */
    fun getValidSeasonPasses(): List<SeasonPass> =
        _ownedSeasonPasses.value.filter { it.isValid() }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Utility Functions
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على أقرب محطة متاحة
     */
    fun getNearestAvailableStation(
        position: Offset,
        region: RegionType,
        type: TransportType? = null
    ): TransportStation? {
        
        val availableStations = getStationsInRegion(region)
            .filter { station ->
                _unlockedStations.value.contains(station.id) &&
                station.status == TransportStatus.AVAILABLE &&
                (type == null || station.type == type)
            }
        
        return availableStations.minByOrNull { station ->
            val dx = station.position.x - position.x
            val dy = station.position.y - position.y
            kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }
    
    /**
     * حساب تكلفة رحلة
     */
    fun calculateTripCost(fromStationId: String, toStationId: String): Int {
        val fromStation = _stations.value[fromStationId] ?: return 0
        
        // التحقق من Season Pass
        if (hasValidSeasonPass(fromStation.type, fromStation.region)) {
            return 0
        }
        
        return when (fromStation.ridePayment) {
            PaymentMethod.COINS -> fromStation.rideCost
            PaymentMethod.SEASON_PASS -> fromStation.rideCost  // إذا لم يكن هناك تصريح
            else -> 0
        }
    }
    
    /**
     * الحصول على جميع المحطات المتصلة
     */
    fun getConnectedStations(stationId: String): List<TransportStation> {
        val station = _stations.value[stationId] ?: return emptyList()
        return station.connectedStationIds.mapNotNull { _stations.value[it] }
    }
    
    /**
     * إضافة حدث
     */
    private fun addEvent(event: TransportEvent) {
        _eventHistory.update { history ->
            (history + event).takeLast(100)  // الاحتفاظ بآخر 100 حدث
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Save/Load
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ الحالة
     */
    fun saveState(): TransportSystemState {
        return TransportSystemState(
            stations = _stations.value,
            routes = _routes.value,
            activeTrip = _activeTrip.value,
            ownedSeasonPasses = _ownedSeasonPasses.value,
            discoveredStations = _discoveredStations.value,
            unlockedStations = _unlockedStations.value,
            totalTripsCount = _totalTripsCount.value,
            totalCoinsSpent = _totalCoinsSpent.value,
            eventHistory = _eventHistory.value
        )
    }
    
    /**
     * تحميل الحالة
     */
    fun loadState(state: TransportSystemState) {
        _stations.value = state.stations
        _routes.value = state.routes
        _activeTrip.value = state.activeTrip
        _ownedSeasonPasses.value = state.ownedSeasonPasses
        _discoveredStations.value = state.discoveredStations
        _unlockedStations.value = state.unlockedStations
        _totalTripsCount.value = state.totalTripsCount
        _totalCoinsSpent.value = state.totalCoinsSpent
        _eventHistory.value = state.eventHistory
    }
    
    /**
     * إعادة تعيين
     */
    fun reset() {
        _stations.value = TransportStationDatabase.createDefaultStations()
        _routes.value = TransportRouteDatabase.createDefaultRoutes()
        _activeTrip.value = null
        _ownedSeasonPasses.value = emptyList()
        _discoveredStations.value = setOf("ferry_ashen_port")
        _unlockedStations.value = setOf("ferry_ashen_port")
        _totalTripsCount.value = 0
        _totalCoinsSpent.value = 0
        _eventHistory.value = emptyList()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Helper Functions
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * الحصول على لون أيقونة وسيلة نقل
 */
fun getTransportIconColor(type: TransportType): Color = when (type) {
    TransportType.RIVER_FERRY -> Color(0xFF42A5F5)
    TransportType.ROPE_GONDOLA -> Color(0xFF26C6DA)
    TransportType.CLOCKWORK_FERRY -> Color(0xFF558B2F)
    TransportType.SKY_BARGE -> Color(0xFF66BB6A)
    TransportType.ROOT_TUNNEL -> Color(0xFF5D4037)
    TransportType.BRIDGE -> Color(0xFF8D6E63)
    TransportType.TELEPORTER -> Color(0xFF7E57C2)
    TransportType.LADDER -> Color(0xFF9E9E9E)
    TransportType.ROPE -> Color(0xFFA1887F)
    TransportType.ELEVATOR -> Color(0xFF78909C)
}

/**
 * الحصول على اسم حالة بالعربية
 */
fun getStatusNameArabic(status: TransportStatus): String = when (status) {
    TransportStatus.LOCKED -> "مقفلة"
    TransportStatus.UNDER_REPAIR -> "تحت الإصلاح"
    TransportStatus.AVAILABLE -> "متاحة"
    TransportStatus.IN_USE -> "قيد الاستخدام"
    TransportStatus.BROKEN -> "معطلة"
    TransportStatus.UNAVAILABLE -> "غير متاحة"
    TransportStatus.DESTROYED -> "مدمرة"
}

/**
 * تنسيق وقت السفر
 */
fun formatTravelTime(timeMs: Long): String {
    val seconds = (timeMs / 1000) % 60
    val minutes = (timeMs / 1000 / 60) % 60
    val hours = (timeMs / 1000 / 60 / 60)
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}