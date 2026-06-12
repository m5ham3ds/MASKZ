package com.erygra.maskoflight.core

import com.erygra.maskoflight.core.GameConfig

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  GameConfig.kt — ثوابت وإعدادات اللعبة المركزية
 *  Erygra Universe 2.0 | Mask of Light
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  مركز التحكم في جميع القيم الرقمية للعبة.
 *  أي تعديل في التوازن (Balancing) يتم هنا فقط.
 *
 *  التنظيم:
 *  - GameConfig.PhysicsConfig      — الجاذبية والحركة
 *  - GameConfig.PlayerConfig       — صحة اللاعب وطاقته
 *  - GameConfig.MemoryConfig       — نظام الذاكرة (MF/FM/XP)
 *  - GameConfig.CombatConfig       — الضرر والقتال
 *  - GameConfig.WorldConfig        — المناطق والعالم
 *  - GameConfig.UIConfig           — الواجهة والتحكم
 *  - GameConfig.AudioConfig        — الصوت والموسيقى
 *  - SaveConfig         — الحفظ والمزامنة
 *  - PerformanceConfig  — الأداء والتحسين
 * ══════════════════════════════════════════════════════════════════════════
 */

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات الفيزياء
// ─────────────────────────────────────────────────────────────────────────────
object GameConfig {
object PhysicsConfig {

    // الجاذبية
    const val GRAVITY: Float = 980f           // px/s² (تقريب واقعي)
    const val MAX_FALL_SPEED: Float = 1200f   // الحد الأقصى لسرعة السقوط
    const val TERMINAL_VELOCITY: Float = 1500f

    // حركة اللاعب الأفقية
    const val MOVE_SPEED: Float = 220f        // px/s أثناء المشي
    const val RUN_SPEED: Float = 380f         // px/s أثناء الجري
    const val CRAWL_SPEED: Float = 100f       // px/s أثناء الزحف

    // القفز
    const val JUMP_FORCE: Float = -650f       // سالب = للأعلى
    const val DOUBLE_JUMP_FORCE: Float = -550f
    const val WALL_JUMP_HORIZONTAL: Float = 280f
    const val WALL_JUMP_VERTICAL: Float = -580f
    const val JUMP_BUFFER_FRAMES: Int = 6    // frames للـ coyote time
    const val COYOTE_FRAMES: Int = 8

    // الشرائح والتسارع
    const val ACCELERATION: Float = 15f       // كيفية وصول سرعة اللاعب لـ MAX
    const val DECELERATION: Float = 10f       // كيفية التباطؤ
    const val AIR_CONTROL: Float = 0.7f       // نسبة التحكم في الهواء

    // Dash
    const val DASH_SPEED: Float = 700f
    const val DASH_DURATION_FRAMES: Int = 12  // مدة الـ Dash
    const val DASH_COOLDOWN_FRAMES: Int = 30  // cooldown بعد الـ Dash
    const val DASH_INVINCIBILITY_FRAMES: Int = 8  // i-frames أثناء Dash

    // Dodge Roll
    const val DODGE_SPEED: Float = 500f
    const val DODGE_DURATION_FRAMES: Int = 18
    const val DODGE_COOLDOWN_FRAMES: Int = 45
    const val DODGE_INVINCIBILITY_FRAMES: Int = 14

    // التسلق
    const val CLIMB_SPEED: Float = 150f
    const val WALL_SLIDE_SPEED: Float = 60f   // سرعة الانزلاق على الجدار

    // حجم اللاعب (Hitbox)
    const val PLAYER_WIDTH: Float = 28f       // px
    const val PLAYER_HEIGHT: Float = 48f      // px

    // إعدادات المنصة
    const val PLATFORM_THICKNESS: Float = 12f
    const val ONE_WAY_PLATFORM_TOLERANCE: Float = 4f
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات اللاعب
// ─────────────────────────────────────────────────────────────────────────────
object PlayerConfig {

