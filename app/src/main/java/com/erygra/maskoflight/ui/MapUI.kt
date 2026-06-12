package com.erygra.maskoflight.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.erygra.maskoflight.world.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════════════════════════════
// Data Classes - فئات البيانات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * حالة الخريطة
 * Map State
 */
data class MapState(
    val regions: List<MapRegion>,
    val playerPosition: Offset,
    val currentRegionId: String,
    val discoveredRegions: Set<String>,
    val explorationPercentage: Float,
    val pointsOfInterest: List<MapPOI>,
    val activeQuests: List<MapQuest>,
    val fastTravelPoints: List<FastTravelPoint>,
    val playerMarkers: List<PlayerMarker>
)

/**
 * منطقة على الخريطة
 * Map Region
 */
data class MapRegion(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val bounds: Rect,
    val difficulty: RegionDifficulty,
    val isDiscovered: Boolean,
    val explorationProgress: Float,
    val backgroundColor: Color,
    val borderColor: Color
)

/**
 * صعوبة المنطقة
 * Region Difficulty
 */
enum class RegionDifficulty(val nameEn: String, val nameAr: String, val color: Color) {
    SAFE("Safe", "آمن", Color(0xFF2ECC71)),
    MODERATE("Moderate", "متوسط", Color(0xFFF39C12)),
    DANGEROUS("Dangerous", "خطر", Color(0xFFE74C3C)),
    DEADLY("Deadly", "مميت", Color(0xFF8B00FF))
}

/**
 * نقطة اهتمام
 * Point of Interest
 */
data class MapPOI(
    val id: String,
    val type: POIType,
    val position: Offset,
    val nameEn: String,
    val nameAr: String,
    val isDiscovered: Boolean,
    val isCompleted: Boolean,
    val regionId: String
)

/**
 * أنواع نقاط الاهتمام
 * POI Types
 */
enum class POIType(
    val nameEn: String,
    val nameAr: String,
    val icon: ImageVector,
    val color: Color
) {
    SANCTUARY("Sanctuary", "ملاذ", Icons.Default.Home, ErytColor.MemoryAqua),
    BOSS("Boss", "زعيم", Icons.Default.Star, ErytColor.BlightRed),
    SECRET("Secret", "سر", Icons.Default.Lock, ErytColor.BlightGold),
    QUEST("Quest", "مهمة", Icons.Default.Refresh, Color(0xFF3498DB)),
    TRANSPORT("Transport", "نقل", Icons.Default.Send, Color(0xFF9B59B6)),
    VENDOR("Vendor", "بائع", Icons.Default.ShoppingCart, Color(0xFFF39C12)),
    SAVE_POINT("Save Point", "نقطة حفظ", Icons.Default.Favorite, Color(0xFF2ECC71))
}

/**
 * مهمة على الخريطة
 * Map Quest
 */
data class MapQuest(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val position: Offset,
    val isMainQuest: Boolean,
    val regionId: String
)

/**
 * نقطة سفر سريع
 * Fast Travel Point
 */
data class FastTravelPoint(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val position: Offset,
    val regionId: String,
    val isUnlocked: Boolean,
    val cost: Int
)

/**
 * علامة اللاعب
 * Player Marker
 */
data class PlayerMarker(
    val id: String,
    val position: Offset,
    val label: String,
    val color: Color,
    val icon: ImageVector
)

/**
 * طبقة الخريطة
 * Map Layer
 */
enum class MapLayer(val nameEn: String, val nameAr: String) {
    REGIONS("Regions", "المناطق"),
    POI("Points of Interest", "نقاط الاهتمام"),
    QUESTS("Quests", "المهام"),
    MARKERS("Player Markers", "علامات اللاعب")
}

