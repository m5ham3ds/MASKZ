package com.erygra.maskoflight.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.engine.ParticleType
import com.erygra.maskoflight.player.AbilityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * World Map System — Mask of Light (Erygra Universe)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * نظام خرائط متقدم مع:
 * - نظام Fog of War (اكتشاف تدريجي)
 * - خرائط قابلة للشراء (Map Overlays)
 * - 7 بائعين للخرائط موزعين على المناطق
 * - نظام Map Shards (5 شظايا = خريطة كاملة)
 * - Mini-map في HUD
 * - Full map screen مع Zoom/Pan
 * - دبابيس وعلامات (Pins & Markers)
 * - Fast Travel من الخريطة
 * - تكامل مع RegionManager
 *
 * الخرائط:
 * - Overlay A: معالم رئيسية (موانئ، بوابات، بائعي خرائط، ملاجئ)
 * - Overlay B: طرق ومخاطر (ممرات، مناطق خطرة، مواقع زعماء)
 * - Full Map: كشف كامل (5 Map Shards أو 1500 Coins)
 *
 * @author Erygra Development Team
 * @version 2.0.0
 * @since 2025-01-09
 */

// ═══════════════════════════════════════════════════════════════════════════════
// Map Enums
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مستويات الخريطة
 */
enum class MapLevel {
    NONE,           // لا خريطة
    OVERLAY_A,      // معالم رئيسية
    OVERLAY_B,      // طرق ومخاطر
    FULL            // كشف كامل
}

/**
 * أنواع العلامات على الخريطة
 */
enum class MapMarkerType {
    // Player & Progress:
    PLAYER_POSITION,        // موقع اللاعب
    DEATH_LOCATION,         // مكان الموت الأخير
    OBJECTIVE_ACTIVE,       // هدف نشط
    OBJECTIVE_COMPLETED,    // هدف مكتمل
    
    // Locations:
    SANCTUARY,              // ملجأ
    SANCTUARY_HIDDEN,       // ملجأ مخفي
    MAP_VENDOR,             // بائع خرائط
    SHOP,                   // متجر
    NPC,                    // شخصية
    
    // Enemies:
    BOSS,                   // زعيم
    MINIBOSS,               // زعيم صغير
    ELITE_ENEMY,            // عدو نخبة
    ENEMY_SPAWN,            // نقطة ظهور أعداء
    
    // Collectibles:
    MEMORY_FRAGMENT,        // شظية ذاكرة
    LORE_OBJECT,            // شيء قصصي
    SECRET,                 // سر
    TREASURE,               // كنز
    
    // Environment:
    HAZARD,                 // خطر
    LOCKED_DOOR,            // باب مقفل
    BRIDGE,                 // جسر
    TELEPORTER,             // انتقال فوري
    
    // Transport:
    FERRY,                  // عبارة
    GONDOLA,                // جندولا
    AIRSHIP,                // سفينة هوائية
    TUNNEL,                 // نفق
    
    // Custom:
    CUSTOM_PIN              // دبوس مخصص من اللاعب
}

/**
 * فئات العلامات
 */
enum class MapMarkerCategory {
    IMPORTANT,      // مهم (دائماً مرئي)
    COLLECTIBLE,    // قابل للجمع
    ENEMY,          // عدو
    LOCATION,       // موقع
    TRANSPORT,      // نقل
    CUSTOM          // مخصص
}

/**
 * أنواع الأيقونات
 */
enum class MapIconType {
    CIRCLE, SQUARE, TRIANGLE, DIAMOND, STAR,
    CROSS, SKULL, CHEST, SCROLL, POTION,
    SWORD, SHIELD, KEY, LOCK, ANCHOR,
    BOAT, BRIDGE, TOWER, HOUSE, TREE
}

/**
 * مستويات التكبير
 */
