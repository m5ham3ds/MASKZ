package com.erygra.maskoflight.player

import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * InventorySystem.kt — نظام المخزون الشامل للعبة "قِنَاعُ النُّور"
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * يدير جميع العناصر التي يحملها اللاعب من أسلحة، أدوات، مواد، آثار، ومفاتيح.
 * يتضمن:
 * - نظام الحقائب (Satchels) القابلة للترقية
 * - العناصر الاستهلاكية (Potions, Bombs, Tools)
 * - الأسلحة والآثار (Weapons, Relics, Key Items)
 * - المواد الخام (Materials, Crafting Components)
 * - نظام التجهيز (Equip/Unequip)
 * - نظام الصناعة (Crafting - Optional)
 * - تكامل مع PlayerStateManager, EventBus
 * 
 * @author Erygra Universe Development Team
 * @version 2.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Item Data Classes
// ═══════════════════════════════════════════════════════════════════════════

/**
 * نوع العنصر
 */
enum class ItemType {
    // الأسلحة
    WEAPON_PRIMARY,      // السلاح الأساسي
    WEAPON_SECONDARY,    // السلاح الثانوي
    
    // الأدوات الاستهلاكية
    CONSUMABLE,          // عنصر استهلاكي عام
    CONSUMABLE_HEALTH,   // شفاء
    CONSUMABLE_ENERGY,   // طاقة
    CONSUMABLE_BUFF,     // تعزيز مؤقت
    CONSUMABLE_UTILITY,  // أداة استخدام واحد
    
    // الأدوات القابلة لإعادة الاستخدام
    TOOL_THROWABLE,      // قنابل، زجاجات
    TOOL_UTILITY,        // أدوات استكشاف
    
    // الآثار (Relics)
    RELIC_PASSIVE,       // تأثير دائم
    RELIC_ACTIVE,        // قدرة نشطة
    
    // المواد
    MATERIAL_COMMON,     // مواد شائعة
    MATERIAL_RARE,       // مواد نادرة
    MATERIAL_LEGENDARY,  // مواد أسطورية
    
    // العناصر الرئيسية
    KEY_ITEM,            // مفاتيح، وثائق، عناصر قصة
    
    // الترقيات
    UPGRADE_SATCHEL,     // ترقية الحقيبة
    UPGRADE_ABILITY,     // ترقية القدرات
    
    // التجميل
    COSMETIC             // عناصر تجميلية
}

/**
 * ندرة العنصر / Rarity
 // Typealias for compatibility
 */
typealias ItemRarity = Rarity

/**
 * فئة العنصر (للتصنيف في الواجهة)
 */
enum class ItemCategory {
    WEAPONS,      // أسلحة
    CONSUMABLES,  // استهلاكيات
    TOOLS,        // أدوات
    RELICS,       // آثار
    MATERIALS,    // مواد
    KEY_ITEMS,    // عناصر رئيسية
    UPGRADES,     // ترقيات
    COSMETICS     // تجميل
}

/**
 * تعريف العنصر
 */
data class Item(
    val id: String,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    val type: ItemType,
    val category: ItemCategory,
    val rarity: ItemRarity,
    
    // الخصائص الأساسية
    val maxStack: Int = 1,           // الحد الأقصى للتكديس
    val value: Int = 0,              // القيمة بالعملة
    val weight: Float = 0f,          // الوزن (للحقائب)
    
    // الخصائص القتالية (للأسلحة)
    val damage: Int = 0,
    val attackSpeed: Float = 1f,
    val range: Float = 0f,
    val critChance: Float = 0f,
    
    // التأثيرات (للاستهلاكيات والآثار)
    val healAmount: Int = 0,
    val energyAmount: Int = 0,
    val buffEffects: List<EffectType> = emptyList(),
    val buffDuration: Long = 0L,
    
    // الاستخدام
    val isConsumable: Boolean = false,
    val isEquippable: Boolean = false,
    val isDroppable: Boolean = true,
    val isTradeable: Boolean = true,
    val isQuestItem: Boolean = false,
    
    // الموارد
    val iconResource: String = "",
    val spriteResource: String = "",
    val soundEffect: String = "",
    
    // الشروط
    val requiredLevel: Int = 0,
    val requiredMF: Int = 0,
    val requiredQuest: String = "",
    
    // البيانات الوصفية
    val loreText: String = "",
    val loreTextArabic: String = "",
    val tags: List<String> = emptyList()
)

/**
 * نسخة من عنصر في المخزون
 */
data class InventoryItem(
    val item: Item,
    val quantity: Int = 1,
    val equipped: Boolean = false,
    val slot: Int = -1,              // موقع العنصر في الحقيبة (-1 = غير محدد)
    val durability: Float = 100f,    // للأسلحة/الأدوات القابلة للكسر
    val customData: Map<String, Any> = emptyMap()  // بيانات مخصصة
) {
    /**
     * هل العنصر قابل للتكديس مع عنصر آخر؟
     */
    fun canStackWith(other: InventoryItem): Boolean {
        return item.id == other.item.id &&
               !equipped &&
               !other.equipped &&
               quantity + other.quantity <= item.maxStack
    }
    
    /**
     * هل العنصر قابل للاستخدام؟
     */
    fun canUse(): Boolean {
        return item.isConsumable && quantity > 0
    }
    
    /**
     * هل العنصر قابل للتجهيز؟
     */
    fun canEquip(): Boolean {
        return item.isEquippable && !equipped
    }
}

/**
 * نوع الحقيبة
 */
enum class SatchelType {
    LEFT,        // الحقيبة اليسرى
    RIGHT,       // الحقيبة اليمنى
    BACK         // حقيبة الظهر (إضافية)
}

/**
 * الحقيبة (Satchel)
 */
data class Satchel(
    val type: SatchelType,
    val capacity: Int = 10,          // عدد الخانات
    val maxWeight: Float = 50f,      // الوزن الأقصى
    val level: Int = 1,              // مستوى الحقيبة
    val specialization: SatchelSpecialization? = null
)

/**
 * تخصص الحقيبة (يمنح مزايا)
 */
enum class SatchelSpecialization {
    COMBAT,       // +20% weapon damage
    UTILITY,      // +30% tool effectiveness
    ALCHEMY,      // +50% potion duration
    SCAVENGER,    // +20% drop rate
    LIGHTWEIGHT   // -30% weight
}

/**
 * نتيجة محاولة إضافة/إزالة عنصر
 */
sealed class InventoryResult {
    data class Success(val message: String = "") : InventoryResult()
    data class Failure(val reason: InventoryFailureReason) : InventoryResult()
}

/**
 * أسباب فشل عمليات المخزون
 */
enum class InventoryFailureReason {
    INVENTORY_FULL,      // المخزون ممتلئ
    ITEM_NOT_FOUND,      // العنصر غير موجود
    INSUFFICIENT_QUANTITY, // الكمية غير كافية
    CANNOT_DROP,         // لا يمكن إسقاط العنصر
    CANNOT_EQUIP,        // لا يمكن تجهيز العنصر
    ALREADY_EQUIPPED,    // العنصر مجهز بالفعل
    REQUIREMENTS_NOT_MET, // الشروط غير محققة
    WEIGHT_EXCEEDED,     // تجاوز الوزن
    INVALID_SLOT         // خانة غير صالحة
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Item Database
// ═══════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات العناصر
 */
object ItemDatabase {
    
