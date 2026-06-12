package com.erygra.maskoflight.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.enemy.EnemyType
import com.erygra.maskoflight.player.AbilityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * World Events System — Mask of Light (Erygra Universe)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * نظام الأحداث العالمية العشوائية:
 * - 7 أنواع أحداث رئيسية
 * - أحداث مرتبطة بـ FM (Forgetfulness Meter)
 * - أحداث إقليمية خاصة
 * - نظام احتمالات ديناميكي
 * - مكافآت وعقوبات
 * - تكامل مع جميع الأنظمة
 *
 * الأحداث:
 * 1. Memory Storm — عاصفة ذاكرة (3x MF gains، 2x FM generation)
 * 2. Remnant Uprising — انتفاضة البقايا (موجة أعداء)
 * 3. Name Auction — مزاد أسماء (مزايدة أو سرقة)
 * 4. Caravan Spawn — قافلة تجارية (خصومات)
 * 5. Wandering Gearwright — صانع متجول (ترقيات نادرة)
 * 6. Lost Child Echo — طفل ضائع (إنقاذ)
 * 7. Regional Events — أحداث إقليمية خاصة
 *
 * @author Erygra Development Team
 * @version 2.0.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════════
// World Event Enums
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * أنواع الأحداث العالمية
 */
enum class WorldEventType {
    MEMORY_STORM,           // عاصفة ذاكرة
    REMNANT_UPRISING,       // انتفاضة البقايا
    NAME_AUCTION,           // مزاد أسماء
    CARAVAN_SPAWN,          // قافلة تجارية
    WANDERING_GEARWRIGHT,   // صانع متجول
    LOST_CHILD_ECHO,        // طفل ضائع
    REGIONAL_EVENT,         // حدث إقليمي خاص
    MERCHANT_FESTIVAL,      // مهرجان تجاري
    SANCTUARY_PILGRIMAGE,   // حج الملاجئ
    MEMORY_CONVERGENCE,     // تقارب الذكريات
    VOID_INCURSION,         // اجتياح الفراغ
    CELESTIAL_ALIGNMENT     // محاذاة سماوية
}

/**
 * حالة الحدث
 */
enum class WorldEventStatus {
    PENDING,        // معلق (في الانتظار)
    ACTIVE,         // نشط
    ONGOING,        // مستمر
    COMPLETED,      // مكتمل
    FAILED,         // فشل
    EXPIRED,        // منتهي
    CANCELLED       // ملغى
}

/**
 * فئة الحدث
 */
enum class EventCategory {
    COMBAT,         // قتالي
    ECONOMIC,       // اقتصادي
    SOCIAL,         // اجتماعي
    EXPLORATION,    // استكشافي
    NARRATIVE,      // سردي
    ENVIRONMENTAL,  // بيئي
    MIXED           // مختلط
}

/**
 * أولوية الحدث
 */
enum class EventPriority(val value: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4),
    STORY(5)
}

// ═══════════════════════════════════════════════════════════════════════════════
// World Event Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * حدث عالمي
 */
data class WorldEvent(
    val id: String,
    val type: WorldEventType,
    val category: EventCategory,
    val priority: EventPriority = EventPriority.NORMAL,
    
    // الأساسيات:
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    
    // الموقع:
    val region: RegionType? = null,         // null = عالمي
    val specificLocation: Offset? = null,    // null = في أي مكان بالمنطقة
    val radius: Float = 50f,                 // نطاق التأثير
    
    // التوقيت:
    val startTime: Long = System.currentTimeMillis(),
    val durationMs: Long = 1800000L,         // 30 دقيقة افتراضياً
    val expiresAt: Long = startTime + durationMs,
    
    // الحالة:
    val status: WorldEventStatus = WorldEventStatus.PENDING,
    val progress: Float = 0f,                // 0.0-1.0
    
    // المتطلبات:
    val minPlayerLevel: Int = 1,
    val maxPlayerLevel: Int = 99,
    val requiredQuests: List<String> = emptyList(),
    val requiredAbilities: List<AbilityType> = emptyList(),
    val fmThreshold: Pair<Int, Int>? = null, // (min, max) FM للظهور
    
    // التأثيرات:
    val effectMultipliers: Map<String, Float> = emptyMap(),  // "mf_gain" -> 3.0, "fm_gen" -> 2.0
    val environmentalEffects: List<String> = emptyList(),    // "fog", "storm", "darkness"
    val weatherOverride: WeatherType? = null,
    
    // المكافآت:
    val coinReward: Int = 0,
    val mfReward: Int = 0,
    val xpReward: Int = 0,
    val itemRewards: List<String> = emptyList(),
    val fmReduction: Int = 0,
    
    // العقوبات:
    val failurePenalty: Int = 0,             // Coins/MF lost on failure
    val fmPenalty: Int = 0,                  // FM gained on failure
    
    // الأهداف:
    val objectives: List<EventObjective> = emptyList(),
    val enemyWaves: List<EnemyWaveData> = emptyList(),
    
    // التفاعل:
    val isPlayerTriggered: Boolean = false,   // هل بدأ بواسطة اللاعب؟
    val canBeSkipped: Boolean = false,
    val canBeFailed: Boolean = true,
    val isRepeatable: Boolean = true,
    
    // القصة:
    val hasDialogue: Boolean = false,
    val dialogueId: String? = null,
    val triggersMemory: Boolean = false,
    val memoryId: String? = null,
    
    // الإحصائيات:
    val participationCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0
) {
    /**
     * هل الحدث نشط؟
     */
    fun isActive(): Boolean =
        status == WorldEventStatus.ACTIVE || status == WorldEventStatus.ONGOING
    
    /**
     * هل الحدث منتهي؟
     */
    fun isExpired(): Boolean =
        System.currentTimeMillis() > expiresAt || status == WorldEventStatus.EXPIRED
    
    /**
     * الوقت المتبقي
     */
    fun getRemainingTimeMs(): Long =
        kotlin.math.max(0, expiresAt - System.currentTimeMillis())
    
    /**
     * هل اللاعب ضمن نطاق الحدث؟
     */
    fun isPlayerInRange(playerPosition: Offset): Boolean {
        if (specificLocation == null) return true
        val distance = kotlin.math.sqrt(
            (specificLocation.x - playerPosition.x) * (specificLocation.x - playerPosition.x) +
            (specificLocation.y - playerPosition.y) * (specificLocation.y - playerPosition.y)
        )
        return distance <= radius
    }
}