enum class ZoomLevel(val scale: Float) {
    VERY_CLOSE(4.0f),
    CLOSE(2.5f),
    NORMAL(1.5f),
    FAR(1.0f),
    VERY_FAR(0.6f),
    OVERVIEW(0.3f)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Map Data Classes
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * بلاطة الخريطة (Tile)
 */
data class MapTile(
    val x: Int,
    val y: Int,
    val region: RegionType,
    val isDiscovered: Boolean = false,
    val isVisible: Boolean = false,       // مرئي في Fog of War
    val terrainType: TerrainType = TerrainType.GROUND,
    val elevation: Int = 0,               // الارتفاع (للطبقات)
    val isWalkable: Boolean = true,
    val hasHazard: Boolean = false,
    val biome: String = ""
) {
    val id: String = "${region.name}_${x}_${y}"
}

/**
 * أنواع التضاريس
 */
enum class TerrainType {
    GROUND, PLATFORM, LADDER, ROPE, WATER,
    LAVA, ICE, MUD, GRASS, STONE, METAL,
    WOOD, GLASS, VOID
}

/**
 * علامة على الخريطة
 */
data class MapMarker(
    val id: String,
    val type: MapMarkerType,
    val category: MapMarkerCategory,
    val position: Offset,                  // موقع عالمي (x, y)
    val region: RegionType,
    val name: String = "",
    val nameArabic: String = "",
    val description: String = "",
    val iconType: MapIconType = MapIconType.CIRCLE,
    val iconColor: Color = Color.White,
    val isDiscovered: Boolean = false,
    val isCompleted: Boolean = false,      // للمهام/الجمع
    val isPermanent: Boolean = true,       // يبقى بعد الجمع؟
    val linkedQuestId: String? = null,
    val linkedNpcId: String? = null,
    val linkedEnemyId: String? = null,
    val customData: Map<String, Any> = emptyMap()
)

/**
 * دبوس مخصص من اللاعب
 */
data class CustomPin(
    val id: String,
    val position: Offset,
    val region: RegionType,
    val iconType: MapIconType,
    val iconColor: Color,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * بائع خرائط
 */
data class MapVendor(
    val id: String,
    val name: String,
    val nameArabic: String,
    val region: RegionType,
    val position: Offset,
    val overlayAPrice: Int = 100,          // سعر Overlay A
    val overlayBPrice: Int = 100,          // سعر Overlay B
    val fullMapPrice: Int = 1500,          // سعر Full Map
    val mapShardPrice: Int = 100,          // سعر Map Shard
    val dialogue: String = "",
    val isDiscovered: Boolean = false
)

/**
 * شظية خريطة
 */
data class MapShard(
    val id: String,
    val region: RegionType,
    val foundAt: Long = System.currentTimeMillis()
)

/**
 * حالة الخريطة لمنطقة
 */
data class RegionMapState(
    val region: RegionType,
    val mapLevel: MapLevel = MapLevel.NONE,
    val discoveredTiles: Set<String> = emptySet(),     // IDs البلاطات المكتشفة
    val totalTiles: Int = 0,
    val mapShards: Int = 0,                             // عدد الشظايا المجموعة
    val overlayAPurchased: Boolean = false,
    val overlayBPurchased: Boolean = false,
    val fullMapPurchased: Boolean = false,
    val markers: List<MapMarker> = emptyList(),
    val customPins: List<CustomPin> = emptyList()
) {
    /**
     * نسبة الاكتشاف (0.0-1.0)
     */
    val discoveryPercentage: Float
        get() = if (totalTiles > 0) discoveredTiles.size.toFloat() / totalTiles else 0f
    
    /**
     * هل الخريطة الكاملة متاحة؟
     */
    val isFullMapAvailable: Boolean
        get() = fullMapPurchased || mapShards >= 5
}

/**
 * إعدادات الخريطة الكاملة (UI)
 */
data class MapViewSettings(
    val zoomLevel: ZoomLevel = ZoomLevel.NORMAL,
    val centerPosition: Offset = Offset.Zero,
    val showPlayerPosition: Boolean = true,
    val showMarkers: Boolean = true,
    val showCustomPins: Boolean = true,
    val showGrid: Boolean = false,
    val showCoordinates: Boolean = false,
    val showRegionBorders: Boolean = true,
    val showFogOfWar: Boolean = true,
    val markerFilters: Set<MapMarkerCategory> = MapMarkerCategory.values().toSet(),
    val highlightedMarkerId: String? = null
)

/**
 * حالة نظام الخرائط (للحفظ/التحميل)
 */
data class WorldMapState(
    val regionMaps: Map<RegionType, RegionMapState>,
    val totalMapShards: Int,
    val customPins: List<CustomPin>,
    val discoveredVendors: Set<String>,
    val viewSettings: MapViewSettings
)

// ═══════════════════════════════════════════════════════════════════════════════
// Map Vendor Database
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات بائعي الخرائط
 */
object MapVendorDatabase {
    
    val vendors = listOf(
        
        // Ashen Sprawl
        MapVendor(
            id = "marrek_chartkeeper",
            name = "Marrek the Chartkeeper",
            nameArabic = "ماريك حافظ الخرائط",
            region = RegionType.ASHEN_SPRAWL,
            position = Offset(34f, 56f),       // Port of Ash coordinates (normalized)
            overlayAPrice = 100,
            overlayBPrice = 100,
            fullMapPrice = 1500,
            mapShardPrice = 100,
            dialogue = "Maps? In a city that forgets its own streets? Brave. Or foolish."
        ),
        
        // Veiled Archives
        MapVendor(
            id = "lysa_ledgers",
            name = "Lysa of Ledgers",
            nameArabic = "ليسا من السجلات",
            region = RegionType.VEILED_ARCHIVES,
            position = Offset(88f, 32f),       // Vault Wharf
            overlayAPrice = 100,
            overlayBPrice = 100,
            fullMapPrice = 1500,
            mapShardPrice = 100,
            dialogue = "Every page tells a story. Every map, a path forgotten."
        ),
        
        // Hollowed Archipelago
        MapVendor(
            id = "harbor_ledger",
            name = "Harbor Ledger",
            nameArabic = "سجل المرفأ",
            region = RegionType.HOLLOWED_ARCHIPELAGO,
            position = Offset(23f, 68f),       // Harbor Reach
            overlayAPrice = 100,
            overlayBPrice = 100,
            fullMapPrice = 1500,
            mapShardPrice = 100,
            dialogue = "Wind changes the islands daily. My maps keep up."
        ),
        
        // Glassfjord Cliffs
        MapVendor(
            id = "shardcart_seller",
            name = "Shardcart Seller",
            nameArabic = "بائع عربة الشظايا",
            region = RegionType.GLASSFJORD_CLIFFS,
            position = Offset(156f, 48f),      // Frozen Basin
            overlayAPrice = 100,
            overlayBPrice = 100,
            fullMapPrice = 1500,
            mapShardPrice = 100,
            dialogue = "Glass reflects many paths. Choose carefully."
        ),
        
        // Sunken Clockworks
        MapVendor(
            id = "cogscribe",
            name = "Cogscribe",
            nameArabic = "كاتب التروس",
            region = RegionType.SUNKEN_CLOCKWORKS,
            position = Offset(220f, 88f),      // Canal Platform
            overlayAPrice = 100,
            overlayBPrice = 100,
            fullMapPrice = 1500,
            mapShardPrice = 100,
            dialogue = "Time flows like water here. My maps track both."
        ),
        
        // Blackroot Moorlands
        MapVendor(
            id = "root_map_hermit",
            name = "Root-Map Hermit",
            nameArabic = "ناسك خريطة الجذور",
            region = RegionType.BLACKROOT_MOORLANDS,
            position = Offset(274f, 136f),     // Bog Pool
            overlayAPrice = 100,
            overlayBPrice = 100,
            fullMapPrice = 1500,
            mapShardPrice = 100,
            dialogue = "The roots remember what the land forgot."
        ),
        
        // Luminous Chasm
        MapVendor(
            id = "biolume_cartographer",
            name = "Biolume Cartographer",
            nameArabic = "رسام الخرائط المضيء",
            region = RegionType.LUMINOUS_CHASM,
            position = Offset(325f, 200f),     // Glow Pond
            overlayAPrice = 100,
            overlayBPrice = 100,
            fullMapPrice = 1500,
            mapShardPrice = 100,
            dialogue = "In the void, only light shows the way. My maps are that light."
        )
    )
    
    /**
     * الحصول على بائع بالمعرّف
     */
    fun getVendor(id: String): MapVendor? = vendors.firstOrNull { it.id == id }
    
    /**
     * الحصول على بائعي منطقة
     */
    fun getVendorsInRegion(region: RegionType): List<MapVendor> =
        vendors.filter { it.region == region }
    
    /**
     * الحصول على جميع البائعين
     */
    fun getAllVendors(): List<MapVendor> = vendors
}

// ═══════════════════════════════════════════════════════════════════════════════
// Map Marker Presets
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * علامات محددة مسبقاً لكل منطقة
 */
object MapMarkerPresets {
    
    /**
     * إنشاء علامات افتراضية لمنطقة
     */
    fun createDefaultMarkersForRegion(region: RegionType): List<MapMarker> {
        val markers = mutableListOf<MapMarker>()
        
        when (region) {
            RegionType.ASHEN_SPRAWL -> {
                // Sanctuaries
                markers.add(MapMarker(
                    id = "ashen_sanctuary_1",
                    type = MapMarkerType.SANCTUARY,
                    category = MapMarkerCategory.IMPORTANT,
                    position = Offset(20f, 40f),
                    region = region,
                    name = "Port Beacon",
                    nameArabic = "منارة الميناء",
                    iconType = MapIconType.TOWER,
                    iconColor = Color(0xFFFFD54F)
                ))
                
                markers.add(MapMarker(
                    id = "ashen_sanctuary_2",
                    type = MapMarkerType.SANCTUARY,
                    category = MapMarkerCategory.IMPORTANT,
                    position = Offset(60f, 70f),
                    region = region,
                    name = "Forge Quarter Shrine",
                    nameArabic = "ضريح حي الحدادة",
                    iconType = MapIconType.TOWER,
                    iconColor = Color(0xFFFFD54F)
                ))
                
                // Map Vendor
                markers.add(MapMarker(
                    id = "marrek_vendor_marker",
                    type = MapMarkerType.MAP_VENDOR,
                    category = MapMarkerCategory.LOCATION,
                    position = Offset(34f, 56f),
                    region = region,
                    name = "Marrek the Chartkeeper",
                    nameArabic = "ماريك حافظ الخرائط",
                    iconType = MapIconType.SCROLL,
                    iconColor = Color(0xFF90CAF9)
                ))
                
                // Boss
                markers.add(MapMarker(
                    id = "pyre_harrow_boss",
                    type = MapMarkerType.BOSS,
                    category = MapMarkerCategory.ENEMY,
                    position = Offset(100f, 80f),
                    region = region,
                    name = "Pyre Harrow",
                    nameArabic = "حارث النار",
                    iconType = MapIconType.SKULL,
                    iconColor = Color(0xFFFF5722)
                ))
                
                // Shops
                markers.add(MapMarker(
                    id = "ashen_shop_1",
                    type = MapMarkerType.SHOP,
                    category = MapMarkerCategory.LOCATION,
                    position = Offset(35f, 55f),
                    region = region,
                    name = "Port of Ash Shop",
                    nameArabic = "متجر ميناء الرماد",
                    iconType = MapIconType.CHEST,
                    iconColor = Color(0xFFFDD835)
                ))
            }
            
            RegionType.VEILED_ARCHIVES -> {
                // Sanctuaries
                markers.add(MapMarker(
                    id = "archive_sanctuary_1",
                    type = MapMarkerType.SANCTUARY,
                    category = MapMarkerCategory.IMPORTANT,
                    position = Offset(30f, 50f),
                    region = region,
                    name = "Reading Alcove Sanctuary",
                    nameArabic = "ملاذ حجرة القراءة",
                    iconType = MapIconType.TOWER,
                    iconColor = Color(0xFFFFD54F)
                ))
                
                // Map Vendor
                markers.add(MapMarker(
                    id = "lysa_vendor_marker",
                    type = MapMarkerType.MAP_VENDOR,
                    category = MapMarkerCategory.LOCATION,
                    position = Offset(88f, 32f),
                    region = region,
                    name = "Lysa of Ledgers",
                    nameArabic = "ليسا من السجلات",
                    iconType = MapIconType.SCROLL,
                    iconColor = Color(0xFF90CAF9)
                ))
                
                // Boss
                markers.add(MapMarker(
                    id = "indexer_boss",
                    type = MapMarkerType.BOSS,
                    category = MapMarkerCategory.ENEMY,
                    position = Offset(150f, 90f),
                    region = region,
                    name = "The Indexer",
                    nameArabic = "المفهرس",
                    iconType = MapIconType.SKULL,
                    iconColor = Color(0xFF1976D2)
                ))
            }
            
            RegionType.HOLLOWED_ARCHIPELAGO -> {
                // Sanctuaries
                markers.add(MapMarker(
                    id = "archipelago_sanctuary_1",
                    type = MapMarkerType.SANCTUARY,
                    category = MapMarkerCategory.IMPORTANT,
                    position = Offset(40f, 30f),
                    region = region,
                    name = "Sky Platform Sanctuary",
                    nameArabic = "ملاذ منصة السماء",
                    iconType = MapIconType.TOWER,
                    iconColor = Color(0xFFFFD54F)
                ))
                
                // Map Vendor
                markers.add(MapMarker(
                    id = "harbor_ledger_marker",
                    type = MapMarkerType.MAP_VENDOR,
                    category = MapMarkerCategory.LOCATION,
                    position = Offset(23f, 68f),
                    region = region,
                    name = "Harbor Ledger",
                    nameArabic = "سجل المرفأ",
                    iconType = MapIconType.SCROLL,
                    iconColor = Color(0xFF90CAF9)
                ))
                
                // Boss
                markers.add(MapMarker(
                    id = "bridgemaster_boss",
                    type = MapMarkerType.BOSS,
                    category = MapMarkerCategory.ENEMY,
                    position = Offset(180f, 70f),
                    region = region,
                    name = "Bridgemaster",
                    nameArabic = "سيد الجسور",
                    iconType = MapIconType.SKULL,
                    iconColor = Color(0xFF0288D1)
                ))
                
                // Transport
                markers.add(MapMarker(
                    id = "archipelago_ferry_1",
                    type = MapMarkerType.FERRY,
                    category = MapMarkerCategory.TRANSPORT,
                    position = Offset(25f, 65f),
                    region = region,
                    name = "Harbor Ferry",
                    nameArabic = "عبارة المرفأ",
                    iconType = MapIconType.BOAT,
                    iconColor = Color(0xFF81D4FA)
                ))
            }
            
            RegionType.GLASSFJORD_CLIFFS -> {
                // Sanctuaries
                markers.add(MapMarker(
                    id = "glass_sanctuary_1",
                    type = MapMarkerType.SANCTUARY,
                    category = MapMarkerCategory.IMPORTANT,
                    position = Offset(50f, 60f),
                    region = region,
                    name = "Crystal Cavern Sanctuary",
                    nameArabic = "ملاذ كهف البلور",
                    iconType = MapIconType.TOWER,
                    iconColor = Color(0xFFFFD54F)
                ))
                
                // Map Vendor
                markers.add(MapMarker(
                    id = "shardcart_marker",
                    type = MapMarkerType.MAP_VENDOR,
                    category = MapMarkerCategory.LOCATION,
                    position = Offset(156f, 48f),
                    region = region,
                    name = "Shardcart Seller",
                    nameArabic = "بائع عربة الشظايا",
                    iconType = MapIconType.SCROLL,
                    iconColor = Color(0xFF90CAF9)
                ))
                
                // Boss
                markers.add(MapMarker(
                    id = "colossus_boss",
                    type = MapMarkerType.BOSS,
                    category = MapMarkerCategory.ENEMY,
                    position = Offset(200f, 80f),
                    region = region,
                    name = "Fractured Colossus",
                    nameArabic = "العملاق المتصدع",
                    iconType = MapIconType.SKULL,
                    iconColor = Color(0xFF00BCD4)
                ))
            }
            
            RegionType.SUNKEN_CLOCKWORKS -> {
                // Sanctuaries
                markers.add(MapMarker(
                    id = "clockwork_sanctuary_1",
                    type = MapMarkerType.SANCTUARY,
                    category = MapMarkerCategory.IMPORTANT,
                    position = Offset(70f, 100f),
                    region = region,
                    name = "Dry Chamber Sanctuary",
                    nameArabic = "ملاذ الغرفة الجافة",
                    iconType = MapIconType.TOWER,
                    iconColor = Color(0xFFFFD54F)
                ))
                
                // Map Vendor
                markers.add(MapMarker(
                    id = "cogscribe_marker",
                    type = MapMarkerType.MAP_VENDOR,
                    category = MapMarkerCategory.LOCATION,
                    position = Offset(220f, 88f),
                    region = region,
                    name = "Cogscribe",
                    nameArabic = "كاتب التروس",
                    iconType = MapIconType.SCROLL,
                    iconColor = Color(0xFF90CAF9)
                ))
                
                // Boss
                markers.add(MapMarker(
                    id = "gideon_remnant_boss",
                    type = MapMarkerType.BOSS,
                    category = MapMarkerCategory.ENEMY,
                    position = Offset(220f, 120f),
                    region = region,
                    name = "Gideon's Remnant",
                    nameArabic = "بقايا جدعون",
                    iconType = MapIconType.SKULL,
                    iconColor = Color(0xFF558B2F)
                ))
            }
            
            RegionType.BLACKROOT_MOORLANDS -> {
                // Sanctuaries
                markers.add(MapMarker(
                    id = "moor_sanctuary_1",
                    type = MapMarkerType.SANCTUARY,
                    category = MapMarkerCategory.IMPORTANT,
                    position = Offset(100f, 150f),
                    region = region,
                    name = "Root Hollow Sanctuary",
                    nameArabic = "ملاذ جوف الجذر",
                    iconType = MapIconType.TOWER,
                    iconColor = Color(0xFFFFD54F)
                ))
                
                // Map Vendor
                markers.add(MapMarker(
                    id = "hermit_marker",
                    type = MapMarkerType.MAP_VENDOR,
                    category = MapMarkerCategory.LOCATION,
                    position = Offset(274f, 136f),
                    region = region,
                    name = "Root-Map Hermit",
                    nameArabic = "ناسك خريطة الجذور",
                    iconType = MapIconType.SCROLL,
                    iconColor = Color(0xFF90CAF9)
                ))
                
                // Boss
                markers.add(MapMarker(
                    id = "root_titan_boss",
                    type = MapMarkerType.BOSS,
                    category = MapMarkerCategory.ENEMY,
                    position = Offset(270f, 160f),
                    region = region,
                    name = "Root Titan",
                    nameArabic = "عملاق الجذور",
                    iconType = MapIconType.SKULL,
                    iconColor = Color(0xFF4E342E)
                ))
            }
            
            RegionType.LUMINOUS_CHASM -> {
                // Sanctuaries
                markers.add(MapMarker(
                    id = "chasm_sanctuary_1",
                    type = MapMarkerType.SANCTUARY,
                    category = MapMarkerCategory.IMPORTANT,
                    position = Offset(150f, 200f),
                    region = region,
                    name = "Light Nexus Sanctuary",
                    nameArabic = "ملاذ نقطة الضوء",
                    iconType = MapIconType.TOWER,
                    iconColor = Color(0xFFFFD54F)
                ))
                
                // Map Vendor
                markers.add(MapMarker(
                    id = "biolume_marker",
                    type = MapMarkerType.MAP_VENDOR,
                    category = MapMarkerCategory.LOCATION,
                    position = Offset(325f, 200f),
                    region = region,
                    name = "Biolume Cartographer",
                    nameArabic = "رسام الخرائط المضيء",
                    iconType = MapIconType.SCROLL,
                    iconColor = Color(0xFF90CAF9)
                ))
                
                // Boss
                markers.add(MapMarker(
                    id = "luminar_host_boss",
                    type = MapMarkerType.BOSS,
                    category = MapMarkerCategory.ENEMY,
                    position = Offset(330f, 230f),
                    region = region,
                    name = "The Luminar Host",
                    nameArabic = "المضيف المتلألئ",
                    iconType = MapIconType.SKULL,
                    iconColor = Color(0xFFFDD835)
                ))
            }
        }
        
        return markers
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// World Map Manager
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * مدير الخرائط العالمية
 */
class WorldMapManager {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════
    
    private val _regionMaps = MutableStateFlow<Map<RegionType, RegionMapState>>(
        RegionType.values().associateWith { region ->
            RegionMapState(
                region = region,
                mapLevel = if (region == RegionType.ASHEN_SPRAWL) MapLevel.OVERLAY_A else MapLevel.NONE,
                totalTiles = calculateTotalTiles(region),
                markers = MapMarkerPresets.createDefaultMarkersForRegion(region)
            )
        }
    )
    val regionMaps: StateFlow<Map<RegionType, RegionMapState>> = _regionMaps.asStateFlow()
    
    private val _totalMapShards = MutableStateFlow(0)
    val totalMapShards: StateFlow<Int> = _totalMapShards.asStateFlow()
    
    private val _customPins = MutableStateFlow<List<CustomPin>>(emptyList())
    val customPins: StateFlow<List<CustomPin>> = _customPins.asStateFlow()
    
    private val _discoveredVendors = MutableStateFlow<Set<String>>(emptySet())
    val discoveredVendors: StateFlow<Set<String>> = _discoveredVendors.asStateFlow()
    
    private val _viewSettings = MutableStateFlow(MapViewSettings())
    val viewSettings: StateFlow<MapViewSettings> = _viewSettings.asStateFlow()
    
    // Discovery tracking
    private val discoveryRadius = 6f  // عدد البلاطات حول اللاعب
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Tile Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * حساب العدد الكلي للبلاطات في منطقة
     */
    private fun calculateTotalTiles(region: RegionType): Int {
        val regionData = RegionDatabase.getRegion(region) ?: return 0
        return regionData.widthTiles * regionData.heightTiles
    }
    
    /**
     * تحديث اكتشاف البلاطات بناءً على موقع اللاعب
     */
    fun updateDiscovery(playerX: Float, playerY: Float, region: RegionType) {
        val regionMap = _regionMaps.value[region] ?: return
        
        // حساب نطاق الاكتشاف
        val minX = (playerX - discoveryRadius).toInt()
        val maxX = (playerX + discoveryRadius).toInt()
        val minY = (playerY - discoveryRadius).toInt()
        val maxY = (playerY + discoveryRadius).toInt()
        
        val newDiscoveredTiles = mutableSetOf<String>()
        newDiscoveredTiles.addAll(regionMap.discoveredTiles)
        
        // اكتشاف البلاطات في النطاق
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val distance = sqrt((x - playerX) * (x - playerX) + (y - playerY) * (y - playerY))
                if (distance <= discoveryRadius) {
                    val tileId = "${region.name}_${x}_${y}"
                    newDiscoveredTiles.add(tileId)
                }
            }
        }
        
        // تحديث الحالة
        if (newDiscoveredTiles.size > regionMap.discoveredTiles.size) {
            _regionMaps.update { maps ->
                maps + (region to regionMap.copy(discoveredTiles = newDiscoveredTiles))
            }
        }
    }
    
    /**
     * التحقق من اكتشاف بلاطة
     */
    fun isTileDiscovered(x: Int, y: Int, region: RegionType): Boolean {
        val tileId = "${region.name}_${x}_${y}"
        return _regionMaps.value[region]?.discoveredTiles?.contains(tileId) == true
    }
    
    /**
     * الحصول على نسبة اكتشاف منطقة
     */
    fun getDiscoveryPercentage(region: RegionType): Float =
        _regionMaps.value[region]?.discoveryPercentage ?: 0f
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Map Level Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على مستوى الخريطة لمنطقة
     */
    fun getMapLevel(region: RegionType): MapLevel =
        _regionMaps.value[region]?.mapLevel ?: MapLevel.NONE
    
    /**
     * شراء Overlay A
     */
    fun purchaseOverlayA(region: RegionType, currency: Int): Boolean {
        val vendor = MapVendorDatabase.getVendorsInRegion(region).firstOrNull() ?: return false
        if (currency < vendor.overlayAPrice) return false
        
        _regionMaps.update { maps ->
            val regionMap = maps[region] ?: return@update maps
            if (regionMap.overlayAPurchased) return@update maps
            
            maps + (region to regionMap.copy(
                overlayAPurchased = true,
                mapLevel = maxOf(regionMap.mapLevel, MapLevel.OVERLAY_A)
            ))
        }
        
        return true
    }
    
    /**
     * شراء Overlay B
     */
    fun purchaseOverlayB(region: RegionType, currency: Int): Boolean {
        val vendor = MapVendorDatabase.getVendorsInRegion(region).firstOrNull() ?: return false
        if (currency < vendor.overlayBPrice) return false
        
        _regionMaps.update { maps ->
            val regionMap = maps[region] ?: return@update maps
            if (regionMap.overlayBPurchased) return@update maps
            
            maps + (region to regionMap.copy(
                overlayBPurchased = true,
                mapLevel = maxOf(regionMap.mapLevel, MapLevel.OVERLAY_B)
            ))
        }
        
        return true
    }
    
    /**
     * شراء الخريطة الكاملة
     */
    fun purchaseFullMap(region: RegionType, currency: Int): Boolean {
        val vendor = MapVendorDatabase.getVendorsInRegion(region).firstOrNull() ?: return false
        if (currency < vendor.fullMapPrice) return false
        
        _regionMaps.update { maps ->
            val regionMap = maps[region] ?: return@update maps
            if (regionMap.fullMapPurchased) return@update maps
            
            maps + (region to regionMap.copy(
                fullMapPurchased = true,
                mapLevel = MapLevel.FULL
            ))
        }
        
        return true
    }
    
    /**
     * فتح الخريطة الكاملة بالشظايا
     */
    fun unlockFullMapWithShards(region: RegionType): Boolean {
        val regionMap = _regionMaps.value[region] ?: return false
        if (regionMap.mapShards < 5) return false
        
        _regionMaps.update { maps ->
            maps + (region to regionMap.copy(mapLevel = MapLevel.FULL))
        }
        
        return true
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Map Shard Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة شظية خريطة
     */
    fun addMapShard(region: RegionType) {
        _regionMaps.update { maps ->
            val regionMap = maps[region] ?: return@update maps
            maps + (region to regionMap.copy(mapShards = min(regionMap.mapShards + 1, 5)))
        }
        
        _totalMapShards.update { it + 1 }
        
        // فتح تلقائي إذا وصل إلى 5
        val updatedMap = _regionMaps.value[region]
        if (updatedMap != null && updatedMap.mapShards >= 5 && !updatedMap.isFullMapAvailable) {
            unlockFullMapWithShards(region)
        }
    }
    
    /**
     * شراء شظية خريطة من بائع
     */
    fun purchaseMapShard(region: RegionType, currency: Int): Boolean {
        val vendor = MapVendorDatabase.getVendorsInRegion(region).firstOrNull() ?: return false
        if (currency < vendor.mapShardPrice) return false
        
        addMapShard(region)
        return true
    }
    
    /**
     * الحصول على عدد الشظايا لمنطقة
     */
    fun getMapShards(region: RegionType): Int =
        _regionMaps.value[region]?.mapShards ?: 0
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Marker Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة علامة
     */
    fun addMarker(marker: MapMarker) {
        _regionMaps.update { maps ->
            val regionMap = maps[marker.region] ?: return@update maps
            val updatedMarkers = regionMap.markers + marker
            maps + (marker.region to regionMap.copy(markers = updatedMarkers))
        }
    }
    
    /**
     * إزالة علامة
     */
    fun removeMarker(markerId: String, region: RegionType) {
        _regionMaps.update { maps ->
            val regionMap = maps[region] ?: return@update maps
            val updatedMarkers = regionMap.markers.filter { it.id != markerId }
            maps + (region to regionMap.copy(markers = updatedMarkers))
        }
    }
    
    /**
     * تحديث علامة
     */
    fun updateMarker(markerId: String, region: RegionType, update: (MapMarker) -> MapMarker) {
        _regionMaps.update { maps ->
            val regionMap = maps[region] ?: return@update maps
            val updatedMarkers = regionMap.markers.map { marker ->
                if (marker.id == markerId) update(marker) else marker
            }
            maps + (region to regionMap.copy(markers = updatedMarkers))
        }
    }
    
    /**
     * وضع علامة كمكتشفة
     */
    fun discoverMarker(markerId: String, region: RegionType) {
        updateMarker(markerId, region) { it.copy(isDiscovered = true) }
    }
    
    /**
     * وضع علامة كمكتملة
     */
    fun completeMarker(markerId: String, region: RegionType) {
        updateMarker(markerId, region) { it.copy(isCompleted = true) }
        
        // إزالة إذا لم تكن دائمة
        val marker = getMarker(markerId, region)
        if (marker != null && !marker.isPermanent) {
            removeMarker(markerId, region)
        }
    }
    
    /**
     * الحصول على علامة
     */
    fun getMarker(markerId: String, region: RegionType): MapMarker? =
        _regionMaps.value[region]?.markers?.firstOrNull { it.id == markerId }
    
    /**
     * الحصول على علامات منطقة
     */
    fun getMarkersInRegion(region: RegionType): List<MapMarker> =
        _regionMaps.value[region]?.markers ?: emptyList()
    
    /**
     * الحصول على علامات حسب النوع
     */
    fun getMarkersByType(type: MapMarkerType, region: RegionType): List<MapMarker> =
        getMarkersInRegion(region).filter { it.type == type }
    
    /**
     * الحصول على علامات حسب الفئة
     */
    fun getMarkersByCategory(category: MapMarkerCategory, region: RegionType): List<MapMarker> =
        getMarkersInRegion(region).filter { it.category == category }
    
    /**
     * الحصول على علامات قريبة من موقع
     */
    fun getNearbyMarkers(position: Offset, radius: Float, region: RegionType): List<MapMarker> =
        getMarkersInRegion(region).filter { marker ->
            val distance = sqrt(
                (marker.position.x - position.x) * (marker.position.x - position.x) +
                (marker.position.y - position.y) * (marker.position.y - position.y)
            )
            distance <= radius
        }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Custom Pin Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * إضافة دبوس مخصص
     */
    fun addCustomPin(pin: CustomPin) {
        _customPins.update { pins ->
            if (pins.size >= 50) {  // حد أقصى 50 دبوس
                pins.drop(1) + pin
            } else {
                pins + pin
            }
        }
    }
    
    /**
     * إزالة دبوس مخصص
     */
    fun removeCustomPin(pinId: String) {
        _customPins.update { pins -> pins.filter { it.id != pinId } }
    }
    
    /**
     * الحصول على دبابيس منطقة
     */
    fun getCustomPinsInRegion(region: RegionType): List<CustomPin> =
        _customPins.value.filter { it.region == region }
    
    /**
     * مسح جميع الدبابيس
     */
    fun clearAllCustomPins() {
        _customPins.value = emptyList()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Vendor Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * اكتشاف بائع
     */
    fun discoverVendor(vendorId: String) {
        _discoveredVendors.update { it + vendorId }
    }
    
    /**
     * التحقق من اكتشاف بائع
     */
    fun isVendorDiscovered(vendorId: String): Boolean =
        _discoveredVendors.value.contains(vendorId)
    
    /**
     * الحصول على البائعين المكتشفين
     */
    fun getDiscoveredVendors(): List<MapVendor> =
        MapVendorDatabase.getAllVendors().filter { isVendorDiscovered(it.id) }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // View Settings
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * تعيين مستوى التكبير
     */
    fun setZoomLevel(level: ZoomLevel) {
        _viewSettings.update { it.copy(zoomLevel = level) }
    }
    
    /**
     * تكبير
     */
    fun zoomIn() {
        val currentLevel = _viewSettings.value.zoomLevel
        val newLevel = when (currentLevel) {
            ZoomLevel.OVERVIEW -> ZoomLevel.VERY_FAR
            ZoomLevel.VERY_FAR -> ZoomLevel.FAR
            ZoomLevel.FAR -> ZoomLevel.NORMAL
            ZoomLevel.NORMAL -> ZoomLevel.CLOSE
            ZoomLevel.CLOSE -> ZoomLevel.VERY_CLOSE
            ZoomLevel.VERY_CLOSE -> ZoomLevel.VERY_CLOSE
        }
        setZoomLevel(newLevel)
    }
    
    /**
     * تصغير
     */
    fun zoomOut() {
        val currentLevel = _viewSettings.value.zoomLevel
        val newLevel = when (currentLevel) {
            ZoomLevel.VERY_CLOSE -> ZoomLevel.CLOSE
            ZoomLevel.CLOSE -> ZoomLevel.NORMAL
            ZoomLevel.NORMAL -> ZoomLevel.FAR
            ZoomLevel.FAR -> ZoomLevel.VERY_FAR
            ZoomLevel.VERY_FAR -> ZoomLevel.OVERVIEW
            ZoomLevel.OVERVIEW -> ZoomLevel.OVERVIEW
        }
        setZoomLevel(newLevel)
    }
    
    /**
     * تعيين موضع المركز
     */
    fun setCenterPosition(position: Offset) {
        _viewSettings.update { it.copy(centerPosition = position) }
    }
    
    /**
     * تحريك الكاميرا
     */
    fun panCamera(delta: Offset) {
        _viewSettings.update { 
            it.copy(centerPosition = it.centerPosition + delta)
        }
    }
    
    /**
     * التركيز على موقع
     */
    fun focusOn(position: Offset) {
        _viewSettings.update { it.copy(centerPosition = position) }
    }
    
    /**
     * التركيز على علامة
     */
    fun focusOnMarker(markerId: String, region: RegionType) {
        val marker = getMarker(markerId, region)
        if (marker != null) {
            focusOn(marker.position)
            _viewSettings.update { it.copy(highlightedMarkerId = markerId) }
        }
    }
    
    /**
     * تبديل إظهار موقع اللاعب
     */
    fun togglePlayerPosition() {
        _viewSettings.update { it.copy(showPlayerPosition = !it.showPlayerPosition) }
    }
    
    /**
     * تبديل إظهار العلامات
     */
    fun toggleMarkers() {
        _viewSettings.update { it.copy(showMarkers = !it.showMarkers) }
    }
    
    /**
     * تبديل إظهار الدبابيس المخصصة
     */
    fun toggleCustomPins() {
        _viewSettings.update { it.copy(showCustomPins = !it.showCustomPins) }
    }
    
    /**
     * تبديل إظهار الشبكة
     */
    fun toggleGrid() {
        _viewSettings.update { it.copy(showGrid = !it.showGrid) }
    }
    
    /**
     * تبديل إظهار الإحداثيات
     */
    fun toggleCoordinates() {
        _viewSettings.update { it.copy(showCoordinates = !it.showCoordinates) }
    }
    
    /**
     * تبديل إظهار حدود المناطق
     */
    fun toggleRegionBorders() {
        _viewSettings.update { it.copy(showRegionBorders = !it.showRegionBorders) }
    }
    
    /**
     * تبديل إظهار Fog of War
     */
    fun toggleFogOfWar() {
        _viewSettings.update { it.copy(showFogOfWar = !it.showFogOfWar) }
    }
    
    /**
     * تبديل فئة علامات
     */
    fun toggleMarkerCategory(category: MapMarkerCategory) {
        _viewSettings.update { settings ->
            val newFilters = if (settings.markerFilters.contains(category)) {
                settings.markerFilters - category
            } else {
                settings.markerFilters + category
            }
            settings.copy(markerFilters = newFilters)
        }
    }
    
    /**
     * إعادة تعيين الإعدادات
     */
    fun resetViewSettings() {
        _viewSettings.value = MapViewSettings()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Utility Functions
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من إمكانية Fast Travel إلى موقع
     */
    fun canFastTravelTo(markerId: String, region: RegionType): Boolean {
        val marker = getMarker(markerId, region) ?: return false
        
        // فقط الملاجئ المكتشفة
        if (marker.type != MapMarkerType.SANCTUARY) return false
        if (!marker.isDiscovered) return false
        
        return true
    }
    
    /**
     * الحصول على أقرب ملجأ
     */
    fun getNearestSanctuary(position: Offset, region: RegionType): MapMarker? {
        val sanctuaries = getMarkersByType(MapMarkerType.SANCTUARY, region)
            .filter { it.isDiscovered }
        
        return sanctuaries.minByOrNull { sanctuary ->
            val dx = sanctuary.position.x - position.x
            val dy = sanctuary.position.y - position.y
            sqrt(dx * dx + dy * dy)
        }
    }
    
    /**
     * الحصول على المسافة بين نقطتين
     */
    fun getDistance(from: Offset, to: Offset): Float {
        val dx = to.x - from.x
        val dy = to.y - from.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * تحويل موقع عالمي إلى إحداثيات بلاطة
     */
    fun worldToTile(position: Offset): Pair<Int, Int> =
        Pair(position.x.toInt(), position.y.toInt())
    
    /**
     * تحويل إحداثيات بلاطة إلى موقع عالمي
     */
    fun tileToWorld(x: Int, y: Int): Offset =
        Offset(x.toFloat(), y.toFloat())
    
    /**
     * الحصول على حدود منطقة
     */
    fun getRegionBounds(region: RegionType): Rect {
        val regionData = RegionDatabase.getRegion(region) ?: return Rect.Zero
        return Rect(
            left = 0f,
            top = 0f,
            right = regionData.widthTiles.toFloat(),
            bottom = regionData.heightTiles.toFloat()
        )
    }
    
    /**
     * التحقق من وجود موقع داخل منطقة
     */
    fun isPositionInRegion(position: Offset, region: RegionType): Boolean {
        val bounds = getRegionBounds(region)
        return position.x >= bounds.left && position.x <= bounds.right &&
               position.y >= bounds.top && position.y <= bounds.bottom
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // Save/Load
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ حالة نظام الخرائط
     */
    fun saveState(): WorldMapState {
        return WorldMapState(
            regionMaps = _regionMaps.value,
            totalMapShards = _totalMapShards.value,
            customPins = _customPins.value,
            discoveredVendors = _discoveredVendors.value,
            viewSettings = _viewSettings.value
        )
    }
    
    /**
     * تحميل حالة نظام الخرائط
     */
    fun loadState(state: WorldMapState) {
        _regionMaps.value = state.regionMaps
        _totalMapShards.value = state.totalMapShards
        _customPins.value = state.customPins
        _discoveredVendors.value = state.discoveredVendors
        _viewSettings.value = state.viewSettings
    }
    
    /**
     * إعادة تعيين كل البيانات
     */
    fun resetAllData() {
        _regionMaps.value = RegionType.values().associateWith { region ->
            RegionMapState(
                region = region,
                mapLevel = if (region == RegionType.ASHEN_SPRAWL) MapLevel.OVERLAY_A else MapLevel.NONE,
                totalTiles = calculateTotalTiles(region),
                markers = MapMarkerPresets.createDefaultMarkersForRegion(region)
            )
        }
        _totalMapShards.value = 0
        _customPins.value = emptyList()
        _discoveredVendors.value = emptySet()
        _viewSettings.value = MapViewSettings()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Helper Functions
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * الحصول على لون أيقونة علامة
 */
fun getMarkerIconColor(type: MapMarkerType): Color = when (type) {
    MapMarkerType.PLAYER_POSITION -> Color(0xFF00E676)
    MapMarkerType.DEATH_LOCATION -> Color(0xFFFF1744)
    MapMarkerType.OBJECTIVE_ACTIVE -> Color(0xFFFFEA00)
    MapMarkerType.OBJECTIVE_COMPLETED -> Color(0xFF00C853)
    MapMarkerType.SANCTUARY -> Color(0xFFFFD54F)
    MapMarkerType.SANCTUARY_HIDDEN -> Color(0xFFFF9800)
    MapMarkerType.MAP_VENDOR -> Color(0xFF90CAF9)
    MapMarkerType.SHOP -> Color(0xFFFDD835)
    MapMarkerType.NPC -> Color(0xFF81C784)
    MapMarkerType.BOSS -> Color(0xFFFF5722)
    MapMarkerType.MINIBOSS -> Color(0xFFFF6F00)
    MapMarkerType.ELITE_ENEMY -> Color(0xFFE91E63)
    MapMarkerType.ENEMY_SPAWN -> Color(0xFFEF5350)
    MapMarkerType.MEMORY_FRAGMENT -> Color(0xFF9C27B0)
    MapMarkerType.LORE_OBJECT -> Color(0xFF64B5F6)
    MapMarkerType.SECRET -> Color(0xFFBA68C8)
    MapMarkerType.TREASURE -> Color(0xFFFFD700)
    MapMarkerType.HAZARD -> Color(0xFFFF9800)
    MapMarkerType.LOCKED_DOOR -> Color(0xFF757575)
    MapMarkerType.BRIDGE -> Color(0xFF8D6E63)
    MapMarkerType.TELEPORTER -> Color(0xFF7E57C2)
    MapMarkerType.FERRY -> Color(0xFF42A5F5)
    MapMarkerType.GONDOLA -> Color(0xFF26C6DA)
    MapMarkerType.AIRSHIP -> Color(0xFF66BB6A)
    MapMarkerType.TUNNEL -> Color(0xFF5D4037)
    MapMarkerType.CUSTOM_PIN -> Color(0xFFEEEEEE)
}

/**
 * الحصول على نوع أيقونة علامة
 */
fun getMarkerIconType(type: MapMarkerType): MapIconType = when (type) {
    MapMarkerType.PLAYER_POSITION -> MapIconType.DIAMOND
    MapMarkerType.DEATH_LOCATION -> MapIconType.CROSS
    MapMarkerType.OBJECTIVE_ACTIVE -> MapIconType.STAR
    MapMarkerType.OBJECTIVE_COMPLETED -> MapIconType.CIRCLE
    MapMarkerType.SANCTUARY -> MapIconType.TOWER
    MapMarkerType.SANCTUARY_HIDDEN -> MapIconType.TOWER
    MapMarkerType.MAP_VENDOR -> MapIconType.SCROLL
    MapMarkerType.SHOP -> MapIconType.CHEST
    MapMarkerType.NPC -> MapIconType.CIRCLE
    MapMarkerType.BOSS -> MapIconType.SKULL
    MapMarkerType.MINIBOSS -> MapIconType.SKULL
    MapMarkerType.ELITE_ENEMY -> MapIconType.SWORD
    MapMarkerType.ENEMY_SPAWN -> MapIconType.TRIANGLE
    MapMarkerType.MEMORY_FRAGMENT -> MapIconType.DIAMOND
    MapMarkerType.LORE_OBJECT -> MapIconType.SCROLL
    MapMarkerType.SECRET -> MapIconType.STAR
    MapMarkerType.TREASURE -> MapIconType.CHEST
    MapMarkerType.HAZARD -> MapIconType.TRIANGLE
    MapMarkerType.LOCKED_DOOR -> MapIconType.LOCK
    MapMarkerType.BRIDGE -> MapIconType.BRIDGE
    MapMarkerType.TELEPORTER -> MapIconType.CIRCLE
    MapMarkerType.FERRY -> MapIconType.BOAT
    MapMarkerType.GONDOLA -> MapIconType.BOAT
    MapMarkerType.AIRSHIP -> MapIconType.BOAT
    MapMarkerType.TUNNEL -> MapIconType.SQUARE
    MapMarkerType.CUSTOM_PIN -> MapIconType.CIRCLE
}

/**
 * حساب رؤية Fog of War
 */
fun calculateFogOfWarVisibility(
    tileX: Int,
    tileY: Int,
    playerX: Float,
    playerY: Float,
    visionRadius: Float
): Boolean {
    val distance = sqrt(
        (tileX - playerX) * (tileX - playerX) +
        (tileY - playerY) * (tileY - playerY)
    )
    return distance <= visionRadius
}

/**
 * تحويل Zoom Level إلى Scale
 */
fun zoomLevelToScale(level: ZoomLevel): Float = level.scale

/**
 * إنشاء Path لحدود منطقة
 */
fun createRegionBorderPath(region: RegionType): Path {
    val bounds = Rect(
        left = 0f,
        top = 0f,
        right = (RegionDatabase.getRegion(region)?.widthTiles ?: 100).toFloat(),
        bottom = (RegionDatabase.getRegion(region)?.heightTiles ?: 100).toFloat()
    )
    
    return Path().apply {
        moveTo(bounds.left, bounds.top)
        lineTo(bounds.right, bounds.top)
        lineTo(bounds.right, bounds.bottom)
        lineTo(bounds.left, bounds.bottom)
        close()
    }
}

/**
 * حساب لون Fog of War
 */
fun calculateFogColor(region: RegionType, isDiscovered: Boolean, isVisible: Boolean): Color {
    val regionData = RegionDatabase.getRegion(region) ?: return Color.Black
    
    return when {
        isVisible -> Color.Transparent  // مرئي تماماً
        isDiscovered -> regionData.fogColor.copy(alpha = 0.3f)  // مكتشف لكن خارج الرؤية
        else -> regionData.fogColor.copy(alpha = 0.8f)  // غير مكتشف
    }
}