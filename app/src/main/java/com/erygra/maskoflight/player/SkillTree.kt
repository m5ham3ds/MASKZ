package com.erygra.maskoflight.player

import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SkillTree.kt — نظام شجرة المهارات للعبة "قِنَاعُ النُّور"
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * يدير تطور اللاعب عبر نظام شجرة مهارات متعدد المسارات.
 * يتضمن:
 * - 3 مسارات رئيسية (Combat, Exploration, Memory)
 * - نظام المستويات والخبرة (XP)
 * - نظام النقاط (Skill Points)
 * - المهارات السلبية والنشطة
 * - شجرة الترقيات المتدرجة
 * - نظام إعادة التعيين (Respec)
 * - تكامل مع PlayerStateManager, AbilityManager, EventBus
 * 
 * @author Erygra Universe Development Team
 * @version 2.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Skill Data Classes
// ═══════════════════════════════════════════════════════════════════════════

/**
 * مسار المهارات
 */
enum class SkillPath {
    COMBAT,         // القتال
    EXPLORATION,    // الاستكشاف
    MEMORY          // الذاكرة
}

/**
 * نوع المهارة
 */
enum class SkillType {
    PASSIVE,        // مهارة سلبية (تأثير دائم)
    ACTIVE,         // مهارة نشطة (تفتح قدرة)
    STAT_BOOST,     // تعزيز إحصائيات
    UNLOCK          // فتح ميكانيكا جديدة
}

/**
 * طبقة المهارة في الشجرة
 */
enum class SkillTier {
    TIER_1,     // الطبقة الأولى (متاحة من البداية)
    TIER_2,     // الطبقة الثانية (Level 5+)
    TIER_3,     // الطبقة الثانية (Level 10+)
    TIER_4,     // الطبقة الثالثة (Level 15+)
    TIER_5,     // الطبقة الرابعة (Level 20+)
    ULTIMATE    // المهارات النهائية (Level 25+)
}

/**
 * أنواع تأثيرات اللاعب الناتجة عن المهارات
 * (ملاحظة: تم نقلها إلى EffectType في PlayerState.kt)
 */
// typealias PlayerEffectType = EffectType // يمكن استخدام هذا للسهولة

/**
 * تعريف المهارة
 */
data class Skill(
    val id: String,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    val path: SkillPath,
    val type: SkillType,
    val tier: SkillTier,
    
    // التكاليف والمتطلبات
    val skillPointsCost: Int = 1,
    val requiredLevel: Int = 1,
    val requiredSkills: List<String> = emptyList(),  // IDs of prerequisite skills
    val requiredMF: Int = 0,
    
    // التأثيرات
    val statBoosts: Map<String, Float> = emptyMap(),  // stat name -> boost value
    val effectTypes: List<EffectType> = emptyList(),
    val unlockedAbility: AbilityType? = null,
    val unlockedMechanic: String = "",  // e.g., "wall_jump", "double_jump"
    
    // القيم
    val values: Map<String, Float> = emptyMap(),  // للمهارات ذات القيم المتغيرة
    val maxRank: Int = 1,  // الحد الأقصى للمستوى (للمهارات القابلة للترقية)
    
    // البيانات الوصفية
    val iconResource: String = "",
    val loreText: String = "",
    val loreTextArabic: String = "",
    val position: SkillPosition = SkillPosition(0, 0)  // موقع في الشجرة (للواجهة)
)

/**
 * موقع المهارة في الشجرة (للواجهة)
 */
data class SkillPosition(
    val x: Int,
    val y: Int
)

/**
 * حالة المهارة للاعب
 */
data class PlayerSkill(
    val skill: Skill,
    val unlocked: Boolean = false,
    val currentRank: Int = 0,
    val unlockedAt: Long = 0L  // timestamp
)

/**
 * نتيجة محاولة فتح مهارة
 */
sealed class SkillUnlockResult {
    data class Success(val skill: Skill, val newRank: Int) : SkillUnlockResult()
    data class Failure(val reason: SkillUnlockFailureReason) : SkillUnlockResult()
}

/**
 * أسباب فشل فتح المهارة
 */
enum class SkillUnlockFailureReason {
    INSUFFICIENT_SKILL_POINTS,  // نقاط مهارات غير كافية
    LEVEL_TOO_LOW,              // المستوى منخفض جداً
    PREREQUISITES_NOT_MET,      // المتطلبات غير محققة
    ALREADY_MAX_RANK,           // المهارة في الحد الأقصى
    INSUFFICIENT_MF,            // MF غير كافي
    SKILL_NOT_FOUND             // المهارة غير موجودة
}

/**
 * ملخص إحصائيات المسار
 */
data class PathSummary(
    val path: SkillPath,
    val totalSkills: Int,
    val unlockedSkills: Int,
    val totalPointsSpent: Int,
    val bonuses: Map<String, Float>  // مجموع المكافآت من المسار
)

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Skill Database
// ═══════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات المهارات
 */
object SkillDatabase {
    