/**
 * هدف حدث
 */
data class EventObjective(
    val id: String,
    val description: String,
    val descriptionArabic: String,
    val type: ObjectiveType,
    val targetValue: Int,                    // القيمة المطلوبة
    val currentValue: Int = 0,               // القيمة الحالية
    val isCompleted: Boolean = false,
    val isOptional: Boolean = false
)

/**
 * نوع الهدف
 */
enum class ObjectiveType {
    KILL_ENEMIES,           // اقتل X أعداء
    SURVIVE_DURATION,       // اصمد لمدة X
    DEFEND_LOCATION,        // دافع عن موقع
    COLLECT_ITEMS,          // اجمع X عناصر
    REACH_LOCATION,         // اصل إلى موقع
    ESCORT_NPC,             // اصطحب NPC
    SOLVE_PUZZLE,           // حل لغز
    DEFEAT_BOSS,            // اهزم زعيماً
    PROTECT_NPC,            // احمِ NPC
    TALK_TO_NPC             // تحدث إلى NPC
}

/**
 * موجة أعداء
 */
data class EnemyWaveData(
    val waveNumber: Int,
    val enemies: List<EnemySpawnData>,
    val delayMs: Long = 0L,                  // تأخير قبل الموجة
    val spawnPattern: SpawnPattern = SpawnPattern.RANDOM,
    val spawnCenter: Offset? = null
)

/**
 * بيانات ظهور عدو
 */
data class EnemySpawnData(
    val type: EnemyType,
    val count: Int,
    val level: Int = 1,
    val spawnDelayMs: Long = 0L
)

/**
 * نمط الظهور
 */
enum class SpawnPattern {
    RANDOM,         // عشوائي
    CIRCLE,         // دائري
    LINE,           // خطي
    BEHIND,         // خلف اللاعب
    SIDES,          // جانبي
    ABOVE,          // من الأعلى
    PORTAL          // من بوابة
}

/**
 * حالة نظام الأحداث (للحفظ/التحميل)
 */
data class WorldEventsState(
    val activeEvents: List<WorldEvent>,
    val completedEventIds: Set<String>,
    val failedEventIds: Set<String>,
    val eventHistory: List<EventHistoryEntry>,
    val totalEventsTriggered: Int,
    val totalEventsCompleted: Int,
    val totalEventsFailed: Int,
    val lastEventTime: Long? = null
)

/**
 * سجل حدث
 */
data class EventHistoryEntry(
    val eventId: String,
    val eventType: WorldEventType,
    val startTime: Long,
    val endTime: Long,
    val status: WorldEventStatus,
    val participationReward: Int,
    val fmChange: Int
)

// ═══════════════════════════════════════════════════════════════════════════════
// World Event Database
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات قوالب الأحداث
 */
object WorldEventDatabase {
    
    /**
     * إنشاء حدث Memory Storm
     */
    fun createMemoryStorm(region: RegionType): WorldEvent {
        return WorldEvent(
            id = "memory_storm_${System.currentTimeMillis()}",
            type = WorldEventType.MEMORY_STORM,
            category = EventCategory.ENVIRONMENTAL,
            priority = EventPriority.HIGH,
            name = "Memory Storm",
            nameArabic = "عاصفة الذاكرة",
            description = "A violent storm of forgotten memories sweeps through the region. " +
                    "Memory Fragments are more abundant, but Forgetfulness builds faster.",
            descriptionArabic = "عاصفة عنيفة من الذكريات المنسية تجتاح المنطقة. شظايا الذاكرة " +
                    "أكثر وفرة، لكن النسيان يتراكم أسرع.",
            region = region,
            durationMs = 1800000L,  // 30 minutes
            effectMultipliers = mapOf(
                "mf_gain" to 3.0f,
                "fm_generation" to 2.0f,
                "enemy_spawn_rate" to 1.5f
            ),
            environmentalEffects = listOf("memory_particles", "static", "distortion"),
            weatherOverride = WeatherType.GLOWING_PARTICLES,
            coinReward = 200,
            mfReward = 5,
            xpReward = 500,
            objectives = listOf(
                EventObjective(
                    id = "survive_storm",
                    description = "Survive the Memory Storm",
                    descriptionArabic = "اصمد أمام عاصفة الذاكرة",
                    type = ObjectiveType.SURVIVE_DURATION,
                    targetValue = 1800  // 30 minutes in seconds
                ),
                EventObjective(
                    id = "collect_storm_fragments",
                    description = "Collect 10 Memory Fragments during the storm",
                    descriptionArabic = "اجمع 10 شظايا ذاكرة أثناء العاصفة",
                    type = ObjectiveType.COLLECT_ITEMS,
                    targetValue = 10,
                    isOptional = true
                )
            ),
            isRepeatable = true
        )
    }
    
