package com.erygra.maskoflight.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.engine.ParticleEngine
import com.erygra.maskoflight.engine.AudioEngine
import com.erygra.maskoflight.player.EffectType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * # نظام المخاطر البيئية (Environmental Hazards System)
 * 
 * يدير جميع المخاطر البيئية في عالم إريغرا:
 * - Physical Hazards: أشواك، مناشير، كاسحات
 * - Elemental Hazards: نار، حمم، سم، كهرباء، جليد
 * - Environmental Hazards: رياح، صخور ساقطة، أشواك، رمال متحركة
 * - Void/Special Hazards: مناطق الفراغ، استنزاف الذاكرة، فساد
 * 
 * Features:
 * - Activation patterns (Timed, Trigger, Proximity, Always)
 * - Warning systems (Visual, Sound, Particle, Shake)
 * - Regional hazard sets (7 regions)
 * - Kill zones with respawn points
 * - Damage over time effects
 * - Status effect application
 * 
 * @author Erygra Development Team
 * @version 2.0
 * @since 2025-01-09
 */

// ══════════════════════════════════════════════════════════════════════════════
// Hazard Models
// ══════════════════════════════════════════════════════════════════════════════

/**
 * أنماط تفعيل المخاطر
 * Hazard activation patterns
 */
sealed class ActivationPattern {
    /**
     * نمط زمني - Timed pattern
     * @param intervalMs المدة بين التفعيلات (ms)
     * @param activeMs مدة النشاط (ms)
     * @param inactiveMs مدة الخمول (ms)
     * @param phaseOffset إزاحة المرحلة للمزامنة (ms)
     */
    data class Timed(
        val intervalMs: Long,
        val activeMs: Long,
        val inactiveMs: Long,
        val phaseOffset: Long = 0L
    ) : ActivationPattern()
    
    /**
     * تفعيل بالزناد - Trigger-based
     * @param triggerId معرّف الزناد
     * @param stayActiveMs مدة البقاء نشطاً (ms)
     * @param resetTimeMs وقت إعادة التعيين (ms)
     */
    data class Trigger(
        val triggerId: String,
        val stayActiveMs: Long,
        val resetTimeMs: Long = 0L
    ) : ActivationPattern()
    
    /**
     * تفعيل بالقرب - Proximity-based
     * @param radius نطاق التفعيل
     * @param activationDelay تأخير التفعيل (ms)
     * @param stayActiveAfterLeave البقاء نشطاً بعد المغادرة (ms)
     */
    data class PlayerProximity(
        val radius: Float,
        val activationDelay: Long = 0L,
        val stayActiveAfterLeave: Long = 0L
    ) : ActivationPattern()
    
    /**
     * نشط دائماً - Always active
     */
    object AlwaysActive : ActivationPattern()
}

/**
 * أنواع التحذيرات
 * Warning types for hazards
 */
enum class WarningType {
    /** مؤشر بصري - Visual indicator (glow, flash) */
    VISUAL_INDICATOR,
    /** إشارة صوتية - Sound cue (beeping, rumble) */
    SOUND_CUE,
    /** اهتزاز الشاشة - Screen shake */
    SCREEN_SHAKE,
    /** تأثير جسيمات - Particle effect (sparks, steam) */
    PARTICLE_EFFECT
}

// ══════════════════════════════════════════════════════════════════════════════
// Data Classes
// ══════════════════════════════════════════════════════════════════════════════

/**
 * بيانات المخاطر
 * Hazard data structure
 */
