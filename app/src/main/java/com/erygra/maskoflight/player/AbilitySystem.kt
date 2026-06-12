package com.erygra.maskoflight.player

import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.engine.CombatEngine
import com.erygra.maskoflight.engine.ParticleEngine
import com.erygra.maskoflight.engine.PhysicsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * AbilitySystem.kt — نظام القدرات الشامل للعبة "قِنَاعُ النُّور"
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * يدير جميع قدرات اللاعب من القدرات الأساسية (XP-based) إلى القدرات القوية (MF-based).
 * يتضمن:
 * - تعريف القدرات وخصائصها
 * - تنفيذ القدرات الفعلي
 * - حساب التأثيرات (Damage, AOE, Knockback, Status Effects)
 * - نظام الترقيات
 * - نظام التركيبات (Combos)
 * - تكامل مع PlayerStateManager, PhysicsEngine, CombatEngine, ParticleEngine
 * 
 * @author Erygra Universe Development Team
 * @version 2.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Ability Data Classes
// ═══════════════════════════════════════════════════════════════════════════

/**
 * نوع القدرة
 */
enum class AbilityType {
    // قدرات الحركة الأساسية (XP-based)
    LEDGE_GRAB,
    ROPE_SWING,
    WALL_JUMP,
    BURST_LEAP,
    DASH,
    DODGE_ROLL,
    DOUBLE_JUMP,
    AIR_DASH,
    GROUND_SLAM,
    
    // قدرات القتال الأساسية (XP-based)
    PRECISION_STRIKE,
    PARRY_COUNTER,
    CHARGED_HEAVY,
    SPIN_ATTACK,
    SHIELD_BASH,
    
    // قدرات القناع القوية (MF-based)
    MEMORY_PULSE_SMALL,
    MEMORY_PULSE_LARGE,
    ECHO_RECALL,
    MASK_SHARD_BLAST,
    BORROWED_NAMES,
    MEMORY_RESTORATION,
    SHADOW_STEP,
    TIME_ECHO,
    VOID_SHIFT,
    
    // قدرات استكشاف خاصة
    WATER_BREATHING,
    CLIMB
}

/**
 * فئة القدرة (للتصنيف)
 */
enum class AbilityCategory {
    MOVEMENT,    // حركة
    COMBAT,      // قتال
    UTILITY,     // خدمات
    ULTIMATE     // قدرات نهائية
}

/**
 * نوع تكلفة القدرة
 */
enum class AbilityCostType {
    NONE,        // مجانية
    ENERGY,      // طاقة
    MF,          // Memory Fragments
    COOLDOWN     // فقط وقت انتظار
}

/**
 * حالة القدرة
 */
enum class AbilityState {
    LOCKED,      // مقفلة
    UNLOCKED,    // مفتوحة
    ACTIVE,      // نشطة حالياً
    COOLDOWN     // في وقت الانتظار
}

/**
 * فروع المهارات
 */
enum class SkillBranch(val nameAr: String) {
    RADIANCE("المشع"),
    MEMORY("الذاكرة"),
    VOID("الفراغ")
}

/**
 * تعريف القدرة
 */
data class Ability(
    val type: AbilityType,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    val category: AbilityCategory,
    val costType: AbilityCostType,
    
    // التكاليف
    val energyCost: Int = 0,
    val mfCost: Int = 0,
    val fmGenerated: Int = 0,  // كم FM يولد عند الاستخدام
    
    // الخصائص القتالية
    val baseDamage: Int = 0,
    val damageScaling: Float = 1.0f,  // مضاعف الضرر حسب المستوى
    val range: Float = 0f,
    val aoeRadius: Float = 0f,
    val knockbackForce: Float = 0f,
    
    // التوقيتات
    val cooldownMs: Long = 0L,
    val durationMs: Long = 0L,
    val castTimeMs: Long = 0L,
    
    // الترقيات
    val level: Int = 1,
    val maxLevel: Int = 5,
    val unlockXP: Int = 0,
    val unlockMF: Int = 0,
    
    // التأثيرات
    val statusEffects: List<EffectType> = emptyList(),
    val particleEffect: String = "",
    val soundEffect: String = "",
    
    // الشروط
    val requiresGrounded: Boolean = false,
    val requiresAirborne: Boolean = false,
    val requiresWallContact: Boolean = false,
    val canCancelInto: List<AbilityType> = emptyList(),
    
    // معلومات إضافية
    val iconResource: String = "",
    val animationState: String = ""
)

/**
 * حالة تنفيذ القدرة
 */
data class AbilityExecution(
    val ability: Ability,
    val startTime: Long,
    val endTime: Long,
    val castComplete: Boolean = false,
    val hitEnemies: MutableSet<String> = mutableSetOf(),  // لمنع الضربات المتكررة
    val spawnedEntities: MutableList<String> = mutableListOf()  // Echo clones, projectiles
)

/**
 * نتيجة محاولة استخدام القدرة
 */
sealed class AbilityResult {
    data class Success(val execution: AbilityExecution) : AbilityResult()
    data class Failure(val reason: AbilityFailureReason) : AbilityResult()
}

/**
 * أسباب فشل استخدام القدرة
 */
enum class AbilityFailureReason {
    LOCKED,              // القدرة مقفلة
    ON_COOLDOWN,         // في وقت الانتظار
    INSUFFICIENT_ENERGY, // طاقة غير كافية
    INSUFFICIENT_MF,     // MF غير كافي
    WRONG_STATE,         // حالة خاطئة (مثلاً في الهواء بينما تحتاج أرض)
    ALREADY_ACTIVE,      // القدرة نشطة بالفعل
    INTERRUPTED          // تم المقاطعة
}

/**
 * تركيب القدرات (Combo)
 */
data class AbilityCombo(
    val name: String,
    val nameArabic: String,
    val abilities: List<AbilityType>,
    val timeWindowMs: Long,  // الوقت المسموح بين القدرات
    val bonusDamage: Int = 0,
    val bonusEffects: List<EffectType> = emptyList(),
    val unlockCondition: String = ""
)

/**
 * ترقية القدرة
 */
data class AbilityUpgrade(
    val abilityType: AbilityType,
    val level: Int,
    val xpCost: Int = 0,
    val mfCost: Int = 0,
    val damageIncrease: Int = 0,
    val cooldownReduction: Long = 0L,
    val energyCostReduction: Int = 0,
    val rangeIncrease: Float = 0f,
    val knockbackIncrease: Float = 0f,
    val durationIncrease: Long = 0L,
    val newEffects: List<EffectType> = emptyList(),
    val description: String = "",
    val descriptionArabic: String = ""
)

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Ability Database
// ═══════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات القدرات
 */
object AbilityDatabase {
    