// ════════════════════════════════════════════════════════════════════════════════════
// Main Map Screen - الشاشة الرئيسية للخريطة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شاشة الخريطة الرئيسية
 * Main Map Screen
 * 
 * @param mapState حالة الخريطة
 * @param onFastTravel عند السفر السريع
 * @param onAddMarker عند إضافة علامة
 * @param onRemoveMarker عند حذف علامة
 * @param onClose عند الإغلاق
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MapScreen(
    mapState: MapState,
    onFastTravel: (String) -> Unit,
    onAddMarker: (Offset, String, Color) -> Unit,
    onRemoveMarker: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Zoom & Pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // UI state
    var selectedPOI by remember { mutableStateOf<MapPOI?>(null) }
    var selectedFastTravelPoint by remember { mutableStateOf<FastTravelPoint?>(null) }
    var showLayersMenu by remember { mutableStateOf(false) }
    var showFastTravelDialog by remember { mutableStateOf(false) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var markerPosition by remember { mutableStateOf(Offset.Zero) }
    
    // Layer visibility
    val enabledLayers = remember { mutableStateMapOf<MapLayer, Boolean>().apply {
        MapLayer.values().forEach { put(it, true) }
    } }
    
    // Map bounds
    var mapSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ErytColor.VoidPrimary.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            MapHeader(
                explorationPercentage = mapState.explorationPercentage,
                currentRegion = mapState.regions.find { it.id == mapState.currentRegionId },
                onToggleLayers = { showLayersMenu = !showLayersMenu },
                onClose = onClose,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Main Map Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    MapCanvas(
                        mapState = mapState,
                        scale = scale,
                        offset = offset,
                        enabledLayers = enabledLayers,
                        onScaleChange = { newScale -> 
                            scale = newScale.coerceIn(0.5f, 3f) 
                        },
                        onOffsetChange = { newOffset -> offset = newOffset },
                        onPOIClick = { poi ->
                            selectedPOI = poi
                            scope.launch {
                                AudioEngine.playSFX("ui_select")
                            }
                        },
                        onFastTravelClick = { point ->
                            selectedFastTravelPoint = point
                            showFastTravelDialog = true
                        },
                        onMapLongPress = { position ->
                            markerPosition = position
                            showAddMarkerDialog = true
                        },
                        onSizeChanged = { size -> mapSize = size },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Zoom Controls
                    ZoomControls(
                        currentZoom = scale,
                        onZoomIn = { scale = (scale + 0.25f).coerceAtMost(3f) },
                        onZoomOut = { scale = (scale - 0.25f).coerceAtLeast(0.5f) },
                        onResetZoom = { 
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )

                    // Mini Map
                    MiniMap(
                        mapState = mapState,
                        currentViewport = calculateViewport(mapSize, scale, offset),
                        onViewportClick = { viewportOffset ->
                            offset = viewportOffset
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(150.dp)
                    )
                }

                // Side Panel - POI Details / Legend
                AnimatedVisibility(
                    visible = selectedPOI != null,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                ) {
                    selectedPOI?.let { poi ->
                        POIDetailsPanel(
                            poi = poi,
                            onClose = { selectedPOI = null },
                            onNavigate = {
                                // TODO: Set navigation waypoint
                                scope.launch {
                                    AudioEngine.playSFX("ui_confirm")
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Bottom Stats Bar
            MapStatsBar(
                discoveredRegions = mapState.discoveredRegions.size,
                totalRegions = mapState.regions.size,
                discoveredPOIs = mapState.pointsOfInterest.count { it.isDiscovered },
                totalPOIs = mapState.pointsOfInterest.size,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Layers Menu
        if (showLayersMenu) {
            LayersMenu(
                enabledLayers = enabledLayers,
                onToggleLayer = { layer ->
                    enabledLayers[layer] = !(enabledLayers[layer] ?: true)
                },
                onClose = { showLayersMenu = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 16.dp)
            )
        }

        // Fast Travel Dialog
        if (showFastTravelDialog && selectedFastTravelPoint != null) {
            FastTravelDialog(
                point = selectedFastTravelPoint!!,
                onConfirm = {
                    onFastTravel(selectedFastTravelPoint!!.id)
                    showFastTravelDialog = false
                    selectedFastTravelPoint = null
                    scope.launch {
                        AudioEngine.playSFX("fast_travel")
                        EventBus.emit(GameEvent.World.FastTravel(selectedFastTravelPoint!!.id))
                    }
                },
                onDismiss = {
                    showFastTravelDialog = false
                    selectedFastTravelPoint = null
                }
            )
        }

        // Add Marker Dialog
        if (showAddMarkerDialog) {
            AddMarkerDialog(
                onConfirm = { label, color ->
                    onAddMarker(markerPosition, label, color)
                    showAddMarkerDialog = false
                    scope.launch {
                        AudioEngine.playSFX("ui_confirm")
                    }
                },
                onDismiss = { showAddMarkerDialog = false }
            )
        }
    }
}

/**
 * حساب منطقة العرض الحالية
 * Calculate Current Viewport
 */
private fun calculateViewport(mapSize: Size, scale: Float, offset: Offset): Rect {
    val viewportWidth = mapSize.width / scale
    val viewportHeight = mapSize.height / scale
    val viewportX = -offset.x / scale
    val viewportY = -offset.y / scale
    
    return Rect(
        left = viewportX,
        top = viewportY,
        right = viewportX + viewportWidth,
        bottom = viewportY + viewportHeight
    )
}

// ════════════════════════════════════════════════════════════════════════════════════
// Map Header - ترويسة الخريطة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * ترويسة شاشة الخريطة
 * Map Screen Header
 */
@Composable
private fun MapHeader(
    explorationPercentage: Float,
    currentRegion: MapRegion?,
    onToggleLayers: () -> Unit,
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
            // Title & Current Region
            Column {
                Text(
                    text = "World Map",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )
                currentRegion?.let {
                    Text(
                        text = "${it.nameAr} | ${it.nameEn}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = it.difficulty.color
                    )
                }
            }

            // Exploration Progress
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${(explorationPercentage * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = ErytColor.MemoryAqua,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الاستكشاف | Exploration",
                    style = MaterialTheme.typography.labelSmall,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                )
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ErytButton(
                    onClick = onToggleLayers,
                    variant = ErytButtonVariant.SECONDARY,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Toggle Layers"
                    )
                }

                ErytButton(
                    onClick = onClose,
                    variant = ErytButtonVariant.SECONDARY,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Map"
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Map Canvas - لوحة رسم الخريطة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * لوحة رسم الخريطة التفاعلية
 * Interactive Map Canvas
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapCanvas(
    mapState: MapState,
    scale: Float,
    offset: Offset,
    enabledLayers: Map<MapLayer, Boolean>,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    onPOIClick: (MapPOI) -> Unit,
    onFastTravelClick: (FastTravelPoint) -> Unit,
    onMapLongPress: (Offset) -> Unit,
    onSizeChanged: (Size) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0E1A))
            .onSizeChanged { size -> 
                canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                onSizeChanged(canvasSize)
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onScaleChange(scale * zoom)
                    onOffsetChange(offset + pan)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { tapOffset ->
                        val mapPosition = screenToMapCoordinates(tapOffset, scale, offset)
                        onMapLongPress(mapPosition)
                    },
                    onTap = { tapOffset ->
                        val mapPosition = screenToMapCoordinates(tapOffset, scale, offset)
                        
                        // Check POI clicks
                        mapState.pointsOfInterest.forEach { poi ->
                            if ((poi.position - mapPosition).getDistance() < 20f) {
                                onPOIClick(poi)
                                return@detectTapGestures
                            }
                        }
                        
                        // Check Fast Travel clicks
                        mapState.fastTravelPoints.forEach { point ->
                            if (point.isUnlocked && (point.position - mapPosition).getDistance() < 20f) {
                                onFastTravelClick(point)
                                return@detectTapGestures
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            // Draw regions
            if (enabledLayers[MapLayer.REGIONS] == true) {
                mapState.regions.forEach { region ->
                    drawRegion(region)
                }
            }

            // Draw fog of war
            mapState.regions.filter { !it.isDiscovered }.forEach { region ->
                drawFogOfWar(region.bounds)
            }

            // Draw POIs
            if (enabledLayers[MapLayer.POI] == true) {
                mapState.pointsOfInterest.filter { it.isDiscovered }.forEach { poi ->
                    drawPOI(poi, canvasSize)
                }
            }

            // Draw quests
            if (enabledLayers[MapLayer.QUESTS] == true) {
                mapState.activeQuests.forEach { quest ->
                    drawQuest(quest, canvasSize)
                }
            }

            // Draw player markers
            if (enabledLayers[MapLayer.MARKERS] == true) {
                mapState.playerMarkers.forEach { marker ->
                    drawPlayerMarker(marker, canvasSize)
                }
            }

            // Draw fast travel points
            mapState.fastTravelPoints.filter { it.isUnlocked }.forEach { point ->
                drawFastTravelPoint(point, canvasSize)
            }

            // Draw player position
            drawPlayerPosition(mapState.playerPosition)
        }
    }
}

/**
 * تحويل إحداثيات الشاشة إلى إحداثيات الخريطة
 * Convert Screen to Map Coordinates
 */
private fun screenToMapCoordinates(screenPos: Offset, scale: Float, offset: Offset): Offset {
    return Offset(
        x = (screenPos.x - offset.x) / scale,
        y = (screenPos.y - offset.y) / scale
    )
}

/**
 * رسم منطقة
 * Draw Region
 */
private fun DrawScope.drawRegion(region: MapRegion) {
    drawRect(
        color = if (region.isDiscovered) {
            region.backgroundColor.copy(alpha = 0.3f)
        } else {
            Color.Black.copy(alpha = 0.7f)
        },
        topLeft = region.bounds.topLeft,
        size = region.bounds.size
    )

    if (region.isDiscovered) {
        drawRect(
            color = region.borderColor,
            topLeft = region.bounds.topLeft,
            size = region.bounds.size,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw region name
        // Note: Text drawing in Canvas is limited, ideally use AndroidView for text
    }
}

/**
 * رسم ضباب الحرب
 * Draw Fog of War
 */
private fun DrawScope.drawFogOfWar(bounds: Rect) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.9f),
                Color.Black.copy(alpha = 0.7f)
            ),
            center = bounds.center
        ),
        topLeft = bounds.topLeft,
        size = bounds.size
    )
}

/**
 * رسم نقطة اهتمام
 * Draw Point of Interest
 */
private fun DrawScope.drawPOI(poi: MapPOI, canvasSize: Size) {
    val position = Offset(
        x = poi.position.x * canvasSize.width,
        y = poi.position.y * canvasSize.height
    )

    // Icon background
    drawCircle(
        color = poi.type.color.copy(alpha = 0.3f),
        radius = 20.dp.toPx(),
        center = position
    )

    // Icon border
    drawCircle(
        color = poi.type.color,
        radius = 15.dp.toPx(),
        center = position,
        style = Stroke(width = 2.dp.toPx())
    )

    // Completion indicator
    if (poi.isCompleted) {
        drawCircle(
            color = Color(0xFF2ECC71),
            radius = 5.dp.toPx(),
            center = position + Offset(10.dp.toPx(), -10.dp.toPx())
        )
    }
}

/**
 * رسم مهمة
 * Draw Quest
 */
private fun DrawScope.drawQuest(quest: MapQuest, canvasSize: Size) {
    val position = Offset(
        x = quest.position.x * canvasSize.width,
        y = quest.position.y * canvasSize.height
    )

    val color = if (quest.isMainQuest) ErytColor.BlightGold else Color(0xFF3498DB)

    // Quest marker
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = 25.dp.toPx(),
        center = position
    )

    drawCircle(
        color = color,
        radius = 18.dp.toPx(),
        center = position,
        style = Stroke(width = 3.dp.toPx())
    )

    // Exclamation mark (simplified)
    drawCircle(
        color = color,
        radius = 8.dp.toPx(),
        center = position
    )
}

/**
 * رسم علامة اللاعب
 * Draw Player Marker
 */
private fun DrawScope.drawPlayerMarker(marker: PlayerMarker, canvasSize: Size) {
    val position = Offset(
        x = marker.position.x * canvasSize.width,
        y = marker.position.y * canvasSize.height
    )

    drawCircle(
        color = marker.color,
        radius = 12.dp.toPx(),
        center = position
    )

    drawCircle(
        color = Color.White,
        radius = 8.dp.toPx(),
        center = position
    )
}

/**
 * رسم نقطة سفر سريع
 * Draw Fast Travel Point
 */
private fun DrawScope.drawFastTravelPoint(point: FastTravelPoint, canvasSize: Size) {
    val position = Offset(
        x = point.position.x * canvasSize.width,
        y = point.position.y * canvasSize.height
    )

    // Pulsing effect (simplified, ideally animated)
    drawCircle(
        color = Color(0xFF9B59B6).copy(alpha = 0.3f),
        radius = 30.dp.toPx(),
        center = position
    )

    drawCircle(
        color = Color(0xFF9B59B6),
        radius = 18.dp.toPx(),
        center = position
    )

    drawCircle(
        color = Color.White,
        radius = 12.dp.toPx(),
        center = position
    )
}

/**
 * رسم موقع اللاعب
 * Draw Player Position
 */
private fun DrawScope.drawPlayerPosition(position: Offset) {
    // Outer glow
    drawCircle(
        color = ErytColor.RadianceWhite.copy(alpha = 0.3f),
        radius = 25.dp.toPx(),
        center = position
    )

    // Player marker
    drawCircle(
        color = ErytColor.RadianceWhite,
        radius = 12.dp.toPx(),
        center = position
    )

    drawCircle(
        color = ErytColor.BlightGold,
        radius = 8.dp.toPx(),
        center = position
    )

    // Direction indicator (arrow pointing up)
    val arrowSize = 6.dp.toPx()
    drawCircle(
        color = Color.Black,
        radius = 3.dp.toPx(),
        center = position + Offset(0f, -arrowSize)
    )
}

// ════════════════════════════════════════════════════════════════════════════════════
// Zoom Controls - أدوات التكبير
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * أدوات التحكم بالتكبير
 * Zoom Control Buttons
 */
@Composable
private fun ZoomControls(
    currentZoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ErytButton(
            onClick = onZoomIn,
            variant = ErytButtonVariant.SECONDARY,
            enabled = currentZoom < 3f,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In"
            )
        }

        Text(
            text = "${(currentZoom * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = ErytColor.RadianceWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(48.dp)
                .background(ErytColor.SurfaceDark.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp)
        )

        ErytButton(
            onClick = onZoomOut,
            variant = ErytButtonVariant.SECONDARY,
            enabled = currentZoom > 0.5f,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Zoom Out"
            )
        }

        ErytButton(
            onClick = onResetZoom,
            variant = ErytButtonVariant.SECONDARY,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Zoom"
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Mini Map - الخريطة المصغرة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * الخريطة المصغرة
 * Mini Map Component
 */
@Composable
private fun MiniMap(
    mapState: MapState,
    currentViewport: Rect,
    onViewportClick: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    ErytPanel(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val normalizedPos = Offset(
                            x = tapOffset.x / size.width,
                            y = tapOffset.y / size.height
                        )
                        onViewportClick(normalizedPos)
                    }
                }
        ) {
            // Draw simplified regions
            mapState.regions.forEach { region ->
                if (region.isDiscovered) {
                    drawRect(
                        color = region.borderColor.copy(alpha = 0.5f),
                        topLeft = Offset(
                            x = region.bounds.left * size.width,
                            y = region.bounds.top * size.height
                        ),
                        size = Size(
                            width = region.bounds.width * size.width,
                            height = region.bounds.height * size.height
                        )
                    )
                }
            }

            // Draw player position
            drawCircle(
                color = ErytColor.BlightGold,
                radius = 4.dp.toPx(),
                center = Offset(
                    x = mapState.playerPosition.x * size.width,
                    y = mapState.playerPosition.y * size.height
                )
            )

            // Draw viewport rectangle
            drawRect(
                color = ErytColor.RadianceWhite,
                topLeft = Offset(
                    x = currentViewport.left * size.width,
                    y = currentViewport.top * size.height
                ),
                size = Size(
                    width = currentViewport.width * size.width,
                    height = currentViewport.height * size.height
                ),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// POI Details Panel - لوحة تفاصيل نقطة الاهتمام
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * لوحة تفاصيل نقطة الاهتمام
 * POI Details Panel
 */
@Composable
private fun POIDetailsPanel(
    poi: MapPOI,
    onClose: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErytPanel(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = poi.type.icon,
                    contentDescription = null,
                    tint = poi.type.color,
                    modifier = Modifier.size(32.dp)
                )

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ErytColor.RadianceWhite
                    )
                }
            }

            // POI Name
            Column {
                Text(
                    text = poi.nameAr,
                    style = MaterialTheme.typography.titleLarge,
                    color = poi.type.color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = poi.nameEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                )
            }

            // Type Badge
            Text(
                text = poi.type.nameAr,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .background(poi.type.color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .border(1.dp, poi.type.color, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Divider(color = ErytColor.OutlineGray)

            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الحالة:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                )
                Text(
                    text = if (poi.isCompleted) "✅ مكتمل" else "🔄 نشط",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (poi.isCompleted) Color(0xFF2ECC71) else ErytColor.MemoryAqua,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigate Button
            ErytButton(
                onClick = onNavigate,
                variant = ErytButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null
                    )
                    Text("تعيين كهدف | Set as Waypoint")
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Map Stats Bar - شريط إحصائيات الخريطة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شريط إحصائيات الخريطة
 * Map Statistics Bar
 */
@Composable
private fun MapStatsBar(
    discoveredRegions: Int,
    totalRegions: Int,
    discoveredPOIs: Int,
    totalPOIs: Int,
    modifier: Modifier = Modifier
) {
    ErytPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Default.LocationOn,
                label = "المناطق",
                value = "$discoveredRegions / $totalRegions",
                color = ErytColor.MemoryAqua
            )

            StatItem(
                icon = Icons.Default.Star,
                label = "نقاط الاهتمام",
                value = "$discoveredPOIs / $totalPOIs",
                color = ErytColor.BlightGold
            )
        }
    }
}

/**
 * عنصر إحصائية واحد
 * Single Stat Item
 */
@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Layers Menu - قائمة الطبقات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * قائمة التحكم بطبقات الخريطة
 * Map Layers Control Menu
 */
@Composable
private fun LayersMenu(
    enabledLayers: Map<MapLayer, Boolean>,
    onToggleLayer: (MapLayer) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErytPanel(
        modifier = modifier
            .width(250.dp)
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "طبقات الخريطة",
                    style = MaterialTheme.typography.titleMedium,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ErytColor.RadianceWhite
                    )
                }
            }

            Divider(color = ErytColor.OutlineGray)

            MapLayer.values().forEach { layer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleLayer(layer) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = layer.nameAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErytColor.RadianceWhite
                    )
                    Switch(
                        checked = enabledLayers[layer] ?: true,
                        onCheckedChange = { onToggleLayer(layer) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ErytColor.BlightGold,
                            checkedTrackColor = ErytColor.BlightGold.copy(alpha = 0.5f)
                        )
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
 * حوار السفر السريع
 * Fast Travel Dialog
 */
@Composable
private fun FastTravelDialog(
    point: FastTravelPoint,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFF9B59B6),
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "سفر سريع",
                            style = MaterialTheme.typography.titleLarge,
                            color = ErytColor.RadianceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fast Travel",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                        )
                    }
                }

                Divider(color = ErytColor.OutlineGray)

                Column {
                    Text(
                        text = point.nameAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF9B59B6),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = point.nameEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                }

                if (point.cost > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                tint = ErytColor.BlightGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = point.cost.toString(),
                                style = MaterialTheme.typography.bodyMedium,
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
                        Text("سفر")
                    }
                }
            }
        }
    }
}

/**
 * حوار إضافة علامة
 * Add Marker Dialog
 */
@Composable
private fun AddMarkerDialog(
    onConfirm: (String, Color) -> Unit,
    onDismiss: () -> Unit
) {
    var markerLabel by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ErytColor.BlightGold) }

    val markerColors = listOf(
        ErytColor.BlightGold,
        ErytColor.MemoryAqua,
        Color(0xFFE74C3C),
        Color(0xFF2ECC71),
        Color(0xFF9B59B6),
        Color(0xFFF39C12)
    )

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
                    text = "إضافة علامة | Add Marker",
                    style = MaterialTheme.typography.titleLarge,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )

                // Label Input
                OutlinedTextField(
                    value = markerLabel,
                    onValueChange = { markerLabel = it },
                    label = { Text("التسمية | Label") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ErytColor.BlightGold,
                        focusedLabelColor = ErytColor.BlightGold,
                        cursorColor = ErytColor.BlightGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اللون | Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        markerColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColor == color) 3.dp else 0.dp,
                                        color = ErytColor.RadianceWhite,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
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
                        onClick = { onConfirm(markerLabel.ifBlank { "علامة" }, selectedColor) },
                        variant = ErytButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إضافة")
                    }
                }
            }
        }
    }
}