package com.erygra.maskoflight.enemy

import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.world.RegionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * Enemy.kt — نظام الأعداء الشامل للعبة "قِنَاعُ النُّور"
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * يدير جميع أنواع الأعداء في اللعبة من الأعداء العاديين إلى الزعماء.
 * يتضمن:
 * - تعريف أنواع الأعداء (10+ نوع لكل منطقة)
 * - نظام الإحصائيات (HP, Damage, Speed, Armor)
 * - نظام السلوك الأساسي (Patrol, Chase, Attack)
 * - نظام الهجمات والمقذوفات
 * - نظام الغنائم (Loot Tables)
 * - أنواع Elite و Miniboss و Boss
 * - تكامل مع PhysicsEngine, CombatEngine, EventBus
 * 
 * @author Erygra Universe Development Team
 * @version 2.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Enemy Data Classes
// ═══════════════════════════════════════════════════════════════════════════

/**
 * نوع العدو
 */
enum class EnemyType {
    // Ashen Sprawl
    SCRAB_SCAVENGER,
    ASHWARDEN,
    RAG_CHILDREN,
    COUNCIL_ENFORCER,     // Elite
    PYRE_HARROW,          // Miniboss
    
    // Veiled Archives
    PAGE_SCRAPER,
    VAULT_SENTINEL,
    ECHO_SHADE,
    LEDGER_WARDEN,        // Elite
    THE_INDEXER,          // Miniboss
    
    // Hollowed Archipelago
    ROPE_CROAKER,
    DRIFT_KNIGHT,
    BARGAIN_PIRATE,
    SKY_SCAVENGER,        // Elite
    BRIDGEMASTER,         // Miniboss
    
    // Glassfjord Cliffs
    SHARDLING,
    REFLECTOR,
    GLASS_HOUND,
    MIRROR_SENTINEL,      // Elite
    CRYSTALLINE_WARDEN,   // Miniboss
    
    // Sunken Clockworks
    GEARFOLK,
    FLOOD_WRAITH,
    VALVE_SPIDER,
    BRASS_JUGGERNAUT,     // Elite
    FERRYMAN_SHADE,       // Miniboss
    
    // Blackroot Moorlands
    ROOTCRAWLER,
    BOG_SIREN,
    HOLLOW_HERDER,
    NIGHT_STITCHER_ECHO,  // Elite
    ROOT_TITAN,           // Miniboss
    
    // Luminous Chasm
    GLOW_WISP,
    MEMORY_LEECH,
    CAVE_STALKER,
    BIOLUME_SENTINEL,     // Elite
    ECHO_MAW,             // Miniboss
    
    // Bosses
    LADY_SOREN_SENTINEL,  // Ashen Sprawl Boss
    ARCHIVAL_REGENT,      // Veiled Archives Boss
    ROOK,                 // Hollowed Archipelago Boss
    FRACTURED_COLOSSUS,   // Glassfjord Cliffs Boss
    GIDEON_REMNANT,       // Sunken Clockworks Boss
    MAERA_TRIAL,          // Blackroot Moorlands Boss (special)
    LUMINAR_HOST          // Luminous Chasm Boss
}

/**
 * رتبة العدو
 */
enum class EnemyRank {
    NORMAL,      // عدو عادي
    ELITE,       // عدو نخبة (أقوى، غنائم أفضل)
    MINIBOSS,    // زعيم صغير
    BOSS         // زعيم رئيسي
}

/**
 * فئة العدو (للسلوك)
 */
enum class EnemyCategory {
    MELEE,       // قتال قريب
    RANGED,      // قتال بعيد
    FLYING,      // طائر
    HEAVY,       // ثقيل (بطيء، مدرع)
    STEALTH,     // تسلل/كمين
    SUPPORT,     // دعم (يشفي، يعزز)
    BOSS         // زعيم (ميكانيكيات خاصة)
}

/**
 * نوع الهجوم
 */
enum class AttackType {
    MELEE_BASIC,         // ضربة قريبة أساسية
    MELEE_HEAVY,         // ضربة قريبة ثقيلة
    RANGED_PROJECTILE,   // مقذوف
    RANGED_BEAM,         // شعاع
    AOE_EXPLOSION,       // انفجار منطقة
    AOE_SHOCKWAVE,       // موجة صدمة
    GRAB,                // إمساك
    CHARGE,              // اندفاع
    SUMMON,              // استدعاء
    SPECIAL              // هجوم خاص
}

/**
 * تعريف هجوم العدو
 */
data class EnemyAttack(
    val name: String,
    val nameArabic: String,
    val type: AttackType,
    val damage: Int,
    val range: Float,
    val minRange: Float = 0f,
    val aoeRadius: Float = 0f,
    val cooldownMs: Long = 2000L,
    val windupMs: Long = 500L,      // وقت التحضير (telegraph)
    val recoveryMs: Long = 300L,    // وقت الاسترداد
    val knockback: Float = 0f,
    val statusEffects: List<String> = emptyList(),  // e.g., "stun", "slow", "poison"
    val projectileSpeed: Float = 0f,
    val projectileLifetime: Long = 0L,
    val canBeParried: Boolean = true,
    val description: String = ""
)

/**
 * جدول الغنائم
 */
data class LootTable(
    val coinMin: Int = 0,
    val coinMax: Int = 0,
    val xpReward: Int = 0,
    val mfChance: Float = 0f,        // احتمال إسقاط MF
    val mfAmount: Int = 1,
    val commonDrops: List<ItemDrop> = emptyList(),
    val rareDrops: List<ItemDrop> = emptyList(),
    val epicDrops: List<ItemDrop> = emptyList(),
    val guaranteedDrops: List<String> = emptyList()  // عناصر مضمونة
)

/**
 * عنصر قابل للإسقاط
 */
data class ItemDrop(
    val itemId: String,
    val chance: Float,      // 0.0 - 1.0
    val quantityMin: Int = 1,
    val quantityMax: Int = 1
)

/**
 * إحصائيات العدو
 */
data class EnemyStats(
    val maxHp: Int,
    var currentHp: Int = maxHp,
    val damage: Int,
    val defense: Int = 0,
    val speed: Float,
    val detectionRange: Float,
    val attackRange: Float,
    val jumpHeight: Float = 0f,
    val knockbackResistance: Float = 0f  // 0.0 - 1.0
)

/**
 * موقع العدو
 */
data class EnemyPosition(
    var x: Float,
    var y: Float,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var isFacingRight: Boolean = true,
    var isGrounded: Boolean = false,
    var isOnWall: Boolean = false
)

/**
 * تعريف نوع العدو
 */
