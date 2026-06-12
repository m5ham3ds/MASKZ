package com.erygra.maskoflight.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.engine.ParticleEngine
import com.erygra.maskoflight.engine.ParticleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * EnemyRenderer.kt — نظام رسم الأعداء
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * نظام الرسم المتقدم للأعداء في لعبة قِنَاعُ النُّور
 * يحتوي على:
 * - رسم الـ Sprites مع الطبقات
 * - تشغيل الرسوم المتحركة
 * - دمج الطبقات (Layer compositing)
 * - تأثيرات الجسيمات
 * - أشرطة الصحة والطاقة
 * - أدوات التصحيح المرئية
 * - تأثيرات الحالة (Status effects)
 * - Damage numbers
 * - Shadow rendering
 * 
 * Advanced Enemy Rendering System for Mask of Light
 * Features:
 * - Sprite rendering with layers
 * - Animation playback
 * - Layer compositing
 * - Particle effects
 * - Health/energy bars
 * - Debug visualization
 * - Status effect rendering
 * - Damage number display
 * - Dynamic shadows
 * 
 * @author M5ham3d
 * @version 2.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Rendering Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * إطار الرسوم المتحركة
 * Animation frame
 */
data class EnemyAnimationFrame(
    val spriteId: String,
    val duration: Long, // Milliseconds
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val tint: Color = Color.White,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val hitboxes: List<Rect> = emptyList(),
    val hurtboxes: List<Rect> = emptyList()
)

/**
 * مقطع الرسوم المتحركة
 * Animation clip
 */
data class EnemyAnimationClip(
    val name: String,
    val frames: List<EnemyAnimationFrame>,
    val looping: Boolean = true,
    val priority: Int = 0,
    val canInterrupt: Boolean = true,
    val nextClip: String? = null, // Auto-transition
    val onComplete: (() -> Unit)? = null
) {
    val totalDuration: Long = frames.sumOf { it.duration }
    val frameCount: Int = frames.size
}

/**
 * حالة الرسوم المتحركة
 * Animation state
 */
enum class EnemyAnimationState {
    // Locomotion
    IDLE,
    WALK,
    RUN,
    TURN,
    
    // Combat
    ATTACK_LIGHT,
    ATTACK_HEAVY,
    ATTACK_RANGED,
    ATTACK_SPECIAL,
    WINDUP,
    RECOVERY,
    
    // Movement
    JUMP_START,
    JUMP_RISE,
    JUMP_FALL,
    LAND,
    CLIMB,
    FLY,
    SWIM,
    
    // Reactions
    HIT,
    STAGGER,
    KNOCKBACK,
    STUNNED,
    BLOCK,
    PARRIED,
    
    // Special
    SPAWN,
    TAUNT,
    FLEE,
    DEATH,
    PHASE_TRANSITION,
    SUMMON,
    
    // Boss-specific
    BOSS_INTRO,
    BOSS_ENRAGE,
    BOSS_ULTIMATE
}

/**
 * طبقة الرسم
 * Render layer
 */
enum class EnemyRenderLayer(val order: Int) {
    SHADOW(0),
    BODY_BACK(1),
    BODY_MAIN(2),
    ARMOR(3),
    WEAPON_BACK(4),
    WEAPON_FRONT(5),
    EFFECTS_BACK(6),
    EFFECTS_FRONT(7),
    STATUS_EFFECTS(8),
    UI(9)
}

/**
 * بيانات طبقة الرسم
 * Layer render data
 */
data class LayerRenderData(
    val layer: EnemyRenderLayer,
    val spriteId: String,
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val tint: Color = Color.White,
    val blendMode: BlendMode = BlendMode.Normal
)

/**
 * وضع الدمج
 * Blend mode for layers
 */
enum class BlendMode {
    Normal,
    Additive,
    Multiply,
    Screen,
    Overlay
}

/**
 * رقم الضرر
 * Damage number display
 */
data class DamageNumber(
    val amount: Float,
    val x: Float,
    val y: Float,
    val isCritical: Boolean = false,
    val isHeal: Boolean = false,
    val spawnTime: Long = System.currentTimeMillis(),
    val duration: Long = 1500L,
    val velocityY: Float = -80f,
    val color: Color = if (isHeal) Color.Green else if (isCritical) Color(0xFFFF6B35) else Color.White
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - spawnTime > duration
    
    fun getCurrentY(): Float {
        val elapsed = (System.currentTimeMillis() - spawnTime) / 1000f
        return y + velocityY * elapsed - 0.5f * 200f * elapsed * elapsed
    }
    
    fun getAlpha(): Float {
        val progress = (System.currentTimeMillis() - spawnTime).toFloat() / duration
        return (1f - progress).coerceIn(0f, 1f)
    }
}

/**
 * تأثير الحالة المرئي
 * Visual status effect
 */
data class VisualStatusEffect(
    val type: EnemyEffectType,
    val color: Color,
    val particleType: ParticleType,
    val glowIntensity: Float = 0f,
    val pulseSpeed: Float = 1f,
    val icon: String? = null
)

/**
 * إعدادات الظل
 * Shadow configuration
 */
