package com.erygra.maskoflight.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.engine.AudioEngine
import com.erygra.maskoflight.player.AbilityType
import com.erygra.maskoflight.player.Rarity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════════════════════════════
// Data Classes - فئات البيانات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * فئات العناصر
 * Item Categories
 */
enum class ItemCategory(
    val nameEn: String,
    val nameAr: String,
    val icon: ImageVector
) {
    ALL("All", "الكل", Icons.Default.List),
    WEAPONS("Weapons", "أسلحة", Icons.Default.Star),
    CONSUMABLES("Consumables", "مستهلكات", Icons.Default.Favorite),
    MATERIALS("Materials", "مواد", Icons.Default.Build),
    KEY_ITEMS("Key Items", "عناصر مفتاحية", Icons.Default.Lock),
    RELICS("Relics", "آثار", Icons.Default.Star),
    QUEST_ITEMS("Quest Items", "عناصر مهام", Icons.Default.Refresh)
}

/**
 * مستويات الندرة
 * Rarity Levels
 * Typealias for compatibility
 */
typealias ItemRarity = Rarity

/**
 * أنواع التأثيرات
 * Effect Types
 */
sealed class ItemEffect {
    data class RestoreHP(val amount: Float) : ItemEffect()
    data class RestoreEnergy(val amount: Float) : ItemEffect()
    data class GrantMF(val amount: Int) : ItemEffect()
    data class ReduceFM(val amount: Int) : ItemEffect()
    data class TemporaryBuff(val type: BuffType, val duration: Long) : ItemEffect()
    data class PermanentStat(val stat: StatType, val value: Float) : ItemEffect()
}

/**
 * أنواع التعزيزات المؤقتة
 * Temporary Buff Types
 */
enum class BuffType(val nameEn: String, val nameAr: String, val icon: ImageVector) {
    DAMAGE_BOOST("Damage Boost", "زيادة الضرر", Icons.Default.Star),
    DEFENSE_BOOST("Defense Boost", "زيادة الدفاع", Icons.Default.Build),
    SPEED_BOOST("Speed Boost", "زيادة السرعة", Icons.Default.Refresh),
    HP_REGEN("HP Regeneration", "تجديد الصحة", Icons.Default.Favorite),
    ENERGY_REGEN("Energy Regeneration", "تجديد الطاقة", Icons.Default.AccountCircle),
    STEALTH("Stealth", "التخفي", Icons.Default.Lock)
}

/**
 * أنواع الإحصائيات
 * Stat Types
 */
enum class StatType(val nameEn: String, val nameAr: String) {
    MAX_HP("Max HP", "أقصى صحة"),
    MAX_ENERGY("Max Energy", "أقصى طاقة"),
    DAMAGE("Damage", "ضرر"),
    DEFENSE("Defense", "دفاع"),
    SPEED("Speed", "سرعة"),
    JUMP_HEIGHT("Jump Height", "ارتفاع القفز"),
    CRIT_CHANCE("Critical Chance", "فرصة ضربة حرجة")
}

/**
 * متطلبات العنصر
 * Item Requirements
 */
data class ItemRequirements(
    val minLevel: Int? = null,
    val requiredAbility: AbilityType? = null,
    val requiredQuest: String? = null
)

/**
 * فتحات التجهيز
 * Equipment Slots
 */
enum class EquipSlot {
    WEAPON, ACCESSORY_1, ACCESSORY_2, RELIC
}

/**
 * بيانات العنصر
 * Item Data
 */
data class InventoryItem(
    val id: String,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    val category: ItemCategory,
    val rarity: ItemRarity,
    val iconPath: String,
    val stackable: Boolean,
    val maxStack: Int,
    val currentStack: Int,
    val isEquipped: Boolean,
    val isFavorite: Boolean,
    val isNew: Boolean,
    val weight: Float,
    val value: Int,
    val effects: List<ItemEffect>,
    val requirements: ItemRequirements? = null
)

/**
 * معلومات الشنطة
 * Satchel Information
 */