    /**
     * إنشاء حدث Remnant Uprising
     */
    fun createRemnantUprising(region: RegionType, playerLevel: Int, fm: Int): WorldEvent {
        // شدة الموجة تعتمد على FM
        val intensity = when {
            fm < 5 -> 1
            fm < 10 -> 2
            fm < 15 -> 3
            else -> 4
        }
        
        val waves = (1..intensity).map { waveNum ->
            EnemyWaveData(
                waveNumber = waveNum,
                enemies = listOf(
                    EnemySpawnData(
                        type = getRegionalEnemy(region),
                        count = 5 + (waveNum * 2),
                        level = playerLevel + waveNum - 1
                    )
                ),
                delayMs = (waveNum - 1) * 60000L,  // 1 minute between waves
                spawnPattern = SpawnPattern.CIRCLE
            )
        }
        
        return WorldEvent(
            id = "uprising_${region.name}_${System.currentTimeMillis()}",
            type = WorldEventType.REMNANT_UPRISING,
            category = EventCategory.COMBAT,
            priority = EventPriority.HIGH,
            name = "Remnant Uprising",
            nameArabic = "انتفاضة البقايا",
            description = "Remnants of the forgotten rise up against the living. " +
                    "Defend the region from waves of enemies.",
            descriptionArabic = "بقايا المنسيين تنهض ضد الأحياء. دافع عن المنطقة من موجات الأعداء.",
            region = region,
            durationMs = intensity * 60000L,  // 1 minute per wave
            fmThreshold = Pair(5, 99),  // يحدث فقط إذا FM >= 5
            enemyWaves = waves,
            coinReward = intensity * 150,
            mfReward = intensity * 2,
            xpReward = intensity * 300,
            fmReduction = intensity * 3,  // مكافأة تقليل FM
            fmPenalty = intensity * 2,    // عقوبة زيادة FM عند الفشل
            objectives = listOf(
                EventObjective(
                    id = "survive_waves",
                    description = "Survive all $intensity waves",
                    descriptionArabic = "اصمد أمام كل الموجات الـ$intensity",
                    type = ObjectiveType.SURVIVE_DURATION,
                    targetValue = intensity * 60
                ),
                EventObjective(
                    id = "protect_npcs",
                    description = "Keep NPCs alive",
                    descriptionArabic = "حافظ على حياة الشخصيات",
                    type = ObjectiveType.PROTECT_NPC,
                    targetValue = 3,
                    isOptional = true
                )
            ),
            canBeFailed = true,
            isRepeatable = true
        )
    }
    
    /**
     * إنشاء حدث Name Auction
     */
    fun createNameAuction(region: RegionType): WorldEvent {
        return WorldEvent(
            id = "auction_${System.currentTimeMillis()}",
            type = WorldEventType.NAME_AUCTION,
            category = EventCategory.SOCIAL,
            priority = EventPriority.NORMAL,
            name = "Name Auction",
            nameArabic = "مزاد الأسماء",
            description = "A rare Name Auction is taking place. Bid for forgotten names " +
                    "or steal them while the bidders are distracted.",
            descriptionArabic = "مزاد نادر للأسماء يجري الآن. زايد على أسماء منسية أو اسرقها " +
                    "بينما المزايدون مشتتون.",
            region = region,
            durationMs = 3600000L,  // 1 hour
            minPlayerLevel = 5,
            coinReward = 500,
            itemRewards = listOf("rare_name_scroll", "memory_echo"),
            objectives = listOf(
                EventObjective(
                    id = "win_auction",
                    description = "Win at least one name in the auction",
                    descriptionArabic = "اربح اسماً واحداً على الأقل في المزاد",
                    type = ObjectiveType.COLLECT_ITEMS,
                    targetValue = 1
                ),
                EventObjective(
                    id = "steal_name",
                    description = "Steal a name during the chaos",
                    descriptionArabic = "اسرق اسماً أثناء الفوضى",
                    type = ObjectiveType.COLLECT_ITEMS,
                    targetValue = 1,
                    isOptional = true
                )
            ),
            hasDialogue = true,
            dialogueId = "name_auction_intro",
            isRepeatable = false  // يحدث مرة واحدة فقط
        )
    }
    
    /**
     * إنشاء حدث Caravan Spawn
     */
    fun createCaravanSpawn(region: RegionType, location: Offset): WorldEvent {
        return WorldEvent(
            id = "caravan_${System.currentTimeMillis()}",
            type = WorldEventType.CARAVAN_SPAWN,
            category = EventCategory.ECONOMIC,
            priority = EventPriority.NORMAL,
            name = "Traveling Caravan",
            nameArabic = "قافلة متجولة",
            description = "A traveling caravan has arrived, offering rare items at discounted prices.",
            descriptionArabic = "وصلت قافلة متجولة، تقدم عناصر نادرة بأسعار مخفضة.",
            region = region,
            specificLocation = location,
            radius = 15f,
            durationMs = 1200000L,  // 20 minutes
            effectMultipliers = mapOf(
                "shop_discount" to 0.8f,  // 20% خصم
                "rare_item_chance" to 2.0f
            ),
            objectives = listOf(
                EventObjective(
                    id = "visit_caravan",
                    description = "Visit the caravan",
                    descriptionArabic = "زر القافلة",
                    type = ObjectiveType.REACH_LOCATION,
                    targetValue = 1
                )
            ),
            hasDialogue = true,
            dialogueId = "caravan_merchant",
            isRepeatable = true
        )
    }
    
