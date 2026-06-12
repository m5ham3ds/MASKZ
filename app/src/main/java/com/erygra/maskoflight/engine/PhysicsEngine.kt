package com.erygra.maskoflight.engine

import com.erygra.maskoflight.core.GameConfig.PhysicsConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  PhysicsEngine.kt — محرك الفيزياء الرئيسي
 *  Erygra Universe 2.0 | Mask of Light
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  يتحكم في كل حركة جسدية داخل اللعبة:
 *  - الجاذبية والسقوط
 *  - حركة اللاعب والأعداء
 *  - كشف التصادم (AABB)
 *  - القفز (عادي، مزدوج، جداري)
 *  - الـ Dash والـ Dodge Roll
 *  - التسلق على الجدران والحواف
 *  - المنصات أحادية الاتجاه (One-Way Platforms)
 *
 *  التصميم:
 *  - Pure functions حيث أمكن (لسهولة الاختبار)
 *  - لا يعتمد على Compose أو Android SDK
 *  - يعمل بوحدة px/s و px/s²
 * ══════════════════════════════════════════════════════════════════════════
 */

// ─────────────────────────────────────────────────────────────────────────────
// الأنواع الرياضية الأساسية
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Vector2D — متجه ثنائي الأبعاد مع عمليات رياضية كاملة.
 *
 * @param x المكوّن الأفقي
 * @param y المكوّن الرأسي (موجب = للأسفل في نظام الشاشة)
 */
