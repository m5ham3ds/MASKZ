package com.erygra.maskoflight.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erygra.maskoflight.player.PlayerState
import com.erygra.maskoflight.player.PlayerStateAbility
import com.erygra.maskoflight.player.AbilityType
import com.erygra.maskoflight.world.RegionType
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * # نظام عرض المعلومات (Heads-Up Display System)
 * 
 * واجهة اللعب الرئيسية التي تعرض:
 * - Health & Energy: أشرطة الصحة والطاقة
 * - MF & FM: عداد شظايا الذاكرة ومقياس النسيان
 * - Minimap: خريطة مصغرة مع بوصلة
 * - Ability Cooldowns: مؤقتات القدرات
 * - Item Slots: فتحات الأدوات السريعة
 * - Quest Tracker: متتبع المهام
 * - Notifications: الإشعارات المؤقتة
 * - Boss Health: شريط صحة الزعيم
 * - Combo Counter: عداد الضربات المتتالية
 * 
 * Features:
 * - Adaptive positioning (top, bottom, corners)
 * - Auto-hide on inactivity
 * - Pulse effects on changes
 * - Warning indicators
 * - Touch-friendly sizing
 * 
 * @author Erygra Development Team
 * @version 2.0
 * @since 2025-01-09
 */

// ══════════════════════════════════════════════════════════════════════════════
// HUD State
// ══════════════════════════════════════════════════════════════════════════════

/**
 * حالة HUD
 * HUD configuration state
 */
data class HUDState(
    /** إظهار HUD - Show HUD */
    val visible: Boolean = true,
    /** الشفافية - Opacity (0.0-1.0) */
    val opacity: Float = 1.0f,
    /** إخفاء تلقائي - Auto-hide enabled */
    val autoHide: Boolean = false,
    /** وقت الإخفاء (ms) - Auto-hide delay */
    val autoHideDelay: Long = 3000L,
    /** إظهار خريطة مصغرة - Show minimap */
    val showMinimap: Boolean = true,
    /** إظهار متتبع مهام - Show quest tracker */
    val showQuestTracker: Boolean = true,
    /** إظهار عداد ضربات - Show combo counter */
    val showComboCounter: Boolean = true,
    /** إظهار إحصائيات مفصلة - Show detailed stats */
    val showDetailedStats: Boolean = false,
    /** حجم الأيقونات - Icon size multiplier */
    val iconSize: Float = 1.0f,
    /** موضع الخريطة المصغرة - Minimap position */
    val minimapPosition: MinimapPosition = MinimapPosition.TOP_RIGHT
)

/**
 * موضع الخريطة المصغرة
 * Minimap position
 */
enum class MinimapPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

/**
 * نوع الإشعار
 * Notification type for HUD
 */
data class HUDNotification(
    val id: String,
    val message: String,
    val messageArabic: String,
    val type: NotificationType,
    val duration: Long = 3000L,
    val timestamp: Long = System.currentTimeMillis(),
    val icon: String? = null
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > timestamp + duration
}

/**
 * معلومات الزعيم
 * Boss information for HUD
 */
data class BossInfo(
    val name: String,
    val nameArabic: String,
    val currentHP: Float,
    val maxHP: Float,
    val phase: Int,
    val maxPhases: Int,
    val isVulnerable: Boolean = false
)

/**
 * معلومات المهمة للعرض
 * Quest info for tracker
 */
data class QuestTrackerInfo(
    val questId: String,
    val title: String,
    val titleArabic: String,
    val currentObjective: String,
    val currentObjectiveArabic: String,
    val progress: Float, // 0.0 - 1.0
    val isOptional: Boolean = false
)

// ══════════════════════════════════════════════════════════════════════════════
// Main HUD Composable
// ══════════════════════════════════════════════════════════════════════════════

/**
 * واجهة HUD الرئيسية
 * Main HUD overlay
 */
