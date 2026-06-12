package com.erygra.maskoflight.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.erygra.maskoflight.player.ItemType
import com.erygra.maskoflight.player.Rarity

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ItemEntity - كيان بيانات العنصر
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * كيان Room لحفظ بيانات العناصر والمعدات في قاعدة البيانات
 * 
 * Room entity for persisting item and equipment data in database
 * 
 * أنواع العناصر / Item Types:
 * - WEAPON: سلاح (Weapon)
 * - ARMOR: درع (Armor)
 * - ACCESSORY: إكسسوار (Accessory)
 * - CONSUMABLE: قابل للاستهلاك (Consumable)
 * - MATERIAL: مادة خام (Material)
 * - KEY_ITEM: عنصر مفتاحي (Key item)
 * - MASK: قناع (Mask)
 * - RUNE: رون سحري (Rune)
 * - QUEST_ITEM: عنصر مهمة (Quest item)
 * 
 * الندرة / Rarity:
 * - COMMON: عادي (Common)
 * - UNCOMMON: غير عادي (Uncommon)
 * - RARE: نادر (Rare)
 * - EPIC: ملحمي (Epic)
 * - LEGENDARY: أسطوري (Legendary)
 * - MYTHIC: أسطوري خارق (Mythic)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Entity(
    tableName = "items",
    indices = [
        Index(value = ["itemId"], unique = true),
        Index(value = ["type"]),
        Index(value = ["rarity"]),
        Index(value = ["level"]),
        Index(value = ["category"])
    ]
)
data class ItemEntity(
    // ═══════════════════════════════════════════════════════════════════════
    // Primary Key & Basic Info
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * معرف العنصر الفريد
     * Unique item identifier
     */
    @PrimaryKey
    @ColumnInfo(name = "itemId")
    val itemId: String,

    /**
     * الاسم بالعربية
     * Name in Arabic
     */
    @ColumnInfo(name = "nameAr")
    val nameAr: String,

    /**
     * الاسم بالإنجليزية
     * Name in English
     */
    @ColumnInfo(name = "nameEn")
    val nameEn: String,

    /**
     * الوصف بالعربية
     * Description in Arabic
     */
    @ColumnInfo(name = "descriptionAr")
    val descriptionAr: String,

    /**
     * الوصف بالإنجليزية
     * Description in English
     */
    @ColumnInfo(name = "descriptionEn")
    val descriptionEn: String,

    /**
     * نوع العنصر
     * Item type
     */
    @ColumnInfo(name = "type")
    val type: ItemType,

    /**
     * الندرة
     * Rarity
     */
    @ColumnInfo(name = "rarity")
    val rarity: Rarity,

    /**
     * التصنيف (للتنظيم)
     * Category (for organization)
     */
    @ColumnInfo(name = "category")
    val category: String,

    // ═══════════════════════════════════════════════════════════════════════
    // Level & Requirements - المستوى والمتطلبات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * مستوى العنصر
     * Item level
     */
    @ColumnInfo(name = "level")
    val level: Int = 1,

    /**
     * المستوى المطلوب للاستخدام
     * Required level to use
     */
    @ColumnInfo(name = "requiredLevel")
    val requiredLevel: Int = 1,

    /**
     * المتطلبات الأخرى
     * Other requirements
     */
    @ColumnInfo(name = "requirements")
    val requirements: Map<String, Any> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Stats & Effects - الإحصائيات والتأثيرات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * قوة الهجوم (للأسلحة)
     * Attack power (for weapons)
     */
    @ColumnInfo(name = "attackPower")
    val attackPower: Float = 0f,

    /**
     * قوة الدفاع (للدروع)
     * Defense power (for armor)
     */
    @ColumnInfo(name = "defense")
    val defense: Float = 0f,

    /**
     * الصحة الإضافية
     * Additional health
     */
    @ColumnInfo(name = "bonusHealth")
    val bonusHealth: Float = 0f,

    /**
     * الطاقة الإضافية
     * Additional energy
     */
    @ColumnInfo(name = "bonusEnergy")
    val bonusEnergy: Float = 0f,

    /**
     * فرصة الضربة الحرجة
     * Critical chance bonus
     */
    @ColumnInfo(name = "criticalChance")
    val criticalChance: Float = 0f,

    /**
     * ضرر الضربة الحرجة
     * Critical damage bonus
     */
    @ColumnInfo(name = "criticalDamage")
    val criticalDamage: Float = 0f,

    /**
     * سرعة الهجوم
     * Attack speed bonus
     */
    @ColumnInfo(name = "attackSpeed")
    val attackSpeed: Float = 0f,

    /**
     * سرعة الحركة
     * Movement speed bonus
     */
    @ColumnInfo(name = "moveSpeed")
    val moveSpeed: Float = 0f,

    /**
     * إحصائيات إضافية
     * Additional stats
     */
    @ColumnInfo(name = "additionalStats")
    val additionalStats: Map<String, Float> = emptyMap(),

    /**
     * التأثيرات الخاصة
     * Special effects
     */
    @ColumnInfo(name = "effects")
    val effects: List<Map<String, Any>> = emptyList(),

    // ═══════════════════════════════════════════════════════════════════════
    // Consumable Properties - خصائص المواد القابلة للاستهلاك
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * هل العنصر قابل للاستهلاك؟
     * Is item consumable?
     */
    @ColumnInfo(name = "isConsumable")
    val isConsumable: Boolean = false,

    /**
     * قيمة الاستعادة (صحة/طاقة)
     * Restoration value (health/energy)
     */
    @ColumnInfo(name = "restoreValue")
    val restoreValue: Float = 0f,

    /**
     * نوع الاستعادة (health, energy, both)
     * Restoration type
     */
    @ColumnInfo(name = "restoreType")
    val restoreType: String? = null,

    /**
     * مدة التأثير (بالثواني)
     * Effect duration (seconds)
     */
    @ColumnInfo(name = "effectDuration")
    val effectDuration: Float = 0f,

    /**
     * مدة الانتظار بعد الاستخدام
     * Cooldown after use
     */
    @ColumnInfo(name = "useCooldown")
    val useCooldown: Float = 0f,

    // ═══════════════════════════════════════════════════════════════════════
    // Equipment Properties - خصائص المعدات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * مكان التجهيز (للمعدات)
     * Equipment slot
     * 
     * مثل: weapon, helmet, chest, legs, boots, accessory1, accessory2
     */
    @ColumnInfo(name = "equipmentSlot")
    val equipmentSlot: String? = null,

    /**
     * هل يمكن تجهيزه؟
     * Is item equippable?
     */
    @ColumnInfo(name = "isEquippable")
    val isEquippable: Boolean = false,

    /**
     * المجموعة (Set) - إن وجدت
     * Set name (if part of a set)
     */
    @ColumnInfo(name = "setName")
    val setName: String? = null,

    /**
     * مكافآت المجموعة
     * Set bonuses
     */
    @ColumnInfo(name = "setBonuses")
    val setBonuses: Map<String, Any> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Stack & Quantity - التكديس والكمية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * هل يمكن تكديسه؟
     * Is item stackable?
     */
    @ColumnInfo(name = "isStackable")
    val isStackable: Boolean = false,

    /**
     * الحد الأقصى للتكديس
     * Maximum stack size
     */
    @ColumnInfo(name = "maxStack")
    val maxStack: Int = 1,

    // ═══════════════════════════════════════════════════════════════════════
    // Crafting & Upgrade - الصياغة والترقية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * هل يمكن صياغته؟
     * Is item craftable?
     */
    @ColumnInfo(name = "isCraftable")
    val isCraftable: Boolean = false,

    /**
     * وصفة الصياغة
     * Crafting recipe
     */
    @ColumnInfo(name = "craftingRecipe")
    val craftingRecipe: Map<String, Int> = emptyMap(),

    /**
     * تكلفة الصياغة (ذهب)
     * Crafting cost (gold)
     */
    @ColumnInfo(name = "craftingCost")
    val craftingCost: Int = 0,

    /**
     * هل يمكن ترقيته؟
     * Is item upgradable?
     */
    @ColumnInfo(name = "isUpgradable")
    val isUpgradable: Boolean = false,

    /**
     * مستوى الترقية الحالي
     * Current upgrade level
     */
    @ColumnInfo(name = "upgradeLevel")
    val upgradeLevel: Int = 0,

    /**
     * الحد الأقصى للترقية
     * Maximum upgrade level
     */
    @ColumnInfo(name = "maxUpgradeLevel")
    val maxUpgradeLevel: Int = 0,

    /**
     * متطلبات الترقية
     * Upgrade requirements
     */
    @ColumnInfo(name = "upgradeRequirements")
    val upgradeRequirements: Map<String, Any> = emptyMap(),

    // ═══════════════════════════════════════════════════════════════════════
    // Trading & Economy - التجارة والاقتصاد
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * سعر الشراء (ذهب)
     * Buy price (gold)
     */
    @ColumnInfo(name = "buyPrice")
    val buyPrice: Int = 0,

    /**
     * سعر البيع (ذهب)
     * Sell price (gold)
     */
    @ColumnInfo(name = "sellPrice")
    val sellPrice: Int = 0,

    /**
     * هل يمكن بيعه؟
     * Is item sellable?
     */
    @ColumnInfo(name = "isSellable")
    val isSellable: Boolean = true,

    /**
     * هل يمكن تداوله؟
     * Is item tradeable?
     */
    @ColumnInfo(name = "isTradeable")
    val isTradeable: Boolean = true,

    /**
     * هل يمكن إسقاطه؟
     * Is item droppable?
     */
    @ColumnInfo(name = "isDroppable")
    val isDroppable: Boolean = true,

    // ═══════════════════════════════════════════════════════════════════════
    // Quest & Special Items - عناصر المهام والخاصة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * هل هو عنصر مهمة؟
     * Is quest item?
     */
    @ColumnInfo(name = "isQuestItem")
    val isQuestItem: Boolean = false,

    /**
     * المهمة المرتبطة
     * Related quest
     */
    @ColumnInfo(name = "relatedQuest")
    val relatedQuest: String? = null,

    /**
     * هل هو عنصر فريد؟
     * Is unique item?
     */
    @ColumnInfo(name = "isUnique")
    val isUnique: Boolean = false,

    /**
     * هل يمكن الحصول عليه مرة واحدة فقط؟
     * Can only be obtained once?
     */
    @ColumnInfo(name = "isOneTimeOnly")
    val isOneTimeOnly: Boolean = false,

    // ═══════════════════════════════════════════════════════════════════════
    // Visual & Audio - المرئيات والصوت
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * مسار الأيقونة
     * Icon path
     */
    @ColumnInfo(name = "icon")
    val icon: String,

    /**
     * مسار النموذج ثلاثي الأبعاد (إن وجد)
     * 3D model path (if exists)
     */
    @ColumnInfo(name = "model3D")
    val model3D: String? = null,

    /**
     * اللون المميز (Hex)
     * Accent color (hex)
     */
    @ColumnInfo(name = "accentColor")
    val accentColor: String? = null,

    /**
     * تأثيرات بصرية
     * Visual effects
     */
    @ColumnInfo(name = "visualEffects")
    val visualEffects: List<String> = emptyList(),

    /**
     * صوت الاستخدام
     * Use sound
     */
    @ColumnInfo(name = "useSound")
    val useSound: String? = null,

    /**
     * صوت التجهيز
     * Equip sound
     */
    @ColumnInfo(name = "equipSound")
    val equipSound: String? = null,

    // ═══════════════════════════════════════════════════════════════════════
    // Additional Data - بيانات إضافية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * العلامات (للبحث والتصنيف)
     * Tags (for search and categorization)
     */
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

    /**
     * نص النصيحة (Lore)
     * Lore text
     */
    @ColumnInfo(name = "loreTextAr")
    val loreTextAr: String? = null,

    /**
     * نص النصيحة بالإنجليزية
     * Lore text in English
     */
    @ColumnInfo(name = "loreTextEn")
    val loreTextEn: String? = null,

    /**
     * بيانات مخصصة
     * Custom data
     */
    @ColumnInfo(name = "customData")
    val customData: Map<String, String> = emptyMap(),

    /**
     * وقت الإنشاء
     * Creation time
     */
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * آخر تحديث
     * Last update time
     */
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * الحصول على الاسم حسب اللغة
     * Get name by language
     */
    fun getName(language: String = "ar"): String {
        return if (language == "ar") nameAr else nameEn
    }

    /**
     * الحصول على الوصف حسب اللغة
     * Get description by language
     */
    fun getDescription(language: String = "ar"): String {
        return if (language == "ar") descriptionAr else descriptionEn
    }

    /**
     * الحصول على نص القصة حسب اللغة
     * Get lore text by language
     */
    fun getLoreText(language: String = "ar"): String? {
        return if (language == "ar") loreTextAr else loreTextEn
    }

    /**
     * حساب القوة الإجمالية للعنصر
     * Calculate total item power
     */
    fun calculatePowerLevel(): Int {
        return (level * 10) +
                (attackPower * 2).toInt() +
                (defense * 2).toInt() +
                (bonusHealth * 0.5f).toInt() +
                (bonusEnergy * 0.5f).toInt() +
                (rarity.ordinal * 50)
    }

    /**
     * التحقق من إمكانية الاستخدام
     * Check if item can be used
     */
    fun canUse(playerLevel: Int): Boolean {
        return playerLevel >= requiredLevel
    }

    /**
     * التحقق من إمكانية التكديس
     * Check if can stack
     */
    fun canStack(): Boolean {
        return isStackable && maxStack > 1
    }

    /**
     * الحصول على سعر البيع الفعلي
     * Get actual sell price
     */
    fun getActualSellPrice(quantity: Int = 1): Int {
        return if (isSellable) sellPrice * quantity else 0
    }

    /**
     * الحصول على سعر الشراء الفعلي
     * Get actual buy price
     */
    fun getActualBuyPrice(quantity: Int = 1): Int {
        return buyPrice * quantity
    }

    /**
     * التحقق من انتماء العنصر لمجموعة
     * Check if item is part of a set
     */
    fun isPartOfSet(): Boolean {
        return setName != null && setBonuses.isNotEmpty()
    }

    /**
     * التحقق من إمكانية الترقية
     * Check if can upgrade
     */
    fun canUpgrade(): Boolean {
        return isUpgradable && upgradeLevel < maxUpgradeLevel
    }

    /**
     * الحصول على نسبة الترقية
     * Get upgrade percentage
     */
    fun getUpgradePercentage(): Float {
        return if (maxUpgradeLevel > 0) {
            (upgradeLevel.toFloat() / maxUpgradeLevel) * 100f
        } else {
            0f
        }
    }

    /**
     * تحويل إلى سلسلة نصية للتصحيح
     * Convert to debug string
     */
    override fun toString(): String {
        return "ItemEntity(id='$itemId', name='$nameAr', type=$type, rarity=$rarity, " +
                "level=$level, power=${calculatePowerLevel()})"
    }
}