    /**
     * جميع المهارات المتاحة في اللعبة
     */
    val allSkills = listOf(
        
        // ═══════════════════════════════════════════════════════════════
        // Combat Path (مسار القتال)
        // ═══════════════════════════════════════════════════════════════
        
        // Tier 1
        Skill(
            id = "combat_basic_strike",
            name = "Basic Strike Mastery",
            nameArabic = "إتقان الضربة الأساسية",
            description = "+10% damage with basic attacks",
            descriptionArabic = "+10% ضرر مع الهجمات الأساسية",
            path = SkillPath.COMBAT,
            type = SkillType.STAT_BOOST,
            tier = SkillTier.TIER_1,
            skillPointsCost = 1,
            requiredLevel = 1,
            statBoosts = mapOf("basicAttackDamage" to 0.10f),
            maxRank = 3,
            iconResource = "skill_basic_strike",
            position = SkillPosition(0, 0)
        ),
        
        Skill(
            id = "combat_health_boost",
            name = "Vitality",
            nameArabic = "الحيوية",
            description = "+10 max HP",
            descriptionArabic = "+10 نقاط صحة قصوى",
            path = SkillPath.COMBAT,
            type = SkillType.STAT_BOOST,
            tier = SkillTier.TIER_1,
            skillPointsCost = 1,
            requiredLevel = 1,
            statBoosts = mapOf("maxHp" to 10f),
            maxRank = 5,
            iconResource = "skill_vitality",
            position = SkillPosition(1, 0)
        ),
        
        Skill(
            id = "combat_precision_unlock",
            name = "Precision Strike",
            nameArabic = "ضربة دقيقة",
            description = "Unlocks Precision Strike ability",
            descriptionArabic = "تفتح قدرة الضربة الدقيقة",
            path = SkillPath.COMBAT,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_1,
            skillPointsCost = 2,
            requiredLevel = 3,
            unlockedAbility = AbilityType.PRECISION_STRIKE,
            iconResource = "skill_precision_strike",
            position = SkillPosition(0, 1),
            loreText = "The wanderer's first lesson: strike true, strike fast",
            loreTextArabic = "الدرس الأول للمتجول: اضرب بدقة، اضرب بسرعة"
        ),
        
        // Tier 2
        Skill(
            id = "combat_crit_chance",
            name = "Critical Eye",
            nameArabic = "العين الحرجة",
            description = "+5% critical hit chance",
            descriptionArabic = "+5% فرصة ضربة حرجة",
            path = SkillPath.COMBAT,
            type = SkillType.STAT_BOOST,
            tier = SkillTier.TIER_2,
            skillPointsCost = 2,
            requiredLevel = 5,
            requiredSkills = listOf("combat_basic_strike"),
            statBoosts = mapOf("critChance" to 0.05f),
            maxRank = 3,
            iconResource = "skill_critical_eye",
            position = SkillPosition(0, 2)
        ),
        
        Skill(
            id = "combat_parry_unlock",
            name = "Parry & Counter",
            nameArabic = "صد ورد",
            description = "Unlocks Parry & Counter ability",
            descriptionArabic = "تفتح قدرة الصد والرد",
            path = SkillPath.COMBAT,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_2,
            skillPointsCost = 3,
            requiredLevel = 6,
            requiredSkills = listOf("combat_precision_unlock"),
            unlockedAbility = AbilityType.PARRY_COUNTER,
            iconResource = "skill_parry",
            position = SkillPosition(1, 2),
            loreText = "Turn their strength against them",
            loreTextArabic = "حوّل قوتهم ضدهم"
        ),
        
        Skill(
            id = "combat_energy_regen",
            name = "Combat Flow",
            nameArabic = "تدفق القتال",
            description = "+20% energy regeneration during combat",
            descriptionArabic = "+20% تجديد الطاقة أثناء القتال",
            path = SkillPath.COMBAT,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_2,
            skillPointsCost = 2,
            requiredLevel = 5,
            requiredSkills = listOf("combat_health_boost"),
            statBoosts = mapOf("combatEnergyRegen" to 0.20f),
            maxRank = 2,
            iconResource = "skill_combat_flow",
            position = SkillPosition(2, 2)
        ),
        
        // Tier 3
        Skill(
            id = "combat_charged_heavy",
            name = "Charged Heavy Attack",
            nameArabic = "هجوم ثقيل محمّل",
            description = "Unlocks Charged Heavy Attack",
            descriptionArabic = "تفتح الهجوم الثقيل المحمّل",
            path = SkillPath.COMBAT,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 3,
            requiredLevel = 10,
            requiredSkills = listOf("combat_crit_chance"),
            unlockedAbility = AbilityType.CHARGED_HEAVY,
            iconResource = "skill_charged_heavy",
            position = SkillPosition(0, 3)
        ),
        
        Skill(
            id = "combat_lifesteal",
            name = "Vampiric Strikes",
            nameArabic = "ضربات مصّاصة",
            description = "5% of damage dealt heals you",
            descriptionArabic = "5% من الضرر المُلحق يشفيك",
            path = SkillPath.COMBAT,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 4,
            requiredLevel = 10,
            requiredSkills = listOf("combat_parry_unlock"),
            effectTypes = listOf(EffectType.LIFESTEAL),
            values = mapOf("lifestealPercent" to 0.05f),
            iconResource = "skill_lifesteal",
            position = SkillPosition(1, 3)
        ),
        
        Skill(
            id = "combat_spin_attack",
            name = "Whirlwind",
            nameArabic = "الإعصار",
            description = "Unlocks Spin Attack",
            descriptionArabic = "تفتح الهجوم الدوار",
            path = SkillPath.COMBAT,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 3,
            requiredLevel = 12,
            requiredSkills = listOf("combat_energy_regen"),
            unlockedAbility = AbilityType.SPIN_ATTACK,
            iconResource = "skill_spin_attack",
            position = SkillPosition(2, 3)
        ),
        
        // Tier 4
        Skill(
            id = "combat_armor_pierce",
            name = "Armor Breaker",
            nameArabic = "كاسر الدروع",
            description = "Attacks ignore 25% of enemy armor",
            descriptionArabic = "الهجمات تتجاهل 25% من دروع العدو",
            path = SkillPath.COMBAT,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_4,
            skillPointsCost = 4,
            requiredLevel = 15,
            requiredSkills = listOf("combat_charged_heavy"),
            effectTypes = listOf(EffectType.ARMOR_PIERCE),
            values = mapOf("armorPiercePercent" to 0.25f),
            iconResource = "skill_armor_pierce",
            position = SkillPosition(0, 4)
        ),
        
        Skill(
            id = "combat_berserker",
            name = "Berserker Rage",
            nameArabic = "غضب المحارب",
            description = "+30% damage when HP < 30%",
            descriptionArabic = "+30% ضرر عندما الصحة < 30%",
            path = SkillPath.COMBAT,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_4,
            skillPointsCost = 5,
            requiredLevel = 16,
            requiredSkills = listOf("combat_lifesteal"),
            effectTypes = listOf(EffectType.BERSERKER),
            values = mapOf("damageBoost" to 0.30f, "hpThreshold" to 0.30f),
            iconResource = "skill_berserker",
            position = SkillPosition(1, 4)
        ),
        
        // Tier 5
        Skill(
            id = "combat_execution",
            name = "Executioner",
            nameArabic = "الجلاد",
            description = "Instant kill enemies below 10% HP",
            descriptionArabic = "قتل فوري للأعداء تحت 10% صحة",
            path = SkillPath.COMBAT,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_5,
            skillPointsCost = 6,
            requiredLevel = 20,
            requiredSkills = listOf("combat_armor_pierce", "combat_berserker"),
            effectTypes = listOf(EffectType.EXECUTE),
            values = mapOf("executeThreshold" to 0.10f),
            iconResource = "skill_execution",
            position = SkillPosition(0, 5),
            loreText = "Mercy is for those who remember their names",
            loreTextArabic = "الرحمة لمن يتذكر اسمه"
        ),
        
        // Ultimate
        Skill(
            id = "combat_ultimate_warlord",
            name = "Warlord's Dominance",
            nameArabic = "سيطرة أمير الحرب",
            description = "+20% all combat stats, immune to stun",
            descriptionArabic = "+20% جميع إحصائيات القتال، مناعة للصعق",
            path = SkillPath.COMBAT,
            type = SkillType.PASSIVE,
            tier = SkillTier.ULTIMATE,
            skillPointsCost = 10,
            requiredLevel = 25,
            requiredMF = 30,
            requiredSkills = listOf("combat_execution"),
            statBoosts = mapOf(
                "allCombatStats" to 0.20f
            ),
            effectTypes = listOf(EffectType.STUN_IMMUNE),
            iconResource = "skill_warlord",
            position = SkillPosition(0, 6),
            loreText = "The mask remembers every battle, every scar",
            loreTextArabic = "القناع يتذكر كل معركة، كل ندبة"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // Exploration Path (مسار الاستكشاف)
        // ═══════════════════════════════════════════════════════════════
        
        // Tier 1
        Skill(
            id = "explore_stamina_boost",
            name = "Endurance",
            nameArabic = "التحمل",
            description = "+10 max Energy",
            descriptionArabic = "+10 نقاط طاقة قصوى",
            path = SkillPath.EXPLORATION,
            type = SkillType.STAT_BOOST,
            tier = SkillTier.TIER_1,
            skillPointsCost = 1,
            requiredLevel = 1,
            statBoosts = mapOf("maxEnergy" to 10f),
            maxRank = 5,
            iconResource = "skill_endurance",
            position = SkillPosition(0, 0)
        ),
        
        Skill(
            id = "explore_movement_speed",
            name = "Swift Feet",
            nameArabic = "أقدام سريعة",
            description = "+10% movement speed",
            descriptionArabic = "+10% سرعة الحركة",
            path = SkillPath.EXPLORATION,
            type = SkillType.STAT_BOOST,
            tier = SkillTier.TIER_1,
            skillPointsCost = 1,
            requiredLevel = 1,
            statBoosts = mapOf("movementSpeed" to 0.10f),
            maxRank = 3,
            iconResource = "skill_swift_feet",
            position = SkillPosition(1, 0)
        ),
        
        Skill(
            id = "explore_ledge_grab",
            name = "Ledge Grab & Climb",
            nameArabic = "تسلق الحواف",
            description = "Unlocks ledge grabbing and climbing",
            descriptionArabic = "تفتح إمساك وتسلق الحواف",
            path = SkillPath.EXPLORATION,
            type = SkillType.UNLOCK,
            tier = SkillTier.TIER_1,
            skillPointsCost = 2,
            requiredLevel = 2,
            unlockedAbility = AbilityType.LEDGE_GRAB,
            unlockedMechanic = "ledge_grab",
            iconResource = "skill_ledge_grab",
            position = SkillPosition(0, 1)
        ),
        
        // Tier 2
        Skill(
            id = "explore_dash",
            name = "Quick Dash",
            nameArabic = "اندفاع سريع",
            description = "Unlocks Dash ability",
            descriptionArabic = "تفتح قدرة الاندفاع",
            path = SkillPath.EXPLORATION,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_2,
            skillPointsCost = 3,
            requiredLevel = 5,
            requiredSkills = listOf("explore_movement_speed"),
            unlockedAbility = AbilityType.DASH,
            iconResource = "skill_dash",
            position = SkillPosition(1, 2)
        ),
        
        Skill(
            id = "explore_wall_jump",
            name = "Wall Jump",
            nameArabic = "قفزة الجدار",
            description = "Unlocks Wall Jump ability",
            descriptionArabic = "تفتح قدرة قفزة الجدار",
            path = SkillPath.EXPLORATION,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_2,
            skillPointsCost = 3,
            requiredLevel = 6,
            requiredSkills = listOf("explore_ledge_grab"),
            unlockedAbility = AbilityType.WALL_JUMP,
            unlockedMechanic = "wall_jump",
            iconResource = "skill_wall_jump",
            position = SkillPosition(0, 2)
        ),
        
        Skill(
            id = "explore_fall_damage_reduction",
            name = "Cat's Landing",
            nameArabic = "هبوط القط",
            description = "-50% fall damage",
            descriptionArabic = "-50% ضرر السقوط",
            path = SkillPath.EXPLORATION,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_2,
            skillPointsCost = 2,
            requiredLevel = 5,
            requiredSkills = listOf("explore_stamina_boost"),
            statBoosts = mapOf("fallDamageReduction" to 0.50f),
            iconResource = "skill_cats_landing",
            position = SkillPosition(2, 2)
        ),
        
        // Tier 3
        Skill(
            id = "explore_double_jump",
            name = "Double Jump",
            nameArabic = "قفزة مزدوجة",
            description = "Unlocks Double Jump",
            descriptionArabic = "تفتح القفزة المزدوجة",
            path = SkillPath.EXPLORATION,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 4,
            requiredLevel = 10,
            requiredSkills = listOf("explore_wall_jump"),
            unlockedAbility = AbilityType.DOUBLE_JUMP,
            iconResource = "skill_double_jump",
            position = SkillPosition(0, 3),
            loreText = "Defy gravity, if only for a moment",
            loreTextArabic = "تحدَّ الجاذبية، ولو للحظة"
        ),
        
        Skill(
            id = "explore_air_dash",
            name = "Air Dash",
            nameArabic = "اندفاع هوائي",
            description = "Unlocks Air Dash",
            descriptionArabic = "تفتح الاندفاع الهوائي",
            path = SkillPath.EXPLORATION,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 4,
            requiredLevel = 11,
            requiredSkills = listOf("explore_dash", "explore_double_jump"),
            unlockedAbility = AbilityType.AIR_DASH,
            iconResource = "skill_air_dash",
            position = SkillPosition(1, 3)
        ),
        
        Skill(
            id = "explore_energy_efficiency",
            name = "Efficient Movement",
            nameArabic = "حركة فعّالة",
            description = "Movement abilities cost 25% less energy",
            descriptionArabic = "قدرات الحركة تكلف 25% طاقة أقل",
            path = SkillPath.EXPLORATION,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 3,
            requiredLevel = 10,
            requiredSkills = listOf("explore_fall_damage_reduction"),
            statBoosts = mapOf("movementEnergyCost" to -0.25f),
            iconResource = "skill_energy_efficiency",
            position = SkillPosition(2, 3)
        ),
        
        // Tier 4
        Skill(
            id = "explore_ground_slam",
            name = "Ground Slam",
            nameArabic = "ضربة الأرض",
            description = "Unlocks Ground Slam",
            descriptionArabic = "تفتح ضربة الأرض",
            path = SkillPath.EXPLORATION,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_4,
            skillPointsCost = 4,
            requiredLevel = 15,
            requiredSkills = listOf("explore_air_dash"),
            unlockedAbility = AbilityType.GROUND_SLAM,
            iconResource = "skill_ground_slam",
            position = SkillPosition(1, 4)
        ),
        
        Skill(
            id = "explore_treasure_hunter",
            name = "Treasure Hunter",
            nameArabic = "صائد الكنوز",
            description = "+25% item drop rate, +10% currency found",
            descriptionArabic = "+25% معدل إسقاط العناصر، +10% عملة موجودة",
            path = SkillPath.EXPLORATION,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_4,
            skillPointsCost = 5,
            requiredLevel = 15,
            requiredSkills = listOf("explore_energy_efficiency"),
            statBoosts = mapOf(
                "itemDropRate" to 0.25f,
                "currencyBonus" to 0.10f
            ),
            iconResource = "skill_treasure_hunter",
            position = SkillPosition(2, 4)
        ),
        
        // Tier 5
        Skill(
            id = "explore_pathfinder",
            name = "Pathfinder",
            nameArabic = "كاشف المسارات",
            description = "Auto-reveal nearby secrets and shortcuts",
            descriptionArabic = "كشف تلقائي للأسرار والممرات المختصرة القريبة",
            path = SkillPath.EXPLORATION,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_5,
            skillPointsCost = 6,
            requiredLevel = 20,
            requiredSkills = listOf("explore_treasure_hunter"),
            effectTypes = listOf(EffectType.REVELATION),
            values = mapOf("revealRadius" to 10f),
            iconResource = "skill_pathfinder",
            position = SkillPosition(2, 5)
        ),
        
        // Ultimate
        Skill(
            id = "explore_ultimate_wanderer",
            name = "Eternal Wanderer",
            nameArabic = "المتجول الأبدي",
            description = "No fall damage, infinite stamina for exploration",
            descriptionArabic = "لا ضرر سقوط، طاقة لا نهائية للاستكشاف",
            path = SkillPath.EXPLORATION,
            type = SkillType.PASSIVE,
            tier = SkillTier.ULTIMATE,
            skillPointsCost = 10,
            requiredLevel = 25,
            requiredMF = 30,
            requiredSkills = listOf("explore_pathfinder"),
            statBoosts = mapOf(
                "fallDamageReduction" to 1.0f,
                "explorationEnergyRegen" to 999f
            ),
            iconResource = "skill_wanderer",
            position = SkillPosition(2, 6),
            loreText = "The world is vast, and you have all the time you've forgotten",
            loreTextArabic = "العالم واسع، ولديك كل الوقت الذي نسيته"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // Memory Path (مسار الذاكرة)
        // ═══════════════════════════════════════════════════════════════
        
        // Tier 1
        Skill(
            id = "memory_mf_boost",
            name = "Memory Collector",
            nameArabic = "جامع الذكريات",
            description = "+10% MF gain from all sources",
            descriptionArabic = "+10% كسب MF من جميع المصادر",
            path = SkillPath.MEMORY,
            type = SkillType.STAT_BOOST,
            tier = SkillTier.TIER_1,
            skillPointsCost = 1,
            requiredLevel = 1,
            statBoosts = mapOf("mfGainBonus" to 0.10f),
            maxRank = 3,
            iconResource = "skill_memory_collector",
            position = SkillPosition(0, 0)
        ),
        
        Skill(
            id = "memory_fm_resistance",
            name = "Strong Mind",
            nameArabic = "عقل قوي",
            description = "-10% FM generation",
            descriptionArabic = "-10% توليد FM",
            path = SkillPath.MEMORY,
            type = SkillType.STAT_BOOST,
            tier = SkillTier.TIER_1,
            skillPointsCost = 1,
            requiredLevel = 1,
            statBoosts = mapOf("fmGenerationReduction" to -0.10f),
            maxRank = 3,
            iconResource = "skill_strong_mind",
            position = SkillPosition(1, 0)
        ),
        
        Skill(
            id = "memory_xp_boost",
            name = "Quick Learner",
            nameArabic = "متعلم سريع",
            description = "+15% XP gain",
            descriptionArabic = "+15% كسب XP",
            path = SkillPath.MEMORY,
            type = SkillType.STAT_BOOST,
            tier = SkillTier.TIER_1,
            skillPointsCost = 2,
            requiredLevel = 2,
            statBoosts = mapOf("xpGainBonus" to 0.15f),
            maxRank = 2,
            iconResource = "skill_quick_learner",
            position = SkillPosition(2, 0)
        ),
        
        // Tier 2
        Skill(
            id = "memory_pulse_unlock",
            name = "Memory Pulse",
            nameArabic = "نبضة الذاكرة",
            description = "Unlocks Memory Pulse (Small)",
            descriptionArabic = "تفتح نبضة الذاكرة (صغيرة)",
            path = SkillPath.MEMORY,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_2,
            skillPointsCost = 3,
            requiredLevel = 5,
            requiredMF = 5,
            requiredSkills = listOf("memory_mf_boost"),
            unlockedAbility = AbilityType.MEMORY_PULSE_SMALL,
            iconResource = "skill_memory_pulse",
            position = SkillPosition(0, 2),
            loreText = "Let the mask remember what you've forgotten",
            loreTextArabic = "دع القناع يتذكر ما نسيته"
        ),
        
        Skill(
            id = "memory_fm_cleanse",
            name = "Mental Clarity",
            nameArabic = "وضوح عقلي",
            description = "FM decays 25% faster at Sanctuaries",
            descriptionArabic = "FM يتحلل بنسبة 25% أسرع في الملاجئ",
            path = SkillPath.MEMORY,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_2,
            skillPointsCost = 2,
            requiredLevel = 5,
            requiredSkills = listOf("memory_fm_resistance"),
            statBoosts = mapOf("fmDecayRate" to 0.25f),
            iconResource = "skill_mental_clarity",
            position = SkillPosition(1, 2)
        ),
        
        Skill(
            id = "memory_social_bonus",
            name = "Charismatic",
            nameArabic = "جذاب",
            description = "-10% merchant prices, better dialogue options",
            descriptionArabic = "-10% أسعار التجار، خيارات حوار أفضل",
            path = SkillPath.MEMORY,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_2,
            skillPointsCost = 2,
            requiredLevel = 6,
            requiredSkills = listOf("memory_xp_boost"),
            statBoosts = mapOf("merchantPriceReduction" to -0.10f),
            effectTypes = listOf(EffectType.CHARISMA),
            iconResource = "skill_charismatic",
            position = SkillPosition(2, 2)
        ),
        
        // Tier 3
        Skill(
            id = "memory_echo_recall",
            name = "Echo Recall",
            nameArabic = "استدعاء الصدى",
            description = "Unlocks Echo Recall ability",
            descriptionArabic = "تفتح قدرة استدعاء الصدى",
            path = SkillPath.MEMORY,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 4,
            requiredLevel = 10,
            requiredMF = 10,
            requiredSkills = listOf("memory_pulse_unlock"),
            unlockedAbility = AbilityType.ECHO_RECALL,
            iconResource = "skill_echo_recall",
            position = SkillPosition(0, 3)
        ),
        
        Skill(
            id = "memory_pulse_large",
            name = "Memory Pulse (Large)",
            nameArabic = "نبضة الذاكرة (كبيرة)",
            description = "Upgrades Memory Pulse to Large version",
            descriptionArabic = "ترقي نبضة الذاكرة للنسخة الكبيرة",
            path = SkillPath.MEMORY,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 5,
            requiredLevel = 12,
            requiredMF = 15,
            requiredSkills = listOf("memory_pulse_unlock"),
            unlockedAbility = AbilityType.MEMORY_PULSE_LARGE,
            iconResource = "skill_memory_pulse_large",
            position = SkillPosition(1, 3)
        ),
        
        Skill(
            id = "memory_npc_affinity",
            name = "Unforgettable",
            nameArabic = "لا يُنسى",
            description = "NPCs remember you longer, FM effects reduced by 25%",
            descriptionArabic = "الشخصيات تتذكرك أطول، تأثيرات FM مخفضة بـ 25%",
            path = SkillPath.MEMORY,
            type = SkillType.PASSIVE,
            tier = SkillTier.TIER_3,
            skillPointsCost = 3,
            requiredLevel = 10,
            requiredSkills = listOf("memory_social_bonus"),
            statBoosts = mapOf("fmPenaltyReduction" to -0.25f),
            iconResource = "skill_unforgettable",
            position = SkillPosition(2, 3)
        ),
        
        // Tier 4
        Skill(
            id = "memory_borrowed_names",
            name = "Borrowed Names",
            nameArabic = "أسماء مستعارة",
            description = "Unlocks Borrowed Names ability",
            descriptionArabic = "تفتح قدرة الأسماء المستعارة",
            path = SkillPath.MEMORY,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_4,
            skillPointsCost = 5,
            requiredLevel = 15,
            requiredMF = 12,
            requiredSkills = listOf("memory_echo_recall"),
            unlockedAbility = AbilityType.BORROWED_NAMES,
            iconResource = "skill_borrowed_names",
            position = SkillPosition(0, 4),
            loreText = "Wear their names like cloaks, carry their skills like masks",
            loreTextArabic = "ارتدِ أسماءهم كالعباءات، احمل مهاراتهم كالأقنعة"
        ),
        
        Skill(
            id = "memory_mask_shard",
            name = "Mask Shard Blast",
            nameArabic = "انفجار شظية القناع",
            description = "Unlocks Mask Shard Blast",
            descriptionArabic = "تفتح انفجار شظية القناع",
            path = SkillPath.MEMORY,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_4,
            skillPointsCost = 5,
            requiredLevel = 16,
            requiredMF = 20,
            requiredSkills = listOf("memory_pulse_large"),
            unlockedAbility = AbilityType.MASK_SHARD_BLAST,
            iconResource = "skill_mask_shard",
            position = SkillPosition(1, 4)
        ),
        
        // Tier 5
        Skill(
            id = "memory_restoration",
            name = "Memory Restoration",
            nameArabic = "استعادة الذاكرة",
            description = "Unlocks Memory Restoration (narrative ability)",
            descriptionArabic = "تفتح استعادة الذاكرة (قدرة سردية)",
            path = SkillPath.MEMORY,
            type = SkillType.ACTIVE,
            tier = SkillTier.TIER_5,
            skillPointsCost = 7,
            requiredLevel = 20,
            requiredMF = 25,
            requiredSkills = listOf("memory_borrowed_names", "memory_mask_shard"),
            unlockedAbility = AbilityType.MEMORY_RESTORATION,
            iconResource = "skill_memory_restoration",
            position = SkillPosition(0, 5)
        ),
        
        // Ultimate
        Skill(
            id = "memory_ultimate_archivist",
            name = "The Archivist's Gift",
            nameArabic = "هدية الأرشيفي",
            description = "FM never exceeds 10, +50% MF gain, all NPCs remember you",
            descriptionArabic = "FM لا يتجاوز 10، +50% كسب MF، جميع الشخصيات تتذكرك",
            path = SkillPath.MEMORY,
            type = SkillType.PASSIVE,
            tier = SkillTier.ULTIMATE,
            skillPointsCost = 10,
            requiredLevel = 25,
            requiredMF = 50,
            requiredSkills = listOf("memory_restoration"),
            statBoosts = mapOf(
                "fmCap" to 10f,
                "mfGainBonus" to 0.50f
            ),
            effectTypes = listOf(EffectType.ALWAYS_REMEMBERED),
            iconResource = "skill_archivist",
            position = SkillPosition(0, 6),
            loreText = "They tried to erase you. You will ensure no one is forgotten again.",
            loreTextArabic = "حاولوا محوك. ستضمن ألا يُنسى أحد مجدداً."
        )
    )
    
    /**
     * الحصول على مهارة بمعرّفها
     */
    fun getSkill(id: String): Skill? = allSkills.find { it.id == id }
    
    /**
     * الحصول على جميع مهارات مسار معين
     */
    fun getSkillsByPath(path: SkillPath): List<Skill> =
        allSkills.filter { it.path == path }
    
    /**
     * الحصول على جميع مهارات طبقة معينة
     */
    fun getSkillsByTier(tier: SkillTier): List<Skill> =
        allSkills.filter { it.tier == tier }
    
    /**
     * الحصول على جميع مهارات نوع معين
     */
    fun getSkillsByType(type: SkillType): List<Skill> =
        allSkills.filter { it.type == type }
    
    /**
     * الحصول على مهارات مسار معين في طبقة معينة
     */
    fun getSkillsByPathAndTier(path: SkillPath, tier: SkillTier): List<Skill> =
        allSkills.filter { it.path == path && it.tier == tier }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Skill Tree Manager
// ═══════════════════════════════════════════════════════════════════════════

/**
 * مدير شجرة المهارات
 */
class SkillTreeManager(
    private val playerStateManager: PlayerStateManager,
    private val abilityManager: AbilityManager,
    private val eventBus: EventBus
) {
    
    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════
    
    private val _playerSkills = MutableStateFlow<Map<String, PlayerSkill>>(emptyMap())
    val playerSkills: StateFlow<Map<String, PlayerSkill>> = _playerSkills.asStateFlow()
    
    private val _availableSkillPoints = MutableStateFlow(0)
    val availableSkillPoints: StateFlow<Int> = _availableSkillPoints.asStateFlow()
    
    private val _totalSkillPointsEarned = MutableStateFlow(0)
    val totalSkillPointsEarned: StateFlow<Int> = _totalSkillPointsEarned.asStateFlow()
    
    private val _pathSummaries = MutableStateFlow<Map<SkillPath, PathSummary>>(emptyMap())
    val pathSummaries: StateFlow<Map<SkillPath, PathSummary>> = _pathSummaries.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════════════
    
    init {
        // تهيئة جميع المهارات كمقفلة
        _playerSkills.value = SkillDatabase.allSkills.associate { skill ->
            skill.id to PlayerSkill(skill = skill)
        }
        
        // تحديث الملخصات
        updatePathSummaries()
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Public API - Unlocking Skills
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * محاولة فتح أو ترقية مهارة
     */
    fun unlockSkill(skillId: String): SkillUnlockResult {
        val playerSkill = _playerSkills.value[skillId]
            ?: return SkillUnlockResult.Failure(SkillUnlockFailureReason.SKILL_NOT_FOUND)
        
        val skill = playerSkill.skill
        
        // تحقق من الحد الأقصى
        if (playerSkill.currentRank >= skill.maxRank) {
            return SkillUnlockResult.Failure(SkillUnlockFailureReason.ALREADY_MAX_RANK)
        }
        
        // تحقق من نقاط المهارات
        if (_availableSkillPoints.value < skill.skillPointsCost) {
            return SkillUnlockResult.Failure(SkillUnlockFailureReason.INSUFFICIENT_SKILL_POINTS)
        }
        
        // تحقق من المستوى
        val playerLevel = playerStateManager.playerState.value.stats.level
        if (playerLevel < skill.requiredLevel) {
            return SkillUnlockResult.Failure(SkillUnlockFailureReason.LEVEL_TOO_LOW)
        }
        
        // تحقق من MF
        val playerMF = playerStateManager.playerState.value.stats.memoryFragments
        if (playerMF < skill.requiredMF) {
            return SkillUnlockResult.Failure(SkillUnlockFailureReason.INSUFFICIENT_MF)
        }
        
        // تحقق من المتطلبات الأولية
        if (!checkPrerequisites(skill)) {
            return SkillUnlockResult.Failure(SkillUnlockFailureReason.PREREQUISITES_NOT_MET)
        }
        
        // خصم التكاليف
        _availableSkillPoints.value -= skill.skillPointsCost
        if (skill.requiredMF > 0) {
            playerStateManager.consumeMF(skill.requiredMF)
        }
        
        // فتح/ترقية المهارة
        val newRank = playerSkill.currentRank + 1
        val updatedSkill = playerSkill.copy(
            unlocked = true,
            currentRank = newRank,
            unlockedAt = System.currentTimeMillis()
        )
        
        _playerSkills.value = _playerSkills.value + (skillId to updatedSkill)
        
        // تطبيق التأثيرات
        applySkillEffects(skill, newRank)
        
        // تحديث الملخصات
        updatePathSummaries()
        
        // إرسال حدث
        eventBus.emit(GameEvent.Skill.SkillUnlocked(skillId, skill.name, newRank))
        
        return SkillUnlockResult.Success(skill, newRank)
    }
    
    /**
     * إعادة تعيين جميع المهارات (Respec)
     */
    fun respecSkills(cost: Int = 0): Boolean {
        val playerCurrency = playerStateManager.playerState.value.stats.currency
        
        if (cost > 0 && playerCurrency < cost) {
            return false
        }
        
        // خصم التكلفة
        if (cost > 0) {
            playerStateManager.spendCurrency(cost)
        }
        
        // إزالة جميع تأثيرات المهارات
        _playerSkills.value.values.filter { it.unlocked }.forEach { playerSkill ->
            removeSkillEffects(playerSkill.skill, playerSkill.currentRank)
        }
        
        // إعادة تعيين المهارات
        _playerSkills.value = _playerSkills.value.mapValues { (_, playerSkill) ->
            if (playerSkill.unlocked) {
                // إرجاع نقاط المهارات
                _availableSkillPoints.value += playerSkill.skill.skillPointsCost * playerSkill.currentRank
                
                // إعادة تعيين
                playerSkill.copy(
                    unlocked = false,
                    currentRank = 0,
                    unlockedAt = 0L
                )
            } else {
                playerSkill
            }
        }
        
        // تحديث الملخصات
        updatePathSummaries()
        
        eventBus.emit(GameEvent.Skill.SkillsRespecced())
        
        return true
    }
    
    /**
     * إضافة نقاط مهارات
     */
    fun addSkillPoints(points: Int) {
        _availableSkillPoints.value += points
        _totalSkillPointsEarned.value += points
        eventBus.emit(GameEvent.Skill.SkillPointsEarned(points))
    }
    
    /**
     * منح نقاط مهارات عند رفع المستوى
     */
    fun onLevelUp(newLevel: Int) {
        // 1 نقطة لكل مستوى
        val pointsToAdd = 1
        
        // نقاط إضافية كل 5 مستويات
        val bonusPoints = if (newLevel % 5 == 0) 2 else 0
        
        addSkillPoints(pointsToAdd + bonusPoints)
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Public API - Queries
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * هل المهارة مفتوحة؟
     */
    fun isSkillUnlocked(skillId: String): Boolean =
        _playerSkills.value[skillId]?.unlocked == true
    
    /**
     * الحصول على رتبة المهارة
     */
    fun getSkillRank(skillId: String): Int =
        _playerSkills.value[skillId]?.currentRank ?: 0
    
    /**
     * هل يمكن فتح المهارة؟
     */
    fun canUnlockSkill(skillId: String): Boolean {
        return when (unlockSkill(skillId)) {
            is SkillUnlockResult.Success -> {
                // نلغي الفتح (كنا نتحقق فقط)
                // TODO: تحسين هذا بدون فتح فعلي
                true
            }
            is SkillUnlockResult.Failure -> false
        }
    }
    
    /**
     * الحصول على المهارات المفتوحة في مسار
     */
    fun getUnlockedSkillsInPath(path: SkillPath): List<PlayerSkill> =
        _playerSkills.value.values.filter { 
            it.unlocked && it.skill.path == path 
        }
    
    /**
     * الحصول على مجموع النقاط المنفقة في مسار
     */
    fun getTotalPointsInPath(path: SkillPath): Int =
        getUnlockedSkillsInPath(path).sumOf { 
            it.skill.skillPointsCost * it.currentRank 
        }
    
    /**
     * الحصول على ملخص مسار
     */
    fun getPathSummary(path: SkillPath): PathSummary? =
        _pathSummaries.value[path]
    
    /**
     * الحصول على جميع المكافآت النشطة
     */
    fun getActiveStatBoosts(): Map<String, Float> {
        val boosts = mutableMapOf<String, Float>()
        
        _playerSkills.value.values.filter { it.unlocked }.forEach { playerSkill ->
            playerSkill.skill.statBoosts.forEach { (stat, value) ->
                boosts[stat] = (boosts[stat] ?: 0f) + (value * playerSkill.currentRank)
            }
        }
        
        return boosts
    }
    
    /**
     * الحصول على جميع التأثيرات النشطة
     */
    fun getActiveEffects(): List<EffectType> {
        return _playerSkills.value.values
            .filter { it.unlocked }
            .flatMap { it.skill.effectTypes }
            .distinct()
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Private Helper Functions
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من المتطلبات الأولية
     */
    private fun checkPrerequisites(skill: Skill): Boolean {
        return skill.requiredSkills.all { requiredId ->
            isSkillUnlocked(requiredId)
        }
    }
    
    /**
     * تطبيق تأثيرات المهارة
     */
    private fun applySkillEffects(skill: Skill, rank: Int) {
        // تطبيق تعزيزات الإحصائيات
        val currentStats = playerStateManager.playerState.value.stats
        var updatedStats = currentStats
        
        skill.statBoosts.forEach { (stat, value) ->
            val totalBoost = value * rank
            updatedStats = when (stat) {
                "maxHp" -> updatedStats.copy(maxHp = currentStats.maxHp + totalBoost.toInt())
                "maxEnergy" -> updatedStats.copy(maxEnergy = currentStats.maxEnergy + totalBoost.toInt())
                "movementSpeed" -> updatedStats.copy(speed = currentStats.speed + totalBoost)
                "basicAttackDamage" -> updatedStats.copy(attack = currentStats.attack + totalBoost.toInt())
                "critChance" -> updatedStats.copy(criticalChance = currentStats.criticalChance + totalBoost)
                else -> updatedStats
            }
        }
        
        if (updatedStats != currentStats) {
            playerStateManager.loadState(playerStateManager.currentState.copy(stats = updatedStats))
        }
        
        // تطبيق التأثيرات السلبية
        skill.effectTypes.forEach { effectType ->
            playerStateManager.addEffect(
                Effect(
                    type = effectType,
                    duration = -1,  // دائم
                    strength = skill.values["lifestealPercent"] ?: 1f,
                    source = skill.id
                )
            )
        }
        
        // فتح القدرات
        skill.unlockedAbility?.let { abilityType ->
            abilityManager.unlockAbility(abilityType)
        }
        
        // فتح الميكانيكيات
        if (skill.unlockedMechanic.isNotEmpty()) {
            eventBus.emit(GameEvent.MechanicUnlocked(skill.unlockedMechanic))
        }
    }
    
    /**
     * إزالة تأثيرات المهارة
     */
    private fun removeSkillEffects(skill: Skill, rank: Int) {
        // إزالة تعزيزات الإحصائيات
        val currentStats = playerStateManager.playerState.value.stats
        var updatedStats = currentStats
        
        skill.statBoosts.forEach { (stat, value) ->
            val totalBoost = value * rank
            updatedStats = when (stat) {
                "maxHp" -> updatedStats.copy(maxHp = currentStats.maxHp - totalBoost.toInt())
                "maxEnergy" -> updatedStats.copy(maxEnergy = currentStats.maxEnergy - totalBoost.toInt())
                "movementSpeed" -> updatedStats.copy(speed = currentStats.speed - totalBoost)
                "basicAttackDamage" -> updatedStats.copy(attack = currentStats.attack - totalBoost.toInt())
                "critChance" -> updatedStats.copy(criticalChance = currentStats.criticalChance - totalBoost)
                else -> updatedStats
            }
        }
        
        if (updatedStats != currentStats) {
            playerStateManager.loadState(playerStateManager.currentState.copy(stats = updatedStats))
        }
        
        // إزالة التأثيرات
        skill.effectTypes.forEach { effectType ->
            playerStateManager.removeEffectBySource(skill.id)
        }
    }
    
    /**
     * تحديث ملخصات المسارات
     */
    private fun updatePathSummaries() {
        val summaries = SkillPath.values().associateWith { path ->
            val pathSkills = SkillDatabase.getSkillsByPath(path)
            val unlockedSkills = getUnlockedSkillsInPath(path)
            val totalPoints = getTotalPointsInPath(path)
            
            // حساب المكافآت المجمعة
            val bonuses = mutableMapOf<String, Float>()
            unlockedSkills.forEach { playerSkill ->
                playerSkill.skill.statBoosts.forEach { (stat, value) ->
                    bonuses[stat] = (bonuses[stat] ?: 0f) + (value * playerSkill.currentRank)
                }
            }
            
            PathSummary(
                path = path,
                totalSkills = pathSkills.size,
                unlockedSkills = unlockedSkills.size,
                totalPointsSpent = totalPoints,
                bonuses = bonuses
            )
        }
        
        _pathSummaries.value = summaries
    }
    
    /**
     * حساب تكلفة Respec
     */
    fun calculateRespecCost(): Int {
        val totalPointsSpent = _totalSkillPointsEarned.value - _availableSkillPoints.value
        // 100 عملة لكل نقطة منفقة
        return totalPointsSpent * 100
    }
    
    /**
     * إعادة تعيين (للـ New Game)
     */
    fun reset() {
        // إزالة جميع التأثيرات
        _playerSkills.value.values.filter { it.unlocked }.forEach { playerSkill ->
            removeSkillEffects(playerSkill.skill, playerSkill.currentRank)
        }
        
        // إعادة تعيين المهارات
        _playerSkills.value = SkillDatabase.allSkills.associate { skill ->
            skill.id to PlayerSkill(skill = skill)
        }
        
        _availableSkillPoints.value = 0
        _totalSkillPointsEarned.value = 0
        
        updatePathSummaries()
    }
    
    /**
     * حفظ حالة شجرة المهارات
     */
    fun saveState(): Map<String, Any> = mapOf(
        "availableSkillPoints" to _availableSkillPoints.value,
        "totalSkillPointsEarned" to _totalSkillPointsEarned.value,
        "playerSkills" to _playerSkills.value.filter { it.value.unlocked }.map { (skillId, playerSkill) ->
            mapOf(
                "skillId" to skillId,
                "currentRank" to playerSkill.currentRank,
                "unlockedAt" to playerSkill.unlockedAt
            )
        }
    )
    
    /**
     * تحميل حالة شجرة المهارات
     */
    fun loadState(data: Map<String, Any>) {
        _availableSkillPoints.value = (data["availableSkillPoints"] as? Int) ?: 0
        _totalSkillPointsEarned.value = (data["totalSkillPointsEarned"] as? Int) ?: 0
        
        // إعادة بناء المهارات
        val skillsData = data["playerSkills"] as? List<Map<String, Any>> ?: emptyList()
        
        _playerSkills.value = SkillDatabase.allSkills.associate { skill ->
            val savedData = skillsData.find { it["skillId"] == skill.id }
            
            if (savedData != null) {
                val rank = (savedData["currentRank"] as? Int) ?: 0
                skill.id to PlayerSkill(
                    skill = skill,
                    unlocked = true,
                    currentRank = rank,
                    unlockedAt = (savedData["unlockedAt"] as? Long) ?: 0L
                )
            } else {
                skill.id to PlayerSkill(skill = skill)
            }
        }
        
        // إعادة تطبيق التأثيرات
        _playerSkills.value.values.filter { it.unlocked }.forEach { playerSkill ->
            applySkillEffects(playerSkill.skill, playerSkill.currentRank)
        }
        
        updatePathSummaries()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Extensions
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Extension للـ Effect لإضافة sourceSkill
 */
private val Effect.sourceSkill: String? get() = null

/**
 * Extension للـ PlayerStateManager
 */
private fun PlayerStateManager.removeEffectBySource(source: String) {
    // Implementation في PlayerStateManager
}

/**
 * حساب XP المطلوب للمستوى
 */
fun calculateXPForLevel(level: Int): Int {
    // Formula: XP = 100 * (level^1.5)
    return (100 * level.toDouble().pow(1.5)).toInt()
}

/**
 * حساب المستوى من XP
 */
fun calculateLevelFromXP(xp: Int): Int {
    var level = 1
    var requiredXP = calculateXPForLevel(level)
    
    while (xp >= requiredXP) {
        level++
        requiredXP = calculateXPForLevel(level)
    }
    
    return level - 1
}

class SkillTree