    /**
     * جميع القدرات المتاحة في اللعبة
     */
    val allAbilities = mapOf(
        
        // ═══════════════════════════════════════════════════════════════
        // قدرات الحركة الأساسية
        // ═══════════════════════════════════════════════════════════════
        
        AbilityType.LEDGE_GRAB to Ability(
            type = AbilityType.LEDGE_GRAB,
            name = "Ledge Grab & Climb",
            nameArabic = "تسلق الحواف",
            description = "Grab and climb ledges and walls",
            descriptionArabic = "إمساك وتسلق الحواف والجدران",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.NONE,
            unlockXP = 100,
            iconResource = "ability_ledge_grab",
            animationState = "climb"
        ),
        
        AbilityType.ROPE_SWING to Ability(
            type = AbilityType.ROPE_SWING,
            name = "Rope Swing",
            nameArabic = "تأرجح الحبال",
            description = "Swing across gaps using ropes",
            descriptionArabic = "التأرجح عبر الفجوات باستخدام الحبال",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.NONE,
            unlockXP = 150,
            iconResource = "ability_rope_swing",
            animationState = "rope_swing"
        ),
        
        AbilityType.WALL_JUMP to Ability(
            type = AbilityType.WALL_JUMP,
            name = "Wall Jump",
            nameArabic = "قفزة الجدار",
            description = "Jump off walls to reach higher platforms",
            descriptionArabic = "القفز من الجدران للوصول لمنصات أعلى",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.ENERGY,
            energyCost = 10,
            unlockXP = 300,
            requiresWallContact = true,
            requiresAirborne = true,
            iconResource = "ability_wall_jump",
            animationState = "wall_jump"
        ),
        
        AbilityType.BURST_LEAP to Ability(
            type = AbilityType.BURST_LEAP,
            name = "Burst Leap",
            nameArabic = "قفزة الانفجار",
            description = "Powerful leap with extended height",
            descriptionArabic = "قفزة قوية بارتفاع ممتد",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.ENERGY,
            energyCost = 15,
            unlockXP = 300,
            cooldownMs = 3000L,
            requiresGrounded = true,
            iconResource = "ability_burst_leap",
            animationState = "burst_leap",
            particleEffect = "burst_leap_particles"
        ),
        
        AbilityType.DASH to Ability(
            type = AbilityType.DASH,
            name = "Dash",
            nameArabic = "اندفاع",
            description = "Quick horizontal dash with brief invulnerability",
            descriptionArabic = "اندفاع أفقي سريع مع حصانة مؤقتة",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.ENERGY,
            energyCost = 15,
            unlockXP = 200,
            cooldownMs = 2000L,
            durationMs = 200L,
            statusEffects = listOf(EffectType.INVULNERABLE),
            iconResource = "ability_dash",
            animationState = "dash",
            particleEffect = "dash_trail",
            soundEffect = "sfx_dash"
        ),
        
        AbilityType.DODGE_ROLL to Ability(
            type = AbilityType.DODGE_ROLL,
            name = "Dodge Roll",
            nameArabic = "لفة التفادي",
            description = "Roll to evade attacks with i-frames",
            descriptionArabic = "لفة للتفادي مع إطارات حصانة",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.ENERGY,
            energyCost = 10,
            unlockXP = 200,
            cooldownMs = 1500L,
            durationMs = 400L,
            statusEffects = listOf(EffectType.INVULNERABLE),
            iconResource = "ability_dodge_roll",
            animationState = "dodge_roll",
            soundEffect = "sfx_dodge"
        ),
        
        AbilityType.DOUBLE_JUMP to Ability(
            type = AbilityType.DOUBLE_JUMP,
            name = "Double Jump",
            nameArabic = "قفزة مزدوجة",
            description = "Additional jump while airborne",
            descriptionArabic = "قفزة إضافية في الهواء",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.ENERGY,
            energyCost = 12,
            unlockXP = 250,
            requiresAirborne = true,
            iconResource = "ability_double_jump",
            animationState = "double_jump",
            particleEffect = "double_jump_burst"
        ),
        
        AbilityType.AIR_DASH to Ability(
            type = AbilityType.AIR_DASH,
            name = "Air Dash",
            nameArabic = "اندفاع هوائي",
            description = "Dash while airborne in any direction",
            descriptionArabic = "اندفاع في الهواء بأي اتجاه",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.ENERGY,
            energyCost = 18,
            unlockXP = 400,
            cooldownMs = 3000L,
            requiresAirborne = true,
            iconResource = "ability_air_dash",
            animationState = "air_dash",
            particleEffect = "air_dash_trail"
        ),
        
        AbilityType.GROUND_SLAM to Ability(
            type = AbilityType.GROUND_SLAM,
            name = "Ground Slam",
            nameArabic = "ضربة الأرض",
            description = "Slam down from the air with AOE damage",
            descriptionArabic = "الهبوط بقوة من الهواء مع ضرر منطقة",
            category = AbilityCategory.COMBAT,
            costType = AbilityCostType.ENERGY,
            energyCost = 20,
            baseDamage = 40,
            aoeRadius = 3f,
            knockbackForce = 400f,
            unlockXP = 350,
            cooldownMs = 4000L,
            requiresAirborne = true,
            iconResource = "ability_ground_slam",
            animationState = "ground_slam",
            particleEffect = "ground_slam_shockwave",
            soundEffect = "sfx_ground_slam"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // قدرات القتال الأساسية
        // ═══════════════════════════════════════════════════════════════
        
        AbilityType.PRECISION_STRIKE to Ability(
            type = AbilityType.PRECISION_STRIKE,
            name = "Precision Strike",
            nameArabic = "ضربة دقيقة",
            description = "Fast, accurate strike with bonus critical chance",
            descriptionArabic = "ضربة سريعة دقيقة مع فرصة إصابة حرجة",
            category = AbilityCategory.COMBAT,
            costType = AbilityCostType.ENERGY,
            energyCost = 15,
            baseDamage = 25,
            range = 1.5f,
            unlockXP = 150,
            cooldownMs = 1000L,
            castTimeMs = 200L,
            canCancelInto = listOf(AbilityType.DASH, AbilityType.DODGE_ROLL),
            iconResource = "ability_precision_strike",
            animationState = "precision_strike",
            soundEffect = "sfx_precision_strike"
        ),
        
        AbilityType.PARRY_COUNTER to Ability(
            type = AbilityType.PARRY_COUNTER,
            name = "Parry & Counter",
            nameArabic = "صد ورد",
            description = "Deflect attack and counter with bonus damage",
            descriptionArabic = "صد الهجوم والرد بضرر إضافي",
            category = AbilityCategory.COMBAT,
            costType = AbilityCostType.ENERGY,
            energyCost = 20,
            baseDamage = 35,
            range = 1.2f,
            unlockXP = 300,
            cooldownMs = 2000L,
            durationMs = 300L,  // نافذة الصد
            statusEffects = listOf(EffectType.PARRY_WINDOW),
            iconResource = "ability_parry",
            animationState = "parry",
            soundEffect = "sfx_parry"
        ),
        
        AbilityType.CHARGED_HEAVY to Ability(
            type = AbilityType.CHARGED_HEAVY,
            name = "Charged Heavy Attack",
            nameArabic = "هجوم ثقيل محمّل",
            description = "Charge up for devastating damage",
            descriptionArabic = "شحن الهجوم لضرر مدمر",
            category = AbilityCategory.COMBAT,
            costType = AbilityCostType.ENERGY,
            energyCost = 30,
            baseDamage = 60,
            damageScaling = 1.5f,
            range = 2f,
            knockbackForce = 300f,
            unlockXP = 250,
            cooldownMs = 3000L,
            castTimeMs = 1000L,  // وقت الشحن
            iconResource = "ability_charged_heavy",
            animationState = "charged_heavy",
            particleEffect = "charge_glow",
            soundEffect = "sfx_charged_heavy"
        ),
        
        AbilityType.SPIN_ATTACK to Ability(
            type = AbilityType.SPIN_ATTACK,
            name = "Spin Attack",
            nameArabic = "هجوم دوار",
            description = "Spinning attack hitting all nearby enemies",
            descriptionArabic = "هجوم دوار يضرب جميع الأعداء القريبين",
            category = AbilityCategory.COMBAT,
            costType = AbilityCostType.ENERGY,
            energyCost = 25,
            baseDamage = 30,
            aoeRadius = 2.5f,
            unlockXP = 350,
            cooldownMs = 4000L,
            durationMs = 600L,
            iconResource = "ability_spin_attack",
            animationState = "spin_attack",
            particleEffect = "spin_trail",
            soundEffect = "sfx_spin_attack"
        ),
        
        AbilityType.SHIELD_BASH to Ability(
            type = AbilityType.SHIELD_BASH,
            name = "Shield Bash",
            nameArabic = "ضربة الدرع",
            description = "Stun enemies with shield bash",
            descriptionArabic = "صعق الأعداء بضربة الدرع",
            category = AbilityCategory.COMBAT,
            costType = AbilityCostType.ENERGY,
            energyCost = 20,
            baseDamage = 20,
            range = 1.5f,
            knockbackForce = 250f,
            unlockXP = 300,
            cooldownMs = 5000L,
            statusEffects = listOf(EffectType.STUN),
            iconResource = "ability_shield_bash",
            animationState = "shield_bash",
            soundEffect = "sfx_shield_bash"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // قدرات القناع القوية (MF-based)
        // ═══════════════════════════════════════════════════════════════
        
        AbilityType.MEMORY_PULSE_SMALL to Ability(
            type = AbilityType.MEMORY_PULSE_SMALL,
            name = "Memory Pulse (Small)",
            nameArabic = "نبضة الذاكرة (صغيرة)",
            description = "AOE pulse that damages and reveals secrets",
            descriptionArabic = "نبضة منطقة تضر وتكشف الأسرار",
            category = AbilityCategory.UTILITY,
            costType = AbilityCostType.MF,
            mfCost = 1,
            fmGenerated = 1,
            baseDamage = 30,
            aoeRadius = 4f,
            knockbackForce = 200f,
            unlockMF = 5,
            cooldownMs = 8000L,
            castTimeMs = 500L,
            iconResource = "ability_memory_pulse_small",
            animationState = "memory_pulse",
            particleEffect = "memory_pulse_wave_small",
            soundEffect = "sfx_memory_pulse"
        ),
        
        AbilityType.MEMORY_PULSE_LARGE to Ability(
            type = AbilityType.MEMORY_PULSE_LARGE,
            name = "Memory Pulse (Large)",
            nameArabic = "نبضة الذاكرة (كبيرة)",
            description = "Massive AOE pulse with extended range",
            descriptionArabic = "نبضة منطقة ضخمة بمدى ممتد",
            category = AbilityCategory.UTILITY,
            costType = AbilityCostType.MF,
            mfCost = 3,
            fmGenerated = 3,
            baseDamage = 50,
            aoeRadius = 8f,
            knockbackForce = 350f,
            unlockMF = 15,
            cooldownMs = 15000L,
            castTimeMs = 1000L,
            iconResource = "ability_memory_pulse_large",
            animationState = "memory_pulse_large",
            particleEffect = "memory_pulse_wave_large",
            soundEffect = "sfx_memory_pulse_large"
        ),
        
        AbilityType.ECHO_RECALL to Ability(
            type = AbilityType.ECHO_RECALL,
            name = "Echo Recall",
            nameArabic = "استدعاء الصدى",
            description = "Summon a shadow clone to fight alongside you",
            descriptionArabic = "استدعاء نسخة ظلية للقتال معك",
            category = AbilityCategory.COMBAT,
            costType = AbilityCostType.MF,
            mfCost = 2,
            fmGenerated = 2,
            baseDamage = 40,  // ضرر النسخة
            unlockMF = 10,
            cooldownMs = 20000L,
            durationMs = 10000L,  // مدة النسخة
            castTimeMs = 800L,
            iconResource = "ability_echo_recall",
            animationState = "echo_recall",
            particleEffect = "echo_spawn",
            soundEffect = "sfx_echo_recall"
        ),
        
        AbilityType.MASK_SHARD_BLAST to Ability(
            type = AbilityType.MASK_SHARD_BLAST,
            name = "Mask Shard Blast",
            nameArabic = "انفجار شظية القناع",
            description = "Forward projectile that breaks barriers",
            descriptionArabic = "مقذوف أمامي يكسر الحواجز",
            category = AbilityCategory.COMBAT,
            costType = AbilityCostType.MF,
            mfCost = 3,
            fmGenerated = 4,
            baseDamage = 70,
            range = 10f,
            knockbackForce = 400f,
            unlockMF = 20,
            cooldownMs = 12000L,
            castTimeMs = 600L,
            iconResource = "ability_mask_shard",
            animationState = "mask_shard_blast",
            particleEffect = "mask_shard_projectile",
            soundEffect = "sfx_mask_shard"
        ),
        
        AbilityType.BORROWED_NAMES to Ability(
            type = AbilityType.BORROWED_NAMES,
            name = "Borrowed Names",
            nameArabic = "أسماء مستعارة",
            description = "Temporarily copy a skill from a saved name",
            descriptionArabic = "نسخ مهارة مؤقتة من اسم محفوظ",
            category = AbilityCategory.UTILITY,
            costType = AbilityCostType.MF,
            mfCost = 2,
            fmGenerated = 1,
            unlockMF = 12,
            cooldownMs = 30000L,
            durationMs = 15000L,
            castTimeMs = 1000L,
            iconResource = "ability_borrowed_names",
            animationState = "borrowed_names",
            particleEffect = "name_transfer",
            soundEffect = "sfx_borrowed_names"
        ),
        
        AbilityType.MEMORY_RESTORATION to Ability(
            type = AbilityType.MEMORY_RESTORATION,
            name = "Memory Restoration",
            nameArabic = "استعادة الذاكرة",
            description = "Restore a complete memory (narrative ability)",
            descriptionArabic = "استعادة ذكرى كاملة (قدرة سردية)",
            category = AbilityCategory.ULTIMATE,
            costType = AbilityCostType.MF,
            mfCost = 5,
            fmGenerated = 5,
            unlockMF = 25,
            cooldownMs = 60000L,
            castTimeMs = 3000L,
            iconResource = "ability_memory_restoration",
            animationState = "memory_restoration",
            particleEffect = "memory_restoration_aura",
            soundEffect = "sfx_memory_restoration"
        ),
        
        AbilityType.SHADOW_STEP to Ability(
            type = AbilityType.SHADOW_STEP,
            name = "Shadow Step",
            nameArabic = "خطوة الظل",
            description = "Short-range teleport through shadows",
            descriptionArabic = "انتقال قصير المدى عبر الظلال",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.MF,
            mfCost = 1,
            fmGenerated = 1,
            range = 5f,
            unlockMF = 8,
            cooldownMs = 6000L,
            castTimeMs = 200L,
            statusEffects = listOf(EffectType.INVULNERABLE),
            iconResource = "ability_shadow_step",
            animationState = "shadow_step",
            particleEffect = "shadow_teleport",
            soundEffect = "sfx_shadow_step"
        ),
        
        AbilityType.TIME_ECHO to Ability(
            type = AbilityType.TIME_ECHO,
            name = "Time Echo",
            nameArabic = "صدى الزمن",
            description = "Rewind 3 seconds to avoid fatal damage",
            descriptionArabic = "إرجاع 3 ثواني لتجنب ضرر قاتل",
            category = AbilityCategory.ULTIMATE,
            costType = AbilityCostType.MF,
            mfCost = 4,
            fmGenerated = 3,
            unlockMF = 30,
            cooldownMs = 90000L,
            durationMs = 3000L,
            iconResource = "ability_time_echo",
            animationState = "time_echo",
            particleEffect = "time_rewind",
            soundEffect = "sfx_time_echo"
        ),
        
        AbilityType.VOID_SHIFT to Ability(
            type = AbilityType.VOID_SHIFT,
            name = "Void Shift",
            nameArabic = "تحول الفراغ",
            description = "Become intangible and pass through obstacles",
            descriptionArabic = "تصبح غير ملموس وتمر عبر العوائق",
            category = AbilityCategory.MOVEMENT,
            costType = AbilityCostType.MF,
            mfCost = 3,
            fmGenerated = 4,
            unlockMF = 35,
            cooldownMs = 25000L,
            durationMs = 5000L,
            castTimeMs = 500L,
            statusEffects = listOf(EffectType.INTANGIBLE),
            iconResource = "ability_void_shift",
            animationState = "void_shift",
            particleEffect = "void_form",
            soundEffect = "sfx_void_shift"
        )
    )
    
    /**
     * الحصول على قدرة بنوعها
     */
    fun getAbility(type: AbilityType): Ability? = allAbilities[type]
    
    /**
     * الحصول على جميع قدرات فئة معينة
     */
    fun getAbilitiesByCategory(category: AbilityCategory): List<Ability> =
        allAbilities.values.filter { it.category == category }
    
    /**
     * الحصول على القدرات القابلة للفتح بـ XP معين
     */
    fun getUnlockableAbilitiesWithXP(xp: Int): List<Ability> =
        allAbilities.values.filter { it.unlockXP > 0 && it.unlockXP <= xp }
    
    /**
     * الحصول على القدرات القابلة للفتح بـ MF معين
     */
    fun getUnlockableAbilitiesWithMF(mf: Int): List<Ability> =
        allAbilities.values.filter { it.unlockMF > 0 && it.unlockMF <= mf }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Ability Combo Database
// ═══════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات تركيبات القدرات
 */
object AbilityComboDatabase {
    
    val allCombos = listOf(
        
        AbilityCombo(
            name = "Shadow Strike",
            nameArabic = "ضربة الظل",
            abilities = listOf(
                AbilityType.DASH,
                AbilityType.PRECISION_STRIKE
            ),
            timeWindowMs = 500L,
            bonusDamage = 15,
            bonusEffects = listOf(EffectType.INCREASED_CRIT_CHANCE)
        ),
        
        AbilityCombo(
            name = "Aerial Assault",
            nameArabic = "هجوم جوي",
            abilities = listOf(
                AbilityType.DOUBLE_JUMP,
                AbilityType.AIR_DASH,
                AbilityType.GROUND_SLAM
            ),
            timeWindowMs = 1000L,
            bonusDamage = 25,
            bonusEffects = listOf(EffectType.AOE_AMPLIFY)
        ),
        
        AbilityCombo(
            name = "Perfect Parry",
            nameArabic = "صد مثالي",
            abilities = listOf(
                AbilityType.PARRY_COUNTER,
                AbilityType.CHARGED_HEAVY
            ),
            timeWindowMs = 300L,
            bonusDamage = 40,
            bonusEffects = listOf(EffectType.ARMOR_BREAK)
        ),
        
        AbilityCombo(
            name = "Echo Barrage",
            nameArabic = "قصف الصدى",
            abilities = listOf(
                AbilityType.ECHO_RECALL,
                AbilityType.SPIN_ATTACK
            ),
            timeWindowMs = 800L,
            bonusDamage = 30,
            bonusEffects = listOf(EffectType.LIFESTEAL)
        ),
        
        AbilityCombo(
            name = "Void Breach",
            nameArabic = "اختراق الفراغ",
            abilities = listOf(
                AbilityType.VOID_SHIFT,
                AbilityType.MASK_SHARD_BLAST
            ),
            timeWindowMs = 1200L,
            bonusDamage = 50,
            bonusEffects = listOf(EffectType.BARRIER_PIERCE)
        ),
        
        AbilityCombo(
            name = "Memory Cascade",
            nameArabic = "تتالي الذاكرة",
            abilities = listOf(
                AbilityType.MEMORY_PULSE_SMALL,
                AbilityType.MEMORY_PULSE_LARGE
            ),
            timeWindowMs = 2000L,
            bonusDamage = 60,
            bonusEffects = listOf(EffectType.REVELATION),
            unlockCondition = "FM < 10"
        ),
        
        AbilityCombo(
            name = "Temporal Strike",
            nameArabic = "ضربة زمنية",
            abilities = listOf(
                AbilityType.TIME_ECHO,
                AbilityType.PRECISION_STRIKE,
                AbilityType.PRECISION_STRIKE,
                AbilityType.PRECISION_STRIKE
            ),
            timeWindowMs = 3000L,
            bonusDamage = 80,
            bonusEffects = listOf(EffectType.TIME_SLOW)
        )
    )
    
    /**
     * البحث عن تركيب بقائمة القدرات
     */
    fun findCombo(recentAbilities: List<AbilityType>): AbilityCombo? {
        return allCombos.find { combo ->
            if (recentAbilities.size < combo.abilities.size) return@find false
            
            val window = recentAbilities.takeLast(combo.abilities.size)
            window == combo.abilities
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Ability Upgrade Database
// ═══════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات ترقيات القدرات
 */
object AbilityUpgradeDatabase {
    
    /**
     * جميع الترقيات المتاحة
     */
    val allUpgrades = mapOf(
        
        // ترقيات Memory Pulse Small
        AbilityType.MEMORY_PULSE_SMALL to listOf(
            AbilityUpgrade(
                abilityType = AbilityType.MEMORY_PULSE_SMALL,
                level = 2,
                xpCost = 500,
                damageIncrease = 10,
                rangeIncrease = 1f,
                description = "Increased damage and range",
                descriptionArabic = "زيادة الضرر والمدى"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.MEMORY_PULSE_SMALL,
                level = 3,
                xpCost = 1000,
                mfCost = 1,
                cooldownReduction = 2000L,
                newEffects = listOf(EffectType.REVELATION),
                description = "Reduced cooldown, reveals hidden items",
                descriptionArabic = "تقليل وقت الانتظار، يكشف العناصر المخفية"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.MEMORY_PULSE_SMALL,
                level = 4,
                xpCost = 2000,
                mfCost = 2,
                damageIncrease = 15,
                knockbackIncrease = 100f,
                description = "Major damage and knockback boost",
                descriptionArabic = "زيادة كبيرة في الضرر والدفع"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.MEMORY_PULSE_SMALL,
                level = 5,
                xpCost = 3500,
                mfCost = 3,
                energyCostReduction = 0,  // يصبح بدون تكلفة طاقة
                newEffects = listOf(EffectType.SLOW),
                description = "No MF cost, slows enemies",
                descriptionArabic = "بدون تكلفة MF، يبطئ الأعداء"
            )
        ),
        
        // ترقيات Echo Recall
        AbilityType.ECHO_RECALL to listOf(
            AbilityUpgrade(
                abilityType = AbilityType.ECHO_RECALL,
                level = 2,
                xpCost = 800,
                durationIncrease = 3000L,
                description = "Echo lasts longer",
                descriptionArabic = "الصدى يستمر لفترة أطول"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.ECHO_RECALL,
                level = 3,
                xpCost = 1500,
                mfCost = 1,
                damageIncrease = 20,
                description = "Echo deals more damage",
                descriptionArabic = "الصدى يسبب ضرر أكبر"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.ECHO_RECALL,
                level = 4,
                xpCost = 2500,
                mfCost = 2,
                description = "Summon 2 echoes instead of 1",
                descriptionArabic = "استدعاء 2 صدى بدلاً من 1"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.ECHO_RECALL,
                level = 5,
                xpCost = 4000,
                mfCost = 3,
                cooldownReduction = 5000L,
                newEffects = listOf(EffectType.LIFESTEAL),
                description = "Reduced cooldown, echo heals you",
                descriptionArabic = "تقليل الانتظار، الصدى يشفيك"
            )
        ),
        
        // ترقيات Dash
        AbilityType.DASH to listOf(
            AbilityUpgrade(
                abilityType = AbilityType.DASH,
                level = 2,
                xpCost = 300,
                energyCostReduction = 3,
                description = "Lower energy cost",
                descriptionArabic = "تكلفة طاقة أقل"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.DASH,
                level = 3,
                xpCost = 600,
                cooldownReduction = 500L,
                description = "Faster cooldown",
                descriptionArabic = "انتظار أسرع"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.DASH,
                level = 4,
                xpCost = 1200,
                durationIncrease = 100L,
                description = "Longer dash distance",
                descriptionArabic = "مسافة اندفاع أطول"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.DASH,
                level = 5,
                xpCost = 2000,
                newEffects = listOf(EffectType.DAMAGE_ON_CONTACT),
                description = "Dash damages enemies on contact",
                descriptionArabic = "الاندفاع يضر الأعداء بالتلامس"
            )
        ),
        
        // ترقيات Ground Slam
        AbilityType.GROUND_SLAM to listOf(
            AbilityUpgrade(
                abilityType = AbilityType.GROUND_SLAM,
                level = 2,
                xpCost = 600,
                damageIncrease = 15,
                description = "Increased damage",
                descriptionArabic = "ضرر متزايد"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.GROUND_SLAM,
                level = 3,
                xpCost = 1200,
                rangeIncrease = 1.5f,
                description = "Larger shockwave radius",
                descriptionArabic = "نصف قطر موجة صدمة أكبر"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.GROUND_SLAM,
                level = 4,
                xpCost = 2000,
                newEffects = listOf(EffectType.STUN),
                description = "Stuns enemies",
                descriptionArabic = "يصعق الأعداء"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.GROUND_SLAM,
                level = 5,
                xpCost = 3000,
                mfCost = 1,
                damageIncrease = 30,
                knockbackIncrease = 200f,
                description = "Massive damage and knockback",
                descriptionArabic = "ضرر ودفع هائل"
            )
        ),
        
        // ترقيات Parry Counter
        AbilityType.PARRY_COUNTER to listOf(
            AbilityUpgrade(
                abilityType = AbilityType.PARRY_COUNTER,
                level = 2,
                xpCost = 500,
                durationIncrease = 50L,
                description = "Larger parry window",
                descriptionArabic = "نافذة صد أكبر"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.PARRY_COUNTER,
                level = 3,
                xpCost = 1000,
                damageIncrease = 20,
                description = "Stronger counter damage",
                descriptionArabic = "ضرر رد أقوى"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.PARRY_COUNTER,
                level = 4,
                xpCost = 1800,
                cooldownReduction = 500L,
                description = "Faster cooldown",
                descriptionArabic = "انتظار أسرع"
            ),
            AbilityUpgrade(
                abilityType = AbilityType.PARRY_COUNTER,
                level = 5,
                xpCost = 2800,
                newEffects = listOf(EffectType.REFLECT_PROJECTILE),
                description = "Perfect parry reflects projectiles",
                descriptionArabic = "الصد المثالي يعكس المقذوفات"
            )
        ),
        
        // يمكن إضافة المزيد من الترقيات لباقي القدرات...
    )
    
    /**
     * الحصول على ترقيات قدرة معينة
     */
    fun getUpgradesForAbility(type: AbilityType): List<AbilityUpgrade> =
        allUpgrades[type] ?: emptyList()
    
    /**
     * الحصول على ترقية محددة
     */
    fun getUpgrade(type: AbilityType, level: Int): AbilityUpgrade? =
        allUpgrades[type]?.find { it.level == level }
}

// Extension لإضافة خصائص محسوبة إلى AbilityUpgrade
private val AbilityUpgrade.knockbackIncrease: Float get() = 0f
private val AbilityUpgrade.durationIncrease: Long get() = 0L

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Ability Manager
// ═══════════════════════════════════════════════════════════════════════════

/**
 * مدير نظام القدرات
 * يتعامل مع تنفيذ القدرات، التحديثات، والتكامل مع باقي الأنظمة
 */
class AbilityManager(
    private val playerStateManager: PlayerStateManager,
    private val physicsEngine: PhysicsEngine,
    private val combatEngine: CombatEngine,
    private val particleEngine: ParticleEngine,
    private val eventBus: EventBus,
    private val scope: CoroutineScope
) {
    
    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════
    
    private val _unlockedAbilities = MutableStateFlow<Set<AbilityType>>(emptySet())
    val unlockedAbilities: StateFlow<Set<AbilityType>> = _unlockedAbilities.asStateFlow()
    
    private val _abilityLevels = MutableStateFlow<Map<AbilityType, Int>>(emptyMap())
    val abilityLevels: StateFlow<Map<AbilityType, Int>> = _abilityLevels.asStateFlow()
    
    private val _abilityCooldowns = MutableStateFlow<Map<AbilityType, Long>>(emptyMap())
    val abilityCooldowns: StateFlow<Map<AbilityType, Long>> = _abilityCooldowns.asStateFlow()
    
    private val _activeExecutions = MutableStateFlow<List<AbilityExecution>>(emptyList())
    val activeExecutions: StateFlow<List<AbilityExecution>> = _activeExecutions.asStateFlow()
    
    private val _recentAbilities = MutableStateFlow<List<Pair<AbilityType, Long>>>(emptyList())
    val recentAbilities: StateFlow<List<Pair<AbilityType, Long>>> = _recentAbilities.asStateFlow()
    
    private val _activeCombo = MutableStateFlow<AbilityCombo?>(null)
    val activeCombo: StateFlow<AbilityCombo?> = _activeCombo.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * محاولة استخدام قدرة
     */
    fun tryUseAbility(type: AbilityType): AbilityResult {
        val ability = AbilityDatabase.getAbility(type)
            ?: return AbilityResult.Failure(AbilityFailureReason.LOCKED)
        
        // تحقق من الفتح
        if (!isAbilityUnlocked(type)) {
            return AbilityResult.Failure(AbilityFailureReason.LOCKED)
        }
        
        // تحقق من الكولداون
        if (isAbilityOnCooldown(type)) {
            return AbilityResult.Failure(AbilityFailureReason.ON_COOLDOWN)
        }
        
        // تحقق من الحالة المناسبة
        if (!checkAbilityConditions(ability)) {
            return AbilityResult.Failure(AbilityFailureReason.WRONG_STATE)
        }
        
        // تحقق من التكلفة
        val costCheck = checkAbilityCost(ability)
        if (costCheck != null) {
            return AbilityResult.Failure(costCheck)
        }
        
        // تحقق إذا كانت نشطة بالفعل (للقدرات ذات المدة)
        if (ability.durationMs > 0 && isAbilityActive(type)) {
            return AbilityResult.Failure(AbilityFailureReason.ALREADY_ACTIVE)
        }
        
        // خصم التكلفة
        consumeAbilityCost(ability)
        
        // بدء التنفيذ
        val execution = startAbilityExecution(ability)
        
        // إضافة للقدرات الأخيرة (لاكتشاف الكومبو)
        addToRecentAbilities(type)
        
        // تشغيل الكولداون
        startCooldown(type, ability.cooldownMs)
        
        // إرسال حدث
        eventBus.emit(GameEvent.AbilityUsed(type, ability.name))
        
        return AbilityResult.Success(execution)
    }
    
    /**
     * فتح قدرة جديدة
     */
    fun unlockAbility(type: AbilityType): Boolean {
        val ability = AbilityDatabase.getAbility(type) ?: return false
        
        // تحقق من المتطلبات
        val playerState = playerStateManager.playerState.value
        
        if (ability.unlockXP > 0 && playerState.stats.xp < ability.unlockXP) {
            return false
        }
        
        if (ability.unlockMF > 0 && playerState.stats.memoryFragments < ability.unlockMF) {
            return false
        }
        
        // فتح القدرة
        _unlockedAbilities.value = _unlockedAbilities.value + type
        _abilityLevels.value = _abilityLevels.value + (type to 1)
        
        eventBus.emit(GameEvent.AbilityUnlocked(type, ability.name))
        
        return true
    }
    
    /**
     * ترقية قدرة
     */
    fun upgradeAbility(type: AbilityType): Boolean {
        val currentLevel = _abilityLevels.value[type] ?: return false
        val nextLevel = currentLevel + 1
        
        val upgrade = AbilityUpgradeDatabase.getUpgrade(type, nextLevel) ?: return false
        
        // تحقق من المتطلبات
        val playerState = playerStateManager.playerState.value
        
        if (upgrade.xpCost > 0 && playerState.stats.xp < upgrade.xpCost) {
            return false
        }
        
        if (upgrade.mfCost > 0 && playerState.stats.memoryFragments < upgrade.mfCost) {
            return false
        }
        
        // خصم التكلفة
        if (upgrade.xpCost > 0) {
            playerStateManager.addXP(-upgrade.xpCost)
        }
        if (upgrade.mfCost > 0) {
            playerStateManager.addMemoryFragments(-upgrade.mfCost)
        }
        
        // ترقية
        _abilityLevels.value = _abilityLevels.value + (type to nextLevel)
        
        eventBus.emit(GameEvent.AbilityUpgraded(type, nextLevel))
        
        return true
    }
    
    /**
     * الحصول على معلومات قدرة مع الترقيات المطبقة
     */
    fun getAbilityInfo(type: AbilityType): Ability? {
        val baseAbility = AbilityDatabase.getAbility(type) ?: return null
        val level = _abilityLevels.value[type] ?: 1
        
        if (level == 1) return baseAbility
        
        // تطبيق الترقيات
        var modifiedAbility = baseAbility.copy(level = level)
        
        for (upgradeLevel in 2..level) {
            val upgrade = AbilityUpgradeDatabase.getUpgrade(type, upgradeLevel) ?: continue
            
            modifiedAbility = modifiedAbility.copy(
                baseDamage = modifiedAbility.baseDamage + upgrade.damageIncrease,
                energyCost = max(0, modifiedAbility.energyCost - upgrade.energyCostReduction),
                cooldownMs = max(0L, modifiedAbility.cooldownMs - upgrade.cooldownReduction),
                range = modifiedAbility.range + upgrade.rangeIncrease,
                statusEffects = modifiedAbility.statusEffects + upgrade.newEffects
            )
        }
        
        return modifiedAbility
    }
    
    /**
     * تحديث نظام القدرات (يُستدعى كل إطار)
     */
    fun update(deltaTime: Float) {
        val currentTime = System.currentTimeMillis()
        
        // تحديث الكولداونات
        updateCooldowns(currentTime)
        
        // تحديث التنفيذات النشطة
        updateActiveExecutions(currentTime, deltaTime)
        
        // تنظيف القدرات الأخيرة القديمة
        cleanupRecentAbilities(currentTime)
        
        // تحديث الكومبو
        updateComboDetection()
    }
    
    /**
     * إلغاء قدرة نشطة
     */
    fun cancelAbility(type: AbilityType) {
        _activeExecutions.value = _activeExecutions.value.filter { it.ability.type != type }
        eventBus.emit(GameEvent.AbilityCanceled(type))
    }
    
    /**
     * إلغاء جميع القدرات النشطة
     */
    fun cancelAllAbilities() {
        _activeExecutions.value = emptyList()
    }
    
    /**
     * الحصول على نسبة الكولداون المتبقية (0-1)
     */
    fun getCooldownProgress(type: AbilityType): Float {
        val cooldownEnd = _abilityCooldowns.value[type] ?: return 0f
        val now = System.currentTimeMillis()
        
        if (now >= cooldownEnd) return 0f
        
        val ability = getAbilityInfo(type) ?: return 0f
        val remaining = (cooldownEnd - now).toFloat()
        val total = ability.cooldownMs.toFloat()
        
        return (remaining / total).coerceIn(0f, 1f)
    }
    
    /**
     * هل القدرة مفتوحة؟
     */
    fun isAbilityUnlocked(type: AbilityType): Boolean =
        _unlockedAbilities.value.contains(type)
    
    /**
     * هل القدرة في كولداون؟
     */
    fun isAbilityOnCooldown(type: AbilityType): Boolean {
        val cooldownEnd = _abilityCooldowns.value[type] ?: return false
        return System.currentTimeMillis() < cooldownEnd
    }
    
    /**
     * هل القدرة نشطة حالياً؟
     */
    fun isAbilityActive(type: AbilityType): Boolean =
        _activeExecutions.value.any { it.ability.type == type }
    
    /**
     * الحصول على مستوى قدرة
     */
    fun getAbilityLevel(type: AbilityType): Int =
        _abilityLevels.value[type] ?: 0
    
    /**
     * الحصول على تكلفة قدرة
     */
    fun getAbilityCost(type: AbilityType): Pair<Int, Int> {  // (Energy, MF)
        val ability = getAbilityInfo(type) ?: return 0 to 0
        return ability.energyCost to ability.mfCost
    }
    
    /**
     * هل يمكن استخدام القدرة؟
     */
    fun canUseAbility(type: AbilityType): Boolean {
        return when (tryUseAbility(type)) {
            is AbilityResult.Success -> {
                // نلغي التنفيذ فوراً لأننا نتحقق فقط
                cancelAbility(type)
                true
            }
            is AbilityResult.Failure -> false
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Private Helper Functions
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من شروط استخدام القدرة
     */
    private fun checkAbilityConditions(ability: Ability): Boolean {
        val playerState = playerStateManager.playerState.value
        
        // تحقق من حالة الأرض/الهواء
        if (ability.requiresGrounded && !playerState.position.isGrounded) return false
        if (ability.requiresAirborne && playerState.position.isGrounded) return false
        if (ability.requiresWallContact && !playerState.position.isOnWall) return false
        
        return true
    }
    
    /**
     * التحقق من تكلفة القدرة
     */
    private fun checkAbilityCost(ability: Ability): AbilityFailureReason? {
        val playerState = playerStateManager.playerState.value
        
        if (ability.energyCost > 0 && playerState.stats.energy < ability.energyCost) {
            return AbilityFailureReason.INSUFFICIENT_ENERGY
        }
        
        if (ability.mfCost > 0 && playerState.stats.memoryFragments < ability.mfCost) {
            return AbilityFailureReason.INSUFFICIENT_MF
        }
        
        return null
    }
    
    /**
     * خصم تكلفة القدرة
     */
    private fun consumeAbilityCost(ability: Ability) {
        if (ability.energyCost > 0) {
            playerStateManager.addEnergy(-ability.energyCost)
        }
        
        if (ability.mfCost > 0) {
            playerStateManager.addMemoryFragments(-ability.mfCost)
            playerStateManager.addForgetfulness(ability.fmGenerated)
        }
    }
    
    /**
     * بدء تنفيذ القدرة
     */
    private fun startAbilityExecution(ability: Ability): AbilityExecution {
        val now = System.currentTimeMillis()
        val execution = AbilityExecution(
            ability = ability,
            startTime = now,
            endTime = now + ability.durationMs,
            castComplete = ability.castTimeMs == 0L
        )
        
        _activeExecutions.value = _activeExecutions.value + execution
        
        // بدء Cast Time إذا كان موجوداً
        if (ability.castTimeMs > 0L) {
            scope.launch {
                delay(ability.castTimeMs)
                completeCast(execution)
            }
        } else {
            // تنفيذ فوري
            executeAbilityEffect(execution)
        }
        
        // تشغيل التأثيرات البصرية/الصوتية
        if (ability.particleEffect.isNotEmpty()) {
            spawnAbilityParticles(ability)
        }
        
        if (ability.soundEffect.isNotEmpty()) {
            eventBus.emit(GameEvent.PlaySound(ability.soundEffect))
        }
        
        return execution
    }
    
    /**
     * إكمال فترة الـ Cast
     */
    private fun completeCast(execution: AbilityExecution) {
        // تحديث حالة التنفيذ
        val updated = execution.copy(castComplete = true)
        _activeExecutions.value = _activeExecutions.value.map {
            if (it == execution) updated else it
        }
        
        // تنفيذ التأثير الفعلي
        executeAbilityEffect(updated)
    }
    
    /**
     * تنفيذ تأثير القدرة الفعلي
     */
    private fun executeAbilityEffect(execution: AbilityExecution) {
        when (execution.ability.type) {
            AbilityType.MEMORY_PULSE_SMALL,
            AbilityType.MEMORY_PULSE_LARGE -> executeMemoryPulse(execution)
            
            AbilityType.ECHO_RECALL -> executeEchoRecall(execution)
            
            AbilityType.MASK_SHARD_BLAST -> executeMaskShardBlast(execution)
            
            AbilityType.GROUND_SLAM -> executeGroundSlam(execution)
            
            AbilityType.DASH,
            AbilityType.AIR_DASH -> executeDash(execution)
            
            AbilityType.SHADOW_STEP -> executeShadowStep(execution)
            
            AbilityType.VOID_SHIFT -> executeVoidShift(execution)
            
            AbilityType.TIME_ECHO -> executeTimeEcho(execution)
            
            AbilityType.PARRY_COUNTER -> executeParry(execution)
            
            AbilityType.SPIN_ATTACK -> executeSpinAttack(execution)
            
            else -> {
                // قدرات أخرى تُنفذ مباشرة من PlayerController
            }
        }
    }
    
    /**
     * تنفيذ Memory Pulse
     */
    private fun executeMemoryPulse(execution: AbilityExecution) {
        val playerPos = playerStateManager.playerState.value.position
        val ability = execution.ability
        
        // حساب الضرر مع الكومبو
        val damage = calculateAbilityDamage(ability)
        
        // AOE damage
        val enemiesInRange = combatEngine.getEnemiesInRadius(
            centerX = playerPos.x,
            centerY = playerPos.y,
            radius = ability.aoeRadius
        )
        
        enemiesInRange.forEach { enemy ->
            if (!execution.hitEnemies.contains(enemy.id)) {
                combatEngine.dealDamage(
                    targetId = enemy.id,
                    damage = damage,
                    knockbackX = (enemy.x - playerPos.x).coerceIn(-1f, 1f) * ability.knockbackForce,
                    knockbackY = -ability.knockbackForce * 0.5f,
                    source = "Memory Pulse"
                )
                execution.hitEnemies.add(enemy.id)
            }
        }
        
        // كشف الأسرار
        if (ability.statusEffects.contains(EffectType.REVELATION)) {
            eventBus.emit(GameEvent.RevealSecrets(playerPos.x, playerPos.y, ability.aoeRadius))
        }
        
        // Particles
        particleEngine.spawnPulseWave(
            x = playerPos.x,
            y = playerPos.y,
            radius = ability.aoeRadius,
            color = if (ability.type == AbilityType.MEMORY_PULSE_LARGE) 0xFFFFD700.toInt() else 0xFFADD8E6.toInt()
        )
    }
    
    /**
     * تنفيذ Echo Recall
     */
    private fun executeEchoRecall(execution: AbilityExecution) {
        val playerState = playerStateManager.playerState.value
        
        // استدعاء نسخة ظلية
        val echoId = "echo_${System.currentTimeMillis()}"
        execution.spawnedEntities.add(echoId)
        
        eventBus.emit(GameEvent.SpawnEcho(
            id = echoId,
            x = playerState.position.x,
            y = playerState.position.y,
            duration = execution.ability.durationMs,
            damage = execution.ability.baseDamage
        ))
        
        // جدولة الإزالة
        scope.launch {
            delay(execution.ability.durationMs)
            eventBus.emit(GameEvent.RemoveEcho(echoId))
        }
    }
    
    /**
     * تنفيذ Mask Shard Blast
     */
    private fun executeMaskShardBlast(execution: AbilityExecution) {
        val playerState = playerStateManager.playerState.value
        val direction = if (playerState.position.isFacingRight) 1f else -1f
        
        // إطلاق مقذوف
        val projectileId = "shard_${System.currentTimeMillis()}"
        execution.spawnedEntities.add(projectileId)
        
        eventBus.emit(GameEvent.SpawnProjectile(
            id = projectileId,
            x = playerState.position.x + direction,
            y = playerState.position.y + 0.5f,
            velocityX = direction * 15f,
            velocityY = 0f,
            damage = execution.ability.baseDamage,
            knockback = execution.ability.knockbackForce,
            piercing = true,  // يخترق الحواجز
            lifetime = 2000L
        ))
    }
    
    /**
     * تنفيذ Ground Slam
     */
    private fun executeGroundSlam(execution: AbilityExecution) {
        val playerState = playerStateManager.playerState.value
        
        // دفع اللاعب للأسفل بقوة
        physicsEngine.applyForce(
            entityId = "player",
            forceX = 0f,
            forceY = -1200f
        )
        
        // عند الاصطدام بالأرض، نفذ الضرر
        scope.launch {
            // انتظر حتى يلمس الأرض
            while (!playerStateManager.playerState.value.position.isGrounded) {
                delay(16L)
            }
            
            // AOE damage
            val damage = calculateAbilityDamage(execution.ability)
            val enemiesInRange = combatEngine.getEnemiesInRadius(
                centerX = playerState.position.x,
                centerY = playerState.position.y,
                radius = execution.ability.aoeRadius
            )
            
            enemiesInRange.forEach { enemy ->
                combatEngine.dealDamage(
                    targetId = enemy.id,
                    damage = damage,
                    knockbackX = (enemy.x - playerState.position.x).coerceIn(-1f, 1f) * execution.ability.knockbackForce,
                    knockbackY = -200f,
                    source = "Ground Slam"
                )
            }
            
            // Shockwave effect
            particleEngine.spawnShockwave(
                x = playerState.position.x,
                y = playerState.position.y,
                radius = execution.ability.aoeRadius
            )
            
            // Screen shake
            eventBus.emit(GameEvent.ScreenShake(intensity = 0.8f, duration = 300L))
        }
    }
    
    /**
     * تنفيذ Dash
     */
    private fun executeDash(execution: AbilityExecution) {
        val playerState = playerStateManager.playerState.value
        val direction = if (playerState.position.isFacingRight) 1f else -1f
        
        // دفع قوي في الاتجاه
        val dashForce = if (execution.ability.type == AbilityType.AIR_DASH) 1200f else 1000f
        
        physicsEngine.applyImpulse(
            entityId = "player",
            impulseX = direction * dashForce,
            impulseY = 0f
        )
        
        // تطبيق حصانة مؤقتة
        playerStateManager.addEffect(
            PlayerEffect(
                type = EffectType.INVULNERABLE,
                duration = execution.ability.durationMs,
                value = 1f
            )
        )
        
        // Trail particles
        particleEngine.spawnDashTrail(
            x = playerState.position.x,
            y = playerState.position.y,
            direction = direction
        )
    }
    
    /**
     * تنفيذ Shadow Step
     */
    private fun executeShadowStep(execution: AbilityExecution) {
        val playerState = playerStateManager.playerState.value
        val direction = if (playerState.position.isFacingRight) 1f else -1f
        
        // حساب الموقع الجديد
        val teleportDistance = execution.ability.range
        val newX = playerState.position.x + (direction * teleportDistance)
        val newY = playerState.position.y
        
        // تحقق من العوائق
        val canTeleport = physicsEngine.canMoveTo(newX, newY)
        
        if (canTeleport) {
            // Teleport particles (start)
            particleEngine.spawnTeleportEffect(
                x = playerState.position.x,
                y = playerState.position.y,
                isShadow = true
            )
            
            // انتقال فوري
            playerStateManager.setPosition(newX, newY)
            
            // Teleport particles (end)
            particleEngine.spawnTeleportEffect(
                x = newX,
                y = newY,
                isShadow = true
            )
            
            // حصانة قصيرة
            playerStateManager.addEffect(
                PlayerEffect(
                    type = EffectType.INVULNERABLE,
                    duration = 200L,
                    value = 1f
                )
            )
        }
    }
    
    /**
     * تنفيذ Void Shift
     */
    private fun executeVoidShift(execution: AbilityExecution) {
        // تطبيق حالة Intangible
        playerStateManager.addEffect(
            PlayerEffect(
                type = EffectType.INTANGIBLE,
                duration = execution.ability.durationMs,
                value = 1f
            )
        )
        
        // تأثير بصري مستمر
        scope.launch {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < execution.ability.durationMs) {
                val playerPos = playerStateManager.playerState.value.position
                particleEngine.spawnVoidParticles(playerPos.x, playerPos.y)
                delay(100L)
            }
        }
    }
    
    /**
     * تنفيذ Time Echo
     */
    private fun executeTimeEcho(execution: AbilityExecution) {
        // حفظ الحالة الحالية
        val savedState = playerStateManager.playerState.value.copy()
        
        // إذا أُصيب اللاعب بضرر قاتل خلال المدة، استرجع الحالة
        scope.launch {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < execution.ability.durationMs) {
                val currentState = playerStateManager.playerState.value
                if (currentState.stats.hp <= 0) {
                    // Rewind!
                    playerStateManager.restoreState(savedState)
                    
                    // تأثيرات
                    particleEngine.spawnTimeRewindEffect(
                        currentState.position.x,
                        currentState.position.y
                    )
                    eventBus.emit(GameEvent.TimeRewound)
                    
                    break
                }
                delay(100L)
            }
        }
    }
    
    /**
     * تنفيذ Parry
     */
    private fun executeParry(execution: AbilityExecution) {
        // تطبيق نافذة الصد
        playerStateManager.addEffect(
            PlayerEffect(
                type = EffectType.PARRY_WINDOW,
                duration = execution.ability.durationMs,
                value = 1f
            )
        )
        
        // الاستماع لحدث Parry Success
        // (يُرسل من CombatEngine عند استقبال ضربة أثناء نافذة الصد)
        // ثم تنفيذ الرد
    }
    
    /**
     * تنفيذ Spin Attack
     */
    private fun executeSpinAttack(execution: AbilityExecution) {
        val playerPos = playerStateManager.playerState.value.position
        val ability = execution.ability
        
        // ضرر دوري أثناء الدوران
        scope.launch {
            val startTime = System.currentTimeMillis()
            val hits = mutableSetOf<String>()
            
            while (System.currentTimeMillis() - startTime < ability.durationMs) {
                val damage = calculateAbilityDamage(ability)
                val enemiesInRange = combatEngine.getEnemiesInRadius(
                    centerX = playerPos.x,
                    centerY = playerPos.y,
                    radius = ability.aoeRadius
                )
                
                enemiesInRange.forEach { enemy ->
                    if (!hits.contains(enemy.id)) {
                        combatEngine.dealDamage(
                            targetId = enemy.id,
                            damage = damage,
                            knockbackX = (enemy.x - playerPos.x).coerceIn(-1f, 1f) * 200f,
                            knockbackY = -100f,
                            source = "Spin Attack"
                        )
                        hits.add(enemy.id)
                    }
                }
                
                // Trail particles
                particleEngine.spawnSpinTrail(playerPos.x, playerPos.y)
                
                delay(100L)
            }
        }
    }
    
    /**
     * حساب الضرر النهائي مع جميع المضاعفات
     */
    private fun calculateAbilityDamage(ability: Ability): Int {
        var damage = ability.baseDamage
        
        // تطبيق Scaling حسب المستوى
        val level = _abilityLevels.value[ability.type] ?: 1
        damage = (damage * (1f + (level - 1) * 0.1f)).toInt()
        
        // تطبيق مضاعف الكومبو
        val combo = _activeCombo.value
        if (combo != null && combo.abilities.contains(ability.type)) {
            damage += combo.bonusDamage
        }
        
        // تطبيق تأثيرات اللاعب
        val playerEffects = playerStateManager.playerState.value.effects.activeEffects
        
        if (playerEffects.any { it.type == EffectType.DAMAGE_BOOST }) {
            damage = (damage * 1.5f).toInt()
        }
        
        if (playerEffects.any { it.type == EffectType.CRITICAL_HIT }) {
            damage = (damage * 2f).toInt()
        }
        
        return damage
    }
    
    /**
     * بدء كولداون لقدرة
     */
    private fun startCooldown(type: AbilityType, cooldownMs: Long) {
        if (cooldownMs <= 0L) return
        
        val endTime = System.currentTimeMillis() + cooldownMs
        _abilityCooldowns.value = _abilityCooldowns.value + (type to endTime)
    }
    
    /**
     * تحديث الكولداونات
     */
    private fun updateCooldowns(currentTime: Long) {
        _abilityCooldowns.value = _abilityCooldowns.value.filterValues { it > currentTime }
    }
    
    /**
     * تحديث التنفيذات النشطة
     */
    private fun updateActiveExecutions(currentTime: Long, deltaTime: Float) {
        // إزالة التنفيذات المنتهية
        _activeExecutions.value = _activeExecutions.value.filter { execution ->
            if (execution.ability.durationMs == 0L) {
                // قدرات فورية تُزال مباشرة بعد التنفيذ
                false
            } else {
                currentTime < execution.endTime
            }
        }
    }
    
    /**
     * إضافة قدرة للقائمة الأخيرة
     */
    private fun addToRecentAbilities(type: AbilityType) {
        val now = System.currentTimeMillis()
        _recentAbilities.value = (_recentAbilities.value + (type to now)).takeLast(10)
    }
    
    /**
     * تنظيف القدرات الأخيرة القديمة
     */
    private fun cleanupRecentAbilities(currentTime: Long) {
        val maxAge = 5000L  // 5 ثواني
        _recentAbilities.value = _recentAbilities.value.filter { (_, timestamp) ->
            currentTime - timestamp < maxAge
        }
    }
    
    /**
     * اكتشاف الكومبو
     */
    private fun updateComboDetection() {
        val recentTypes = _recentAbilities.value.map { it.first }
        val combo = AbilityComboDatabase.findCombo(recentTypes)
        
        if (combo != _activeCombo.value) {
            _activeCombo.value = combo
            if (combo != null) {
                eventBus.emit(GameEvent.ComboActivated(combo.name))
                
                // تطبيق تأثيرات الكومبو
                combo.bonusEffects.forEach { effectType ->
                    playerStateManager.addEffect(
                        PlayerEffect(
                            type = effectType,
                            duration = 3000L,
                            value = 1f
                        )
                    )
                }
            }
        }
    }
    
    /**
     * توليد جزيئات القدرة
     */
    private fun spawnAbilityParticles(ability: Ability) {
        val playerPos = playerStateManager.playerState.value.position
        
        when (ability.particleEffect) {
            "burst_leap_particles" -> particleEngine.spawnBurstLeap(playerPos.x, playerPos.y)
            "dash_trail" -> particleEngine.spawnDashTrail(playerPos.x, playerPos.y, if (playerPos.isFacingRight) 1f else -1f)
            "double_jump_burst" -> particleEngine.spawnDoubleJumpBurst(playerPos.x, playerPos.y)
            "air_dash_trail" -> particleEngine.spawnAirDashTrail(playerPos.x, playerPos.y)
            "ground_slam_shockwave" -> particleEngine.spawnShockwave(playerPos.x, playerPos.y, ability.aoeRadius)
            "charge_glow" -> particleEngine.spawnChargeGlow(playerPos.x, playerPos.y)
            "spin_trail" -> particleEngine.spawnSpinTrail(playerPos.x, playerPos.y)
            "memory_pulse_wave_small" -> particleEngine.spawnPulseWave(playerPos.x, playerPos.y, ability.aoeRadius, 0xFFADD8E6.toInt())
            "memory_pulse_wave_large" -> particleEngine.spawnPulseWave(playerPos.x, playerPos.y, ability.aoeRadius, 0xFFFFD700.toInt())
            "echo_spawn" -> particleEngine.spawnEchoEffect(playerPos.x, playerPos.y)
            "mask_shard_projectile" -> particleEngine.spawnShardProjectile(playerPos.x, playerPos.y)
            "name_transfer" -> particleEngine.spawnNameTransfer(playerPos.x, playerPos.y)
            "memory_restoration_aura" -> particleEngine.spawnRestorationAura(playerPos.x, playerPos.y)
            "shadow_teleport" -> particleEngine.spawnTeleportEffect(playerPos.x, playerPos.y, true)
            "time_rewind" -> particleEngine.spawnTimeRewindEffect(playerPos.x, playerPos.y)
            "void_form" -> particleEngine.spawnVoidParticles(playerPos.x, playerPos.y)
        }
    }
    
    /**
     * إعادة تعيين جميع القدرات (للـ New Game)
     */
    fun reset() {
        _unlockedAbilities.value = emptySet()
        _abilityLevels.value = emptyMap()
        _abilityCooldowns.value = emptyMap()
        _activeExecutions.value = emptyList()
        _recentAbilities.value = emptyList()
        _activeCombo.value = null
    }
    
    /**
     * حفظ حالة القدرات
     */
    fun saveState(): Map<String, Any> = mapOf(
        "unlockedAbilities" to _unlockedAbilities.value.map { it.name },
        "abilityLevels" to _abilityLevels.value.mapKeys { it.key.name }
    )
    
    /**
     * تحميل حالة القدرات
     */
    fun loadState(data: Map<String, Any>) {
        val unlockedNames = data["unlockedAbilities"] as? List<String> ?: emptyList()
        _unlockedAbilities.value = unlockedNames.mapNotNull { name ->
            AbilityType.values().find { it.name == name }
        }.toSet()
        
        val levelMap = data["abilityLevels"] as? Map<String, Int> ?: emptyMap()
        _abilityLevels.value = levelMap.mapKeys { (name, _) ->
            AbilityType.values().find { it.name == name }
        }.filterKeys { it != null }.mapKeys { it.key!! }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Extensions
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Extension functions للـ ParticleEngine لتوليد جزيئات القدرات
 * (هذه دوال placeholder — التنفيذ الفعلي في ParticleEngine.kt)
 */
private fun ParticleEngine.spawnBurstLeap(x: Float, y: Float) {}
private fun ParticleEngine.spawnDashTrail(x: Float, y: Float, direction: Float) {}
private fun ParticleEngine.spawnDoubleJumpBurst(x: Float, y: Float) {}
private fun ParticleEngine.spawnAirDashTrail(x: Float, y: Float) {}
private fun ParticleEngine.spawnShockwave(x: Float, y: Float, radius: Float) {}
private fun ParticleEngine.spawnChargeGlow(x: Float, y: Float) {}
private fun ParticleEngine.spawnSpinTrail(x: Float, y: Float) {}
private fun ParticleEngine.spawnPulseWave(x: Float, y: Float, radius: Float, color: Int) {}
private fun ParticleEngine.spawnEchoEffect(x: Float, y: Float) {}
private fun ParticleEngine.spawnShardProjectile(x: Float, y: Float) {}
private fun ParticleEngine.spawnNameTransfer(x: Float, y: Float) {}
private fun ParticleEngine.spawnRestorationAura(x: Float, y: Float) {}
private fun ParticleEngine.spawnTeleportEffect(x: Float, y: Float, isShadow: Boolean) {}
private fun ParticleEngine.spawnTimeRewindEffect(x: Float, y: Float) {}
private fun ParticleEngine.spawnVoidParticles(x: Float, y: Float) {}

/**
 * Extension للـ PhysicsEngine
 */
private fun PhysicsEngine.applyForce(entityId: String, forceX: Float, forceY: Float) {}
private fun PhysicsEngine.applyImpulse(entityId: String, impulseX: Float, impulseY: Float) {}
private fun PhysicsEngine.canMoveTo(x: Float, y: Float): Boolean = true

/**
 * Extension للـ CombatEngine
 */
private data class EnemyData(val id: String, val x: Float, val y: Float)
private fun CombatEngine.getEnemiesInRadius(centerX: Float, centerY: Float, radius: Float): List<EnemyData> = emptyList()
private fun CombatEngine.dealDamage(targetId: String, damage: Int, knockbackX: Float, knockbackY: Float, source: String) {}

/**
 * Extension للـ PlayerStateManager
 */
private fun PlayerStateManager.setPosition(x: Float, y: Float) {}
private fun PlayerStateManager.restoreState(state: PlayerState) {}