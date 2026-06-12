package com.erygra.maskoflight.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.engine.AudioEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════════════════════════════
// Data Classes - فئات البيانات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * حالة المتجر
 * Shop State
 */
data class ShopState(
    val merchant: MerchantData,
    val shopInventory: List<ShopItem>,
    val playerInventory: List<ShopItem>,
    val buybackItems: List<ShopItem>,
    val playerCurrency: Int,
    val playerMemoryFragments: Int,
    val relationshipLevel: Int,
    val currentTab: ShopTab,
    val selectedCategory: ItemCategory
)

/**
 * بيانات التاجر
 * Merchant Data
 */
data class MerchantData(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val description: String,
    val portraitPath: String,
    val color: Color,
    val specialization: MerchantSpecialization,
    val greetingEn: String,
    val greetingAr: String,
    val farewellEn: String,
    val farewellAr: String
)

/**
 * تخصص التاجر
 * Merchant Specialization
 */
enum class MerchantSpecialization(
    val nameEn: String,
    val nameAr: String,
    val icon: ImageVector
) {
    GENERAL("General Goods", "بضائع عامة", Icons.Default.ShoppingCart),
    WEAPONS("Weapons & Armor", "أسلحة ودروع", Icons.Default.Star),
    POTIONS("Potions & Herbs", "جرعات وأعشاب", Icons.Default.Favorite),
    RELICS("Ancient Relics", "آثار قديمة", Icons.Default.AccountCircle),
    MATERIALS("Crafting Materials", "مواد تصنيع", Icons.Default.Build),
    SECRETS("Rare Secrets", "أسرار نادرة", Icons.Default.Lock)
}

/**
 * عنصر في المتجر
 * Shop Item
 */
data class ShopItem(
    val itemId: String,
    val nameEn: String,
    val nameAr: String,
    val description: String,
    val category: ItemCategory,
    val rarity: ItemRarity,
    val basePrice: Int,
    val sellPrice: Int,
    val currentPrice: Int, // بعد الخصومات
    val stock: Int?, // null = unlimited
    val isSpecialOffer: Boolean,
    val discountPercentage: Int,
    val iconPath: String,
    val effects: List<ItemEffect>,
    val requirements: ItemRequirements?
)

/**
 * تبويبات المتجر
 * Shop Tabs
 */
enum class ShopTab(val nameEn: String, val nameAr: String, val icon: ImageVector) {
    BUY("Buy", "شراء", Icons.Default.ShoppingCart),
    SELL("Sell", "بيع", Icons.Default.Star),
    BUYBACK("Buyback", "استرجاع", Icons.Default.Refresh)
}

/**
 * مستويات العلاقة مع التاجر
 * Merchant Relationship Levels
 */
enum class RelationshipLevel(
    val level: Int,
    val nameEn: String,
    val nameAr: String,
    val discountPercentage: Int,
    val sellBonus: Int,
    val color: Color
) {
    STRANGER(0, "Stranger", "غريب", 0, 0, ErytColor.OutlineGray),
    ACQUAINTANCE(1, "Acquaintance", "معارف", 5, 5, Color(0xFF95A5A6)),
    FRIEND(2, "Friend", "صديق", 10, 10, Color(0xFF3498DB)),
    TRUSTED(3, "Trusted", "موثوق", 15, 15, Color(0xFF2ECC71)),
    PARTNER(4, "Partner", "شريك", 20, 20, ErytColor.BlightGold),
    LEGENDARY(5, "Legendary", "أسطوري", 25, 25, Color(0xFF9B59B6))
}

// ════════════════════════════════════════════════════════════════════════════════════
// Main Shop Screen - الشاشة الرئيسية للمتجر
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شاشة المتجر الرئيسية
 * Main Shop Screen
 * 
 * @param shopState حالة المتجر
 * @param onBuyItem شراء عنصر
 * @param onSellItem بيع عنصر
 * @param onBuybackItem استرجاع عنصر
 * @param onTabChange تغيير التبويب
 * @param onCategoryChange تغيير الفئة
 * @param onClose إغلاق المتجر
 */