    /**
     * جميع العناصر المتاحة في اللعبة
     */
    val allItems = mapOf(
        
        // ═══════════════════════════════════════════════════════════════
        // الأسلحة
        // ═══════════════════════════════════════════════════════════════
        
        "satchel_blade" to Item(
            id = "satchel_blade",
            name = "Satchel Blade",
            nameArabic = "نصل الحقيبة",
            description = "A short blade concealed in the satchel",
            descriptionArabic = "نصل قصير مخفي في الحقيبة",
            type = ItemType.WEAPON_PRIMARY,
            category = ItemCategory.WEAPONS,
            rarity = ItemRarity.COMMON,
            damage = 15,
            attackSpeed = 1.2f,
            range = 1.5f,
            critChance = 0.05f,
            value = 50,
            weight = 2f,
            isEquippable = true,
            iconResource = "weapon_satchel_blade",
            spriteResource = "sprite_satchel_blade"
        ),
        
        "memory_staff" to Item(
            id = "memory_staff",
            name = "Memory Staff",
            nameArabic = "عصا الذاكرة",
            description = "A staff that channels memory fragments",
            descriptionArabic = "عصا تُوجه شظايا الذاكرة",
            type = ItemType.WEAPON_PRIMARY,
            category = ItemCategory.WEAPONS,
            rarity = ItemRarity.RARE,
            damage = 25,
            attackSpeed = 0.8f,
            range = 2.5f,
            critChance = 0.15f,
            value = 500,
            weight = 4f,
            isEquippable = true,
            requiredMF = 10,
            iconResource = "weapon_memory_staff",
            spriteResource = "sprite_memory_staff",
            loreText = "Forged from condensed memories of forgotten scholars",
            loreTextArabic = "مُصاغ من ذكريات مكثفة لعلماء منسيين"
        ),
        
        "void_dagger" to Item(
            id = "void_dagger",
            name = "Void Dagger",
            nameArabic = "خنجر الفراغ",
            description = "A dagger that phases through armor",
            descriptionArabic = "خنجر يخترق الدروع",
            type = ItemType.WEAPON_PRIMARY,
            category = ItemCategory.WEAPONS,
            rarity = ItemRarity.EPIC,
            damage = 35,
            attackSpeed = 1.5f,
            range = 1.2f,
            critChance = 0.25f,
            value = 1200,
            weight = 1.5f,
            isEquippable = true,
            requiredLevel = 10,
            buffEffects = listOf(EffectType.ARMOR_PIERCE),
            iconResource = "weapon_void_dagger",
            spriteResource = "sprite_void_dagger",
            loreText = "Tempered in the Luminous Chasm's deepest void",
            loreTextArabic = "مُقوى في أعمق فراغات الهاوية المضيئة"
        ),
        
        "mask_bow" to Item(
            id = "mask_bow",
            name = "Mask Bow",
            nameArabic = "قوس القناع",
            description = "Fires arrows infused with mask energy",
            descriptionArabic = "يطلق سهاماً مشحونة بطاقة القناع",
            type = ItemType.WEAPON_SECONDARY,
            category = ItemCategory.WEAPONS,
            rarity = ItemRarity.RARE,
            damage = 20,
            attackSpeed = 1f,
            range = 8f,
            critChance = 0.20f,
            value = 800,
            weight = 3f,
            isEquippable = true,
            requiredMF = 15,
            iconResource = "weapon_mask_bow",
            spriteResource = "sprite_mask_bow"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // الاستهلاكيات - شفاء
        // ═══════════════════════════════════════════════════════════════
        
        "health_vial" to Item(
            id = "health_vial",
            name = "Health Vial",
            nameArabic = "قارورة صحة",
            description = "Restores 30 HP",
            descriptionArabic = "تستعيد 30 نقطة صحة",
            type = ItemType.CONSUMABLE_HEALTH,
            category = ItemCategory.CONSUMABLES,
            rarity = ItemRarity.COMMON,
            maxStack = 10,
            healAmount = 30,
            value = 20,
            weight = 0.5f,
            isConsumable = true,
            iconResource = "consumable_health_vial",
            soundEffect = "sfx_drink_potion"
        ),
        
        "greater_health_vial" to Item(
            id = "greater_health_vial",
            name = "Greater Health Vial",
            nameArabic = "قارورة صحة كبرى",
            description = "Restores 60 HP",
            descriptionArabic = "تستعيد 60 نقطة صحة",
            type = ItemType.CONSUMABLE_HEALTH,
            category = ItemCategory.CONSUMABLES,
            rarity = ItemRarity.UNCOMMON,
            maxStack = 5,
            healAmount = 60,
            value = 50,
            weight = 0.8f,
            isConsumable = true,
            iconResource = "consumable_health_vial_greater",
            soundEffect = "sfx_drink_potion"
        ),
        
        "memory_elixir" to Item(
            id = "memory_elixir",
            name = "Memory Elixir",
            nameArabic = "إكسير الذاكرة",
            description = "Fully restores HP and Energy",
            descriptionArabic = "يستعيد الصحة والطاقة بالكامل",
            type = ItemType.CONSUMABLE_HEALTH,
            category = ItemCategory.CONSUMABLES,
            rarity = ItemRarity.RARE,
            maxStack = 3,
            healAmount = 9999,  // رمز للشفاء الكامل
            energyAmount = 9999,
            value = 200,
            weight = 1f,
            isConsumable = true,
            iconResource = "consumable_memory_elixir",
            soundEffect = "sfx_drink_elixir"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // الاستهلاكيات - طاقة
        // ═══════════════════════════════════════════════════════════════
        
        "energy_crystal" to Item(
            id = "energy_crystal",
            name = "Energy Crystal",
            nameArabic = "بلورة طاقة",
            description = "Restores 25 Energy",
            descriptionArabic = "تستعيد 25 نقطة طاقة",
            type = ItemType.CONSUMABLE_ENERGY,
            category = ItemCategory.CONSUMABLES,
            rarity = ItemRarity.COMMON,
            maxStack = 10,
            energyAmount = 25,
            value = 15,
            weight = 0.3f,
            isConsumable = true,
            iconResource = "consumable_energy_crystal",
            soundEffect = "sfx_consume_crystal"
        ),
        
        "echo_shard" to Item(
            id = "echo_shard",
            name = "Echo Shard",
            nameArabic = "شظية صدى",
            description = "Restores 50 Energy",
            descriptionArabic = "تستعيد 50 نقطة طاقة",
            type = ItemType.CONSUMABLE_ENERGY,
            category = ItemCategory.CONSUMABLES,
            rarity = ItemRarity.UNCOMMON,
            maxStack = 5,
            energyAmount = 50,
            value = 40,
            weight = 0.5f,
            isConsumable = true,
            iconResource = "consumable_echo_shard",
            soundEffect = "sfx_consume_crystal"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // الاستهلاكيات - تعزيز
        // ═══════════════════════════════════════════════════════════════
        
        "strength_tonic" to Item(
            id = "strength_tonic",
            name = "Strength Tonic",
            nameArabic = "منشط القوة",
            description = "+30% damage for 60 seconds",
            descriptionArabic = "+30% ضرر لمدة 60 ثانية",
            type = ItemType.CONSUMABLE_BUFF,
            category = ItemCategory.CONSUMABLES,
            rarity = ItemRarity.UNCOMMON,
            maxStack = 5,
            buffEffects = listOf(EffectType.DAMAGE_BOOST),
            buffDuration = 60000L,
            value = 60,
            weight = 0.5f,
            isConsumable = true,
            iconResource = "consumable_strength_tonic",
            soundEffect = "sfx_drink_tonic"
        ),
        
        "swiftness_brew" to Item(
            id = "swiftness_brew",
            name = "Swiftness Brew",
            nameArabic = "جرعة السرعة",
            description = "+20% movement speed for 90 seconds",
            descriptionArabic = "+20% سرعة حركة لمدة 90 ثانية",
            type = ItemType.CONSUMABLE_BUFF,
            category = ItemCategory.CONSUMABLES,
            rarity = ItemRarity.UNCOMMON,
            maxStack = 5,
            buffEffects = listOf(EffectType.SPEED_BOOST),
            buffDuration = 90000L,
            value = 50,
            weight = 0.5f,
            isConsumable = true,
            iconResource = "consumable_swiftness_brew",
            soundEffect = "sfx_drink_tonic"
        ),
        
        "fm_reducer" to Item(
            id = "fm_reducer",
            name = "Memory Clarity Potion",
            nameArabic = "جرعة وضوح الذاكرة",
            description = "Reduces FM by 5",
            descriptionArabic = "تقلل FM بمقدار 5",
            type = ItemType.CONSUMABLE_UTILITY,
            category = ItemCategory.CONSUMABLES,
            rarity = ItemRarity.RARE,
            maxStack = 3,
            value = 300,
            weight = 1f,
            isConsumable = true,
            iconResource = "consumable_fm_reducer",
            soundEffect = "sfx_drink_elixir"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // الأدوات - قابلة للرمي
        // ═══════════════════════════════════════════════════════════════
        
        "ash_bomb" to Item(
            id = "ash_bomb",
            name = "Ash Bomb",
            nameArabic = "قنبلة رماد",
            description = "Deals 30 damage in small area",
            descriptionArabic = "تسبب 30 ضرراً في منطقة صغيرة",
            type = ItemType.TOOL_THROWABLE,
            category = ItemCategory.TOOLS,
            rarity = ItemRarity.COMMON,
            maxStack = 15,
            damage = 30,
            range = 5f,
            value = 10,
            weight = 0.5f,
            isConsumable = true,
            iconResource = "tool_ash_bomb",
            soundEffect = "sfx_throw_bomb"
        ),
        
        "shard_grenade" to Item(
            id = "shard_grenade",
            name = "Shard Grenade",
            nameArabic = "قنبلة شظايا",
            description = "Deals 50 damage and blinds enemies",
            descriptionArabic = "تسبب 50 ضرراً وتعمي الأعداء",
            type = ItemType.TOOL_THROWABLE,
            category = ItemCategory.TOOLS,
            rarity = ItemRarity.UNCOMMON,
            maxStack = 10,
            damage = 50,
            range = 6f,
            buffEffects = listOf(EffectType.BLIND),
            buffDuration = 3000L,
            value = 30,
            weight = 0.8f,
            isConsumable = true,
            iconResource = "tool_shard_grenade",
            soundEffect = "sfx_throw_grenade"
        ),
        
        "smoke_bottle" to Item(
            id = "smoke_bottle",
            name = "Smoke Bottle",
            nameArabic = "زجاجة دخان",
            description = "Creates smoke cloud for stealth",
            descriptionArabic = "تخلق سحابة دخان للتخفي",
            type = ItemType.TOOL_THROWABLE,
            category = ItemCategory.TOOLS,
            rarity = ItemRarity.COMMON,
            maxStack = 10,
            value = 15,
            weight = 0.4f,
            isConsumable = true,
            iconResource = "tool_smoke_bottle",
            soundEffect = "sfx_throw_bottle"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // الأدوات - استكشاف
        // ═══════════════════════════════════════════════════════════════
        
        "rope_harness" to Item(
            id = "rope_harness",
            name = "Rope Harness",
            nameArabic = "حزام الحبل",
            description = "Allows rope swinging",
            descriptionArabic = "يسمح بالتأرجح على الحبال",
            type = ItemType.TOOL_UTILITY,
            category = ItemCategory.TOOLS,
            rarity = ItemRarity.UNCOMMON,
            value = 100,
            weight = 2f,
            isEquippable = true,
            isDroppable = false,
            iconResource = "tool_rope_harness"
        ),
        
        "biolume_talisman" to Item(
            id = "biolume_talisman",
            name = "Biolume Talisman",
            nameArabic = "تعويذة البيولومين",
            description = "Reveals hidden secrets nearby",
            descriptionArabic = "تكشف الأسرار المخفية القريبة",
            type = ItemType.TOOL_UTILITY,
            category = ItemCategory.TOOLS,
            rarity = ItemRarity.RARE,
            value = 250,
            weight = 1f,
            isEquippable = true,
            buffEffects = listOf(EffectType.REVELATION),
            iconResource = "tool_biolume_talisman",
            loreText = "Carved from the heart of a Luminous Chasm crystal",
            loreTextArabic = "منحوت من قلب بلورة الهاوية المضيئة"
        ),
        
        "grapple_hook" to Item(
            id = "grapple_hook",
            name = "Grapple Hook",
            nameArabic = "خطاف التسلق",
            description = "Allows grappling to distant ledges",
            descriptionArabic = "يسمح بالتعلق بالحواف البعيدة",
            type = ItemType.TOOL_UTILITY,
            category = ItemCategory.TOOLS,
            rarity = ItemRarity.RARE,
            value = 400,
            weight = 3f,
            isEquippable = true,
            isDroppable = false,
            iconResource = "tool_grapple_hook"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // الآثار (Relics)
        // ═══════════════════════════════════════════════════════════════
        
        "mask_fragment_alpha" to Item(
            id = "mask_fragment_alpha",
            name = "Mask Fragment α",
            nameArabic = "شظية القناع α",
            description = "Increases max Energy by 20",
            descriptionArabic = "تزيد الطاقة القصوى بـ 20",
            type = ItemType.RELIC_PASSIVE,
            category = ItemCategory.RELICS,
            rarity = ItemRarity.EPIC,
            value = 1000,
            weight = 0f,
            isEquippable = true,
            isDroppable = false,
            isTradeable = false,
            iconResource = "relic_mask_fragment_alpha",
            loreText = "The first fragment of the shattered Mask of Light",
            loreTextArabic = "الشظية الأولى من قناع النور المحطم"
        ),
        
        "edda_locket" to Item(
            id = "edda_locket",
            name = "Edda's Locket",
            nameArabic = "قلادة إيدا",
            description = "+10% XP gain, FM generation reduced by 1",
            descriptionArabic = "+10% اكتساب XP، توليد FM يقل بـ 1",
            type = ItemType.RELIC_PASSIVE,
            category = ItemCategory.RELICS,
            rarity = ItemRarity.RARE,
            value = 800,
            weight = 0f,
            isEquippable = true,
            isDroppable = false,
            isTradeable = false,
            isQuestItem = true,
            iconResource = "relic_edda_locket",
            loreText = "A gift from Edda, warm with remembered kindness",
            loreTextArabic = "هدية من إيدا، دافئة بلطف محفوظ في الذاكرة"
        ),
        
        "root_talisman" to Item(
            id = "root_talisman",
            name = "Root Talisman",
            nameArabic = "تعويذة الجذور",
            description = "+15% healing from all sources",
            descriptionArabic = "+15% شفاء من جميع المصادر",
            type = ItemType.RELIC_PASSIVE,
            category = ItemCategory.RELICS,
            rarity = ItemRarity.UNCOMMON,
            value = 300,
            weight = 0f,
            isEquippable = true,
            iconResource = "relic_root_talisman"
        ),
        
        "echo_ring" to Item(
            id = "echo_ring",
            name = "Echo Ring",
            nameArabic = "خاتم الصدى",
            description = "Echo Recall cooldown -25%",
            descriptionArabic = "انتظار استدعاء الصدى -25%",
            type = ItemType.RELIC_PASSIVE,
            category = ItemCategory.RELICS,
            rarity = ItemRarity.RARE,
            value = 600,
            weight = 0f,
            isEquippable = true,
            iconResource = "relic_echo_ring"
        ),
        
        "void_stone" to Item(
            id = "void_stone",
            name = "Void Stone",
            nameArabic = "حجر الفراغ",
            description = "Grants brief invisibility after dodge",
            descriptionArabic = "يمنح اختفاء قصيراً بعد التفادي",
            type = ItemType.RELIC_ACTIVE,
            category = ItemCategory.RELICS,
            rarity = ItemRarity.EPIC,
            value = 1500,
            weight = 0f,
            isEquippable = true,
            buffEffects = listOf(EffectType.INVISIBLE),
            buffDuration = 2000L,
            iconResource = "relic_void_stone",
            loreText = "Contains a sliver of pure void",
            loreTextArabic = "يحتوي على شريحة من الفراغ النقي"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // المواد
        // ═══════════════════════════════════════════════════════════════
        
        "leather_scrap" to Item(
            id = "leather_scrap",
            name = "Leather Scrap",
            nameArabic = "قصاصة جلد",
            description = "Common crafting material",
            descriptionArabic = "مادة صناعة شائعة",
            type = ItemType.MATERIAL_COMMON,
            category = ItemCategory.MATERIALS,
            rarity = ItemRarity.COMMON,
            maxStack = 99,
            value = 2,
            weight = 0.1f,
            iconResource = "material_leather_scrap"
        ),
        
        "metal_scrap" to Item(
            id = "metal_scrap",
            name = "Metal Scrap",
            nameArabic = "قصاصة معدن",
            description = "Common crafting material",
            descriptionArabic = "مادة صناعة شائعة",
            type = ItemType.MATERIAL_COMMON,
            category = ItemCategory.MATERIALS,
            rarity = ItemRarity.COMMON,
            maxStack = 99,
            value = 3,
            weight = 0.2f,
            iconResource = "material_metal_scrap"
        ),
        
        "memory_dust" to Item(
            id = "memory_dust",
            name = "Memory Dust",
            nameArabic = "غبار الذاكرة",
            description = "Rare essence from forgotten moments",
            descriptionArabic = "جوهر نادر من لحظات منسية",
            type = ItemType.MATERIAL_RARE,
            category = ItemCategory.MATERIALS,
            rarity = ItemRarity.RARE,
            maxStack = 50,
            value = 25,
            weight = 0.1f,
            iconResource = "material_memory_dust"
        ),
        
        "void_essence" to Item(
            id = "void_essence",
            name = "Void Essence",
            nameArabic = "جوهر الفراغ",
            description = "Legendary crafting material",
            descriptionArabic = "مادة صناعة أسطورية",
            type = ItemType.MATERIAL_LEGENDARY,
            category = ItemCategory.MATERIALS,
            rarity = ItemRarity.LEGENDARY,
            maxStack = 10,
            value = 500,
            weight = 0.5f,
            iconResource = "material_void_essence",
            loreText = "Crystallized from the Luminous Chasm's deepest void",
            loreTextArabic = "متبلور من أعمق فراغ في الهاوية المضيئة"
        ),
        
        "clockwork_gear" to Item(
            id = "clockwork_gear",
            name = "Clockwork Gear",
            nameArabic = "ترس ساعة",
            description = "Precision gear from Sunken Clockworks",
            descriptionArabic = "ترس دقيق من الساعات الغارقة",
            type = ItemType.MATERIAL_RARE,
            category = ItemCategory.MATERIALS,
            rarity = ItemRarity.UNCOMMON,
            maxStack = 30,
            value = 15,
            weight = 0.3f,
            iconResource = "material_clockwork_gear"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // العناصر الرئيسية
        // ═══════════════════════════════════════════════════════════════
        
        "vault_key" to Item(
            id = "vault_key",
            name = "Vault Key",
            nameArabic = "مفتاح الخزنة",
            description = "Opens the Grand Stacks Vault",
            descriptionArabic = "يفتح خزنة المخطوطات الكبرى",
            type = ItemType.KEY_ITEM,
            category = ItemCategory.KEY_ITEMS,
            rarity = ItemRarity.UNIQUE,
            value = 0,
            weight = 0f,
            isDroppable = false,
            isTradeable = false,
            isQuestItem = true,
            iconResource = "key_vault_key"
        ),
        
        "ferry_pass" to Item(
            id = "ferry_pass",
            name = "Ferry Season Pass",
            nameArabic = "تذكرة العبّارة الموسمية",
            description = "Unlimited ferry travel",
            descriptionArabic = "سفر غير محدود بالعبّارة",
            type = ItemType.KEY_ITEM,
            category = ItemCategory.KEY_ITEMS,
            rarity = ItemRarity.RARE,
            value = 250,
            weight = 0f,
            isDroppable = false,
            iconResource = "key_ferry_pass"
        ),
        
        "edda_letter" to Item(
            id = "edda_letter",
            name = "Edda's Letter",
            nameArabic = "رسالة إيدا",
            description = "A letter from Edda",
            descriptionArabic = "رسالة من إيدا",
            type = ItemType.KEY_ITEM,
            category = ItemCategory.KEY_ITEMS,
            rarity = ItemRarity.COMMON,
            value = 0,
            weight = 0f,
            isDroppable = false,
            isTradeable = false,
            isQuestItem = true,
            iconResource = "key_edda_letter",
            loreText = "Ink still fresh, promises still warm",
            loreTextArabic = "الحبر لا يزال طازجاً، الوعود لا تزال دافئة"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // الترقيات
        // ═══════════════════════════════════════════════════════════════
        
        "satchel_upgrade_left" to Item(
            id = "satchel_upgrade_left",
            name = "Left Satchel Upgrade",
            nameArabic = "ترقية الحقيبة اليسرى",
            description = "Increases left satchel capacity by 5",
            descriptionArabic = "تزيد سعة الحقيبة اليسرى بـ 5",
            type = ItemType.UPGRADE_SATCHEL,
            category = ItemCategory.UPGRADES,
            rarity = ItemRarity.UNCOMMON,
            value = 200,
            weight = 0f,
            isConsumable = true,
            iconResource = "upgrade_satchel_left"
        ),
        
        "satchel_upgrade_right" to Item(
            id = "satchel_upgrade_right",
            name = "Right Satchel Upgrade",
            nameArabic = "ترقية الحقيبة اليمنى",
            description = "Increases right satchel capacity by 5",
            descriptionArabic = "تزيد سعة الحقيبة اليمنى بـ 5",
            type = ItemType.UPGRADE_SATCHEL,
            category = ItemCategory.UPGRADES,
            rarity = ItemRarity.UNCOMMON,
            value = 200,
            weight = 0f,
            isConsumable = true,
            iconResource = "upgrade_satchel_right"
        ),
        
        "weight_reduction_charm" to Item(
            id = "weight_reduction_charm",
            name = "Weight Reduction Charm",
            nameArabic = "سحر تخفيف الوزن",
            description = "Reduces all item weights by 20%",
            descriptionArabic = "يقلل أوزان جميع العناصر بـ 20%",
            type = ItemType.UPGRADE_SATCHEL,
            category = ItemCategory.UPGRADES,
            rarity = ItemRarity.RARE,
            value = 400,
            weight = 0f,
            isEquippable = true,
            iconResource = "upgrade_weight_charm"
        ),
        
        // ═══════════════════════════════════════════════════════════════
        // التجميل
        // ═══════════════════════════════════════════════════════════════
        
        "mask_variant_gold" to Item(
            id = "mask_variant_gold",
            name = "Golden Mask Variant",
            nameArabic = "نسخة القناع الذهبي",
            description = "Cosmetic mask variant",
            descriptionArabic = "نسخة قناع تجميلية",
            type = ItemType.COSMETIC,
            category = ItemCategory.COSMETICS,
            rarity = ItemRarity.LEGENDARY,
            value = 0,
            weight = 0f,
            isEquippable = true,
            isDroppable = false,
            isTradeable = false,
            iconResource = "cosmetic_mask_gold"
        ),
        
        "cloak_ashen" to Item(
            id = "cloak_ashen",
            name = "Ashen Cloak",
            nameArabic = "عباءة الرماد",
            description = "Cosmetic cloak from Ashen Sprawl",
            descriptionArabic = "عباءة تجميلية من انتشار الرماد",
            type = ItemType.COSMETIC,
            category = ItemCategory.COSMETICS,
            rarity = ItemRarity.RARE,
            value = 0,
            weight = 0f,
            isEquippable = true,
            iconResource = "cosmetic_cloak_ashen"
        )
    )
    
    /**
     * الحصول على عنصر بمعرّفه
     */
    fun getItem(id: String): Item? = allItems[id]
    
    /**
     * الحصول على جميع عناصر فئة معينة
     */
    fun getItemsByCategory(category: ItemCategory): List<Item> =
        allItems.values.filter { it.category == category }
    
    /**
     * الحصول على جميع عناصر نوع معين
     */
    fun getItemsByType(type: ItemType): List<Item> =
        allItems.values.filter { it.type == type }
    
    /**
     * الحصول على جميع عناصر ندرة معينة
     */
    fun getItemsByRarity(rarity: ItemRarity): List<Item> =
        allItems.values.filter { it.rarity == rarity }
    
    /**
     * البحث عن عناصر بالاسم
     */
    fun searchItems(query: String): List<Item> {
        val lowerQuery = query.lowercase()
        return allItems.values.filter { 
            it.name.lowercase().contains(lowerQuery) ||
            it.nameArabic.contains(query) ||
            it.description.lowercase().contains(lowerQuery)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Crafting System (Optional)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * وصفة صناعة
 */
data class CraftingRecipe(
    val resultItemId: String,
    val resultQuantity: Int = 1,
    val requiredItems: Map<String, Int>,  // itemId -> quantity
    val requiredLevel: Int = 0,
    val craftingTime: Long = 0L,  // بالميلي ثانية (0 = فوري)
    val unlockCondition: String = ""
)

/**
 * قاعدة بيانات الصناعة
 */
object CraftingDatabase {
    
    val allRecipes = listOf(
        
        // صناعة القنابل
        CraftingRecipe(
            resultItemId = "ash_bomb",
            resultQuantity = 3,
            requiredItems = mapOf(
                "metal_scrap" to 2,
                "leather_scrap" to 1
            )
        ),
        
        CraftingRecipe(
            resultItemId = "shard_grenade",
            resultQuantity = 2,
            requiredItems = mapOf(
                "metal_scrap" to 3,
                "memory_dust" to 1
            ),
            requiredLevel = 5
        ),
        
        // صناعة الجرعات
        CraftingRecipe(
            resultItemId = "health_vial",
            resultQuantity = 2,
            requiredItems = mapOf(
                "leather_scrap" to 1,
                "memory_dust" to 1
            )
        ),
        
        CraftingRecipe(
            resultItemId = "fm_reducer",
            resultQuantity = 1,
            requiredItems = mapOf(
                "memory_dust" to 5,
                "void_essence" to 1
            ),
            requiredLevel = 10,
            craftingTime = 5000L
        ),
        
        // ترقية الأسلحة
        CraftingRecipe(
            resultItemId = "void_dagger",
            resultQuantity = 1,
            requiredItems = mapOf(
                "satchel_blade" to 1,
                "void_essence" to 3,
                "metal_scrap" to 10
            ),
            requiredLevel = 10,
            craftingTime = 10000L,
            unlockCondition = "completed_chasm_quest"
        )
    )
    
    /**
     * الحصول على وصفات صناعة عنصر معين
     */
    fun getRecipesFor(itemId: String): List<CraftingRecipe> =
        allRecipes.filter { it.resultItemId == itemId }
    
    /**
     * الحصول على جميع الوصفات المتاحة للاعب
     */
    fun getAvailableRecipes(level: Int, completedQuests: Set<String>): List<CraftingRecipe> =
        allRecipes.filter { recipe ->
            recipe.requiredLevel <= level &&
            (recipe.unlockCondition.isEmpty() || completedQuests.contains(recipe.unlockCondition))
        }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Inventory Manager
// ═══════════════════════════════════════════════════════════════════════════

/**
 * مدير نظام المخزون
 */
class InventoryManager(
    private val playerStateManager: PlayerStateManager,
    private val eventBus: EventBus
) {
    
    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════
    
    private val _leftSatchel = MutableStateFlow(
        Satchel(type = SatchelType.LEFT, capacity = 10, maxWeight = 50f)
    )
    val leftSatchel: StateFlow<Satchel> = _leftSatchel.asStateFlow()
    
    private val _rightSatchel = MutableStateFlow(
        Satchel(type = SatchelType.RIGHT, capacity = 10, maxWeight = 50f)
    )
    val rightSatchel: StateFlow<Satchel> = _rightSatchel.asStateFlow()
    
    private val _backSatchel = MutableStateFlow<Satchel?>(null)
    val backSatchel: StateFlow<Satchel?> = _backSatchel.asStateFlow()
    
    private val _items = MutableStateFlow<List<InventoryItem>>(emptyList())
    val items: StateFlow<List<InventoryItem>> = _items.asStateFlow()
    
    private val _equippedWeapon = MutableStateFlow<InventoryItem?>(null)
    val equippedWeapon: StateFlow<InventoryItem?> = _equippedWeapon.asStateFlow()
    
    private val _equippedRelics = MutableStateFlow<List<InventoryItem>>(emptyList())
    val equippedRelics: StateFlow<List<InventoryItem>> = _equippedRelics.asStateFlow()
    
    private val _equippedTools = MutableStateFlow<List<InventoryItem>>(emptyList())
    val equippedTools: StateFlow<List<InventoryItem>> = _equippedTools.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════
    // Public API - Adding/Removing Items
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة عنصر للمخزون
     */
    fun addItem(itemId: String, quantity: Int = 1): InventoryResult {
        val item = ItemDatabase.getItem(itemId)
            ?: return InventoryResult.Failure(InventoryFailureReason.ITEM_NOT_FOUND)
        
        // تحقق من المتطلبات
        if (!meetsRequirements(item)) {
            return InventoryResult.Failure(InventoryFailureReason.REQUIREMENTS_NOT_MET)
        }
        
        // محاولة التكديس مع عناصر موجودة
        val existingItem = _items.value.find { 
            it.item.id == itemId && it.canStackWith(InventoryItem(item, quantity))
        }
        
        if (existingItem != null && existingItem.quantity + quantity <= item.maxStack) {
            // تكديس
            _items.value = _items.value.map {
                if (it == existingItem) {
                    it.copy(quantity = it.quantity + quantity)
                } else {
                    it
                }
            }
        } else {
            // إضافة جديدة
            if (!hasSpace(item, quantity)) {
                return InventoryResult.Failure(InventoryFailureReason.INVENTORY_FULL)
            }
            
            if (!canCarryWeight(item.weight * quantity)) {
                return InventoryResult.Failure(InventoryFailureReason.WEIGHT_EXCEEDED)
            }
            
            val newItem = InventoryItem(
                item = item,
                quantity = min(quantity, item.maxStack),
                slot = findEmptySlot()
            )
            
            _items.value = _items.value + newItem
        }
        
        eventBus.emit(GameEvent.ItemAdded(itemId, quantity))
        
        return InventoryResult.Success("Added $quantity x ${item.name}")
    }
    
    /**
     * إزالة عنصر من المخزون
     */
    fun removeItem(itemId: String, quantity: Int = 1): InventoryResult {
        val inventoryItem = _items.value.find { it.item.id == itemId }
            ?: return InventoryResult.Failure(InventoryFailureReason.ITEM_NOT_FOUND)
        
        if (inventoryItem.quantity < quantity) {
            return InventoryResult.Failure(InventoryFailureReason.INSUFFICIENT_QUANTITY)
        }
        
        if (!inventoryItem.item.isDroppable) {
            return InventoryResult.Failure(InventoryFailureReason.CANNOT_DROP)
        }
        
        if (inventoryItem.quantity == quantity) {
            // إزالة كاملة
            _items.value = _items.value - inventoryItem
        } else {
            // تقليل الكمية
            _items.value = _items.value.map {
                if (it == inventoryItem) {
                    it.copy(quantity = it.quantity - quantity)
                } else {
                    it
                }
            }
        }
        
        eventBus.emit(GameEvent.ItemRemoved(itemId, quantity))
        
        return InventoryResult.Success("Removed $quantity x ${inventoryItem.item.name}")
    }
    
    /**
     * استخدام عنصر استهلاكي
     */
    fun useItem(itemId: String): InventoryResult {
        val inventoryItem = _items.value.find { it.item.id == itemId }
            ?: return InventoryResult.Failure(InventoryFailureReason.ITEM_NOT_FOUND)
        
        if (!inventoryItem.canUse()) {
            return InventoryResult.Failure(InventoryFailureReason.INSUFFICIENT_QUANTITY)
        }
        
        val item = inventoryItem.item
        
        // تطبيق التأثيرات
        when (item.type) {
            ItemType.CONSUMABLE_HEALTH -> {
                val healAmount = if (item.healAmount == 9999) {
                    playerStateManager.playerState.value.stats.maxHp
                } else {
                    item.healAmount
                }
                playerStateManager.addHP(healAmount)
            }
            
            ItemType.CONSUMABLE_ENERGY -> {
                val energyAmount = if (item.energyAmount == 9999) {
                    playerStateManager.playerState.value.stats.maxEnergy
                } else {
                    item.energyAmount
                }
                playerStateManager.addEnergy(energyAmount)
            }
            
            ItemType.CONSUMABLE_BUFF -> {
                item.buffEffects.forEach { effectType ->
                    playerStateManager.addEffect(
                        PlayerEffect(
                            type = effectType,
                            duration = item.buffDuration,
                            value = 1f
                        )
                    )
                }
            }
            
            ItemType.CONSUMABLE_UTILITY -> {
                when (itemId) {
                    "fm_reducer" -> {
                        playerStateManager.addForgetfulness(-5)
                    }
                }
            }
            
            ItemType.TOOL_THROWABLE -> {
                // سيتم التعامل معه من PlayerController
                eventBus.emit(GameEvent.ThrowItem(itemId))
            }
            
            ItemType.UPGRADE_SATCHEL -> {
                applySatchelUpgrade(itemId)
            }
            
            else -> {
                return InventoryResult.Failure(InventoryFailureReason.ITEM_NOT_FOUND)
            }
        }
        
        // تقليل الكمية
        removeItem(itemId, 1)
        
        // تشغيل الصوت
        if (item.soundEffect.isNotEmpty()) {
            eventBus.emit(GameEvent.PlaySound(item.soundEffect))
        }
        
        eventBus.emit(GameEvent.ItemUsed(itemId))
        
        return InventoryResult.Success("Used ${item.name}")
    }
    
    /**
     * تجهيز عنصر
     */
    fun equipItem(itemId: String): InventoryResult {
        val inventoryItem = _items.value.find { it.item.id == itemId }
            ?: return InventoryResult.Failure(InventoryFailureReason.ITEM_NOT_FOUND)
        
        if (!inventoryItem.canEquip()) {
            return InventoryResult.Failure(InventoryFailureReason.CANNOT_EQUIP)
        }
        
        if (inventoryItem.equipped) {
            return InventoryResult.Failure(InventoryFailureReason.ALREADY_EQUIPPED)
        }
        
        val item = inventoryItem.item
        
        when (item.type) {
            ItemType.WEAPON_PRIMARY, ItemType.WEAPON_SECONDARY -> {
                // إلغاء تجهيز السلاح السابق
                _equippedWeapon.value?.let { unequipItem(it.item.id) }
                
                _equippedWeapon.value = inventoryItem.copy(equipped = true)
                updateItemEquipState(itemId, true)
            }
            
            ItemType.RELIC_PASSIVE, ItemType.RELIC_ACTIVE -> {
                // تحقق من عدد الآثار المجهزة (حد أقصى 3)
                if (_equippedRelics.value.size >= 3) {
                    return InventoryResult.Failure(InventoryFailureReason.INVENTORY_FULL)
                }
                
                _equippedRelics.value = _equippedRelics.value + inventoryItem.copy(equipped = true)
                updateItemEquipState(itemId, true)
                
                // تطبيق التأثيرات السلبية
                applyRelicPassiveEffects(item)
            }
            
            ItemType.TOOL_UTILITY -> {
                _equippedTools.value = _equippedTools.value + inventoryItem.copy(equipped = true)
                updateItemEquipState(itemId, true)
            }
            
            ItemType.COSMETIC -> {
                updateItemEquipState(itemId, true)
            }
            
            else -> {
                return InventoryResult.Failure(InventoryFailureReason.CANNOT_EQUIP)
            }
        }
        
        eventBus.emit(GameEvent.ItemEquipped(itemId))
        
        return InventoryResult.Success("Equipped ${item.name}")
    }
    
    /**
     * إلغاء تجهيز عنصر
     */
    fun unequipItem(itemId: String): InventoryResult {
        val inventoryItem = _items.value.find { it.item.id == itemId && it.equipped }
            ?: return InventoryResult.Failure(InventoryFailureReason.ITEM_NOT_FOUND)
        
        val item = inventoryItem.item
        
        when (item.type) {
            ItemType.WEAPON_PRIMARY, ItemType.WEAPON_SECONDARY -> {
                _equippedWeapon.value = null
                updateItemEquipState(itemId, false)
            }
            
            ItemType.RELIC_PASSIVE, ItemType.RELIC_ACTIVE -> {
                _equippedRelics.value = _equippedRelics.value.filter { it.item.id != itemId }
                updateItemEquipState(itemId, false)
                
                // إزالة التأثيرات السلبية
                removeRelicPassiveEffects(item)
            }
            
            ItemType.TOOL_UTILITY -> {
                _equippedTools.value = _equippedTools.value.filter { it.item.id != itemId }
                updateItemEquipState(itemId, false)
            }
            
            ItemType.COSMETIC -> {
                updateItemEquipState(itemId, false)
            }
            
            else -> {
                return InventoryResult.Failure(InventoryFailureReason.CANNOT_EQUIP)
            }
        }
        
        eventBus.emit(GameEvent.ItemUnequipped(itemId))
        
        return InventoryResult.Success("Unequipped ${item.name}")
    }
    
    /**
     * صناعة عنصر
     */
    fun craftItem(recipeIndex: Int, playerLevel: Int, completedQuests: Set<String>): InventoryResult {
        val availableRecipes = CraftingDatabase.getAvailableRecipes(playerLevel, completedQuests)
        
        if (recipeIndex !in availableRecipes.indices) {
            return InventoryResult.Failure(InventoryFailureReason.ITEM_NOT_FOUND)
        }
        
        val recipe = availableRecipes[recipeIndex]
        
        // تحقق من المواد
        for ((itemId, requiredQty) in recipe.requiredItems) {
            val available = getItemCount(itemId)
            if (available < requiredQty) {
                return InventoryResult.Failure(InventoryFailureReason.INSUFFICIENT_QUANTITY)
            }
        }
        
        // استهلاك المواد
        for ((itemId, requiredQty) in recipe.requiredItems) {
            removeItem(itemId, requiredQty)
        }
        
        // إضافة النتيجة
        addItem(recipe.resultItemId, recipe.resultQuantity)
        
        eventBus.emit(GameEvent.ItemCrafted(recipe.resultItemId, recipe.resultQuantity))
        
        return InventoryResult.Success("Crafted ${recipe.resultQuantity}x ${recipe.resultItemId}")
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Public API - Queries
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على عدد عنصر معين
     */
    fun getItemCount(itemId: String): Int =
        _items.value.filter { it.item.id == itemId }.sumOf { it.quantity }
    
    /**
     * هل يمتلك اللاعب عنصراً؟
     */
    fun hasItem(itemId: String, quantity: Int = 1): Boolean =
        getItemCount(itemId) >= quantity
    
    /**
     * الحصول على عنصر بالمعرف
     */
    fun getInventoryItem(itemId: String): InventoryItem? =
        _items.value.find { it.item.id == itemId }
    
    /**
     * الحصول على جميع عناصر فئة معينة
     */
    fun getItemsByCategory(category: ItemCategory): List<InventoryItem> =
        _items.value.filter { it.item.category == category }
    
    /**
     * الحصول على الوزن الحالي
     */
    fun getCurrentWeight(satchelType: SatchelType? = null): Float {
        val items = if (satchelType != null) {
            _items.value.filter { getItemSatchel(it) == satchelType }
        } else {
            _items.value
        }
        
        return items.sumOf { (it.item.weight * it.quantity).toDouble() }.toFloat()
    }
    
    /**
     * الحصول على السعة المتاحة
     */
    fun getAvailableCapacity(satchelType: SatchelType): Int {
        val satchel = when (satchelType) {
            SatchelType.LEFT -> _leftSatchel.value
            SatchelType.RIGHT -> _rightSatchel.value
            SatchelType.BACK -> _backSatchel.value ?: return 0
        }
        
        val usedSlots = _items.value.count { getItemSatchel(it) == satchelType }
        return satchel.capacity - usedSlots
    }
    
    /**
     * هل يوجد مساحة لعنصر؟
     */
    fun hasSpace(item: Item, quantity: Int = 1): Boolean {
        // تحقق من التكديس أولاً
        val stackableItem = _items.value.find { 
            it.item.id == item.id && it.quantity + quantity <= item.maxStack 
        }
        if (stackableItem != null) return true
        
        // تحقق من الخانات الفارغة
        val totalSlots = _leftSatchel.value.capacity + 
                        _rightSatchel.value.capacity + 
                        (_backSatchel.value?.capacity ?: 0)
        val usedSlots = _items.value.size
        
        return usedSlots < totalSlots
    }
    
    /**
     * هل يمكن حمل الوزن؟
     */
    fun canCarryWeight(additionalWeight: Float): Boolean {
        val currentWeight = getCurrentWeight()
        val maxWeight = _leftSatchel.value.maxWeight + 
                       _rightSatchel.value.maxWeight + 
                       (_backSatchel.value?.maxWeight ?: 0f)
        
        return currentWeight + additionalWeight <= maxWeight
    }
    
    /**
     * هل العنصر مجهز؟
     */
    fun isEquipped(itemId: String): Boolean =
        _items.value.any { it.item.id == itemId && it.equipped }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Public API - Satchel Management
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * ترقية حقيبة
     */
    fun upgradeSatchel(type: SatchelType, capacityIncrease: Int = 5, weightIncrease: Float = 10f) {
        when (type) {
            SatchelType.LEFT -> {
                _leftSatchel.value = _leftSatchel.value.copy(
                    capacity = _leftSatchel.value.capacity + capacityIncrease,
                    maxWeight = _leftSatchel.value.maxWeight + weightIncrease,
                    level = _leftSatchel.value.level + 1
                )
            }
            SatchelType.RIGHT -> {
                _rightSatchel.value = _rightSatchel.value.copy(
                    capacity = _rightSatchel.value.capacity + capacityIncrease,
                    maxWeight = _rightSatchel.value.maxWeight + weightIncrease,
                    level = _rightSatchel.value.level + 1
                )
            }
            SatchelType.BACK -> {
                if (_backSatchel.value == null) {
                    _backSatchel.value = Satchel(type = SatchelType.BACK, capacity = 15, maxWeight = 30f)
                } else {
                    _backSatchel.value = _backSatchel.value!!.copy(
                        capacity = _backSatchel.value!!.capacity + capacityIncrease,
                        maxWeight = _backSatchel.value!!.maxWeight + weightIncrease,
                        level = _backSatchel.value!!.level + 1
                    )
                }
            }
        }
        
        eventBus.emit(GameEvent.SatchelUpgraded(type))
    }
    
    /**
     * تعيين تخصص حقيبة
     */
    fun setSatchelSpecialization(type: SatchelType, specialization: SatchelSpecialization) {
        when (type) {
            SatchelType.LEFT -> {
                _leftSatchel.value = _leftSatchel.value.copy(specialization = specialization)
            }
            SatchelType.RIGHT -> {
                _rightSatchel.value = _rightSatchel.value.copy(specialization = specialization)
            }
            SatchelType.BACK -> {
                _backSatchel.value?.let {
                    _backSatchel.value = it.copy(specialization = specialization)
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Private Helper Functions
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * إيجاد خانة فارغة
     */
    private fun findEmptySlot(): Int {
        val usedSlots = _items.value.map { it.slot }.toSet()
        val totalSlots = _leftSatchel.value.capacity + 
                        _rightSatchel.value.capacity + 
                        (_backSatchel.value?.capacity ?: 0)
        
        for (i in 0 until totalSlots) {
            if (i !in usedSlots) return i
        }
        
        return -1
    }
    
    /**
     * تحديد أي حقيبة يتبع لها العنصر
     */
    private fun getItemSatchel(inventoryItem: InventoryItem): SatchelType {
        val slot = inventoryItem.slot
        
        return when {
            slot < _leftSatchel.value.capacity -> SatchelType.LEFT
            slot < _leftSatchel.value.capacity + _rightSatchel.value.capacity -> SatchelType.RIGHT
            else -> SatchelType.BACK
        }
    }
    
    /**
     * تحديث حالة تجهيز العنصر
     */
    private fun updateItemEquipState(itemId: String, equipped: Boolean) {
        _items.value = _items.value.map {
            if (it.item.id == itemId) {
                it.copy(equipped = equipped)
            } else {
                it
            }
        }
    }
    
    /**
     * تحقق من متطلبات العنصر
     */
    private fun meetsRequirements(item: Item): Boolean {
        val playerState = playerStateManager.playerState.value
        
        if (item.requiredLevel > 0 && playerState.stats.level < item.requiredLevel) {
            return false
        }
        
        if (item.requiredMF > 0 && playerState.stats.memoryFragments < item.requiredMF) {
            return false
        }
        
        // يمكن إضافة فحص للمهام المطلوبة هنا
        
        return true
    }
    
    /**
     * تطبيق ترقية حقيبة
     */
    private fun applySatchelUpgrade(upgradeId: String) {
        when (upgradeId) {
            "satchel_upgrade_left" -> upgradeSatchel(SatchelType.LEFT)
            "satchel_upgrade_right" -> upgradeSatchel(SatchelType.RIGHT)
        }
    }
    
    /**
     * تطبيق التأثيرات السلبية للآثار
     */
    private fun applyRelicPassiveEffects(item: Item) {
        when (item.id) {
            "mask_fragment_alpha" -> {
                // زيادة الطاقة القصوى
                val currentStats = playerStateManager.playerState.value.stats
                playerStateManager.updateStats(
                    currentStats.copy(maxEnergy = currentStats.maxEnergy + 20)
                )
            }
            
            "root_talisman" -> {
                // تأثير الشفاء سيُطبق عند الاستشفاء
                playerStateManager.addEffect(
                    PlayerEffect(
                        type = EffectType.HEALING_BOOST,
                        duration = Long.MAX_VALUE,  // دائم
                        value = 0.15f
                    )
                )
            }
            
            "edda_locket" -> {
                // تأثير XP سيُطبق عند الحصول على XP
                playerStateManager.addEffect(
                    PlayerEffect(
                        type = EffectType.XP_BOOST,
                        duration = Long.MAX_VALUE,
                        value = 0.10f
                    )
                )
            }
            
            "echo_ring" -> {
                // تأثير Cooldown سيُطبق في AbilityManager
                playerStateManager.addEffect(
                    PlayerEffect(
                        type = EffectType.COOLDOWN_REDUCTION,
                        duration = Long.MAX_VALUE,
                        value = 0.25f,
                        sourceAbility = "echo_recall"
                    )
                )
            }
        }
    }
    
    /**
     * إزالة التأثيرات السلبية للآثار
     */
    private fun removeRelicPassiveEffects(item: Item) {
        when (item.id) {
            "mask_fragment_alpha" -> {
                val currentStats = playerStateManager.playerState.value.stats
                playerStateManager.updateStats(
                    currentStats.copy(maxEnergy = currentStats.maxEnergy - 20)
                )
            }
            
            "root_talisman" -> {
                playerStateManager.removeEffectByType(EffectType.HEALING_BOOST)
            }
            
            "edda_locket" -> {
                playerStateManager.removeEffectByType(EffectType.XP_BOOST)
            }
            
            "echo_ring" -> {
                playerStateManager.removeEffectByType(EffectType.COOLDOWN_REDUCTION)
            }
        }
    }
    
    /**
     * إعادة تعيين المخزون (للـ New Game)
     */
    fun reset() {
        _leftSatchel.value = Satchel(type = SatchelType.LEFT, capacity = 10, maxWeight = 50f)
        _rightSatchel.value = Satchel(type = SatchelType.RIGHT, capacity = 10, maxWeight = 50f)
        _backSatchel.value = null
        _items.value = emptyList()
        _equippedWeapon.value = null
        _equippedRelics.value = emptyList()
        _equippedTools.value = emptyList()
    }
    
    /**
     * حفظ حالة المخزون
     */
    fun saveState(): Map<String, Any> = mapOf(
        "leftSatchel" to mapOf(
            "capacity" to _leftSatchel.value.capacity,
            "maxWeight" to _leftSatchel.value.maxWeight,
            "level" to _leftSatchel.value.level,
            "specialization" to (_leftSatchel.value.specialization?.name ?: "")
        ),
        "rightSatchel" to mapOf(
            "capacity" to _rightSatchel.value.capacity,
            "maxWeight" to _rightSatchel.value.maxWeight,
            "level" to _rightSatchel.value.level,
            "specialization" to (_rightSatchel.value.specialization?.name ?: "")
        ),
        "backSatchel" to (_backSatchel.value?.let {
            mapOf(
                "capacity" to it.capacity,
                "maxWeight" to it.maxWeight,
                "level" to it.level,
                "specialization" to (it.specialization?.name ?: "")
            )
        } ?: emptyMap<String, Any>()),
        "items" to _items.value.map { 
            mapOf(
                "itemId" to it.item.id,
                "quantity" to it.quantity,
                "equipped" to it.equipped,
                "slot" to it.slot,
                "durability" to it.durability
            )
        }
    )
    
    /**
     * تحميل حالة المخزون
     */
    fun loadState(data: Map<String, Any>) {
        // تحميل الحقائب
        (data["leftSatchel"] as? Map<String, Any>)?.let { satchelData ->
            _leftSatchel.value = Satchel(
                type = SatchelType.LEFT,
                capacity = (satchelData["capacity"] as? Int) ?: 10,
                maxWeight = ((satchelData["maxWeight"] as? Number)?.toFloat()) ?: 50f,
                level = (satchelData["level"] as? Int) ?: 1,
                specialization = (satchelData["specialization"] as? String)?.let {
                    if (it.isNotEmpty()) SatchelSpecialization.valueOf(it) else null
                }
            )
        }
        
        (data["rightSatchel"] as? Map<String, Any>)?.let { satchelData ->
            _rightSatchel.value = Satchel(
                type = SatchelType.RIGHT,
                capacity = (satchelData["capacity"] as? Int) ?: 10,
                maxWeight = ((satchelData["maxWeight"] as? Number)?.toFloat()) ?: 50f,
                level = (satchelData["level"] as? Int) ?: 1,
                specialization = (satchelData["specialization"] as? String)?.let {
                    if (it.isNotEmpty()) SatchelSpecialization.valueOf(it) else null
                }
            )
        }
        
        (data["backSatchel"] as? Map<String, Any>)?.let { satchelData ->
            if (satchelData.isNotEmpty()) {
                _backSatchel.value = Satchel(
                    type = SatchelType.BACK,
                    capacity = (satchelData["capacity"] as? Int) ?: 15,
                    maxWeight = ((satchelData["maxWeight"] as? Number)?.toFloat()) ?: 30f,
                    level = (satchelData["level"] as? Int) ?: 1,
                    specialization = (satchelData["specialization"] as? String)?.let {
                        if (it.isNotEmpty()) SatchelSpecialization.valueOf(it) else null
                    }
                )
            }
        }
        
        // تحميل العناصر
        (data["items"] as? List<Map<String, Any>>)?.let { itemsData ->
            _items.value = itemsData.mapNotNull { itemData ->
                val itemId = itemData["itemId"] as? String ?: return@mapNotNull null
                val item = ItemDatabase.getItem(itemId) ?: return@mapNotNull null
                
                InventoryItem(
                    item = item,
                    quantity = (itemData["quantity"] as? Int) ?: 1,
                    equipped = (itemData["equipped"] as? Boolean) ?: false,
                    slot = (itemData["slot"] as? Int) ?: -1,
                    durability = ((itemData["durability"] as? Number)?.toFloat()) ?: 100f
                )
            }
        }
        
        // إعادة بناء العناصر المجهزة
        _equippedWeapon.value = _items.value.find { 
            it.equipped && (it.item.type == ItemType.WEAPON_PRIMARY || it.item.type == ItemType.WEAPON_SECONDARY)
        }
        
        _equippedRelics.value = _items.value.filter {
            it.equipped && (it.item.type == ItemType.RELIC_PASSIVE || it.item.type == ItemType.RELIC_ACTIVE)
        }
        
        _equippedTools.value = _items.value.filter {
            it.equipped && it.item.type == ItemType.TOOL_UTILITY
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MARK: - Extensions
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Extension للـ PlayerStateManager لإضافة دوال مفقودة
 */
private fun PlayerStateManager.updateStats(stats: PlayerStats) {
    // Implementation في PlayerStateManager
}

private fun PlayerStateManager.removeEffectByType(type: EffectType) {
    // Implementation في PlayerStateManager
}

class InventorySystem(val repository: com.erygra.maskoflight.data.repository.InventoryRepository)