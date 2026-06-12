package com.erygra.maskoflight.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
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
 * حالة الحوار
 * Dialogue State
 */
data class DialogueState(
    val currentNode: DialogueNode,
    val speaker: DialogueSpeaker,
    val history: List<DialogueHistoryEntry>,
    val isAutoPlay: Boolean,
    val canSkip: Boolean,
    val backgroundImage: String?
)

/**
 * عقدة حوار
 * Dialogue Node
 */
data class DialogueNode(
    val id: String,
    val textEn: String,
    val textAr: String,
    val speaker: DialogueSpeaker,
    val expression: SpeakerExpression,
    val voiceFile: String?,
    val choices: List<DialogueChoice>,
    val effects: List<DialogueEffect>,
    val conditions: List<DialogueCondition>,
    val nextNodeId: String?,
    val isThought: Boolean = false
)

/**
 * المتحدث
 * Dialogue Speaker
 */
data class DialogueSpeaker(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val portraitPath: String,
    val color: Color
)

/**
 * تعبيرات الشخصية
 * Speaker Expressions
 */
enum class SpeakerExpression(val namePart: String) {
    NEUTRAL("neutral"),
    HAPPY("happy"),
    SAD("sad"),
    ANGRY("angry"),
    SURPRISED("surprised"),
    WORRIED("worried"),
    DETERMINED("determined"),
    CONFUSED("confused")
}

/**
 * خيار حوار
 * Dialogue Choice
 */
data class DialogueChoice(
    val id: String,
    val textEn: String,
    val textAr: String,
    val nextNodeId: String,
    val conditions: List<DialogueCondition>,
    val effects: List<DialogueEffect>,
    val icon: ImageVector? = null,
    val color: Color? = null,
    val isDisabled: Boolean = false,
    val disabledReason: String? = null
)

/**
 * شروط الحوار
 * Dialogue Conditions
 */
sealed class DialogueCondition {
    data class HasMemoryFragment(val fragmentId: String) : DialogueCondition()
    data class QuestCompleted(val questId: String) : DialogueCondition()
    data class QuestActive(val questId: String) : DialogueCondition()
    data class HasItem(val itemId: String, val quantity: Int = 1) : DialogueCondition()
    data class MinLevel(val level: Int) : DialogueCondition()
    data class FlagSet(val flagId: String, val value: Boolean = true) : DialogueCondition()
    data class RelationshipLevel(val npcId: String, val minLevel: Int) : DialogueCondition()
}

/**
 * تأثيرات الحوار
 * Dialogue Effects
 */
sealed class DialogueEffect {
    data class GrantItem(val itemId: String, val quantity: Int) : DialogueEffect()
    data class GrantMF(val amount: Int) : DialogueEffect()
    data class GrantXP(val amount: Int) : DialogueEffect()
    data class StartQuest(val questId: String) : DialogueEffect()
    data class CompleteQuest(val questId: String) : DialogueEffect()
    data class SetFlag(val flagId: String, val value: Boolean) : DialogueEffect()
    data class ChangeRelationship(val npcId: String, val amount: Int) : DialogueEffect()
    data class PlaySound(val soundId: String) : DialogueEffect()
    data class ScreenShake(val intensity: Float) : DialogueEffect()
    data class FlashScreen(val color: Color, val duration: Long) : DialogueEffect()
    data class ChangeMusic(val musicId: String) : DialogueEffect()
}

/**
 * مدخل سجل الحوار
 * Dialogue History Entry
 */
data class DialogueHistoryEntry(
    val speaker: DialogueSpeaker,
    val textEn: String,
    val textAr: String,
    val timestamp: Long,
    val isChoice: Boolean = false
)

// ════════════════════════════════════════════════════════════════════════════════════
// Main Dialogue Screen - الشاشة الرئيسية للحوار
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شاشة الحوار الرئيسية
 * Main Dialogue Screen
 * 
 * @param dialogueState حالة الحوار الحالية
 * @param onChoiceSelected عند اختيار خيار
 * @param onAdvance عند التقدم للعقدة التالية
 * @param onSkip عند تخطي الحوار
 * @param onToggleAutoPlay تبديل التشغيل التلقائي
 * @param onShowHistory عرض سجل الحوار
 * @param onClose إغلاق الحوار
 */
