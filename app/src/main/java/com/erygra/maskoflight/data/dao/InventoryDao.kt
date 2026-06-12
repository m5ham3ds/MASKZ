package com.erygra.maskoflight.data.dao

import androidx.room.*
import com.erygra.maskoflight.data.entities.ItemEntity
import com.erygra.maskoflight.player.ItemType
import com.erygra.maskoflight.player.Rarity
import kotlinx.coroutines.flow.Flow

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * InventoryDao - واجهة الوصول لبيانات المخزون والعناصر
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Data Access Object لإدارة عمليات قاعدة البيانات المتعلقة بالعناصر والمخزون
 * 
 * Data Access Object for managing item and inventory database operations
 * 
 * العمليات المدعومة / Supported Operations:
 * - إدارة العناصر (Item management)
 * - تصفية وبحث (Filtering & search)
 * - الصياغة والترقية (Crafting & upgrading)
 * - إحصائيات العناصر (Item statistics)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Dao
interface InventoryDao {

    // ═══════════════════════════════════════════════════════════════════════
    // Insert Operations - عمليات الإدراج
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إدراج عنصر جديد
     * Insert new item
     * 
     * @param item بيانات العنصر / Item data
     * @return معرف السطر المُدرج / Inserted row ID
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: ItemEntity): Long

    /**
     * إدراج أو استبدال عنصر
     * Insert or replace item
     * 
     * @param item بيانات العنصر / Item data
     * @return معرف السطر / Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceItem(item: ItemEntity): Long

    /**
     * إدراج عدة عناصر
     * Insert multiple items
     * 
     * @param items قائمة العناصر / Items list
     * @return قائمة معرفات الصفوف / Row IDs list
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<ItemEntity>): List<Long>

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations - عمليات التحديث
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث عنصر
     * Update item
     * 
     * @param item بيانات العنصر المحدثة / Updated item data
     * @return عدد الصفوف المحدثة / Number of updated rows
     */
    @Update
    suspend fun updateItem(item: ItemEntity): Int

    /**
     * تحديث عدة عناصر
     * Update multiple items
     * 
     * @param items قائمة العناصر / Items list
     * @return عدد الصفوف المحدثة / Number of updated rows
     */
    @Update
    suspend fun updateItems(items: List<ItemEntity>): Int

    /**
     * ترقية عنصر
     * Upgrade item
     * 
     * @param itemId معرف العنصر / Item ID
     * @param newLevel المستوى الجديد / New upgrade level
     */
    @Query("""
        UPDATE items 
        SET upgradeLevel = :newLevel,
            updatedAt = :updateTime
        WHERE itemId = :itemId AND isUpgradable = 1
    """)
    suspend fun upgradeItem(
        itemId: String,
        newLevel: Int,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث إحصائيات عنصر
     * Update item stats
     * 
     * @param itemId معرف العنصر / Item ID
     * @param attackPower قوة الهجوم / Attack power
     * @param defense قوة الدفاع / Defense
     */
    @Query("""
        UPDATE items 
        SET attackPower = :attackPower,
            defense = :defense,
            updatedAt = :updateTime
        WHERE itemId = :itemId
    """)
    suspend fun updateItemStats(
        itemId: String,
        attackPower: Float,
        defense: Float,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Query Operations - عمليات الاستعلام
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على عنصر بمعرفه (مع المراقبة)
     * Get item by ID (with observation)
     * 
     * @param itemId معرف العنصر / Item ID
     * @return Flow من بيانات العنصر / Flow of item data
     */
    @Query("SELECT * FROM items WHERE itemId = :itemId")
    fun getItemFlow(itemId: String): Flow<ItemEntity?>

    /**
     * الحصول على عنصر بمعرفه (استعلام مباشر)
     * Get item by ID (direct query)
     * 
     * @param itemId معرف العنصر / Item ID
     * @return بيانات العنصر / Item data
     */
    @Query("SELECT * FROM items WHERE itemId = :itemId")
    suspend fun getItem(itemId: String): ItemEntity?

    /**
     * الحصول على جميع العناصر
     * Get all items
     * 
     * @return قائمة العناصر / Items list
     */
    @Query("SELECT * FROM items ORDER BY rarity DESC, level DESC")
    suspend fun getAllItems(): List<ItemEntity>

    /**
     * الحصول على جميع العناصر (مع المراقبة)
     * Get all items (with observation)
     * 
     * @return Flow من قائمة العناصر / Flow of items list
     */
    @Query("SELECT * FROM items ORDER BY rarity DESC, level DESC")
    fun getAllItemsFlow(): Flow<List<ItemEntity>>

    /**
     * الحصول على العناصر حسب النوع
     * Get items by type
     * 
     * @param type نوع العنصر / Item type
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = :type 
        ORDER BY rarity DESC, level DESC
    """)
    suspend fun getItemsByType(type: ItemType): List<ItemEntity>

    /**
     * الحصول على العناصر حسب النوع (مع المراقبة)
     * Get items by type (with observation)
     * 
     * @param type نوع العنصر / Item type
     * @return Flow من قائمة العناصر / Flow of items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = :type 
        ORDER BY rarity DESC, level DESC
    """)
    fun getItemsByTypeFlow(type: ItemType): Flow<List<ItemEntity>>

    /**
     * الحصول على العناصر حسب الندرة
     * Get items by rarity
     * 
     * @param rarity الندرة / ItemRarity
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE rarity = :rarity 
        ORDER BY level DESC
    """)
    suspend fun getItemsByRarity(rarity: Rarity): List<ItemEntity>

    /**
     * الحصول على العناصر حسب الفئة
     * Get items by category
     * 
     * @param category الفئة / Category
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE category = :category 
        ORDER BY rarity DESC, level DESC
    """)
    suspend fun getItemsByCategory(category: String): List<ItemEntity>

    /**
     * الحصول على العناصر القابلة للتجهيز
     * Get equippable items
     * 
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE isEquippable = 1 
        ORDER BY rarity DESC, level DESC
    """)
    suspend fun getEquippableItems(): List<ItemEntity>

    /**
     * الحصول على العناصر القابلة للاستهلاك
     * Get consumable items
     * 
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE isConsumable = 1 
        ORDER BY level DESC
    """)
    suspend fun getConsumableItems(): List<ItemEntity>

    /**
     * الحصول على الأسلحة
     * Get weapons
     * 
     * @return قائمة الأسلحة / Weapons list
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = 'WEAPON' 
        ORDER BY attackPower DESC, rarity DESC
    """)
    suspend fun getWeapons(): List<ItemEntity>

    /**
     * الحصول على الدروع
     * Get armor
     * 
     * @return قائمة الدروع / Armor list
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = 'ARMOR' 
        ORDER BY defense DESC, rarity DESC
    """)
    suspend fun getArmor(): List<ItemEntity>

    /**
     * الحصول على الإكسسوارات
     * Get accessories
     * 
     * @return قائمة الإكسسوارات / Accessories list
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = 'ACCESSORY' 
        ORDER BY rarity DESC
    """)
    suspend fun getAccessories(): List<ItemEntity>

    /**
     * الحصول على الأقنعة
     * Get masks
     * 
     * @return قائمة الأقنعة / Masks list
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = 'MASK' 
        ORDER BY rarity DESC, level DESC
    """)
    suspend fun getMasks(): List<ItemEntity>

    /**
     * الحصول على المواد الخام
     * Get materials
     * 
     * @return قائمة المواد / Materials list
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = 'MATERIAL' 
        ORDER BY rarity DESC
    """)
    suspend fun getMaterials(): List<ItemEntity>

    /**
     * الحصول على عناصر المهام
     * Get quest items
     * 
     * @return قائمة عناصر المهام / Quest items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE isQuestItem = 1 
        ORDER BY updatedAt DESC
    """)
    suspend fun getQuestItems(): List<ItemEntity>

    /**
     * الحصول على العناصر القابلة للترقية
     * Get upgradable items
     * 
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE isUpgradable = 1 
        ORDER BY upgradeLevel ASC, rarity DESC
    """)
    suspend fun getUpgradableItems(): List<ItemEntity>

    /**
     * الحصول على العناصر القابلة للصياغة
     * Get craftable items
     * 
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE isCraftable = 1 
        ORDER BY rarity DESC, level DESC
    """)
    suspend fun getCraftableItems(): List<ItemEntity>

    /**
     * الحصول على العناصر حسب نطاق المستوى
     * Get items by level range
     * 
     * @param minLevel المستوى الأدنى / Minimum level
     * @param maxLevel المستوى الأقصى / Maximum level
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE level BETWEEN :minLevel AND :maxLevel
        ORDER BY rarity DESC, level DESC
    """)
    suspend fun getItemsByLevelRange(minLevel: Int, maxLevel: Int): List<ItemEntity>

    /**
     * الحصول على عناصر مجموعة
     * Get set items
     * 
     * @param setName اسم المجموعة / Set name
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE setName = :setName 
        ORDER BY equipmentSlot ASC
    """)
    suspend fun getSetItems(setName: String): List<ItemEntity>

    /**
     * البحث عن عناصر بالاسم
     * Search items by name
     * 
     * @param searchQuery نص البحث / Search query
     * @return قائمة العناصر المطابقة / Matching items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE nameAr LIKE '%' || :searchQuery || '%' 
           OR nameEn LIKE '%' || :searchQuery || '%'
        ORDER BY rarity DESC, level DESC
    """)
    suspend fun searchItemsByName(searchQuery: String): List<ItemEntity>

    /**
     * البحث عن عناصر بالعلامات
     * Search items by tags
     * 
     * @param tag العلامة / Tag
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE tags LIKE '%' || :tag || '%'
        ORDER BY rarity DESC, level DESC
    """)
    suspend fun searchItemsByTag(tag: String): List<ItemEntity>

    /**
     * فحص وجود عنصر
     * Check if item exists
     * 
     * @param itemId معرف العنصر / Item ID
     * @return true إذا كان موجود / true if exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM items WHERE itemId = :itemId)")
    suspend fun itemExists(itemId: String): Boolean

    /**
     * الحصول على عدد العناصر
     * Get item count
     * 
     * @return عدد العناصر / Item count
     */
    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemCount(): Int

    /**
     * الحصول على عدد العناصر حسب النوع
     * Get item count by type
     * 
     * @param type نوع العنصر / Item type
     * @return عدد العناصر / Item count
     */
    @Query("SELECT COUNT(*) FROM items WHERE type = :type")
    suspend fun getItemCountByType(type: ItemType): Int

    /**
     * الحصول على عدد العناصر حسب الندرة
     * Get item count by rarity
     * 
     * @param rarity الندرة / ItemRarity
     * @return عدد العناصر / Item count
     */
    @Query("SELECT COUNT(*) FROM items WHERE rarity = :rarity")
    suspend fun getItemCountByRarity(rarity: Rarity): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Delete Operations - عمليات الحذف
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حذف عنصر
     * Delete item
     * 
     * @param item بيانات العنصر / Item data
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Delete
    suspend fun deleteItem(item: ItemEntity): Int

    /**
     * حذف عنصر بمعرفه
     * Delete item by ID
     * 
     * @param itemId معرف العنصر / Item ID
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM items WHERE itemId = :itemId")
    suspend fun deleteItemById(itemId: String): Int

    /**
     * حذف جميع العناصر
     * Delete all items
     * 
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM items")
    suspend fun deleteAllItems(): Int

    /**
     * حذف عناصر حسب النوع
     * Delete items by type
     * 
     * @param type نوع العنصر / Item type
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM items WHERE type = :type")
    suspend fun deleteItemsByType(type: ItemType): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Sorting & Filtering - الترتيب والتصفية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على العناصر مرتبة حسب القوة
     * Get items sorted by power
     * 
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE isEquippable = 1 
        ORDER BY (attackPower + defense) DESC
    """)
    suspend fun getItemsSortedByPower(): List<ItemEntity>

    /**
     * الحصول على العناصر مرتبة حسب القيمة
     * Get items sorted by value
     * 
     * @return قائمة العناصر / Items list
     */
    @Query("""
        SELECT * FROM items 
        WHERE isSellable = 1 
        ORDER BY sellPrice DESC
    """)
    suspend fun getItemsSortedByValue(): List<ItemEntity>

    /**
     * الحصول على أقوى سلاح
     * Get strongest weapon
     * 
     * @return بيانات السلاح / Weapon data
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = 'WEAPON' 
        ORDER BY attackPower DESC 
        LIMIT 1
    """)
    suspend fun getStrongestWeapon(): ItemEntity?

    /**
     * الحصول على أقوى درع
     * Get strongest armor
     * 
     * @return بيانات الدرع / Armor data
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = 'ARMOR' 
        ORDER BY defense DESC 
        LIMIT 1
    """)
    suspend fun getStrongestArmor(): ItemEntity?

    // ═══════════════════════════════════════════════════════════════════════
    // Analytics Queries - استعلامات التحليلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على متوسط مستوى العناصر
     * Get average item level
     * 
     * @return متوسط المستوى / Average level
     */
    @Query("SELECT AVG(level) FROM items")
    suspend fun getAverageItemLevel(): Float

    /**
     * الحصول على إجمالي قيمة العناصر
     * Get total items value
     * 
     * @return إجمالي القيمة / Total value
     */
    @Query("SELECT SUM(sellPrice) FROM items WHERE isSellable = 1")
    suspend fun getTotalItemsValue(): Long

    /**
     * الحصول على توزيع العناصر حسب النوع
     * Get item distribution by type
     * 
     * @return خريطة النوع -> العدد / Type -> Count map
     */
    @Query("""
        SELECT type, COUNT(*) as count 
        FROM items 
        GROUP BY type 
        ORDER BY count DESC
    """)
    @MapInfo(keyColumn = "type", valueColumn = "count")
    suspend fun getItemDistributionByType(): Map<String, Int>

    /**
     * الحصول على توزيع العناصر حسب الندرة
     * Get item distribution by rarity
     * 
     * @return خريطة الندرة -> العدد / Rarity -> Count map
     */
    @Query("""
        SELECT rarity, COUNT(*) as count 
        FROM items 
        GROUP BY rarity 
        ORDER BY rarity DESC
    """)
    @MapInfo(keyColumn = "rarity", valueColumn = "count")
    suspend fun getItemDistributionByRarity(): Map<String, Int>

    /**
     * الحصول على أغلى عنصر
     * Get most expensive item
     * 
     * @return بيانات العنصر / Item data
     */
    @Query("""
        SELECT * FROM items 
        ORDER BY sellPrice DESC 
        LIMIT 1
    """)
    suspend fun getMostExpensiveItem(): ItemEntity?
}