    // الصحة
    const val BASE_HP: Int = 100
    const val MAX_HP: Int = 250              // بعد كل الترقيات
    const val HP_PER_LEVEL: Int = 5
    const val INITIAL_HP: Int = BASE_HP
    const val INITIAL_MAX_HP: Int = BASE_HP

    // الطاقة
    const val BASE_ENERGY: Int = 100
    const val MAX_ENERGY: Int = 200
    const val ENERGY_REGEN_PER_SECOND: Float = 8f
    const val ENERGY_REGEN_DELAY_AFTER_USE: Float = 2f  // ثوانٍ
    const val INITIAL_ENERGY: Int = BASE_ENERGY
    const val INITIAL_MAX_ENERGY: Int = BASE_ENERGY
    const val ENERGY_PER_LEVEL: Int = 5
    
    // الأخرى
    const val BASE_XP_TO_LEVEL: Int = 100
    const val MAX_ACCESSORY_SLOTS: Int = 3
    const val GROUND_SLAM_ENERGY_COST: Int = 30

    // الضرر
    const val BASE_LIGHT_ATTACK_DAMAGE: Int = 12
    const val BASE_HEAVY_ATTACK_DAMAGE: Int = 28
    const val BASE_GUN_DAMAGE: Int = 15
    const val BASE_SATCHEL_DAMAGE: Int = 35

    // الـ Parry
    const val PARRY_WINDOW_FRAMES: Int = 4   // نافذة الـ Parry الناجح
    const val PARRY_COOLDOWN_FRAMES: Int = 20
    const val PARRY_SUCCESS_DAMAGE_MULTIPLIER: Float = 2.5f
    const val PARRY_SUCCESS_STUN_FRAMES: Int = 60

    // مستويات التطور
    const val XP_PER_LEVEL_BASE: Int = 100
    const val XP_PER_LEVEL_MULTIPLIER: Float = 1.5f
    const val MAX_LEVEL: Int = 30

    // الحصانة بعد الإصابة
    const val INVINCIBILITY_AFTER_HIT_FRAMES: Int = 60  // 1 ثانية عند 60fps

    // الاستشفاء
    const val HEAL_ITEM_BASE_AMOUNT: Int = 30
    const val SANCTUARY_FULL_HEAL: Boolean = true
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات نظام الذاكرة
// ─────────────────────────────────────────────────────────────────────────────
object MemoryConfig {

    // حدود MF
    const val MAX_MEMORY_FRAGMENTS: Int = 99
    const val STARTING_MEMORY_FRAGMENTS: Int = 0

    // حدود FM
    const val MAX_FORGETFULNESS: Int = 30    // فوق 20 = كارثي
    const val STARTING_FORGETFULNESS: Int = 0

    // تكاليف القدرات (MF + FM)
    const val MEMORY_PULSE_SMALL_MF_COST: Int = 1
    const val MEMORY_PULSE_SMALL_FM_GAIN: Int = 1

    const val ECHO_RECALL_MF_COST: Int = 2
    const val ECHO_RECALL_FM_GAIN: Int = 2

    const val MASK_SHARD_BLAST_MF_COST: Int = 3
    const val MASK_SHARD_BLAST_FM_GAIN: Int = 4

    const val BORROWED_NAMES_MF_COST: Int = 2
    const val BORROWED_NAMES_FM_GAIN: Int = 1

    const val MEMORY_RESTORATION_MF_COST: Int = 5
    const val MEMORY_RESTORATION_FM_GAIN: Int = 5

    // تأثيرات FM (عتبات)
    val FM_THRESHOLD_LIGHT: Int = 4          // بداية التأثير الخفيف
    val FM_THRESHOLD_MEDIUM: Int = 8
    val FM_THRESHOLD_HEAVY: Int = 13
    val FM_THRESHOLD_CRITICAL: Int = 20      // كارثي

    // تقليل FM
    const val SANCTUARY_RITUAL_FM_REDUCTION_MIN: Int = 5
    const val SANCTUARY_RITUAL_FM_REDUCTION_MAX: Int = 15
    const val FM_REDUCER_POTION_MIN: Int = 2
    const val FM_REDUCER_POTION_MAX: Int = 5
    