data class Hazard(
    /** معرّف فريد - Unique ID */
    val id: String,
    /** نوع المخطر - Hazard type */
    val type: HazardType,
    /** الموقع الأفقي - X position */
    val x: Float,
    /** الموقع العمودي - Y position */
    val y: Float,
    /** العرض - Width */
    val width: Float,
    /** الارتفاع - Height */
    val height: Float,
    /** الضرر لكل تلامس - Damage per contact */
    val damage: Float,
    /** الفاصل بين الضرر (ms) - Damage interval */
    val damageInterval: Long = 1000L,
    /** قوة الدفع - Knockback force */
    val knockbackForce: Float = 0f,
    /** اتجاه الدفع (درجات) - Knockback direction (degrees, 0 = right) */
    val knockbackDirection: Float = 180f,
    /** تأثير الحالة - Status effect to apply */
    val statusEffect: EffectType? = null,
    /** مدة التأثير (ms) - Effect duration */
    val effectDuration: Long = 0L,
    /** نشط - Is currently active */
    var isActive: Boolean = true,
    /** دائم - Is permanent (cannot be disabled) */
    val isPermanent: Boolean = true,
    /** نمط التفعيل - Activation pattern */
    val activationPattern: ActivationPattern = ActivationPattern.AlwaysActive,
    /** وقت التحذير (ms) - Warning time before activation */
    val warningTime: Long = 0L,
    /** أنواع التحذير - Warning types to use */
    val warningTypes: List<WarningType> = emptyList(),
    /** المنطقة - Region */
    val region: RegionType,
    /** اللون - Color for rendering */
    val color: Color = Color.Red,
    /** الشفافية - Opacity */
    val opacity: Float = 0.8f,
    /** سرعة الحركة (للمخاطر المتحركة) - Movement speed */
    val movementSpeed: Float = 0f,
    /** نطاق الحركة - Movement range */
    val movementRange: Float = 0f,
    /** معرّف الرسوم المتحركة - Animation ID */
    val animationId: String? = null
) {
    /** حدود المخطر - Hazard bounds */
    val bounds: Rect
        get() = Rect(x, y, x + width, y + height)
    
    /** آخر وقت إلحاق ضرر - Last damage time */
    var lastDamageTime: Long = 0L
    
    /** وقت بدء التحذير - Warning start time */
    var warningStartTime: Long = 0L
    
    /** حالة التحذير - Is warning */
    var isWarning: Boolean = false
    
    /** الموضع الأصلي (للحركة) - Original position */
    private val originalX = x
    private val originalY = y
    
    /** وقت بدء الحركة - Movement start time */
    var movementStartTime: Long = System.currentTimeMillis()
}

/**
 * مناطق القتل الفوري
 * Instant death zones
 */
data class KillZone(
    /** معرّف فريد - Unique ID */
    val id: String,
    /** المنطقة - Region */
    val region: RegionType,
    /** الحدود - Bounds */
    val bounds: Rect,
    /** النوع - Type */
    val type: KillZoneType,
    /** نقطة الإحياء - Respawn point */
    val respawnPoint: Pair<Float, Float>,
    /** رسالة الموت - Death message */
    val deathMessage: String = "",
    /** رسالة الموت بالعربية - Death message (Arabic) */
    val deathMessageArabic: String = ""
)

/**
 * أنواع مناطق القتل
 * Kill zone types
 */
enum class KillZoneType {
    /** فراغ - Instant void death */
    VOID,
    /** حمم - Lava death (damage then death) */
    LAVA,
    /** خارج الحدود - Out of bounds */
    OUT_OF_BOUNDS,
    /** سقوط - Fatal fall */
    FATAL_FALL
}

/**
 * تحذير المخطر
 * Hazard warning
 */