data class Vector2D(
    val x: Float = 0f,
    val y: Float = 0f
) {
    // ─── العمليات الحسابية ────────────────────────────────────────────────

    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vector2D(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vector2D(x / scalar, y / scalar)
    operator fun unaryMinus() = Vector2D(-x, -y)

    // ─── الخصائص ─────────────────────────────────────────────────────────

    /** طول المتجه */
    val magnitude: Float get() = sqrt(x * x + y * y)

    /** مربع الطول (أسرع من magnitude عند المقارنة) */
    val sqrMagnitude: Float get() = x * x + y * y

    /** المتجه المُوحَّد (طوله = 1) */
    val normalized: Vector2D
        get() {
            val mag = magnitude
            return if (mag > 0.0001f) Vector2D(x / mag, y / mag) else ZERO
        }

    /** هل المتجه صفر تقريباً؟ */
    val isNearZero: Boolean get() = sqrMagnitude < 0.0001f

    // ─── الحساب المتجهي ───────────────────────────────────────────────────

    /** الضرب النقطي (Dot Product) */
    fun dot(other: Vector2D): Float = x * other.x + y * other.y

    /** المسافة إلى متجه آخر */
    fun distanceTo(other: Vector2D): Float = (this - other).magnitude

    /** المسافة المربعة (أسرع) */
    fun sqrDistanceTo(other: Vector2D): Float = (this - other).sqrMagnitude

    /** تحريك نحو متجه هدف بخطوة محددة */
    fun moveTowards(target: Vector2D, maxDelta: Float): Vector2D {
        val direction = target - this
        val distance = direction.magnitude
        return if (distance <= maxDelta || distance < 0.0001f) target
        else this + direction.normalized * maxDelta
    }

    /** الاستيفاء الخطي (Lerp) */
    fun lerp(target: Vector2D, t: Float): Vector2D {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector2D(
            x + (target.x - x) * clampedT,
            y + (target.y - y) * clampedT
        )
    }

    /** تقريب للـ Int */
    fun toIntPair(): Pair<Int, Int> = Pair(x.toInt(), y.toInt())

    override fun toString(): String = "Vector2D(%.2f, %.2f)".format(x, y)

    companion object {
        val ZERO = Vector2D(0f, 0f)
        val ONE = Vector2D(1f, 1f)
        val UP = Vector2D(0f, -1f)
        val DOWN = Vector2D(0f, 1f)
        val LEFT = Vector2D(-1f, 0f)
        val RIGHT = Vector2D(1f, 0f)

        /**
         * احسب المسافة بين نقطتين
         */
        fun distance(a: Vector2D, b: Vector2D): Float = (a - b).magnitude
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// المستطيل ونظام الاصطدام (AABB)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Rect — مستطيل Axis-Aligned Bounding Box للاصطدامات.
 *
 * @param left الحافة اليسرى
 * @param top الحافة العلوية
 * @param right الحافة اليمنى
 * @param bottom الحافة السفلية
 */
data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val center: Vector2D get() = Vector2D(centerX, centerY)

    /** هل يتقاطع هذا المستطيل مع آخر؟ */
    fun intersects(other: Rect): Boolean {
        return left < other.right &&
                right > other.left &&
                top < other.bottom &&
                bottom > other.top
    }

    /** هل تقع النقطة داخل المستطيل؟ */
    fun contains(point: Vector2D): Boolean {
        return point.x in left..right && point.y in top..bottom
    }

    /** احسب عمق التداخل مع مستطيل آخر */
    fun overlapDepth(other: Rect): Vector2D {
        val overlapX = when {
            centerX < other.centerX -> other.left - right
            else -> other.right - left
        }
        val overlapY = when {
            centerY < other.centerY -> other.top - bottom
            else -> other.bottom - top
        }
        return Vector2D(overlapX, overlapY)
    }

    /** توسيع المستطيل بمقدار محدد */
    fun expand(amount: Float): Rect = Rect(
        left - amount,
        top - amount,
        right + amount,
        bottom + amount
    )

    /** تحريك المستطيل */
    fun translate(dx: Float, dy: Float): Rect = Rect(
        left + dx,
        top + dy,
        right + dx,
        bottom + dy
    )

    companion object {
        /**
         * إنشاء مستطيل من موقع ومقاس
         */
        fun fromPosition(x: Float, y: Float, width: Float, height: Float): Rect {
            return Rect(x, y, x + width, y + height)
        }

        /**
         * إنشاء مستطيل من المركز والمقاس
         */
        fun fromCenter(cx: Float, cy: Float, width: Float, height: Float): Rect {
            val halfW = width / 2f
            val halfH = height / 2f
            return Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// أنواع المنصات
// ─────────────────────────────────────────────────────────────────────────────

/**
 * PlatformType — نوع المنصة يحدد سلوك الاصطدام.
 */
enum class PlatformType {
    /** منصة صلبة من كل الجهات */
    SOLID,

    /** منصة يمكن القفز عبرها من الأسفل (اصطدام من الأعلى فقط) */
    ONE_WAY,

    /** منصة زلقة (تقليل احتكاك) */
    SLIPPERY,

    /** منصة متحركة */
    MOVING,

    /** سطح لاصق (تسلق جداري) */
    CLIMBABLE,

    /** خطر (حمم أو شوك) — يسبب ضرراً */
    HAZARD,

    /** ماء — يغير فيزياء الحركة */
    WATER
}

/**
 * Platform — منصة في العالم.
 *
 * @param bounds حدود المنصة
 * @param type نوع المنصة
 * @param damagePerSecond الضرر في الثانية (للـ HAZARD)
 * @param movementPath مسار الحركة (للـ MOVING)
 * @param frictionMultiplier معامل الاحتكاك (1 = طبيعي، 0 = لا احتكاك)
 */
data class Platform(
    val id: String,
    val bounds: Rect,
    val type: PlatformType = PlatformType.SOLID,
    val damagePerSecond: Int = 0,
    val movementPath: List<Vector2D> = emptyList(),
    val movementSpeed: Float = 0f,
    val frictionMultiplier: Float = 1f
) {
    /** هل المنصة تسبب ضرراً؟ */
    val isHazard: Boolean get() = type == PlatformType.HAZARD

    /** هل يمكن التسلق عليها؟ */
    val isClimbable: Boolean get() = type == PlatformType.CLIMBABLE

    /** هل هي زلقة؟ */
    val isSlippery: Boolean get() = type == PlatformType.SLIPPERY
}

// ─────────────────────────────────────────────────────────────────────────────
// جسم الفيزياء
// ─────────────────────────────────────────────────────────────────────────────

/**
 * JumpType — نوع القفز.
 */
enum class JumpType {
    NORMAL,       // قفز عادي
    DOUBLE,       // قفز مزدوج في الهواء
    WALL_LEFT,    // قفز من جدار يساري
    WALL_RIGHT,   // قفز من جدار يميني
    LEDGE         // قفز من حافة (Coyote Time)
}

/**
 * DashDirection — اتجاه الـ Dash.
 */
enum class DashDirection {
    LEFT, RIGHT, UP, DOWN,
    UP_LEFT, UP_RIGHT,
    DOWN_LEFT, DOWN_RIGHT
}

/**
 * PhysicsBodyType — نوع الجسم الفيزيائي.
 */
enum class PhysicsBodyType {
    PLAYER,
    ENEMY,
    PROJECTILE,
    STATIC,
    TRIGGER
}

/**
 * PhysicsBody — يمثل جسماً خاضعاً للفيزياء في العالم.
 *
 * هذا هو data class الرئيسي الذي يُمرر بين الأنظمة.
 * immutable بالكامل — كل تحديث ينتج نسخة جديدة.
 */
data class PhysicsBody(
    val id: String,
    val type: PhysicsBodyType = PhysicsBodyType.PLAYER,

    // ─── الموقع والحركة ──────────────────────────────────────────────────
    val position: Vector2D = Vector2D.ZERO,
    val velocity: Vector2D = Vector2D.ZERO,
    val acceleration: Vector2D = Vector2D.ZERO,

    // ─── الحجم ───────────────────────────────────────────────────────────
    val width: Float = GameConfig.PhysicsConfig.PLAYER_WIDTH,
    val height: Float = GameConfig.PhysicsConfig.PLAYER_HEIGHT,

    // ─── حالة الأرضية والجدران ───────────────────────────────────────────
    val isGrounded: Boolean = false,
    val wasGrounded: Boolean = false,        // الفريم السابق (Coyote Time)
    val isTouchingWallLeft: Boolean = false,
    val isTouchingWallRight: Boolean = false,
    val isTouchingCeiling: Boolean = false,
    val isOnOneWayPlatform: Boolean = false,
    val isInWater: Boolean = false,

    // ─── حالة القفز ──────────────────────────────────────────────────────
    val jumpsRemaining: Int = 1,             // 1 = قفز عادي، 2 = مزدوج
    val jumpBufferFrames: Int = 0,           // Buffered jump input
    val coyoteFramesRemaining: Int = 0,      // Coyote time countdown

    // ─── حالة الـ Dash ────────────────────────────────────────────────────
    val isDashing: Boolean = false,
    val dashFramesRemaining: Int = 0,
    val dashCooldownFrames: Int = 0,
    val dashDirection: DashDirection = DashDirection.RIGHT,
    val dashInvincibilityFrames: Int = 0,

    // ─── حالة الـ Dodge ───────────────────────────────────────────────────
    val isDodging: Boolean = false,
    val dodgeFramesRemaining: Int = 0,
    val dodgeCooldownFrames: Int = 0,
    val dodgeInvincibilityFrames: Int = 0,

    // ─── التسلق ──────────────────────────────────────────────────────────
    val isClimbing: Boolean = false,
    val isOnLedge: Boolean = false,
    val climbingPlatformId: String? = null,

    // ─── الفيزياء ─────────────────────────────────────────────────────────
    val gravityScale: Float = 1f,           // مضاعف الجاذبية (0 = طيران)
    val mass: Float = 1f,
    val frictionOverride: Float = -1f,       // -1 = استخدم فريكشن المنصة

    // ─── الـ Hitbox (قد يختلف عن الـ bounds للرسم) ───────────────────────
    val hitboxOffsetX: Float = 0f,
    val hitboxOffsetY: Float = 0f,
    val hitboxWidth: Float = -1f,           // -1 = استخدم width
    val hitboxHeight: Float = -1f,          // -1 = استخدم height

    // ─── حالات التأثير ────────────────────────────────────────────────────
    val isKnockedBack: Boolean = false,
    val knockbackFrames: Int = 0,
    val isFrozen: Boolean = false,           // لا يتحرك (حالة خاصة)
    val isInvincible: Boolean = false,       // لا يستقبل ضرراً
    val invincibilityFrames: Int = 0,

    // ─── الاتجاه ──────────────────────────────────────────────────────────
    val facingRight: Boolean = true,

    // ─── إضافي ───────────────────────────────────────────────────────────
    val currentPlatformId: String? = null   // المنصة التي يقف عليها
) {
    /** الحدود الكاملة (للرسم) */
    val bounds: Rect
        get() = Rect(
            left = position.x,
            top = position.y,
            right = position.x + width,
            bottom = position.y + height
        )

    /** الـ Hitbox (للاصطدامات) */
    val hitbox: Rect
        get() {
            val hW = if (hitboxWidth > 0f) hitboxWidth else width
            val hH = if (hitboxHeight > 0f) hitboxHeight else height
            return Rect(
                left = position.x + hitboxOffsetX,
                top = position.y + hitboxOffsetY,
                right = position.x + hitboxOffsetX + hW,
                bottom = position.y + hitboxOffsetY + hH
            )
        }

    /** هل الجسم في حالة حصانة؟ */
    val hasInvincibility: Boolean
        get() = isInvincible ||
                invincibilityFrames > 0 ||
                dashInvincibilityFrames > 0 ||
                dodgeInvincibilityFrames > 0

    /** هل يمكن للجسم القفز الآن؟ */
    val canJump: Boolean
        get() = (isGrounded || coyoteFramesRemaining > 0) && jumpsRemaining > 0

    /** هل يمكن الـ Dash الآن؟ */
    val canDash: Boolean
        get() = !isDashing && dashCooldownFrames <= 0

    /** هل يمكن الـ Dodge الآن؟ */
    val canDodge: Boolean
        get() = !isDodging && dodgeCooldownFrames <= 0 && isGrounded

    /** هل الجسم يسقط؟ */
    val isFalling: Boolean get() = !isGrounded && velocity.y > 0f

    /** هل الجسم يصعد؟ */
    val isRising: Boolean get() = velocity.y < 0f

    /** هل يلمس جداراً؟ */
    val isTouchingWall: Boolean
        get() = isTouchingWallLeft || isTouchingWallRight

    /** مركز الجسم */
    val center: Vector2D
        get() = Vector2D(position.x + width / 2f, position.y + height / 2f)
}

// ─────────────────────────────────────────────────────────────────────────────
// نتيجة الاصطدام
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CollisionResult — نتيجة فحص الاصطدام بين جسمين.
 */
data class CollisionResult(
    val hasCollision: Boolean,
    val platform: Platform? = null,
    val penetrationDepth: Vector2D = Vector2D.ZERO,
    val collisionNormal: Vector2D = Vector2D.ZERO,
    val isFromAbove: Boolean = false,
    val isFromBelow: Boolean = false,
    val isFromLeft: Boolean = false,
    val isFromRight: Boolean = false
)

/**
 * PhysicsUpdateResult — نتيجة تحديث الفيزياء لفريم واحد.
 */
data class PhysicsUpdateResult(
    val updatedBody: PhysicsBody,
    val collisions: List<CollisionResult> = emptyList(),
    val hazardDamage: Float = 0f,             // ضرر من المنصات الخطرة
    val isInWater: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// إدخال اللاعب للفيزياء
// ─────────────────────────────────────────────────────────────────────────────

/**
 * PhysicsInput — الإدخال الخام للحركة (من PlayerController).
 *
 * @param horizontalAxis -1 يسار، 0 ثابت، 1 يمين
 * @param verticalAxis -1 للأعلى (تسلق)، 1 للأسفل
 * @param jumpPressed ضُغط زر القفز هذا الفريم
 * @param jumpHeld الزر مضغوط باستمرار (قفز أعلى)
 * @param dashPressed ضُغط زر Dash
 * @param dodgePressed ضُغط زر Dodge
 * @param climbPressed ضُغط التسلق
 * @param dropThroughPressed إسقاط من One-Way Platform
 * @param isRunning يجري (يضغط على Run modifier)
 * @param dashDirection اتجاه الـ Dash
 */
data class PhysicsInput(
    val horizontalAxis: Float = 0f,
    val verticalAxis: Float = 0f,
    val jumpPressed: Boolean = false,
    val jumpHeld: Boolean = false,
    val dashPressed: Boolean = false,
    val dodgePressed: Boolean = false,
    val climbPressed: Boolean = false,
    val dropThroughPressed: Boolean = false,
    val isRunning: Boolean = false,
    val dashDirection: DashDirection = DashDirection.RIGHT
) {
    val isMoving: Boolean get() = abs(horizontalAxis) > 0.1f
    val movingRight: Boolean get() = horizontalAxis > 0.1f
    val movingLeft: Boolean get() = horizontalAxis < -0.1f
}

// ─────────────────────────────────────────────────────────────────────────────
// محرك الفيزياء الرئيسي
// ─────────────────────────────────────────────────────────────────────────────

/**
 * PhysicsEngine — المحرك الرئيسي للفيزياء.
 *
 * كل الوظائف تأخذ PhysicsBody وترجع PhysicsBody جديداً.
 * لا side effects — pure transformations.
 *
 * الاستخدام النموذجي لكل فريم:
 * ```
 * val result = PhysicsEngine.update(
 *     body = playerBody,
 *     input = currentInput,
 *     platforms = activePlatforms,
 *     deltaTime = frameTime
 * )
 * playerBody = result.updatedBody
 * ```
 */
object PhysicsEngine {

    // ─────────────────────────────────────────────────────────────────────
    // الدالة الرئيسية — تحديث فريم كامل
    // ─────────────────────────────────────────────────────────────────────

    /**
     * update — يحدث حالة الجسم الفيزيائي لفريم واحد.
     *
     * ترتيب التنفيذ:
     * 1. تحديث العدادات (cooldowns, timers)
     * 2. تطبيق الإدخال (dash, dodge, jump buffer)
     * 3. تطبيق الجاذبية
     * 4. تطبيق الحركة الأفقية
     * 5. تحريك الجسم
     * 6. كشف التصادمات وحلها
     * 7. تحديث حالة الأرضية
     *
     * @param body الجسم الحالي
     * @param input الإدخال من هذا الفريم
     * @param platforms قائمة المنصات النشطة
     * @param deltaTime الزمن الماضي منذ آخر فريم (بالثواني)
     */
    fun update(
        body: PhysicsBody,
        input: PhysicsInput,
        platforms: List<Platform>,
        deltaTime: Float
    ): PhysicsUpdateResult {
        if (body.isFrozen) {
            return PhysicsUpdateResult(updatedBody = body)
        }

        var current = body

        // 1. تحديث العدادات
        current = updateTimers(current)

        // 2. تطبيق الـ Dash
        current = if (input.dashPressed && current.canDash) {
            startDash(current, input.dashDirection)
        } else {
            continueDash(current, deltaTime)
        }

        // 3. تطبيق الـ Dodge
        current = if (input.dodgePressed && current.canDodge) {
            startDodge(current, input.horizontalAxis)
        } else {
            continueDodge(current, deltaTime)
        }

        // 4. Buffer القفز
        current = updateJumpBuffer(current, input)

        // 5. تطبيق القفز إذا كان الـ buffer نشطاً
        current = processJump(current, input)

        // 6. تطبيق الجاذبية (ما لم يكن Dash أو Climb)
        current = applyGravity(current, deltaTime)

        // 7. الحركة الأفقية
        current = applyHorizontalMovement(current, input, deltaTime)

        // 8. التسلق
        current = if (current.isClimbing && input.climbPressed) {
            applyClimbing(current, input, deltaTime)
        } else {
            current.copy(isClimbing = false)
        }

        // 9. تقييد السرعة
        current = clampVelocity(current)

        // 10. تحريك الجسم
        val newPosition = current.position + current.velocity * deltaTime
        current = current.copy(position = newPosition)

        // 11. كشف التصادمات وحلها
        val collisions = mutableListOf<CollisionResult>()
        var hazardDamage = 0f
        var inWater = false

        for (platform in platforms) {
            val collision = checkCollision(current, platform)
            if (collision.hasCollision) {
                collisions.add(collision)
                when (platform.type) {
                    PlatformType.HAZARD -> {
                        hazardDamage += platform.damagePerSecond * deltaTime
                    }
                    PlatformType.WATER -> {
                        inWater = true
                        current = applyWaterPhysics(current)
                    }
                    else -> {
                        current = resolveCollision(current, collision, platform)
                    }
                }
            }
        }

        // 12. فحص الأرضية
        current = updateGroundedState(current, platforms)

        // 13. فحص الجدران
        current = updateWallState(current, platforms)

        // 14. تحديث Coyote Time
        current = updateCoyoteTime(current)

        // 15. تحديث الاتجاه
        current = updateFacingDirection(current, input)

        // 16. تحديث حالة الماء
        current = current.copy(isInWater = inWater)

        return PhysicsUpdateResult(
            updatedBody = current,
            collisions = collisions,
            hazardDamage = hazardDamage,
            isInWater = inWater
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // تحديث العدادات
    // ─────────────────────────────────────────────────────────────────────

    /**
     * updateTimers — يقلل جميع العدادات الزمنية بمقدار 1 لكل فريم.
     */
    private fun updateTimers(body: PhysicsBody): PhysicsBody {
        return body.copy(
            dashCooldownFrames = max(0, body.dashCooldownFrames - 1),
            dodgeCooldownFrames = max(0, body.dodgeCooldownFrames - 1),
            dashInvincibilityFrames = max(0, body.dashInvincibilityFrames - 1),
            dodgeInvincibilityFrames = max(0, body.dodgeInvincibilityFrames - 1),
            invincibilityFrames = max(0, body.invincibilityFrames - 1),
            knockbackFrames = max(0, body.knockbackFrames - 1),
            jumpBufferFrames = max(0, body.jumpBufferFrames - 1)
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // الجاذبية
    // ─────────────────────────────────────────────────────────────────────

    /**
     * applyGravity — يطبق الجاذبية على الجسم.
     *
     * لا تُطبَّق الجاذبية إذا:
     * - الجسم على الأرض
     * - الجسم يتسلق
     * - الجسم في حالة Dash
     * - gravityScale = 0
     */
    private fun applyGravity(body: PhysicsBody, deltaTime: Float): PhysicsBody {
        if (body.isGrounded || body.isClimbing || body.isDashing || body.gravityScale == 0f) {
            return body
        }

        // جاذبية أقوى عند السقوط (feel أفضل)
        val gravityMultiplier = if (body.velocity.y > 0f) 1.4f else 1f

        val gravityForce = GameConfig.PhysicsConfig.GRAVITY *
                body.gravityScale *
                gravityMultiplier *
                deltaTime

        val newVelocityY = body.velocity.y + gravityForce

        // تقييد سرعة السقوط في الماء
        val maxFall = if (body.isInWater) {
            GameConfig.PhysicsConfig.MAX_FALL_SPEED * 0.4f
        } else {
            GameConfig.PhysicsConfig.MAX_FALL_SPEED
        }

        return body.copy(
            velocity = body.velocity.copy(
                y = min(newVelocityY, maxFall)
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // الحركة الأفقية
    // ─────────────────────────────────────────────────────────────────────

    /**
     * applyHorizontalMovement — يحرك الجسم أفقياً بناءً على الإدخال.
     */
    private fun applyHorizontalMovement(
        body: PhysicsBody,
        input: PhysicsInput,
        deltaTime: Float
    ): PhysicsBody {
        // لا حركة أفقية عادية أثناء Dash أو Knockback
        if (body.isDashing || body.isKnockedBack) return body

        val targetSpeed = when {
            !input.isMoving -> 0f
            input.isRunning -> GameConfig.PhysicsConfig.RUN_SPEED * input.horizontalAxis
            else -> GameConfig.PhysicsConfig.MOVE_SPEED * input.horizontalAxis
        }

        // تسارع أبطأ في الهواء
        val acceleration = if (body.isGrounded) {
            GameConfig.PhysicsConfig.ACCELERATION
        } else {
            GameConfig.PhysicsConfig.ACCELERATION * GameConfig.PhysicsConfig.AIR_CONTROL
        }

        // تباطؤ عند التوقف
        val deceleration = if (body.isGrounded) {
            GameConfig.PhysicsConfig.DECELERATION
        } else {
            GameConfig.PhysicsConfig.DECELERATION * GameConfig.PhysicsConfig.AIR_CONTROL
        }

        val currentSpeed = body.velocity.x
        val newSpeed = when {
            abs(targetSpeed) > abs(currentSpeed) || sign(targetSpeed) != sign(currentSpeed) -> {
                // تسارع نحو السرعة المستهدفة
                currentSpeed + (targetSpeed - currentSpeed) * acceleration * deltaTime
            }
            abs(targetSpeed) < abs(currentSpeed) -> {
                // تباطؤ
                val decelerationAmount = deceleration * deltaTime
                if (abs(currentSpeed - targetSpeed) < decelerationAmount) targetSpeed
                else currentSpeed - sign(currentSpeed) * decelerationAmount
            }
            else -> targetSpeed
        }

        return body.copy(
            velocity = body.velocity.copy(x = newSpeed)
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // القفز
    // ─────────────────────────────────────────────────────────────────────

    /**
     * updateJumpBuffer — يُخزن طلب القفز لعدة فريمات (Jump Buffering).
     */
    private fun updateJumpBuffer(body: PhysicsBody, input: PhysicsInput): PhysicsBody {
        return if (input.jumpPressed) {
            body.copy(jumpBufferFrames = GameConfig.PhysicsConfig.JUMP_BUFFER_FRAMES)
        } else {
            body
        }
    }

    /**
     * processJump — يُنفذ القفز إذا كانت الشروط مكتملة.
     */
    fun processJump(body: PhysicsBody, input: PhysicsInput): PhysicsBody {
        if (body.jumpBufferFrames <= 0) return body

        return when {
            // قفز عادي أو Coyote
            body.isGrounded || body.coyoteFramesRemaining > 0 -> {
                performJump(body, JumpType.NORMAL)
            }

            // قفز مزدوج
            body.jumpsRemaining > 0 && !body.isGrounded -> {
                performJump(body, JumpType.DOUBLE)
            }

            // قفز جداري يسار
            body.isTouchingWallLeft && !body.isGrounded -> {
                performJump(body, JumpType.WALL_RIGHT)
            }

            // قفز جداري يمين
            body.isTouchingWallRight && !body.isGrounded -> {
                performJump(body, JumpType.WALL_LEFT)
            }

            else -> body
        }
    }

    /**
     * performJump — ينفذ نوعاً محدداً من القفز.
     */
    private fun performJump(body: PhysicsBody, type: JumpType): PhysicsBody {
        val jumpVelocity = when (type) {
            JumpType.NORMAL, JumpType.LEDGE -> {
                Vector2D(body.velocity.x, GameConfig.PhysicsConfig.JUMP_FORCE)
            }
            JumpType.DOUBLE -> {
                Vector2D(body.velocity.x, GameConfig.PhysicsConfig.DOUBLE_JUMP_FORCE)
            }
            JumpType.WALL_LEFT -> {
                // قفز بعيداً عن الجدار الأيسر (يمين)
                Vector2D(GameConfig.PhysicsConfig.WALL_JUMP_HORIZONTAL, GameConfig.PhysicsConfig.WALL_JUMP_VERTICAL)
            }
            JumpType.WALL_RIGHT -> {
                // قفز بعيداً عن الجدار الأيمن (يسار)
                Vector2D(-GameConfig.PhysicsConfig.WALL_JUMP_HORIZONTAL, GameConfig.PhysicsConfig.WALL_JUMP_VERTICAL)
            }
        }

        val newJumpsRemaining = when (type) {
            JumpType.NORMAL, JumpType.LEDGE -> body.jumpsRemaining
            JumpType.DOUBLE -> body.jumpsRemaining - 1
            JumpType.WALL_LEFT, JumpType.WALL_RIGHT -> 1 // يعيد القفز المزدوج
        }

        return body.copy(
            velocity = jumpVelocity,
            isGrounded = false,
            jumpsRemaining = newJumpsRemaining,
            jumpBufferFrames = 0,
            coyoteFramesRemaining = 0
        )
    }

    /**
     * applyJump — واجهة عامة لتطبيق القفز خارجياً.
     */
    fun applyJump(body: PhysicsBody, type: JumpType): PhysicsBody {
        return performJump(body, type)
    }

    // ─────────────────────────────────────────────────────────────────────
    // الـ Dash
    // ─────────────────────────────────────────────────────────────────────

    /**
     * startDash — يبدأ حركة Dash.
     */
    fun startDash(body: PhysicsBody, direction: DashDirection): PhysicsBody {
        val dashVelocity = when (direction) {
            DashDirection.RIGHT -> Vector2D(GameConfig.PhysicsConfig.DASH_SPEED, 0f)
            DashDirection.LEFT -> Vector2D(-GameConfig.PhysicsConfig.DASH_SPEED, 0f)
            DashDirection.UP -> Vector2D(0f, -GameConfig.PhysicsConfig.DASH_SPEED)
            DashDirection.DOWN -> Vector2D(0f, GameConfig.PhysicsConfig.DASH_SPEED)
            DashDirection.UP_RIGHT -> Vector2D(
                GameConfig.PhysicsConfig.DASH_SPEED * 0.707f,
                -GameConfig.PhysicsConfig.DASH_SPEED * 0.707f
            )
            DashDirection.UP_LEFT -> Vector2D(
                -GameConfig.PhysicsConfig.DASH_SPEED * 0.707f,
                -GameConfig.PhysicsConfig.DASH_SPEED * 0.707f
            )
            DashDirection.DOWN_RIGHT -> Vector2D(
                GameConfig.PhysicsConfig.DASH_SPEED * 0.707f,
                GameConfig.PhysicsConfig.DASH_SPEED * 0.707f
            )
            DashDirection.DOWN_LEFT -> Vector2D(
                -GameConfig.PhysicsConfig.DASH_SPEED * 0.707f,
                GameConfig.PhysicsConfig.DASH_SPEED * 0.707f
            )
        }

        return body.copy(
            velocity = dashVelocity,
            isDashing = true,
            dashFramesRemaining = GameConfig.PhysicsConfig.DASH_DURATION_FRAMES,
            dashCooldownFrames = GameConfig.PhysicsConfig.DASH_COOLDOWN_FRAMES,
            dashDirection = direction,
            dashInvincibilityFrames = GameConfig.PhysicsConfig.DASH_INVINCIBILITY_FRAMES,
            gravityScale = 0f  // لا جاذبية أثناء Dash
        )
    }

    /**
     * continueDash — يستمر في تنفيذ الـ Dash أو ينهيه.
     */
    private fun continueDash(body: PhysicsBody, deltaTime: Float): PhysicsBody {
        if (!body.isDashing) return body

        val newFrames = body.dashFramesRemaining - 1

        return if (newFrames <= 0) {
            // انتهى الـ Dash — عودة للجاذبية الطبيعية
            body.copy(
                isDashing = false,
                dashFramesRemaining = 0,
                gravityScale = 1f,
                // حفظ بعض من سرعة الـ Dash
                velocity = body.velocity.copy(
                    x = body.velocity.x * 0.5f,
                    y = if (body.velocity.y < 0) body.velocity.y else 0f
                )
            )
        } else {
            body.copy(dashFramesRemaining = newFrames)
        }
    }

    /**
     * applyDash — واجهة عامة للـ Dash.
     */
    fun applyDash(body: PhysicsBody, direction: DashDirection): PhysicsBody {
        return if (body.canDash) startDash(body, direction) else body
    }

    // ─────────────────────────────────────────────────────────────────────
    // الـ Dodge Roll
    // ─────────────────────────────────────────────────────────────────────

    /**
     * startDodge — يبدأ حركة Dodge Roll.
     */
    fun startDodge(body: PhysicsBody, horizontalAxis: Float): PhysicsBody {
        val direction = if (horizontalAxis >= 0) 1f else -1f

        return body.copy(
            velocity = body.velocity.copy(x = GameConfig.PhysicsConfig.DODGE_SPEED * direction),
            isDodging = true,
            dodgeFramesRemaining = GameConfig.PhysicsConfig.DODGE_DURATION_FRAMES,
            dodgeCooldownFrames = GameConfig.PhysicsConfig.DODGE_COOLDOWN_FRAMES,
            dodgeInvincibilityFrames = GameConfig.PhysicsConfig.DODGE_INVINCIBILITY_FRAMES
        )
    }

    /**
     * continueDodge — يستمر في الـ Dodge أو ينهيه.
     */
    private fun continueDodge(body: PhysicsBody, deltaTime: Float): PhysicsBody {
        if (!body.isDodging) return body

        val newFrames = body.dodgeFramesRemaining - 1
        return if (newFrames <= 0) {
            body.copy(
                isDodging = false,
                dodgeFramesRemaining = 0
            )
        } else {
            body.copy(dodgeFramesRemaining = newFrames)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // التسلق
    // ─────────────────────────────────────────────────────────────────────

    /**
     * applyClimbing — يطبق حركة التسلق على الجدار أو الحافة.
     */
    fun applyClimbing(
        body: PhysicsBody,
        input: PhysicsInput,
        deltaTime: Float
    ): PhysicsBody {
        val climbVelocityX = 0f  // لا حركة أفقية أثناء التسلق
        val climbVelocityY = input.verticalAxis * GameConfig.PhysicsConfig.CLIMB_SPEED

        return body.copy(
            velocity = Vector2D(climbVelocityX, climbVelocityY),
            gravityScale = 0f,
            isClimbing = true
        )
    }

    /**
     * checkAndStartClimbing — يفحص إمكانية بدء التسلق.
     */
    fun checkAndStartClimbing(body: PhysicsBody, platforms: List<Platform>): PhysicsBody {
        val climbablePlatform = platforms.firstOrNull { platform ->
            platform.isClimbable && body.hitbox.intersects(platform.bounds)
        }

        return if (climbablePlatform != null) {
            body.copy(
                isClimbing = true,
                climbingPlatformId = climbablePlatform.id
            )
        } else {
            body.copy(isClimbing = false, climbingPlatformId = null)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // كشف التصادم وحله
    // ─────────────────────────────────────────────────────────────────────

    /**
     * checkCollision — يفحص التصادم بين جسم ومنصة.
     */
    fun checkCollision(body: PhysicsBody, platform: Platform): CollisionResult {
        val bodyHitbox = body.hitbox
        val platformBounds = platform.bounds

        if (!bodyHitbox.intersects(platformBounds)) {
            return CollisionResult(hasCollision = false)
        }

        // One-Way Platform: فقط من الأعلى
        if (platform.type == PlatformType.ONE_WAY) {
            val prevBottom = body.position.y + body.height - body.velocity.y * (1f / 60f)
            val isLandingOnTop = prevBottom <= platformBounds.top +
                    GameConfig.PhysicsConfig.ONE_WAY_PLATFORM_TOLERANCE
            if (!isLandingOnTop || body.velocity.y < 0f) {
                return CollisionResult(hasCollision = false)
            }
        }

        // احسب عمق التداخل
        val overlapX = when {
            bodyHitbox.centerX < platformBounds.centerX ->
                platformBounds.left - bodyHitbox.right
            else -> platformBounds.right - bodyHitbox.left
        }

        val overlapY = when {
            bodyHitbox.centerY < platformBounds.centerY ->
                platformBounds.top - bodyHitbox.bottom
            else -> platformBounds.bottom - bodyHitbox.top
        }

        // حدد اتجاه الاصطدام بأصغر تداخل
        val isFromAbove = abs(overlapY) <= abs(overlapX) &&
                bodyHitbox.centerY < platformBounds.centerY
        val isFromBelow = abs(overlapY) <= abs(overlapX) &&
                bodyHitbox.centerY >= platformBounds.centerY
        val isFromLeft = abs(overlapX) < abs(overlapY) &&
                bodyHitbox.centerX < platformBounds.centerX
        val isFromRight = abs(overlapX) < abs(overlapY) &&
                bodyHitbox.centerX >= platformBounds.centerX

        val normal = when {
            isFromAbove -> Vector2D.UP
            isFromBelow -> Vector2D.DOWN
            isFromLeft -> Vector2D.LEFT
            else -> Vector2D.RIGHT
        }

        return CollisionResult(
            hasCollision = true,
            platform = platform,
            penetrationDepth = Vector2D(overlapX, overlapY),
            collisionNormal = normal,
            isFromAbove = isFromAbove,
            isFromBelow = isFromBelow,
            isFromLeft = isFromLeft,
            isFromRight = isFromRight
        )
    }

    /**
     * resolveCollision — يحل التصادم بتعديل موقع وسرعة الجسم.
     */
    private fun resolveCollision(
        body: PhysicsBody,
        collision: CollisionResult,
        platform: Platform
    ): PhysicsBody {
        var newPosition = body.position
        var newVelocity = body.velocity

        val friction = when {
            body.frictionOverride >= 0 -> body.frictionOverride
            platform.isSlippery -> 0.05f
            else -> platform.frictionMultiplier
        }

        when {
            collision.isFromAbove -> {
                // هبوط على الأرض
                newPosition = newPosition.copy(
                    y = collision.platform!!.bounds.top - body.height
                )
                newVelocity = newVelocity.copy(y = 0f)
                // تطبيق الاحتكاك
                newVelocity = newVelocity.copy(
                    x = newVelocity.x * friction
                )
            }
            collision.isFromBelow -> {
                // اصطدام بالسقف
                newPosition = newPosition.copy(
                    y = collision.platform!!.bounds.bottom
                )
                newVelocity = newVelocity.copy(y = 0f)
            }
            collision.isFromLeft -> {
                // اصطدام بجدار يسار
                newPosition = newPosition.copy(
                    x = collision.platform!!.bounds.left - body.width
                )
                newVelocity = newVelocity.copy(x = 0f)
            }
            collision.isFromRight -> {
                // اصطدام بجدار يمين
                newPosition = newPosition.copy(
                    x = collision.platform!!.bounds.right
                )
                newVelocity = newVelocity.copy(x = 0f)
            }
        }

        return body.copy(
            position = newPosition,
            velocity = newVelocity
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // تحديث الحالات
    // ─────────────────────────────────────────────────────────────────────

    /**
     * updateGroundedState — يحدّث حالة isGrounded بعد حل التصادمات.
     */
    private fun updateGroundedState(
        body: PhysicsBody,
        platforms: List<Platform>
    ): PhysicsBody {
        // فحص بسيط: هل يوجد منصة أسفل الجسم بمسافة صغيرة؟
        val feetRect = Rect(
            left = body.hitbox.left + 2f,
            top = body.hitbox.bottom,
            right = body.hitbox.right - 2f,
            bottom = body.hitbox.bottom + 4f
        )

        val grounded = platforms.any { platform ->
            platform.type != PlatformType.HAZARD &&
                    feetRect.intersects(platform.bounds)
        }

        val jumpsReset = if (grounded && !body.isGrounded) 1 else body.jumpsRemaining

        return body.copy(
            wasGrounded = body.isGrounded,
            isGrounded = grounded,
            jumpsRemaining = if (grounded) 1 else jumpsReset,
            currentPlatformId = if (grounded) {
                platforms.firstOrNull { feetRect.intersects(it.bounds) }?.id
            } else null
        )
    }

    /**
     * updateWallState — يحدّث حالة لمس الجدران.
     */
    private fun updateWallState(
        body: PhysicsBody,
        platforms: List<Platform>
    ): PhysicsBody {
        val leftRect = Rect(
            left = body.hitbox.left - 4f,
            top = body.hitbox.top + 8f,
            right = body.hitbox.left,
            bottom = body.hitbox.bottom - 8f
        )

        val rightRect = Rect(
            left = body.hitbox.right,
            top = body.hitbox.top + 8f,
            right = body.hitbox.right + 4f,
            bottom = body.hitbox.bottom - 8f
        )

        val solidPlatforms = platforms.filter {
            it.type == PlatformType.SOLID || it.type == PlatformType.CLIMBABLE
        }

        val touchingLeft = solidPlatforms.any { leftRect.intersects(it.bounds) }
        val touchingRight = solidPlatforms.any { rightRect.intersects(it.bounds) }

        return body.copy(
            isTouchingWallLeft = touchingLeft,
            isTouchingWallRight = touchingRight
        )
    }

    /**
     * updateCoyoteTime — يدير Coyote Time (قفز بعد مغادرة الحافة).
     */
    private fun updateCoyoteTime(body: PhysicsBody): PhysicsBody {
        return when {
            // كان على الأرض والآن في الهواء — ابدأ العداد
            body.wasGrounded && !body.isGrounded && body.velocity.y >= 0f -> {
                body.copy(coyoteFramesRemaining = GameConfig.PhysicsConfig.JUMP_BUFFER_FRAMES)
            }
            // خصم فريم من العداد
            body.coyoteFramesRemaining > 0 -> {
                body.copy(coyoteFramesRemaining = body.coyoteFramesRemaining - 1)
            }
            else -> body
        }
    }

    /**
     * updateFacingDirection — يحدّث اتجاه مواجهة الجسم.
     */
    private fun updateFacingDirection(body: PhysicsBody, input: PhysicsInput): PhysicsBody {
        return when {
            input.movingRight -> body.copy(facingRight = true)
            input.movingLeft -> body.copy(facingRight = false)
            else -> body
        }
    }

    /**
     * applyWaterPhysics — يعدّل الفيزياء داخل الماء.
     */
    private fun applyWaterPhysics(body: PhysicsBody): PhysicsBody {
        return body.copy(
            velocity = body.velocity * 0.6f,  // تباطؤ في الماء
            gravityScale = 0.3f
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // تقييد السرعة
    // ─────────────────────────────────────────────────────────────────────

    /**
     * clampVelocity — يقيّد السرعة بالحدود القصوى.
     */
    private fun clampVelocity(body: PhysicsBody): PhysicsBody {
        if (body.isDashing) return body  // الـ Dash يتجاوز الحدود العادية

        val maxH = if (body.isInWater) {
            GameConfig.PhysicsConfig.MOVE_SPEED * 0.6f
        } else {
            GameConfig.PhysicsConfig.RUN_SPEED
        }

        return body.copy(
            velocity = Vector2D(
                x = body.velocity.x.coerceIn(-maxH, maxH),
                y = body.velocity.y.coerceIn(
                    -GameConfig.PhysicsConfig.TERMINAL_VELOCITY,
                    GameConfig.PhysicsConfig.TERMINAL_VELOCITY
                )
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // الـ Knockback
    // ─────────────────────────────────────────────────────────────────────

    /**
     * applyKnockback — يطبق رد فعل الضربة على الجسم.
     *
     * @param body الجسم المُصاب
     * @param sourcePosition موقع مصدر الضربة
     * @param force قوة الدفع
     * @param isHeavy هل هو Knockback قوي؟
     */
    fun applyKnockback(
        body: PhysicsBody,
        sourcePosition: Vector2D,
        force: Float = com.erygra.maskoflight.core.GameConfig.CombatConfig.KNOCKBACK_BASE,
        isHeavy: Boolean = false
    ): PhysicsBody {
        val direction = (body.center - sourcePosition).normalized
        val knockbackForce = if (isHeavy) com.erygra.maskoflight.core.GameConfig.CombatConfig.KNOCKBACK_HEAVY else force
        val airMultiplier = if (!body.isGrounded) com.erygra.maskoflight.core.GameConfig.CombatConfig.KNOCKBACK_AIR_MULTIPLIER else 1f

        val knockbackVelocity = direction * knockbackForce * airMultiplier

        return body.copy(
            velocity = knockbackVelocity,
            isKnockedBack = true,
            knockbackFrames = 20,
            invincibilityFrames = com.erygra.maskoflight.core.GameConfig.PlayerConfig.INVINCIBILITY_AFTER_HIT_FRAMES
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // الـ Teleport والـ Warp
    // ─────────────────────────────────────────────────────────────────────

    /**
     * teleportTo — ينقل الجسم فوراً إلى موقع جديد.
     *
     * يُستخدم للـ Fast Travel وتحميل Sanctuary.
     */
    fun teleportTo(body: PhysicsBody, targetPosition: Vector2D): PhysicsBody {
        return body.copy(
            position = targetPosition,
            velocity = Vector2D.ZERO,
            isGrounded = false,
            isDashing = false,
            dashFramesRemaining = 0,
            isClimbing = false
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // استعلامات مساعدة
    // ─────────────────────────────────────────────────────────────────────

    /**
     * isBodyOnScreen — هل الجسم داخل نطاق الكاميرا؟
     *
     * @param body الجسم المفحوص
     * @param cameraRect حدود الكاميرا
     * @param margin هامش إضافي
     */
    fun isBodyOnScreen(
        body: PhysicsBody,
        cameraRect: Rect,
        margin: Float = 100f
    ): Boolean {
        return body.bounds.intersects(cameraRect.expand(margin))
    }

    /**
     * getBodiesInRange — يُرجع الأجسام القريبة من موقع.
     *
     * @param bodies قائمة الأجسام
     * @param center المركز
     * @param radius نصف القطر
     */
    fun getBodiesInRange(
        bodies: List<PhysicsBody>,
        center: Vector2D,
        radius: Float
    ): List<PhysicsBody> {
        val radiusSqr = radius * radius
        return bodies.filter {
            it.center.sqrDistanceTo(center) <= radiusSqr
        }
    }

    /**
     * checkLineOfSight — هل هناك خط رؤية بين نقطتين؟ (بدون عوائق)
     *
     * @param from نقطة البداية
     * @param to نقطة النهاية
     * @param platforms المنصات كعوائق
     * @param steps دقة الفحص
     */
    fun checkLineOfSight(
        from: Vector2D,
        to: Vector2D,
        platforms: List<Platform>,
        steps: Int = 20
    ): Boolean {
        val direction = to - from
        val stepSize = 1f / steps

        for (i in 1 until steps) {
            val point = from + direction * (stepSize * i)
            val pointRect = Rect(point.x - 1f, point.y - 1f, point.x + 1f, point.y + 1f)

            val blocked = platforms.any { platform ->
                platform.type == PlatformType.SOLID &&
                        pointRect.intersects(platform.bounds)
            }

            if (blocked) return false
        }

        return true
    }
}