    // Additional FM properties
    const val FM_PRICE_MULTIPLIER_MILD = 1.2f
    const val FM_PRICE_MULTIPLIER_MODERATE = 1.5f
    const val FM_PRICE_MULTIPLIER_CRITICAL = 2.0f
    const val FM_PRICE_MULTIPLIER_CATASTROPHIC = 3.0f

    const val FM_XP_MULTIPLIER_MILD = 0.9f
    const val FM_XP_MULTIPLIER_MODERATE = 0.7f
    const val FM_XP_MULTIPLIER_CRITICAL = 0.5f
    const val FM_XP_MULTIPLIER_CATASTROPHIC = 0.2f

    const val FM_MERCHANT_FORGET_THRESHOLD = 8
    const val FM_NPC_FORGET_THRESHOLD = 13
    const val FM_QUEST_LOCK_THRESHOLD = 20
    
    const val MAX_FM = 30

    // MF drops من الأعداء
    const val MF_DROP_COMMON_ENEMY: Float = 0.05f   // 5% احتمال
    const val MF_DROP_ELITE_ENEMY: Float = 0.20f
    const val MF_DROP_MINIBOSS: Float = 1.0f        // مضمون
    const val MF_DROP_BOSS: Float = 3.0f            // 3 MF مضمونة
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات القتال
// ─────────────────────────────────────────────────────────────────────────────
object CombatConfig {

    // كشف التصادم
    const val MELEE_ATTACK_RANGE: Float = 65f        // px
    const val MELEE_ATTACK_HEIGHT: Float = 40f

    // الضرر
    const val DAMAGE_REDUCTION_PER_ARMOR: Float = 0.05f  // 5% لكل نقطة درع
    const val MAX_DAMAGE_REDUCTION: Float = 0.70f        // لا يتجاوز 70%
    const val CRITICAL_HIT_MULTIPLIER: Float = 1.8f
    const val CRITICAL_HIT_CHANCE_BASE: Float = 0.08f   // 8%

    // Combo
    const val COMBO_WINDOW_FRAMES: Int = 45          // نافذة الـ Combo
    const val MAX_COMBO_COUNT: Int = 5
    const val COMBO_DAMAGE_MULTIPLIER_PER_HIT: Float = 0.1f  // +10% لكل ضربة

    // Knockback
    const val KNOCKBACK_BASE: Float = 180f
    const val KNOCKBACK_HEAVY: Float = 320f
    const val KNOCKBACK_AIR_MULTIPLIER: Float = 1.3f

    // الأضرار البيئية
    const val LAVA_DAMAGE_PER_SECOND: Int = 20
    const val SPIKE_DAMAGE: Int = 35
    const val FALL_DAMAGE_MIN_HEIGHT: Float = 400f   // px قبل حدوث ضرر السقوط
    const val FALL_DAMAGE_PER_100PX: Int = 5
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات الأعداء
// ─────────────────────────────────────────────────────────────────────────────
object EnemyConfig {

    // رؤية الأعداء
    const val DETECTION_RANGE_BASE: Float = 300f     // px
    const val DETECTION_RANGE_ALERT: Float = 500f    // عند الإنذار
    const val DETECTION_ANGLE: Float = 120f          // درجة (FOV)

    // السلوك
    const val PATROL_WAIT_FRAMES: Int = 120          // 2 ثانية
    const val CHASE_TIMEOUT_FRAMES: Int = 300        // 5 ثوانٍ
    const val ATTACK_COOLDOWN_FRAMES: Int = 60       // ثانية بين الهجمات

    // الـ Scaling
    const val HEALTH_SCALE_PER_PLAYER_LEVEL: Float = 0.08f  // +8% لكل level
    const val DAMAGE_SCALE_PER_PLAYER_LEVEL: Float = 0.05f