@Composable
fun DialogueScreen(
    dialogueState: DialogueState,
    onChoiceSelected: (String) -> Unit,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onShowHistory: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Typewriter effect state
    var displayedText by remember(dialogueState.currentNode.id) { mutableStateOf("") }
    var isTyping by remember(dialogueState.currentNode.id) { mutableStateOf(true) }
    
    // Screen effects
    var screenShakeOffset by remember { mutableStateOf(Offset.Zero) }
    var flashColor by remember { mutableStateOf<Color?>(null) }
    
    // Show history dialog
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Typewriter effect
    LaunchedEffect(dialogueState.currentNode.id) {
        displayedText = ""
        isTyping = true
        val fullText = dialogueState.currentNode.textAr
        
        fullText.forEachIndexed { index, char ->
            if (!isTyping) return@LaunchedEffect
            displayedText += char
            delay(30) // Typing speed
            
            // Sound effect for typing (every 3 chars)
            if (index % 3 == 0) {
                AudioEngine.playSFX("dialogue_type")
            }
        }
        
        isTyping = false
        
        // Auto-advance if auto-play is enabled and no choices
        if (dialogueState.isAutoPlay && dialogueState.currentNode.choices.isEmpty()) {
            delay(2000)
            onAdvance()
        }
    }

    // Process effects
    LaunchedEffect(dialogueState.currentNode.id) {
        dialogueState.currentNode.effects.forEach { effect ->
            when (effect) {
                is DialogueEffect.ScreenShake -> {
                    repeat(10) {
                        screenShakeOffset = Offset(
                            x = (kotlin.random.Random.nextFloat() * 2 - 1) * effect.intensity,
                            y = (kotlin.random.Random.nextFloat() * 2 - 1) * effect.intensity
                        )
                        delay(50)
                    }
                    screenShakeOffset = Offset.Zero
                }
                is DialogueEffect.FlashScreen -> {
                    flashColor = effect.color
                    delay(effect.duration)
                    flashColor = null
                }
                is DialogueEffect.PlaySound -> {
                    AudioEngine.playSFX(effect.soundId)
                }
                is DialogueEffect.ChangeMusic -> {
                    AudioEngine.instance?.playRegionMusic(effect.musicId)
                }
                else -> {
                    // Other effects handled by game logic
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset(screenShakeOffset.x.dp, screenShakeOffset.y.dp)
    ) {
        // Background
        dialogueState.backgroundImage?.let { bgPath ->
            // TODO: Load background image from path
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ErytColor.VoidPrimary.copy(alpha = 0.3f))
            )
        }

        // Dim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Character Portrait & Name
            AnimatedVisibility(
                visible = !dialogueState.currentNode.isThought,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            ) {
                CharacterPortrait(
                    speaker = dialogueState.speaker,
                    expression = dialogueState.currentNode.expression,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(120.dp)
                )
            }

            // Dialogue Box
            DialogueBox(
                node = dialogueState.currentNode,
                displayedText = displayedText,
                isTyping = isTyping,
                isAutoPlay = dialogueState.isAutoPlay,
                canSkip = dialogueState.canSkip,
                onAdvance = {
                    if (isTyping) {
                        isTyping = false
                        displayedText = dialogueState.currentNode.textAr
                    } else {
                        onAdvance()
                    }
                },
                onToggleAutoPlay = onToggleAutoPlay,
                onShowHistory = { showHistoryDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            // Dialogue Choices
            AnimatedVisibility(
                visible = !isTyping && dialogueState.currentNode.choices.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                DialogueChoices(
                    choices = dialogueState.currentNode.choices,
                    onChoiceSelected = { choiceId ->
                        scope.launch {
                            AudioEngine.playSFX("dialogue_choice")
                        }
                        onChoiceSelected(choiceId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }

        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            // History Button
            IconButton(
                onClick = { showHistoryDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .background(ErytColor.SurfaceDark.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Show History",
                    tint = ErytColor.RadianceWhite
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Skip Button
            if (dialogueState.canSkip) {
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .size(40.dp)
                        .background(ErytColor.SurfaceDark.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Skip",
                        tint = ErytColor.RadianceWhite
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .background(ErytColor.SurfaceDark.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = ErytColor.RadianceWhite
                )
            }
        }

        // Screen Flash Effect
        flashColor?.let { color ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.5f))
                    .zIndex(100f)
            )
        }

        // History Dialog
        if (showHistoryDialog) {
            DialogueHistoryDialog(
                history = dialogueState.history,
                onDismiss = { showHistoryDialog = false }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Character Portrait - صورة الشخصية
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * صورة وتعبير الشخصية
 * Character Portrait with Expression
 */
@Composable
private fun CharacterPortrait(
    speaker: DialogueSpeaker,
    expression: SpeakerExpression,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Portrait
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            speaker.color.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
                .border(3.dp, speaker.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // TODO: Load portrait image from speaker.portraitPath + expression.namePart
            // Placeholder icon
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = speaker.nameAr,
                tint = speaker.color,
                modifier = Modifier.size(80.dp)
            )
        }

        // Name
        Text(
            text = speaker.nameAr,
            style = MaterialTheme.typography.titleMedium,
            color = speaker.color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(
                    ErytColor.SurfaceDark.copy(alpha = 0.8f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Dialogue Box - صندوق الحوار
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * صندوق الحوار الرئيسي
 * Main Dialogue Box
 */
@Composable
private fun DialogueBox(
    node: DialogueNode,
    displayedText: String,
    isTyping: Boolean,
    isAutoPlay: Boolean,
    canSkip: Boolean,
    onAdvance: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boxColor = if (node.isThought) {
        Color(0xFF2C3E50)
    } else {
        ErytColor.SurfaceDark
    }

    val borderColor = if (node.isThought) {
        Color(0xFF3498DB)
    } else {
        node.speaker.color
    }

    ErytPanel(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 300.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures {
                    onAdvance()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Text Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = displayedText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp
                    ),
                    color = ErytColor.RadianceWhite,
                    fontStyle = if (node.isThought) FontStyle.Italic else FontStyle.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )

                // Typing indicator
                if (isTyping) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        TypingIndicator()
                    }
                }
            }

            // Bottom Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thought indicator
                if (node.isThought) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFF3498DB),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "فكرة داخلية",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF3498DB)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Auto-play toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleAutoPlay,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isAutoPlay) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = if (isAutoPlay) "Disable Auto-Play" else "Enable Auto-Play",
                            tint = if (isAutoPlay) ErytColor.BlightGold else ErytColor.OutlineGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Continue indicator
                    if (!isTyping && node.choices.isEmpty()) {
                        val alpha by rememberInfiniteTransition(label = "continue").animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "continue_alpha"
                        )

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Continue",
                            tint = ErytColor.MemoryAqua.copy(alpha = alpha),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * مؤشر الكتابة
 * Typing Indicator
 */
@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ErytColor.MemoryAqua.copy(alpha = alpha), CircleShape)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Dialogue Choices - خيارات الحوار
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * قائمة خيارات الحوار
 * Dialogue Choices List
 */
@Composable
private fun DialogueChoices(
    choices: List<DialogueChoice>,
    onChoiceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        choices.forEach { choice ->
            DialogueChoiceButton(
                choice = choice,
                onClick = { onChoiceSelected(choice.id) }
            )
        }
    }
}

/**
 * زر خيار حوار واحد
 * Single Dialogue Choice Button
 */
@Composable
private fun DialogueChoiceButton(
    choice: DialogueChoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered && !choice.isDisabled) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    val backgroundColor = when {
        choice.isDisabled -> ErytColor.OutlineGray.copy(alpha = 0.3f)
        choice.color != null -> choice.color.copy(alpha = 0.2f)
        else -> ErytColor.SurfaceDark.copy(alpha = 0.8f)
    }

    val borderColor = when {
        choice.isDisabled -> ErytColor.OutlineGray
        choice.color != null -> choice.color
        else -> ErytColor.MemoryAqua
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                enabled = !choice.isDisabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            choice.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (choice.isDisabled) ErytColor.OutlineGray else borderColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = choice.textAr,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (choice.isDisabled) ErytColor.OutlineGray else ErytColor.RadianceWhite,
                    fontWeight = if (choice.isDisabled) FontWeight.Normal else FontWeight.Medium
                )

                // Disabled reason
                if (choice.isDisabled && choice.disabledReason != null) {
                    Text(
                        text = "🔒 ${choice.disabledReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErytColor.BlightRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Arrow
            if (!choice.isDisabled) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Dialogue History Dialog - حوار سجل الحوارات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * حوار سجل الحوارات السابقة
 * Dialogue History Dialog
 */
@Composable
private fun DialogueHistoryDialog(
    history: List<DialogueHistoryEntry>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ErytPanel(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dialogue History",
                            style = MaterialTheme.typography.titleLarge,
                            color = ErytColor.RadianceWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "سجل الحوارات",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ErytColor.RadianceWhite
                        )
                    }
                }

                Divider(color = ErytColor.OutlineGray)

                // History List
                val listState = rememberLazyListState()
                
                LaunchedEffect(Unit) {
                    // Auto-scroll to bottom (latest entry)
                    if (history.isNotEmpty()) {
                        listState.scrollToItem(history.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(
                        items = history,
                        key = { it.timestamp }
                    ) { entry ->
                        DialogueHistoryEntry(entry = entry)
                    }
                }
            }
        }
    }
}

/**
 * مدخل واحد في سجل الحوار
 * Single Dialogue History Entry
 */
@Composable
private fun DialogueHistoryEntry(
    entry: DialogueHistoryEntry,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (entry.isChoice) {
                    ErytColor.BlightGold.copy(alpha = 0.1f)
                } else {
                    ErytColor.SurfaceDark.copy(alpha = 0.5f)
                },
                RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (entry.isChoice) ErytColor.BlightGold else entry.speaker.color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Speaker indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(entry.speaker.color.copy(alpha = 0.3f))
                .border(2.dp, entry.speaker.color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (entry.isChoice) Icons.Default.Person else Icons.Default.AccountCircle,
                contentDescription = null,
                tint = entry.speaker.color,
                modifier = Modifier.size(24.dp)
            )
        }

        // Content
        Column(modifier = Modifier.weight(1f)) {
            // Speaker name
            Text(
                text = if (entry.isChoice) "⚡ اختيار اللاعب" else entry.speaker.nameAr,
                style = MaterialTheme.typography.labelMedium,
                color = if (entry.isChoice) ErytColor.BlightGold else entry.speaker.color,
                fontWeight = FontWeight.Bold
            )

            // Text
            Text(
                text = entry.textAr,
                style = MaterialTheme.typography.bodyMedium,
                color = ErytColor.RadianceWhite,
                fontStyle = if (entry.isChoice) FontStyle.Italic else FontStyle.Normal,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Dialogue Quick Message - رسالة سريعة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * رسالة حوار سريعة (للإشعارات)
 * Quick Dialogue Message (for notifications)
 */
@Composable
fun QuickDialogueMessage(
    message: String,
    speaker: DialogueSpeaker,
    duration: Long = 3000,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(duration)
        isVisible = false
        delay(300) // Animation duration
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ErytColor.SurfaceDark.copy(alpha = 0.95f),
                            ErytColor.SurfaceDark.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(2.dp, speaker.color, RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speaker icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(speaker.color.copy(alpha = 0.3f))
                    .border(2.dp, speaker.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = speaker.color,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = speaker.nameAr,
                    style = MaterialTheme.typography.labelMedium,
                    color = speaker.color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Dialogue System Integration - تكامل نظام الحوار
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * معاينة خيار الحوار (للتطوير)
 * Dialogue Choice Preview (for development)
 */
@Composable
fun DialogueChoicePreview(
    choice: DialogueChoice,
    modifier: Modifier = Modifier
) {
    DialogueChoiceButton(
        choice = choice,
        onClick = {},
        modifier = modifier
    )
}

/**
 * مساعد لإنشاء حوار بسيط
 * Helper to create simple dialogue
 */
object DialogueHelper {
    /**
     * إنشاء متحدث افتراضي
     */
    fun createDefaultSpeaker(
        id: String,
        nameEn: String,
        nameAr: String,
        color: Color = ErytColor.MemoryAqua
    ) = DialogueSpeaker(
        id = id,
        nameEn = nameEn,
        nameAr = nameAr,
        portraitPath = "portraits/$id",
        color = color
    )

    /**
     * إنشاء عقدة حوار بسيطة
     */
    fun createSimpleNode(
        id: String,
        textEn: String,
        textAr: String,
        speaker: DialogueSpeaker,
        nextNodeId: String? = null
    ) = DialogueNode(
        id = id,
        textEn = textEn,
        textAr = textAr,
        speaker = speaker,
        expression = SpeakerExpression.NEUTRAL,
        voiceFile = null,
        choices = emptyList(),
        effects = emptyList(),
        conditions = emptyList(),
        nextNodeId = nextNodeId
    )

    /**
     * إنشاء خيار حوار بسيط
     */
    fun createSimpleChoice(
        id: String,
        textEn: String,
        textAr: String,
        nextNodeId: String
    ) = DialogueChoice(
        id = id,
        textEn = textEn,
        textAr = textAr,
        nextNodeId = nextNodeId,
        conditions = emptyList(),
        effects = emptyList()
    )
}

// ════════════════════════════════════════════════════════════════════════════════════
// Dialogue Animations - رسوم متحركة للحوار
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * تأثير نبض للنص المهم
 * Pulse effect for important text
 */
@Composable
fun PulsingText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.scale(scale)
    )
}

/**
 * تأثير توهج للنص
 * Glow effect for text
 */
fun Modifier.textGlow(color: Color, blurRadius: Dp = 8.dp): Modifier {
    return this.drawBehind {
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = blurRadius.toPx(),
            center = center
        )
    }
}