@Composable
fun ShopScreen(
    shopState: ShopState,
    onBuyItem: (String, Int) -> Unit,
    onSellItem: (String, Int) -> Unit,
    onBuybackItem: (String, Int) -> Unit,
    onTabChange: (ShopTab) -> Unit,
    onCategoryChange: (ItemCategory) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf<ShopItem?>(null) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionQuantity by remember { mutableStateOf(1) }

    // Calculate relationship level
    val relationshipLevel = remember(shopState.relationshipLevel) {
        RelationshipLevel.values().findLast { it.level <= shopState.relationshipLevel } ?: RelationshipLevel.STRANGER
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ErytColor.VoidPrimary.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ShopHeader(
                merchant = shopState.merchant,
                relationshipLevel = relationshipLevel,
                currency = shopState.playerCurrency,
                memoryFragments = shopState.playerMemoryFragments,
                onClose = onClose,
                modifier = Modifier.fillMaxWidth()
            )

            // Tabs
            ShopTabs(
                currentTab = shopState.currentTab,
                buybackCount = shopState.buybackItems.size,
                onTabChange = { tab ->
                    onTabChange(tab)
                    scope.launch {
                        AudioEngine.playSFX("ui_tab_switch")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Items List
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    // Category Filter
                    if (shopState.currentTab != ShopTab.BUYBACK) {
                        CategoryFilter(
                            selectedCategory = shopState.selectedCategory,
                            onCategoryChange = onCategoryChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Items Grid
                    val itemsToShow = when (shopState.currentTab) {
                        ShopTab.BUY -> shopState.shopInventory.filter {
                            shopState.selectedCategory == ItemCategory.ALL || it.category == shopState.selectedCategory
                        }
                        ShopTab.SELL -> shopState.playerInventory.filter {
                            shopState.selectedCategory == ItemCategory.ALL || it.category == shopState.selectedCategory
                        }
                        ShopTab.BUYBACK -> shopState.buybackItems
                    }

                    ShopItemsList(
                        items = itemsToShow,
                        currentTab = shopState.currentTab,
                        relationshipLevel = relationshipLevel,
                        selectedItemId = selectedItem?.itemId,
                        onItemClick = { item ->
                            selectedItem = item
                            scope.launch {
                                AudioEngine.playSFX("ui_select")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                // Item Details Panel
                AnimatedVisibility(
                    visible = selectedItem != null,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                ) {
                    selectedItem?.let { item ->
                        ShopItemDetailsPanel(
                            item = item,
                            currentTab = shopState.currentTab,
                            relationshipLevel = relationshipLevel,
                            playerCurrency = shopState.playerCurrency,
                            onTransaction = { quantity ->
                                transactionQuantity = quantity
                                showTransactionDialog = true
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Transaction Confirmation Dialog
        if (showTransactionDialog && selectedItem != null) {
            TransactionConfirmationDialog(
                item = selectedItem!!,
                quantity = transactionQuantity,
                transactionType = shopState.currentTab,
                relationshipLevel = relationshipLevel,
                playerCurrency = shopState.playerCurrency,
                onConfirm = {
                    when (shopState.currentTab) {
                        ShopTab.BUY -> onBuyItem(selectedItem!!.itemId, transactionQuantity)
                        ShopTab.SELL -> onSellItem(selectedItem!!.itemId, transactionQuantity)
                        ShopTab.BUYBACK -> onBuybackItem(selectedItem!!.itemId, transactionQuantity)
                    }
                    showTransactionDialog = false
                    selectedItem = null
                    scope.launch {
                        AudioEngine.playSFX("shop_transaction")
                        EventBus.emit(
                            when (shopState.currentTab) {
                                ShopTab.BUY -> GameEvent.Shop.ItemPurchased(selectedItem!!.itemId, transactionQuantity)
                                ShopTab.SELL -> GameEvent.Shop.ItemSold(selectedItem!!.itemId, transactionQuantity)
                                ShopTab.BUYBACK -> GameEvent.Shop.ItemBoughtBack(selectedItem!!.itemId, transactionQuantity)
                            }
                        )
                    }
                },
                onDismiss = {
                    showTransactionDialog = false
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Shop Header - ترويسة المتجر
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * ترويسة شاشة المتجر
 * Shop Screen Header
 */
@Composable
private fun ShopHeader(
    merchant: MerchantData,
    relationshipLevel: RelationshipLevel,
    currency: Int,
    memoryFragments: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErytPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Merchant Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Portrait
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    merchant.color.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(3.dp, merchant.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = merchant.specialization.icon,
                        contentDescription = merchant.nameAr,
                        tint = merchant.color,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Column {
                    Text(
                        text = merchant.nameAr,
                        style = MaterialTheme.typography.titleLarge,
                        color = merchant.color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = merchant.specialization.nameAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                    // Relationship Level
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = relationshipLevel.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${relationshipLevel.nameAr} (${relationshipLevel.discountPercentage}% خصم)",
                            style = MaterialTheme.typography.labelSmall,
                            color = relationshipLevel.color
                        )
                    }
                }
            }

            // Currency Display
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = ErytColor.BlightGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = currency.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ErytColor.BlightGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = ErytColor.MemoryAqua,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = memoryFragments.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ErytColor.MemoryAqua,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Close Button
            ErytButton(
                onClick = onClose,
                variant = ErytButtonVariant.SECONDARY,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Shop"
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Shop Tabs - تبويبات المتجر
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * تبويبات المتجر
 * Shop Tabs
 */
@Composable
private fun ShopTabs(
    currentTab: ShopTab,
    buybackCount: Int,
    onTabChange: (ShopTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShopTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )

            ErytButton(
                onClick = { onTabChange(tab) },
                variant = if (isSelected) ErytButtonVariant.PRIMARY else ErytButtonVariant.SECONDARY,
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(tab.nameAr)
                    
                    // Buyback badge
                    if (tab == ShopTab.BUYBACK && buybackCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(ErytColor.BlightRed, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = buybackCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Category Filter - تصفية الفئات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شريط تصفية الفئات
 * Category Filter Bar
 */
@Composable
private fun CategoryFilter(
    selectedCategory: ItemCategory,
    onCategoryChange: (ItemCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ItemCategory.values().forEach { category ->
            val isSelected = selectedCategory == category
            
            FilterChip(
                selected = isSelected,
                onClick = { onCategoryChange(category) },
                label = { Text(category.nameAr) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ErytColor.BlightGold.copy(alpha = 0.3f),
                    selectedLabelColor = ErytColor.BlightGold,
                    selectedLeadingIconColor = ErytColor.BlightGold
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Shop Items List - قائمة العناصر
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * قائمة عناصر المتجر
 * Shop Items List
 */
@Composable
private fun ShopItemsList(
    items: List<ShopItem>,
    currentTab: ShopTab,
    relationshipLevel: RelationshipLevel,
    selectedItemId: String?,
    onItemClick: (ShopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ErytColor.SurfaceDark.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(
            items = items,
            key = { it.itemId }
        ) { item ->
            ShopItemCard(
                item = item,
                currentTab = currentTab,
                relationshipLevel = relationshipLevel,
                isSelected = selectedItemId == item.itemId,
                onClick = { onItemClick(item) }
            )
        }

        if (items.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (currentTab) {
                            ShopTab.BUY -> "لا توجد عناصر متاحة"
                            ShopTab.SELL -> "مخزونك فارغ"
                            ShopTab.BUYBACK -> "لا توجد عناصر للاسترجاع"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * بطاقة عنصر واحد
 * Single Shop Item Card
 */
@Composable
private fun ShopItemCard(
    item: ShopItem,
    currentTab: ShopTab,
    relationshipLevel: RelationshipLevel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered || isSelected) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    val price = when (currentTab) {
        ShopTab.BUY -> item.currentPrice
        ShopTab.SELL -> item.sellPrice + (item.sellPrice * relationshipLevel.sellBonus / 100)
        ShopTab.BUYBACK -> item.sellPrice
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    ErytColor.SurfaceDark.copy(alpha = 0.9f)
                } else {
                    ErytColor.SurfaceDark.copy(alpha = 0.6f)
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ErytColor.BlightGold else item.rarity.color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(item.rarity.color.copy(alpha = 0.2f))
                    .border(1.dp, item.rarity.color, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.category.icon,
                    contentDescription = null,
                    tint = item.rarity.color,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Name
                Text(
                    text = item.nameAr,
                    style = MaterialTheme.typography.titleSmall,
                    color = item.rarity.color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Rarity
                Text(
                    text = item.rarity.nameAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                )

                // Stock
                item.stock?.let { stock ->
                    if (stock < 5) {
                        Text(
                            text = "⚠️ متبقي: $stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = ErytColor.BlightRed
                        )
                    }
                }
            }

            // Price
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Discounted price
                if (item.isSpecialOffer && currentTab == ShopTab.BUY) {
                    Text(
                        text = item.basePrice.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = ErytColor.OutlineGray,
                        textDecoration = TextDecoration.LineThrough
                    )
                }

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
                        text = price.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ErytColor.BlightGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Special offer badge
                if (item.isSpecialOffer && currentTab == ShopTab.BUY) {
                    Text(
                        text = "🔥 ${item.discountPercentage}% خصم",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErytColor.BlightRed
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Item Details Panel - لوحة تفاصيل العنصر
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * لوحة تفاصيل عنصر المتجر
 * Shop Item Details Panel
 */
@Composable
private fun ShopItemDetailsPanel(
    item: ShopItem,
    currentTab: ShopTab,
    relationshipLevel: RelationshipLevel,
    playerCurrency: Int,
    onTransaction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var quantity by remember(item.itemId) { mutableStateOf(1) }
    
    val price = when (currentTab) {
        ShopTab.BUY -> item.currentPrice
        ShopTab.SELL -> item.sellPrice + (item.sellPrice * relationshipLevel.sellBonus / 100)
        ShopTab.BUYBACK -> item.sellPrice
    }
    
    val totalPrice = price * quantity
    val canAfford = when (currentTab) {
        ShopTab.BUY, ShopTab.BUYBACK -> playerCurrency >= totalPrice
        ShopTab.SELL -> true
    }

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
                Icon(
                    imageVector = item.category.icon,
                    contentDescription = null,
                    tint = item.rarity.color,
                    modifier = Modifier.size(70.dp)
                )
            }

            // Name & Rarity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.nameAr,
                    style = MaterialTheme.typography.titleLarge,
                    color = item.rarity.color,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.rarity.nameAr,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier
                        .background(item.rarity.color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .border(1.dp, item.rarity.color, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Divider(color = ErytColor.OutlineGray)

            // Description
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ErytColor.RadianceWhite
            )

            // Effects (if any)
            if (item.effects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "التأثيرات",
                        style = MaterialTheme.typography.titleSmall,
                        color = ErytColor.MemoryAqua,
                        fontWeight = FontWeight.Bold
                    )
                    // Display effects (simplified)
                    item.effects.forEach { effect ->
                        Text(
                            text = "• ${effect.toString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Divider(color = ErytColor.OutlineGray)

            // Price Info
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Base price vs current
                if (item.isSpecialOffer && currentTab == ShopTab.BUY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "السعر الأصلي:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                        )
                        Text(
                            text = item.basePrice.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.OutlineGray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (currentTab) {
                            ShopTab.BUY -> "سعر الشراء:"
                            ShopTab.SELL -> "سعر البيع:"
                            ShopTab.BUYBACK -> "سعر الاسترجاع:"
                        },
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
                            tint = ErytColor.BlightGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = price.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = ErytColor.BlightGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quantity Selector
            QuantitySelector(
                quantity = quantity,
                maxQuantity = item.stock ?: 99,
                onQuantityChange = { quantity = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الإجمالي:",
                    style = MaterialTheme.typography.titleMedium,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (canAfford) ErytColor.BlightGold else ErytColor.BlightRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = totalPrice.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (canAfford) ErytColor.BlightGold else ErytColor.BlightRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Transaction Button
            ErytButton(
                onClick = { onTransaction(quantity) },
                variant = ErytButtonVariant.PRIMARY,
                enabled = canAfford,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (currentTab) {
                        ShopTab.BUY -> "شراء"
                        ShopTab.SELL -> "بيع"
                        ShopTab.BUYBACK -> "استرجاع"
                    }
                )
            }

            if (!canAfford && currentTab == ShopTab.BUY) {
                Text(
                    text = "⚠️ عملات غير كافية!",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErytColor.BlightRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Quantity Selector - محدد الكمية
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * محدد كمية العناصر
 * Item Quantity Selector
 */
@Composable
private fun QuantitySelector(
    quantity: Int,
    maxQuantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ErytColor.SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "الكمية:",
            style = MaterialTheme.typography.bodyMedium,
            color = ErytColor.RadianceWhite
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrease
            IconButton(
                onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                enabled = quantity > 1,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Decrease",
                    tint = if (quantity > 1) ErytColor.RadianceWhite else ErytColor.OutlineGray
                )
            }

            // Quantity Display
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = ErytColor.BlightGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 40.dp),
                textAlign = TextAlign.Center
            )

            // Increase
            IconButton(
                onClick = { if (quantity < maxQuantity) onQuantityChange(quantity + 1) },
                enabled = quantity < maxQuantity,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (quantity < maxQuantity) ErytColor.RadianceWhite else ErytColor.OutlineGray
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Transaction Confirmation Dialog - حوار تأكيد المعاملة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * حوار تأكيد المعاملة
 * Transaction Confirmation Dialog
 */
@Composable
private fun TransactionConfirmationDialog(
    item: ShopItem,
    quantity: Int,
    transactionType: ShopTab,
    relationshipLevel: RelationshipLevel,
    playerCurrency: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val price = when (transactionType) {
        ShopTab.BUY -> item.currentPrice
        ShopTab.SELL -> item.sellPrice + (item.sellPrice * relationshipLevel.sellBonus / 100)
        ShopTab.BUYBACK -> item.sellPrice
    }
    
    val totalPrice = price * quantity

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
                    text = when (transactionType) {
                        ShopTab.BUY -> "تأكيد الشراء"
                        ShopTab.SELL -> "تأكيد البيع"
                        ShopTab.BUYBACK -> "تأكيد الاسترجاع"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )

                Divider(color = ErytColor.OutlineGray)

                // Item info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(item.rarity.color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, item.rarity.color, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.category.icon,
                            contentDescription = null,
                            tint = item.rarity.color,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.nameAr,
                            style = MaterialTheme.typography.titleMedium,
                            color = item.rarity.color,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الكمية: $quantity",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                        )
                    }
                }

                // Price breakdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "السعر لكل قطعة:",
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
                                tint = ErytColor.BlightGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = price.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErytColor.BlightGold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الإجمالي:",
                            style = MaterialTheme.typography.titleMedium,
                            color = ErytColor.RadianceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = ErytColor.BlightGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = totalPrice.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = ErytColor.BlightGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
                        Text("إلغاء")
                    }

                    ErytButton(
                        onClick = onConfirm,
                        variant = ErytButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تأكيد")
                    }
                }
            }
        }
    }
}