    // الـ Loot
    const val BASE_XP_COMMON: Int = 15
    const val BASE_XP_ELITE: Int = 50
    const val BASE_XP_MINIBOSS: Int = 150
    const val BASE_XP_BOSS: Int = 500

    val BASE_COINS_COMMON: IntRange = 1..8
    val BASE_COINS_ELITE: IntRange = 10..25
    val BASE_COINS_MINIBOSS: IntRange = 30..60
    val BASE_COINS_BOSS: IntRange = 80..150

    // Spawning
    const val MAX_ENEMIES_ON_SCREEN: Int = 12
    const val SPAWN_COOLDOWN_FRAMES: Int = 180       // 3 ثوانٍ
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات العالم
// ─────────────────────────────────────────────────────────────────────────────
object WorldConfig {

    // حجم الـ Tiles
    const val TILE_SIZE: Int = 32                    // px
    const val CHUNK_SIZE: Int = 16                   // tiles per chunk
    const val CHUNK_LOAD_RADIUS: Int = 3             // chunks حول اللاعب

    // إجمالي المناطق
    const val TOTAL_REGIONS: Int = 7

    // أسماء المناطق
    val REGION_IDS = listOf(
        "ashen_sprawl",
        "veiled_archives",
        "hollowed_archipelago",
        "glassfjord_cliffs",
        "sunken_clockworks",
        "blackroot_moorlands",
        "luminous_chasm"
    )

    // نظام Fog of War
    const val FOG_REVEAL_RADIUS_TILES: Int = 7
    const val BIOLUME_TALISMAN_REVEAL_RADIUS: Int = 15
    const val MAP_SHARD_COST_COINS: Int = 100
    const val FULL_MAP_COST_COINS: Int = 1500
    const val MAP_SHARDS_FOR_FULL_REVEAL: Int = 5

    // Fast Travel
    const val FAST_TRAVEL_LOCAL_COST: Int = 5
    const val FAST_TRAVEL_REGIONAL_COST: Int = 20
    const val FAST_TRAVEL_INTERCITY_COST: Int = 50

    // الأحداث العالمية
    const val MEMORY_STORM_CHANCE_PER_HOUR: Float = 0.02f
    const val MEMORY_STORM_DURATION_MINUTES: Int = 30
    const val MEMORY_STORM_MF_MULTIPLIER: Float = 3f
    const val MEMORY_STORM_FM_MULTIPLIER: Float = 2f

    const val CARAVAN_SPAWN_CHANCE_PER_HOUR: Float = 0.05f
    const val CARAVAN_DURATION_MINUTES: Int = 20
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات الواجهة
// ─────────────────────────────────────────────────────────────────────────────
object UIConfig {

    // أبعاد أزرار التحكم
    val DPAD_SIZE = 100.dp
    val BUTTON_SIZE_SMALL = 52.dp
    val BUTTON_SIZE_MEDIUM = 64.dp
    val BUTTON_SIZE_LARGE = 76.dp

    // opacity
    const val CONTROLS_OPACITY_MIN = 0.3f
    const val CONTROLS_OPACITY_MAX = 0.8f
    const val CONTROLS_OPACITY_DEFAULT = 0.55f

    // نسب الـ HUD
    const val HP_BAR_WIDTH_FRACTION = 0.28f    // نسبة من عرض الشاشة
    const val HP_BAR_HEIGHT_DP = 12
    const val ENERGY_BAR_HEIGHT_DP = 8
    const val FM_BAR_HEIGHT_DP = 6

    // المخطط المصغر (Minimap)
    val MINIMAP_SIZE = 80.dp
    const val MINIMAP_ALPHA = 0.75f

    // Notifications
    const val NOTIFICATION_DURATION_MS = 3000L
    const val ACHIEVEMENT_DISPLAY_MS = 5000L

    // Typography
    val HUD_TEXT_SIZE = 11.sp
    val DIALOGUE_TEXT_SIZE = 14.sp
    val MENU_TITLE_SIZE = 28.sp
    val MENU_ITEM_SIZE = 16.sp