    /**
     * إنشاء حدث Wandering Gearwright
     */
    fun createWanderingGearwright(region: RegionType, location: Offset): WorldEvent {
        return WorldEvent(
            id = "gearwright_${System.currentTimeMillis()}",
            type = WorldEventType.WANDERING_GEARWRIGHT,
            category = EventCategory.ECONOMIC,
            priority = EventPriority.NORMAL,
            name = "Wandering Gearwright",
            nameArabic = "صانع التروس المتجول",
            description = "A skilled Gearwright is offering weapon and satchel upgrades.",
            descriptionArabic = "صانع تروس ماهر يقدم ترقيات للأسلحة والشنط.",
            region = region,
            specificLocation = location,
            radius = 10f,
            durationMs = 900000L,  // 15 minutes
            minPlayerLevel = 3,
            itemRewards = listOf("weapon_upgrade_blueprint", "satchel_expansion_kit"),
            objectives = listOf(
                EventObjective(
                    id = "talk_to_gearwright",
                    description = "Talk to the Gearwright",
                    descriptionArabic = "تحدث إلى صانع التروس",
                    type = ObjectiveType.TALK_TO_NPC,
                    targetValue = 1
                )
            ),
            hasDialogue = true,
            dialogueId = "gearwright_greeting",
            isRepeatable = true
        )
    }
    
    /**
     * إنشاء حدث Lost Child Echo
     */
    fun createLostChildEcho(region: RegionType, location: Offset): WorldEvent {
        return WorldEvent(
            id = "child_echo_${System.currentTimeMillis()}",
            type = WorldEventType.LOST_CHILD_ECHO,
            category = EventCategory.NARRATIVE,
            priority = EventPriority.HIGH,
            name = "Lost Child Echo",
            nameArabic = "صدى طفل ضائع",
            description = "A child's echo is lost in the region. Find and guide them to safety.",
            descriptionArabic = "صدى طفل ضائع في المنطقة. ابحث عنه واصطحبه إلى الأمان.",
            region = region,
            specificLocation = location,
            radius = 20f,
            durationMs = 600000L,  // 10 minutes
            mfReward = 3,
            fmReduction = 5,
            objectives = listOf(
                EventObjective(
                    id = "find_child",
                    description = "Find the lost child",
                    descriptionArabic = "ابحث عن الطفل الضائع",
                    type = ObjectiveType.REACH_LOCATION,
                    targetValue = 1
                ),
                EventObjective(
                    id = "escort_child",
                    description = "Escort the child to safety",
                    descriptionArabic = "اصطحب الطفل إلى الأمان",
                    type = ObjectiveType.ESCORT_NPC,
                    targetValue = 1
                )
            ),
            hasDialogue = true,
            dialogueId = "lost_child_intro",
            triggersMemory = true,
            memoryId = "childhood_memory_fragment",
            canBeFailed = true,
            fmPenalty = 5,  // عقوبة FM إذا فشل
            isRepeatable = true
        )
    }
    
