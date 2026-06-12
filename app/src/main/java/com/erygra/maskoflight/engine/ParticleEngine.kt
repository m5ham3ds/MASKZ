package com.erygra.maskoflight.engine

import com.erygra.maskoflight.core.GameConfig

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  ParticleEngine.kt — محرك الجسيمات والمؤثرات البصرية
 *  Erygra Universe 2.0 | Mask of Light
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  يُنشئ ويُحدّث ويُدير الجسيمات للمؤثرات البصرية:
 *  - جسيمات الضربات والقتال
 *  - تأثيرات البيئة (رماد، دخان، جليد)
 *  - توهج القناع (Mask Glow)
 *  - تأثيرات قدرات الذاكرة
 *  - تأثيرات الموت
 *
 *  التصميم:
 *  - Object Pooling لإعادة استخدام الجسيمات
 *  - إدارة حد أقصى للجسيمات حسب إعدادات الأداء
 *  - كل جسيم = data class immutable
 * ══════════════════════════════════════════════════════════════════════════
 */

// ─────────────────────────────────────────────────────────────────────────────
// أنواع الجسيمات
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ParticleType — نوع الجسيم يحدد سلوكه ومظهره.
 */
enum class ParticleType {
    // ─── قتال ────────────────────────────────────────────────────────────
    HIT_SPARK,          // شرارة الضربة
    HIT_SLASH,          // أثر الشرطة
    BLOOD_SPLATTER,     // رذاذ ضرر
    CRITICAL_BURST,     // انفجار الضربة الحرجة
    PARRY_FLASH,        // وميض الـ Parry
    BLOCK_DUST,         // غبار الصد

    // ─── قدرات الذاكرة ───────────────────────────────────────────────────
    MEMORY_PULSE_WAVE,  // موجة نبضة الذاكرة
    ECHO_TRAIL,         // أثر الصدى
    MASK_GLOW,          // توهج القناع
    VOID_FRAGMENT,      // شظية الفراغ

    // ─── البيئة ───────────────────────────────────────────────────────────
    ASH_FLOAT,          // رماد طائر (Ashen Sprawl)
    DUST_PUFF,          // غبار الخطوات
    SMOKE_PUFF,         // دخان
    EMBER,              // جمرة
    LEAF,               // أوراق (Moorlands)
    SNOW_FLAKE,         // ندفة ثلج (Glassfjord)
    BIOLUME_SPARK,      // بيولوم (Luminous Chasm)
    GEAR_SPARK,         // شرارة ميكانيكية (Clockworks)
    WATER_SPLASH,       // رذاذ ماء
    FROST, 
    SPARK, 
    POISON_BUBBLE, 
    FIRE, 
    ICE_SHARD,

    // ─── UI والتأثيرات ────────────────────────────────────────────────────
    XP_ORB,             // كرة XP
    COIN_SPARKLE,       // بريق العملة
    LEVEL_UP_BURST,     // انفجار ترقية المستوى
    SANCTUARY_LIGHT,    // ضوء نقطة التفتيش
    DEATH_DISSOLVE,      // تلاشي عند الموت
    
    // ─── أنواع إضافية للمناطق ───────────────────────────────────────────
    ASH,
    SMOKE,
    DUST,
    PAPER_SCRAP,
    CLOUD,
    FEATHER,
    WIND_GUST,
    SNOW,
    ICE_CRYSTAL,
    AURORA,
    BUBBLE,
    STEAM,
    OIL_DRIP,
    FOG,
    SPORE,
    FIREFLY,
    LIGHT_RAY,
    SPARKLE,
    VOID_PARTICLE
}

