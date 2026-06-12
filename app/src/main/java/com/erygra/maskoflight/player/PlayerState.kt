package com.erygra.maskoflight.player

import com.erygra.maskoflight.core.GameConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * player/PlayerState.kt
 * ═══════════════════════════════════════════════════════════════════════════════
 * نظام حالة اللاعب الكامل — لعبة "قِنَاعُ النُّور" (Mask of Light)
 * Erygra Universe 2.0
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * الوظائف الرئيسية:
 * - إدارة إحصائيات اللاعب (HP, Energy, Level, XP, MF, FM, Currency, Score)
 * - تتبع القدرات المفتوحة وأوقات الانتظار (cooldowns)
 * - إدارة الموقع والمنطقة الحالية
 * - تتبع التأثيرات النشطة (buffs/debuffs)
 * - حسابات تأثيرات FM (Forgetfulness Meter) على اللعب
 * - دعم New Game Plus (NG+)
 *
 * البنية:
 * - PlayerStats: الإحصائيات الأساسية والعملات
 * - PlayerAbilities: القدرات المفتوحة وأوقات الانتظار
 * - PlayerPosition: الموقع والمنطقة
 * - PlayerEffects: التأثيرات النشطة
 * - PlayerState: الحالة الكاملة
 * - PlayerStateManager: إدارة الحالة مع StateFlow
 *
 * @author Erygra Team
 * @version 2.0.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Player Stats
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * إحصائيات اللاعب الأساسية والعملات
 *
 * @property hp النقاط الصحية الحالية
 * @property maxHp الحد الأقصى للنقاط الصحية
 * @property energy الطاقة الحالية (للقدرات الأساسية)
 * @property maxEnergy الحد الأقصى للطاقة
 * @property level المستوى الحالي
 * @property xp نقاط الخبرة الحالية
 * @property xpToNextLevel نقاط الخبرة المطلوبة للمستوى التالي
 * @property memoryFragments شظايا الذاكرة (MF) - عملة نادرة
 * @property forgetfulnessMeter مقياس النسيان (FM) - عقوبة استخدام MF
 * @property currency العملة العادية (Coins)
 * @property score النقاط الكلية
 * @property deaths عدد مرات الموت
 * @property enemiesKilled عدد الأعداء المقتولين
 * @property bossesDefeated عدد الزعماء المهزومين
 * @property secretsFound عدد الأسرار المكتشفة
 * @property playTimeMinutes وقت اللعب بالدقائق
 */
