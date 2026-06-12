package com.erygra.maskoflight.engine

import com.erygra.maskoflight.core.GameConfig.CombatConfig
import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.core.GameConfig.MemoryConfig
import kotlin.math.min
import kotlin.random.Random

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  CombatEngine.kt — محرك القتال الرئيسي
 *  Erygra Universe 2.0 | Mask of Light
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  يتحكم في كل جوانب القتال:
 *  - كشف منطقة الضربة (Hitbox detection)
 *  - حساب الضرر (مع criticals، armor، combos)
 *  - الـ Parry والـ Counter
 *  - المقذوفات (Projectiles)
 *  - قدرات الذاكرة (Memory Abilities)
 *  - أضرار البيئة
 *  - الـ Combo system
 * ══════════════════════════════════════════════════════════════════════════
 */

// ─────────────────────────────────────────────────────────────────────────────
// أنواع الهجمات
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AttackType — نوع الهجوم يحدد damage و range و animation.
 */
enum class AttackType {
    LIGHT_ATTACK,      // ضربة خفيفة — سريعة، ضرر منخفض
    HEAVY_ATTACK,      // ضربة ثقيلة — بطيئة، ضرر عالٍ
    COMBO_FINISHER,    // نهاية الـ Combo — ضرر مضاعف
    PROJECTILE_GUN,    // طلق من المسدس
    PROJECTILE_SATCHEL,// قذيفة الشنطة (AOE)
    PARRY_COUNTER,     // رد فعل الـ Parry
    MEMORY_PULSE,      // نبضة الذاكرة (AOE دفع)
    ECHO_RECALL,       // استدعاء الصدى (ظل يهاجم)
    MASK_SHARD_BLAST,  // انفجار شظية القناع
    BORROWED_NAMES,    // مهارة مؤقتة مستعارة
    STOMP,             // قفز والهبوط على عدو
    ENVIRONMENTAL      // ضرر بيئي (حمم، شوك...)
}

/**
 * DamageType — نوع الضرر يحدد المناعات والمقاومات.
 */
enum class DamageType {
    PHYSICAL,   // ضرر جسدي عادي
    FIRE,       // حرق (ضرر مستمر)
    ICE,        // تجميد (يبطئ)
    LIGHTNING,  // صعق (يشل مؤقتاً)
    VOID,       // ظلام — يتجاوز بعض الدروع
    MEMORY,     // ضرر الذاكرة — يستنزف MF
    TRUE        // ضرر حقيقي — لا يتأثر بالدروع
}

/**
 * ProjectileType — نوع المقذوف.
 */
enum class ProjectileType {
    LINEAR,    // خط مستقيم
    HOMING,    // يتتبع العدو
    ARC,       // قوسي (مع الجاذبية)
    BEAM,      // شعاع مستمر
    AREA       // انفجار في منطقة
}

// ─────────────────────────────────────────────────────────────────────────────
// بيانات الهجوم
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Attack — يصف هجوماً واحداً.
 *
 * @param id معرف فريد للهجوم
 * @param type نوع الهجوم
 * @param damageType نوع الضرر
 * @param baseDamage الضرر الأساسي
 * @param hitbox منطقة التأثير
 * @param startupFrames فريمات قبل بدء التأثير
 * @param activeFrames فريمات الهجوم النشط
 * @param recoveryFrames فريمات التعافي بعد الهجوم
 * @param knockbackForce قوة الدفع
 * @param isHeavyKnockback هل يكون الدفع قوياً؟
 * @param stunFrames فريمات الشلل للعدو
 * @param comboCount رقم هذه الضربة في السلسلة
 * @param canParry هل يمكن صده؟
 * @param isTelegraphed هل يُحذّر قبله؟
 */
data class Attack(
    val id: String,
    val type: AttackType,
    val damageType: DamageType = DamageType.PHYSICAL,
    val baseDamage: Int,
    val hitbox: Rect,
    val startupFrames: Int = 4,
    val activeFrames: Int = 6,
    val recoveryFrames: Int = 10,
    val knockbackForce: Float = GameConfig.CombatConfig.KNOCKBACK_BASE,
    val isHeavyKnockback: Boolean = false,
    val stunFrames: Int = 0,
    val comboCount: Int = 1,
    val canParry: Boolean = true,
    val isTelegraphed: Boolean = false,
    val telegraphFrames: Int = 0
) {
    val totalFrames: Int get() = startupFrames + activeFrames + recoveryFrames
}

