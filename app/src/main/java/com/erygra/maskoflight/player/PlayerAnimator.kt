package com.erygra.maskoflight.player

import androidx.compose.ui.graphics.ImageBitmap
import com.erygra.maskoflight.core.GameConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sin

/**
 * player/PlayerAnimator.kt
 * ═══════════════════════════════════════════════════════════════════════════════
 * نظام أنيميشن اللاعب — لعبة "قِنَاعُ النُّور" (Mask of Light)
 * Erygra Universe 2.0
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * الوظائف الرئيسية:
 * - إدارة Sprite Sheets للشخصية
 * - State Machine للأنيميشن (Idle, Walk, Jump, Attack, etc.)
 * - Transitions سلسة بين الحالات
 * - Frame-by-frame animation مع FPS قابل للتخصيص
 * - Secondary animations (Cloak physics, Satchel swing, Mask glow)
 * - Blend animations (مزج بين حركتين)
 * - Mirror/Flip sprites حسب الاتجاه
 * - Layer-based rendering (Body → Cloak → Satchels → Hat → Mask → Weapon)
 *
 * البنية:
 * - AnimationState: حالات الأنيميشن
 * - AnimationClip: كليب أنيميشن واحد
 * - SpriteFrame: إطار واحد من الأنيميشن
 * - LayerAnimation: أنيميشن طبقة واحدة
 * - PlayerAnimator: المدير الرئيسي
 *
 * @author Erygra Team
 * @version 2.0.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Animation State
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * حالات الأنيميشن الممكنة
 */
enum class AnimationState {
    // حركة أساسية
    IDLE,                   // وقوف (2-4 frames, looping)
    IDLE_BLINK,             // وقوف مع رمش (تشغيل عشوائي)
    WALK,                   // مشي (6-8 frames, looping)
    RUN,                    // جري (8-10 frames, looping)
    TURN_AROUND,            // استدارة (3-4 frames, one-shot)
    
    // قفز وسقوط
    JUMP_START,             // بداية القفز (2-3 frames, one-shot)
    JUMP_RISE,              // صعود (2-3 frames, looping)
    JUMP_PEAK,              // قمة القفز (1-2 frames, hold)
    JUMP_FALL,              // سقوط (2-3 frames, looping)
    LAND_LIGHT,             // هبوط خفيف (2-3 frames, one-shot)
    LAND_HEAVY,             // هبوط ثقيل (3-4 frames, one-shot)
    
    // حركة متقدمة
    DASH,                   // اندفاع (4-6 frames, one-shot)
    DODGE_ROLL,             // لفة تفادي (6-8 frames, one-shot)
    WALL_SLIDE,             // انزلاق على جدار (2-3 frames, looping)
    WALL_JUMP,              // قفز جداري (3-4 frames, one-shot)
    LEDGE_GRAB,             // تعلق بحافة (1-2 frames, hold)
    LEDGE_CLIMB,            // تسلق حافة (4-6 frames, one-shot)
    CLIMB,                  // تسلق (4-6 frames, looping)
    SWING,                  // تأرجح (6-8 frames, looping)
    
    // قتال - هجوم
    ATTACK_LIGHT_1,         // هجوم خفيف 1 (4-5 frames, one-shot)
    ATTACK_LIGHT_2,         // هجوم خفيف 2 (4-5 frames, one-shot)
    ATTACK_LIGHT_3,         // هجوم خفيف 3 (Combo finisher, 5-6 frames)
    ATTACK_HEAVY,           // هجوم ثقيل (6-8 frames, one-shot)
    ATTACK_HEAVY_CHARGE,    // شحن هجوم ثقيل (2-3 frames, looping)
    ATTACK_AIR,             // هجوم جوي (5-6 frames, one-shot)
    GROUND_SLAM,            // ضربة أرضية (6-8 frames, one-shot)
    
    // قتال - دفاع
    PARRY_READY,            // استعداد للصد (1-2 frames, hold)
    PARRY_SUCCESS,          // صد ناجح (3-4 frames, one-shot)
    BLOCK,                  // حجب (1-2 frames, hold)
    HURT_LIGHT,             // إصابة خفيفة (2-3 frames, one-shot)
    HURT_HEAVY,             // إصابة ثقيلة (3-4 frames, one-shot)
    KNOCKBACK,              // ارتداد (4-5 frames, one-shot)
    
    // قدرات
    ABILITY_MEMORY_PULSE,   // نبضة ذاكرة (5-6 frames, one-shot)
    ABILITY_ECHO_RECALL,    // استدعاء صدى (6-8 frames, one-shot)
    ABILITY_MASK_SHARD,     // شظية القناع (6-8 frames, one-shot)
    ABILITY_CHANNEL,        // تحضير قدرة (2-3 frames, looping)
    