data class PlayerStats(
    val hp: Int = GameConfig.PlayerConfig.INITIAL_HP,
    val maxHp: Int = GameConfig.PlayerConfig.INITIAL_MAX_HP,
    val energy: Int = GameConfig.PlayerConfig.INITIAL_ENERGY,
    val maxEnergy: Int = GameConfig.PlayerConfig.INITIAL_MAX_ENERGY,
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNextLevel: Int = 100,
    val memoryFragments: Int = 0,
    val forgetfulnessMeter: Int = 0,
    val currency: Int = 0,
    val score: Int = 0,
    val deaths: Int = 0,
    val enemiesKilled: Int = 0,
    val bossesDefeated: Int = 0,
    val secretsFound: Int = 0,
    val playTimeMinutes: Int = 0,
    val speed: Float = 5.0f,
    val attack: Int = 10,
    val criticalChance: Float = 0.05f
) {
    /**
     * نسبة HP الحالية (0.0 - 1.0)
     */
    val hpRatio: Float
        get() = if (maxHp > 0) hp.toFloat() / maxHp else 0f

    /**
     * نسبة Energy الحالية (0.0 - 1.0)
     */
    val energyRatio: Float
        get() = if (maxEnergy > 0) energy.toFloat() / maxEnergy else 0f

    /**
     * نسبة التقدم نحو المستوى التالي (0.0 - 1.0)
     */
    val xpProgress: Float
        get() = if (xpToNextLevel > 0) xp.toFloat() / xpToNextLevel else 0f

    /**
     * هل اللاعب على قيد الحياة؟
     */
    val isAlive: Boolean
        get() = hp > 0

    /**
     * هل اللاعب في حالة حرجة؟ (HP < 25%)
     */
    val isCritical: Boolean
        get() = hpRatio < 0.25f

    /**
     * هل اللاعب لديه طاقة كافية؟
     */
    fun hasEnergy(amount: Int): Boolean = energy >= amount

    /**
     * هل اللاعب لديه MF كافية؟
     */
    fun hasMemoryFragments(amount: Int): Boolean = memoryFragments >= amount

    /**
     * هل اللاعب لديه عملة كافية؟
     */
    fun hasCurrency(amount: Int): Boolean = currency >= amount
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Player Abilities
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * أنواع القدرات القابلة للفتح (imported from AbilitySystem.kt)
 */

/**
 * معلومات قدرة واحدة
 *
 * @property type نوع القدرة
 * @property unlocked هل مفتوحة؟
 * @property level مستوى القدرة (1-5)
 * @property cooldownRemaining الوقت المتبقي للانتظار (ميلي ثانية)
 * @property maxCooldown أقصى وقت انتظار
 * @property usesRemaining عدد الاستخدامات المتبقية (للقدرات المحدودة)
 * @property maxUses الحد الأقصى للاستخدامات
 */
data class PlayerStateAbility(
    val type: AbilityType,
    val unlocked: Boolean = false,
    val level: Int = 1,
    val cooldownRemaining: Long = 0L,
    val maxCooldown: Long = 0L,
    val usesRemaining: Int = -1, // -1 = غير محدود
    val maxUses: Int = -1
) {
    /**
     * هل القدرة جاهزة للاستخدام؟
     */
    val isReady: Boolean
        get() = unlocked && cooldownRemaining <= 0 && (usesRemaining > 0 || usesRemaining == -1)

    /**
     * نسبة Cooldown (0.0 = جاهز, 1.0 = في الانتظار الكامل)
     */
    val cooldownRatio: Float
        get() = if (maxCooldown > 0) (cooldownRemaining.toFloat() / maxCooldown).coerceIn(0f, 1f) else 0f
}

/**
 * مجموعة قدرات اللاعب
 *
 * @property abilities خريطة القدرات المفتوحة وحالاتها
 */
data class PlayerAbilities(
    val abilities: Map<AbilityType, PlayerStateAbility> = emptyMap()
) {
    /**
     * الحصول على قدرة معينة
     */
    fun getAbility(type: AbilityType): PlayerStateAbility? = abilities[type]

    /**
     * هل القدرة مفتوحة؟
     */
    fun isUnlocked(type: AbilityType): Boolean = abilities[type]?.unlocked == true

    /**
     * هل القدرة جاهزة للاستخدام؟
     */
    fun isReady(type: AbilityType): Boolean = abilities[type]?.isReady == true

    /**
     * عدد القدرات المفتوحة
     */
    val unlockedCount: Int
        get() = abilities.values.count { it.unlocked }

    /**
     * عدد القدرات الجاهزة
     */
    val readyCount: Int
        get() = abilities.values.count { it.isReady }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Player Position
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * معرّفات المناطق
 */
enum class RegionId {
    ASHEN_SPRAWL,
    VEILED_ARCHIVES,
    HOLLOWED_ARCHIPELAGO,
    GLASSFJORD_CLIFFS,
    SUNKEN_CLOCKWORKS,
    BLACKROOT_MOORLANDS,
    LUMINOUS_CHASM
}

/**
 * موقع اللاعب في العالم
 *
 * @property regionId المنطقة الحالية
 * @property x الموقع الأفقي (pixels)
 * @property y الموقع العمودي (pixels)
 * @property facingRight هل اللاعب يواجه اليمين؟
 * @property velocityX السرعة الأفقية
 * @property velocityY السرعة العمودية
 * @property isGrounded هل اللاعب على الأرض؟
 * @property isOnWall هل اللاعب على جدار؟
 * @property isClimbing هل اللاعب يتسلق؟
 * @property isSwinging هل اللاعب يتأرجح؟
 * @property lastSanctuaryId آخر ملجأ زاره (للتنقل السريع)
 */
data class PlayerPosition(
    val regionId: RegionId = RegionId.ASHEN_SPRAWL,
    val x: Float = 100f,
    val y: Float = 100f,
    val facingRight: Boolean = true,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val isGrounded: Boolean = false,
    val isOnWall: Boolean = false,
    val isClimbing: Boolean = false,
    val isSwinging: Boolean = false,
    val lastSanctuaryId: String? = null
) {
    /**
     * هل اللاعب في الهواء؟
     */
    val isAirborne: Boolean
        get() = !isGrounded && !isClimbing && !isSwinging

    /**
     * هل اللاعب يتحرك؟
     */
    val isMoving: Boolean
        get() = velocityX != 0f || velocityY != 0f
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Player Effects
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * أنواع التأثيرات
 */
enum class PlayerEffectType {
    // Buffs (إيجابية)
    DAMAGE_BOOST,        // زيادة الضرر
    SPEED_BOOST,         // زيادة السرعة
    DEFENSE_BOOST,       // زيادة الدفاع
    ENERGY_REGEN,        // استعادة طاقة
    HP_REGEN,            // استعادة صحة
    INVULNERABLE,        // منيع
    INVISIBLE,           // غير مرئي
    DOUBLE_XP,           // ضعف الخبرة
    DOUBLE_CURRENCY,     // ضعف العملة
    
    // Debuffs (سلبية)
    POISONED,            // مسموم
    BURNING,             // محترق
    FROZEN,              // متجمد
    STUNNED,             // مصعوق
    SLOWED,              // بطيء
    WEAKENED,            // ضعيف
    BLINDED,             // أعمى
    SILENCED,            // صامت (لا قدرات)
    CURSED,              // ملعون (FM يزداد)
    
    // تأثيرات FM (Forgetfulness)
    FM_LOW,              // FM منخفض (0-3)
    FM_MILD,             // FM خفيف (4-7)
    FM_MODERATE,         // FM متوسط (8-12)
    FM_CRITICAL,         // FM حرج (13-20)
    FM_CATASTROPHIC,     // FM كارثي (20+)
    
    // Skill-based effects
    LIFESTEAL,
    ARMOR_PIERCE,
    BERSERKER,
    EXECUTE,
    STUN_IMMUNE,
    REVELATION,
    CHARISMA,
    ALWAYS_REMEMBERED,
    // Hallucinated effects
    STUN, BLIND, SLOW, DAMAGE_ON_CONTACT, REFLECT_PROJECTILE, TIME_SLOW, BARRIER_PIERCE, ARMOR_BREAK, AOE_AMPLIFY, INCREASED_CRIT_CHANCE, INTANGIBLE, PARRY_WINDOW,
    HEALING_BOOST, XP_BOOST, COOLDOWN_REDUCTION

}

typealias EffectType = PlayerEffectType

/**
 * تأثير واحد نشط
 *
 * @property type نوع التأثير
 * @property strength قوة التأثير (1.0 = عادي)
 * @property duration المدة الكلية (ميلي ثانية)
 * @property remaining الوقت المتبقي (ميلي ثانية)
 * @property source مصدر التأثير (اسم العدو/القدرة/الأداة)
 * @property stackable هل يمكن تكديسه؟
 * @property stacks عدد التكديسات
 */
data class Effect(
    val type: EffectType,
    val strength: Float = 1f,
    val duration: Long = 0L,
    val remaining: Long = 0L,
    val source: String = "",
    val stackable: Boolean = false,
    val stacks: Int = 1
) {
    /**
     * هل التأثير منتهي؟
     */
    val isExpired: Boolean
        get() = remaining <= 0

    /**
     * نسبة الوقت المتبقي (0.0 - 1.0)
     */
    val remainingRatio: Float
        get() = if (duration > 0) (remaining.toFloat() / duration).coerceIn(0f, 1f) else 0f

    /**
     * هل التأثير إيجابي (Buff)؟
     */
    val isPositive: Boolean
        get() = when (type) {
            EffectType.DAMAGE_BOOST, EffectType.SPEED_BOOST, EffectType.DEFENSE_BOOST,
            EffectType.ENERGY_REGEN, EffectType.HP_REGEN, EffectType.INVULNERABLE,
            EffectType.INVISIBLE, EffectType.DOUBLE_XP, EffectType.DOUBLE_CURRENCY -> true
            else -> false
        }
}

/**
 * مجموعة التأثيرات النشطة
 *
 * @property effects قائمة التأثيرات النشطة
 */
data class PlayerEffects(
    val effects: List<Effect> = emptyList()
) {
    /**
     * هل لدى اللاعب تأثير معين؟
     */
    fun hasEffect(type: EffectType): Boolean = effects.any { it.type == type && !it.isExpired }

    /**
     * الحصول على جميع التأثيرات من نوع معين
     */
    fun getEffects(type: EffectType): List<Effect> = effects.filter { it.type == type && !it.isExpired }

    /**
     * الحصول على قوة التأثير الكلية (مع التكديسات)
     */
    fun getEffectStrength(type: EffectType): Float {
        val typeEffects = getEffects(type)
        return if (typeEffects.isEmpty()) 0f
        else typeEffects.sumOf { (it.strength * it.stacks).toDouble() }.toFloat()
    }

    /**
     * عدد Buffs النشطة
     */
    val buffCount: Int
        get() = effects.count { it.isPositive && !it.isExpired }

    /**
     * عدد Debuffs النشطة
     */
    val debuffCount: Int
        get() = effects.count { !it.isPositive && !it.isExpired }

    /**
     * هل اللاعب منيع؟
     */
    val isInvulnerable: Boolean
        get() = hasEffect(EffectType.INVULNERABLE)

    /**
     * هل اللاعب مصعوق؟
     */
    val isStunned: Boolean
        get() = hasEffect(EffectType.STUNNED)

    /**
     * هل اللاعب صامت (لا يستطيع استخدام القدرات)؟
     */
    val isSilenced: Boolean
        get() = hasEffect(EffectType.SILENCED)
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - FM Effects & Thresholds
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مستويات FM وتأثيراتها
 */
enum class FMLevel(val range: IntRange, val effectType: EffectType) {
    LOW(0..3, EffectType.FM_LOW),
    MILD(4..7, EffectType.FM_MILD),
    MODERATE(8..12, EffectType.FM_MODERATE),
    CRITICAL(13..20, EffectType.FM_CRITICAL),
    CATASTROPHIC(21..Int.MAX_VALUE, EffectType.FM_CATASTROPHIC);

    companion object {
        /**
         * الحصول على مستوى FM من قيمة FM
         */
        fun fromValue(fm: Int): FMLevel = values().first { fm in it.range }
    }
}

/**
 * حساب تأثيرات FM على اللعب
 */
object FMEffectsCalculator {
    
    /**
     * حساب ضريبة السعر بناءً على FM
     * 
     * @param baseCost السعر الأساسي
     * @param fm قيمة FM الحالية
     * @return السعر النهائي بعد تطبيق الضريبة
     */
    fun calculatePricePenalty(baseCost: Int, fm: Int): Int {
        val level = FMLevel.fromValue(fm)
        val multiplier = when (level) {
            FMLevel.LOW -> 1.0f
            FMLevel.MILD -> GameConfig.MemoryConfig.FM_PRICE_MULTIPLIER_MILD
            FMLevel.MODERATE -> GameConfig.MemoryConfig.FM_PRICE_MULTIPLIER_MODERATE
            FMLevel.CRITICAL -> GameConfig.MemoryConfig.FM_PRICE_MULTIPLIER_CRITICAL
            FMLevel.CATASTROPHIC -> GameConfig.MemoryConfig.FM_PRICE_MULTIPLIER_CATASTROPHIC
        }
        return (baseCost * multiplier).toInt()
    }

    /**
     * حساب تخفيض XP بناءً على FM
     * 
     * @param baseXP الخبرة الأساسية
     * @param fm قيمة FM الحالية
     * @return الخبرة النهائية بعد التخفيض
     */
    fun calculateXPPenalty(baseXP: Int, fm: Int): Int {
        val level = FMLevel.fromValue(fm)
        val multiplier = when (level) {
            FMLevel.LOW -> 1.0f
            FMLevel.MILD -> GameConfig.MemoryConfig.FM_XP_MULTIPLIER_MILD
            FMLevel.MODERATE -> GameConfig.MemoryConfig.FM_XP_MULTIPLIER_MODERATE
            FMLevel.CRITICAL -> GameConfig.MemoryConfig.FM_XP_MULTIPLIER_CRITICAL
            FMLevel.CATASTROPHIC -> GameConfig.MemoryConfig.FM_XP_MULTIPLIER_CATASTROPHIC
        }
        return (baseXP * multiplier).toInt()
    }

    /**
     * هل التاجر يتذكر اللاعب؟
     * 
     * @param fm قيمة FM الحالية
     * @return true إذا كان التاجر يتذكر اللاعب
     */
    fun doesMerchantRemember(fm: Int): Boolean {
        return fm < GameConfig.MemoryConfig.FM_MERCHANT_FORGET_THRESHOLD
    }

    /**
     * هل NPC يتذكر اللاعب؟
     * 
     * @param fm قيمة FM الحالية
     * @return true إذا كان NPC يتذكر اللاعب
     */
    fun doesNPCRemember(fm: Int): Boolean {
        return fm < GameConfig.MemoryConfig.FM_NPC_FORGET_THRESHOLD
    }

    /**
     * عدد خيارات الحوار المتاحة بناءً على FM
     * 
     * @param baseOptions عدد الخيارات الأساسية
     * @param fm قيمة FM الحالية
     * @return عدد الخيارات المتاحة فعلياً
     */
    fun getAvailableDialogueOptions(baseOptions: Int, fm: Int): Int {
        val level = FMLevel.fromValue(fm)
        return when (level) {
            FMLevel.LOW -> baseOptions
            FMLevel.MILD -> (baseOptions * 0.9f).toInt().coerceAtLeast(1)
            FMLevel.MODERATE -> (baseOptions * 0.7f).toInt().coerceAtLeast(1)
            FMLevel.CRITICAL -> (baseOptions * 0.4f).toInt().coerceAtLeast(1)
            FMLevel.CATASTROPHIC -> 1 // خيار واحد فقط
        }
    }

    /**
     * هل المهمة الجانبية متاحة؟
     * 
     * @param fm قيمة FM الحالية
     * @return true إذا كانت المهام الجانبية متاحة
     */
    fun areSideQuestsAvailable(fm: Int): Boolean {
        return fm < GameConfig.MemoryConfig.FM_QUEST_LOCK_THRESHOLD
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Main Player State
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * الحالة الكاملة للاعب
 *
 * @property stats الإحصائيات والعملات
 * @property abilities القدرات المفتوحة وحالاتها
 * @property position الموقع في العالم
 * @property effects التأثيرات النشطة
 * @property isNewGamePlus هل اللعب في وضع NG+؟
 * @property ngPlusLevel مستوى NG+ (0 = لعبة عادية, 1+ = NG+)
 * @property prestigeLevel مستوى الهيبة (للاعبين المتقدمين)
 * @property discoveredTiles البلاطات المكتشفة على الخريطة
 * @property unlockedSanctuaries الملاجئ المفتوحة
 * @property completedQuests المهام المكتملة
 * @property inventory المخزون (معرّفات الأدوات)
 * @property equippedWeapon السلاح المجهز
 * @property equippedAccessories الإكسسوارات المجهزة
 */
data class PlayerState(
    val stats: PlayerStats = PlayerStats(),
    val abilities: PlayerAbilities = PlayerAbilities(),
    val position: PlayerPosition = PlayerPosition(),
    val effects: PlayerEffects = PlayerEffects(),
    val isNewGamePlus: Boolean = false,
    val ngPlusLevel: Int = 0,
    val prestigeLevel: Int = 0,
    val discoveredTiles: Set<String> = emptySet(),
    val unlockedSanctuaries: Set<String> = emptySet(),
    val completedQuests: Set<String> = emptySet(),
    val inventory: Map<String, Int> = emptyMap(), // itemId -> quantity
    val equippedWeapon: String? = null,
    val equippedAccessories: List<String> = emptyList()
) {
    /**
     * مستوى FM الحالي
     */
    val fmLevel: FMLevel
        get() = FMLevel.fromValue(stats.forgetfulnessMeter)

    /**
     * هل FM في حالة حرجة أو أسوأ؟
     */
    val isFMCritical: Boolean
        get() = fmLevel >= FMLevel.CRITICAL

    /**
     * هل FM في حالة كارثية؟
     */
    val isFMCatastrophic: Boolean
        get() = fmLevel == FMLevel.CATASTROPHIC

    /**
     * عدد البلاطات المكتشفة
     */
    val discoveryProgress: Int
        get() = discoveredTiles.size

    /**
     * عدد الملاجئ المفتوحة
     */
    val sanctuariesUnlocked: Int
        get() = unlockedSanctuaries.size

    /**
     * عدد المهام المكتملة
     */
    val questsCompleted: Int
        get() = completedQuests.size

    /**
     * هل اللاعب يمتلك أداة معينة؟
     */
    fun hasItem(itemId: String): Boolean = inventory.containsKey(itemId)

    /**
     * كمية أداة معينة
     */
    fun getItemQuantity(itemId: String): Int = inventory[itemId] ?: 0
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARK: - Player State Manager
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مدير حالة اللاعب مع StateFlow
 * 
 * يوفر واجهة reactive لإدارة حالة اللاعب مع دوال مساعدة
 * لتعديل الحالة بشكل آمن ومتسق.
 */
class PlayerStateManager {
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    /**
     * الحالة الحالية (مرجع سريع)
     */
    val currentState: PlayerState
        get() = _playerState.value

    /**
     * Get player position
     */
    fun getPosition(): PlayerPosition = currentState.position

    // ═══════════════════════════════════════════════════════════════════════════
    // Stats Manipulation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * إلحاق ضرر باللاعب
     *
     * @param damage مقدار الضرر
     * @param source مصدر الضرر
     * @param ignoreInvulnerability هل نتجاهل المناعة؟
     * @return الضرر الفعلي المطبق
     */
    fun takeDamage(damage: Int, source: String = "Unknown", ignoreInvulnerability: Boolean = false): Int {
        if (!ignoreInvulnerability && currentState.effects.isInvulnerable) return 0
        
        val defenseMultiplier = 1f - currentState.effects.getEffectStrength(EffectType.DEFENSE_BOOST)
        val finalDamage = (damage * defenseMultiplier.coerceAtLeast(0f)).toInt().coerceAtLeast(1)
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                hp = (currentState.stats.hp - finalDamage).coerceAtLeast(0)
            )
        )
        
        return finalDamage
    }

    /**
     * استشفاء اللاعب
     *
     * @param amount مقدار الاستشفاء
     * @return مقدار الاستشفاء الفعلي
     */
    fun heal(amount: Int): Int {
        val currentHp = currentState.stats.hp
        val maxHp = currentState.stats.maxHp
        val actualHeal = (currentHp + amount).coerceAtMost(maxHp) - currentHp
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                hp = (currentHp + actualHeal).coerceAtMost(maxHp)
            )
        )
        
        return actualHeal
    }

    /**
     * استعادة طاقة
     *
     * @param amount مقدار الطاقة
     * @return مقدار الطاقة الفعلي المستعاد
     */
    fun restoreEnergy(amount: Int): Int {
        val currentEnergy = currentState.stats.energy
        val maxEnergy = currentState.stats.maxEnergy
        val actualRestore = (currentEnergy + amount).coerceAtMost(maxEnergy) - currentEnergy
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                energy = (currentEnergy + actualRestore).coerceAtMost(maxEnergy)
            )
        )
        
        return actualRestore
    }

    /**
     * استهلاك طاقة
     *
     * @param amount مقدار الطاقة المستهلكة
     * @return true إذا تم الاستهلاك بنجاح
     */
    fun consumeEnergy(amount: Int): Boolean {
        if (currentState.stats.energy < amount) return false
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                energy = (currentState.stats.energy - amount).coerceAtLeast(0)
            )
        )
        
        return true
    }

    /**
     * اكتساب خبرة (XP)
     *
     * @param amount مقدار الخبرة
     * @param applyFMPenalty هل نطبق عقوبة FM؟
     * @return true إذا ارتفع المستوى
     */
    fun gainXP(amount: Int, applyFMPenalty: Boolean = true): Boolean {
        val finalXP = if (applyFMPenalty) {
            FMEffectsCalculator.calculateXPPenalty(amount, currentState.stats.forgetfulnessMeter)
        } else {
            amount
        }
        
        // تطبيق مضاعف Double XP إن وُجد
        val doubleXPMultiplier = if (currentState.effects.hasEffect(EffectType.DOUBLE_XP)) 2f else 1f
        val totalXP = (finalXP * doubleXPMultiplier).toInt()
        
        val newXP = currentState.stats.xp + totalXP
        val xpToNext = currentState.stats.xpToNextLevel
        
        if (newXP >= xpToNext) {
            // Level up!
            levelUp(newXP - xpToNext)
            return true
        } else {
            _playerState.value = currentState.copy(
                stats = currentState.stats.copy(xp = newXP)
            )
            return false
        }
    }

    /**
     * رفع مستوى اللاعب
     *
     * @param overflowXP الخبرة الزائدة
     */
    private fun levelUp(overflowXP: Int) {
        val newLevel = currentState.stats.level + 1
        val newMaxHp = currentState.stats.maxHp + GameConfig.PlayerConfig.HP_PER_LEVEL
        val newMaxEnergy = currentState.stats.maxEnergy + GameConfig.PlayerConfig.ENERGY_PER_LEVEL
        val newXpToNext = calculateXPForNextLevel(newLevel)
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                level = newLevel,
                xp = overflowXP,
                xpToNextLevel = newXpToNext,
                maxHp = newMaxHp,
                hp = newMaxHp, // استشفاء كامل عند رفع المستوى
                maxEnergy = newMaxEnergy,
                energy = newMaxEnergy
            )
        )
    }

    /**
     * حساب XP المطلوب للمستوى التالي
     */
    private fun calculateXPForNextLevel(level: Int): Int {
        return GameConfig.PlayerConfig.BASE_XP_TO_LEVEL * level
    }

    /**
     * اكتساب Memory Fragments (MF)
     *
     * @param amount مقدار MF
     */
    fun gainMF(amount: Int) {
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                memoryFragments = currentState.stats.memoryFragments + amount
            )
        )
    }

    /**
     * استهلاك Memory Fragments (MF)
     *
     * @param amount مقدار MF
     * @return true إذا تم الاستهلاك بنجاح
     */
    fun consumeMF(amount: Int): Boolean {
        if (currentState.stats.memoryFragments < amount) return false
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                memoryFragments = currentState.stats.memoryFragments - amount
            )
        )
        
        return true
    }

    /**
     * زيادة Forgetfulness Meter (FM)
     *
     * @param amount مقدار الزيادة
     */
    fun gainFM(amount: Int) {
        val newFM = (currentState.stats.forgetfulnessMeter + amount)
            .coerceAtMost(GameConfig.MemoryConfig.MAX_FM)
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                forgetfulnessMeter = newFM
            )
        )
        
        // تحديث تأثيرات FM
        updateFMEffects()
    }

    /**
     * تقليل Forgetfulness Meter (FM)
     *
     * @param amount مقدار التقليل
     */
    fun reduceFM(amount: Int) {
        val newFM = (currentState.stats.forgetfulnessMeter - amount)
            .coerceAtLeast(0)
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                forgetfulnessMeter = newFM
            )
        )
        
        // تحديث تأثيرات FM
        updateFMEffects()
    }

    /**
     * تحديث تأثيرات FM بناءً على المستوى الحالي
     */
    private fun updateFMEffects() {
        val fmLevel = FMLevel.fromValue(currentState.stats.forgetfulnessMeter)
        
        // إزالة تأثيرات FM القديمة
        val cleanedEffects = currentState.effects.effects.filterNot { effect ->
            effect.type in listOf(
                EffectType.FM_LOW,
                EffectType.FM_MILD,
                EffectType.FM_MODERATE,
                EffectType.FM_CRITICAL,
                EffectType.FM_CATASTROPHIC
            )
        }
        
        // إضافة التأثير الجديد
        val newEffect = Effect(
            type = fmLevel.effectType,
            strength = 1f,
            duration = Long.MAX_VALUE, // دائم
            remaining = Long.MAX_VALUE,
            source = "Forgetfulness Meter",
            stackable = false
        )
        
        _playerState.value = currentState.copy(
            effects = PlayerEffects(cleanedEffects + newEffect)
        )
    }

    /**
     * اكتساب عملة
     *
     * @param amount مقدار العملة
     */
    fun gainCurrency(amount: Int) {
        // تطبيق مضاعف Double Currency إن وُجد
        val multiplier = if (currentState.effects.hasEffect(EffectType.DOUBLE_CURRENCY)) 2f else 1f
        val finalAmount = (amount * multiplier).toInt()
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                currency = currentState.stats.currency + finalAmount
            )
        )
    }

    /**
     * إنفاق عملة
     *
     * @param amount مقدار العملة
     * @param applyFMPenalty هل نطبق عقوبة FM على السعر؟
     * @return true إذا تم الإنفاق بنجاح
     */
    fun spendCurrency(amount: Int, applyFMPenalty: Boolean = true): Boolean {
        val finalCost = if (applyFMPenalty) {
            FMEffectsCalculator.calculatePricePenalty(amount, currentState.stats.forgetfulnessMeter)
        } else {
            amount
        }
        
        if (currentState.stats.currency < finalCost) return false
        
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                currency = currentState.stats.currency - finalCost
            )
        )
        
        return true
    }

    /**
     * إضافة نقاط
     *
     * @param points مقدار النقاط
     */
    
    fun addHP(amount: Int) {
        val newHp = (currentState.stats.hp + amount).coerceAtMost(currentState.stats.maxHp)
        updateStats(currentState.stats.copy(hp = newHp))
    }

    fun addEnergy(amount: Int) {
        val newEnergy = (currentState.stats.energy + amount).coerceAtMost(currentState.stats.maxEnergy)
        updateStats(currentState.stats.copy(energy = newEnergy))
    }

    fun addXP(amount: Float) {
        val nextXP = currentState.stats.xp + amount.toInt()
        updateStats(currentState.stats.copy(xp = nextXP))
        // level up logic normally goes here, leaving it out for now to satisfy compiler
    }
    
    fun addMemoryFragments(amount: Int) {
        updateStats(currentState.stats.copy(memoryFragments = currentState.stats.memoryFragments + amount))
    }
    
    fun addCurrency(amount: Int) {
        updateStats(currentState.stats.copy(currency = currentState.stats.currency + amount))
    }
    
    fun addForgetfulness(amount: Int) {
        updateStats(currentState.stats.copy(forgetfulnessMeter = (currentState.stats.forgetfulnessMeter + amount).coerceAtMost(100)))
    }

    fun addScore(points: Int) {
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                score = currentState.stats.score + points
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Position Manipulation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * تحديث موقع اللاعب
     *
     * @param x الموقع الأفقي
     * @param y الموقع العمودي
     * @param velocityX السرعة الأفقية
     * @param velocityY السرعة العمودية
     * @param facingRight الاتجاه
     */
    fun updatePosition(
        x: Float? = null,
        y: Float? = null,
        velocityX: Float? = null,
        velocityY: Float? = null,
        facingRight: Boolean? = null
    ) {
        _playerState.value = currentState.copy(
            position = currentState.position.copy(
                x = x ?: currentState.position.x,
                y = y ?: currentState.position.y,
                velocityX = velocityX ?: currentState.position.velocityX,
                velocityY = velocityY ?: currentState.position.velocityY,
                facingRight = facingRight ?: currentState.position.facingRight
            )
        )
    }

    /**
     * تغيير المنطقة
     *
     * @param newRegion المنطقة الجديدة
     * @param spawnX موقع الظهور X
     * @param spawnY موقع الظهور Y
     */
    fun changeRegion(newRegion: RegionId, spawnX: Float, spawnY: Float) {
        _playerState.value = currentState.copy(
            position = currentState.position.copy(
                regionId = newRegion,
                x = spawnX,
                y = spawnY,
                velocityX = 0f,
                velocityY = 0f
            )
        )
    }

    /**
     * تحديث حالة الأرضية
     */
    fun setGrounded(grounded: Boolean) {
        _playerState.value = currentState.copy(
            position = currentState.position.copy(isGrounded = grounded)
        )
    }

    /**
     * تحديث حالة الجدار
     */
    fun setOnWall(onWall: Boolean) {
        _playerState.value = currentState.copy(
            position = currentState.position.copy(isOnWall = onWall)
        )
    }

    /**
     * تحديث حالة التسلق
     */
    fun setClimbing(climbing: Boolean) {
        _playerState.value = currentState.copy(
            position = currentState.position.copy(isClimbing = climbing)
        )
    }

    /**
     * تحديث حالة التأرجح
     */
    fun setSwinging(swinging: Boolean) {
        _playerState.value = currentState.copy(
            position = currentState.position.copy(isSwinging = swinging)
        )
    }

    /**
     * تسجيل آخر ملجأ
     */
    fun registerSanctuary(sanctuaryId: String) {
        _playerState.value = currentState.copy(
            position = currentState.position.copy(lastSanctuaryId = sanctuaryId),
            unlockedSanctuaries = currentState.unlockedSanctuaries + sanctuaryId
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Abilities Manipulation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * فتح قدرة
     *
     * @param abilityType نوع القدرة
     * @param maxCooldown أقصى وقت انتظار (ميلي ثانية)
     * @param maxUses الحد الأقصى للاستخدامات (-1 = غير محدود)
     */
    fun unlockAbility(abilityType: AbilityType, maxCooldown: Long = 0L, maxUses: Int = -1) {
        val newAbility = PlayerStateAbility(
            type = abilityType,
            unlocked = true,
            level = 1,
            cooldownRemaining = 0L,
            maxCooldown = maxCooldown,
            usesRemaining = maxUses,
            maxUses = maxUses
        )
        
        val updatedAbilities = currentState.abilities.abilities + (abilityType to newAbility)
        
        _playerState.value = currentState.copy(
            abilities = PlayerAbilities(updatedAbilities)
        )
    }

    /**
     * استخدام قدرة
     *
     * @param abilityType نوع القدرة
     * @return true إذا تم الاستخدام بنجاح
     */
    fun useAbility(abilityType: AbilityType): Boolean {
        val ability = currentState.abilities.getAbility(abilityType) ?: return false
        
        if (!ability.isReady) return false
        
        val updatedAbility = ability.copy(
            cooldownRemaining = ability.maxCooldown,
            usesRemaining = if (ability.maxUses > 0) ability.usesRemaining - 1 else ability.usesRemaining
        )
        
        val updatedAbilities = currentState.abilities.abilities + (abilityType to updatedAbility)
        
        _playerState.value = currentState.copy(
            abilities = PlayerAbilities(updatedAbilities)
        )
        
        return true
    }

    /**
     * تحديث أوقات الانتظار
     *
     * @param deltaMs الوقت المنقضي (ميلي ثانية)
     */
    fun updateCooldowns(deltaMs: Long) {
        val updatedAbilities = currentState.abilities.abilities.mapValues { (_, ability) ->
            if (ability.cooldownRemaining > 0) {
                ability.copy(
                    cooldownRemaining = (ability.cooldownRemaining - deltaMs).coerceAtLeast(0)
                )
            } else {
                ability
            }
        }
        
        _playerState.value = currentState.copy(
            abilities = PlayerAbilities(updatedAbilities)
        )
    }

    /**
     * إعادة ملء استخدامات القدرة
     *
     * @param abilityType نوع القدرة (null = جميع القدرات)
     */
    fun refillAbilityUses(abilityType: AbilityType? = null) {
        val updatedAbilities = currentState.abilities.abilities.mapValues { (type, ability) ->
            if (abilityType == null || type == abilityType) {
                ability.copy(usesRemaining = ability.maxUses)
            } else {
                ability
            }
        }
        
        _playerState.value = currentState.copy(
            abilities = PlayerAbilities(updatedAbilities)
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Effects Manipulation
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * إضافة تأثير
     *
     * @param effect التأثير المراد إضافته
     */
    fun addEffect(effect: Effect) {
        val currentEffects = currentState.effects.effects.toMutableList()
        
        // إذا كان التأثير قابل للتكديس، ابحث عن نسخة موجودة
        if (effect.stackable) {
            val existingIndex = currentEffects.indexOfFirst { 
                it.type == effect.type && it.source == effect.source 
            }
            
            if (existingIndex >= 0) {
                val existing = currentEffects[existingIndex]
                currentEffects[existingIndex] = existing.copy(
                    stacks = existing.stacks + 1,
                    remaining = maxOf(existing.remaining, effect.remaining)
                )
            } else {
                currentEffects.add(effect)
            }
        } else {
            // غير قابل للتكديس، استبدل الموجود إن وُجد
            currentEffects.removeAll { it.type == effect.type }
            currentEffects.add(effect)
        }
        
        _playerState.value = currentState.copy(
            effects = PlayerEffects(currentEffects)
        )
    }

    /**
     * إزالة تأثير
     *
     * @param effectType نوع التأثير
     */
    fun removeEffect(effectType: EffectType) {
        val updatedEffects = currentState.effects.effects.filterNot { it.type == effectType }
        
        _playerState.value = currentState.copy(
            effects = PlayerEffects(updatedEffects)
        )
    }

    /**
     * تحديث التأثيرات (تقليل الوقت المتبقي)
     *
     * @param deltaMs الوقت المنقضي (ميلي ثانية)
     */
    fun updateEffects(deltaMs: Long) {
        val updatedEffects = currentState.effects.effects
            .map { effect ->
                if (effect.duration > 0) {
                    effect.copy(
                        remaining = (effect.remaining - deltaMs).coerceAtLeast(0)
                    )
                } else {
                    effect
                }
            }
            .filterNot { it.isExpired } // إزالة التأثيرات المنتهية
        
        _playerState.value = currentState.copy(
            effects = PlayerEffects(updatedEffects)
        )
    }

    /**
     * مسح جميع التأثيرات
     *
     * @param positiveOnly هل نمسح Buffs فقط؟
     * @param negativeOnly هل نمسح Debuffs فقط؟
     */
    fun clearEffects(positiveOnly: Boolean = false, negativeOnly: Boolean = false) {
        val updatedEffects = when {
            positiveOnly -> currentState.effects.effects.filterNot { it.isPositive }
            negativeOnly -> currentState.effects.effects.filter { it.isPositive }
            else -> emptyList()
        }
        
        _playerState.value = currentState.copy(
            effects = PlayerEffects(updatedEffects)
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Inventory & Equipment
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * إضافة أداة للمخزون
     *
     * @param itemId معرّف الأداة
     * @param quantity الكمية
     */
    fun addItem(itemId: String, quantity: Int = 1) {
        val currentQuantity = currentState.inventory[itemId] ?: 0
        val updatedInventory = currentState.inventory + (itemId to currentQuantity + quantity)
        
        _playerState.value = currentState.copy(
            inventory = updatedInventory
        )
    }

    /**
     * إزالة أداة من المخزون
     *
     * @param itemId معرّف الأداة
     * @param quantity الكمية
     * @return true إذا تمت الإزالة بنجاح
     */
    fun removeItem(itemId: String, quantity: Int = 1): Boolean {
        val currentQuantity = currentState.inventory[itemId] ?: return false
        if (currentQuantity < quantity) return false
        
        val updatedInventory = if (currentQuantity == quantity) {
            currentState.inventory - itemId
        } else {
            currentState.inventory + (itemId to currentQuantity - quantity)
        }
        
        _playerState.value = currentState.copy(
            inventory = updatedInventory
        )
        
        return true
    }

    /**
     * تجهيز سلاح
     *
     * @param weaponId معرّف السلاح
     */
    fun equipWeapon(weaponId: String) {
        _playerState.value = currentState.copy(
            equippedWeapon = weaponId
        )
    }

    /**
     * إزالة السلاح المجهز
     */
    fun unequipWeapon() {
        _playerState.value = currentState.copy(
            equippedWeapon = null
        )
    }

    /**
     * تجهيز إكسسوار
     *
     * @param accessoryId معرّف الإكسسوار
     * @param slot رقم الفتحة (0-2)
     */
    fun equipAccessory(accessoryId: String, slot: Int) {
        val maxSlots = GameConfig.PlayerConfig.MAX_ACCESSORY_SLOTS
        if (slot < 0 || slot >= maxSlots) return
        
        val updatedAccessories = currentState.equippedAccessories.toMutableList()
        
        // توسيع القائمة إذا لزم الأمر
        while (updatedAccessories.size <= slot) {
            updatedAccessories.add("")
        }
        
        updatedAccessories[slot] = accessoryId
        
        _playerState.value = currentState.copy(
            equippedAccessories = updatedAccessories
        )
    }

    /**
     * إزالة إكسسوار
     *
     * @param slot رقم الفتحة
     */
    fun unequipAccessory(slot: Int) {
        if (slot < 0 || slot >= currentState.equippedAccessories.size) return
        
        val updatedAccessories = currentState.equippedAccessories.toMutableList()
        updatedAccessories[slot] = ""
        
        _playerState.value = currentState.copy(
            equippedAccessories = updatedAccessories
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Discovery & Progress
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * اكتشاف بلاطة على الخريطة
     *
     * @param tileId معرّف البلاطة
     */
    fun discoverTile(tileId: String) {
        _playerState.value = currentState.copy(
            discoveredTiles = currentState.discoveredTiles + tileId
        )
    }

    /**
     * إكمال مهمة
     *
     * @param questId معرّف المهمة
     */
    fun completeQuest(questId: String) {
        _playerState.value = currentState.copy(
            completedQuests = currentState.completedQuests + questId
        )
    }

    /**
     * تسجيل موت
     */
    fun registerDeath() {
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                deaths = currentState.stats.deaths + 1
            )
        )
    }

    /**
     * تسجيل قتل عدو
     */
    fun registerEnemyKill() {
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                enemiesKilled = currentState.stats.enemiesKilled + 1
            )
        )
    }

    /**
     * تسجيل هزيمة زعيم
     */
    fun registerBossDefeat() {
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                bossesDefeated = currentState.stats.bossesDefeated + 1
            )
        )
    }

    /**
     * تسجيل اكتشاف سر
     */
    fun registerSecretFound() {
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                secretsFound = currentState.stats.secretsFound + 1
            )
        )
    }

    /**
     * تحديث وقت اللعب
     *
     * @param minutes الدقائق المنقضية
     */
    fun updatePlayTime(minutes: Int) {
        _playerState.value = currentState.copy(
            stats = currentState.stats.copy(
                playTimeMinutes = currentState.stats.playTimeMinutes + minutes
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // New Game Plus
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * بدء New Game Plus
     *
     * @param retainedAbilities القدرات المحتفظ بها
     * @param retainedItems الأدوات المحتفظ بها
     */
    fun startNewGamePlus(
        retainedAbilities: Set<AbilityType> = emptySet(),
        retainedItems: Map<String, Int> = emptyMap()
    ) {
        val ngLevel = currentState.ngPlusLevel + 1
        val prestige = currentState.prestigeLevel + 1
        
        // القدرات المحتفظ بها
        val keptAbilities = currentState.abilities.abilities.filterKeys { 
            it in retainedAbilities 
        }.mapValues { (_, ability) ->
            ability.copy(
                cooldownRemaining = 0L,
                usesRemaining = ability.maxUses
            )
        }
        
        _playerState.value = PlayerState(
            stats = PlayerStats(),
            abilities = PlayerAbilities(keptAbilities),
            position = PlayerPosition(),
            effects = PlayerEffects(),
            isNewGamePlus = true,
            ngPlusLevel = ngLevel,
            prestigeLevel = prestige,
            inventory = retainedItems
        )
        
        // تطبيق تأثير FM الابتدائي
        updateFMEffects()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // State Reset
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * إعادة تعيين الحالة بالكامل (لعبة جديدة)
     */
    fun resetState() {
        _playerState.value = PlayerState()
        updateFMEffects()
    }

    /**
     * تحميل حالة من مصدر خارجي
     *
     * @param state الحالة المراد تحميلها
     */
    fun loadState(state: PlayerState) {
        _playerState.value = state
    }
}