/**
 * Projectile — مقذوف نشط في العالم.
 *
 * @param id معرف فريد
 * @param ownerId معرف المالك (لاعب أو عدو)
 * @param type نوع المقذوف
 * @param position الموقع الحالي
 * @param velocity السرعة والاتجاه
 * @param damage الضرر عند الإصابة
 * @param damageType نوع الضرر
 * @param hitbox منطقة التأثير
 * @param lifeTimeFrames عمر المقذوف (يُحذف بعده)
 * @param homingTarget هدف التتبع (للـ HOMING)
 * @param homingStrength قوة التتبع
 * @param penetrating هل يخترق الأعداء؟
 * @param aoeRadius نصف قطر الانفجار (للـ AREA)
 */
data class Projectile(
    val id: String,
    val ownerId: String,
    val type: ProjectileType,
    val position: Vector2D,
    val velocity: Vector2D,
    val damage: Int,
    val damageType: DamageType = DamageType.PHYSICAL,
    val hitbox: Rect,
    val lifeTimeFrames: Int,
    val homingTarget: Vector2D? = null,
    val homingStrength: Float = 0.05f,
    val penetrating: Boolean = false,
    val aoeRadius: Float = 0f,
    val framesAlive: Int = 0,
    val hasHitTargets: MutableSet<String> = mutableSetOf()
) {
    val isAlive: Boolean get() = framesAlive < lifeTimeFrames
    val isFromPlayer: Boolean get() = ownerId == "player"
}

// ─────────────────────────────────────────────────────────────────────────────
// نتائج القتال
// ─────────────────────────────────────────────────────────────────────────────

/**
 * DamageResult — نتيجة تطبيق الضرر على هدف.
 */
data class DamageResult(
    val targetId: String,
    val rawDamage: Int,
    val finalDamage: Int,
    val isCritical: Boolean,
    val damageType: DamageType,
    val wasBlocked: Boolean = false,
    val wasParried: Boolean = false,
    val knockbackApplied: Boolean = false,
    val stunApplied: Boolean = false,
    val didKill: Boolean = false
)

/**
 * ParryResult — نتيجة محاولة الـ Parry.
 */
data class ParryResult(
    val success: Boolean,
    val counterDamage: Int = 0,
    val stunFrames: Int = 0,
    val attackId: String = ""
)

/**
 * ComboState — حالة الـ Combo الحالية.
 *
 * @param count عدد الضربات المتتالية
 * @param windowFrames الفريمات المتبقية لمواصلة الـ Combo
 * @param damageMultiplier مضاعف الضرر الحالي
 */
data class ComboState(
    val count: Int = 0,
    val windowFrames: Int = 0,
    val damageMultiplier: Float = 1f
) {
    val isActive: Boolean get() = windowFrames > 0 && count > 0
    val isFinisher: Boolean get() = count >= 3
}

// ─────────────────────────────────────────────────────────────────────────────
// إعدادات القدرة
// ─────────────────────────────────────────────────────────────────────────────

/**
 * AbilityConfig — إعدادات قدرة محددة.
 */