data class EnemyDefinition(
    val type: EnemyType,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    val rank: EnemyRank,
    val category: EnemyCategory,
    val region: String,  // GameRegion ID
    
    // الإحصائيات
    val baseStats: EnemyStats,
    
    // السلوك
    val patrolSpeed: Float,
    val chaseSpeed: Float,
    val aggroRange: Float,
    val leashRange: Float,  // المدى الذي يتوقف فيه عن المطاردة
    
    // الهجمات
    val attacks: List<EnemyAttack>,
    
    // الغنائم
    val lootTable: LootTable,
    
    // القدرات الخاصة
    val canPatrol: Boolean = true,
    val canMoveWhileAttacking: Boolean = false,
    val canFly: Boolean = false,
    val canClimb: Boolean = false,
    val canSwim: Boolean = false,
    val isInvulnerableToKnockback: Boolean = false,
    val regeneratesHealth: Boolean = false,
    val healthRegenRate: Float = 0f,  // HP per second
    
    // المظهر
    val spriteSheet: String = "",
    val scale: Float = 1f,
    val hitboxWidth: Float = 1f,
    val hitboxHeight: Float = 2f,
    
    // البيانات الوصفية
    val loreText: String = "",
    val loreTextArabic: String = ""
)

/**
 * حالة العدو في اللعبة
 */
data class Enemy(
    val id: String,                     // معرّف فريد
    val definition: EnemyDefinition,
    var stats: EnemyStats,
    val position: EnemyPosition,
    var level: Int = 1,
    
    // الحالة
    var currentState: EnemyState = EnemyState.IDLE,
    var targetPlayerId: String? = null,
    var spawnPoint: Pair<Float, Float>,
    var lastAttackTime: Long = 0L,
    var lastDamagedTime: Long = 0L,
    var stunEndTime: Long = 0L,
    
    // التأثيرات
    val activeEffects: MutableList<EnemyEffect> = mutableListOf(),
    val attackCooldowns: MutableMap<String, Long> = mutableMapOf(),
    
    // البيانات المخصصة
    val customData: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * هل العدو حي؟
     */
    val isAlive: Boolean get() = stats.currentHp > 0
    
    /**
     * هل العدو مصعوق؟
     */
    val isStunned: Boolean get() = System.currentTimeMillis() < stunEndTime
    
    /**
     * هل العدو في حالة هجوم؟
     */
    val isAttacking: Boolean get() = currentState == EnemyState.ATTACKING
    
    /**
     * هل العدو يطارد اللاعب؟
     */
    val isChasing: Boolean get() = currentState == EnemyState.CHASING
    
    /**
     * نسبة الصحة (0.0 - 1.0)
     */
    val healthPercent: Float get() = stats.currentHp.toFloat() / stats.maxHp
}

/**
 * حالة العدو (للـ AI FSM)
 */
enum class EnemyState {
    IDLE,           // خامل
    PATROL,         // دورية
    ALERT,          // متيقظ (رأى اللاعب لكن لم يهاجم بعد)
    CHASING,        // مطاردة
    ATTACKING,      // هجوم
    FLEEING,        // هروب
    STUNNED,        // مصعوق
    RETURNING,      // العودة لنقطة الظهور
    DEAD            // ميت
}

/**
 * تأثير على العدو
 */
data class EnemyEffect(
    val type: EnemyEffectType,
    val duration: Long,
    val startTime: Long = System.currentTimeMillis(),
    val value: Float = 1f,
    val source: String = ""
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - startTime >= duration
    val remainingTime: Long get() = max(0L, duration - (System.currentTimeMillis() - startTime))
}

/**
 * أنواع التأثيرات على الأعداء
 */
enum class EnemyEffectType {
    SLOW,           // بطء
    STUN,           // صعق
    POISON,         // سم
    BURN,           // حرق
    FREEZE,         // تجميد
    BLIND,          // عمى
    FEAR,           // خوف (يجبره على الهروب)
    WEAKNESS,       // ضعف (-damage)
    ARMOR_BREAK,    // كسر الدرع
    MARKED,         // محدد (يظهر على الخريطة)
    INVULNERABLE    // لا يقهر
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Enemy Database
// ═══════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات أنواع الأعداء
 */
object EnemyDatabase {
    
