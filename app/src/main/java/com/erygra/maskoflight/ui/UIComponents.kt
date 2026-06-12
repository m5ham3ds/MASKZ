package com.erygra.maskoflight.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * # مكونات واجهة المستخدم الأساسية (Core UI Components)
 * 
 * مكتبة شاملة لمكونات UI قابلة لإعادة الاستخدام:
 * - Buttons (أزرار بأنماط متعددة)
 * - Panels & Cards (لوحات وبطاقات)
 * - Progress Bars (أشرطة التقدم)
 * - Health/Energy Bars (أشرطة الصحة والطاقة)
 * - Dialogs & Tooltips (حوارات وتلميحات)
 * - Icons & Badges (أيقونات وشارات)
 * - Notifications (إشعارات)
 * 
 * جميع المكونات تدعم:
 * - RTL (Right-to-Left)
 * - Dark theme
 * - Animations
 * - Accessibility
 * 
 * @author Erygra Development Team
 * @version 2.0
 * @since 2025-01-09
 */

// ══════════════════════════════════════════════════════════════════════════════
// Theme Colors
// ══════════════════════════════════════════════════════════════════════════════

object ErytColor {
    val VoidPrimary = Color(0xFF1A1A2E)
    val SurfaceDark = Color(0xFF16213E)
    val SurfaceLight = Color(0xFF2B3A5A)
    val TextPrimary = Color(0xFFE0E0E0)
    val BlightGold = Color(0xFFD4AF37)
    val BlightRed = Color(0xFFE53935)
    val MemoryAqua = Color(0xFF00BFA5)
    val EchoesBlue = Color(0xFF4A90E2)
    val VitalityRed = Color(0xFFE74C3C)
    val RadianceWhite = Color(0xFFF8F9FA)
    val OutlineGray = Color(0xFF6C757D)
    val SuccessGreen = Color(0xFF27AE60)
    val WarningOrange = Color(0xFFF39C12)
    val DangerRed = Color(0xFFC0392B)
    val PoisonGreen = Color(0xFF9ACD32)
    val ElectricCyan = Color(0xFF00FFFF)
    val FrostBlue = Color(0xFF87CEEB)
}

// ══════════════════════════════════════════════════════════════════════════════
// Buttons
// ══════════════════════════════════════════════════════════════════════════════

enum class ErytButtonVariant {
    PRIMARY,
    SECONDARY,
    DANGER
}

/**
 * زر أساسي مخصص
 * Custom primary button
 */