data class ShadowConfig(
    val enabled: Boolean = true,
    val offsetX: Float = 0f,
    val offsetY: Float = 8f,
    val scaleX: Float = 0.8f,
    val scaleY: Float = 0.3f,
    val alpha: Float = 0.3f,
    val color: Color = Color.Black,
    val dynamic: Boolean = true // Adjust based on height
)

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Animation Controller
// ════════════════════════════════════════════════════════════════════════════════

/**
 * متحكم الرسوم المتحركة للعدو
 * Enemy animation controller
 */
class EnemyAnimationController(
    private val enemy: Enemy
) {
    private var currentClip: EnemyAnimationClip? = null
    private var currentState: EnemyAnimationState = EnemyAnimationState.IDLE
    private var currentFrameIndex = 0
    private var frameStartTime = 0L
    private var clipStartTime = 0L
    
    private val animationClips = mutableMapOf<EnemyAnimationState, EnemyAnimationClip>()
    
    // Blending
    private var previousClip: EnemyAnimationClip? = null
    private var blendProgress = 1f
    private var blendDuration = 200L
    
    init {
        loadAnimations()
    }
    
    /**
     * تحميل الرسوم المتحركة
     * Load animation clips
     */
    private fun loadAnimations() {
        // This would load from sprite sheets
        // For now, creating placeholder clips
        
        // Idle animation
        animationClips[EnemyAnimationState.IDLE] = EnemyAnimationClip(
            name = "idle",
            frames = List(4) { i ->
                EnemyAnimationFrame(
                    spriteId = "${enemy.definition.type}_idle_$i",
                    duration = 200L,
                    offset = Offset(0f, sin(i * 0.5f) * 2f) // Slight bobbing
                )
            },
            looping = true,
            priority = 0
        )
        
        // Walk animation
        animationClips[EnemyAnimationState.WALK] = EnemyAnimationClip(
            name = "walk",
            frames = List(8) { i ->
                EnemyAnimationFrame(
                    spriteId = "${enemy.definition.type}_walk_$i",
                    duration = 150L,
                    offset = Offset(0f, sin(i * 0.8f) * 3f)
                )
            },
            looping = true,
            priority = 1
        )
        
        // Run animation
        animationClips[EnemyAnimationState.RUN] = EnemyAnimationClip(
            name = "run",
            frames = List(6) { i ->
                EnemyAnimationFrame(
                    spriteId = "${enemy.definition.type}_run_$i",
                    duration = 100L,
                    offset = Offset(0f, sin(i * 1.2f) * 4f)
                )
            },
            looping = true,
            priority = 2
        )
        
        // Attack animations
        animationClips[EnemyAnimationState.ATTACK_LIGHT] = EnemyAnimationClip(
            name = "attack_light",
            frames = List(5) { i ->
                EnemyAnimationFrame(
                    spriteId = "${enemy.definition.type}_attack_light_$i",
                    duration = if (i == 2) 100L else 80L, // Impact frame
                    offset = if (i == 2) Offset(10f, 0f) else Offset.Zero
                )
            },
            looping = false,
            priority = 10,
            canInterrupt = false,
            nextClip = "idle"
        )
        
        animationClips[EnemyAnimationState.ATTACK_HEAVY] = EnemyAnimationClip(
            name = "attack_heavy",
            frames = List(7) { i ->
                EnemyAnimationFrame(
                    spriteId = "${enemy.definition.type}_attack_heavy_$i",
                    duration = when (i) {
                        0, 1 -> 150L // Windup
                        4 -> 100L // Impact
                        else -> 120L
                    },
                    offset = if (i == 4) Offset(15f, -5f) else Offset.Zero,
                    scale = if (i == 4) 1.1f else 1f
                )
            },
            looping = false,
            priority = 11,
            canInterrupt = false
        )
        
        // Hit reaction
        animationClips[EnemyAnimationState.HIT] = EnemyAnimationClip(
            name = "hit",
            frames = List(3) { i ->
                EnemyAnimationFrame(
                    spriteId = "${enemy.definition.type}_hit_$i",
                    duration = 100L,
                    tint = if (i == 0) Color(0xFFFF4444) else Color.White,
                    offset = if (i == 0) Offset(-5f, 0f) else Offset.Zero
                )
            },
            looping = false,
            priority = 15,
            canInterrupt = true,
            nextClip = "idle"
        )
        
        // Death animation
        animationClips[EnemyAnimationState.DEATH] = EnemyAnimationClip(
            name = "death",
            frames = List(8) { i ->
                EnemyAnimationFrame(
                    spriteId = "${enemy.definition.type}_death_$i",
                    duration = 150L,
                    rotation = i * 10f,
                    alpha = 1f - (i / 8f) * 0.5f,
                    offset = Offset(0f, i * 5f)
                )
            },
            looping = false,
            priority = 20,
            canInterrupt = false
        )
        
        // Stunned
        animationClips[EnemyAnimationState.STUNNED] = EnemyAnimationClip(
            name = "stunned",
            frames = List(4) { i ->
                EnemyAnimationFrame(
                    spriteId = "${enemy.definition.type}_stunned_$i",
                    duration = 200L,
                    rotation = sin(i * 0.5f) * 5f,
                    offset = Offset(sin(i * 0.8f) * 3f, 0f)
                )
            },
            looping = true,
            priority = 12
        )
    }
    
    /**
     * تحديث الرسوم المتحركة
     * Update animation
     */
    fun update(deltaTime: Float) {
        val clip = currentClip ?: return
        val now = System.currentTimeMillis()
        
        // Update blend
        if (blendProgress < 1f) {
            blendProgress = ((now - clipStartTime).toFloat() / blendDuration).coerceIn(0f, 1f)
        }
        
        // Check frame advancement
        val frameElapsed = now - frameStartTime
        val currentFrame = clip.frames[currentFrameIndex]
        
        if (frameElapsed >= currentFrame.duration) {
            // Advance to next frame
            currentFrameIndex++
            frameStartTime = now
            
            // Check clip completion
            if (currentFrameIndex >= clip.frames.size) {
                if (clip.looping) {
                    currentFrameIndex = 0
                } else {
                    // Clip finished
                    clip.onComplete?.invoke()
                    
                    // Auto-transition
                    clip.nextClip?.let { nextClipName ->
                        val nextState = EnemyAnimationState.values().find { 
                            animationClips[it]?.name == nextClipName 
                        }
                        nextState?.let { transitionTo(it) }
                    }
                }
            }
        }
        
        // Sync with enemy state
        updateStateFromEnemy()
    }
    
    /**
     * تحديث الحالة من حالة العدو
     * Update animation state from enemy state
     */
    private fun updateStateFromEnemy() {
        val targetState = when (enemy.currentState) {
            EnemyState.IDLE -> EnemyAnimationState.IDLE
            EnemyState.PATROL -> {
                if (abs(enemy.position.velocityX) > 50f) EnemyAnimationState.WALK
                else EnemyAnimationState.IDLE
            }
            EnemyState.CHASING -> {
                if (abs(enemy.position.velocityX) > 100f) EnemyAnimationState.RUN
                else EnemyAnimationState.WALK
            }
            EnemyState.ATTACKING -> {
                // Handled by attack execution
                currentState
            }
            EnemyState.FLEEING -> EnemyAnimationState.RUN
            EnemyState.STUNNED -> EnemyAnimationState.STUNNED
            EnemyState.DEAD -> EnemyAnimationState.DEATH
            else -> currentState
        }
        
        if (targetState != currentState && currentClip?.canInterrupt != false) {
            transitionTo(targetState)
        }
    }
    
    /**
     * الانتقال إلى حالة جديدة
     * Transition to new animation state
     */
    fun transitionTo(newState: EnemyAnimationState, forceTransition: Boolean = false) {
        val newClip = animationClips[newState] ?: return
        
        // Check if can interrupt
        if (!forceTransition && currentClip?.canInterrupt == false) {
            // Can't interrupt current animation
            if (newClip.priority <= (currentClip?.priority ?: 0)) {
                return
            }
        }
        
        // Start blending
        previousClip = currentClip
        blendProgress = 0f
        
        // Set new animation
        currentClip = newClip
        currentState = newState
        currentFrameIndex = 0
        frameStartTime = System.currentTimeMillis()
        clipStartTime = frameStartTime
    }
    
    /**
     * الحصول على الإطار الحالي
     * Get current frame
     */
    fun getCurrentFrame(): EnemyAnimationFrame? {
        val clip = currentClip ?: return null
        if (currentFrameIndex >= clip.frames.size) return null
        return clip.frames[currentFrameIndex]
    }
    
    /**
     * الحصول على الإطار مع الدمج
     * Get blended frame
     */
    fun getBlendedFrame(): EnemyAnimationFrame? {
        val current = getCurrentFrame() ?: return null
        
        if (blendProgress >= 1f || previousClip == null) {
            return current
        }
        
        // Blend with previous frame
        val previousFrame = previousClip?.frames?.getOrNull(
            (previousClip!!.frames.size - 1).coerceAtMost(currentFrameIndex)
        ) ?: return current
        
        return EnemyAnimationFrame(
            spriteId = current.spriteId, // Use current sprite
            duration = current.duration,
            offset = Offset(
                previousFrame.offset.x + (current.offset.x - previousFrame.offset.x) * blendProgress,
                previousFrame.offset.y + (current.offset.y - previousFrame.offset.y) * blendProgress
            ),
            scale = previousFrame.scale + (current.scale - previousFrame.scale) * blendProgress,
            rotation = previousFrame.rotation + (current.rotation - previousFrame.rotation) * blendProgress,
            alpha = previousFrame.alpha + (current.alpha - previousFrame.alpha) * blendProgress,
            tint = current.tint, // No color blending for simplicity
            flipHorizontal = current.flipHorizontal,
            flipVertical = current.flipVertical
        )
    }
    
    /**
     * تشغيل رسوم متحركة لمرة واحدة
     * Play one-shot animation
     */
    fun playOneShot(state: EnemyAnimationState, onComplete: (() -> Unit)? = null) {
        val clip = animationClips[state] ?: return
        
        // Create temporary clip with callback
        val tempClip = clip.copy(
            looping = false,
            onComplete = onComplete
        )
        
        animationClips[state] = tempClip
        transitionTo(state, forceTransition = true)
    }
    
    fun getCurrentState(): EnemyAnimationState = currentState
    fun isPlaying(state: EnemyAnimationState): Boolean = currentState == state
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Status Effect Renderer
// ════════════════════════════════════════════════════════════════════════════════

/**
 * رسام تأثيرات الحالة
 * Status effect renderer
 */
class StatusEffectRenderer {
    private val effectVisuals = mapOf(
        EnemyEffectType.SLOW to VisualStatusEffect(
            type = EnemyEffectType.SLOW,
            color = Color(0xFF4A90E2),
            particleType = ParticleType.FROST,
            glowIntensity = 0.3f,
            pulseSpeed = 0.5f
        ),
        EnemyEffectType.STUN to VisualStatusEffect(
            type = EnemyEffectType.STUN,
            color = Color(0xFFFFD700),
            particleType = ParticleType.SPARK,
            glowIntensity = 0.5f,
            pulseSpeed = 2f
        ),
        EnemyEffectType.POISON to VisualStatusEffect(
            type = EnemyEffectType.POISON,
            color = Color(0xFF00FF00),
            particleType = ParticleType.POISON_BUBBLE,
            glowIntensity = 0.4f,
            pulseSpeed = 1f
        ),
        EnemyEffectType.BURN to VisualStatusEffect(
            type = EnemyEffectType.BURN,
            color = Color(0xFFFF4500),
            particleType = ParticleType.FIRE,
            glowIntensity = 0.6f,
            pulseSpeed = 1.5f
        ),
        EnemyEffectType.FREEZE to VisualStatusEffect(
            type = EnemyEffectType.FREEZE,
            color = Color(0xFF87CEEB),
            particleType = ParticleType.ICE_SHARD,
            glowIntensity = 0.5f,
            pulseSpeed = 0.3f
        ),
        EnemyEffectType.BLIND to VisualStatusEffect(
            type = EnemyEffectType.BLIND,
            color = Color(0xFF2F2F2F),
            particleType = ParticleType.SMOKE,
            glowIntensity = 0.2f,
            pulseSpeed = 0.8f
        ),
        EnemyEffectType.MARKED to VisualStatusEffect(
            type = EnemyEffectType.MARKED,
            color = Color(0xFFFF00FF),
            particleType = ParticleType.SPARKLE,
            glowIntensity = 0.7f,
            pulseSpeed = 1.2f
        )
    )
    
    /**
     * رسم تأثيرات الحالة
     * Render status effects
     */
    fun render(
        drawScope: DrawScope,
        enemy: Enemy,
        x: Float,
        y: Float,
        time: Float
    ) {
        val activeEffects = enemy.activeEffects.filter { !it.isExpired }
        
        activeEffects.forEachIndexed { index, effect ->
            val visual = effectVisuals[effect.type] ?: return@forEachIndexed
            
            // Glow effect
            if (visual.glowIntensity > 0f) {
                val pulse = (sin(time * visual.pulseSpeed) * 0.5f + 0.5f) * visual.glowIntensity
                drawGlow(drawScope, x, y, visual.color, pulse)
            }
            
            // Status icon
            visual.icon?.let { iconId ->
                drawStatusIcon(
                    drawScope,
                    x + index * 20f - 30f,
                    y - 50f,
                    iconId,
                    visual.color
                )
            }
        }
    }
    
    /**
     * رسم التوهج
     * Draw glow effect
     */
    private fun drawGlow(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        color: Color,
        intensity: Float
    ) {
        with(drawScope) {
            drawCircle(
                color = color.copy(alpha = intensity * 0.3f),
                radius = 60f,
                center = Offset(x, y)
            )
            drawCircle(
                color = color.copy(alpha = intensity * 0.2f),
                radius = 80f,
                center = Offset(x, y)
            )
        }
    }
    
    /**
     * رسم أيقونة الحالة
     * Draw status icon
     */
    private fun drawStatusIcon(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        iconId: String,
        color: Color
    ) {
        with(drawScope) {
            // Simple circular icon
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 12f,
                center = Offset(x, y)
            )
            drawCircle(
                color = color,
                radius = 10f,
                center = Offset(x, y)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Damage Number Renderer
// ════════════════════════════════════════════════════════════════════════════════

/**
 * رسام أرقام الضرر
 * Damage number renderer
 */
class DamageNumberRenderer {
    private val damageNumbers = mutableListOf<DamageNumber>()
    
    /**
     * إضافة رقم ضرر
     * Add damage number
     */
    fun addDamageNumber(
        amount: Float,
        x: Float,
        y: Float,
        isCritical: Boolean = false,
        isHeal: Boolean = false
    ) {
        damageNumbers.add(
            DamageNumber(
                amount = amount,
                x = x + (Math.random().toFloat() - 0.5f) * 20f,
                y = y,
                isCritical = isCritical,
                isHeal = isHeal
            )
        )
    }
    
    /**
     * تحديث وإزالة الأرقام المنتهية
     * Update and remove expired numbers
     */
    fun update() {
        damageNumbers.removeAll { it.isExpired() }
    }
    
    /**
     * رسم أرقام الضرر
     * Render damage numbers
     */
    fun render(drawScope: DrawScope) {
        damageNumbers.forEach { number ->
            val alpha = number.getAlpha()
            val currentY = number.getCurrentY()
            val scale = if (number.isCritical) 1.5f else 1f
            
            with(drawScope) {
                // Shadow
                // drawText removed
                
                // Main text
                // drawText removed
            }
        }
    }
    
    /**
     * رسم نص (placeholder - would use actual text rendering)
     * Draw text placeholder
     */
    private fun DrawScope.// drawText removed {
        // In actual implementation, would use Canvas.nativeCanvas.drawText
        // For now, using circles as placeholders
        val radius = 8f * scale
        text.forEachIndexed { index, char ->
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(x + index * radius * 2, y)
            )
        }
    }
    
    fun clear() {
        damageNumbers.clear()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Main Enemy Renderer
// ════════════════════════════════════════════════════════════════════════════════

/**
 * رسام العدو الرئيسي
 * Main enemy renderer
 */
class EnemyRenderer(
    private val enemy: Enemy,
    private val particleEngine: ParticleEngine
) {
    private val animationController = EnemyAnimationController(enemy)
    private val statusEffectRenderer = StatusEffectRenderer()
    private val damageNumberRenderer = DamageNumberRenderer()
    
    private val shadowConfig = ShadowConfig()
    
    // Render settings
    private var showHitboxes = false
    private var showHealthBar = true
    private var showStatusEffects = true
    private var showDamageNumbers = true
    private var showShadow = true
    
    // Camera shake
    private var shakeIntensity = 0f
    private var shakeDecay = 5f
    
    /**
     * تحديث الرسام
     * Update renderer
     */
    fun update(deltaTime: Float) {
        animationController.update(deltaTime)
        damageNumberRenderer.update()
        
        // Update shake
        if (shakeIntensity > 0f) {
            shakeIntensity -= shakeDecay * deltaTime
            if (shakeIntensity < 0f) shakeIntensity = 0f
        }
    }
    
    /**
     * رسم العدو
     * Render enemy
     */
    fun render(
        drawScope: DrawScope,
        cameraX: Float,
        cameraY: Float,
        time: Float
    ) {
        val position = enemy.position
        val screenX = position.x - cameraX
        val screenY = position.y - cameraY
        
        // Apply shake
        val shakeX = if (shakeIntensity > 0f) (Math.random().toFloat() - 0.5f) * shakeIntensity else 0f
        val shakeY = if (shakeIntensity > 0f) (Math.random().toFloat() - 0.5f) * shakeIntensity else 0f
        
        val finalX = screenX + shakeX
        val finalY = screenY + shakeY
        
        with(drawScope) {
            // Render shadow
            if (showShadow && enemy.currentState != EnemyState.DEAD) {
                renderShadow(this, finalX, finalY)
            }
            
            // Render main sprite
            renderSprite(this, finalX, finalY, time)
            
            // Render status effects
            if (showStatusEffects) {
                statusEffectRenderer.render(this, enemy, finalX, finalY, time)
            }
            
            // Render health bar
            if (showHealthBar && enemy.currentState != EnemyState.DEAD) {
                renderHealthBar(this, finalX, finalY)
            }
            
            // Render damage numbers
            if (showDamageNumbers) {
                damageNumberRenderer.render(this)
            }
            
            // Debug rendering
            if (showHitboxes) {
                renderDebug(this, finalX, finalY)
            }
        }
    }
    
    /**
     * رسم الظل
     * Render shadow
     */
    private fun renderShadow(drawScope: DrawScope, x: Float, y: Float) {
        if (!shadowConfig.enabled) return
        
        val config = shadowConfig
        val heightOffset = if (config.dynamic) {
            // Adjust shadow based on entity height (jumping, flying, etc.)
            val baseY = enemy.spawnPoint.second
            val currentY = enemy.position.y
            val heightDiff = baseY - currentY
            heightDiff * 0.1f // Scale factor
        } else {
            0f
        }
        
        with(drawScope) {
            drawOval(
                color = config.color.copy(alpha = config.alpha * (1f - heightOffset.coerceIn(0f, 0.8f))),
                topLeft = Offset(
                    x - 32f * config.scaleX + config.offsetX,
                    y + config.offsetY + heightOffset
                ),
                size = Size(
                    64f * config.scaleX,
                    16f * config.scaleY
                )
            )
        }
    }
    
    /**
     * رسم الـ Sprite
     * Render sprite
     */
    private fun renderSprite(drawScope: DrawScope, x: Float, y: Float, time: Float) {
        val frame = animationController.getBlendedFrame() ?: return
        
        with(drawScope) {
            withTransform({
                translate(
                    left = x + frame.offset.x,
                    top = y + frame.offset.y
                )
                rotate(
                    degrees = frame.rotation,
                    pivot = Offset(0f, 0f)
                )
                scale(
                    scaleX = if (frame.flipHorizontal) -frame.scale else frame.scale,
                    scaleY = if (frame.flipVertical) -frame.scale else frame.scale,
                    pivot = Offset(0f, 0f)
                )
            }) {
                // Draw sprite
                // In actual implementation, would draw from sprite sheet
                // For now, using placeholder rectangle
                drawRect(
                    color = frame.tint.copy(alpha = frame.alpha),
                    topLeft = Offset(-32f, -32f),
                    size = Size(64f, 64f)
                )
                
                // Draw enemy type indicator (placeholder)
                drawCircle(
                    color = getEnemyTypeColor(),
                    radius = 20f,
                    center = Offset(0f, 0f),
                    alpha = frame.alpha
                )
            }
        }
    }
    
    /**
     * رسم شريط الصحة
     * Render health bar
     */
    private fun renderHealthBar(drawScope: DrawScope, x: Float, y: Float) {
        val stats = enemy.stats
        val hpPercent = stats.currentHp / stats.maxHp
        
        val barWidth = 60f
        val barHeight = 6f
        val barY = y - 45f
        
        with(drawScope) {
            // Background
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(x - barWidth / 2 - 1f, barY - 1f),
                size = Size(barWidth + 2f, barHeight + 2f)
            )
            
            // Health
            val healthColor = when {
                hpPercent > 0.6f -> Color(0xFF00FF00)
                hpPercent > 0.3f -> Color(0xFFFFFF00)
                else -> Color(0xFFFF0000)
            }
            
            drawRect(
                color = healthColor,
                topLeft = Offset(x - barWidth / 2, barY),
                size = Size(barWidth * hpPercent, barHeight)
            )
            
            // Border
            drawRect(
                color = Color.White,
                topLeft = Offset(x - barWidth / 2, barY),
                size = Size(barWidth, barHeight),
                style = Stroke(width = 1f)
            )
        }
        
        // Elite/Boss indicator
        if (enemy.definition.rank != EnemyRank.NORMAL) {
            drawScope.drawRect(
                color = getRankColor(),
                topLeft = Offset(x - barWidth / 2, barY - 3f),
                size = Size(barWidth, 2f)
            )
        }
    }
    
    /**
     * رسم معلومات التصحيح
     * Render debug info
     */
    private fun renderDebug(drawScope: DrawScope, x: Float, y: Float) {
        val frame = animationController.getCurrentFrame() ?: return
        
        with(drawScope) {
            // Hitboxes (attack boxes)
            frame.hitboxes.forEach { hitbox ->
                drawRect(
                    color = Color.Red.copy(alpha = 0.3f),
                    topLeft = Offset(x + hitbox.left, y + hitbox.top),
                    size = Size(hitbox.width, hitbox.height),
                    style = Stroke(width = 2f)
                )
            }
            
            // Hurtboxes (vulnerable areas)
            frame.hurtboxes.forEach { hurtbox ->
                drawRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = Offset(x + hurtbox.left, y + hurtbox.top),
                    size = Size(hurtbox.width, hurtbox.height),
                    style = Stroke(width = 2f)
                )
            }
            
            // Center point
            drawCircle(
                color = Color.Green,
                radius = 3f,
                center = Offset(x, y)
            )
            
            // Velocity vector
            if (enemy.position.velocityX != 0f || enemy.position.velocityY != 0f) {
                drawLine(
                    color = Color.Yellow,
                    start = Offset(x, y),
                    end = Offset(
                        x + enemy.position.velocityX * 0.5f,
                        y + enemy.position.velocityY * 0.5f
                    ),
                    strokeWidth = 2f
                )
            }
            
            // State text
            // drawText removed
        }
    }
    
    /**
     * الحصول على لون نوع العدو
     * Get color for enemy type
     */
    private fun getEnemyTypeColor(): Color {
        return when (enemy.definition.category) {
            EnemyCategory.MELEE -> Color(0xFFE74C3C)
            EnemyCategory.RANGED -> Color(0xFF3498DB)
            EnemyCategory.FLYING -> Color(0xFF9B59B6)
            EnemyCategory.HEAVY -> Color(0xFF95A5A6)
            EnemyCategory.STEALTH -> Color(0xFF34495E)
            EnemyCategory.SUPPORT -> Color(0xFF1ABC9C)
            EnemyCategory.BOSS -> Color(0xFFF39C12)
        }
    }
    
    /**
     * الحصول على لون الرتبة
     * Get color for enemy rank
     */
    private fun getRankColor(): Color {
        return when (enemy.definition.rank) {
            EnemyRank.NORMAL -> Color.White
            EnemyRank.ELITE -> Color(0xFF3498DB)
            EnemyRank.MINIBOSS -> Color(0xFF9B59B6)
            EnemyRank.BOSS -> Color(0xFFF39C12)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Public Interface
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * تشغيل رسوم متحركة
     * Play animation
     */
    fun playAnimation(state: EnemyAnimationState, force: Boolean = false) {
        animationController.transitionTo(state, force)
    }
    
    /**
     * إضافة رقم ضرر
     * Add damage number
     */
    fun addDamage(amount: Float, isCritical: Boolean = false) {
        damageNumberRenderer.addDamageNumber(
            amount = amount,
            x = enemy.position.x,
            y = enemy.position.y - 20f,
            isCritical = isCritical,
            isHeal = false
        )
        
        // Trigger shake on critical
        if (isCritical) {
            triggerShake(10f)
        }
    }
    
    /**
     * إضافة رقم شفاء
     * Add heal number
     */
    fun addHeal(amount: Float) {
        damageNumberRenderer.addDamageNumber(
            amount = amount,
            x = enemy.position.x,
            y = enemy.position.y - 20f,
            isCritical = false,
            isHeal = true
        )
    }
    
    /**
     * تفعيل اهتزاز الكاميرا
     * Trigger camera shake
     */
    fun triggerShake(intensity: Float) {
        shakeIntensity = intensity.coerceIn(0f, 20f)
    }
    
    /**
     * إعدادات العرض
     * Toggle render settings
     */
    fun setShowHitboxes(show: Boolean) { showHitboxes = show }
    fun setShowHealthBar(show: Boolean) { showHealthBar = show }
    fun setShowStatusEffects(show: Boolean) { showStatusEffects = show }
    fun setShowDamageNumbers(show: Boolean) { showDamageNumbers = show }
    fun setShowShadow(show: Boolean) { showShadow = show }
    
    /**
     * تنظيف الموارد
     * Cleanup
     */
    fun cleanup() {
        damageNumberRenderer.clear()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Batch Renderer (للأداء)
// ════════════════════════════════════════════════════════════════════════════════

/**
 * رسام مجمع للأعداء (تحسين الأداء)
 * Batch renderer for multiple enemies
 */
class EnemyBatchRenderer(
    private val particleEngine: ParticleEngine
) {
    private val enemyRenderers = mutableMapOf<String, EnemyRenderer>()
    private val _renderers = MutableStateFlow<List<EnemyRenderer>>(emptyList())
    val renderers: StateFlow<List<EnemyRenderer>> = _renderers.asStateFlow()
    
    // Performance settings
    private var maxRenderDistance = 1200f
    private var lodDistance = 600f // Level of detail distance
    
    /**
     * تسجيل عدو
     * Register enemy for rendering
     */
    fun registerEnemy(enemy: Enemy): EnemyRenderer {
        val renderer = EnemyRenderer(enemy, particleEngine)
        enemyRenderers[enemy.id] = renderer
        updateRendererList()
        return renderer
    }
    
    /**
     * إلغاء تسجيل عدو
     * Unregister enemy
     */
    fun unregisterEnemy(enemyId: String) {
        enemyRenderers[enemyId]?.cleanup()
        enemyRenderers.remove(enemyId)
        updateRendererList()
    }
    
    /**
     * تحديث جميع الرسامين
     * Update all renderers
     */
    fun updateAll(deltaTime: Float) {
        enemyRenderers.values.forEach { it.update(deltaTime) }
    }
    
    /**
     * رسم جميع الأعداء
     * Render all enemies
     */
    fun renderAll(
        drawScope: DrawScope,
        cameraX: Float,
        cameraY: Float,
        time: Float,
        enemies: List<Enemy>
    ) {
        // Sort by Y position for proper layering
        val sortedEnemies = enemies.sortedBy { it.position.y }
        
        sortedEnemies.forEach { enemy ->
            val renderer = enemyRenderers[enemy.id] ?: return@forEach
            
            // Distance culling
            val screenX = enemy.position.x - cameraX
            val screenY = enemy.position.y - cameraY
            val distance = sqrt(screenX * screenX + screenY * screenY)
            
            if (distance > maxRenderDistance) return@forEach
            
            // LOD system (simplified rendering for distant enemies)
            val useLOD = distance > lodDistance
            if (useLOD) {
                renderer.setShowShadow(false)
                renderer.setShowStatusEffects(false)
            } else {
                renderer.setShowShadow(true)
                renderer.setShowStatusEffects(true)
            }
            
            // Render
            renderer.render(drawScope, cameraX, cameraY, time)
        }
    }
    
    /**
     * تحديث قائمة الرسامين
     * Update renderer list
     */
    private fun updateRendererList() {
        _renderers.value = enemyRenderers.values.toList()
    }
    
    /**
     * الحصول على رسام عدو
     * Get enemy renderer
     */
    fun getRenderer(enemyId: String): EnemyRenderer? = enemyRenderers[enemyId]
    
    /**
     * تعيين مسافة الرسم
     * Set render distance
     */
    fun setRenderDistance(distance: Float) {
        maxRenderDistance = distance
    }
    
    /**
     * تعيين مسافة LOD
     * Set LOD distance
     */
    fun setLODDistance(distance: Float) {
        lodDistance = distance
    }
    
    /**
     * تنظيف جميع الموارد
     * Cleanup all resources
     */
    fun cleanup() {
        enemyRenderers.values.forEach { it.cleanup() }
        enemyRenderers.clear()
        updateRendererList()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Debug Renderer
// ════════════════════════════════════════════════════════════════════════════════

/**
 * رسام التصحيح المتقدم
 * Advanced debug renderer
 */
class EnemyDebugRenderer {
    private var showPaths = false
    private var showVisionCones = false
    private var showDetectionRadius = false
    private var showStates = true
    private var showStats = false
    
    /**
     * رسم معلومات التصحيح
     * Render debug information
     */
    fun render(
        drawScope: DrawScope,
        enemy: Enemy,
        cameraX: Float,
        cameraY: Float,
        stateMachine: EnemyStateMachine?,
        detectionSystem: EnemyDetectionSystem?
    ) {
        val position = enemy.position
        val screenX = position.x - cameraX
        val screenY = position.y - cameraY
        
        with(drawScope) {
            // Vision cone
            if (showVisionCones && detectionSystem != null) {
                renderVisionCone(this, screenX, screenY, enemy, detectionSystem)
            }
            
            // Detection radius
            if (showDetectionRadius) {
                renderDetectionRadius(this, screenX, screenY, enemy)
            }
            
            // State info
            if (showStates && stateMachine != null) {
                renderStateInfo(this, screenX, screenY, enemy, stateMachine)
            }
            
            // Stats
            if (showStats) {
                renderStatsInfo(this, screenX, screenY, enemy)
            }
        }
    }
    
    /**
     * رسم مخروط الرؤية
     * Render vision cone
     */
    private fun renderVisionCone(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        enemy: Enemy,
        detectionSystem: EnemyDetectionSystem
    ) {
        // Simplified vision cone rendering
        val config = EnemyAIConfigDatabase.getConfig(enemy.definition.type)
        val visionRange = config.visionRange
        val visionAngle = config.visionAngle
        
        val direction = if (enemy.position.isFacingRight) 0f else 180f
        
        with(drawScope) {
            // Detection data
            val detectionData = detectionSystem.getDetectionData()
            val color = when (detectionData.alertLevel) {
                AlertLevel.UNAWARE -> Color.Green.copy(alpha = 0.2f)
                AlertLevel.SUSPICIOUS -> Color.Yellow.copy(alpha = 0.3f)
                AlertLevel.INVESTIGATING -> Color.Yellow.copy(alpha = 0.4f)
                AlertLevel.ALERTED -> Color(0xFFFFA500).copy(alpha = 0.5f)
                AlertLevel.COMBAT -> Color.Red.copy(alpha = 0.6f)
            }
            
            // Draw arc
            drawArc(
                color = color,
                startAngle = direction - visionAngle / 2f,
                sweepAngle = visionAngle,
                useCenter = true,
                topLeft = Offset(x - visionRange, y - visionRange),
                size = Size(visionRange * 2, visionRange * 2)
            )
        }
    }
    
    /**
     * رسم نطاق الكشف
     * Render detection radius
     */
    private fun renderDetectionRadius(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        enemy: Enemy
    ) {
        val config = EnemyAIConfigDatabase.getConfig(enemy.definition.type)
        
        with(drawScope) {
            // Aggro range
            drawCircle(
                color = Color.Red.copy(alpha = 0.2f),
                radius = config.visionRange,
                center = Offset(x, y),
                style = Stroke(width = 2f)
            )
            
            // Leash range
            drawCircle(
                color = Color.Blue.copy(alpha = 0.2f),
                radius = config.maxChaseDistance,
                center = Offset(x, y),
                style = Stroke(width = 1f)
            )
        }
    }
    
    /**
     * رسم معلومات الحالة
     * Render state info
     */
    private fun renderStateInfo(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        enemy: Enemy,
        stateMachine: EnemyStateMachine
    ) {
        with(drawScope) {
            val state = stateMachine.getCurrentState()
            val timeInState = stateMachine.getTimeInState()
            
            // State name
            // drawText removed
            
            // Time in state
            // drawText removed
        }
    }
    
    /**
     * رسم معلومات الإحصائيات
     * Render stats info
     */
    private fun renderStatsInfo(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        enemy: Enemy
    ) {
        val stats = enemy.stats
        
        with(drawScope) {
            // HP
            // drawText removed
            
            // Level
            // drawText removed
        }
    }
    
    /**
     * رسم نص (placeholder)
     */
    private fun DrawScope.// drawText removed {
        // Placeholder - actual implementation would use proper text rendering
        drawRect(
            color = color.copy(alpha = 0.7f),
            topLeft = Offset(x, y),
            size = Size(text.length * 8f * scale, 12f * scale)
        )
    }
    
    // Toggle functions
    fun setShowPaths(show: Boolean) { showPaths = show }
    fun setShowVisionCones(show: Boolean) { showVisionCones = show }
    fun setShowDetectionRadius(show: Boolean) { showDetectionRadius = show }
    fun setShowStates(show: Boolean) { showStates = show }
    fun setShowStats(show: Boolean) { showStats = show }
}

// ════════════════════════════════════════════════════════════════════════════════
// تم الانتهاء من EnemyRenderer.kt
// EnemyRenderer.kt Complete
// ════════════════════════════════════════════════════════════════════════════════