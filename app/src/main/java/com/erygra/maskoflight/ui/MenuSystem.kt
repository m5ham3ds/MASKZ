package com.erygra.maskoflight.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * # نظام القوائم الكامل (Complete Menu System)
 * 
 * يشمل جميع شاشات القوائم:
 * - Main Menu: القائمة الرئيسية
 * - Pause Menu: قائمة الإيقاف المؤقت
 * - Settings: الإعدادات (Audio, Video, Controls, Accessibility)
 * - Load/Save Game: تحميل/حفظ اللعبة
 * - Credits: شاشة الشكر والتقدير
 * - Tutorial: شاشة التعليمات
 * - Confirmation Dialogs: حوارات التأكيد
 * 
 * Features:
 * - Smooth transitions
 * - Background blur effects
 * - Arabic/English support
 * - Keyboard/gamepad navigation
 * - Persistent settings
 * 
 * @author Erygra Development Team
 * @version 2.0
 * @since 2025-01-09
 */

// ══════════════════════════════════════════════════════════════════════════════
// Menu Navigation State
// ══════════════════════════════════════════════════════════════════════════════

/**
 * حالة التنقل بين القوائم
 * Menu navigation state
 */
sealed class MenuScreen {
    object MainMenu : MenuScreen()
    object PauseMenu : MenuScreen()
    object NewGame : MenuScreen()
    object LoadGame : MenuScreen()
    object Settings : MenuScreen()
    object SettingsAudio : MenuScreen()
    object SettingsVideo : MenuScreen()
    object SettingsControls : MenuScreen()
    object SettingsAccessibility : MenuScreen()
    object Credits : MenuScreen()
    object Tutorial : MenuScreen()
    object Quit : MenuScreen()
}

/**
 * أحداث القائمة
 * Menu events
 */
sealed class MenuEvent {
    object StartNewGame : MenuEvent()
    object ContinueGame : MenuEvent()
    data class LoadGame(val slotId: Int) : MenuEvent()
    object SaveGame : MenuEvent()
    object ResumeGame : MenuEvent()
    object ReturnToMainMenu : MenuEvent()
    object QuitGame : MenuEvent()
    data class NavigateTo(val screen: MenuScreen) : MenuEvent()
    data class UpdateSettings(val settings: GameSettings) : MenuEvent()
}

/**
 * إعدادات اللعبة
 * Game settings
 */
data class GameSettings(
    // Audio
    val masterVolume: Float = 1.0f,
    val musicVolume: Float = 0.8f,
    val sfxVolume: Float = 1.0f,
    val ambientVolume: Float = 0.6f,
    val muteAll: Boolean = false,
    
    // Video
    val targetFPS: Int = 60,
    val vsyncEnabled: Boolean = true,
    val particleQuality: ParticleQuality = ParticleQuality.HIGH,
    val shadowQuality: ShadowQuality = ShadowQuality.MEDIUM,
    val screenShakeEnabled: Boolean = true,
    val screenShakeIntensity: Float = 1.0f,
    
    // Controls
    val dpadSize: Float = 1.0f,
    val dpadOpacity: Float = 0.7f,
    val dpadPosition: DPadPosition = DPadPosition.BOTTOM_LEFT,
    val buttonSize: Float = 1.0f,
    val buttonOpacity: Float = 0.7f,
    val vibrationEnabled: Boolean = true,
    val vibrationIntensity: Float = 1.0f,
    
    // Accessibility
    val language: Language = Language.ARABIC,
    val textSize: Float = 1.0f,
    val highContrast: Boolean = false,
    val colorblindMode: ColorblindMode = ColorblindMode.NONE,
    val reduceMotion: Boolean = false,
    val audioDescriptions: Boolean = false,
    val subtitlesEnabled: Boolean = true,
    val subtitleSize: Float = 1.0f
)

enum class ParticleQuality { LOW, MEDIUM, HIGH, ULTRA }
enum class ShadowQuality { OFF, LOW, MEDIUM, HIGH }
enum class DPadPosition { BOTTOM_LEFT, BOTTOM_RIGHT }
enum class Language { ARABIC, ENGLISH }
enum class ColorblindMode { NONE, PROTANOPIA, DEUTERANOPIA, TRITANOPIA }

// ══════════════════════════════════════════════════════════════════════════════
// Main Menu
// ══════════════════════════════════════════════════════════════════════════════

/**
 * القائمة الرئيسية
 * Main menu screen
 */