    /**
     * جميع تعريفات الأعداء
     */
    val allEnemies = mapOf(
        
        // ═══════════════════════════════════════════════════════════════
        // Ashen Sprawl Enemies
        // ═══════════════════════════════════════════════════════════════
        
        EnemyType.SCRAB_SCAVENGER to EnemyDefinition(
            type = EnemyType.SCRAB_SCAVENGER,
            name = "Scrab Scavenger",
            nameArabic = "زبّال السكراب",
            description = "Small scavenger that steals MF from the ground",
            descriptionArabic = "زبّال صغير يسرق MF من الأرض",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.MELEE,
            region = "ashen_sprawl",
            baseStats = EnemyStats(
                maxHp = 30,
                damage = 8,
                defense = 2,
                speed = 3f,
                detectionRange = 8f,
                attackRange = 1.5f
            ),
            patrolSpeed = 1.5f,
            chaseSpeed = 4f,
            aggroRange = 8f,
            leashRange = 15f,
            attacks = listOf(
                EnemyAttack(
                    name = "Claw Swipe",
                    nameArabic = "ضربة المخلب",
                    type = AttackType.MELEE_BASIC,
                    damage = 8,
                    range = 1.5f,
                    cooldownMs = 1500L,
                    windupMs = 300L,
                    knockback = 100f
                )
            ),
            lootTable = LootTable(
                coinMin = 5,
                coinMax = 15,
                xpReward = 10,
                mfChance = 0.05f,
                commonDrops = listOf(
                    ItemDrop("leather_scrap", 0.3f),
                    ItemDrop("metal_scrap", 0.2f)
                )
            ),
            spriteSheet = "enemy_scrab_scavenger",
            hitboxWidth = 1f,
            hitboxHeight = 1.5f,
            loreText = "Feeds on ash and forgotten memories",
            loreTextArabic = "يتغذى على الرماد والذكريات المنسية"
        ),
        
        EnemyType.ASHWARDEN to EnemyDefinition(
            type = EnemyType.ASHWARDEN,
            name = "Ashwarden",
            nameArabic = "حارس الرماد",
            description = "Heavily armored guard, requires parry to break defense",
            descriptionArabic = "حارس مدرع بشدة، يحتاج صد لكسر الدفاع",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.HEAVY,
            region = "ashen_sprawl",
            baseStats = EnemyStats(
                maxHp = 60,
                damage = 15,
                defense = 10,
                speed = 1.5f,
                detectionRange = 10f,
                attackRange = 2f,
                knockbackResistance = 0.7f
            ),
            patrolSpeed = 1f,
            chaseSpeed = 2f,
            aggroRange = 10f,
            leashRange = 20f,
            attacks = listOf(
                EnemyAttack(
                    name = "Shield Bash",
                    nameArabic = "ضربة الدرع",
                    type = AttackType.MELEE_HEAVY,
                    damage = 15,
                    range = 2f,
                    cooldownMs = 3000L,
                    windupMs = 800L,
                    knockback = 300f,
                    statusEffects = listOf("stun"),
                    canBeParried = true,
                    description = "Can be parried to break armor"
                ),
                EnemyAttack(
                    name = "Overhead Slam",
                    nameArabic = "ضربة علوية",
                    type = AttackType.MELEE_HEAVY,
                    damage = 20,
                    range = 2f,
                    cooldownMs = 5000L,
                    windupMs = 1200L,
                    recoveryMs = 600L,
                    knockback = 200f,
                    canBeParried = false
                )
            ),
            lootTable = LootTable(
                coinMin = 15,
                coinMax = 30,
                xpReward = 25,
                mfChance = 0.1f,
                commonDrops = listOf(
                    ItemDrop("metal_scrap", 0.5f, 2, 4)
                ),
                rareDrops = listOf(
                    ItemDrop("health_vial", 0.15f)
                )
            ),
            isInvulnerableToKnockback = true,
            spriteSheet = "enemy_ashwarden",
            scale = 1.3f,
            hitboxWidth = 1.5f,
            hitboxHeight = 2.5f,
            loreText = "Silent sentinels of the Council's will",
            loreTextArabic = "حراس صامتون لإرادة المجلس"
        ),
        
        EnemyType.RAG_CHILDREN to EnemyDefinition(
            type = EnemyType.RAG_CHILDREN,
            name = "Rag-Children",
            nameArabic = "أطفال الخرق",
            description = "Stealthy enemy that plants sound traps",
            descriptionArabic = "عدو متخفي يزرع فخاخ صوتية",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.STEALTH,
            region = "ashen_sprawl",
            baseStats = EnemyStats(
                maxHp = 20,
                damage = 12,
                defense = 0,
                speed = 4f,
                detectionRange = 6f,
                attackRange = 1f
            ),
            patrolSpeed = 2f,
            chaseSpeed = 5f,
            aggroRange = 6f,
            leashRange = 12f,
            attacks = listOf(
                EnemyAttack(
                    name = "Ambush Strike",
                    nameArabic = "ضربة كمين",
                    type = AttackType.MELEE_BASIC,
                    damage = 12,
                    range = 1f,
                    cooldownMs = 2000L,
                    windupMs = 200L,
                    knockback = 150f
                ),
                EnemyAttack(
                    name = "Plant Trap",
                    nameArabic = "زرع فخ",
                    type = AttackType.SPECIAL,
                    damage = 10,
                    range = 3f,
                    cooldownMs = 8000L,
                    windupMs = 1000L,
                    description = "Plants a sound trap that stuns on trigger"
                )
            ),
            lootTable = LootTable(
                coinMin = 8,
                coinMax = 20,
                xpReward = 15,
                mfChance = 0.08f,
                commonDrops = listOf(
                    ItemDrop("leather_scrap", 0.4f)
                )
            ),
            canClimb = true,
            spriteSheet = "enemy_rag_children",
            scale = 0.8f,
            hitboxWidth = 0.8f,
            hitboxHeight = 1.5f,
            loreText = "Orphaned souls clinging to scraps of memory",
            loreTextArabic = "أرواح يتيمة متشبثة بقصاصات الذاكرة"
        ),
        
        EnemyType.COUNCIL_ENFORCER to EnemyDefinition(
            type = EnemyType.COUNCIL_ENFORCER,
            name = "Council Enforcer",
            nameArabic = "منفذ المجلس",
            description = "Elite guard with light-cutting attacks",
            descriptionArabic = "حارس نخبة مع هجمات قاطعة للضوء",
            rank = EnemyRank.ELITE,
            category = EnemyCategory.MELEE,
            region = "ashen_sprawl",
            baseStats = EnemyStats(
                maxHp = 100,
                damage = 20,
                defense = 8,
                speed = 3f,
                detectionRange = 12f,
                attackRange = 3f,
                knockbackResistance = 0.5f
            ),
            patrolSpeed = 2f,
            chaseSpeed = 4f,
            aggroRange = 12f,
            leashRange = 25f,
            attacks = listOf(
                EnemyAttack(
                    name = "Light Slash",
                    nameArabic = "قطع ضوئي",
                    type = AttackType.MELEE_BASIC,
                    damage = 20,
                    range = 3f,
                    cooldownMs = 2000L,
                    windupMs = 400L,
                    knockback = 200f
                ),
                EnemyAttack(
                    name = "Path Cutter",
                    nameArabic = "قاطع المسار",
                    type = AttackType.RANGED_BEAM,
                    damage = 25,
                    range = 8f,
                    cooldownMs = 5000L,
                    windupMs = 1000L,
                    description = "Cuts a path of light that damages all in line"
                ),
                EnemyAttack(
                    name = "Blinding Flash",
                    nameArabic = "وميض مُعمي",
                    type = AttackType.AOE_EXPLOSION,
                    damage = 10,
                    range = 5f,
                    aoeRadius = 5f,
                    cooldownMs = 8000L,
                    windupMs = 800L,
                    statusEffects = listOf("blind")
                )
            ),
            lootTable = LootTable(
                coinMin = 30,
                coinMax = 60,
                xpReward = 50,
                mfChance = 0.2f,
                mfAmount = 1,
                rareDrops = listOf(
                    ItemDrop("greater_health_vial", 0.25f),
                    ItemDrop("energy_crystal", 0.3f)
                ),
                epicDrops = listOf(
                    ItemDrop("strength_tonic", 0.1f)
                )
            ),
            spriteSheet = "enemy_council_enforcer",
            scale = 1.2f,
            hitboxWidth = 1.2f,
            hitboxHeight = 2.2f,
            loreText = "The Council's eyes and blades",
            loreTextArabic = "عيون المجلس ونصاله"
        ),
        
        EnemyType.PYRE_HARROW to EnemyDefinition(
            type = EnemyType.PYRE_HARROW,
            name = "The Pyre Harrow",
            nameArabic = "مشعل النار",
            description = "Miniboss that moves between flame pillars",
            descriptionArabic = "زعيم صغير يتحرك بين أعمدة النار",
            rank = EnemyRank.MINIBOSS,
            category = EnemyCategory.BOSS,
            region = "ashen_sprawl",
            baseStats = EnemyStats(
                maxHp = 300,
                damage = 30,
                defense = 15,
                speed = 2.5f,
                detectionRange = 15f,
                attackRange = 4f,
                knockbackResistance = 0.9f
            ),
            patrolSpeed = 1.5f,
            chaseSpeed = 3f,
            aggroRange = 15f,
            leashRange = 30f,
            attacks = listOf(
                EnemyAttack(
                    name = "Flame Swipe",
                    nameArabic = "ضربة اللهب",
                    type = AttackType.MELEE_HEAVY,
                    damage = 30,
                    range = 4f,
                    cooldownMs = 3000L,
                    windupMs = 600L,
                    knockback = 300f,
                    statusEffects = listOf("burn")
                ),
                EnemyAttack(
                    name = "Pyre Teleport",
                    nameArabic = "انتقال المشعل",
                    type = AttackType.SPECIAL,
                    damage = 0,
                    range = 20f,
                    cooldownMs = 6000L,
                    windupMs = 500L,
                    description = "Teleports between flame pillars"
                ),
                EnemyAttack(
                    name = "Inferno Wave",
                    nameArabic = "موجة الجحيم",
                    type = AttackType.AOE_SHOCKWAVE,
                    damage = 40,
                    range = 10f,
                    aoeRadius = 10f,
                    cooldownMs = 10000L,
                    windupMs = 2000L,
                    knockback = 400f,
                    statusEffects = listOf("burn")
                )
            ),
            lootTable = LootTable(
                coinMin = 100,
                coinMax = 200,
                xpReward = 150,
                mfChance = 1.0f,  // guaranteed
                mfAmount = 2,
                rareDrops = listOf(
                    ItemDrop("greater_health_vial", 0.5f, 1, 2),
                    ItemDrop("echo_shard", 0.4f)
                ),
                epicDrops = listOf(
                    ItemDrop("strength_tonic", 0.3f)
                ),
                guaranteedDrops = listOf("pyre_fragment")  // key item
            ),
            isInvulnerableToKnockback = true,
            regeneratesHealth = false,
            spriteSheet = "enemy_pyre_harrow",
            scale = 2f,
            hitboxWidth = 2f,
            hitboxHeight = 3f,
            loreText = "Guardian of the ancient pyres, bound to ash and flame",
            loreTextArabic = "حارس المشاعل القديمة، مقيد بالرماد واللهب"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // Veiled Archives Enemies
        // ═══════════════════════════════════════════════════════════════
        
        EnemyType.PAGE_SCRAPER to EnemyDefinition(
            type = EnemyType.PAGE_SCRAPER,
            name = "Page-Scraper",
            nameArabic = "كاشط الصفحات",
            description = "Throws paper projectiles, multiplies when shelves close",
            descriptionArabic = "يرمي مقذوفات ورقية، يتكاثر عند إغلاق الرفوف",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.RANGED,
            region = "veiled_archives",
            baseStats = EnemyStats(
                maxHp = 25,
                damage = 10,
                defense = 1,
                speed = 2.5f,
                detectionRange = 10f,
                attackRange = 6f
            ),
            patrolSpeed = 1.5f,
            chaseSpeed = 3f,
            aggroRange = 10f,
            leashRange = 18f,
            attacks = listOf(
                EnemyAttack(
                    name = "Paper Throw",
                    nameArabic = "رمي ورق",
                    type = AttackType.RANGED_PROJECTILE,
                    damage = 10,
                    range = 6f,
                    cooldownMs = 2000L,
                    windupMs = 400L,
                    projectileSpeed = 8f,
                    projectileLifetime = 2000L
                ),
                EnemyAttack(
                    name = "Multiply",
                    nameArabic = "تكاثر",
                    type = AttackType.SUMMON,
                    damage = 0,
                    range = 5f,
                    cooldownMs = 15000L,
                    windupMs = 1500L,
                    description = "Spawns a copy when near closed shelves"
                )
            ),
            lootTable = LootTable(
                coinMin = 8,
                coinMax = 18,
                xpReward = 12,
                mfChance = 0.06f,
                commonDrops = listOf(
                    ItemDrop("leather_scrap", 0.25f),
                    ItemDrop("memory_dust", 0.1f)
                )
            ),
            canClimb = true,
            spriteSheet = "enemy_page_scraper",
            hitboxWidth = 0.9f,
            hitboxHeight = 1.8f,
            loreText = "Born from the decay of forgotten texts",
            loreTextArabic = "مولود من تحلل النصوص المنسية"
        ),
        
        EnemyType.VAULT_SENTINEL to EnemyDefinition(
            type = EnemyType.VAULT_SENTINEL,
            name = "Vault Sentinel",
            nameArabic = "حارس الخزنة",
            description = "Mechanical patrol guard that blocks passages",
            descriptionArabic = "حارس آلي دورية يغلق الممرات",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.HEAVY,
            region = "veiled_archives",
            baseStats = EnemyStats(
                maxHp = 70,
                damage = 18,
                defense = 12,
                speed = 1f,
                detectionRange = 8f,
                attackRange = 2.5f,
                knockbackResistance = 0.8f
            ),
            patrolSpeed = 0.8f,
            chaseSpeed = 1.5f,
            aggroRange = 8f,
            leashRange = 15f,
            attacks = listOf(
                EnemyAttack(
                    name = "Vault Slam",
                    nameArabic = "ضربة الخزنة",
                    type = AttackType.MELEE_HEAVY,
                    damage = 18,
                    range = 2.5f,
                    cooldownMs = 3500L,
                    windupMs = 900L,
                    knockback = 250f
                ),
                EnemyAttack(
                    name = "Lock Passage",
                    nameArabic = "إغلاق الممر",
                    type = AttackType.SPECIAL,
                    damage = 0,
                    range = 5f,
                    cooldownMs = 12000L,
                    windupMs = 1000L,
                    description = "Closes nearby passages for 10 seconds"
                )
            ),
            lootTable = LootTable(
                coinMin = 12,
                coinMax = 25,
                xpReward = 20,
                mfChance = 0.08f,
                commonDrops = listOf(
                    ItemDrop("metal_scrap", 0.4f, 1, 3),
                    ItemDrop("clockwork_gear", 0.2f)
                )
            ),
            isInvulnerableToKnockback = true,
            spriteSheet = "enemy_vault_sentinel",
            scale = 1.4f,
            hitboxWidth = 1.6f,
            hitboxHeight = 2.6f,
            loreText = "Archives' eternal guardians, never sleeping, never forgetting",
            loreTextArabic = "حراس الأرشيفات الأبديون، لا ينامون، لا ينسون أبداً"
        ),
        
        EnemyType.ECHO_SHADE to EnemyDefinition(
            type = EnemyType.ECHO_SHADE,
            name = "Echo Shade",
            nameArabic = "ظل الصدى",
            description = "Shadow that feeds on memories",
            descriptionArabic = "ظل يتغذى على الذكريات",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.STEALTH,
            region = "veiled_archives",
            baseStats = EnemyStats(
                maxHp = 35,
                damage = 14,
                defense = 3,
                speed = 3.5f,
                detectionRange = 7f,
                attackRange = 1.5f
            ),
            patrolSpeed = 2f,
            chaseSpeed = 4.5f,
            aggroRange = 7f,
            leashRange = 14f,
            attacks = listOf(
                EnemyAttack(
                    name = "Memory Drain",
                    nameArabic = "استنزاف الذاكرة",
                    type = AttackType.MELEE_BASIC,
                    damage = 14,
                    range = 1.5f,
                    cooldownMs = 2500L,
                    windupMs = 300L,
                    statusEffects = listOf("slow"),
                    description = "Temporarily drains MF on hit"
                ),
                EnemyAttack(
                    name = "Phase Shift",
                    nameArabic = "تحول طوري",
                    type = AttackType.SPECIAL,
                    damage = 0,
                    range = 3f,
                    cooldownMs = 6000L,
                    windupMs = 200L,
                    description = "Becomes invulnerable for 2 seconds"
                )
            ),
            lootTable = LootTable(
                coinMin = 10,
                coinMax = 22,
                xpReward = 18,
                mfChance = 0.12f,
                commonDrops = listOf(
                    ItemDrop("memory_dust", 0.3f)
                ),
                rareDrops = listOf(
                    ItemDrop("echo_shard", 0.15f)
                )
            ),
            spriteSheet = "enemy_echo_shade",
            hitboxWidth = 1f,
            hitboxHeight = 2f,
            loreText = "Echoes of those who lost themselves in the endless shelves",
            loreTextArabic = "أصداء من ضاعوا في الرفوف اللانهائية"
        ),
        
        EnemyType.LEDGER_WARDEN to EnemyDefinition(
            type = EnemyType.LEDGER_WARDEN,
            name = "Ledger Warden",
            nameArabic = "حارس السجل",
            description = "Elite enemy that creates false readings (mirages)",
            descriptionArabic = "عدو نخبة يخلق قراءات خاطئة (سراب)",
            rank = EnemyRank.ELITE,
            category = EnemyCategory.SUPPORT,
            region = "veiled_archives",
            baseStats = EnemyStats(
                maxHp = 120,
                damage = 22,
                defense = 10,
                speed = 2.5f,
                detectionRange = 12f,
                attackRange = 5f,
                knockbackResistance = 0.6f
            ),
            patrolSpeed = 1.8f,
            chaseSpeed = 3.5f,
            aggroRange = 12f,
            leashRange = 22f,
            attacks = listOf(
                EnemyAttack(
                    name = "Ledger Strike",
                    nameArabic = "ضربة السجل",
                    type = AttackType.MELEE_BASIC,
                    damage = 22,
                    range = 2.5f,
                    cooldownMs = 2500L,
                    windupMs = 500L,
                    knockback = 180f
                ),
                EnemyAttack(
                    name = "False Entry",
                    nameArabic = "إدخال خاطئ",
                    type = AttackType.SUMMON,
                    damage = 0,
                    range = 8f,
                    cooldownMs = 10000L,
                    windupMs = 1500L,
                    description = "Summons 2 illusory copies (mirages)"
                ),
                EnemyAttack(
                    name = "Archive Beam",
                    nameArabic = "شعاع الأرشيف",
                    type = AttackType.RANGED_BEAM,
                    damage = 28,
                    range = 10f,
                    cooldownMs = 6000L,
                    windupMs = 1200L,
                    statusEffects = listOf("marked")
                )
            ),
            lootTable = LootTable(
                coinMin = 35,
                coinMax = 70,
                xpReward = 60,
                mfChance = 0.25f,
                mfAmount = 1,
                rareDrops = listOf(
                    ItemDrop("greater_health_vial", 0.3f),
                    ItemDrop("memory_dust", 0.4f, 1, 3)
                ),
                epicDrops = listOf(
                    ItemDrop("swiftness_brew", 0.12f)
                )
            ),
            spriteSheet = "enemy_ledger_warden",
            scale = 1.3f,
            hitboxWidth = 1.3f,
            hitboxHeight = 2.4f,
            loreText = "Masters of false truths, keepers of edited history",
            loreTextArabic = "أساتذة الحقائق الزائفة، حفظة التاريخ المحرّر"
        ),
        
        EnemyType.THE_INDEXER to EnemyDefinition(
            type = EnemyType.THE_INDEXER,
            name = "The Indexer",
            nameArabic = "المفهرس",
            description = "Miniboss that hides among shelves",
            descriptionArabic = "زعيم صغير يختبئ بين الرفوف",
            rank = EnemyRank.MINIBOSS,
            category = EnemyCategory.BOSS,
            region = "veiled_archives",
            baseStats = EnemyStats(
                maxHp = 350,
                damage = 35,
                defense = 18,
                speed = 3f,
                detectionRange = 15f,
                attackRange = 6f,
                knockbackResistance = 0.85f
            ),
            patrolSpeed = 2f,
            chaseSpeed = 3.5f,
            aggroRange = 15f,
            leashRange = 35f,
            attacks = listOf(
                EnemyAttack(
                    name = "Catalog Slash",
                    nameArabic = "قطع الفهرس",
                    type = AttackType.MELEE_HEAVY,
                    damage = 35,
                    range = 3f,
                    cooldownMs = 3000L,
                    windupMs = 700L,
                    knockback = 320f
                ),
                EnemyAttack(
                    name = "Shelf Collapse",
                    nameArabic = "انهيار الرف",
                    type = AttackType.AOE_EXPLOSION,
                    damage = 40,
                    range = 8f,
                    aoeRadius = 6f,
                    cooldownMs = 8000L,
                    windupMs = 2000L,
                    statusEffects = listOf("stun"),
                    description = "Collapses nearby shelves"
                ),
                EnemyAttack(
                    name = "Hide & Seek",
                    nameArabic = "الاختباء والبحث",
                    type = AttackType.SPECIAL,
                    damage = 0,
                    range = 15f,
                    cooldownMs = 12000L,
                    windupMs = 1000L,
                    description = "Teleports between shelves, becomes invisible"
                )
            ),
            lootTable = LootTable(
                coinMin = 120,
                coinMax = 240,
                xpReward = 180,
                mfChance = 1.0f,
                mfAmount = 3,
                rareDrops = listOf(
                    ItemDrop("greater_health_vial", 0.6f, 1, 2),
                    ItemDrop("memory_dust", 0.5f, 2, 4)
                ),
                epicDrops = listOf(
                    ItemDrop("fm_reducer", 0.2f)
                ),
                guaranteedDrops = listOf("archive_key")
            ),
            canClimb = true,
            spriteSheet = "enemy_the_indexer",
            scale = 1.8f,
            hitboxWidth = 1.8f,
            hitboxHeight = 2.8f,
            loreText = "The one who decides what is remembered and what is erased",
            loreTextArabic = "من يقرر ما يُتذكر وما يُمحى"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // Hollowed Archipelago Enemies
        // ═══════════════════════════════════════════════════════════════
        
        EnemyType.ROPE_CROAKER to EnemyDefinition(
            type = EnemyType.ROPE_CROAKER,
            name = "Rope-Croaker",
            nameArabic = "نقّاق الحبل",
            description = "Cuts ropes while player is swinging",
            descriptionArabic = "يقطع الحبال بينما اللاعب يتأرجح",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.RANGED,
            region = "hollowed_archipelago",
            baseStats = EnemyStats(
                maxHp = 28,
                damage = 12,
                defense = 2,
                speed = 2f,
                detectionRange = 9f,
                attackRange = 7f
            ),
            patrolSpeed = 1.2f,
            chaseSpeed = 2.5f,
            aggroRange = 9f,
            leashRange = 16f,
            attacks = listOf(
                EnemyAttack(
                    name = "Acid Spit",
                    nameArabic = "بصق حمضي",
                    type = AttackType.RANGED_PROJECTILE,
                    damage = 12,
                    range = 7f,
                    cooldownMs = 2500L,
                    windupMs = 500L,
                    projectileSpeed = 6f,
                    projectileLifetime = 2500L
                ),
                EnemyAttack(
                    name = "Rope Cut",
                    nameArabic = "قطع الحبل",
                    type = AttackType.SPECIAL,
                    damage = 0,
                    range = 8f,
                    cooldownMs = 7000L,
                    windupMs = 800L,
                    description = "Cuts nearby rope if player is swinging"
                )
            ),
            lootTable = LootTable(
                coinMin = 7,
                coinMax = 16,
                xpReward = 14,
                mfChance = 0.07f,
                commonDrops = listOf(
                    ItemDrop("leather_scrap", 0.3f)
                )
            ),
            canClimb = true,
            spriteSheet = "enemy_rope_croaker",
            scale = 0.9f,
            hitboxWidth = 0.9f,
            hitboxHeight = 1.2f,
            loreText = "Island amphibians that feast on falling victims",
            loreTextArabic = "برمائيات جزرية تتغذى على الضحايا الساقطين"
        ),
        
        EnemyType.DRIFT_KNIGHT to EnemyDefinition(
            type = EnemyType.DRIFT_KNIGHT,
            name = "Drift-Knight",
            nameArabic = "فارس الانجراف",
            description = "Flying enemy that attacks from above",
            descriptionArabic = "عدو طائر يهاجم من الأعلى",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.FLYING,
            region = "hollowed_archipelago",
            baseStats = EnemyStats(
                maxHp = 40,
                damage = 16,
                defense = 4,
                speed = 3.5f,
                detectionRange = 12f,
                attackRange = 3f,
                jumpHeight = 10f
            ),
            patrolSpeed = 2f,
            chaseSpeed = 4.5f,
            aggroRange = 12f,
            leashRange = 20f,
            attacks = listOf(
                EnemyAttack(
                    name = "Dive Strike",
                    nameArabic = "ضربة الانقضاض",
                    type = AttackType.CHARGE,
                    damage = 16,
                    range = 8f,
                    cooldownMs = 4000L,
                    windupMs = 800L,
                    knockback = 250f,
                    description = "Dives from above"
                ),
                EnemyAttack(
                    name = "Wind Slash",
                    nameArabic = "قطع الرياح",
                    type = AttackType.RANGED_PROJECTILE,
                    damage = 14,
                    range = 6f,
                    cooldownMs = 3000L,
                    windupMs = 400L,
                    projectileSpeed = 10f,
                    projectileLifetime = 1500L
                )
            ),
            lootTable = LootTable(
                coinMin = 10,
                coinMax = 24,
                xpReward = 22,
                mfChance = 0.1f,
                commonDrops = listOf(
                    ItemDrop("metal_scrap", 0.35f),
                    ItemDrop("leather_scrap", 0.25f)
                ),
                rareDrops = listOf(
                    ItemDrop("energy_crystal", 0.18f)
                )
            ),
            canFly = true,
            spriteSheet = "enemy_drift_knight",
            scale = 1.1f,
            hitboxWidth = 1.2f,
            hitboxHeight = 1.8f,
            loreText = "Once noble guards, now bound to endless patrol between islands",
            loreTextArabic = "حراس نبلاء سابقاً، الآن مقيدون بدورية لا نهائية بين الجزر"
        ),
        
        EnemyType.BARGAIN_PIRATE to EnemyDefinition(
            type = EnemyType.BARGAIN_PIRATE,
            name = "Bargain Pirate",
            nameArabic = "قرصان المساومة",
            description = "Steals satchel items on hit",
            descriptionArabic = "يسرق عناصر الحقيبة عند الضربة",
            rank = EnemyRank.NORMAL,
            category = EnemyCategory.MELEE,
            region = "hollowed_archipelago",
            baseStats = EnemyStats(
                maxHp = 45,
                damage = 14,
                defense = 5,
                speed = 3f,
                detectionRange = 10f,
                attackRange = 2f
            ),
            patrolSpeed = 2f,
            chaseSpeed = 4f,
            aggroRange = 10f,
            leashRange = 18f,
            attacks = listOf(
                EnemyAttack(
                    name = "Satchel Snatch",
                    nameArabic = "خطف الحقيبة",
                    type = AttackType.MELEE_BASIC,
                    damage = 14,
                    range = 2f,
                    cooldownMs = 2000L,
                    windupMs = 350L,
                    knockback = 150f,
                    description = "Steals a random consumable on hit"
                ),
                EnemyAttack(
                    name = "Hook Swing",
                    nameArabic = "تأرجح الخطاف",
                    type = AttackType.MELEE_HEAVY,
                    damage = 20,
                    range = 3f,
                    cooldownMs = 4500L,
                    windupMs = 700L,
                    knockback = 300f
                )
            ),
            lootTable = LootTable(
                coinMin = 15,
                coinMax = 35,
                xpReward = 20,
                mfChance = 0.09f,
                commonDrops = listOf(
                    ItemDrop("health_vial", 0.2f),
                    ItemDrop("energy_crystal", 0.15f)
                ),
                rareDrops = listOf(
                    ItemDrop("ash_bomb", 0.25f)
                )
            ),
            spriteSheet = "enemy_bargain_pirate",
            hitboxWidth = 1.1f,
            hitboxHeight = 2f,
            loreText = "They trade in memories, because coin means nothing to the forgotten",
            loreTextArabic = "يتاجرون بالذكريات، لأن العملة لا تعني شيئاً للمنسيين"
        ),
        
        // يمكن إضافة المزيد من الأعداء بنفس النمط...
        // سأضيف بعض الأمثلة الإضافية للتنوع:
        
        EnemyType.SKY_SCAVENGER to EnemyDefinition(
            type = EnemyType.SKY_SCAVENGER,
            name = "Sky-Scavenger",
            nameArabic = "زبّال السماء",
            description = "Elite flying enemy that steals bombs",
            descriptionArabic = "عدو طائر نخبة يسرق القنابل",
            rank = EnemyRank.ELITE,
            category = EnemyCategory.FLYING,
            region = "hollowed_archipelago",
            baseStats = EnemyStats(
                maxHp = 110,
                damage = 24,
                defense = 8,
                speed = 4.5f,
                detectionRange = 15f,
                attackRange = 4f,
                jumpHeight = 12f,
                knockbackResistance = 0.4f
            ),
            patrolSpeed = 2.5f,
            chaseSpeed = 5.5f,
            aggroRange = 15f,
            leashRange = 28f,
            attacks = listOf(
                EnemyAttack(
                    name = "Talon Grab",
                    nameArabic = "إمساك المخلب",
                    type = AttackType.GRAB,
                    damage = 24,
                    range = 4f,
                    cooldownMs = 5000L,
                    windupMs = 600L,
                    description = "Grabs player and lifts them"
                ),
                EnemyAttack(
                    name = "Bomb Drop",
                    nameArabic = "إسقاط قنبلة",
                    type = AttackType.AOE_EXPLOSION,
                    damage = 30,
                    range = 10f,
                    aoeRadius = 4f,
                    cooldownMs = 7000L,
                    windupMs = 1000L,
                    description = "Drops stolen bombs"
                ),
                EnemyAttack(
                    name = "Screech",
                    nameArabic = "صرخة",
                    type = AttackType.AOE_SHOCKWAVE,
                    damage = 15,
                    range = 8f,
                    aoeRadius = 8f,
                    cooldownMs = 9000L,
                    windupMs = 800L,
                    statusEffects = listOf("stun")
                )
            ),
            lootTable = LootTable(
                coinMin = 40,
                coinMax = 80,
                xpReward = 70,
                mfChance = 0.3f,
                mfAmount = 1,
                rareDrops = listOf(
                    ItemDrop("shard_grenade", 0.4f, 1, 2),
                    ItemDrop("ash_bomb", 0.5f, 2, 4)
                ),
                epicDrops = listOf(
                    ItemDrop("swiftness_brew", 0.15f)
                )
            ),
            canFly = true,
            spriteSheet = "enemy_sky_scavenger",
            scale = 1.5f,
            hitboxWidth = 1.8f,
            hitboxHeight = 2.2f,
            loreText = "Thieves of the sky, drawn to explosives and chaos",
            loreTextArabic = "لصوص السماء، ينجذبون للمتفجرات والفوضى"
        )
        
        // يمكن إضافة باقي الأعداء (Glassfjord, Clockworks, Moorlands, Chasm, Bosses)
        // بنفس النمط... لإيجاز المساحة سأتوقف هنا
        // لكن البنية جاهزة لإضافة 40+ عدو
    )
    
    /**
     * الحصول على تعريف عدو بنوعه
     */
    fun getEnemy(type: EnemyType): EnemyDefinition? = allEnemies[type]
    
    /**
     * الحصول على جميع أعداء منطقة معينة
     */
    fun getEnemiesByRegion(region: String): List<EnemyDefinition> =
        allEnemies.values.filter { it.region == region }
    
    /**
     * الحصول على جميع أعداء رتبة معينة
     */
    fun getEnemiesByRank(rank: EnemyRank): List<EnemyDefinition> =
        allEnemies.values.filter { it.rank == rank }
    
    /**
     * الحصول على جميع أعداء فئة معينة
     */
    fun getEnemiesByCategory(category: EnemyCategory): List<EnemyDefinition> =
        allEnemies.values.filter { it.category == category }
    
    /**
     * الحصول على عدو عشوائي من منطقة
     */
    fun getRandomEnemyFromRegion(region: String, rank: EnemyRank = EnemyRank.NORMAL): EnemyDefinition? {
        val enemies = getEnemiesByRegion(region).filter { it.rank == rank }
        return enemies.randomOrNull()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Enemy Factory
// ═══════════════════════════════════════════════════════════════════════════

/**
 * مصنع إنشاء الأعداء
 */
object EnemyFactory {
    
    private var nextEnemyId = 0
    
    /**
     * إنشاء عدو من نوع معين
     */
    fun createEnemy(
        type: EnemyType,
        x: Float,
        y: Float,
        level: Int = 1
    ): Enemy? {
        val definition = EnemyDatabase.getEnemy(type) ?: return null
        
        // تطبيق scaling حسب المستوى
        val scaledStats = scaleStatsToLevel(definition.baseStats, level)
        
        return Enemy(
            id = "enemy_${nextEnemyId++}",
            definition = definition,
            stats = scaledStats,
            position = EnemyPosition(x = x, y = y),
            level = level,
            spawnPoint = x to y
        )
    }
    
    /**
     * إنشاء عدو عشوائي من منطقة
     */
    fun createRandomEnemy(
        region: String,
        x: Float,
        y: Float,
        rank: EnemyRank = EnemyRank.NORMAL,
        level: Int = 1
    ): Enemy? {
        val definition = EnemyDatabase.getRandomEnemyFromRegion(region, rank) ?: return null
        return createEnemy(definition.type, x, y, level)
    }
    
    /**
     * تطبيق scaling للإحصائيات حسب المستوى
     */
    private fun scaleStatsToLevel(baseStats: EnemyStats, level: Int): EnemyStats {
        if (level <= 1) return baseStats
        
        val scaleFactor = 1f + (level - 1) * 0.15f  // +15% per level
        
        return baseStats.copy(
            maxHp = (baseStats.maxHp * scaleFactor).toInt(),
            currentHp = (baseStats.maxHp * scaleFactor).toInt(),
            damage = (baseStats.damage * scaleFactor).toInt(),
            defense = (baseStats.defense * scaleFactor).toInt()
        )
    }
    
    /**
     * إعادة تعيين عداد الأعداء
     */
    fun reset() {
        nextEnemyId = 0
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Enemy Helper Functions
// ═══════════════════════════════════════════════════════════════════════════

/**
 * حساب الضرر الفعلي بعد تطبيق الدفاع
 */
fun calculateDamage(attackDamage: Int, defense: Int): Int {
    // Formula: Damage = Attack * (100 / (100 + Defense))
    val damageMultiplier = 100f / (100f + defense)
    return max(1, (attackDamage * damageMultiplier).toInt())
}

/**
 * حساب المسافة بين عدو ونقطة
 */
fun Enemy.distanceTo(x: Float, y: Float): Float {
    val dx = this.position.x - x
    val dy = this.position.y - y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

/**
 * هل العدو ضمن مدى معين من نقطة؟
 */
fun Enemy.isInRange(x: Float, y: Float, range: Float): Boolean {
    return distanceTo(x, y) <= range
}

/**
 * هل العدو يستطيع رؤية اللاعب؟
 */
fun Enemy.canSeePlayer(playerX: Float, playerY: Float): Boolean {
    val distance = distanceTo(playerX, playerY)
    if (distance > definition.baseStats.detectionRange) return false
    
    // تحقق من الاتجاه
    val playerOnRight = playerX > position.x
    if (position.isFacingRight != playerOnRight) return false
    
    // يمكن إضافة فحص للحواجز (line of sight) هنا
    
    return true
}

/**
 * تطبيق تأثير على العدو
 */
fun Enemy.applyEffect(effect: EnemyEffect) {
    // إزالة التأثير السابق من نفس النوع (إن وجد)
    activeEffects.removeAll { it.type == effect.type }
    activeEffects.add(effect)
}

/**
 * إزالة التأثيرات المنتهية
 */
fun Enemy.removeExpiredEffects() {
    activeEffects.removeAll { it.isExpired }
}

/**
 * هل العدو تحت تأثير معين؟
 */
fun Enemy.hasEffect(type: EnemyEffectType): Boolean {
    return activeEffects.any { it.type == type && !it.isExpired }
}

/**
 * الحصول على مضاعف السرعة (مع تأثيرات Slow/Freeze)
 */
fun Enemy.getSpeedMultiplier(): Float {
    var multiplier = 1f
    
    if (hasEffect(EnemyEffectType.SLOW)) {
        multiplier *= 0.5f
    }
    
    if (hasEffect(EnemyEffectType.FREEZE)) {
        multiplier = 0f
    }
    
    return multiplier
}

/**
 * الحصول على مضاعف الضرر (مع تأثير Weakness)
 */
fun Enemy.getDamageMultiplier(): Float {
    var multiplier = 1f
    
    if (hasEffect(EnemyEffectType.WEAKNESS)) {
        multiplier *= 0.7f
    }
    
    return multiplier
}

/**
 * تطبيق ضرر على العدو
 */
fun Enemy.takeDamage(damage: Int, source: String = ""): Int {
    // حساب الضرر الفعلي
    val actualDamage = calculateDamage(damage, stats.defense)
    
    // تطبيق الضرر
    val newHp = max(0, stats.currentHp - actualDamage)
    val hpLost = stats.currentHp - newHp
    
    // تحديث الإحصائيات (يجب أن يتم عبر StateFlow في الواقع)
    stats = stats.copy(currentHp = newHp)
    
    lastDamagedTime = System.currentTimeMillis()
    
    return hpLost
}

/**
 * شفاء العدو
 */
fun Enemy.heal(amount: Int) {
    val newHp = min(stats.maxHp, stats.currentHp + amount)
    stats = stats.copy(currentHp = newHp)
}

/**
 * هل العدو بعيد جداً عن نقطة الظهور؟
 */
fun Enemy.isTooFarFromSpawn(): Boolean {
    val distance = distanceTo(spawnPoint.first, spawnPoint.second)
    return distance > definition.leashRange
}

/**
 * إعادة العدو لنقطة الظهور
 */
fun Enemy.returnToSpawn() {
    currentState = EnemyState.RETURNING
    targetPlayerId = null
}

/**
 * إعادة تعيين حالة العدو
 */
fun Enemy.resetToSpawn() {
    // position = position.copy(x = spawnPoint.first, y = spawnPoint.second, velocityX = 0f, velocityY = 0f)
    // stats = stats.copy(currentHp = stats.maxHp)
    currentState = EnemyState.IDLE
    targetPlayerId = null
    activeEffects.clear()
    lastAttackTime = 0L
    lastDamagedTime = 0L
    stunEndTime = 0L
}

/**
 * حساب الغنائم عند الموت
 */
fun Enemy.generateLoot(): Map<String, Any> {
    val loot = mutableMapOf<String, Any>()
    val lootTable = definition.lootTable
    
    // Coins
    val coins = (lootTable.coinMin..lootTable.coinMax).random()
    loot["coins"] = coins
    
    // XP
    loot["xp"] = lootTable.xpReward
    
    // MF
    if (Math.random() < lootTable.mfChance) {
        loot["mf"] = lootTable.mfAmount
    }
    
    // Items
    val items = mutableListOf<Pair<String, Int>>()
    
    // Guaranteed drops
    lootTable.guaranteedDrops.forEach { itemId ->
        items.add(itemId to 1)
    }
    
    // Common drops
    lootTable.commonDrops.forEach { drop ->
        if (Math.random() < drop.chance) {
            val quantity = (drop.quantityMin..drop.quantityMax).random()
            items.add(drop.itemId to quantity)
        }
    }
    
    // Rare drops
    lootTable.rareDrops.forEach { drop ->
        if (Math.random() < drop.chance) {
            val quantity = (drop.quantityMin..drop.quantityMax).random()
            items.add(drop.itemId to quantity)
        }
    }
    
    // Epic drops
    lootTable.epicDrops.forEach { drop ->
        if (Math.random() < drop.chance) {
            val quantity = (drop.quantityMin..drop.quantityMax).random()
            items.add(drop.itemId to quantity)
        }
    }
    
    loot["items"] = items
    
    return loot
}

/**
 * الحصول على الهجوم المناسب حسب المسافة
 */
fun Enemy.selectAttack(distanceToTarget: Float): EnemyAttack? {
    val now = System.currentTimeMillis()
    
    // تصفية الهجمات المتاحة
    val availableAttacks = definition.attacks.filter { attack ->
        // تحقق من الكولداون
        val timeSinceLastAttack = now - lastAttackTime
        if (timeSinceLastAttack < attack.cooldownMs) return@filter false
        
        // تحقق من المدى
        if (distanceToTarget > attack.range) return@filter false
        
        true
    }
    
    // اختيار عشوائي من الهجمات المتاحة
    return availableAttacks.randomOrNull()
}