    // تفاعل
    INTERACT,               // تفاعل (4-5 frames, one-shot)
    PICKUP,                 // التقاط (3-4 frames, one-shot)
    TALK,                   // حديث (2-4 frames, looping)
    READ,                   // قراءة (2-3 frames, looping)
    
    // حالات خاصة
    DEATH,                  // موت (8-12 frames, one-shot → hold last)
    RESPAWN,                // إحياء (6-8 frames, one-shot)
    LEVEL_UP,               // رفع مستوى (8-10 frames, one-shot)
    SANCTUARY_REST,         // راحة في الملجأ (3-4 frames, looping)
    
    // حالات عاطفية (للحوار)
    EMOTION_THINK,          // تفكير (3-4 frames, looping)
    EMOTION_SURPRISE,       // تفاجؤ (3-4 frames, one-shot)
    EMOTION_SAD,            // حزن (2-3 frames, hold)
    EMOTION_DETERMINED      // تصميم (2-3 frames, hold)
}

/**
 * نوع الأنيميشن
 */
enum class AnimationType {
    LOOPING,        // يتكرر باستمرار
    ONE_SHOT,       // يشتغل مرة واحدة ثم يتوقف
    HOLD_LAST       // يشتغل مرة ويبقى على آخر إطار
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Sprite Frame
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * إطار واحد من الأنيميشن
 *
 * @property sprite الصورة (ImageBitmap أو مرجع)
 * @property duration مدة الإطار (ثواني)
 * @property offsetX إزاحة X للرسم
 * @property offsetY إزاحة Y للرسم
 * @property hitboxes صناديق الضربات لهذا الإطار (للهجوم)
 * @property hurtboxes صناديق الإصابة لهذا الإطار (للدفاع)
 */
data class SpriteFrame(
    val sprite: ImageBitmap? = null,
    val spriteIndex: Int = 0, // مؤشر في sprite sheet
    val duration: Float = 0.083f, // ~12 FPS default
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val hitboxes: List<AnimationHitbox> = emptyList(),
    val hurtboxes: List<AnimationHitbox> = emptyList()
)

/**
 * صندوق ضربة/إصابة للأنيميشن
 *
 * @property x الموقع النسبي X
 * @property y الموقع النسبي Y
 * @property width العرض
 * @property height الارتفاع
 * @property damage الضرر (للهجوم فقط)
 * @property type نوع الضربة
 */
data class AnimationHitbox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val damage: Int = 0,
    val type: String = "normal" // normal, heavy, projectile
)

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Animation Clip
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * كليب أنيميشن كامل
 *
 * @property state الحالة المرتبطة
 * @property frames قائمة الإطارات
 * @property type نوع الأنيميشن
 * @property fps سرعة التشغيل (إطار/ثانية)
 * @property canBeInterrupted هل يمكن قطعه؟
 * @property priority الأولوية (أعلى = أولوية أعلى)
 * @property nextState الحالة التالية بعد الانتهاء (null = يبقى)
 */