// ─────────────────────────────────────────────────────────────────────────────
// الجسيم
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Particle — جسيم واحد في العالم.
 *
 * @param id معرف فريد
 * @param type نوع الجسيم
 * @param position الموقع الحالي
 * @param velocity سرعة واتجاه الحركة
 * @param color اللون الحالي
 * @param startColor اللون الابتدائي
 * @param endColor اللون النهائي (للتدرج)
 * @param size الحجم الحالي
 * @param startSize الحجم الابتدائي
 * @param endSize الحجم النهائي
 * @param alpha الشفافية (0–1)
 * @param rotation الدوران (درجات)
 * @param angularVelocity سرعة الدوران
 * @param lifeTime العمر الكلي (ثوانٍ)
 * @param age العمر الحالي (ثوانٍ)
 * @param gravityScale تأثير الجاذبية عليه
 * @param friction مقاومة الهواء
 */
data class Particle(
    val id: Long,
    val type: ParticleType,
    val position: Vector2D,
    val velocity: Vector2D,
    val startColor: Color,
    val endColor: Color,
    val startSize: Float,
    val endSize: Float,
    val alpha: Float = 1f,
    val rotation: Float = 0f,
    val angularVelocity: Float = 0f,
    val lifeTime: Float,
    val age: Float = 0f,
    val gravityScale: Float = 0.3f,
    val friction: Float = 0.98f,
    val isAlive: Boolean = true
) {
    /** نسبة الحياة المتبقية (0 = طازج، 1 = ميت) */
    val lifeRatio: Float get() = if (lifeTime > 0f) (age / lifeTime).coerceIn(0f, 1f) else 1f

    /** اللون الحالي (interpolated) */
    val currentColor: Color
        get() = Color(
            red = startColor.red + (endColor.red - startColor.red) * lifeRatio,
            green = startColor.green + (endColor.green - startColor.green) * lifeRatio,
            blue = startColor.blue + (endColor.blue - startColor.blue) * lifeRatio,
            alpha = alpha * (1f - lifeRatio)
        )

    /** الحجم الحالي (interpolated) */
    val currentSize: Float
        get() = startSize + (endSize - startSize) * lifeRatio
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات المنبع
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ParticleEmitterConfig — إعدادات منبع الجسيمات.
 *
 * @param type نوع الجسيم
 * @param count عدد الجسيمات المنبثقة دفعة واحدة
 * @param continuous هل ينبثق باستمرار؟
 * @param emitRate معدل الانبثاق (جسيمات/ثانية) للـ continuous
 * @param spreadAngle زاوية الانتشار (درجات، 0 = اتجاه واحد، 360 = كل الاتجاهات)
 * @param baseDirection الاتجاه الأساسي (normalized)
 * @param minSpeed الحد الأدنى للسرعة
 * @param maxSpeed الحد الأقصى للسرعة
 * @param startColor اللون الابتدائي
 * @param endColor اللون النهائي
 * @param minSize الحجم الأدنى
 * @param maxSize الحجم الأقصى
 * @param endSizeMultiplier مضاعف الحجم النهائي
 * @param minLifeTime الحد الأدنى للعمر
 * @param maxLifeTime الحد الأقصى للعمر
 * @param gravityScale تأثير الجاذبية
 * @param friction الاحتكاك
 */
data class ParticleEmitterConfig(
    val type: ParticleType,
    val count: Int = 5,
    val continuous: Boolean = false,
    val emitRate: Float = 10f,
    val spreadAngle: Float = 360f,
    val baseDirection: Vector2D = Vector2D.UP,
    val minSpeed: Float = 50f,
    val maxSpeed: Float = 150f,
    val startColor: Color = Color.White,
    val endColor: Color = Color.Transparent,
    val minSize: Float = 4f,
    val maxSize: Float = 12f,
    val endSizeMultiplier: Float = 0.1f,
    val minLifeTime: Float = 0.3f,
    val maxLifeTime: Float = 0.8f,
    val gravityScale: Float = 0.3f,
    val friction: Float = 0.95f
)

// ─────────────────────────────────────────────────────────────────────────────
// محرك الجسيمات
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ParticleEngine — يدير إنشاء وتحديث الجسيمات.
 *
 * يستخدم قائمة mutable داخلياً (managed state).
 * يجب استدعاء update() كل فريم وrenderList للرسم.
 *
 * @param maxParticles الحد الأقصى للجسيمات المتزامنة
 */
class ParticleEngine(
    private val maxParticles: Int = 300
) {
    private val particles = ArrayDeque<Particle>(maxParticles)
    private var nextId = 0L

    /** قائمة الجسيمات للرسم (read-only snapshot) */
    val renderList: List<Particle> get() = particles.toList()

    /** عدد الجسيمات النشطة */
    val activeCount: Int get() = particles.size

    // ─────────────────────────────────────────────────────────────────────
    // التحديث الرئيسي
    // ─────────────────────────────────────────────────────────────────────

    /**
     * update — يحدث جميع الجسيمات لفريم واحد.
     *
     * @param deltaTime الفريم الزمني
     */
    fun update(deltaTime: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            val updated = updateParticle(particle, deltaTime)
            if (!updated.isAlive) {
                iterator.remove()
            }
        }
    }

    /**
     * updateParticle — يحدث جسيماً واحداً.
     */
    private fun updateParticle(particle: Particle, deltaTime: Float): Particle {
        val newAge = particle.age + deltaTime
        if (newAge >= particle.lifeTime) {
            return particle.copy(isAlive = false)
        }

        // تطبيق الجاذبية
        val gravityEffect = GameConfig.PhysicsConfig.GRAVITY * particle.gravityScale * deltaTime
        val newVelocity = Vector2D(
            x = particle.velocity.x * particle.friction,
            y = particle.velocity.y * particle.friction + gravityEffect
        )

        // تحديث الموقع
        val newPosition = particle.position + newVelocity * deltaTime

        // تحديث الدوران
        val newRotation = particle.rotation + particle.angularVelocity * deltaTime

        return particle.copy(
            position = newPosition,
            velocity = newVelocity,
            rotation = newRotation,
            age = newAge
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // إطلاق الجسيمات
    // ─────────────────────────────────────────────────────────────────────

    /**
     * emit — يُطلق مجموعة من الجسيمات.
     *
     * @param position موقع الانبثاق
     * @param config إعدادات الانبثاق
     * @param count عدد الجسيمات (يُلغي الـ config.count)
     */
    fun emit(
        position: Vector2D,
        config: ParticleEmitterConfig,
        count: Int = config.count
    ) {
        val available = maxParticles - particles.size
        val toEmit = minOf(count, available)

        repeat(toEmit) {
            particles.addLast(createParticle(position, config))
        }
    }

    /**
     * createParticle — ينشئ جسيماً واحداً بناءً على الإعدادات.
     */
    private fun createParticle(
        position: Vector2D,
        config: ParticleEmitterConfig
    ): Particle {
        // زاوية عشوائية ضمن نطاق الانتشار
        val halfSpread = config.spreadAngle / 2f
        val baseAngle = Math.toDegrees(
            kotlin.math.atan2(
                config.baseDirection.y.toDouble(),
                config.baseDirection.x.toDouble()
            )
        ).toFloat()
        val angle = Math.toRadians(
            (baseAngle + Random.nextFloat() * config.spreadAngle - halfSpread).toDouble()
        ).toFloat()

        val speed = config.minSpeed + Random.nextFloat() * (config.maxSpeed - config.minSpeed)
        val velocity = Vector2D(cos(angle) * speed, sin(angle) * speed)

        val size = config.minSize + Random.nextFloat() * (config.maxSize - config.minSize)
        val lifeTime = config.minLifeTime +
                Random.nextFloat() * (config.maxLifeTime - config.minLifeTime)

        return Particle(
            id = nextId++,
            type = config.type,
            position = position,
            velocity = velocity,
            startColor = config.startColor,
            endColor = config.endColor,
            startSize = size,
            endSize = size * config.endSizeMultiplier,
            angularVelocity = Random.nextFloat() * 360f - 180f,
            lifeTime = lifeTime,
            gravityScale = config.gravityScale,
            friction = config.friction
        )
    }

    /**
     * clear — يمسح جميع الجسيمات.
     */
    fun clear() {
        particles.clear()
    }

    // ─────────────────────────────────────────────────────────────────────
    // دوال مساعدة للمؤثرات المُعرَّفة مسبقاً
    // ─────────────────────────────────────────────────────────────────────

    /**
     * emitHitSpark — شرارة ضربة عادية.
     */
    fun emitHitSpark(position: Vector2D, isCritical: Boolean = false) {
        emit(
            position = position,
            config = ParticleEmitterConfig(
                type = if (isCritical) ParticleType.CRITICAL_BURST else ParticleType.HIT_SPARK,
                count = if (isCritical) 15 else 6,
                spreadAngle = 360f,
                minSpeed = if (isCritical) 100f else 60f,
                maxSpeed = if (isCritical) 280f else 150f,
                startColor = if (isCritical) Color(0xFFFFD700) else Color(0xFFFFAA44),
                endColor = Color.Transparent,
                minSize = if (isCritical) 4f else 2f,
                maxSize = if (isCritical) 10f else 6f,
                minLifeTime = 0.15f,
                maxLifeTime = if (isCritical) 0.5f else 0.3f,
                gravityScale = 0.5f
            )
        )
    }

    /**
     * emitParryFlash — وميض الـ Parry الناجح.
     */
    fun emitParryFlash(position: Vector2D) {
        emit(
            position = position,
            config = ParticleEmitterConfig(
                type = ParticleType.PARRY_FLASH,
                count = 20,
                spreadAngle = 360f,
                minSpeed = 150f,
                maxSpeed = 350f,
                startColor = Color(0xFF44DDFF),
                endColor = Color.Transparent,
                minSize = 3f,
                maxSize = 8f,
                minLifeTime = 0.2f,
                maxLifeTime = 0.5f,
                gravityScale = 0f,
                friction = 0.90f
            )
        )
    }

    /**
     * emitMemoryPulse — موجة نبضة الذاكرة.
     */
    fun emitMemoryPulse(position: Vector2D) {
        emit(
            position = position,
            config = ParticleEmitterConfig(
                type = ParticleType.MEMORY_PULSE_WAVE,
                count = 30,
                spreadAngle = 360f,
                baseDirection = Vector2D.RIGHT,
                minSpeed = 200f,
                maxSpeed = 400f,
                startColor = Color(0xFF8844FF),
                endColor = Color.Transparent,
                minSize = 5f,
                maxSize = 14f,
                endSizeMultiplier = 2f,
                minLifeTime = 0.3f,
                maxLifeTime = 0.6f,
                gravityScale = 0f,
                friction = 0.88f
            )
        )
    }

    /**
     * emitMaskGlow — توهج القناع.
     */
    fun emitMaskGlow(position: Vector2D, intensity: Float = 1f) {
        emit(
            position = position,
            config = ParticleEmitterConfig(
                type = ParticleType.MASK_GLOW,
                count = (3 * intensity).toInt().coerceAtLeast(1),
                spreadAngle = 120f,
                baseDirection = Vector2D.UP,
                minSpeed = 20f,
                maxSpeed = 60f,
                startColor = Color(0xFFFFEEAA),
                endColor = Color.Transparent,
                minSize = 3f,
                maxSize = 8f,
                minLifeTime = 0.4f,
                maxLifeTime = 0.8f,
                gravityScale = -0.1f,
                friction = 0.97f
            )
        )
    }

    /**
     * emitDustPuff — غبار الخطوات.
     */
    fun emitDustPuff(position: Vector2D) {
        emit(
            position = position,
            config = ParticleEmitterConfig(
                type = ParticleType.DUST_PUFF,
                count = 4,
                spreadAngle = 60f,
                baseDirection = Vector2D.UP,
                minSpeed = 30f,
                maxSpeed = 80f,
                startColor = Color(0xFFAA9977),
                endColor = Color.Transparent,
                minSize = 6f,
                maxSize = 14f,
                endSizeMultiplier = 2f,
                minLifeTime = 0.2f,
                maxLifeTime = 0.4f,
                gravityScale = -0.1f
            )
        )
    }

    /**
     * emitAshFloat — رماد عائم (Ashen Sprawl).
     *
     * يُستدعى باستمرار كل بضع ثوانٍ للمنطقة.
     */
    fun emitAshFloat(spawnArea: Rect, count: Int = 3) {
        repeat(count) {
            val randomX = spawnArea.left + Random.nextFloat() * spawnArea.width
            val randomY = spawnArea.bottom  // ينبثق من الأسفل ويصعد

            emit(
                position = Vector2D(randomX, randomY),
                config = ParticleEmitterConfig(
                    type = ParticleType.ASH_FLOAT,
                    count = 1,
                    spreadAngle = 30f,
                    baseDirection = Vector2D.UP,
                    minSpeed = 15f,
                    maxSpeed = 40f,
                    startColor = Color(0xFF888888),
                    endColor = Color.Transparent,
                    minSize = 2f,
                    maxSize = 5f,
                    minLifeTime = 2f,
                    maxLifeTime = 4f,
                    gravityScale = -0.05f,
                    friction = 0.99f
                )
            )
        }
    }

    /**
     * emitBiolumeSpark — بيولوم (Luminous Chasm).
     */
    fun emitBiolumeSpark(position: Vector2D) {
        emit(
            position = position,
            config = ParticleEmitterConfig(
                type = ParticleType.BIOLUME_SPARK,
                count = 8,
                spreadAngle = 360f,
                minSpeed = 30f,
                maxSpeed = 100f,
                startColor = Color(0xFF44FFAA),
                endColor = Color(0xFF004422),
                minSize = 3f,
                maxSize = 9f,
                endSizeMultiplier = 0.5f,
                minLifeTime = 0.8f,
                maxLifeTime = 2f,
                gravityScale = -0.05f,
                friction = 0.96f
            )
        )
    }

    /**
     * emitDeathDissolve — تلاشي عند الموت.
     */
    fun emitDeathDissolve(position: Vector2D, bodySize: Vector2D) {
        val count = 40
        repeat(count) {
            val offsetX = Random.nextFloat() * bodySize.x - bodySize.x / 2f
            val offsetY = Random.nextFloat() * bodySize.y - bodySize.y / 2f

            emit(
                position = Vector2D(position.x + offsetX, position.y + offsetY),
                config = ParticleEmitterConfig(
                    type = ParticleType.DEATH_DISSOLVE,
                    count = 1,
                    spreadAngle = 360f,
                    minSpeed = 50f,
                    maxSpeed = 200f,
                    startColor = Color(0xFF666666),
                    endColor = Color.Transparent,
                    minSize = 4f,
                    maxSize = 12f,
                    minLifeTime = 0.5f,
                    maxLifeTime = 1.2f,
                    gravityScale = 0.4f
                )
            )
        }
    }

    /**
     * emitLevelUp — انفجار ترقية المستوى.
     */
    fun emitLevelUp(position: Vector2D) {
        emit(
            position = position,
            config = ParticleEmitterConfig(
                type = ParticleType.LEVEL_UP_BURST,
                count = 50,
                spreadAngle = 360f,
                minSpeed = 100f,
                maxSpeed = 350f,
                startColor = Color(0xFFFFDD00),
                endColor = Color.Transparent,
                minSize = 3f,
                maxSize = 10f,
                minLifeTime = 0.4f,
                maxLifeTime = 1f,
                gravityScale = 0.2f
            )
        )
    }

    /**
     * emitSanctuaryLight — ضوء نقطة التفتيش.
     */
    fun emitSanctuaryLight(position: Vector2D) {
        emit(
            position = position,
            config = ParticleEmitterConfig(
                type = ParticleType.SANCTUARY_LIGHT,
                count = 5,
                spreadAngle = 20f,
                baseDirection = Vector2D.UP,
                minSpeed = 30f,
                maxSpeed = 70f,
                startColor = Color(0xFFFFFFDD),
                endColor = Color.Transparent,
                minSize = 4f,
                maxSize = 10f,
                endSizeMultiplier = 0.2f,
                minLifeTime = 1f,
                maxLifeTime = 2f,
                gravityScale = -0.1f
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات مُعرَّفة مسبقاً لكل منطقة
// ─────────────────────────────────────────────────────────────────────────────

/**
 * RegionParticlePresets — إعدادات الجسيمات البيئية لكل منطقة.
 */
object RegionParticlePresets {

    /** Ashen Sprawl — رماد ودخان */
    val ASHEN_SPRAWL = ParticleEmitterConfig(
        type = ParticleType.ASH_FLOAT,
        count = 2,
        continuous = true,
        emitRate = 3f,
        spreadAngle = 180f,
        baseDirection = Vector2D.UP,
        minSpeed = 10f,
        maxSpeed = 35f,
        startColor = Color(0xFF999988),
        endColor = Color.Transparent,
        minSize = 1f,
        maxSize = 4f,
        minLifeTime = 3f,
        maxLifeTime = 6f,
        gravityScale = -0.03f,
        friction = 0.99f
    )

    /** Luminous Chasm — بيولوم */
    val LUMINOUS_CHASM = ParticleEmitterConfig(
        type = ParticleType.BIOLUME_SPARK,
        count = 1,
        continuous = true,
        emitRate = 5f,
        spreadAngle = 360f,
        minSpeed = 20f,
        maxSpeed = 60f,
        startColor = Color(0xFF33FFBB),
        endColor = Color.Transparent,
        minSize = 2f,
        maxSize = 7f,
        minLifeTime = 1f,
        maxLifeTime = 3f,
        gravityScale = -0.05f
    )

    /** Glassfjord Cliffs — ثلج */
    val GLASSFJORD = ParticleEmitterConfig(
        type = ParticleType.SNOW_FLAKE,
        count = 2,
        continuous = true,
        emitRate = 8f,
        spreadAngle = 20f,
        baseDirection = Vector2D.DOWN,
        minSpeed = 30f,
        maxSpeed = 70f,
        startColor = Color(0xFFDDEEFF),
        endColor = Color.Transparent,
        minSize = 1f,
        maxSize = 4f,
        minLifeTime = 2f,
        maxLifeTime = 4f,
        gravityScale = 0.1f,
        friction = 0.99f
    )

    /** Blackroot Moorlands — أوراق وغبار */
    val BLACKROOT_MOORLANDS = ParticleEmitterConfig(
        type = ParticleType.LEAF,
        count = 1,
        continuous = true,
        emitRate = 2f,
        spreadAngle = 45f,
        baseDirection = Vector2D(0.5f, 0.5f),
        minSpeed = 20f,
        maxSpeed = 50f,
        startColor = Color(0xFF445533),
        endColor = Color(0xFF222211),
        minSize = 3f,
        maxSize = 8f,
        minLifeTime = 2f,
        maxLifeTime = 5f,
        gravityScale = 0.15f
    )

    /** Sunken Clockworks — شرار ميكانيكي */
    val SUNKEN_CLOCKWORKS = ParticleEmitterConfig(
        type = ParticleType.GEAR_SPARK,
        count = 1,
        continuous = true,
        emitRate = 4f,
        spreadAngle = 360f,
        minSpeed = 50f,
        maxSpeed = 120f,
        startColor = Color(0xFFFFAA00),
        endColor = Color.Transparent,
        minSize = 1f,
        maxSize = 3f,
        minLifeTime = 0.2f,
        maxLifeTime = 0.5f,
        gravityScale = 0.6f
    )
}