data class SatchelInfo(
    val totalSlots: Int,
    val usedSlots: Int,
    val currentWeight: Float,
    val maxWeight: Float,
    val upgradeLevel: Int,
    val upgradeCost: Int
) {
    companion object {
        /**
         * جدول ترقيات الشنطة
         * Satchel Upgrade Table
         */
        fun getUpgradeInfo(level: Int): Pair<Int, Float> = when (level) {
            0 -> Pair(60, 50f)
            1 -> Pair(75, 60f)
            2 -> Pair(90, 70f)
            3 -> Pair(110, 80f)
            4 -> Pair(130, 90f)
            5 -> Pair(150, 100f)
            else -> Pair(150, 100f)
        }

        fun getUpgradeCost(level: Int): Int = when (level) {
            0 -> 500
            1 -> 1000
            2 -> 2000
            3 -> 4000
            4 -> 8000
            else -> 0 // Max level
        }
    }
}

/**
 * أنواع الترتيب
 * Sort Types
 */
enum class SortType(val nameEn: String, val nameAr: String) {
    NAME_ASC("Name (A-Z)", "الاسم (أ-ي)"),
    NAME_DESC("Name (Z-A)", "الاسم (ي-أ)"),
    RARITY_ASC("Rarity ↑", "الندرة ↑"),
    RARITY_DESC("Rarity ↓", "الندرة ↓"),
    TYPE("Type", "النوع"),
    WEIGHT_ASC("Weight ↑", "الوزن ↑"),
    WEIGHT_DESC("Weight ↓", "الوزن ↓"),
    VALUE_ASC("Value ↑", "القيمة ↑"),
    VALUE_DESC("Value ↓", "القيمة ↓"),
    RECENT("Recent", "الأحدث")
}