data class AnimationClip(
    val state: AnimationState,
    val frames: List<SpriteFrame>,
    val type: AnimationType = AnimationType.LOOPING,
    val fps: Float = 12f,
    val canBeInterrupted: Boolean = true,
    val priority: Int = 0,
    val nextState: AnimationState? = null
) {
    /**
     * المدة الكلية للأنيميشن (ثواني)
     */
    val totalDuration: Float
        get() = frames.sumOf { it.duration.toDouble() }.toFloat()

    /**
     * عدد الإطارات
     */
    val frameCount: Int
        get() = frames.size
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Animation Layers
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * طبقات الرسم
 */
enum class AnimationLayer {
    BODY,           // الجسم الرئيسي
    CLOAK,          // العباءة (secondary physics)
    SATCHEL_LEFT,   // الحقيبة اليسرى
    SATCHEL_RIGHT,  // الحقيبة اليمنى
    HAT,            // القبعة
    MASK_GLOW,      // توهج القناع
    WEAPON,         // السلاح
    VFX_FRONT,      // تأثيرات أمامية
    VFX_BACK        // تأثيرات خلفية
}

/**
 * أنيميشن طبقة واحدة
 *
 * @property layer الطبقة
 * @property currentFrame الإطار الحالي
 * @property enabled هل الطبقة مفعلة؟
 * @property alpha الشفافية
 * @property tint اللون المضاف
 */
data class LayerAnimation(
    val layer: AnimationLayer,
    val currentFrame: SpriteFrame? = null,
    val enabled: Boolean = true,
    val alpha: Float = 1f,
    val tint: Long = 0xFFFFFFFF, // ARGB
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Animation Transition
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * انتقال بين حالتين
 *
 * @property from الحالة الأصلية
 * @property to الحالة المستهدفة
 * @property duration مدة الانتقال (ثواني)
 * @property blendType نوع المزج
 */
data class AnimationTransition(
    val from: AnimationState,
    val to: AnimationState,
    val duration: Float = 0.1f,
    val blendType: BlendType = BlendType.LINEAR
)

/**
 * نوع المزج
 */
enum class BlendType {
    LINEAR,         // مزج خطي
    EASE_IN,        // تسارع تدريجي
    EASE_OUT,       // تباطؤ تدريجي
    EASE_IN_OUT,    // تسارع ثم تباطؤ
    SNAP            // قطع فوري
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Secondary Animation (Physics-based)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * أنيميشن ثانوي (فيزياء بسيطة)
 * للعباءة، الحقائب، إلخ
 *
 * @property amplitude السعة
 * @property frequency التردد
 * @property damping التخميد
 * @property phase الطور الحالي
 */
data class SecondaryAnimation(
    val amplitude: Float = 1f,
    val frequency: Float = 1f,
    val damping: Float = 0.95f,
    var phase: Float = 0f,
    var velocity: Float = 0f
) {
    /**
     * تحديث الطور
     */
    fun update(deltaTime: Float, input: Float = 0f): Float {
        velocity += input
        velocity *= damping
        phase += velocity * deltaTime * frequency
        return sin(phase) * amplitude
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Player Animator
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مدير أنيميشن اللاعب
 */
class PlayerAnimator {

    // ═══════════════════════════════════════════════════════════════════════════
    // Properties
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * الحالة الحالية للأنيميشن
     */
    private val _currentState = MutableStateFlow(AnimationState.IDLE)
    val currentState: StateFlow<AnimationState> = _currentState.asStateFlow()

    /**
     * الحالة السابقة
     */
    private var previousState: AnimationState = AnimationState.IDLE

    /**
     * الكليب الحالي
     */
    private var currentClip: AnimationClip? = null

    /**
     * مكتبة كليبات الأنيميشن
     */
    private val animationClips = mutableMapOf<AnimationState, AnimationClip>()

    /**
     * قائمة الانتقالات
     */
    private val transitions = mutableListOf<AnimationTransition>()

    /**
     * الإطار الحالي
     */
    private var currentFrameIndex: Int = 0

    /**
     * الوقت المنقضي في الإطار الحالي
     */
    private var frameTime: Float = 0f

    /**
     * هل الأنيميشن منتهي؟ (للـ ONE_SHOT)
     */
    private var isAnimationFinished: Boolean = false

    /**
     * هل نحن في انتقال؟
     */
    private var isTransitioning: Boolean = false

    /**
     * الانتقال الحالي
     */
    private var currentTransition: AnimationTransition? = null

    /**
     * وقت الانتقال المنقضي
     */
    private var transitionTime: Float = 0f

    /**
     * الطبقات الحالية
     */
    private val _layers = MutableStateFlow(
        AnimationLayer.values().associateWith { layer ->
            LayerAnimation(layer = layer)
        }
    )
    val layers: StateFlow<Map<AnimationLayer, LayerAnimation>> = _layers.asStateFlow()

    /**
     * الأنيميشن الثانوي للعباءة
     */
    private val cloakAnimation = SecondaryAnimation(
        amplitude = 5f,
        frequency = 2f,
        damping = 0.92f
    )

    /**
     * الأنيميشن الثانوي للحقيبة اليسرى
     */
    private val satchelLeftAnimation = SecondaryAnimation(
        amplitude = 3f,
        frequency = 1.5f,
        damping = 0.9f
    )

    /**
     * الأنيميشن الثانوي للحقيبة اليمنى
     */
    private val satchelRightAnimation = SecondaryAnimation(
        amplitude = 3f,
        frequency = 1.5f,
        damping = 0.9f,
        phase = 3.14159f // طور معاكس
    )

    /**
     * توهج القناع (نبضي)
     */
    private var maskGlowPhase: Float = 0f
    private val maskGlowFrequency: Float = 0.5f

    /**
     * هل الشخصية متجهة لليمين؟
     */
    private var facingRight: Boolean = true

    /**
     * السرعة الحالية (لحساب الأنيميشن الثانوي)
     */
    private var currentVelocityX: Float = 0f
    private var currentVelocityY: Float = 0f

    /**
     * إحصائيات الأنيميشن (للتصحيح)
     */
    private var totalFramesPlayed: Int = 0
    private var totalTransitions: Int = 0

    // ═══════════════════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════════════════

    init {
        // تحميل الأنيميشن الافتراضي (سيتم استبداله بـ sprites حقيقية)
        loadDefaultAnimations()
        
        // تعيين الأنيميشن الافتراضي
        currentClip = animationClips[AnimationState.IDLE]
    }

    /**
     * تحميل الأنيميشن الافتراضي (Placeholder)
     */
    private fun loadDefaultAnimations() {
        // IDLE
        animationClips[AnimationState.IDLE] = AnimationClip(
            state = AnimationState.IDLE,
            frames = List(4) { SpriteFrame(spriteIndex = it, duration = 0.15f) },
            type = AnimationType.LOOPING,
            fps = 8f,
            canBeInterrupted = true,
            priority = 0
        )

        // WALK
        animationClips[AnimationState.WALK] = AnimationClip(
            state = AnimationState.WALK,
            frames = List(8) { SpriteFrame(spriteIndex = it, duration = 0.1f) },
            type = AnimationType.LOOPING,
            fps = 12f,
            canBeInterrupted = true,
            priority = 1
        )

        // RUN
        animationClips[AnimationState.RUN] = AnimationClip(
            state = AnimationState.RUN,
            frames = List(10) { SpriteFrame(spriteIndex = it, duration = 0.08f) },
            type = AnimationType.LOOPING,
            fps = 15f,
            canBeInterrupted = true,
            priority = 1
        )

        // JUMP_START
        animationClips[AnimationState.JUMP_START] = AnimationClip(
            state = AnimationState.JUMP_START,
            frames = List(3) { SpriteFrame(spriteIndex = it, duration = 0.05f) },
            type = AnimationType.ONE_SHOT,
            fps = 20f,
            canBeInterrupted = false,
            priority = 5,
            nextState = AnimationState.JUMP_RISE
        )

        // JUMP_RISE
        animationClips[AnimationState.JUMP_RISE] = AnimationClip(
            state = AnimationState.JUMP_RISE,
            frames = List(2) { SpriteFrame(spriteIndex = it, duration = 0.1f) },
            type = AnimationType.LOOPING,
            fps = 10f,
            canBeInterrupted = true,
            priority = 4
        )

        // JUMP_FALL
        animationClips[AnimationState.JUMP_FALL] = AnimationClip(
            state = AnimationState.JUMP_FALL,
            frames = List(3) { SpriteFrame(spriteIndex = it, duration = 0.1f) },
            type = AnimationType.LOOPING,
            fps = 10f,
            canBeInterrupted = true,
            priority = 4
        )

        // LAND_LIGHT
        animationClips[AnimationState.LAND_LIGHT] = AnimationClip(
            state = AnimationState.LAND_LIGHT,
            frames = List(3) { SpriteFrame(spriteIndex = it, duration = 0.05f) },
            type = AnimationType.ONE_SHOT,
            fps = 20f,
            canBeInterrupted = true,
            priority = 3,
            nextState = AnimationState.IDLE
        )

        // DASH
        animationClips[AnimationState.DASH] = AnimationClip(
            state = AnimationState.DASH,
            frames = List(5) { SpriteFrame(spriteIndex = it, duration = 0.04f) },
            type = AnimationType.ONE_SHOT,
            fps = 25f,
            canBeInterrupted = false,
            priority = 10,
            nextState = AnimationState.IDLE
        )

        // DODGE_ROLL
        animationClips[AnimationState.DODGE_ROLL] = AnimationClip(
            state = AnimationState.DODGE_ROLL,
            frames = List(8) { SpriteFrame(spriteIndex = it, duration = 0.05f) },
            type = AnimationType.ONE_SHOT,
            fps = 20f,
            canBeInterrupted = false,
            priority = 10,
            nextState = AnimationState.IDLE
        )

        // ATTACK_LIGHT_1
        animationClips[AnimationState.ATTACK_LIGHT_1] = AnimationClip(
            state = AnimationState.ATTACK_LIGHT_1,
            frames = listOf(
                SpriteFrame(spriteIndex = 0, duration = 0.05f), // windup
                SpriteFrame(spriteIndex = 1, duration = 0.05f), // strike
                SpriteFrame(
                    spriteIndex = 2,
                    duration = 0.1f,
                    hitboxes = listOf(
                        AnimationHitbox(x = 20f, y = 0f, width = 30f, height = 20f, damage = 10)
                    )
                ), // active
                SpriteFrame(spriteIndex = 3, duration = 0.1f)  // recovery
            ),
            type = AnimationType.ONE_SHOT,
            fps = 20f,
            canBeInterrupted = false,
            priority = 8,
            nextState = AnimationState.IDLE
        )

        // ATTACK_HEAVY
        animationClips[AnimationState.ATTACK_HEAVY] = AnimationClip(
            state = AnimationState.ATTACK_HEAVY,
            frames = listOf(
                SpriteFrame(spriteIndex = 0, duration = 0.1f),  // windup
                SpriteFrame(spriteIndex = 1, duration = 0.1f),  // charge
                SpriteFrame(
                    spriteIndex = 2,
                    duration = 0.15f,
                    hitboxes = listOf(
                        AnimationHitbox(x = 25f, y = -10f, width = 40f, height = 30f, damage = 25, type = "heavy")
                    )
                ), // strike
                SpriteFrame(spriteIndex = 3, duration = 0.15f)  // recovery
            ),
            type = AnimationType.ONE_SHOT,
            fps = 15f,
            canBeInterrupted = false,
            priority = 9,
            nextState = AnimationState.IDLE
        )

        // PARRY_READY
        animationClips[AnimationState.PARRY_READY] = AnimationClip(
            state = AnimationState.PARRY_READY,
            frames = List(2) { SpriteFrame(spriteIndex = it, duration = 0.1f) },
            type = AnimationType.HOLD_LAST,
            fps = 10f,
            canBeInterrupted = true,
            priority = 6
        )

        // HURT_LIGHT
        animationClips[AnimationState.HURT_LIGHT] = AnimationClip(
            state = AnimationState.HURT_LIGHT,
            frames = List(3) { SpriteFrame(spriteIndex = it, duration = 0.08f) },
            type = AnimationType.ONE_SHOT,
            fps = 12f,
            canBeInterrupted = false,
            priority = 15,
            nextState = AnimationState.IDLE
        )

        // ABILITY_MEMORY_PULSE
        animationClips[AnimationState.ABILITY_MEMORY_PULSE] = AnimationClip(
            state = AnimationState.ABILITY_MEMORY_PULSE,
            frames = List(6) { SpriteFrame(spriteIndex = it, duration = 0.08f) },
            type = AnimationType.ONE_SHOT,
            fps = 12f,
            canBeInterrupted = false,
            priority = 12,
            nextState = AnimationState.IDLE
        )

        // DEATH
        animationClips[AnimationState.DEATH] = AnimationClip(
            state = AnimationState.DEATH,
            frames = List(10) { SpriteFrame(spriteIndex = it, duration = 0.1f) },
            type = AnimationType.HOLD_LAST,
            fps = 10f,
            canBeInterrupted = false,
            priority = 100
        )

        // TODO: إضافة باقي الأنيميشن
    }

    /**
     * تحميل sprite sheet من ملف
     * (سيتم استدعاؤها من نظام الموارد)
     *
     * @param state حالة الأنيميشن
     * @param spriteSheet الصورة الكاملة
     * @param frameWidth عرض الإطار
     * @param frameHeight ارتفاع الإطار
     * @param frameCount عدد الإطارات
     */
    fun loadSpriteSheet(
        state: AnimationState,
        spriteSheet: ImageBitmap,
        frameWidth: Int,
        frameHeight: Int,
        frameCount: Int,
        fps: Float = 12f,
        type: AnimationType = AnimationType.LOOPING
    ) {
        val frames = mutableListOf<SpriteFrame>()
        
        for (i in 0 until frameCount) {
            // هنا يمكن تقطيع الـ sprite sheet
            // لكن Compose لا يدعم تقطيع ImageBitmap مباشرة
            // سنستخدم spriteIndex فقط
            frames.add(
                SpriteFrame(
                    sprite = spriteSheet,
                    spriteIndex = i,
                    duration = 1f / fps
                )
            )
        }

        animationClips[state] = AnimationClip(
            state = state,
            frames = frames,
            type = type,
            fps = fps
        )
    }

    /**
     * إضافة انتقال
     */
    fun addTransition(from: AnimationState, to: AnimationState, duration: Float = 0.1f, blend: BlendType = BlendType.LINEAR) {
        transitions.add(AnimationTransition(from, to, duration, blend))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Update Loop
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * التحديث الرئيسي
     *
     * @param deltaTime الوقت المنقضي (ثواني)
     * @param actionState حالة الإجراء من PlayerController
     * @param velocityX السرعة الأفقية
     * @param velocityY السرعة العمودية
     * @param facingRight الاتجاه
     */
    fun update(
        deltaTime: Float,
        actionState: PlayerActionState,
        velocityX: Float,
        velocityY: Float,
        facingRight: Boolean
    ) {
        // تحديث البيانات
        this.currentVelocityX = velocityX
        this.currentVelocityY = velocityY
        this.facingRight = facingRight

        // تحديد الحالة المطلوبة من actionState
        val targetState = mapActionStateToAnimationState(actionState, velocityX, velocityY)

        // التحقق من الحاجة للانتقال
        if (targetState != _currentState.value) {
            requestTransition(targetState)
        }

        // تحديث الانتقال إن وُجد
        if (isTransitioning) {
            updateTransition(deltaTime)
        } else {
            // تحديث الأنيميشن الحالي
            updateCurrentAnimation(deltaTime)
        }

        // تحديث الأنيميشن الثانوي
        updateSecondaryAnimations(deltaTime)

        // تحديث الطبقات
        updateLayers()
    }

    /**
     * تحويل PlayerActionState إلى AnimationState
     */
    private fun mapActionStateToAnimationState(
        actionState: PlayerActionState,
        velocityX: Float,
        velocityY: Float
    ): AnimationState {
        return when (actionState) {
            PlayerActionState.IDLE -> AnimationState.IDLE
            PlayerActionState.WALKING -> AnimationState.WALK
            PlayerActionState.RUNNING -> AnimationState.RUN
            PlayerActionState.JUMPING -> {
                when {
                    velocityY > 100f -> AnimationState.JUMP_RISE
                    velocityY > -50f -> AnimationState.JUMP_PEAK
                    else -> AnimationState.JUMP_FALL
                }
            }
            PlayerActionState.FALLING -> AnimationState.JUMP_FALL
            PlayerActionState.DASHING -> AnimationState.DASH
            PlayerActionState.DODGING -> AnimationState.DODGE_ROLL
            PlayerActionState.WALL_SLIDING -> AnimationState.WALL_SLIDE
            PlayerActionState.WALL_JUMPING -> AnimationState.WALL_JUMP
            PlayerActionState.CLIMBING -> AnimationState.CLIMB
            PlayerActionState.SWINGING -> AnimationState.SWING
            PlayerActionState.ATTACKING_LIGHT -> {
                // تحديد أي هجوم خفيف بناءً على combo
                AnimationState.ATTACK_LIGHT_1 // TODO: دعم combo
            }
            PlayerActionState.ATTACKING_HEAVY -> AnimationState.ATTACK_HEAVY
            PlayerActionState.PARRYING -> AnimationState.PARRY_READY
            PlayerActionState.USING_ABILITY -> AnimationState.ABILITY_MEMORY_PULSE // TODO: تحديد القدرة
            PlayerActionState.INTERACTING -> AnimationState.INTERACT
            PlayerActionState.HURT -> AnimationState.HURT_LIGHT
            PlayerActionState.DEAD -> AnimationState.DEATH
        }
    }

    /**
     * طلب انتقال لحالة جديدة
     */
    private fun requestTransition(newState: AnimationState) {
        // التحقق من إمكانية القطع
        val currentClip = currentClip
        if (currentClip != null && !currentClip.canBeInterrupted) {
            // لا يمكن قطع الأنيميشن الحالي
            return
        }

        // التحقق من الأولوية
        val newClip = animationClips[newState]
        if (newClip != null && currentClip != null) {
            if (newClip.priority < currentClip.priority) {
                // الأنيميشن الجديد أولويته أقل
                return
            }
        }

        // البحث عن انتقال مناسب
        val transition = transitions.find { it.from == _currentState.value && it.to == newState }

        if (transition != null && transition.blendType != BlendType.SNAP) {
            // بدء انتقال سلس
            startTransition(transition)
        } else {
            // انتقال فوري
            changeState(newState)
        }
    }

    /**
     * بدء انتقال
     */
    private fun startTransition(transition: AnimationTransition) {
        isTransitioning = true
        currentTransition = transition
        transitionTime = 0f
        totalTransitions++
    }

    /**
     * تحديث الانتقال
     */
    private fun updateTransition(deltaTime: Float) {
        val transition = currentTransition ?: return

        transitionTime += deltaTime

        if (transitionTime >= transition.duration) {
            // الانتقال انتهى
            isTransitioning = false
            currentTransition = null
            changeState(transition.to)
        } else {
            // لا يزال في الانتقال
            // يمكن تطبيق blend هنا (مزج بين الإطارات)
            // لكن لبساطة، سنقوم بتغيير الحالة مباشرة
        }
    }

    /**
     * تغيير الحالة مباشرة
     */
    private fun changeState(newState: AnimationState) {
        previousState = _currentState.value
        _currentState.value = newState
        currentClip = animationClips[newState]
        currentFrameIndex = 0
        frameTime = 0f
        isAnimationFinished = false
    }

    /**
     * تحديث الأنيميشن الحالي
     */
    private fun updateCurrentAnimation(deltaTime: Float) {
        val clip = currentClip ?: return

        if (isAnimationFinished && clip.type == AnimationType.HOLD_LAST) {
            // البقاء على آخر إطار
            return
        }

        frameTime += deltaTime

        val currentFrame = clip.frames.getOrNull(currentFrameIndex) ?: return

        if (frameTime >= currentFrame.duration) {
            // الانتقال للإطار التالي
            frameTime = 0f
            currentFrameIndex++
            totalFramesPlayed++

            if (currentFrameIndex >= clip.frames.size) {
                // وصلنا لنهاية الأنيميشن
                when (clip.type) {
                    AnimationType.LOOPING -> {
                        currentFrameIndex = 0 // إعادة من البداية
                    }
                    AnimationType.ONE_SHOT -> {
                        currentFrameIndex = clip.frames.size - 1 // البقاء على آخر إطار
                        isAnimationFinished = true
                        
                        // الانتقال للحالة التالية إن وُجدت
                        clip.nextState?.let { nextState ->
                            changeState(nextState)
                        }
                    }
                    AnimationType.HOLD_LAST -> {
                        currentFrameIndex = clip.frames.size - 1
                        isAnimationFinished = true
                    }
                }
            }
        }
    }

    /**
     * تحديث الأنيميشن الثانوي
     */
    private fun updateSecondaryAnimations(deltaTime: Float) {
        // العباءة تتأثر بالسرعة
        val cloakInput = abs(currentVelocityX) * 0.1f
        cloakAnimation.update(deltaTime, cloakInput)

        // الحقائب تتأرجح مع الحركة
        val satchelInput = if (abs(currentVelocityX) > 50f) 1f else 0f
        satchelLeftAnimation.update(deltaTime, satchelInput)
        satchelRightAnimation.update(deltaTime, satchelInput)

        // توهج القناع
        maskGlowPhase += deltaTime * maskGlowFrequency
        if (maskGlowPhase > 6.28318f) { // 2π
            maskGlowPhase -= 6.28318f
        }
    }

    /**
     * تحديث الطبقات
     */
    private fun updateLayers() {
        val currentFrame = currentClip?.frames?.getOrNull(currentFrameIndex)

        val updatedLayers = _layers.value.toMutableMap()

        // طبقة الجسم
        updatedLayers[AnimationLayer.BODY] = LayerAnimation(
            layer = AnimationLayer.BODY,
            currentFrame = currentFrame,
            enabled = true
        )

        // طبقة العباءة (مع إزاحة ثانوية)
        updatedLayers[AnimationLayer.CLOAK] = LayerAnimation(
            layer = AnimationLayer.CLOAK,
            currentFrame = currentFrame, // TODO: إطار منفصل
            enabled = true,
            offsetX = cloakAnimation.phase * 2f,
            rotation = cloakAnimation.phase * 5f // درجات
        )

        // طبقة الحقيبة اليسرى
        updatedLayers[AnimationLayer.SATCHEL_LEFT] = LayerAnimation(
            layer = AnimationLayer.SATCHEL_LEFT,
            currentFrame = currentFrame, // TODO: إطار منفصل
            enabled = true,
            offsetY = satchelLeftAnimation.phase * 2f,
            rotation = satchelLeftAnimation.phase * 3f
        )

        // طبقة الحقيبة اليمنى
        updatedLayers[AnimationLayer.SATCHEL_RIGHT] = LayerAnimation(
            layer = AnimationLayer.SATCHEL_RIGHT,
            currentFrame = currentFrame, // TODO: إطار منفصل
            enabled = true,
            offsetY = satchelRightAnimation.phase * 2f,
            rotation = satchelRightAnimation.phase * 3f
        )

        // طبقة توهج القناع
        val glowAlpha = (sin(maskGlowPhase) * 0.3f + 0.7f).coerceIn(0.4f, 1f)
        updatedLayers[AnimationLayer.MASK_GLOW] = LayerAnimation(
            layer = AnimationLayer.MASK_GLOW,
            currentFrame = currentFrame, // TODO: إطار توهج منفصل
            enabled = true,
            alpha = glowAlpha,
            tint = 0xFFFFD700 // ذهبي
        )

        _layers.value = updatedLayers
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * فرض حالة معينة (بدون تحقق من الشروط)
     */
    fun forceState(state: AnimationState) {
        changeState(state)
    }

    /**
     * الحصول على الإطار الحالي
     */
    fun getCurrentFrame(): SpriteFrame? {
        return currentClip?.frames?.getOrNull(currentFrameIndex)
    }

    /**
     * الحصول على الطبقة المحددة
     */
    fun getLayer(layer: AnimationLayer): LayerAnimation? {
        return _layers.value[layer]
    }

    /**
     * تفعيل/تعطيل طبقة
     */
    fun setLayerEnabled(layer: AnimationLayer, enabled: Boolean) {
        val updatedLayers = _layers.value.toMutableMap()
        updatedLayers[layer]?.let { current ->
            updatedLayers[layer] = current.copy(enabled = enabled)
        }
        _layers.value = updatedLayers
    }

    /**
     * تعيين شفافية طبقة
     */
    fun setLayerAlpha(layer: AnimationLayer, alpha: Float) {
        val updatedLayers = _layers.value.toMutableMap()
        updatedLayers[layer]?.let { current ->
            updatedLayers[layer] = current.copy(alpha = alpha.coerceIn(0f, 1f))
        }
        _layers.value = updatedLayers
    }

    /**
     * تعيين لون طبقة
     */
    fun setLayerTint(layer: AnimationLayer, tint: Long) {
        val updatedLayers = _layers.value.toMutableMap()
        updatedLayers[layer]?.let { current ->
            updatedLayers[layer] = current.copy(tint = tint)
        }
        _layers.value = updatedLayers
    }

    /**
     * الحصول على صناديق الضربات للإطار الحالي
     */
    fun getCurrentHitboxes(): List<AnimationHitbox> {
        return getCurrentFrame()?.hitboxes ?: emptyList()
    }

    /**
     * الحصول على صناديق الإصابة للإطار الحالي
     */
    fun getCurrentHurtboxes(): List<AnimationHitbox> {
        return getCurrentFrame()?.hurtboxes ?: emptyList()
    }

    /**
     * هل الأنيميشن الحالي انتهى؟
     */
    fun isFinished(): Boolean = isAnimationFinished

    /**
     * تشغيل أنيميشن مؤقت (VFX)
     *
     * @param state حالة الأنيميشن
     * @param onComplete callback عند الانتهاء
     */
    fun playOneShot(state: AnimationState, onComplete: (() -> Unit)? = null) {
        forceState(state)
        // TODO: تتبع onComplete callbacks
    }

    /**
     * إضافة flash effect (للضرر/الشفاء)
     *
     * @param color اللون
     * @param duration المدة
     */
    fun flashEffect(color: Long, duration: Float) {
        // TODO: تطبيق flash على جميع الطبقات
    }

    /**
     * إعادة تعيين الأنيميشن
     */
    fun reset() {
        changeState(AnimationState.IDLE)
        currentVelocityX = 0f
        currentVelocityY = 0f
        facingRight = true
        isTransitioning = false
        currentTransition = null
        transitionTime = 0f
        cloakAnimation.phase = 0f
        cloakAnimation.velocity = 0f
        satchelLeftAnimation.phase = 0f
        satchelLeftAnimation.velocity = 0f
        satchelRightAnimation.phase = 0f
        satchelRightAnimation.velocity = 0f
        maskGlowPhase = 0f
    }

    /**
     * الحصول على الإحصائيات (للتصحيح)
     */
    fun getStats(): AnimatorStats {
        return AnimatorStats(
            currentState = _currentState.value,
            previousState = previousState,
            currentFrameIndex = currentFrameIndex,
            totalFrames = currentClip?.frameCount ?: 0,
            isTransitioning = isTransitioning,
            isFinished = isAnimationFinished,
            totalFramesPlayed = totalFramesPlayed,
            totalTransitions = totalTransitions,
            fps = currentClip?.fps ?: 0f,
            loadedClips = animationClips.size,
            loadedTransitions = transitions.size
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Animator Stats (Debug)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * إحصائيات الأنيميشن (للتصحيح)
 */
data class AnimatorStats(
    val currentState: AnimationState,
    val previousState: AnimationState,
    val currentFrameIndex: Int,
    val totalFrames: Int,
    val isTransitioning: Boolean,
    val isFinished: Boolean,
    val totalFramesPlayed: Int,
    val totalTransitions: Int,
    val fps: Float,
    val loadedClips: Int,
    val loadedTransitions: Int
)