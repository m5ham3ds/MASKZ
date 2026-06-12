package com.erygra.maskoflight.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.engine.AudioEngine
import com.erygra.maskoflight.player.SkillBranch
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ════════════════════════════════════════════════════════════════════════════════════
// Data Classes - فئات البيانات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * معلومات شجرة المهارات
 * Skill Tree Information
 */
data class SkillTreeState(
    val availablePoints: Int,
    val usedPoints: Int,
    val totalMF: Int,
    val skills: Map<String, SkillNodeState>
)

/**
 * حالة مهارة واحدة
 * Single Skill Node State
 */
data class SkillNodeState(
    val id: String,
    val branch: SkillBranch,
    val nameEn: String,
    val nameAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val icon: ImageVector,
    val currentLevel: Int,
    val maxLevel: Int,
    val isUnlocked: Boolean,
    val canUpgrade: Boolean,
    val upgradeCost: Int,
    val prerequisites: List<String>,
    val position: Offset, // موقع العقدة في الشجرة
    val effects: List<SkillEffect>
)

/**
 * تأثيرات المهارات
 * Skill Effects
 */
sealed class SkillEffect {
    data class DamageBoost(val percentage: Float) : SkillEffect()
    data class DefenseBoost(val percentage: Float) : SkillEffect()
    data class HPBoost(val amount: Float) : SkillEffect()
    data class EnergyBoost(val amount: Float) : SkillEffect()
    data class SpeedBoost(val percentage: Float) : SkillEffect()
    data class CritChanceBoost(val percentage: Float) : SkillEffect()
    data class UnlockAbility(val abilityName: String) : SkillEffect()
    data class ReduceCost(val percentage: Float) : SkillEffect()
}

/**
 * ألوان الفروع
 * Branch Colors
 */
object SkillBranchColors {
    val Radiance = Color(0xFFFFD700) // ذهبي
    val Memory = Color(0xFF00CED1) // أكوا
    val Void = Color(0xFF8B00FF) // بنفسجي
}

// ════════════════════════════════════════════════════════════════════════════════════
// Main Skill Tree Screen - الشاشة الرئيسية لشجرة المهارات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * شاشة شجرة المهارات الرئيسية
 * Main Skill Tree Screen
 * 
 * @param treeState حالة شجرة المهارات
 * @param selectedBranch الفرع المختار
 * @param onBranchChange عند تغيير الفرع
 * @param onUpgradeSkill عند ترقية مهارة
 * @param onRespec عند إعادة توزيع النقاط
 * @param onClose عند الإغلاق
 */