@Composable
fun MainMenuScreen(
    onEvent: (MenuEvent) -> Unit,
    hasSaveData: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf(0) }
    
    val menuItems = buildList {
        add(MenuItem.NewGame)
        if (hasSaveData) add(MenuItem.Continue)
        add(MenuItem.LoadGame)
        add(MenuItem.Settings)
        add(MenuItem.Credits)
        add(MenuItem.Quit)
    }
    
    // Background animation
    val infiniteTransition = rememberInfiniteTransition()
    val backgroundShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ErytColor.VoidPrimary,
                        ErytColor.SurfaceDark,
                        ErytColor.VoidPrimary
                    ),
                    startY = backgroundShift,
                    endY = backgroundShift + 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Game Title
            GameTitle()
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Menu Items
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                menuItems.forEachIndexed { index, item ->
                    MainMenuItem(
                        item = item,
                        isSelected = index == selectedIndex,
                        onClick = {
                            selectedIndex = index
                            handleMenuItemClick(item, onEvent)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Version info
            Text(
                text = "الإصدار 2.0.0 | Erygra Universe",
                color = ErytColor.OutlineGray.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * عنوان اللعبة
 * Game title
 */
@Composable
fun GameTitle() {
    val pulseAnim = rememberInfiniteTransition()
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(pulse)
    ) {
        Text(
            text = "قِنَاعُ النُّور",
            color = ErytColor.BlightGold,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = ErytColor.BlightGold.copy(alpha = 0.5f),
                    offset = Offset(0f, 0f),
                    blurRadius = 20f
                )
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "MASK OF LIGHT",
            color = ErytColor.RadianceWhite.copy(alpha = 0.8f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 4.sp
        )
    }
}

/**
 * عناصر القائمة
 * Menu items enum
 */
sealed class MenuItem(val titleAr: String, val titleEn: String) {
    object NewGame : MenuItem("لعبة جديدة", "NEW GAME")
    object Continue : MenuItem("متابعة", "CONTINUE")
    object LoadGame : MenuItem("تحميل لعبة", "LOAD GAME")
    object Settings : MenuItem("الإعدادات", "SETTINGS")
    object Credits : MenuItem("شكر وتقدير", "CREDITS")
    object Tutorial : MenuItem("التعليمات", "TUTORIAL")
    object Quit : MenuItem("خروج", "QUIT")
    object Resume : MenuItem("استئناف", "RESUME")
    object Save : MenuItem("حفظ اللعبة", "SAVE GAME")
    object MainMenu : MenuItem("القائمة الرئيسية", "MAIN MENU")
}

/**
 * عنصر قائمة رئيسي
 * Main menu item
 */
@Composable
fun MainMenuItem(
    item: MenuItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.7f
    )
    
    Box(
        modifier = modifier
            .width(400.dp)
            .scale(scale)
            .alpha(alpha)
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isSelected) 
                    ErytColor.BlightGold.copy(alpha = 0.2f) 
                else 
                    ErytColor.SurfaceDark.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ErytColor.BlightGold else ErytColor.OutlineGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.titleAr,
            color = if (isSelected) ErytColor.BlightGold else ErytColor.RadianceWhite,
            fontSize = 24.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * معالجة النقر على عنصر القائمة
 * Handle menu item click
 */
private fun handleMenuItemClick(item: MenuItem, onEvent: (MenuEvent) -> Unit) {
    when (item) {
        MenuItem.NewGame -> onEvent(MenuEvent.StartNewGame)
        MenuItem.Continue -> onEvent(MenuEvent.ContinueGame)
        MenuItem.LoadGame -> onEvent(MenuEvent.NavigateTo(MenuScreen.LoadGame))
        MenuItem.Settings -> onEvent(MenuEvent.NavigateTo(MenuScreen.Settings))
        MenuItem.Credits -> onEvent(MenuEvent.NavigateTo(MenuScreen.Credits))
        MenuItem.Tutorial -> onEvent(MenuEvent.NavigateTo(MenuScreen.Tutorial))
        MenuItem.Quit -> onEvent(MenuEvent.NavigateTo(MenuScreen.Quit))
        MenuItem.Resume -> onEvent(MenuEvent.ResumeGame)
        MenuItem.Save -> onEvent(MenuEvent.SaveGame)
        MenuItem.MainMenu -> onEvent(MenuEvent.ReturnToMainMenu)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Pause Menu
// ══════════════════════════════════════════════════════════════════════════════

/**
 * قائمة الإيقاف المؤقت
 * Pause menu screen
 */
@Composable
fun PauseMenuScreen(
    onEvent: (MenuEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val menuItems = listOf(
        MenuItem.Resume,
        MenuItem.Save,
        MenuItem.Settings,
        MenuItem.MainMenu,
        MenuItem.Quit
    )
    
    var selectedIndex by remember { mutableStateOf(0) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .blur(radius = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        ErytPanel(
            modifier = Modifier
                .width(500.dp)
                .wrapContentHeight(),
            backgroundColor = ErytColor.VoidPrimary.copy(alpha = 0.95f),
            borderColor = ErytColor.BlightGold,
            borderWidth = 2.dp,
            elevation = 16.dp
        ) {
            Text(
                text = "مُوقَّف مؤقتاً",
                color = ErytColor.BlightGold,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            menuItems.forEachIndexed { index, item ->
                MainMenuItem(
                    item = item,
                    isSelected = index == selectedIndex,
                    onClick = {
                        selectedIndex = index
                        handleMenuItemClick(item, onEvent)
                    }
                )
                
                if (index < menuItems.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Settings Menu
// ══════════════════════════════════════════════════════════════════════════════

/**
 * قائمة الإعدادات الرئيسية
 * Main settings menu
 */
@Composable
fun SettingsMenuScreen(
    currentSettings: GameSettings,
    onEvent: (MenuEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.AUDIO) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ErytColor.VoidPrimary)
            .padding(32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الإعدادات",
                color = ErytColor.BlightGold,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            ErytIconButton(
                onClick = onBack,
                color = ErytColor.SurfaceDark
            ) {
                Text(text = "✕", fontSize = 24.sp, color = ErytColor.RadianceWhite)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Tabs
        SettingsTabs(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Content
        when (selectedTab) {
            SettingsTab.AUDIO -> AudioSettingsPanel(
                settings = currentSettings,
                onSettingsChange = { onEvent(MenuEvent.UpdateSettings(it)) }
            )
            SettingsTab.VIDEO -> VideoSettingsPanel(
                settings = currentSettings,
                onSettingsChange = { onEvent(MenuEvent.UpdateSettings(it)) }
            )
            SettingsTab.CONTROLS -> ControlsSettingsPanel(
                settings = currentSettings,
                onSettingsChange = { onEvent(MenuEvent.UpdateSettings(it)) }
            )
            SettingsTab.ACCESSIBILITY -> AccessibilitySettingsPanel(
                settings = currentSettings,
                onSettingsChange = { onEvent(MenuEvent.UpdateSettings(it)) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Reset to defaults button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ErytOutlineButton(
                text = "استعادة الافتراضيات",
                onClick = {
                    onEvent(MenuEvent.UpdateSettings(GameSettings()))
                },
                borderColor = ErytColor.WarningOrange,
                textColor = ErytColor.WarningOrange
            )
        }
    }
}

/**
 * تبويبات الإعدادات
 * Settings tabs enum
 */
enum class SettingsTab(val titleAr: String, val titleEn: String) {
    AUDIO("الصوت", "AUDIO"),
    VIDEO("الفيديو", "VIDEO"),
    CONTROLS("التحكم", "CONTROLS"),
    ACCESSIBILITY("إمكانية الوصول", "ACCESSIBILITY")
}

/**
 * أشرطة تبويبات الإعدادات
 * Settings tabs row
 */
@Composable
fun SettingsTabs(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SettingsTab.values().forEach { tab ->
            SettingsTabItem(
                tab = tab,
                isSelected = tab == selectedTab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

/**
 * عنصر تبويب واحد
 * Single settings tab
 */
@Composable
fun SettingsTabItem(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tab.titleAr,
            color = if (isSelected) ErytColor.BlightGold else ErytColor.OutlineGray,
            fontSize = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(ErytColor.BlightGold)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Audio Settings
// ══════════════════════════════════════════════════════════════════════════════

/**
 * لوحة إعدادات الصوت
 * Audio settings panel
 */
@Composable
fun AudioSettingsPanel(
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Volume
        SettingSlider(
            label = "الصوت الرئيسي",
            value = settings.masterVolume,
            onValueChange = { onSettingsChange(settings.copy(masterVolume = it)) },
            enabled = !settings.muteAll
        )
        
        // Music Volume
        SettingSlider(
            label = "موسيقى",
            value = settings.musicVolume,
            onValueChange = { onSettingsChange(settings.copy(musicVolume = it)) },
            enabled = !settings.muteAll
        )
        
        // SFX Volume
        SettingSlider(
            label = "مؤثرات صوتية",
            value = settings.sfxVolume,
            onValueChange = { onSettingsChange(settings.copy(sfxVolume = it)) },
            enabled = !settings.muteAll
        )
        
        // Ambient Volume
        SettingSlider(
            label = "أصوات محيطة",
            value = settings.ambientVolume,
            onValueChange = { onSettingsChange(settings.copy(ambientVolume = it)) },
            enabled = !settings.muteAll
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Mute All Toggle
        SettingToggle(
            label = "كتم الكل",
            checked = settings.muteAll,
            onCheckedChange = { onSettingsChange(settings.copy(muteAll = it)) }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Video Settings
// ══════════════════════════════════════════════════════════════════════════════

/**
 * لوحة إعدادات الفيديو
 * Video settings panel
 */
@Composable
fun VideoSettingsPanel(
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target FPS
        SettingDropdown(
            label = "معدل الإطارات المستهدف",
            value = settings.targetFPS.toString(),
            options = listOf("30", "60", "120"),
            onValueChange = { 
                onSettingsChange(settings.copy(targetFPS = it.toInt())) 
            }
        )
        
        // VSync
        SettingToggle(
            label = "المزامنة العمودية (VSync)",
            checked = settings.vsyncEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(vsyncEnabled = it)) }
        )
        
        // Particle Quality
        SettingDropdown(
            label = "جودة الجسيمات",
            value = settings.particleQuality.name,
            options = ParticleQuality.values().map { it.name },
            onValueChange = { 
                onSettingsChange(settings.copy(particleQuality = ParticleQuality.valueOf(it))) 
            }
        )
        
        // Shadow Quality
        SettingDropdown(
            label = "جودة الظلال",
            value = settings.shadowQuality.name,
            options = ShadowQuality.values().map { it.name },
            onValueChange = { 
                onSettingsChange(settings.copy(shadowQuality = ShadowQuality.valueOf(it))) 
            }
        )
        
        // Screen Shake
        SettingToggle(
            label = "اهتزاز الشاشة",
            checked = settings.screenShakeEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(screenShakeEnabled = it)) }
        )
        
        if (settings.screenShakeEnabled) {
            SettingSlider(
                label = "شدة الاهتزاز",
                value = settings.screenShakeIntensity,
                onValueChange = { onSettingsChange(settings.copy(screenShakeIntensity = it)) }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Controls Settings
// ══════════════════════════════════════════════════════════════════════════════

/**
 * لوحة إعدادات التحكم
 * Controls settings panel
 */
@Composable
fun ControlsSettingsPanel(
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "عناصر التحكم",
            color = ErytColor.BlightGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        // D-Pad Size
        SettingSlider(
            label = "حجم لوحة الاتجاهات",
            value = settings.dpadSize,
            valueRange = 0.5f..2.0f,
            onValueChange = { onSettingsChange(settings.copy(dpadSize = it)) }
        )
        
        // D-Pad Opacity
        SettingSlider(
            label = "شفافية لوحة الاتجاهات",
            value = settings.dpadOpacity,
            onValueChange = { onSettingsChange(settings.copy(dpadOpacity = it)) }
        )
        
        // D-Pad Position
        SettingDropdown(
            label = "موضع لوحة الاتجاهات",
            value = settings.dpadPosition.name,
            options = DPadPosition.values().map { it.name },
            onValueChange = { 
                onSettingsChange(settings.copy(dpadPosition = DPadPosition.valueOf(it))) 
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Button Size
        SettingSlider(
            label = "حجم الأزرار",
            value = settings.buttonSize,
            valueRange = 0.5f..2.0f,
            onValueChange = { onSettingsChange(settings.copy(buttonSize = it)) }
        )
        
        // Button Opacity
        SettingSlider(
            label = "شفافية الأزرار",
            value = settings.buttonOpacity,
            onValueChange = { onSettingsChange(settings.copy(buttonOpacity = it)) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Vibration
        SettingToggle(
            label = "الاهتزاز",
            checked = settings.vibrationEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(vibrationEnabled = it)) }
        )
        
        if (settings.vibrationEnabled) {
            SettingSlider(
                label = "شدة الاهتزاز",
                value = settings.vibrationIntensity,
                onValueChange = { onSettingsChange(settings.copy(vibrationIntensity = it)) }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Accessibility Settings
// ══════════════════════════════════════════════════════════════════════════════

/**
 * لوحة إعدادات إمكانية الوصول
 * Accessibility settings panel
 */
@Composable
fun AccessibilitySettingsPanel(
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language
        SettingDropdown(
            label = "اللغة",
            value = settings.language.name,
            options = Language.values().map { it.name },
            onValueChange = { 
                onSettingsChange(settings.copy(language = Language.valueOf(it))) 
            }
        )
        
        // Text Size
        SettingSlider(
            label = "حجم النص",
            value = settings.textSize,
            valueRange = 0.5f..2.0f,
            onValueChange = { onSettingsChange(settings.copy(textSize = it)) }
        )
        
        // High Contrast
        SettingToggle(
            label = "تباين عالي",
            checked = settings.highContrast,
            onCheckedChange = { onSettingsChange(settings.copy(highContrast = it)) }
        )
        
        // Colorblind Mode
        SettingDropdown(
            label = "وضع عمى الألوان",
            value = settings.colorblindMode.name,
            options = ColorblindMode.values().map { it.name },
            onValueChange = { 
                onSettingsChange(settings.copy(colorblindMode = ColorblindMode.valueOf(it))) 
            }
        )
        
        // Reduce Motion
        SettingToggle(
            label = "تقليل الحركة",
            checked = settings.reduceMotion,
            onCheckedChange = { onSettingsChange(settings.copy(reduceMotion = it)) }
        )
        
        // Audio Descriptions
        SettingToggle(
            label = "الأوصاف الصوتية",
            checked = settings.audioDescriptions,
            onCheckedChange = { onSettingsChange(settings.copy(audioDescriptions = it)) }
        )
        
        // Subtitles
        SettingToggle(
            label = "الترجمة",
            checked = settings.subtitlesEnabled,
            onCheckedChange = { onSettingsChange(settings.copy(subtitlesEnabled = it)) }
        )
        
        if (settings.subtitlesEnabled) {
            SettingSlider(
                label = "حجم الترجمة",
                value = settings.subtitleSize,
                valueRange = 0.5f..2.0f,
                onValueChange = { onSettingsChange(settings.copy(subtitleSize = it)) }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Setting Components
// ══════════════════════════════════════════════════════════════════════════════

/**
 * مكون شريط التمرير للإعدادات
 * Settings slider component
 */
@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (enabled) ErytColor.RadianceWhite else ErytColor.OutlineGray,
                fontSize = 16.sp
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = ErytColor.BlightGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = ErytColor.BlightGold,
                activeTrackColor = ErytColor.BlightGold,
                inactiveTrackColor = ErytColor.OutlineGray
            )
        )
    }
}

/**
 * مكون التبديل للإعدادات
 * Settings toggle component
 */
@Composable
fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) ErytColor.RadianceWhite else ErytColor.OutlineGray,
            fontSize = 16.sp
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ErytColor.BlightGold,
                checkedTrackColor = ErytColor.BlightGold.copy(alpha = 0.5f),
                uncheckedThumbColor = ErytColor.OutlineGray,
                uncheckedTrackColor = ErytColor.OutlineGray.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * مكون القائمة المنسدلة للإعدادات
 * Settings dropdown component
 */
@Composable
fun SettingDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = ErytColor.RadianceWhite,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ErytColor.SurfaceDark,
                    contentColor = ErytColor.BlightGold
                ),
                border = BorderStroke(1.dp, ErytColor.OutlineGray)
            ) {
                Text(text = value, modifier = Modifier.weight(1f))
                Text(text = "▼", fontSize = 12.sp)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ErytColor.SurfaceDark)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == value) ErytColor.BlightGold else ErytColor.RadianceWhite
                            )
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Load/Save Game
// ══════════════════════════════════════════════════════════════════════════════

/**
 * بيانات فتحة الحفظ
 * Save slot data
 */
data class SaveSlotData(
    val slotId: Int,
    val playerName: String?,
    val level: Int,
    val playTimeMinutes: Int,
    val currentRegion: String,
    val lastPlayed: Long,
    val screenshotPath: String?
)

/**
 * شاشة تحميل اللعبة
 * Load game screen
 */
@Composable
fun LoadGameScreen(
    saveSlots: List<SaveSlotData>,
    onLoadSlot: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ErytColor.VoidPrimary)
            .padding(32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تحميل لعبة",
                color = ErytColor.BlightGold,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            
            ErytIconButton(onClick = onBack) {
                Text(text = "✕", fontSize = 24.sp, color = ErytColor.RadianceWhite)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Save slots
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(saveSlots) { slot ->
                SaveSlotItem(
                    slotData = slot,
                    onClick = { onLoadSlot(slot.slotId) }
                )
            }
        }
    }
}

/**
 * عنصر فتحة حفظ
 * Save slot item
 */
@Composable
fun SaveSlotItem(
    slotData: SaveSlotData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErytPanel(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = ErytColor.SurfaceDark,
        borderColor = ErytColor.OutlineGray
    ) {
        if (slotData.playerName != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "فتحة ${slotData.slotId}",
                        color = ErytColor.OutlineGray,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = slotData.playerName,
                        color = ErytColor.RadianceWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row {
                        Text(
                            text = "المستوى ${slotData.level}",
                            color = ErytColor.BlightGold,
                            fontSize = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = slotData.currentRegion,
                            color = ErytColor.EchoesBlue,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = formatPlayTime(slotData.playTimeMinutes),
                        color = ErytColor.OutlineGray,
                        fontSize = 12.sp
                    )
                }
                
                // Placeholder for screenshot
                Box(
                    modifier = Modifier
                        .size(120.dp, 80.dp)
                        .background(
                            color = ErytColor.VoidPrimary,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        } else {
            // Empty slot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "فتحة فارغة",
                    color = ErytColor.OutlineGray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * تنسيق وقت اللعب
 * Format play time
 */
private fun formatPlayTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) {
        "${hours}س ${mins}د"
    } else {
        "${mins}د"
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Credits
// ══════════════════════════════════════════════════════════════════════════════

/**
 * شاشة شكر وتقدير
 * Credits screen
 */
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        // Auto-scroll effect
        while (true) {
            delay(50)
            scrollState.animateScrollTo(
                scrollState.value + 1,
                animationSpec = tween(50, easing = LinearEasing)
            )
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ErytColor.VoidPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            
            // Game title
            Text(
                text = "قِنَاعُ النُّور",
                color = ErytColor.BlightGold,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "MASK OF LIGHT",
                color = ErytColor.RadianceWhite,
                fontSize = 24.sp,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Credits sections
            CreditsSection(
                title = "تطوير",
                entries = listOf(
                    "Lead Developer" to "Erygra Team",
                    "Game Design" to "Erygra Team",
                    "Programming" to "Kotlin & Compose"
                )
            )
            
            CreditsSection(
                title = "فن وصوت",
                entries = listOf(
                    "Art Direction" to "Erygra Team",
                    "Music & Sound" to "Erygra Team",
                    "Voice Acting" to "TBD"
                )
            )
            
            CreditsSection(
                title = "شكر خاص",
                entries = listOf(
                    "Community" to "All Players",
                    "Testers" to "Beta Team",
                    "Support" to "Everyone"
                )
            )
            
            Spacer(modifier = Modifier.height(100.dp))
            
            Text(
                text = "© 2025 Erygra Universe",
                color = ErytColor.OutlineGray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(200.dp))
        }
        
        // Back button
        ErytIconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp)
        ) {
            Text(text = "✕", fontSize = 24.sp, color = ErytColor.RadianceWhite)
        }
    }
}

/**
 * قسم شكر وتقدير
 * Credits section
 */
@Composable
fun CreditsSection(
    title: String,
    entries: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = ErytColor.BlightGold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        entries.forEach { (role, name) ->
            Text(
                text = role,
                color = ErytColor.OutlineGray,
                fontSize = 16.sp
            )
            Text(
                text = name,
                color = ErytColor.RadianceWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Confirmation Dialog
// ══════════════════════════════════════════════════════════════════════════════

/**
 * حوار تأكيد
 * Confirmation dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "تأكيد",
    dismissText: String = "إلغاء",
    modifier: Modifier = Modifier
) {
    ErytDialog(
        title = title,
        onDismiss = onDismiss,
        confirmButton = {
            ErytButton(
                text = confirmText,
                onClick = onConfirm,
                color = ErytColor.DangerRed
            )
        },
        dismissButton = {
            ErytOutlineButton(
                text = dismissText,
                onClick = onDismiss
            )
        }
    ) {
        Text(
            text = message,
            color = ErytColor.RadianceWhite,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}