    /**
     * إنشاء حدث إقليمي خاص
     */
    fun createRegionalEvent(region: RegionType): WorldEvent? {
        return when (region) {
            RegionType.ASHEN_SPRAWL -> WorldEvent(
                id = "ashen_heat_wave_${System.currentTimeMillis()}",
                type = WorldEventType.REGIONAL_EVENT,
                category = EventCategory.ENVIRONMENTAL,
                name = "Scorching Heat Wave",
                nameArabic = "موجة حر حارقة",
                description = "An extreme heat wave makes the ash glow red-hot.",
                descriptionArabic = "موجة حر شديدة تجعل الرماد يتوهج باللون الأحمر الساخن.",
                region = region,
                durationMs = 2400000L,  // 40 minutes
                environmentalEffects = listOf("heat_distortion", "ember_particles"),
                effectMultipliers = mapOf(
                    "fire_damage" to 1.5f,
                    "movement_speed" to 0.9f
                ),
                isRepeatable = true
            )
            
            RegionType.VEILED_ARCHIVES -> WorldEvent(
                id = "archive_memory_overflow_${System.currentTimeMillis()}",
                type = WorldEventType.REGIONAL_EVENT,
                category = EventCategory.ENVIRONMENTAL,
                name = "Memory Overflow",
                nameArabic = "فيضان الذاكرة",
                description = "The archives overflow with escaped memories, creating temporal anomalies.",
                descriptionArabic = "تفيض الأرشيفات بذكريات هاربة، تخلق شذوذات زمنية.",
                region = region,
                durationMs = 1800000L,
                environmentalEffects = listOf("temporal_distortion", "memory_echoes"),
                mfReward = 10,
                isRepeatable = true
            )
            
            RegionType.HOLLOWED_ARCHIPELAGO -> WorldEvent(
                id = "archipelago_wind_surge_${System.currentTimeMillis()}",
                type = WorldEventType.REGIONAL_EVENT,
                category = EventCategory.ENVIRONMENTAL,
                name = "Wind Surge",
                nameArabic = "اندفاع الرياح",
                description = "Powerful winds shake the islands, making traversal challenging.",
                descriptionArabic = "رياح قوية تهز الجزر، تجعل الحركة صعبة.",
                region = region,
                durationMs = 1200000L,
                environmentalEffects = listOf("strong_wind", "rope_sway"),
                effectMultipliers = mapOf(
                    "jump_height" to 1.3f,
                    "movement_speed" to 0.8f
                ),
                isRepeatable = true
            )
            
            RegionType.GLASSFJORD_CLIFFS -> WorldEvent(
                id = "glass_aurora_bloom_${System.currentTimeMillis()}",
                type = WorldEventType.REGIONAL_EVENT,
                category = EventCategory.ENVIRONMENTAL,
                name = "Aurora Bloom",
                nameArabic = "ازدهار الشفق",
                description = "The aurora intensifies, revealing hidden reflections.",
                descriptionArabic = "يشتد الشفق، يكشف انعكاسات خفية.",
                region = region,
                durationMs = 3600000L,
                environmentalEffects = listOf("aurora_glow", "mirror_reflections"),
                effectMultipliers = mapOf(
                    "discovery_radius" to 1.5f
                ),
                itemRewards = listOf("aurora_crystal"),
                isRepeatable = true
            )
            
            RegionType.SUNKEN_CLOCKWORKS -> WorldEvent(
                id = "clockwork_surge_${System.currentTimeMillis()}",
                type = WorldEventType.REGIONAL_EVENT,
                category = EventCategory.ENVIRONMENTAL,
                name = "Clockwork Surge",
                nameArabic = "اندفاع الساعات",
                description = "The gears accelerate, time flows faster.",
                descriptionArabic = "تتسارع التروس، يتدفق الزمن أسرع.",
                region = region,
                durationMs = 1800000L,
                environmentalEffects = listOf("gear_acceleration", "time_distortion"),
                effectMultipliers = mapOf(
                    "cooldown_reduction" to 0.75f,
                    "movement_speed" to 1.2f
                ),
                isRepeatable = true
            )
            
            RegionType.BLACKROOT_MOORLANDS -> WorldEvent(
                id = "moor_bloom_${System.currentTimeMillis()}",
                type = WorldEventType.REGIONAL_EVENT,
                category = EventCategory.ENVIRONMENTAL,
                name = "Toxic Bloom",
                nameArabic = "ازدهار سام",
                description = "The roots bloom with toxic flowers, poisoning the air.",
                descriptionArabic = "تزدهر الجذور بأزهار سامة، تسمم الهواء.",
                region = region,
                durationMs = 2400000L,
                environmentalEffects = listOf("toxic_fog", "spore_particles"),
                effectMultipliers = mapOf(
                    "poison_resistance" to 0.7f
                ),
                itemRewards = listOf("toxic_flower_extract"),
                isRepeatable = true
            )
            
            RegionType.LUMINOUS_CHASM -> WorldEvent(
                id = "chasm_convergence_${System.currentTimeMillis()}",
                type = WorldEventType.REGIONAL_EVENT,
                category = EventCategory.ENVIRONMENTAL,
                name = "Memory Convergence",
                nameArabic = "تقارب الذاكرة",
                description = "All memories converge at the chasm's heart, creating a surge of light.",
                descriptionArabic = "تتقارب كل الذكريات في قلب الهاوية، تخلق اندفاعاً من النور.",
                region = region,
                durationMs = 3600000L,
                environmentalEffects = listOf("light_surge", "memory_vortex"),
                mfReward = 15,
                fmReduction = 10,
                isRepeatable = true
            )
        }
    }
    