@Composable
fun SkillTreeScreen(
    treeState: SkillTreeState,
    selectedBranch: SkillBranch,
    onBranchChange: (SkillBranch) -> Unit,
    onUpgradeSkill: (String) -> Unit,
    onRespec: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedSkillId by remember { mutableStateOf<String?>(null) }
    var showRespecDialog by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val branchSkills = remember(treeState.skills, selectedBranch) {
        treeState.skills.values.filter { it.branch == selectedBranch }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ErytColor.VoidPrimary.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            SkillTreeHeader(
                availablePoints = treeState.availablePoints,
                usedPoints = treeState.usedPoints,
                totalMF = treeState.totalMF,
                onRespec = { showRespecDialog = true },
                onClose = onClose,
                modifier = Modifier.fillMaxWidth()
            )

            // Branch Selector
            BranchSelector(
                selectedBranch = selectedBranch,
                onBranchSelect = { branch ->
                    onBranchChange(branch)
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
                // Tree Canvas
                Box(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    SkillTreeCanvas(
                        skills = branchSkills,
                        selectedSkillId = selectedSkillId,
                        branch = selectedBranch,
                        scale = scale,
                        offset = offset,
                        onSkillClick = { skillId ->
                            selectedSkillId = skillId
                            scope.launch {
                                AudioEngine.playSFX("ui_select")
                            }
                        },
                        onScaleChange = { newScale -> scale = newScale.coerceIn(0.5f, 2f) },
                        onOffsetChange = { newOffset -> offset = newOffset },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Skill Details Panel
                AnimatedVisibility(
                    visible = selectedSkillId != null,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight()
                ) {
                    branchSkills.find { it.id == selectedSkillId }?.let { skill ->
                        SkillDetailsPanel(
                            skill = skill,
                            availablePoints = treeState.availablePoints,
                            onUpgrade = {
                                onUpgradeSkill(skill.id)
                                scope.launch {
                                    AudioEngine.playSFX("skill_upgrade")
                                    EventBus.emit(GameEvent.Skill.SkillUpgraded(skill.id, skill.currentLevel + 1))
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        // Respec Dialog
        if (showRespecDialog) {
            RespecDialog(
                usedPoints = treeState.usedPoints,
                respecCost = calculateRespecCost(treeState.usedPoints),
                currentMF = treeState.totalMF,
                onConfirm = {
                    onRespec()
                    showRespecDialog = false
                    scope.launch {
                        AudioEngine.playSFX("skill_respec")
                    }
                },
                onDismiss = {
                    showRespecDialog = false
                }
            )
        }
    }
}

/**
 * حساب تكلفة إعادة التوزيع
 * Calculate Respec Cost
 */
private fun calculateRespecCost(usedPoints: Int): Int {
    return (usedPoints * 10).coerceAtLeast(50)
}

// ════════════════════════════════════════════════════════════════════════════════════
// Skill Tree Header - ترويسة شجرة المهارات
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * ترويسة شاشة شجرة المهارات
 * Skill Tree Header
 */
@Composable
private fun SkillTreeHeader(
    availablePoints: Int,
    usedPoints: Int,
    totalMF: Int,
    onRespec: () -> Unit,
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
                    text = "Skill Tree",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "شجرة المهارات",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                )
            }

            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Available Points
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = availablePoints.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = ErytColor.BlightGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "نقاط متاحة",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                }

                // Used Points
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = usedPoints.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = ErytColor.MemoryAqua,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "نقاط مستخدمة",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                    )
                }

                // MF
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
                        text = totalMF.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ErytColor.MemoryAqua,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (usedPoints > 0) {
                    ErytButton(
                        onClick = onRespec,
                        variant = ErytButtonVariant.SECONDARY,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("إعادة توزيع")
                        }
                    }
                }

                ErytButton(
                    onClick = onClose,
                    variant = ErytButtonVariant.SECONDARY,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Branch Selector - محدد الفرع
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * محدد فرع شجرة المهارات
 * Skill Tree Branch Selector
 */
@Composable
private fun BranchSelector(
    selectedBranch: SkillBranch,
    onBranchSelect: (SkillBranch) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        SkillBranch.values().forEach { branch ->
            BranchTab(
                branch = branch,
                isSelected = selectedBranch == branch,
                onClick = { onBranchSelect(branch) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * تبويبة فرع واحد
 * Single Branch Tab
 */
@Composable
private fun BranchTab(
    branch: SkillBranch,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    val branchColor = when (branch) {
        SkillBranch.RADIANCE -> SkillBranchColors.Radiance
        SkillBranch.MEMORY -> SkillBranchColors.Memory
        SkillBranch.VOID -> SkillBranchColors.Void
    }

    Box(
        modifier = modifier
            .height(80.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            branchColor.copy(alpha = 0.3f),
                            branchColor.copy(alpha = 0.1f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            ErytColor.SurfaceDark,
                            ErytColor.SurfaceDark.copy(alpha = 0.8f)
                        )
                    )
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) branchColor else ErytColor.OutlineGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (branch) {
                    SkillBranch.RADIANCE -> Icons.Default.Star
                    SkillBranch.MEMORY -> Icons.Default.AccountCircle
                    SkillBranch.VOID -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = branchColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = branch.nameAr,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) branchColor else ErytColor.RadianceWhite,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Skill Tree Canvas - لوحة رسم الشجرة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * لوحة رسم شجرة المهارات
 * Skill Tree Drawing Canvas
 */
@Composable
private fun SkillTreeCanvas(
    skills: List<SkillNodeState>,
    selectedSkillId: String?,
    branch: SkillBranch,
    scale: Float,
    offset: Offset,
    onSkillClick: (String) -> Unit,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val branchColor = when (branch) {
        SkillBranch.RADIANCE -> SkillBranchColors.Radiance
        SkillBranch.MEMORY -> SkillBranchColors.Memory
        SkillBranch.VOID -> SkillBranchColors.Void
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // Detect skill node clicks
                    skills.forEach { skill ->
                        val nodePosition = Offset(
                            skill.position.x * size.width,
                            skill.position.y * size.height
                        )
                        val distance = (tapOffset - nodePosition).getDistance()
                        if (distance < 40.dp.toPx()) {
                            onSkillClick(skill.id)
                        }
                    }
                }
            }
    ) {
        // Draw connection lines
        skills.forEach { skill ->
            skill.prerequisites.forEach { prereqId ->
                skills.find { it.id == prereqId }?.let { prereq ->
                    val start = Offset(
                        prereq.position.x * size.width,
                        prereq.position.y * size.height
                    )
                    val end = Offset(
                        skill.position.x * size.width,
                        skill.position.y * size.height
                    )

                    val lineColor = if (prereq.isUnlocked && skill.isUnlocked) {
                        branchColor
                    } else if (prereq.isUnlocked) {
                        branchColor.copy(alpha = 0.3f)
                    } else {
                        ErytColor.OutlineGray.copy(alpha = 0.2f)
                    }

                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }

    // Draw skill nodes
    Box(modifier = Modifier.fillMaxSize()) {
        skills.forEach { skill ->
            val nodeSize = 72.dp
            val nodeOffset = DpOffset(
                x = (skill.position.x * 1000).dp - nodeSize / 2,
                y = (skill.position.y * 600).dp - nodeSize / 2
            )

            SkillNode(
                skill = skill,
                isSelected = selectedSkillId == skill.id,
                branchColor = branchColor,
                onClick = { onSkillClick(skill.id) },
                modifier = Modifier
                    .offset(nodeOffset.x, nodeOffset.y)
                    .size(nodeSize)
            )
        }
    }
}

/**
 * عقدة مهارة واحدة
 * Single Skill Node
 */
@Composable
private fun SkillNode(
    skill: SkillNodeState,
    isSelected: Boolean,
    branchColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (skill.canUpgrade) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick, enabled = skill.isUnlocked || skill.canUpgrade)
            .semantics {
                contentDescription = "${skill.nameAr} - Level ${skill.currentLevel}/${skill.maxLevel}"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        if (skill.canUpgrade) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
                    .background(
                        branchColor.copy(alpha = glowAlpha),
                        CircleShape
                    )
            )
        }

        // Node circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (skill.isUnlocked) {
                        Brush.radialGradient(
                            colors = listOf(
                                branchColor.copy(alpha = 0.8f),
                                branchColor.copy(alpha = 0.4f)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                ErytColor.OutlineGray.copy(alpha = 0.3f),
                                ErytColor.OutlineGray.copy(alpha = 0.1f)
                            )
                        )
                    },
                    CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isSelected) ErytColor.BlightGold else branchColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = skill.icon,
                contentDescription = null,
                tint = if (skill.isUnlocked) Color.White else ErytColor.OutlineGray,
                modifier = Modifier.size(32.dp)
            )
        }

        // Level indicator
        if (skill.currentLevel > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .background(branchColor, CircleShape)
                    .size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skill.currentLevel.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Lock icon
        if (!skill.isUnlocked && !skill.canUpgrade) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = ErytColor.OutlineGray,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════════
// Skill Details Panel - لوحة تفاصيل المهارة
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * لوحة تفاصيل المهارة
 * Skill Details Panel
 */
@Composable
private fun SkillDetailsPanel(
    skill: SkillNodeState,
    availablePoints: Int,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val branchColor = when (skill.branch) {
        SkillBranch.RADIANCE -> SkillBranchColors.Radiance
        SkillBranch.MEMORY -> SkillBranchColors.Memory
        SkillBranch.VOID -> SkillBranchColors.Void
    }

    ErytPanel(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Skill Icon Large
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                branchColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, branchColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = skill.icon,
                    contentDescription = null,
                    tint = branchColor,
                    modifier = Modifier.size(60.dp)
                )
            }

            // Skill Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = skill.nameAr,
                    style = MaterialTheme.typography.titleLarge,
                    color = branchColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = skill.nameEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // Level Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المستوى | Level",
                    style = MaterialTheme.typography.labelMedium,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                )
                Text(
                    text = "${skill.currentLevel} / ${skill.maxLevel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = branchColor,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { skill.currentLevel.toFloat() / skill.maxLevel },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = branchColor,
                trackColor = ErytColor.OutlineGray
            )

            Divider(color = ErytColor.OutlineGray)

            // Description
            Text(
                text = skill.descriptionAr,
                style = MaterialTheme.typography.bodyMedium,
                color = ErytColor.RadianceWhite,
                textAlign = TextAlign.Start
            )

            // Effects
            if (skill.effects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "التأثيرات | Effects",
                        style = MaterialTheme.typography.titleSmall,
                        color = branchColor,
                        fontWeight = FontWeight.Bold
                    )
                    skill.effects.forEach { effect ->
                        SkillEffectRow(effect = effect)
                    }
                }
            }

            // Prerequisites
            if (skill.prerequisites.isNotEmpty() && !skill.isUnlocked) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "المتطلبات | Prerequisites",
                        style = MaterialTheme.typography.titleSmall,
                        color = ErytColor.BlightRed,
                        fontWeight = FontWeight.Bold
                    )
                    skill.prerequisites.forEach { prereqId ->
                        Text(
                            text = "• يتطلب: $prereqId",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Upgrade Button
            if (skill.currentLevel < skill.maxLevel) {
                val canAfford = availablePoints >= skill.upgradeCost

                ErytButton(
                    onClick = onUpgrade,
                    variant = ErytButtonVariant.PRIMARY,
                    enabled = skill.canUpgrade && canAfford,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null
                        )
                        Text(
                            text = if (skill.canUpgrade) {
                                "ترقية (${skill.upgradeCost} MF)"
                            } else {
                                "مقفل"
                            }
                        )
                    }
                }

                if (!canAfford && skill.canUpgrade) {
                    Text(
                        text = "شظايا ذاكرة غير كافية!",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErytColor.BlightRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = "✅ مكتمل | Maxed",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2ECC71),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * صف عرض تأثير المهارة
 * Skill Effect Display Row
 */
@Composable
private fun SkillEffectRow(effect: SkillEffect) {
    val (icon, text, color) = when (effect) {
        is SkillEffect.DamageBoost -> Triple(
            Icons.Default.Star,
            "+${effect.percentage.roundToInt()}% ضرر | Damage",
            Color(0xFFE74C3C)
        )
        is SkillEffect.DefenseBoost -> Triple(
            Icons.Default.Build,
            "+${effect.percentage.roundToInt()}% دفاع | Defense",
            Color(0xFF3498DB)
        )
        is SkillEffect.HPBoost -> Triple(
            Icons.Default.Favorite,
            "+${effect.amount.roundToInt()} صحة | HP",
            Color(0xFF2ECC71)
        )
        is SkillEffect.EnergyBoost -> Triple(
            Icons.Default.AccountCircle,
            "+${effect.amount.roundToInt()} طاقة | Energy",
            ErytColor.MemoryAqua
        )
        is SkillEffect.SpeedBoost -> Triple(
            Icons.Default.Refresh,
            "+${effect.percentage.roundToInt()}% سرعة | Speed",
            Color(0xFFFFD700)
        )
        is SkillEffect.CritChanceBoost -> Triple(
            Icons.Default.Star,
            "+${effect.percentage.roundToInt()}% فرصة حرجة | Crit",
            ErytColor.BlightGold
        )
        is SkillEffect.UnlockAbility -> Triple(
            Icons.Default.Lock,
            "يفتح: ${effect.abilityName}",
            ErytColor.BlightGold
        )
        is SkillEffect.ReduceCost -> Triple(
            Icons.Default.AccountCircle,
            "-${effect.percentage.roundToInt()}% تكلفة | Cost",
            Color(0xFF9B59B6)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ErytColor.SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
// Respec Dialog - حوار إعادة التوزيع
// ════════════════════════════════════════════════════════════════════════════════════

/**
 * حوار إعادة توزيع نقاط المهارات
 * Skill Points Respec Dialog
 */
@Composable
private fun RespecDialog(
    usedPoints: Int,
    respecCost: Int,
    currentMF: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val canAfford = currentMF >= respecCost

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
                    text = "إعادة توزيع المهارات",
                    style = MaterialTheme.typography.titleLarge,
                    color = ErytColor.RadianceWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Skill Tree Reset",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite.copy(alpha = 0.7f)
                )

                Divider(color = ErytColor.OutlineGray)

                Text(
                    text = "سيتم استرجاع جميع نقاط المهارات المستخدمة ($usedPoints نقطة) ويمكنك إعادة توزيعها.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErytColor.RadianceWhite
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التكلفة:",
                        style = MaterialTheme.typography.titleMedium,
                        color = ErytColor.RadianceWhite
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = if (canAfford) ErytColor.MemoryAqua else ErytColor.BlightRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$respecCost MF",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (canAfford) ErytColor.MemoryAqua else ErytColor.BlightRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!canAfford) {
                    Text(
                        text = "⚠️ شظايا ذاكرة غير كافية!",
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
                        variant = ErytButtonVariant.DANGER,
                        enabled = canAfford,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تأكيد | Confirm")
                    }
                }
            }
        }
    }
}