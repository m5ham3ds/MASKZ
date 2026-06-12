package com.erygra.maskoflight.data.repository

import com.erygra.maskoflight.data.dao.InventoryDao
import com.erygra.maskoflight.data.entities.ItemEntity
import com.erygra.maskoflight.player.ItemType
import com.erygra.maskoflight.player.Rarity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * InventoryRepository - مستودع بيانات المخزون والعناصر
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * طبقة المستودع لإدارة بيانات العناصر والمخزون
 * 
 * Repository layer for managing item and inventory data
 * 
 * المسؤوليات / Responsibilities:
 * - إدارة العناصر (Item management)
 * - عمليات المخزون (Inventory operations)
 * - الصياغة والترقية (Crafting & upgrading)
 * - البحث والتصفية (Search & filtering)
 * - التخزين المؤقت (Caching)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao
) {

    // ═══════════════════════════════════════════════════════════════════════
    // Cache - التخزين المؤقت
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * كاش جميع العناصر
     * All items cache
     */
    private var cachedItems: List<ItemEntity>? = null

    /**
     * كاش العناصر حسب النوع
     * Items by type cache
     */
    private val cachedItemsByType = mutableMapOf<ItemType, List<ItemEntity>>()

    /**
     * وقت آخر تحديث للكاش
     * Last cache update time
     */
    private var lastCacheUpdate: Long = 0L

    /**
     * مدة صلاحية الكاش (10 دقائق - العناصر لا تتغير كثيراً)
     * Cache validity duration (10 minutes - items don't change often)
     */
    private val cacheValidityDuration = 10 * 60 * 1000L

    // ═══════════════════════════════════════════════════════════════════════
    // Create Operations - عمليات الإنشاء
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إضافة عنصر جديد
     * Add new item
     * 
     * @param item بيانات العنصر / Item data
     * @return Result<Long> معرف الصف المُدرج / Inserted row ID
     */
    suspend fun addItem(item: ItemEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val rowId = inventoryDao.insertItem(item)
            invalidateCache()
            Result.success(rowId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * إضافة عدة عناصر
     * Add multiple items
     * 
     * @param items قائمة العناصر / Items list
     * @return Result<List<Long>> قائمة معرفات الصفوف / Row IDs list
     */
    suspend fun addItems(items: List<ItemEntity>): Result<List<Long>> = 
        withContext(Dispatchers.IO) {
            try {
                val rowIds = inventoryDao.insertItems(items)
                invalidateCache()
                Result.success(rowIds)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * إضافة أو استبدال عنصر
     * Add or replace item
     * 
     * @param item بيانات العنصر / Item data
     * @return Result<Long> معرف الصف / Row ID
     */
    suspend fun addOrReplaceItem(item: ItemEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val rowId = inventoryDao.insertOrReplaceItem(item)
            invalidateCache()
            Result.success(rowId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Read Operations - عمليات القراءة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على عنصر بمعرفه (مع المراقبة)
     * Get item by ID (with observation)
     * 
     * @param itemId معرف العنصر / Item ID
     * @return Flow<ItemEntity?> تدفق بيانات العنصر / Flow of item data
     */
    fun getItemFlow(itemId: String): Flow<ItemEntity?> {
        return inventoryDao.getItemFlow(itemId)
            .catch { e ->
                e.printStackTrace()
                emit(null)
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * الحصول على عنصر بمعرفه
     * Get item by ID
     * 
     * @param itemId معرف العنصر / Item ID
     * @return Result<ItemEntity?> بيانات العنصر / Item data
     */
    suspend fun getItem(itemId: String): Result<ItemEntity?> = withContext(Dispatchers.IO) {
        try {
            // البحث في الكاش أولاً
            // Search in cache first
            val cachedItem = cachedItems?.find { it.itemId == itemId }
            if (cachedItem != null && isCacheValid()) {
                return@withContext Result.success(cachedItem)
            }

            // جلب من قاعدة البيانات
            // Fetch from database
            val item = inventoryDao.getItem(itemId)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على جميع العناصر (مع التخزين المؤقت)
     * Get all items (with caching)
     * 
     * @param forceRefresh إجبار التحديث / Force refresh
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getAllItems(forceRefresh: Boolean = false): Result<List<ItemEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                // التحقق من الكاش
                // Check cache
                if (!forceRefresh && isCacheValid() && cachedItems != null) {
                    return@withContext Result.success(cachedItems!!)
                }

                // جلب من قاعدة البيانات
                // Fetch from database
                val items = inventoryDao.getAllItems()

                // تحديث الكاش
                // Update cache
                cachedItems = items
                lastCacheUpdate = System.currentTimeMillis()

                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على جميع العناصر (مع المراقبة)
     * Get all items (with observation)
     * 
     * @return Flow<List<ItemEntity>> تدفق قائمة العناصر / Flow of items list
     */
    fun getAllItemsFlow(): Flow<List<ItemEntity>> {
        return inventoryDao.getAllItemsFlow()
            .catch { e ->
                e.printStackTrace()
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * الحصول على العناصر حسب النوع (مع التخزين المؤقت)
     * Get items by type (with caching)
     * 
     * @param type نوع العنصر / Item type
     * @param forceRefresh إجبار التحديث / Force refresh
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getItemsByType(
        type: ItemType,
        forceRefresh: Boolean = false
    ): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            // التحقق من الكاش
            // Check cache
            if (!forceRefresh && isCacheValid() && cachedItemsByType.containsKey(type)) {
                return@withContext Result.success(cachedItemsByType[type]!!)
            }

            // جلب من قاعدة البيانات
            // Fetch from database
            val items = inventoryDao.getItemsByType(type)

            // تحديث الكاش
            // Update cache
            cachedItemsByType[type] = items
            lastCacheUpdate = System.currentTimeMillis()

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على العناصر حسب النوع (مع المراقبة)
     * Get items by type (with observation)
     * 
     * @param type نوع العنصر / Item type
     * @return Flow<List<ItemEntity>> تدفق قائمة العناصر / Flow of items list
     */
    fun getItemsByTypeFlow(type: ItemType): Flow<List<ItemEntity>> {
        return inventoryDao.getItemsByTypeFlow(type)
            .catch { e ->
                e.printStackTrace()
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * الحصول على العناصر حسب الندرة
     * Get items by rarity
     * 
     * @param rarity الندرة / Rarity
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getItemsByRarity(rarity: Rarity): Result<List<ItemEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val items = inventoryDao.getItemsByRarity(rarity)
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على العناصر حسب الفئة
     * Get items by category
     * 
     * @param category الفئة / Category
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getItemsByCategory(category: String): Result<List<ItemEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val items = inventoryDao.getItemsByCategory(category)
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على العناصر القابلة للتجهيز
     * Get equippable items
     * 
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getEquippableItems(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val items = inventoryDao.getEquippableItems()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على العناصر القابلة للاستهلاك
     * Get consumable items
     * 
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getConsumableItems(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val items = inventoryDao.getConsumableItems()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على الأسلحة
     * Get weapons
     * 
     * @return Result<List<ItemEntity>> قائمة الأسلحة / Weapons list
     */
    suspend fun getWeapons(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val weapons = inventoryDao.getWeapons()
            Result.success(weapons)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على الدروع
     * Get armor
     * 
     * @return Result<List<ItemEntity>> قائمة الدروع / Armor list
     */
    suspend fun getArmor(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val armor = inventoryDao.getArmor()
            Result.success(armor)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على الأقنعة
     * Get masks
     * 
     * @return Result<List<ItemEntity>> قائمة الأقنعة / Masks list
     */
    suspend fun getMasks(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val masks = inventoryDao.getMasks()
            Result.success(masks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على المواد الخام
     * Get materials
     * 
     * @return Result<List<ItemEntity>> قائمة المواد / Materials list
     */
    suspend fun getMaterials(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val materials = inventoryDao.getMaterials()
            Result.success(materials)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على العناصر القابلة للترقية
     * Get upgradable items
     * 
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getUpgradableItems(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val items = inventoryDao.getUpgradableItems()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على العناصر القابلة للصياغة
     * Get craftable items
     * 
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getCraftableItems(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val items = inventoryDao.getCraftableItems()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على عناصر مجموعة
     * Get set items
     * 
     * @param setName اسم المجموعة / Set name
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getSetItems(setName: String): Result<List<ItemEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val items = inventoryDao.getSetItems(setName)
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * البحث عن عناصر بالاسم
     * Search items by name
     * 
     * @param searchQuery نص البحث / Search query
     * @return Result<List<ItemEntity>> نتائج البحث / Search results
     */
    suspend fun searchItemsByName(searchQuery: String): Result<List<ItemEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val items = inventoryDao.searchItemsByName(searchQuery)
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * البحث عن عناصر بالعلامات
     * Search items by tags
     * 
     * @param tag العلامة / Tag
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun searchItemsByTag(tag: String): Result<List<ItemEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val items = inventoryDao.searchItemsByTag(tag)
                Result.success(items)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations - عمليات التحديث
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث عنصر
     * Update item
     * 
     * @param item بيانات العنصر المحدثة / Updated item data
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateItem(item: ItemEntity): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = inventoryDao.updateItem(item)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ترقية عنصر
     * Upgrade item
     * 
     * @param itemId معرف العنصر / Item ID
     * @param newLevel المستوى الجديد / New upgrade level
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun upgradeItem(itemId: String, newLevel: Int): Result<Int> = 
        withContext(Dispatchers.IO) {
            try {
                val updatedRows = inventoryDao.upgradeItem(itemId, newLevel)
                invalidateCache()
                Result.success(updatedRows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * تحديث إحصائيات عنصر
     * Update item stats
     * 
     * @param itemId معرف العنصر / Item ID
     * @param attackPower قوة الهجوم / Attack power
     * @param defense قوة الدفاع / Defense
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateItemStats(
        itemId: String,
        attackPower: Float,
        defense: Float
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = inventoryDao.updateItemStats(itemId, attackPower, defense)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Delete Operations - عمليات الحذف
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حذف عنصر
     * Delete item
     * 
     * @param item بيانات العنصر / Item data
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteItem(item: ItemEntity): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = inventoryDao.deleteItem(item)
            invalidateCache()
            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف عنصر بمعرفه
     * Delete item by ID
     * 
     * @param itemId معرف العنصر / Item ID
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteItemById(itemId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = inventoryDao.deleteItemById(itemId)
            invalidateCache()
            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Sorting & Filtering - الترتيب والتصفية
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على العناصر مرتبة حسب القوة
     * Get items sorted by power
     * 
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getItemsSortedByPower(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val items = inventoryDao.getItemsSortedByPower()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على العناصر مرتبة حسب القيمة
     * Get items sorted by value
     * 
     * @return Result<List<ItemEntity>> قائمة العناصر / Items list
     */
    suspend fun getItemsSortedByValue(): Result<List<ItemEntity>> = withContext(Dispatchers.IO) {
        try {
            val items = inventoryDao.getItemsSortedByValue()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على أقوى سلاح
     * Get strongest weapon
     * 
     * @return Result<ItemEntity?> بيانات السلاح / Weapon data
     */
    suspend fun getStrongestWeapon(): Result<ItemEntity?> = withContext(Dispatchers.IO) {
        try {
            val weapon = inventoryDao.getStrongestWeapon()
            Result.success(weapon)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على أقوى درع
     * Get strongest armor
     * 
     * @return Result<ItemEntity?> بيانات الدرع / Armor data
     */
    suspend fun getStrongestArmor(): Result<ItemEntity?> = withContext(Dispatchers.IO) {
        try {
            val armor = inventoryDao.getStrongestArmor()
            Result.success(armor)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Analytics Operations - عمليات التحليلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على عدد العناصر
     * Get item count
     * 
     * @return Result<Int> عدد العناصر / Item count
     */
    suspend fun getItemCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = inventoryDao.getItemCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على عدد العناصر حسب النوع
     * Get item count by type
     * 
     * @param type نوع العنصر / Item type
     * @return Result<Int> عدد العناصر / Item count
     */
    suspend fun getItemCountByType(type: ItemType): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = inventoryDao.getItemCountByType(type)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على متوسط مستوى العناصر
     * Get average item level
     * 
     * @return Result<Float> متوسط المستوى / Average level
     */
    suspend fun getAverageItemLevel(): Result<Float> = withContext(Dispatchers.IO) {
        try {
            val average = inventoryDao.getAverageItemLevel()
            Result.success(average)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على إجمالي قيمة العناصر
     * Get total items value
     * 
     * @return Result<Long> إجمالي القيمة / Total value
     */
    suspend fun getTotalItemsValue(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val total = inventoryDao.getTotalItemsValue()
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على توزيع العناصر حسب النوع
     * Get item distribution by type
     * 
     * @return Result<Map<String, Int>> خريطة التوزيع / Distribution map
     */
    suspend fun getItemDistributionByType(): Result<Map<String, Int>> = 
        withContext(Dispatchers.IO) {
            try {
                val distribution = inventoryDao.getItemDistributionByType()
                Result.success(distribution)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على توزيع العناصر حسب الندرة
     * Get item distribution by rarity
     * 
     * @return Result<Map<String, Int>> خريطة التوزيع / Distribution map
     */
    suspend fun getItemDistributionByRarity(): Result<Map<String, Int>> = 
        withContext(Dispatchers.IO) {
            try {
                val distribution = inventoryDao.getItemDistributionByRarity()
                Result.success(distribution)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // Validation & Helper Methods - التحقق والدوال المساعدة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * التحقق من وجود عنصر
     * Check if item exists
     * 
     * @param itemId معرف العنصر / Item ID
     * @return Result<Boolean> true إذا كان موجود / true if exists
     */
    suspend fun itemExists(itemId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val exists = inventoryDao.itemExists(itemId)
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * التحقق من إمكانية ترقية عنصر
     * Check if item can be upgraded
     * 
     * @param itemId معرف العنصر / Item ID
     * @return Result<Boolean> true إذا كان قابل للترقية / true if can upgrade
     */
    suspend fun canUpgradeItem(itemId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val item = inventoryDao.getItem(itemId)
            val canUpgrade = item?.canUpgrade() ?: false
            Result.success(canUpgrade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * التحقق من إمكانية صياغة عنصر
     * Check if item can be crafted
     * 
     * @param itemId معرف العنصر / Item ID
     * @return Result<Boolean> true إذا كان قابل للصياغة / true if can craft
     */
    suspend fun canCraftItem(itemId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val item = inventoryDao.getItem(itemId)
            val canCraft = item?.isCraftable ?: false
            Result.success(canCraft)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cache Management - إدارة الكاش
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * التحقق من صلاحية الكاش
     * Check cache validity
     */
    private fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastCacheUpdate) < cacheValidityDuration
    }

    /**
     * إبطال الكاش
     * Invalidate cache
     */
    private fun invalidateCache() {
        cachedItems = null
        cachedItemsByType.clear()
        lastCacheUpdate = 0L
    }

    /**
     * مسح الكاش يدوياً
     * Clear cache manually
     */
    fun clearCache() {
        invalidateCache()
    }

    /**
     * تحديث الكاش مسبقاً
     * Preload cache
     */
    suspend fun preloadCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // تحميل جميع العناصر
            // Load all items
            getAllItems(forceRefresh = true)

            // تحميل العناصر حسب الأنواع الأساسية
            // Load items by main types
            ItemType.values().forEach { type ->
                getItemsByType(type, forceRefresh = true)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}