    // Animation
    const val SCREEN_TRANSITION_MS = 350L
    const val BUTTON_PRESS_ANIMATION_MS = 100L
    const val HUD_UPDATE_ANIMATION_MS = 200L
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات الصوت
// ─────────────────────────────────────────────────────────────────────────────
object AudioConfig {

    // مستويات الصوت الافتراضية (0f–1f)
    const val MASTER_VOLUME_DEFAULT = 0.85f
    const val MUSIC_VOLUME_DEFAULT = 0.65f
    const val SFX_VOLUME_DEFAULT = 0.80f
    const val AMBIENT_VOLUME_DEFAULT = 0.50f

    // Crossfade عند تغيير المنطقة
    const val REGION_MUSIC_CROSSFADE_MS = 2000L

    // Ducking (تقليل الموسيقى عند الكلام)
    const val DIALOGUE_MUSIC_DUCK_FACTOR = 0.4f
    const val BOSS_MUSIC_DUCK_FACTOR = 0.2f   // تقريباً كامل إيقاف الـ ambient

    // Combat tension
    const val COMBAT_MUSIC_LAYER_FADE_IN_MS = 800L
    const val COMBAT_MUSIC_LAYER_FADE_OUT_MS = 3000L

    // SFX Pooling
    const val MAX_SIMULTANEOUS_SFX = 16
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات الحفظ
// ─────────────────────────────────────────────────────────────────────────────
object SaveConfig {

    const val MAX_SAVE_SLOTS = 10
    const val AUTO_SAVE_INTERVAL_MINUTES = 5
    const val SAVE_VERSION = 1              // لتحديد توافق الملفات

    // أسماء الملفات
    const val LOCAL_SAVE_DIRECTORY = "save_data"
    const val SETTINGS_FILE_NAME = "game_settings.json"

    // Cloud Sync
    const val CLOUD_SYNC_INTERVAL_MINUTES = 5
    const val MAX_CLOUD_SAVE_SLOTS = 10
    const val SYNC_CONFLICT_TIMEOUT_MS = 10_000L
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات الأداء
// ─────────────────────────────────────────────────────────────────────────────
object PerformanceConfig {

    // FPS المستهدف
    const val TARGET_FPS_HIGH = 60
    const val TARGET_FPS_MEDIUM = 45
    const val TARGET_FPS_LOW = 30

    // حدود الـ Particles
    const val MAX_PARTICLES_HIGH = 500
    const val MAX_PARTICLES_MEDIUM = 200
    const val MAX_PARTICLES_LOW = 80

    // Object Pooling
    const val ENEMY_POOL_SIZE = 20
    const val PARTICLE_POOL_SIZE = 200
    const val PROJECTILE_POOL_SIZE = 50

    // Battery
    const val BATTERY_SAVE_THRESHOLD = 0.20f    // 20% — تقليل FPS
    const val BATTERY_SAVE_FPS = 30

    // Memory
    const val TEXTURE_CACHE_MB = 64
    const val AUDIO_CACHE_MB = 32

    // LOD
    const val LOD_DISTANCE_HIGH = 300f           // px من اللاعب
    const val LOD_DISTANCE_MEDIUM = 600f
    // أبعد من 600 = LOD منخفض
}

// ─────────────────────────────────────────────────────────────────────────────
// ثوابت API
// ─────────────────────────────────────────────────────────────────────────────
object ApiConfig {
    const val GEMINI_MODEL = "gemini-2.0-flash"
    const val GEMINI_API_TIMEOUT_SECONDS = 30L
    const val GEMINI_MAX_TOKENS = 1024
    const val BACKEND_TIMEOUT_SECONDS = 15L
    const val BACKEND_RETRY_COUNT = 3
    const val BACKEND_RETRY_DELAY_MS = 1000L
}

object ParticleConfig {
    const val MAX_PARTICLES = 1000
}

// ─────────────────────────────────────────────────────────────────────────────
}