    /**
     * الحصول على عدو إقليمي مناسب
     */
    private fun getRegionalEnemy(region: RegionType): EnemyType {
        return when (region) {
            RegionType.ASHEN_SPRAWL -> EnemyType.SCRAB_SCAVENGER
            RegionType.VEILED_ARCHIVES -> EnemyType.PAGE_SCRAPER
            RegionType.HOLLOWED_ARCHIPELAGO -> EnemyType.ROPE_CROAKER
            RegionType.GLASSFJORD_CLIFFS -> EnemyType.SHARDLING
            RegionType.SUNKEN_CLOCKWORKS -> EnemyType.GEARFOLK
            RegionType.BLACKROOT_MOORLANDS -> EnemyType.ROOTCRAWLER
            RegionType.LUMINOUS_CHASM -> EnemyType.GLOW_WISP
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// World Events Manager
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مدير الأحداث العالمية
 */
class WorldEventsManager {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════
    
    private val _activeEvents = MutableStateFlow<List<WorldEvent>>(emptyList())
    val activeEvents: StateFlow<List<WorldEvent>> = _activeEvents.asStateFlow()
    
    private val _completedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val completedEventIds: StateFlow<Set<String>> = _completedEventIds.asStateFlow()
    
    private val _failedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val failedEventIds: StateFlow<Set<String>> = _failedEventIds.asStateFlow()
    
    private val _eventHistory = MutableStateFlow<List<EventHistoryEntry>>(emptyList())
    val eventHistory: StateFlow<List<EventHistoryEntry>> = _eventHistory.asStateFlow()
    
    private val _totalEventsTriggered = MutableStateFlow(0)
    val totalEventsTriggered: StateFlow<Int> = _totalEventsTriggered.asStateFlow()
    
    private val _totalEventsCompleted = MutableStateFlow(0)
    val totalEventsCompleted: StateFlow<Int> = _totalEventsCompleted.asStateFlow()
    
    private val _totalEventsFailed = MutableStateFlow(0)
    val totalEventsFailed: StateFlow<Int> = _totalEventsFailed.asStateFlow()
    
    private val _lastEventTime = MutableStateFlow<Long?>(null)
    val lastEventTime: StateFlow<Long?> = _lastEventTime.asStateFlow()
    
    // Event spawn chances (per hour of gameplay)
    private val eventSpawnChances = mapOf(
        WorldEventType.MEMORY_STORM to 0.02f,           // 2%
        WorldEventType.REMNANT_UPRISING to 0.0f,        // Depends on FM
        WorldEventType.NAME_AUCTION to 0.01f,           // 1% per day
        WorldEventType.CARAVAN_SPAWN to 0.05f,          // 5%
        WorldEventType.WANDERING_GEARWRIGHT to 0.03f,   // 3%
        WorldEventType.LOST_CHILD_ECHO to 0.01f,        // 1%
        WorldEventType.REGIONAL_EVENT to 0.04f          // 4%
    )
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Event Generation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * تحديث (يُستدعى كل إطار)
     */
    fun update(
        deltaTimeMs: Long,
        currentRegion: RegionType,
        playerPosition: Offset,
        playerLevel: Int,
        playerFM: Int
    ) {
        // تحديث الأحداث النشطة
        updateActiveEvents(deltaTimeMs, playerPosition)
        
        // محاولة توليد أحداث جديدة (كل ساعة لعبة)
        tryGenerateNewEvents(currentRegion, playerPosition, playerLevel, playerFM)
    }
    
    /**
     * تحديث الأحداث النشطة
     */
    private fun updateActiveEvents(deltaTimeMs: Long, playerPosition: Offset) {
        _activeEvents.update { events ->
            events.mapNotNull { event ->
                when {
                    event.isExpired() -> {
                        // انتهى الحدث
                        expireEvent(event)
                        null
                    }
                    event.status == WorldEventStatus.PENDING && event.isPlayerInRange(playerPosition) -> {
                        // تفعيل الحدث
                        event.copy(status = WorldEventStatus.ACTIVE)
                    }
                    else -> event
                }
            }
        }
    }
    
    /**
     * محاولة توليد أحداث جديدة
     */
    private fun tryGenerateNewEvents(
        currentRegion: RegionType,
        playerPosition: Offset,
        playerLevel: Int,
        playerFM: Int
    ) {
        // تحقق من الحد الأقصى للأحداث النشطة
        if (_activeEvents.value.size >= 3) return
        
        // Memory Storm
        if (Random.nextFloat() < eventSpawnChances[WorldEventType.MEMORY_STORM]!!) {
            val event = WorldEventDatabase.createMemoryStorm(currentRegion)
            startEvent(event)
        }
        
        // Remnant Uprising (يعتمد على FM)
        val uprisingChance = when {
            playerFM < 5 -> 0.0f
            playerFM < 10 -> 0.05f
            playerFM < 15 -> 0.10f
            else -> 0.15f
        }
        if (Random.nextFloat() < uprisingChance) {
            val event = WorldEventDatabase.createRemnantUprising(currentRegion, playerLevel, playerFM)
            startEvent(event)
        }
        
        // Name Auction (نادر جداً)
        if (Random.nextFloat() < eventSpawnChances[WorldEventType.NAME_AUCTION]!! / 24f) {  // per hour
            val event = WorldEventDatabase.createNameAuction(currentRegion)
            if (!_completedEventIds.value.contains(event.id)) {
                startEvent(event)
            }
        }
        
        // Caravan Spawn
        if (Random.nextFloat() < eventSpawnChances[WorldEventType.CARAVAN_SPAWN]!!) {
            val location = generateRandomLocation(currentRegion, playerPosition)
            val event = WorldEventDatabase.createCaravanSpawn(currentRegion, location)
            startEvent(event)
        }
        
        // Wandering Gearwright
        if (Random.nextFloat() < eventSpawnChances[WorldEventType.WANDERING_GEARWRIGHT]!!) {
            val location = generateRandomLocation(currentRegion, playerPosition)
            val event = WorldEventDatabase.createWanderingGearwright(currentRegion, location)
            startEvent(event)
        }
        
        // Lost Child Echo
        if (Random.nextFloat() < eventSpawnChances[WorldEventType.LOST_CHILD_ECHO]!!) {
            val location = generateRandomLocation(currentRegion, playerPosition)
            val event = WorldEventDatabase.createLostChildEcho(currentRegion, location)
            startEvent(event)
        }
        
        // Regional Event
        if (Random.nextFloat() < eventSpawnChances[WorldEventType.REGIONAL_EVENT]!!) {
            val event = WorldEventDatabase.createRegionalEvent(currentRegion)
            if (event != null) {
                startEvent(event)
            }
        }
    }
    
    /**
     * توليد موقع عشوائي في المنطقة
     */
    private fun generateRandomLocation(region: RegionType, playerPosition: Offset): Offset {
        // توليد موقع بالقرب من اللاعب (±30 وحدة)
        val offsetX = Random.nextFloat() * 60f - 30f
        val offsetY = Random.nextFloat() * 60f - 30f
        return Offset(playerPosition.x + offsetX, playerPosition.y + offsetY)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Event Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء حدث
     */
    fun startEvent(event: WorldEvent) {
        _activeEvents.update { it + event.copy(status = WorldEventStatus.PENDING) }
        _totalEventsTriggered.update { it + 1 }
        _lastEventTime.value = System.currentTimeMillis()
    }
    
    /**
     * تفعيل حدث
     */
    fun activateEvent(eventId: String) {
        _activeEvents.update { events ->
            events.map { event ->
                if (event.id == eventId && event.status == WorldEventStatus.PENDING) {
                    event.copy(status = WorldEventStatus.ACTIVE)
                } else {
                    event
                }
            }
        }
    }
    
    /**
     * تحديث تقدم حدث
     */
    fun updateEventProgress(eventId: String, progress: Float) {
        _activeEvents.update { events ->
            events.map { event ->
                if (event.id == eventId) {
                    event.copy(
                        progress = progress.coerceIn(0f, 1f),
                        status = if (progress >= 1f) WorldEventStatus.COMPLETED else event.status
                    )
                } else {
                    event
                }
            }
        }
        
        // إكمال تلقائي إذا وصل التقدم 100%
        val event = _activeEvents.value.find { it.id == eventId }
        if (event != null && event.progress >= 1f) {
            completeEvent(eventId)
        }
    }
    
    /**
     * تحديث هدف حدث
     */
    fun updateObjective(eventId: String, objectiveId: String, currentValue: Int) {
        _activeEvents.update { events ->
            events.map { event ->
                if (event.id == eventId) {
                    val updatedObjectives = event.objectives.map { objective ->
                        if (objective.id == objectiveId) {
                            objective.copy(
                                currentValue = currentValue,
                                isCompleted = currentValue >= objective.targetValue
                            )
                        } else {
                            objective
                        }
                    }
                    
                    // حساب التقدم الكلي
                    val requiredObjectives = updatedObjectives.filter { !it.isOptional }
                    val completedRequired = requiredObjectives.count { it.isCompleted }
                    val newProgress = if (requiredObjectives.isNotEmpty()) {
                        completedRequired.toFloat() / requiredObjectives.size
                    } else {
                        0f
                    }
                    
                    event.copy(
                        objectives = updatedObjectives,
                        progress = newProgress
                    )
                } else {
                    event
                }
            }
        }
    }
    
    /**
     * إكمال حدث
     */
    fun completeEvent(eventId: String) {
        val event = _activeEvents.value.find { it.id == eventId } ?: return
        
        _activeEvents.update { events ->
            events.filter { it.id != eventId }
        }
        
        _completedEventIds.update { it + eventId }
        _totalEventsCompleted.update { it + 1 }
        
        // تسجيل في التاريخ
        _eventHistory.update { history ->
            (history + EventHistoryEntry(
                eventId = eventId,
                eventType = event.type,
                startTime = event.startTime,
                endTime = System.currentTimeMillis(),
                status = WorldEventStatus.COMPLETED,
                participationReward = event.coinReward + event.xpReward,
                fmChange = -event.fmReduction
            )).takeLast(100)
        }
    }
    
    /**
     * فشل حدث
     */
    fun failEvent(eventId: String) {
        val event = _activeEvents.value.find { it.id == eventId } ?: return
        
        _activeEvents.update { events ->
            events.filter { it.id != eventId }
        }
        
        _failedEventIds.update { it + eventId }
        _totalEventsFailed.update { it + 1 }
        
        // تسجيل في التاريخ
        _eventHistory.update { history ->
            (history + EventHistoryEntry(
                eventId = eventId,
                eventType = event.type,
                startTime = event.startTime,
                endTime = System.currentTimeMillis(),
                status = WorldEventStatus.FAILED,
                participationReward = -event.failurePenalty,
                fmChange = event.fmPenalty
            )).takeLast(100)
        }
    }
    
    /**
     * انتهاء صلاحية حدث
     */
    private fun expireEvent(event: WorldEvent) {
        _activeEvents.update { events ->
            events.filter { it.id != event.id }
        }
        
        // تسجيل في التاريخ
        _eventHistory.update { history ->
            (history + EventHistoryEntry(
                eventId = event.id,
                eventType = event.type,
                startTime = event.startTime,
                endTime = System.currentTimeMillis(),
                status = WorldEventStatus.EXPIRED,
                participationReward = 0,
                fmChange = 0
            )).takeLast(100)
        }
    }
    
    /**
     * إلغاء حدث
     */
    fun cancelEvent(eventId: String) {
        _activeEvents.update { events ->
            events.filter { it.id != eventId }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Queries
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على حدث
     */
    fun getEvent(eventId: String): WorldEvent? =
        _activeEvents.value.find { it.id == eventId }
    
    /**
     * الحصول على الأحداث النشطة في منطقة
     */
    fun getActiveEventsInRegion(region: RegionType): List<WorldEvent> =
        _activeEvents.value.filter { it.region == region && it.isActive() }
    
    /**
     * الحصول على الأحداث حسب النوع
     */
    fun getEventsByType(type: WorldEventType): List<WorldEvent> =
        _activeEvents.value.filter { it.type == type }
    
    /**
     * الحصول على الأحداث القريبة من موقع
     */
    fun getNearbyEvents(position: Offset, radius: Float): List<WorldEvent> =
        _activeEvents.value.filter { event ->
            if (event.specificLocation == null) return@filter false
            val distance = kotlin.math.sqrt(
                (event.specificLocation.x - position.x) * (event.specificLocation.x - position.x) +
                (event.specificLocation.y - position.y) * (event.specificLocation.y - position.y)
            )
            distance <= radius
        }
    
    /**
     * الحصول على معدل تأثير لحدث
     */
    fun getEffectMultiplier(eventId: String, effectName: String): Float {
        val event = getEvent(eventId) ?: return 1.0f
        return event.effectMultipliers[effectName] ?: 1.0f
    }
    
    /**
     * الحصول على معدلات التأثير المجمعة لجميع الأحداث النشطة
     */
    fun getCombinedEffectMultipliers(): Map<String, Float> {
        val combined = mutableMapOf<String, Float>()
        
        _activeEvents.value.filter { it.isActive() }.forEach { event ->
            event.effectMultipliers.forEach { (effect, multiplier) ->
                combined[effect] = (combined[effect] ?: 1.0f) * multiplier
            }
        }
        
        return combined
    }
    
    /**
     * التحقق من وجود حدث نشط من نوع معين
     */
    fun hasActiveEventOfType(type: WorldEventType): Boolean =
        _activeEvents.value.any { it.type == type && it.isActive() }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Save/Load
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ الحالة
     */
    fun saveState(): WorldEventsState {
        return WorldEventsState(
            activeEvents = _activeEvents.value,
            completedEventIds = _completedEventIds.value,
            failedEventIds = _failedEventIds.value,
            eventHistory = _eventHistory.value,
            totalEventsTriggered = _totalEventsTriggered.value,
            totalEventsCompleted = _totalEventsCompleted.value,
            totalEventsFailed = _totalEventsFailed.value,
            lastEventTime = _lastEventTime.value
        )
    }
    
    /**
     * تحميل الحالة
     */
    fun loadState(state: WorldEventsState) {
        _activeEvents.value = state.activeEvents
        _completedEventIds.value = state.completedEventIds
        _failedEventIds.value = state.failedEventIds
        _eventHistory.value = state.eventHistory
        _totalEventsTriggered.value = state.totalEventsTriggered
        _totalEventsCompleted.value = state.totalEventsCompleted
        _totalEventsFailed.value = state.totalEventsFailed
        _lastEventTime.value = state.lastEventTime
    }
    
    /**
     * إعادة تعيين
     */
    fun reset() {
        _activeEvents.value = emptyList()
        _completedEventIds.value = emptySet()
        _failedEventIds.value = emptySet()
        _eventHistory.value = emptyList()
        _totalEventsTriggered.value = 0
        _totalEventsCompleted.value = 0
        _totalEventsFailed.value = 0
        _lastEventTime.value = null
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Helper Functions
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * الحصول على لون أيقونة حدث
 */
fun getEventIconColor(type: WorldEventType): Color = when (type) {
    WorldEventType.MEMORY_STORM -> Color(0xFF9C27B0)
    WorldEventType.REMNANT_UPRISING -> Color(0xFFFF5722)
    WorldEventType.NAME_AUCTION -> Color(0xFFFDD835)
    WorldEventType.CARAVAN_SPAWN -> Color(0xFF66BB6A)
    WorldEventType.WANDERING_GEARWRIGHT -> Color(0xFF558B2F)
    WorldEventType.LOST_CHILD_ECHO -> Color(0xFF90CAF9)
    WorldEventType.REGIONAL_EVENT -> Color(0xFFFFB74D)
    WorldEventType.MERCHANT_FESTIVAL -> Color(0xFF4CAF50)
    WorldEventType.SANCTUARY_PILGRIMAGE -> Color(0xFFFFD54F)
    WorldEventType.MEMORY_CONVERGENCE -> Color(0xFFBA68C8)
    WorldEventType.VOID_INCURSION -> Color(0xFF212121)
    WorldEventType.CELESTIAL_ALIGNMENT -> Color(0xFF81D4FA)
}

/**
 * الحصول على أولوية حدث
 */
fun getEventPriorityValue(event: WorldEvent): Int = event.priority.value

/**
 * ترتيب الأحداث حسب الأولوية
 */
fun List<WorldEvent>.sortedByPriority(): List<WorldEvent> =
    this.sortedByDescending { it.priority.value }

/**
 * تنسيق وقت الحدث المتبقي
 */
fun formatEventTimeRemaining(timeMs: Long): String {
    val seconds = (timeMs / 1000) % 60
    val minutes = (timeMs / 1000 / 60) % 60
    val hours = (timeMs / 1000 / 60 / 60)
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

/**
 * الحصول على نص حالة حدث بالعربية
 */
fun getEventStatusTextArabic(status: WorldEventStatus): String = when (status) {
    WorldEventStatus.PENDING -> "معلق"
    WorldEventStatus.ACTIVE -> "نشط"
    WorldEventStatus.ONGOING -> "مستمر"
    WorldEventStatus.COMPLETED -> "مكتمل"
    WorldEventStatus.FAILED -> "فشل"
    WorldEventStatus.EXPIRED -> "منتهي"
    WorldEventStatus.CANCELLED -> "ملغى"
}