// ════════════════════════════════════════════════════════════════════════════════════
// Main Inventory Screen - الشاشة الرئيسية للمخزون
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شاشة المخزون الرئيسية
 * Main Inventory Screen
 * 
 * @param inventory قائمة جميع العناصر
 * @param equippedItems العناصر المجهزة حالياً
 * @param satchelInfo معلومات الشنطة
 * @param currency العملات الحالية
 * @param memoryFragments شظايا الذاكرة
 * @param selectedCategory الفئة المختارة
 * @param sortType نوع الترتيب
 * @param onItemSelect عند اختيار عنصر
 * @param onItemUse عند استخدام عنصر
 * @param onItemEquip عند تجهيز عنصر
 * @param onItemDrop عند إسقاط عنصر
 * @param onItemFavorite عند تفضيل عنصر
 * @param onCategoryChange عند تغيير الفئة
 * @param onSortChange عند تغيير الترتيب
 * @param onUpgradeSatchel عند ترقية الشنطة
 * @param onClose عند الإغلاق
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    inventory: List<InventoryItem>,
    equippedItems: Map<EquipSlot, InventoryItem?>,
    satchelInfo: SatchelInfo,
    currency: Int,
    memoryFragments: Int,
    selectedCategory: ItemCategory,
    sortType: SortType,
    onItemSelect: (String) -> Unit,
    onItemUse: (String) -> Unit,
    onItemEquip: (String) -> Unit,
    onItemDrop: (String) -> Unit,
    onItemFavorite: (String) -> Unit,
    onCategoryChange: (ItemCategory) -> Unit,
    onSortChange: (SortType) -> Unit,
    onUpgradeSatchel: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showDropDialog by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<String>() }

    // تصفية وترتيب العناصر
    val filteredAndSortedItems by remember(inventory, selectedCategory, sortType) {
        derivedStateOf {
            val filtered = if (selectedCategory == ItemCategory.ALL) {
                inventory
            } else {
                inventory.filter { it.category == selectedCategory }
            }

            when (sortType) {
                SortType.NAME_ASC -> filtered.sortedBy { it.name }
                SortType.NAME_DESC -> filtered.sortedByDescending { it.name }
                SortType.RARITY_ASC -> filtered.sortedBy { it.rarity.ordinal }
                SortType.RARITY_DESC -> filtered.sortedByDescending { it.rarity.ordinal }
                SortType.TYPE -> filtered.sortedBy { it.category.ordinal }
                SortType.WEIGHT_ASC -> filtered.sortedBy { it.weight }
                SortType.WEIGHT_DESC -> filtered.sortedByDescending { it.weight }
                SortType.VALUE_ASC -> filtered.sortedBy { it.value }
                SortType.VALUE_DESC -> filtered.sortedByDescending { it.value }
                SortType.RECENT -> filtered.sortedByDescending { it.isNew }
            }
        }
    }

    val selectedItem = filteredAndSortedItems.find { it.id == selectedItemId }

    // التعامل مع لوحة المفاتيح
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ErytColor.VoidPrimary.copy(alpha = 0.95f))
            .onKeyEvent { event ->
                when {
                    event.key == Key.Escape && event.type == KeyEventType.KeyUp -> {
                        if (isMultiSelectMode) {
                            isMultiSelectMode = false
                            selectedItems.clear()
                        } else {
                            onClose()
                        }
                        true
                    }
                    event.key == Key.F && event.type == KeyEventType.KeyUp -> {
                        selectedItemId?.let { onItemFavorite(it) }
                        true
                    }
                    event.key == Key.D && event.type == KeyEventType.KeyUp -> {
                        selectedItemId?.let { showDropDialog = true }
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            InventoryHeader(
                currency = currency,
                memoryFragments = memoryFragments,
                onClose = onClose,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Left Panel - Grid & Categories
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    // Category & Sort Bar
                    SortFilterBar(
                        selectedCategory = selectedCategory,
                        currentSort = sortType,
                        onCategoryChange = { category ->
                            onCategoryChange(category)
                            scope.launch {
                                AudioEngine.playSFX("ui_tab_switch")
                            }
                        },
                        onSortChange = { sort ->
                            showSortMenu = false
                            onSortChange(sort)
                            scope.launch {
                                AudioEngine.playSFX("ui_click")
                            }
                        },
                        onSortMenuToggle = { showSortMenu = !showSortMenu },
                        showSortMenu = showSortMenu,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Satchel Info
                    SatchelInfoPanel(
                        satchelInfo = satchelInfo,
                        onUpgrade = { showUpgradeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Item Grid
                    InventoryGrid(
                        items = filteredAndSortedItems,
                        selectedItemId = selectedItemId,
                        selectedItems = selectedItems,
                        isMultiSelectMode = isMultiSelectMode,
                        columns = 4,
                        onItemClick = { itemId ->
                            if (isMultiSelectMode) {
                                if (selectedItems.contains(itemId)) {
                                    selectedItems.remove(itemId)
                                } else {
                                    selectedItems.add(itemId)
                                }
                            } else {
                                selectedItemId = itemId
                                onItemSelect(itemId)
                            }
                            scope.launch {
                                AudioEngine.playSFX("ui_select")
                            }
                        },
                        onItemLongPress = { itemId ->
                            isMultiSelectMode = true
                            selectedItems.clear()
                            selectedItems.add(itemId)
                            scope.launch {
                                AudioEngine.playSFX("ui_long_press")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // Multi-Select Actions
                    AnimatedVisibility(
                        visible = isMultiSelectMode && selectedItems.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        QuickActionsMenu(
                            selectedCount = selectedItems.size,
                            onDropAll = {
                                showDropDialog = true
                            },
                            onFavoriteAll = {
                                selectedItems.forEach { onItemFavorite(it) }
                                scope.launch {
                                    AudioEngine.playSFX("ui_favorite")
                                }
                            },
                            onCancel = {
                                isMultiSelectMode = false
                                selectedItems.clear()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Right Panel - Item Details
                AnimatedVisibility(
                    visible = selectedItem != null && !isMultiSelectMode,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                ) {
                    selectedItem?.let { item ->
                        ItemDetailsPanel(
                            item = item,
                            isEquipped = item.isEquipped,
                            canUse = item.category == ItemCategory.CONSUMABLES,
                            canEquip = item.category in listOf(
                                ItemCategory.WEAPONS,
                                ItemCategory.RELICS
                            ),
                            onUse = {
                                onItemUse(item.id)
                                scope.launch {
                                    AudioEngine.playSFX("item_use")
                                }
                            },
                            onEquip = {
                                onItemEquip(item.id)
                                scope.launch {
                                    AudioEngine.playSFX("item_equip")
                                }
                            },
                            onDrop = {
                                showDropDialog = true
                            },
                            onFavorite = {
                                onItemFavorite(item.id)
                                scope.launch {
                                    AudioEngine.playSFX("ui_favorite")
                                }
                            },
                            onCompare = if (item.category == ItemCategory.WEAPONS) {
                                { /* TODO: Compare with current weapon */ }
                            } else null,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Upgrade Dialog
        if (showUpgradeDialog) {
            val nextLevel = satchelInfo.upgradeLevel + 1
            val (newSlots, newWeight) = SatchelInfo.getUpgradeInfo(nextLevel)

            UpgradeSatchelDialog(
                currentLevel = satchelInfo.upgradeLevel,
                upgradeCost = satchelInfo.upgradeCost,
                newSlots = newSlots,
                newWeight = newWeight,
                currentCurrency = currency,
                onConfirm = {
                    onUpgradeSatchel()
                    showUpgradeDialog = false
                    scope.launch {
                        AudioEngine.playSFX("upgrade_success")
                        EventBus.emit(GameEvent.Inventory.SatchelUpgraded(nextLevel))
                    }
                },
                onDismiss = {
                    showUpgradeDialog = false
                }
            )
        }

        // Drop Dialog
        if (showDropDialog) {
            if (isMultiSelectMode) {
                DropMultipleItemsDialog(
                    itemCount = selectedItems.size,
                    onConfirm = {
                        selectedItems.forEach { onItemDrop(it) }
                        selectedItems.clear()
                        isMultiSelectMode = false
                        showDropDialog = false
                        scope.launch {
                            AudioEngine.playSFX("item_drop")
                        }
                    },
                    onDismiss = {
                        showDropDialog = false
                    }
                )
            } else {
                selectedItem?.let { item ->
                    DropItemDialog(
                        item = item,
                        quantity = item.currentStack,
                        onConfirm = {
                            onItemDrop(item.id)
                            selectedItemId = null
                            showDropDialog = false
                            scope.launch {
                                AudioEngine.playSFX("item_drop")
                            }
                        },
                        onDismiss = {
                            showDropDialog = false
                        }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Inventory Header - ترويسة المخزون
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * ترويسة شاشة المخزون
 * Inventory Screen Header
 */
@Composable
private fun InventoryHeader(
    currency: Int,
    memoryFragments: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErytPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title
            Column {
                Text(
                    text = "Inventory",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "المخزون",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                )
            }

            // Currency Bar
            CurrencyBar(
                coins = currency,
                memoryFragments = memoryFragments,
                modifier = Modifier.weight(1f)
            )

            // Close Button
            ErytButton(
                onClick = onClose,
                variant = ErytButtonVariant.SECONDARY,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Inventory / إغلاق المخزون",
                    tint = ErytColor.RadianceWhite
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Currency Bar - شريط العملات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شريط عرض العملات
 * Currency Display Bar
 */
@Composable
private fun CurrencyBar(
    coins: Int,
    memoryFragments: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coins
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = ErytColor.BlightGold,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = coins.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = ErytColor.BlightGold,
                fontWeight = FontWeight.Bold
            )
        }

        // Memory Fragments
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = ErytColor.MemoryAqua,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = memoryFragments.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = ErytColor.MemoryAqua,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Sort & Filter Bar - شريط الترتيب والتصفية
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شريط الفئات والترتيب
 * Category and Sort Bar
 */
@Composable
private fun SortFilterBar(
    selectedCategory: ItemCategory,
    currentSort: SortType,
    onCategoryChange: (ItemCategory) -> Unit,
    onSortChange: (SortType) -> Unit,
    onSortMenuToggle: () -> Unit,
    showSortMenu: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ItemCategory.values().forEach { category ->
                val isSelected = selectedCategory == category
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                )

                ErytButton(
                    onClick = { onCategoryChange(category) },
                    variant = if (isSelected) ErytButtonVariant.PRIMARY else ErytButtonVariant.SECONDARY,
                    modifier = Modifier.scale(scale)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = category.nameAr,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sort Button
        Box {
            ErytButton(
                onClick = onSortMenuToggle,
                variant = ErytButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = currentSort.nameAr)
                    }
                    Icon(
                        imageVector = if (showSortMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Sort Dropdown
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = onSortMenuToggle,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(ErytColor.SurfaceDark)
            ) {
                SortType.values().forEach { sortType ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = sortType.nameAr,
                                color = if (sortType == currentSort) ErytColor.BlightGold else ErytColor.RadianceWhite
                            )
                        },
                        onClick = { onSortChange(sortType) },
                        leadingIcon = {
                            if (sortType == currentSort) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ErytColor.BlightGold
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Satchel Info Panel - لوحة معلومات الشنطة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * لوحة معلومات الشنطة
 * Satchel Information Panel
 */
@Composable
private fun SatchelInfoPanel(
    satchelInfo: SatchelInfo,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slotsPercentage = satchelInfo.usedSlots.toFloat() / satchelInfo.totalSlots
    val weightPercentage = satchelInfo.currentWeight / satchelInfo.maxWeight
    val canUpgrade = satchelInfo.upgradeLevel < 5

    ErytPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الشنطة | Satchel Lv.${satchelInfo.upgradeLevel}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )

                if (canUpgrade) {
                    ErytButton(
                        onClick = onUpgrade,
                        variant = ErytButtonVariant.PRIMARY,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ترقية ${satchelInfo.upgradeCost}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Slots Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الفتحات | Slots",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${satchelInfo.usedSlots} / ${satchelInfo.totalSlots}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (slotsPercentage > 0.9f) ErytColor.BlightRed else ErytColor.MemoryAqua,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { slotsPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (slotsPercentage > 0.9f) ErytColor.BlightRed else ErytColor.MemoryAqua,
                    trackColor = ErytColor.OutlineGray
                )
            }

            // Weight Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الوزن | Weight",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${satchelInfo.currentWeight.roundToInt()} / ${satchelInfo.maxWeight.roundToInt()} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (weightPercentage > 0.9f) ErytColor.BlightRed else ErytColor.BlightGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { weightPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (weightPercentage > 0.9f) ErytColor.BlightRed else ErytColor.BlightGold,
                    trackColor = ErytColor.OutlineGray
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Inventory Grid - شبكة العناصر
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شبكة عرض العناصر
 * Item Display Grid
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InventoryGrid(
    items: List<InventoryItem>,
    selectedItemId: String?,
    selectedItems: List<String>,
    isMultiSelectMode: Boolean,
    columns: Int,
    onItemClick: (String) -> Unit,
    onItemLongPress: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ErytColor.SurfaceDark.copy(alpha = 0.5f))
            .padding(8.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = items.size,
            key = { index -> items[index].id }
        ) { index ->
            val item = items[index]
            val isSelected = if (isMultiSelectMode) {
                selectedItems.contains(item.id)
            } else {
                selectedItemId == item.id
            }

            InventoryItemCell(
                item = item,
                isSelected = isSelected,
                isMultiSelectMode = isMultiSelectMode,
                onClick = { onItemClick(item.id) },
                onLongPress = { onItemLongPress(item.id) },
                modifier = Modifier.animateItemPlacement(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            )
        }
    }
}

/**
 * خلية عنصر في الشبكة
 * Grid Item Cell
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InventoryItemCell(
    item: InventoryItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    val borderColor = if (isSelected) ErytColor.BlightGold else item.rarity.color

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(ErytColor.SurfaceDark)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .semantics {
                contentDescription = "${item.nameArabic} - ${item.rarity.nameAr}"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        // Rarity Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .blur(8.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            item.rarity.color.copy(alpha = item.rarity.glowIntensity * 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Item Icon (Placeholder)
        ItemIcon(
            item = item,
            size = 56.dp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Stack Counter
        if (item.stackable && item.currentStack > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        ErytColor.VoidPrimary.copy(alpha = 0.8f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "×${item.currentStack}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // New Badge
        if (item.isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(ErytColor.BlightRed, CircleShape)
                    .size(12.dp)
            )
        }

        // Favorite Star
        if (item.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite",
                tint = ErytColor.BlightGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        // Equipped Marker
        if (item.isEquipped) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Equipped",
                tint = Color(0xFF2ECC71),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        // Multi-Select Checkbox
        if (isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = ErytColor.BlightGold,
                    uncheckedColor = ErytColor.OutlineGray
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Item Icon - أيقونة العنصر
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * أيقونة العنصر مع التوهج
 * Item Icon with Glow Effect
 */
@Composable
private fun ItemIcon(
    item: InventoryItem,
    size: Dp,
    modifier: Modifier = Modifier
) {
    // Placeholder icon (في الإنتاج، سيتم تحميل الصورة من item.iconPath)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(item.rarity.color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.category.icon,
            contentDescription = null,
            tint = item.rarity.color,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Item Details Panel - لوحة تفاصيل العنصر
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * لوحة تفاصيل العنصر
 * Item Details Panel
 */
@Composable
private fun ItemDetailsPanel(
    item: InventoryItem,
    isEquipped: Boolean,
    canUse: Boolean,
    canEquip: Boolean,
    onUse: () -> Unit,
    onEquip: () -> Unit,
    onDrop: () -> Unit,
    onFavorite: () -> Unit,
    onCompare: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    ErytPanel(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Item Icon Large
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                item.rarity.color.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, item.rarity.color, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                ItemIcon(item = item, size = 80.dp)
            }

            // Item Name & Rarity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.nameArabic,
                    style = MaterialTheme.typography.titleLarge,
                    color = item.rarity.color,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.rarity.nameAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = item.rarity.color,
                    modifier = Modifier
                        .background(item.rarity.color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Divider(color = ErytColor.OutlineGray)

            // Description
            Text(
                text = item.descriptionArabic,
                style = MaterialTheme.typography.bodyMedium,
                color = ErytColor.RadianceWhite,
                textAlign = TextAlign.Start
            )

            // Effects
            if (item.effects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "التأثيرات | Effects",
                        style = MaterialTheme.typography.titleSmall,
                        color = ErytColor.MemoryAqua,
                        fontWeight = FontWeight.Bold
                    )
                    item.effects.forEach { effect ->
                        ItemEffectRow(effect = effect)
                    }
                }
            }

            // Requirements
            item.requirements?.let { req ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "المتطلبات | Requirements",
                        style = MaterialTheme.typography.titleSmall,
                        color = ErytColor.BlightRed,
                        fontWeight = FontWeight.Bold
                    )
                    req.minLevel?.let { level ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = ErytColor.BlightGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "المستوى: $level",
                                style = MaterialTheme.typography.bodySmall,
                                color = ErytColor.RadianceWhite
                            )
                        }
                    }
                }
            }

            Divider(color = ErytColor.OutlineGray)

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "الوزن | Weight",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${item.weight} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErytColor.RadianceWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "القيمة | Value",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ErytColor.BlightGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.value.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.BlightGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canUse && !isEquipped) {
                    ErytButton(
                        onClick = onUse,
                        variant = ErytButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("استخدام | Use")
                    }
                }

                if (canEquip) {
                    ErytButton(
                        onClick = onEquip,
                        variant = if (isEquipped) ErytButtonVariant.SECONDARY else ErytButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEquipped) "إزالة | Unequip" else "تجهيز | Equip")
                    }
                }

                onCompare?.let {
                    ErytButton(
                        onClick = it,
                        variant = ErytButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مقارنة | Compare")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ErytButton(
                        onClick = onFavorite,
                        variant = ErytButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) ErytColor.BlightGold else ErytColor.RadianceWhite
                        )
                    }

                    ErytButton(
                        onClick = onDrop,
                        variant = ErytButtonVariant.DANGER,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Drop"
                        )
                    }
                }
            }
        }
    }
}

/**
 * صف عرض تأثير العنصر
 * Item Effect Display Row
 */
@Composable
private fun ItemEffectRow(effect: ItemEffect) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ErytColor.SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, text, color) = when (effect) {
            is ItemEffect.RestoreHP -> Triple(
                Icons.Default.Favorite,
                "استعادة ${effect.amount.roundToInt()} صحة | Restore ${effect.amount.roundToInt()} HP",
                Color(0xFF2ECC71)
            )
            is ItemEffect.RestoreEnergy -> Triple(
                Icons.Default.AccountCircle,
                "استعادة ${effect.amount.roundToInt()} طاقة | Restore ${effect.amount.roundToInt()} Energy",
                ErytColor.MemoryAqua
            )
            is ItemEffect.GrantMF -> Triple(
                Icons.Default.Star,
                "+${effect.amount} شظايا | +${effect.amount} MF",
                ErytColor.MemoryAqua
            )
            is ItemEffect.ReduceFM -> Triple(
                Icons.Default.AccountCircle,
                "-${effect.amount} ظلام | -${effect.amount} FM",
                ErytColor.BlightGold
            )
            is ItemEffect.TemporaryBuff -> Triple(
                effect.type.icon,
                "${effect.type.nameAr} (${effect.duration / 1000}s)",
                ErytColor.BlightGold
            )
            is ItemEffect.PermanentStat -> Triple(
                Icons.Default.Star,
                "+${effect.value} ${effect.stat.nameAr}",
                ErytColor.BlightGold
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = ErytColor.RadianceWhite
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Quick Actions Menu - قائمة الإجراءات السريعة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * قائمة الإجراءات السريعة للعناصر المتعددة
 * Quick Actions Menu for Multiple Items
 */
@Composable
private fun QuickActionsMenu(
    selectedCount: Int,
    onDropAll: () -> Unit,
    onFavoriteAll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErytPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount عنصر محدد",
                style = MaterialTheme.typography.titleMedium,
                color = ErytColor.RadianceWhite,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ErytButton(
                    onClick = onFavoriteAll,
                    variant = ErytButtonVariant.SECONDARY,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorite All",
                        modifier = Modifier.size(20.dp)
                    )
                }

                ErytButton(
                    onClick = onDropAll,
                    variant = ErytButtonVariant.DANGER,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Drop All",
                        modifier = Modifier.size(20.dp)
                    )
                }

                ErytButton(
                    onClick = onCancel,
                    variant = ErytButtonVariant.SECONDARY,
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Dialogs - الحوارات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * حوار تأكيد إسقاط عنصر واحد
 * Single Item Drop Confirmation Dialog
 */
@Composable
private fun DropItemDialog(
    item: InventoryItem,
    quantity: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ErytPanel(
            modifier = Modifier
                .width(350.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "تأكيد الإسقاط",
                    style = MaterialTheme.typography.titleLarge,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "هل أنت متأكد من إسقاط ${item.nameArabic}${if (quantity > 1) " (×$quantity)" else ""}؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite
                )

                Text(
                    text = "لن تتمكن من استعادته!",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErytColor.BlightRed
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ErytButton(
                        onClick = onDismiss,
                        variant = ErytButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء | Cancel")
                    }

                    ErytButton(
                        onClick = onConfirm,
                        variant = ErytButtonVariant.DANGER,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إسقاط | Drop")
                    }
                }
            }
        }
    }
}

/**
 * حوار تأكيد إسقاط عناصر متعددة
 * Multiple Items Drop Confirmation Dialog
 */
@Composable
private fun DropMultipleItemsDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ErytPanel(
            modifier = Modifier
                .width(350.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "تأكيد الإسقاط الجماعي",
                    style = MaterialTheme.typography.titleLarge,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "هل أنت متأكد من إسقاط $itemCount عنصر؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite
                )

                Text(
                    text = "لن تتمكن من استعادتهم!",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErytColor.BlightRed
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ErytButton(
                        onClick = onDismiss,
                        variant = ErytButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء | Cancel")
                    }

                    ErytButton(
                        onClick = onConfirm,
                        variant = ErytButtonVariant.DANGER,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إسقاط الكل | Drop All")
                    }
                }
            }
        }
    }
}

/**
 * حوار ترقية الشنطة
 * Satchel Upgrade Dialog
 */
@Composable
private fun UpgradeSatchelDialog(
    currentLevel: Int,
    upgradeCost: Int,
    newSlots: Int,
    newWeight: Float,
    currentCurrency: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val canAfford = currentCurrency >= upgradeCost

    Dialog(onDismissRequest = onDismiss) {
        ErytPanel(
            modifier = Modifier
                .width(400.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ترقية الشنطة | Upgrade Satchel",
                    style = MaterialTheme.typography.titleLarge,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "المستوى: $currentLevel → ${currentLevel + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    color = ErytColor.MemoryAqua
                )

                Divider(color = ErytColor.OutlineGray)

                // Upgrade Info
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الفتحات:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${SatchelInfo.getUpgradeInfo(currentLevel).first} → $newSlots",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.BlightGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الوزن الأقصى:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${SatchelInfo.getUpgradeInfo(currentLevel).second} → $newWeight kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.BlightGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "التكلفة:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (canAfford) ErytColor.BlightGold else ErytColor.BlightRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = upgradeCost.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (canAfford) ErytColor.BlightGold else ErytColor.BlightRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!canAfford) {
                    Text(
                        text = "عملات غير كافية!",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErytColor.BlightRed
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ErytButton(
                        onClick = onDismiss,
                        variant = ErytButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء | Cancel")
                    }

                    ErytButton(
                        onClick = onConfirm,
                        variant = ErytButtonVariant.PRIMARY,
                        enabled = canAfford,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ترقية | Upgrade")
                    }
                }
            }
        }
    }
}