data class AbilityConfig(
    val id: String,
    val attackType: AttackType,
    val baseDamage: Int,
    val aoeRadius: Float = 0f,
    val mfCost: Int = 0,
    val fmGain: Int = 0,
    val cooldownFrames: Int = 0,
    val duration: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// محرك القتال الرئيسي
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CombatEngine — يدير جميع تفاعلات القتال.
 */
object CombatEngine {

    // ─────────────────────────────────────────────────────────────────────
    // حساب الضرر
    // ─────────────────────────────────────────────────────────────────────

    /**
     * calculateDamage — يحسب الضرر النهائي مع الحسابات المعقدة.
     *
     * @param baseDamage الضرر الأساسي
     * @param attackerLevel مستوى المهاجم
     * @param defenderArmor درع المدافع
     * @param damageType نوع الضرر
     * @param comboMultiplier مضاعف الـ Combo
     * @param isCritical هل ضربة حرجة؟
     * @param isWeakness هل هو نقطة ضعف العدو؟
     */
    fun calculateDamage(
        baseDamage: Int,
        attackerLevel: Int = 1,
        defenderArmor: Int = 0,
        damageType: DamageType = DamageType.PHYSICAL,
        comboMultiplier: Float = 1f,
        isCritical: Boolean = false,
        isWeakness: Boolean = false
    ): Int {
        var damage = baseDamage.toFloat()

        // مضاعف المستوى
        damage *= (1f + (attackerLevel - 1) * 0.05f)

        // مضاعف الـ Combo
        damage *= comboMultiplier

        // تخفيض الدرع (لا يؤثر على TRUE damage)
        if (damageType != DamageType.TRUE) {
            val armorReduction = min(
                defenderArmor * GameConfig.CombatConfig.DAMAGE_REDUCTION_PER_ARMOR,
                GameConfig.CombatConfig.MAX_DAMAGE_REDUCTION
            )
            damage *= (1f - armorReduction)
        }

        // مضاعف الضربة الحرجة
        if (isCritical) {
            damage *= GameConfig.CombatConfig.CRITICAL_HIT_MULTIPLIER
        }

        // مضاعف نقطة الضعف
        if (isWeakness) {
            damage *= 1.5f
        }

        // ضرر الماء لا يقل عن 1
        return maxOf(1, damage.toInt())
    }

    /**
     * rollCritical — يحسب إذا كانت الضربة حرجة.
     *
     * @param critChance احتمال الـ Critical (0–1)
     */
    fun rollCritical(critChance: Float = GameConfig.CombatConfig.CRITICAL_HIT_CHANCE_BASE): Boolean {
        return Random.nextFloat() < critChance
    }

    // ─────────────────────────────────────────────────────────────────────
    // كشف التصادم في القتال
    // ─────────────────────────────────────────────────────────────────────

    /**
     * checkAttackHit — يفحص إذا كانت ضربة تصيب هدفاً.
     *
     * @param attack الهجوم
     * @param attackerPosition موقع المهاجم
     * @param facingRight اتجاه المهاجم
     * @param targetHitbox الـ Hitbox الهدف
     */
    fun checkAttackHit(
        attack: Attack,
        attackerPosition: Vector2D,
        facingRight: Boolean,
        targetHitbox: Rect
    ): Boolean {
        // حساب موقع Hitbox الهجوم بناءً على اتجاه المهاجم
        val attackHitbox = getAttackHitbox(attack, attackerPosition, facingRight)
        return attackHitbox.intersects(targetHitbox)
    }

    /**
     * getAttackHitbox — يحسب موقع Hitbox الهجوم في العالم.
     */
    fun getAttackHitbox(
        attack: Attack,
        attackerPosition: Vector2D,
        facingRight: Boolean
    ): Rect {
        return if (facingRight) {
            attack.hitbox.translate(attackerPosition.x, attackerPosition.y)
        } else {
            // عكس الـ Hitbox أفقياً
            val flippedLeft = -attack.hitbox.right
            val flippedRight = -attack.hitbox.left
            Rect(
                left = attackerPosition.x + flippedLeft,
                top = attackerPosition.y + attack.hitbox.top,
                right = attackerPosition.x + flippedRight,
                bottom = attackerPosition.y + attack.hitbox.bottom
            )
        }
    }

    /**
     * checkAoeHit — يفحص الأهداف داخل منطقة AOE.
     *
     * @param center مركز الانفجار
     * @param radius نصف القطر
     * @param targets قائمة الأهداف (id → hitbox)
     */
    fun checkAoeHit(
        center: Vector2D,
        radius: Float,
        targets: Map<String, Rect>
    ): List<String> {
        val radiusSqr = radius * radius
        return targets.entries
            .filter { (_, hitbox) ->
                hitbox.center.sqrDistanceTo(center) <= radiusSqr
            }
            .map { it.key }
    }

    /**
     * checkProjectileHit — يفحص إذا أصاب مقذوف هدفاً.
     */
    fun checkProjectileHit(
        projectile: Projectile,
        targetId: String,
        targetHitbox: Rect
    ): Boolean {
        // لم يصب هذا الهدف من قبل (لمنع الضرر المتكرر)
        if (targetId in projectile.hasHitTargets && !projectile.penetrating) return false
        return projectile.hitbox.intersects(targetHitbox)
    }

    // ─────────────────────────────────────────────────────────────────────
    // الـ Parry والـ Counter
    // ─────────────────────────────────────────────────────────────────────

    /**
     * attemptParry — يحاول الـ Parry على هجوم وارد.
     *
     * @param parryFramesRemaining الفريمات المتبقية في نافذة الـ Parry
     * @param attack الهجوم الوارد
     * @param attackerLevel مستوى المهاجم (لحساب Counter)
     */
    fun attemptParry(
        parryFramesRemaining: Int,
        attack: Attack,
        attackerLevel: Int = 1
    ): ParryResult {
        // لا يمكن Parry بعض الهجمات
        if (!attack.canParry) {
            return ParryResult(success = false, attackId = attack.id)
        }

        // نافذة الـ Parry ليست مفتوحة
        if (parryFramesRemaining <= 0) {
            return ParryResult(success = false, attackId = attack.id)
        }

        // نجح الـ Parry — احسب Counter damage
        val counterDamage = calculateDamage(
            baseDamage = attack.baseDamage,
            attackerLevel = attackerLevel,
            comboMultiplier = 2f
        )

        // إطلاق حدث
        EventBus.emit(
            GameEvent.ParrySuccess(
                enemyId = "unknown",
                counterDamage = counterDamage
            )
        )

        return ParryResult(
            success = true,
            counterDamage = counterDamage,
            stunFrames = 30f,
            attackId = attack.id
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // الـ Combo System
    // ─────────────────────────────────────────────────────────────────────

    /**
     * updateCombo — يحدث حالة الـ Combo بعد ضربة ناجحة.
     *
     * @param currentCombo الحالة الحالية للـ Combo
     * @param hitLanded هل أصابت الضربة؟
     */
    fun updateCombo(currentCombo: ComboState, hitLanded: Boolean): ComboState {
        return if (hitLanded) {
            val newCount = currentCombo.count + 1
            val newMultiplier = 1f + (newCount - 1) *
                    GameConfig.CombatConfig.COMBO_DAMAGE_MULTIPLIER_PER_HIT

            val state = ComboState(
                count = newCount.coerceAtMost(3),
                windowFrames = GameConfig.CombatConfig.COMBO_WINDOW_FRAMES,
                damageMultiplier = newMultiplier
            )

            // إطلاق حدث تغيير الـ Combo
            EventBus.emit(GameEvent.ComboCountChanged(state.count))

            state
        } else {
            currentCombo
        }
    }

    /**
     * tickCombo — يخصم فريم من نافذة الـ Combo.
     * إذا انتهت النافذة — يُعاد ضبط الـ Combo.
     */
    fun tickCombo(currentCombo: ComboState): ComboState {
        if (!currentCombo.isActive) return currentCombo

        val newFrames = currentCombo.windowFrames - 1
        return if (newFrames <= 0) {
            // انتهت النافذة — reset
            ComboState()
        } else {
            currentCombo.copy(windowFrames = newFrames)
        }
    }

    /**
     * getComboAttackType — يحدد نوع الهجوم بناءً على رقم الـ Combo.
     */
    fun getComboAttackType(comboCount: Int): AttackType {
        return when {
            comboCount >= 3 -> AttackType.COMBO_FINISHER
            comboCount % 2 == 0 -> AttackType.HEAVY_ATTACK
            else -> AttackType.LIGHT_ATTACK
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // المقذوفات
    // ─────────────────────────────────────────────────────────────────────

    /**
     * createProjectile — ينشئ مقذوفاً جديداً.
     *
     * @param ownerId المالك
     * @param type نوع المقذوف
     * @param startPosition موقع البداية
     * @param direction اتجاه الحركة (normalized)
     * @param speed سرعة المقذوف
     * @param damage الضرر
     * @param homingTarget هدف التتبع (للـ HOMING)
     */
    fun createProjectile(
        ownerId: String,
        type: ProjectileType,
        startPosition: Vector2D,
        direction: Vector2D,
        speed: Float,
        damage: Int,
        damageType: DamageType = DamageType.PHYSICAL,
        lifeTimeFrames: Int = 120,
        homingTarget: Vector2D? = null,
        aoeRadius: Float = 0f,
        penetrating: Boolean = false
    ): Projectile {
        val velocity = direction.normalized * speed

        val hitboxSize = when (type) {
            ProjectileType.BEAM -> 8f
            ProjectileType.AREA -> aoeRadius
            else -> 12f
        }

        return Projectile(
            id = "proj_${System.currentTimeMillis()}_${ownerId}",
            ownerId = ownerId,
            type = type,
            position = startPosition,
            velocity = velocity,
            damage = damage,
            damageType = damageType,
            hitbox = Rect.fromCenter(
                startPosition.x, startPosition.y,
                hitboxSize, hitboxSize
            ),
            lifeTimeFrames = lifeTimeFrames,
            homingTarget = homingTarget,
            aoeRadius = aoeRadius,
            penetrating = penetrating
        )
    }

    /**
     * updateProjectile — يحدث حالة مقذوف لفريم واحد.
     *
     * @param projectile المقذوف
     * @param deltaTime الفريم الزمني
     * @param targetPosition موقع الهدف الحالي (للـ HOMING)
     * @param platforms المنصات (للاصطدام)
     */
    fun updateProjectile(
        projectile: Projectile,
        deltaTime: Float,
        targetPosition: Vector2D? = null,
        platforms: List<Platform> = emptyList()
    ): Projectile? {
        if (!projectile.isAlive) return null

        var velocity = projectile.velocity

        // تعديل اتجاه المقذوف الذكي
        if (projectile.type == ProjectileType.HOMING && targetPosition != null) {
            val toTarget = (targetPosition - projectile.position).normalized
            velocity = velocity.lerp(toTarget * velocity.magnitude, projectile.homingStrength)
        }

        // إضافة الجاذبية للمقذوف القوسي
        if (projectile.type == ProjectileType.ARC) {
            velocity = velocity.copy(y = velocity.y + GameConfig.PhysicsConfig.GRAVITY * 0.3f * deltaTime)
        }

        val newPosition = projectile.position + velocity * deltaTime

        // فحص اصطدام بالمنصات
        val hitPlatform = platforms.any { platform ->
            platform.type == PlatformType.SOLID &&
                    platform.bounds.contains(newPosition)
        }

        if (hitPlatform) return null  // المقذوف يُدمر عند اصطدامه

        return projectile.copy(
            position = newPosition,
            velocity = velocity,
            framesAlive = projectile.framesAlive + 1,
            hitbox = Rect.fromCenter(newPosition.x, newPosition.y, 12f, 12f)
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // قدرات الذاكرة
    // ─────────────────────────────────────────────────────────────────────

    /**
     * executeMemoryPulse — ينفذ نبضة الذاكرة (AOE دفع).
     *
     * @param casterPosition موقع المُطلق
     * @param targets الأهداف القريبة (id → position)
     * @param level مستوى القدرة (1 = صغير، 2 = كبير)
     */
    fun executeMemoryPulse(
        casterPosition: Vector2D,
        targets: Map<String, Vector2D>,
        level: Int = 1
    ): Map<String, Vector2D> {
        val radius = if (level == 1) 150f else 250f
        val pushForce = if (level == 1) 400f else 600f

        val knockbackResults = mutableMapOf<String, Vector2D>()

        for ((id, position) in targets) {
            val distance = position.distanceTo(casterPosition)
            if (distance <= radius) {
                val direction = (position - casterPosition).normalized
                val forceFalloff = 1f - (distance / radius)
                knockbackResults[id] = direction * pushForce * forceFalloff
            }
        }

        return knockbackResults
    }

    /**
     * executeMaskShardBlast — ينفذ انفجار شظية القناع.
     *
     * @param casterPosition موقع المُطلق
     * @param facingRight اتجاه المُطلق
     * @param targets الأهداف
     */
    fun executeMaskShardBlast(
        casterPosition: Vector2D,
        facingRight: Boolean,
        targets: Map<String, Pair<Vector2D, Rect>>
    ): List<DamageResult> {
        val blastRect = if (facingRight) {
            Rect(
                casterPosition.x,
                casterPosition.y - 30f,
                casterPosition.x + 200f,
                casterPosition.y + 30f
            )
        } else {
            Rect(
                casterPosition.x - 200f,
                casterPosition.y - 30f,
                casterPosition.x,
                casterPosition.y + 30f
            )
        }

        val results = mutableListOf<DamageResult>()

        for ((id, data) in targets) {
            val (position, hitbox) = data
            if (blastRect.intersects(hitbox)) {
                val isCrit = rollCritical()
                val damage = calculateDamage(
                    baseDamage = 45,
                    damageType = DamageType.VOID,
                    isCritical = isCrit
                )
                results.add(
                    DamageResult(
                        targetId = id,
                        rawDamage = 45,
                        finalDamage = damage,
                        isCritical = isCrit,
                        damageType = DamageType.VOID,
                        knockbackApplied = true
                    )
                )
            }
        }

        return results
    }

    // ─────────────────────────────────────────────────────────────────────
    // الأضرار البيئية
    // ─────────────────────────────────────────────────────────────────────

    /**
     * calculateEnvironmentalDamage — يحسب الضرر البيئي.
     *
     * @param hazardType نوع الخطر
     * @param deltaTime الفريم الزمني
     * @param playerArmor درع اللاعب
     */
    fun calculateEnvironmentalDamage(
        hazardType: AttackType,
        deltaTime: Float,
        playerArmor: Int = 0
    ): Int {
        val baseDamage = when (hazardType) {
            AttackType.ENVIRONMENTAL -> GameConfig.CombatConfig.LAVA_DAMAGE_PER_SECOND
            else -> 0
        }

        return calculateDamage(
            baseDamage = (baseDamage * deltaTime).toInt(),
            defenderArmor = playerArmor,
            damageType = DamageType.FIRE
        )
    }

    /**
     * calculateFallDamage — يحسب ضرر السقوط.
     *
     * @param fallDistance المسافة التي سقط منها
     */
    fun calculateFallDamage(fallDistance: Float): Int {
        if (fallDistance <= GameConfig.CombatConfig.FALL_DAMAGE_MIN_HEIGHT) return 0

        val excessHeight = fallDistance - GameConfig.CombatConfig.FALL_DAMAGE_MIN_HEIGHT
        val damage = (excessHeight / 100f) * GameConfig.CombatConfig.FALL_DAMAGE_PER_100PX

        return damage.toInt().coerceAtLeast(0)
    }

    // ─────────────────────────────────────────────────────────────────────
    // هجمات اللاعب المُعرَّفة
    // ─────────────────────────────────────────────────────────────────────

    /**
     * getPlayerAttack — يُعيد تعريف هجوم اللاعب بناءً على النوع والمستوى.
     *
     * @param type نوع الهجوم
     * @param playerLevel مستوى اللاعب
     * @param facingRight اتجاه اللاعب
     * @param comboCount رقم الـ Combo
     */
    fun getPlayerAttack(
        type: AttackType,
        playerLevel: Int,
        facingRight: Boolean,
        comboCount: Int = 1
    ): Attack {
        val levelBonus = (playerLevel - 1) * 2

        return when (type) {
            AttackType.LIGHT_ATTACK -> Attack(
                id = "player_light_$comboCount",
                type = type,
                baseDamage = 10 + levelBonus,
                hitbox = Rect(
                    left = if (facingRight) 10f else -65f,
                    top = -10f,
                    right = if (facingRight) 65f else -10f,
                    bottom = 30f
                ),
                startupFrames = 3,
                activeFrames = 5,
                recoveryFrames = 8,
                comboCount = comboCount
            )

            AttackType.HEAVY_ATTACK -> Attack(
                id = "player_heavy_$comboCount",
                type = type,
                baseDamage = 25 + levelBonus * 2,
                hitbox = Rect(
                    left = if (facingRight) 8f else -80f,
                    top = -20f,
                    right = if (facingRight) 80f else -8f,
                    bottom = 40f
                ),
                startupFrames = 10,
                activeFrames = 8,
                recoveryFrames = 18,
                knockbackForce = GameConfig.CombatConfig.KNOCKBACK_HEAVY,
                isHeavyKnockback = true,
                comboCount = comboCount
            )

            AttackType.COMBO_FINISHER -> Attack(
                id = "player_finisher",
                type = type,
                baseDamage = (25f * 1.8f + levelBonus * 3).toInt(),
                hitbox = Rect(
                    left = if (facingRight) 5f else -100f,
                    top = -30f,
                    right = if (facingRight) 100f else -5f,
                    bottom = 50f
                ),
                startupFrames = 8,
                activeFrames = 10,
                recoveryFrames = 22,
                knockbackForce = GameConfig.CombatConfig.KNOCKBACK_HEAVY * 1.5f,
                isHeavyKnockback = true,
                stunFrames = 30,
                comboCount = 3
            )

            AttackType.STOMP -> Attack(
                id = "player_stomp",
                type = type,
                baseDamage = 25 + levelBonus,
                hitbox = Rect(-20f, 0f, 20f, 20f),  // أسفل اللاعب
                startupFrames = 0,
                activeFrames = 3,
                recoveryFrames = 5,
                knockbackForce = GameConfig.CombatConfig.KNOCKBACK_BASE,
                isHeavyKnockback = false
            )

            else -> Attack(
                id = "player_${type.name.lowercase()}",
                type = type,
                baseDamage = 10,
                hitbox = Rect(0f, 0f, 60f, 40f),
                startupFrames = 4,
                activeFrames = 6,
                recoveryFrames = 10
            )
        }
    }
}