@Composable
fun GameHUD(
    playerState: PlayerState,
    hudState: HUDState,
    currentRegion: RegionType,
    notifications: List<HUDNotification>,
    bossInfo: BossInfo? = null,
    activeQuests: List<QuestTrackerInfo> = emptyList(),
    comboCount: Int = 0,
    abilityStates: Map<AbilityType, AbilityState>,
    modifier: Modifier = Modifier,
    onAbilityClick: (AbilityType) -> Unit = {},
    onMinimapClick: () -> Unit = {},
    onQuestClick: (String) -> Unit = {}
) {
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val isIdle = remember(lastInteractionTime) {
        derivedStateOf {
            hudState.autoHide && 
            (System.currentTimeMillis() - lastInteractionTime) > hudState.autoHideDelay
        }
    }
    
    val hudAlpha by animateFloatAsState(
        targetValue = if (isIdle.value) 0.3f else hudState.opacity,
        animationSpec = tween(durationMillis = 500)
    )
    
    if (!hudState.visible) return
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(hudAlpha)
            .pointerInput(Unit) {
                // Track interactions
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            }
    ) {
        // ════ Top HUD ════
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Player Stats
            PlayerStatsBar(
                playerState = playerState,
                showDetailed = hudState.showDetailedStats
            )
            
            // Boss Health (if fighting boss)
            bossInfo?.let {
                Spacer(modifier = Modifier.height(8.dp))
                BossHealthBar(bossInfo = it)
            }
        }
        
        // ════ Top Right - Minimap ════
        if (hudState.showMinimap && hudState.minimapPosition == MinimapPosition.TOP_RIGHT) {
            Minimap(
                currentRegion = currentRegion,
                playerPosition = Offset(playerState.position.x, playerState.position.y),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clickable(onClick = onMinimapClick),
                size = 120.dp * hudState.iconSize
            )
        }
        
        // ════ Bottom Left - Ability Cooldowns ════
        AbilityCooldownBar(
            abilityStates = abilityStates,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            iconSize = hudState.iconSize,
            onAbilityClick = onAbilityClick
        )
        
        // ════ Bottom Right - Item Slots ════
        ItemSlotBar(
            playerState = playerState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            iconSize = hudState.iconSize
        )
        
        // ════ Right Side - Quest Tracker ════
        if (hudState.showQuestTracker && activeQuests.isNotEmpty()) {
            QuestTracker(
                quests = activeQuests,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
                onQuestClick = onQuestClick
            )
        }
        
        // ════ Center - Combo Counter ════
        if (hudState.showComboCounter && comboCount > 0) {
            ComboCounter(
                count = comboCount,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // ════ Top Left - Notifications ════
        NotificationStack(
            notifications = notifications,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Player Stats Bar
// ══════════════════════════════════════════════════════════════════════════════

/**
 * شريط إحصائيات اللاعب
 * Player stats display
 */
@Composable
fun PlayerStatsBar(
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    showDetailed: Boolean = false
) {
    Column(modifier = modifier) {
        // Health Bar
        ErytStatBar(
            current = playerState.stats.hp.toFloat(),
            max = playerState.stats.maxHp.toFloat(),
            label = "HP",
            color = ErytColor.VitalityRed,
            height = 28.dp,
            showValue = showDetailed,
            icon = {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = ErytColor.VitalityRed,
                        radius = size.minDimension / 2 * 0.7f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2 * 0.3f,
                        center = center
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Energy Bar
        ErytStatBar(
            current = playerState.stats.energy.toFloat(),
            max = playerState.stats.maxEnergy.toFloat(),
            label = "EN",
            color = ErytColor.EchoesBlue,
            height = 28.dp,
            showValue = showDetailed,
            icon = {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(center.x, center.y - size.minDimension / 3)
                        lineTo(center.x + size.minDimension / 4, center.y)
                        lineTo(center.x, center.y + size.minDimension / 3)
                        lineTo(center.x - size.minDimension / 4, center.y)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = ErytColor.EchoesBlue
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // MF & FM Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Memory Fragments
            MemoryFragmentDisplay(
                count = playerState.stats.memoryFragments,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Forgetfulness Meter
            ForgetfulnessMeter(
                fm = playerState.stats.forgetfulnessMeter,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Level & XP (if detailed)
        if (showDetailed) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المستوى ${playerState.stats.level}",
                    color = ErytColor.BlightGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                ErytProgressBar(
                    progress = playerState.stats.xp.toFloat() / playerState.stats.xpToNextLevel.toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    foregroundColor = ErytColor.BlightGold,
                    height = 6.dp
                )
                
                Text(
                    text = "${playerState.stats.xp}/${playerState.stats.xpToNextLevel}",
                    color = ErytColor.OutlineGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Memory Fragment Display
// ══════════════════════════════════════════════════════════════════════════════

/**
 * عرض شظايا الذاكرة
 * Memory fragments counter
 */
@Composable
fun MemoryFragmentDisplay(
    count: Int,
    modifier: Modifier = Modifier
) {
    val pulseAnim = rememberInfiniteTransition()
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Row(
        modifier = modifier
            .background(
                color = ErytColor.SurfaceDark.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = ErytColor.BlightGold,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // MF Icon
        Canvas(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = if (count > 0) pulse else 1f
                    scaleY = if (count > 0) pulse else 1f
                }
        ) {
            drawMFIcon(this)
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = count.toString(),
            color = ErytColor.BlightGold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = "MF",
            color = ErytColor.OutlineGray,
            fontSize = 12.sp
        )
    }
}

/**
 * رسم أيقونة MF
 * Draw MF icon
 */
private fun drawMFIcon(drawScope: DrawScope) {
    with(drawScope) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.8f
        
        // Outer glow
        drawCircle(
            color = ErytColor.BlightGold.copy(alpha = 0.3f),
            radius = radius * 1.2f,
            center = center
        )
        
        // Main circle
        drawCircle(
            color = ErytColor.BlightGold,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Inner fragments
        for (i in 0 until 6) {
            val angle = (i * 60f).toRadians()
            val startX = center.x + cos(angle) * radius * 0.3f
            val startY = center.y + sin(angle) * radius * 0.3f
            val endX = center.x + cos(angle) * radius * 0.7f
            val endY = center.y + sin(angle) * radius * 0.7f
            
            drawLine(
                color = ErytColor.BlightGold,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Forgetfulness Meter
// ══════════════════════════════════════════════════════════════════════════════

/**
 * مقياس النسيان
 * Forgetfulness meter
 */
@Composable
fun ForgetfulnessMeter(
    fm: Int,
    modifier: Modifier = Modifier,
    maxFM: Int = 20
) {
    val dangerLevel = when {
        fm < 4 -> 0 // Safe
        fm < 8 -> 1 // Mild
        fm < 13 -> 2 // Moderate
        fm < 20 -> 3 // Critical
        else -> 4 // Catastrophic
    }
    
    val color = when (dangerLevel) {
        0 -> ErytColor.SuccessGreen
        1 -> ErytColor.EchoesBlue
        2 -> ErytColor.WarningOrange
        3 -> ErytColor.VitalityRed
        else -> ErytColor.DangerRed
    }
    
    val pulseAnim = rememberInfiniteTransition()
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (dangerLevel >= 3) 500 else 1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Row(
        modifier = modifier
            .background(
                color = ErytColor.SurfaceDark.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = color,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
            .graphicsLayer {
                if (dangerLevel >= 3) {
                    scaleX = pulse
                    scaleY = pulse
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // FM Icon
        Canvas(modifier = Modifier.size(24.dp)) {
            drawFMIcon(this, color, dangerLevel)
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = fm.toString(),
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = "FM",
            color = ErytColor.OutlineGray,
            fontSize = 12.sp
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Visual bars
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 0 until 5) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(3.dp)
                        .background(
                            color = if (i < dangerLevel) color else ErytColor.OutlineGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

/**
 * رسم أيقونة FM
 * Draw FM icon
 */
private fun drawFMIcon(drawScope: DrawScope, color: Color, dangerLevel: Int) {
    with(drawScope) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.7f
        
        // Spiral pattern (representing memory fading)
        val path = Path().apply {
            var angle = 0f
            var currentRadius = radius
            moveTo(center.x, center.y)
            
            while (currentRadius > 0) {
                val x = center.x + cos(angle.toRadians()) * currentRadius
                val y = center.y + sin(angle.toRadians()) * currentRadius
                lineTo(x, y)
                angle += 30f
                currentRadius -= radius / 12f
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Warning dots
        if (dangerLevel >= 3) {
            for (i in 0 until 3) {
                val dotAngle = (i * 120f).toRadians()
                val dotX = center.x + cos(dotAngle) * radius * 1.2f
                val dotY = center.y + sin(dotAngle) * radius * 1.2f
                drawCircle(
                    color = color,
                    radius = 2.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Boss Health Bar
// ══════════════════════════════════════════════════════════════════════════════

/**
 * شريط صحة الزعيم
 * Boss health bar
 */
@Composable
fun BossHealthBar(
    bossInfo: BossInfo,
    modifier: Modifier = Modifier
) {
    val healthPercent = bossInfo.currentHP / bossInfo.maxHP
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = ErytColor.VoidPrimary.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 2.dp,
                color = ErytColor.DangerRed,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Boss name
        Text(
            text = bossInfo.nameArabic,
            color = ErytColor.DangerRed,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Health bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ErytColor.SurfaceDark)
        ) {
            // Health fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(healthPercent)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ErytColor.DangerRed,
                                ErytColor.VitalityRed,
                                ErytColor.DangerRed
                            )
                        )
                    )
            )
            
            // Vulnerable indicator
            if (bossInfo.isVulnerable) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Yellow.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            
            // HP text
            Text(
                text = "${bossInfo.currentHP.toInt()} / ${bossInfo.maxHP.toInt()}",
                color = ErytColor.RadianceWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Phase indicator
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..bossInfo.maxPhases) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (i <= bossInfo.phase) ErytColor.DangerRed else ErytColor.OutlineGray,
                            shape = CircleShape
                        )
                )
                if (i < bossInfo.maxPhases) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Ability Cooldown Bar
// ══════════════════════════════════════════════════════════════════════════════

/**
 * حالة القدرة
 * Ability state
 */
data class AbilityState(
    val type: AbilityType,
    val isUnlocked: Boolean,
    val isOnCooldown: Boolean,
    val cooldownRemaining: Float, // 0.0 - 1.0
    val manaCost: Int,
    val hotkey: String? = null
)

/**
 * شريط مؤقتات القدرات
 * Ability cooldown display
 */
@Composable
fun AbilityCooldownBar(
    abilityStates: Map<AbilityType, AbilityState>,
    modifier: Modifier = Modifier,
    iconSize: Float = 1.0f,
    onAbilityClick: (AbilityType) -> Unit = {}
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        abilityStates.forEach { (type, state) ->
            if (state.isUnlocked) {
                AbilityIcon(
                    abilityState = state,
                    iconSize = (56.dp * iconSize),
                    onClick = { onAbilityClick(type) }
                )
            }
        }
    }
}

/**
 * أيقونة قدرة واحدة
 * Single ability icon
 */
@Composable
fun AbilityIcon(
    abilityState: AbilityState,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(iconSize)
            .shadow(4.dp, CircleShape)
            .background(
                color = ErytColor.SurfaceDark,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = if (abilityState.isOnCooldown) 
                    ErytColor.OutlineGray 
                else 
                    ErytColor.BlightGold,
                shape = CircleShape
            )
            .clickable(
                enabled = !abilityState.isOnCooldown,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ability icon (placeholder - would use actual icons)
        Text(
            text = getAbilityEmoji(abilityState.type),
            fontSize = (iconSize.value * 0.5f).sp
        )
        
        // Cooldown overlay
        if (abilityState.isOnCooldown) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sweepAngle = 360f * (1f - abilityState.cooldownRemaining)
                drawArc(
                    color = Color.Black.copy(alpha = 0.7f),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = this.size
                )
            }
            
            Text(
                text = "${(abilityState.cooldownRemaining * 10).toInt()}",
                color = ErytColor.RadianceWhite,
                fontSize = (iconSize.value * 0.3f).sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Mana cost
        if (!abilityState.isOnCooldown && abilityState.manaCost > 0) {
            Text(
                text = abilityState.manaCost.toString(),
                color = ErytColor.EchoesBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-4).dp)
            )
        }
        
        // Hotkey
        abilityState.hotkey?.let {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                color = ErytColor.VoidPrimary,
                shape = CircleShape
            ) {
                Text(
                    text = it,
                    color = ErytColor.RadianceWhite,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

/**
 * الحصول على إيموجي القدرة
 * Get ability emoji (placeholder)
 */
private fun getAbilityEmoji(type: AbilityType): String {
    return when (type) {
        AbilityType.DASH -> "⚡"
        AbilityType.WALL_JUMP -> "🧗"
        else -> "?"
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Item Slot Bar
// ══════════════════════════════════════════════════════════════════════════════

/**
 * شريط فتحات الأدوات
 * Quick item slots
 */
@Composable
fun ItemSlotBar(
    playerState: PlayerState,
    modifier: Modifier = Modifier,
    iconSize: Float = 1.0f,
    maxSlots: Int = 4
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until maxSlots) {
            ItemSlot(
                itemId = playerState.equippedAccessories.getOrNull(i),
                slotNumber = i + 1,
                size = (56.dp * iconSize)
            )
        }
    }
}

/**
 * فتحة عنصر واحد
 * Single item slot
 */
@Composable
fun ItemSlot(
    itemId: String?,
    slotNumber: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .background(
                color = ErytColor.SurfaceDark,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = ErytColor.OutlineGray,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (itemId != null) {
            // Item icon (placeholder)
            Text(
                text = "🎒",
                fontSize = (size.value * 0.5f).sp
            )
        } else {
            // Empty slot
            Text(
                text = slotNumber.toString(),
                color = ErytColor.OutlineGray.copy(alpha = 0.5f),
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Minimap
// ══════════════════════════════════════════════════════════════════════════════

/**
 * خريطة مصغرة مع بوصلة
 * Minimap with compass
 */
@Composable
fun Minimap(
    currentRegion: RegionType,
    playerPosition: Offset,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        )
    )
    
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .background(
                color = ErytColor.SurfaceDark.copy(alpha = 0.9f),
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = ErytColor.BlightGold,
                shape = CircleShape
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = this.size.minDimension / 2 * 0.8f
            
            // Region background
            drawCircle(
                color = getRegionColor(currentRegion).copy(alpha = 0.3f),
                radius = radius,
                center = center
            )
            
            // Compass rose
            rotate(rotation, pivot = center) {
                for (i in 0 until 4) {
                    val angle = (i * 90f).toRadians()
                    val startRadius = radius * 0.6f
                    val endRadius = radius * 0.9f
                    
                    drawLine(
                        color = if (i == 0) ErytColor.DangerRed else ErytColor.OutlineGray,
                        start = Offset(
                            center.x + cos(angle) * startRadius,
                            center.y + sin(angle) * startRadius
                        ),
                        end = Offset(
                            center.x + cos(angle) * endRadius,
                            center.y + sin(angle) * endRadius
                        ),
                        strokeWidth = if (i == 0) 3.dp.toPx() else 1.dp.toPx()
                    )
                }
            }
            
            // Player marker
            drawCircle(
                color = ErytColor.BlightGold,
                radius = 4.dp.toPx(),
                center = center
            )
            drawCircle(
                color = ErytColor.RadianceWhite,
                radius = 2.dp.toPx(),
                center = center
            )
        }
        
        // Region name
        Text(
            text = getRegionAbbreviation(currentRegion),
            color = ErytColor.BlightGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

/**
 * الحصول على لون المنطقة
 * Get region color
 */
private fun getRegionColor(region: RegionType): Color {
    return when (region) {
        RegionType.ASHEN_SPRAWL -> Color(0xFFFF6347)
        RegionType.VEILED_ARCHIVES -> Color(0xFF4A90E2)
        RegionType.HOLLOWED_ARCHIPELAGO -> Color(0xFF87CEEB)
        RegionType.GLASSFJORD_CLIFFS -> Color(0xFFB0E0E6)
        RegionType.SUNKEN_CLOCKWORKS -> Color(0xFFCD853F)
        RegionType.BLACKROOT_MOORLANDS -> Color(0xFF6B8E23)
        RegionType.LUMINOUS_CHASM -> Color(0xFF9370DB)
    }
}

/**
 * الحصول على اختصار المنطقة
 * Get region abbreviation
 */
private fun getRegionAbbreviation(region: RegionType): String {
    return when (region) {
        RegionType.ASHEN_SPRAWL -> "AS"
        RegionType.VEILED_ARCHIVES -> "VA"
        RegionType.HOLLOWED_ARCHIPELAGO -> "HA"
        RegionType.GLASSFJORD_CLIFFS -> "GC"
        RegionType.SUNKEN_CLOCKWORKS -> "SC"
        RegionType.BLACKROOT_MOORLANDS -> "BM"
        RegionType.LUMINOUS_CHASM -> "LC"
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Quest Tracker
// ══════════════════════════════════════════════════════════════════════════════

/**
 * متتبع المهام
 * Active quest tracker
 */
@Composable
fun QuestTracker(
    quests: List<QuestTrackerInfo>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
    onQuestClick: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .width(250.dp)
            .background(
                color = ErytColor.SurfaceDark.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = ErytColor.BlightGold,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "المهام النشطة",
            color = ErytColor.BlightGold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        quests.take(maxVisible).forEach { quest ->
            QuestTrackerItem(
                quest = quest,
                onClick = { onQuestClick(quest.questId) }
            )
        }
        
        if (quests.size > maxVisible) {
            Text(
                text = "+${quests.size - maxVisible} أخرى",
                color = ErytColor.OutlineGray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * عنصر مهمة واحد
 * Single quest tracker item
 */
@Composable
fun QuestTrackerItem(
    quest: QuestTrackerInfo,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = ErytColor.VoidPrimary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = quest.titleArabic,
                color = ErytColor.RadianceWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            if (quest.isOptional) {
                Spacer(modifier = Modifier.width(4.dp))
                ErytBadge(
                    text = "اختياري",
                    color = ErytColor.EchoesBlue
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = quest.currentObjectiveArabic,
            color = ErytColor.OutlineGray,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        if (quest.progress > 0f) {
            Spacer(modifier = Modifier.height(4.dp))
            ErytProgressBar(
                progress = quest.progress,
                height = 4.dp,
                foregroundColor = ErytColor.BlightGold
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Combo Counter
// ══════════════════════════════════════════════════════════════════════════════

/**
 * عداد الضربات المتتالية
 * Combo hit counter
 */
@Composable
fun ComboCounter(
    count: Int,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (count > 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            color = ErytColor.BlightGold,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = ErytColor.VoidPrimary,
                    offset = Offset(4f, 4f),
                    blurRadius = 8f
                )
            )
        )
        
        Text(
            text = "COMBO",
            color = ErytColor.RadianceWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = ErytColor.VoidPrimary,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Notification Stack
// ══════════════════════════════════════════════════════════════════════════════

/**
 * مكدس الإشعارات
 * Notification stack
 */
@Composable
fun NotificationStack(
    notifications: List<HUDNotification>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        notifications
            .filter { !it.isExpired() }
            .take(maxVisible)
            .forEach { notification ->
                HUDNotificationItem(notification = notification)
            }
    }
}

/**
 * عنصر إشعار واحد
 * Single HUD notification
 */
@Composable
fun HUDNotificationItem(
    notification: HUDNotification,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (notification.isExpired()) 0f else 1f,
        animationSpec = tween(durationMillis = 300)
    )
    
    ErytNotification(
        message = notification.messageArabic,
        type = notification.type,
        modifier = modifier
            .width(300.dp)
            .alpha(alpha)
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Helper Extensions
// ══════════════════════════════════════════════════════════════════════════════

/**
 * تحويل الدرجات إلى راديان
 * Convert degrees to radians
 */
private fun Float.toRadians(): Float = this * (Math.PI.toFloat() / 180f)