@Composable
fun ErytButton(
    text: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ErytButtonVariant = ErytButtonVariant.PRIMARY,
    color: Color = when(variant) {
        ErytButtonVariant.PRIMARY -> ErytColor.BlightGold
        ErytButtonVariant.SECONDARY -> ErytColor.SurfaceLight
        ErytButtonVariant.DANGER -> Color(0xFFC62828)
    },
    textColor: Color = when(variant) {
        ErytButtonVariant.PRIMARY -> ErytColor.VoidPrimary
        ErytButtonVariant.SECONDARY -> ErytColor.TextPrimary
        ErytButtonVariant.DANGER -> Color.White
    },
    icon: (@Composable () -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (enabled) color else color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (content != null) {
                CompositionLocalProvider(LocalContentColor provides (if (enabled) textColor else textColor.copy(alpha = 0.5f))) {
                    content()
                }
            } else {
                icon?.let {
                    it()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * زر ثانوي (outline)
 * Secondary outline button
 */
@Composable
fun ErytOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = ErytColor.BlightGold,
    textColor: Color = ErytColor.BlightGold
) {
    Box(
        modifier = modifier
            .border(
                width = 2.dp,
                color = if (enabled) borderColor else borderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * زر أيقونة دائري
 * Icon button (circular)
 */
@Composable
fun ErytIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = ErytColor.SurfaceDark,
    contentColor: Color = ErytColor.RadianceWhite,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .background(
                color = if (enabled) color else color.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Panels & Cards
// ══════════════════════════════════════════════════════════════════════════════

/**
 * لوحة مخصصة
 * Custom panel
 */
@Composable
fun ErytPanel(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ErytColor.SurfaceDark,
    borderColor: Color = ErytColor.OutlineGray,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.shadow(elevation, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

/**
 * بطاقة عنصر
 * Item card
 */
@Composable
fun ErytItemCard(
    title: String,
    description: String?,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ErytColor.SurfaceDark),
        border = BorderStroke(1.dp, ErytColor.OutlineGray)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Box(modifier = Modifier.size(48.dp)) {
                    it()
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = ErytColor.RadianceWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    badge?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        ErytBadge(text = it, color = ErytColor.BlightGold)
                    }
                }
                
                description?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = ErytColor.OutlineGray,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Progress Bars
// ══════════════════════════════════════════════════════════════════════════════

/**
 * شريط تقدم أفقي
 * Horizontal progress bar
 */
@Composable
fun ErytProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ErytColor.VoidPrimary,
    foregroundColor: Color = ErytColor.BlightGold,
    height: Dp = 8.dp,
    showText: Boolean = false,
    textFormat: (Float) -> String = { "${(it * 100).toInt()}%" }
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300)
    )
    
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(height / 2))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                foregroundColor,
                                foregroundColor.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        }
        
        if (showText) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = textFormat(animatedProgress),
                color = ErytColor.OutlineGray,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * شريط صحة/طاقة متقدم
 * Advanced health/energy bar
 */
@Composable
fun ErytStatBar(
    current: Float,
    max: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: (@Composable () -> Unit)? = null,
    color: Color = ErytColor.VitalityRed,
    backgroundColor: Color = ErytColor.VoidPrimary,
    height: Dp = 24.dp,
    showValue: Boolean = true,
    animated: Boolean = true
) {
    val progress = (current / max).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress else progress,
        animationSpec = tween(durationMillis = 300)
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Box(modifier = Modifier.size(height)) {
                it()
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        label?.let {
            Text(
                text = it,
                color = ErytColor.RadianceWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(height / 2)
                )
        ) {
            // Background glow effect
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(height / 2))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.3f),
                                color,
                                color.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            
            if (showValue) {
                Text(
                    text = "${current.toInt()} / ${max.toInt()}",
                    color = ErytColor.RadianceWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Dialogs & Tooltips
// ══════════════════════════════════════════════════════════════════════════════

/**
 * حوار مخصص
 * Custom dialog
 */
@Composable
fun ErytDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = ErytColor.BlightGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                content()
            }
        },
        confirmButton = {
            confirmButton?.invoke() ?: ErytButton(
                text = "تأكيد",
                onClick = onDismiss
            )
        },
        dismissButton = dismissButton,
        containerColor = ErytColor.SurfaceDark,
        modifier = modifier
    )
}

/**
 * تلميح أداة
 * Tooltip
 */
@Composable
fun ErytTooltip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ErytColor.VoidPrimary,
    textColor: Color = ErytColor.RadianceWhite
) {
    Surface(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(4.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Icons & Badges
// ══════════════════════════════════════════════════════════════════════════════

/**
 * شارة
 * Badge
 */
@Composable
fun ErytBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ErytColor.BlightGold,
    textColor: Color = ErytColor.VoidPrimary
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * أيقونة دائرية مع عداد
 * Circular icon with counter
 */
@Composable
fun ErytCounterIcon(
    count: Int,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    counterColor: Color = ErytColor.VitalityRed
) {
    Box(modifier = modifier) {
        icon()
        
        if (count > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                color = counterColor,
                shape = CircleShape
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    color = ErytColor.RadianceWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Notifications
// ══════════════════════════════════════════════════════════════════════════════

/**
 * نوع الإشعار
 * Notification type
 */
enum class NotificationType {
    INFO, SUCCESS, WARNING, DANGER
}

/**
 * إشعار منبثق
 * Toast notification
 */
@Composable
fun ErytNotification(
    message: String,
    type: NotificationType = NotificationType.INFO,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val backgroundColor = when (type) {
        NotificationType.INFO -> ErytColor.EchoesBlue
        NotificationType.SUCCESS -> ErytColor.SuccessGreen
        NotificationType.WARNING -> ErytColor.WarningOrange
        NotificationType.DANGER -> ErytColor.DangerRed
    }
    
    Surface(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(8.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = ErytColor.RadianceWhite,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            
            onDismiss?.let {
                Spacer(modifier = Modifier.width(8.dp))
                ErytIconButton(
                    onClick = it,
                    color = Color.Transparent,
                    size = 24.dp
                ) {
                    Text(
                        text = "✕",
                        color = ErytColor.RadianceWhite,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}