data class HazardWarning(
    /** معرّف المخطر - Hazard ID */
    val hazardId: String,
    /** نوع التحذير - Warning type */
    val warningType: WarningType,
    /** وقت البدء - Start time */
    val startTime: Long,
    /** المدة - Duration */
    val duration: Long,
    /** الشدة (0.0-1.0) - Intensity */
    val intensity: Float = 1.0f
) {
    /** التقدم (0.0-1.0) - Progress */
    fun getProgress(currentTime: Long): Float {
        val elapsed = currentTime - startTime
        return (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
    
    /** انتهى - Is finished */
    fun isFinished(currentTime: Long): Boolean {
        return currentTime >= startTime + duration
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Hazard Manager
// ══════════════════════════════════════════════════════════════════════════════

/**
 * مدير المخاطر البيئية
 * Environmental hazards manager
 */
class HazardManager(
    private val particleEngine: ParticleEngine,
    private val audioEngine: AudioEngine,
    private val eventBus: EventBus
) {
    /** المخاطر المسجلة - Registered hazards */
    private val _hazards = MutableStateFlow<List<Hazard>>(emptyList())
    val hazards: StateFlow<List<Hazard>> = _hazards.asStateFlow()
    
    /** مناطق القتل - Kill zones */
    private val _killZones = MutableStateFlow<List<KillZone>>(emptyList())
    val killZones: StateFlow<List<KillZone>> = _killZones.asStateFlow()
    
    /** التحذيرات النشطة - Active warnings */
    private val _activeWarnings = MutableStateFlow<List<HazardWarning>>(emptyList())
    val activeWarnings: StateFlow<List<HazardWarning>> = _activeWarnings.asStateFlow()
    
    /** الزناد المفعّلة - Activated triggers */
    private val activatedTriggers = mutableSetOf<String>()
    
    /** آخر وقت تحديث - Last update time */
    private var lastUpdateTime = System.currentTimeMillis()
    
    /**
     * تسجيل مخطر
     * Register a hazard
     */
    fun registerHazard(hazard: Hazard) {
        val currentHazards = _hazards.value.toMutableList()
        currentHazards.add(hazard)
        _hazards.value = currentHazards
        
        EventBus.emit(GameEvent.World.HazardRegistered(hazard.id, hazard.type, hazard.region))
    }
    
    /**
     * إلغاء تسجيل مخطر
     * Unregister a hazard
     */
    fun unregisterHazard(hazardId: String) {
        val hazard = _hazards.value.find { it.id == hazardId } ?: return
        _hazards.value = _hazards.value.filter { it.id != hazardId }
        
        EventBus.emit(GameEvent.World.HazardUnregistered(hazardId, hazard.type, hazard.region))
    }
    
    /**
     * تسجيل منطقة قتل
     * Register a kill zone
     */
    fun registerKillZone(killZone: KillZone) {
        val currentZones = _killZones.value.toMutableList()
        currentZones.add(killZone)
        _killZones.value = currentZones
    }
    
    /**
     * إلغاء تسجيل منطقة قتل
     * Unregister a kill zone
     */
    fun unregisterKillZone(killZoneId: String) {
        _killZones.value = _killZones.value.filter { it.id != killZoneId }
    }
    
    /**
     * تحديث المخاطر
     * Update hazards
     */
    fun updateHazards(deltaTime: Float) {
        val currentTime = System.currentTimeMillis()
        val deltaMiliseconds = (currentTime - lastUpdateTime).toFloat()
        lastUpdateTime = currentTime
        
        val updatedHazards = _hazards.value.map { hazard ->
            updateHazardActivation(hazard, currentTime)
        }
        _hazards.value = updatedHazards
        
        // Update warnings
        updateWarnings(currentTime)
    }
    
    /**
     * تحديث تفعيل المخطر
     * Update hazard activation
     */
    private fun updateHazardActivation(hazard: Hazard, currentTime: Long): Hazard {
        if (hazard.isPermanent && hazard.activationPattern is ActivationPattern.AlwaysActive) {
            return hazard
        }
        
        val wasActive = hazard.isActive
        
        when (val pattern = hazard.activationPattern) {
            is ActivationPattern.Timed -> {
                val phaseTime = (currentTime + pattern.phaseOffset) % pattern.intervalMs
                val shouldBeActive = phaseTime < pattern.activeMs
                
                if (!wasActive && shouldBeActive && hazard.warningTime > 0) {
                    // Start warning
                    startWarning(hazard, currentTime)
                    hazard.isWarning = true
                    hazard.warningStartTime = currentTime
                } else if (hazard.isWarning && currentTime >= hazard.warningStartTime + hazard.warningTime) {
                    // Warning ended, activate
                    hazard.isActive = true
                    hazard.isWarning = false
                    eventBus.emit(GameEvent.World.HazardActivated(hazard.id, hazard.type))
                } else if (!shouldBeActive) {
                    hazard.isActive = false
                    hazard.isWarning = false
                    if (wasActive) {
                        eventBus.emit(GameEvent.World.HazardDeactivated(hazard.id, hazard.type))
                    }
                }
            }
            
            is ActivationPattern.Trigger -> {
                if (activatedTriggers.contains(pattern.triggerId)) {
                    if (!wasActive) {
                        if (hazard.warningTime > 0) {
                            startWarning(hazard, currentTime)
                            hazard.isWarning = true
                            hazard.warningStartTime = currentTime
                        } else {
                            hazard.isActive = true
                            eventBus.emit(GameEvent.World.HazardActivated(hazard.id, hazard.type))
                        }
                    } else if (hazard.isWarning && currentTime >= hazard.warningStartTime + hazard.warningTime) {
                        hazard.isActive = true
                        hazard.isWarning = false
                        eventBus.emit(GameEvent.World.HazardActivated(hazard.id, hazard.type))
                    }
                    
                    // Check reset time
                    if (pattern.resetTimeMs > 0 && currentTime >= hazard.lastDamageTime + pattern.resetTimeMs) {
                        activatedTriggers.remove(pattern.triggerId)
                        hazard.isActive = false
                        hazard.isWarning = false
                        eventBus.emit(GameEvent.World.HazardDeactivated(hazard.id, hazard.type))
                    }
                }
            }
            
            is ActivationPattern.PlayerProximity -> {
                // Handled in checkPlayerCollision
            }
            
            ActivationPattern.AlwaysActive -> {
                hazard.isActive = true
            }
        }
        
        // Update movement for moving hazards
        if (hazard.movementSpeed > 0f && hazard.movementRange > 0f) {
            updateHazardMovement(hazard, currentTime)
        }
        
        return hazard
    }
    
    /**
     * تحديث حركة المخطر
     * Update hazard movement
     */
    private fun updateHazardMovement(hazard: Hazard, currentTime: Long) {
        val elapsed = (currentTime - hazard.movementStartTime).toFloat()
        val progress = (elapsed * hazard.movementSpeed / 1000f) % (hazard.movementRange * 2)
        
        val offset = if (progress < hazard.movementRange) {
            progress
        } else {
            hazard.movementRange * 2 - progress
        }
        
        // Update position based on hazard type
        when (hazard.type) {
            HazardType.SAW_BLADE, HazardType.SWINGING_BLADE -> {
                // Horizontal movement
                // Note: Direct modification not recommended, would need copy
            }
            else -> {}
        }
    }
    
    /**
     * بدء التحذير
     * Start warning for hazard
     */
    private fun startWarning(hazard: Hazard, currentTime: Long) {
        val warnings = hazard.warningTypes.map { type ->
            HazardWarning(
                hazardId = hazard.id,
                warningType = type,
                startTime = currentTime,
                duration = hazard.warningTime
            )
        }
        
        _activeWarnings.value = _activeWarnings.value + warnings
        
        // Play warning sounds
        if (hazard.warningTypes.contains(WarningType.SOUND_CUE)) {
            audioEngine.playSfx(getWarningSoundId(hazard.type))
        }
    }
    
    /**
     * تحديث التحذيرات
     * Update warnings
     */
    private fun updateWarnings(currentTime: Long) {
        _activeWarnings.value = _activeWarnings.value.filter { !it.isFinished(currentTime) }
    }
    
    /**
     * فحص تصادم اللاعب
     * Check player collision with hazards
     */
    fun checkPlayerCollision(
        playerX: Float,
        playerY: Float,
        playerWidth: Float = 32f,
        playerHeight: Float = 64f
    ): List<Hazard> {
        val currentTime = System.currentTimeMillis()
        val playerRect = Rect(playerX, playerY, playerX + playerWidth, playerY + playerHeight)
        
        return _hazards.value.filter { hazard ->
            if (!hazard.isActive || hazard.isWarning) return@filter false
            
            // Check collision
            val collides = hazard.bounds.overlaps(playerRect)
            
            if (collides) {
                // Check proximity pattern
                if (hazard.activationPattern is ActivationPattern.PlayerProximity) {
                    val pattern = hazard.activationPattern
                    val distance = getDistance(
                        playerX + playerWidth / 2,
                        playerY + playerHeight / 2,
                        hazard.x + hazard.width / 2,
                        hazard.y + hazard.height / 2
                    )
                    
                    if (distance <= pattern.radius) {
                        if (!hazard.isActive && pattern.activationDelay > 0) {
                            if (hazard.warningStartTime == 0L) {
                                hazard.warningStartTime = currentTime
                                startWarning(hazard, currentTime)
                                hazard.isWarning = true
                            } else if (currentTime >= hazard.warningStartTime + pattern.activationDelay) {
                                hazard.isActive = true
                                hazard.isWarning = false
                                eventBus.emit(GameEvent.World.HazardActivated(hazard.id, hazard.type))
                            }
                        }
                    } else if (hazard.isActive && pattern.stayActiveAfterLeave > 0) {
                        if (hazard.lastDamageTime > 0 && 
                            currentTime >= hazard.lastDamageTime + pattern.stayActiveAfterLeave) {
                            hazard.isActive = false
                            hazard.warningStartTime = 0L
                            eventBus.emit(GameEvent.World.HazardDeactivated(hazard.id, hazard.type))
                        }
                    }
                }
                
                // Check damage interval
                if (currentTime >= hazard.lastDamageTime + hazard.damageInterval) {
                    hazard.lastDamageTime = currentTime
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }
    
    /**
     * فحص منطقة القتل
     * Check kill zone
     */
    fun checkKillZone(playerX: Float, playerY: Float): KillZone? {
        val playerPoint = Offset(playerX, playerY)
        return _killZones.value.find { it.bounds.contains(playerPoint) }
    }
    
    /**
     * تفعيل مخطر
     * Activate hazard
     */
    fun activateHazard(hazardId: String) {
        val hazard = _hazards.value.find { it.id == hazardId } ?: return
        if (!hazard.isPermanent) {
            hazard.isActive = true
            EventBus.emit(GameEvent.World.HazardActivated(hazardId, hazard.type))
        }
    }
    
    /**
     * إلغاء تفعيل مخطر
     * Deactivate hazard
     */
    fun deactivateHazard(hazardId: String) {
        val hazard = _hazards.value.find { it.id == hazardId } ?: return
        if (!hazard.isPermanent) {
            hazard.isActive = false
            hazard.isWarning = false
            EventBus.emit(GameEvent.World.HazardDeactivated(hazardId, hazard.type))
        }
    }
    
    /**
     * تفعيل زناد
     * Activate trigger
     */
    fun activateTrigger(triggerId: String) {
        activatedTriggers.add(triggerId)
        EventBus.emit(GameEvent.World.TriggerActivated(triggerId))
    }
    
    /**
     * إلغاء تفعيل زناد
     * Deactivate trigger
     */
    fun deactivateTrigger(triggerId: String) {
        activatedTriggers.remove(triggerId)
        EventBus.emit(GameEvent.World.TriggerDeactivated(triggerId))
    }
    
    /**
     * الحصول على المخاطر في المنطقة
     * Get hazards in region
     */
    fun getHazardsInRegion(region: RegionType): List<Hazard> {
        return _hazards.value.filter { it.region == region }
    }
    
    /**
     * الحصول على المخاطر النشطة
     * Get active hazards
     */
    fun getActiveHazards(): List<Hazard> {
        return _hazards.value.filter { it.isActive }
    }
    
    /**
     * مسح كل المخاطر
     * Clear all hazards
     */
    fun clearAllHazards() {
        _hazards.value = emptyList()
        _killZones.value = emptyList()
        _activeWarnings.value = emptyList()
        activatedTriggers.clear()
    }
    
    /**
     * حفظ الحالة
     * Save state
     */
    fun saveState(): HazardManagerState {
        return HazardManagerState(
            hazards = _hazards.value.map { HazardState.fromHazard(it) },
            killZones = _killZones.value,
            activatedTriggers = activatedTriggers.toList()
        )
    }
    
    /**
     * تحميل الحالة
     * Load state
     */
    fun loadState(state: HazardManagerState) {
        _hazards.value = state.hazards.map { it.toHazard() }
        _killZones.value = state.killZones
        activatedTriggers.clear()
        activatedTriggers.addAll(state.activatedTriggers)
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // Helper Functions
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * حساب المسافة
     * Calculate distance
     */
    private fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    /**
     * الحصول على معرّف صوت التحذير
     * Get warning sound ID
     */
    private fun getWarningSoundId(type: HazardType): String {
        return when (type) {
            HazardType.SPIKES, HazardType.SPIKES_RETRACTABLE -> "sfx_spike_warning"
            HazardType.SAW_BLADE -> "sfx_saw_warning"
            HazardType.CRUSHER -> "sfx_crusher_warning"
            HazardType.ARROW_TRAP -> "sfx_arrow_warning"
            HazardType.LAVA, HazardType.LAVA_GEYSER -> "sfx_lava_warning"
            HazardType.ELECTRICITY, HazardType.ELECTRIC_FIELD -> "sfx_electric_warning"
            else -> "sfx_hazard_warning"
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Save State
// ══════════════════════════════════════════════════════════════════════════════

/**
 * حالة مدير المخاطر
 * Hazard manager state (for save/load)
 */
data class HazardManagerState(
    val hazards: List<HazardState>,
    val killZones: List<KillZone>,
    val activatedTriggers: List<String>
)

/**
 * حالة المخطر (للحفظ)
 * Hazard state (for serialization)
 */
data class HazardState(
    val id: String,
    val type: HazardType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val damage: Float,
    val damageInterval: Long,
    val knockbackForce: Float,
    val knockbackDirection: Float,
    val statusEffect: EffectType?,
    val effectDuration: Long,
    val isActive: Boolean,
    val isPermanent: Boolean,
    val activationPattern: ActivationPattern,
    val warningTime: Long,
    val warningTypes: List<WarningType>,
    val region: RegionType,
    val lastDamageTime: Long,
    val warningStartTime: Long,
    val isWarning: Boolean
) {
    fun toHazard(): Hazard {
        return Hazard(
            id = id,
            type = type,
            x = x,
            y = y,
            width = width,
            height = height,
            damage = damage,
            damageInterval = damageInterval,
            knockbackForce = knockbackForce,
            knockbackDirection = knockbackDirection,
            statusEffect = statusEffect,
            effectDuration = effectDuration,
            isActive = isActive,
            isPermanent = isPermanent,
            activationPattern = activationPattern,
            warningTime = warningTime,
            warningTypes = warningTypes,
            region = region
        ).also {
            it.lastDamageTime = lastDamageTime
            it.warningStartTime = warningStartTime
            it.isWarning = isWarning
        }
    }
    
    companion object {
        fun fromHazard(hazard: Hazard): HazardState {
            return HazardState(
                id = hazard.id,
                type = hazard.type,
                x = hazard.x,
                y = hazard.y,
                width = hazard.width,
                height = hazard.height,
                damage = hazard.damage,
                damageInterval = hazard.damageInterval,
                knockbackForce = hazard.knockbackForce,
                knockbackDirection = hazard.knockbackDirection,
                statusEffect = hazard.statusEffect,
                effectDuration = hazard.effectDuration,
                isActive = hazard.isActive,
                isPermanent = hazard.isPermanent,
                activationPattern = hazard.activationPattern,
                warningTime = hazard.warningTime,
                warningTypes = hazard.warningTypes,
                region = hazard.region,
                lastDamageTime = hazard.lastDamageTime,
                warningStartTime = hazard.warningStartTime,
                isWarning = hazard.isWarning
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Regional Hazard Database
// ══════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات المخاطر الإقليمية
 * Regional hazard database
 */
object RegionalHazardDatabase {
    
    /**
     * المخاطر حسب المنطقة
     * Hazards by region
     */
    fun getRegionHazardTypes(region: RegionType): List<HazardType> {
        return when (region) {
            RegionType.ASHEN_SPRAWL -> listOf(
                HazardType.FIRE,
                HazardType.LAVA,
                HazardType.LAVA_GEYSER,
                HazardType.FALLING_ROCKS,
                HazardType.SPIKES
            )
            
            RegionType.VEILED_ARCHIVES -> listOf(
                HazardType.FALLING_PLATFORM,
                HazardType.DARKNESS,
                HazardType.MEMORY_DRAIN,
                HazardType.ARROW_TRAP
            )
            
            RegionType.HOLLOWED_ARCHIPELAGO -> listOf(
                HazardType.WIND_CURRENT,
                HazardType.FALLING_PLATFORM,
                HazardType.COLLAPSING_FLOOR,
                HazardType.VOID_ZONE
            )
            
            RegionType.GLASSFJORD_CLIFFS -> listOf(
                HazardType.ICE,
                HazardType.FREEZING_WATER,
                HazardType.AVALANCHE,
                HazardType.SPIKES_RETRACTABLE
            )
            
            RegionType.SUNKEN_CLOCKWORKS -> listOf(
                HazardType.ELECTRICITY,
                HazardType.ELECTRIC_FIELD,
                HazardType.WATER_CURRENT,
                HazardType.SAW_BLADE,
                HazardType.CRUSHER
            )
            
            RegionType.BLACKROOT_MOORLANDS -> listOf(
                HazardType.POISON_GAS,
                HazardType.POISON_WATER,
                HazardType.THORNS,
                HazardType.QUICKSAND,
                HazardType.CORRUPTION
            )
            
            RegionType.LUMINOUS_CHASM -> listOf(
                HazardType.VOID_ZONE,
                HazardType.MEMORY_DRAIN,
                HazardType.DARKNESS,
                HazardType.FALLING_ROCKS
            )
        }
    }
    
    /**
     * إنشاء مخطر نموذجي
     * Create template hazard
     */
    fun createTemplateHazard(
        id: String,
        type: HazardType,
        region: RegionType,
        x: Float,
        y: Float
    ): Hazard {
        return when (type) {
            HazardType.SPIKES -> Hazard(
                id = id,
                type = type,
                x = x,
                y = y,
                width = 32f,
                height = 16f,
                damage = 15f,
                damageInterval = 500L,
                knockbackForce = 200f,
                knockbackDirection = 90f,
                region = region,
                color = Color(0xFF8B0000)
            )
            
            HazardType.FIRE -> Hazard(
                id = id,
                type = type,
                x = x,
                y = y,
                width = 64f,
                height = 64f,
                damage = 10f,
                damageInterval = 500L,
                statusEffect = EffectType.BURNING,
                effectDuration = 3000L,
                region = region,
                color = Color(0xFFFF4500)
            )
            
            HazardType.LAVA -> Hazard(
                id = id,
                type = type,
                x = x,
                y = y,
                width = 128f,
                height = 64f,
                damage = 25f,
                damageInterval = 300L,
                statusEffect = EffectType.BURNING,
                effectDuration = 5000L,
                region = region,
                color = Color(0xFFFF6347)
            )
            
            HazardType.ELECTRICITY -> Hazard(
                id = id,
                type = type,
                x = x,
                y = y,
                width = 48f,
                height = 96f,
                damage = 20f,
                damageInterval = 200L,
                statusEffect = EffectType.STUNNED,
                effectDuration = 1000L,
                knockbackForce = 300f,
                region = region,
                color = Color(0xFF00FFFF)
            )
            
            HazardType.POISON_GAS -> Hazard(
                id = id,
                type = type,
                x = x,
                y = y,
                width = 96f,
                height = 96f,
                damage = 5f,
                damageInterval = 1000L,
                statusEffect = EffectType.POISONED,
                effectDuration = 10000L,
                region = region,
                color = Color(0xFF9ACD32),
                opacity = 0.5f
            )
            
            else -> Hazard(
                id = id,
                type = type,
                x = x,
                y = y,
                width = 64f,
                height = 64f,
                damage = 10f,
                region = region
            )
        }
    }
}