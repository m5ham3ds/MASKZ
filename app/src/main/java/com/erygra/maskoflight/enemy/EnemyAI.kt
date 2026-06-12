package com.erygra.maskoflight.enemy

import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.engine.PhysicsEngine
import com.erygra.maskoflight.engine.CombatEngine
import com.erygra.maskoflight.player.PlayerStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * EnemyAI.kt — نظام الذكاء الاصطناعي للأعداء
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * النظام المتقدم للذكاء الاصطناعي في لعبة قِنَاعُ النُّور
 * يحتوي على:
 * - Finite State Machine (FSM) متقدم لكل حالة
 * - نظام Pathfinding بخوارزمية A* مبسطة
 * - نظام Detection/Vision متطور
 * - منطق اختيار الهجمات الذكي
 * - تنسيق المجموعات والسلوك الجماعي
 * - إطار عمل خاص لـ Boss AI
 * - EnemyAIManager مع تحسينات الأداء
 * 
 * Advanced Enemy AI System for Mask of Light
 * Features:
 * - Advanced Finite State Machine for all states
 * - A* pathfinding system
 * - Advanced detection/vision system
 * - Intelligent attack selection
 * - Group coordination and behavior
 * - Boss AI framework
 * - Optimized EnemyAIManager
 * 
 * @author M5ham3d
 * @version 2.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - AI Configuration Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * إعدادات الذكاء الاصطناعي لنوع معين من الأعداء
 * AI configuration for specific enemy type
 */
data class EnemyAIConfig(
    val enemyType: EnemyType,
    
    // Detection Settings
    val visionRange: Float = 300f,
    val visionAngle: Float = 120f, // Degrees
    val hearingRange: Float = 200f,
    val hasPeripheralVision: Boolean = false,
    val canSeeInDark: Boolean = false,
    val detectionSpeed: Float = 1f, // Multiplier for alert buildup
    
    // Behavior Settings
    val aggressionLevel: Float = 0.5f, // 0.0 = Passive, 1.0 = Aggressive
    val cowardiceThreshold: Float = 0.25f, // HP% to start fleeing
    val callForHelpThreshold: Float = 0.5f, // HP% to call allies
    val preferredDistance: Float = 100f, // Ideal combat distance
    val minAttackDistance: Float = 50f,
    val maxAttackDistance: Float = 300f,
    
    // Patrol Settings
    val patrolWaypointCount: Int = 3,
    val patrolWaitTimeMs: Long = 2000L,
    val patrolRandomness: Float = 0.3f, // Chance to pick random direction
    
    // Chase Settings
    val chaseUpdateIntervalMs: Long = 500L,
    val maxChaseDistance: Float = 800f, // Leash distance
    val giveUpChaseTimeMs: Long = 5000L,
    val canJumpWhileChasing: Boolean = true,
    val canClimbWhileChasing: Boolean = false,
    
    // Attack Settings
    val attackDecisionIntervalMs: Long = 800L,
    val comboChance: Float = 0.3f,
    val useSpecialAttackChance: Float = 0.2f,
    val minTimeBetweenAttacksMs: Long = 1000L,
    
    // Group Behavior
    val canCoordinate: Boolean = false,
    val leaderBonus: Float = 1.2f, // Damage/defense bonus when near leader
    val formationPreference: FormationType = FormationType.NONE,
    val maxGroupSize: Int = 5,
    
    // Special AI
    val hasPhases: Boolean = false,
    val phaseCount: Int = 1,
    val isAdaptive: Boolean = false, // Learn from player patterns
    val canUseTactics: Boolean = false
)

/**
 * نوع التشكيل للمجموعات
 * Formation type for enemy groups
 */
enum class FormationType {
    NONE,
    SURROUND, // Encircle player
    LINE, // Stay in a line
    VANGUARD, // Melee front, ranged back
    SCATTER, // Random positions
    PINCER // Attack from two sides
}

/**
 * نقطة مسار للدورية أو الملاحقة
 * Waypoint for patrol or pathfinding
 */
data class Waypoint(
    val x: Float,
    val y: Float,
    val waitTimeMs: Long = 0L,
    val isOptional: Boolean = false
)

/**
 * عقدة في نظام Pathfinding
 * Node in pathfinding system
 */
data class PathNode(
    val x: Float,
    val y: Float,
    val gCost: Float = Float.MAX_VALUE, // Cost from start
    val hCost: Float = 0f, // Heuristic to goal
    val parent: PathNode? = null,
    val isWalkable: Boolean = true,
    val isPlatform: Boolean = false,
    val requiresJump: Boolean = false
) {
    val fCost: Float get() = gCost + hCost
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PathNode) return false
        return abs(x - other.x) < 16f && abs(y - other.y) < 16f
    }
    
    override fun hashCode(): Int {
        return (x.toInt() / 16) * 31 + (y.toInt() / 16)
    }
}

/**
 * مسار محسوب
 * Calculated path
 */
data class Path(
    val nodes: List<PathNode>,
    val currentIndex: Int = 0,
    val calculatedAt: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
) {
    val currentNode: PathNode? get() = nodes.getOrNull(currentIndex)
    val nextNode: PathNode? get() = nodes.getOrNull(currentIndex + 1)
    val isComplete: Boolean get() = currentIndex >= nodes.size - 1
    val remainingDistance: Float get() {
        var distance = 0f
        for (i in currentIndex until nodes.size - 1) {
            val current = nodes[i]
            val next = nodes[i + 1]
            distance += sqrt((next.x - current.x).pow(2) + (next.y - current.y).pow(2))
        }
        return distance
    }
}

/**
 * مستوى التنبيه
 * Alert level
 */
enum class AlertLevel {
    UNAWARE, // Hasn't detected anything
    SUSPICIOUS, // Heard/saw something
    INVESTIGATING, // Moving to check
    ALERTED, // Confirmed threat
    COMBAT // In active combat
}

/**
 * بيانات الكشف
 * Detection data
 */
data class DetectionData(
    var alertLevel: AlertLevel = AlertLevel.UNAWARE,
    var alertProgress: Float = 0f, // 0.0 to 1.0
    var lastKnownPlayerX: Float = 0f,
    var lastKnownPlayerY: Float = 0f,
    var timeSinceLastSeen: Long = 0L,
    var hasLineOfSight: Boolean = false,
    var canHearPlayer: Boolean = false,
    var investigationPoint: Pair<Float, Float>? = null
)

/**
 * بيانات السلوك الجماعي
 * Group behavior data
 */
data class GroupData(
    val groupId: String,
    val leaderId: String? = null,
    val memberIds: MutableList<String> = mutableListOf(),
    var formationType: FormationType = FormationType.NONE,
    var targetPosition: Pair<Float, Float>? = null,
    var isCoordinating: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * مرحلة Boss
 * Boss phase
 */
data class BossPhase(
    val phaseNumber: Int,
    val hpThreshold: Float, // HP% to trigger this phase
    val name: String,
    val attacks: List<EnemyAttack>,
    val speedMultiplier: Float = 1f,
    val damageMultiplier: Float = 1f,
    val defenseMultiplier: Float = 1f,
    val canSummon: Boolean = false,
    val summonType: EnemyType? = null,
    val summonCount: Int = 0,
    val environmentalAttacks: List<String> = emptyList(),
    val invulnerableDuringTransition: Boolean = true,
    val transitionDurationMs: Long = 3000L
)

/**
 * سياق القرار
 * Decision context for AI
 */
data class DecisionContext(
    val enemy: Enemy,
    val playerDistance: Float,
    val playerDirection: Float, // Angle to player in radians
    val hasLineOfSight: Boolean,
    val alliesNearby: Int,
    val enemiesNearby: Int,
    val hpPercentage: Float,
    val energyPercentage: Float,
    val canAttack: Boolean,
    val isPlayerAttacking: Boolean,
    val timeSinceLastAttack: Long,
    val availableAttacks: List<EnemyAttack>
)

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - AI State Machine
// ════════════════════════════════════════════════════════════════════════════════

/**
 * آلة الحالة المحدودة للعدو
 * Finite State Machine for enemy AI
 */
class EnemyStateMachine(
    private val enemy: Enemy,
    private val config: EnemyAIConfig
) {
    private var currentState: EnemyState = EnemyState.IDLE
    private var previousState: EnemyState = EnemyState.IDLE
    private var stateEnterTime: Long = System.currentTimeMillis()
    private val stateHistory = mutableListOf<Pair<EnemyState, Long>>()
    
    // State-specific data
    private var patrolWaypoints = mutableListOf<Waypoint>()
    private var currentWaypointIndex = 0
    private var waypointReachedTime: Long = 0L
    
    private var chasePath: Path? = null
    private var lastPathUpdateTime: Long = 0L
    
    private var currentAttack: EnemyAttack? = null
    private var attackStartTime: Long = 0L
    private var attackPhase: AttackPhase = AttackPhase.IDLE
    
    private var returnPath: Path? = null
    private var healthBeforeReturn: Float = 0f
    
    enum class AttackPhase {
        IDLE, WINDUP, EXECUTE, RECOVERY
    }
    
    /**
     * تحديث آلة الحالة
     * Update the state machine
     */
    fun update(
        deltaTime: Float,
        playerStateManager: PlayerStateManager,
        detectionData: DetectionData,
        groupData: GroupData?,
        physicsEngine: PhysicsEngine,
        combatEngine: CombatEngine
    ) {
        val timeInState = System.currentTimeMillis() - stateEnterTime
        
        // Check for state transitions
        val newState = checkTransitions(
            playerStateManager,
            detectionData,
            groupData,
            timeInState
        )
        
        if (newState != currentState) {
            transitionTo(newState)
        }
        
        // Execute current state behavior
        when (currentState) {
            EnemyState.IDLE -> updateIdle(deltaTime, detectionData)
            EnemyState.PATROL -> updatePatrol(deltaTime, detectionData, physicsEngine)
            EnemyState.ALERT -> updateAlert(deltaTime, detectionData, physicsEngine)
            EnemyState.CHASING -> updateChasing(deltaTime, playerStateManager, physicsEngine)
            EnemyState.ATTACKING -> updateAttacking(deltaTime, playerStateManager, combatEngine)
            EnemyState.FLEEING -> updateFleeing(deltaTime, playerStateManager, physicsEngine)
            EnemyState.STUNNED -> updateStunned(deltaTime)
            EnemyState.RETURNING -> updateReturning(deltaTime, physicsEngine)
            EnemyState.DEAD -> updateDead(deltaTime)
        }
    }
    
    /**
     * فحص التحولات بين الحالات
     * Check for state transitions
     */
    private fun checkTransitions(
        playerStateManager: PlayerStateManager,
        detectionData: DetectionData,
        groupData: GroupData?,
        timeInState: Long
    ): EnemyState {
        val stats = enemy.stats
        val position = enemy.position
        
        // Dead state is permanent
        if (currentState == EnemyState.DEAD) return EnemyState.DEAD
        
        // Stunned state must expire
        if (currentState == EnemyState.STUNNED) {
            val stunEffect = enemy.activeEffects.find { it.type == EnemyEffectType.STUN }
            if (stunEffect == null || stunEffect.isExpired) {
                return previousState // Return to previous state
            }
            return EnemyState.STUNNED
        }
        
        // Check death
        if (stats.currentHp <= 0) {
            return EnemyState.DEAD
        }
        
        // Check stun
        if (enemy.hasEffect(EnemyEffectType.STUN)) {
            return EnemyState.STUNNED
        }
        
        // Check fear effect
        if (enemy.hasEffect(EnemyEffectType.FEAR)) {
            return EnemyState.FLEEING
        }
        
        val playerPos = playerStateManager.getPosition()
        val distanceToPlayer = enemy.distanceTo(playerPos.x, playerPos.y)
        val distanceToSpawn = sqrt(
            (position.x - enemy.spawnPoint.first).pow(2) +
            (position.y - enemy.spawnPoint.second).pow(2)
        )
        
        // Priority 1: Return if too far from spawn (leash)
        if (distanceToSpawn > config.maxChaseDistance &&
            currentState !in listOf(EnemyState.RETURNING, EnemyState.FLEEING)) {
            return EnemyState.RETURNING
        }
        
        // Priority 2: Flee if low HP
        val hpPercent = stats.currentHp / stats.maxHp
        if (hpPercent < config.cowardiceThreshold &&
            currentState !in listOf(EnemyState.FLEEING, EnemyState.RETURNING)) {
            return EnemyState.FLEEING
        }
        
        // Priority 3: Combat states (if player detected)
        if (detectionData.alertLevel == AlertLevel.COMBAT) {
            return when {
                distanceToPlayer <= config.maxAttackDistance && 
                distanceToPlayer >= config.minAttackDistance &&
                canAttack() -> EnemyState.ATTACKING
                
                distanceToPlayer > config.maxAttackDistance ||
                distanceToPlayer < config.minAttackDistance -> EnemyState.CHASING
                
                currentState == EnemyState.ATTACKING -> EnemyState.ATTACKING
                
                else -> EnemyState.CHASING
            }
        }
        
        // Priority 4: Alert states
        if (detectionData.alertLevel == AlertLevel.ALERTED) {
            return EnemyState.CHASING
        }
        
        if (detectionData.alertLevel == AlertLevel.INVESTIGATING) {
            return EnemyState.ALERT
        }
        
        // Priority 5: Return if damaged but no longer in combat
        if (currentState == EnemyState.RETURNING) {
            if (distanceToSpawn < 32f) { // Reached spawn
                return if (enemy.definition.canPatrol) EnemyState.PATROL else EnemyState.IDLE
            }
            return EnemyState.RETURNING
        }
        
        // Priority 6: Default patrol/idle
        return when {
            currentState == EnemyState.PATROL && patrolWaypoints.isNotEmpty() -> EnemyState.PATROL
            enemy.definition.canPatrol && currentState == EnemyState.IDLE && timeInState > 3000L -> EnemyState.PATROL
            else -> currentState
        }
    }
    
    /**
     * الانتقال إلى حالة جديدة
     * Transition to new state
     */
    private fun transitionTo(newState: EnemyState) {
        // Exit current state
        exitState(currentState)
        
        // Record history
        stateHistory.add(Pair(currentState, System.currentTimeMillis() - stateEnterTime))
        if (stateHistory.size > 10) {
            stateHistory.removeAt(0)
        }
        
        // Update states
        previousState = currentState
        currentState = newState
        stateEnterTime = System.currentTimeMillis()
        
        // Update enemy state
        enemy.currentState = newState
        
        // Enter new state
        enterState(newState)
        
        // Emit event
        EventBus.emit(GameEvent.Enemy.StateChanged(
            enemyId = enemy.id,
            enemyType = enemy.definition.type,
            oldState = previousState,
            newState = newState
        ))
    }
    
    /**
     * دخول حالة جديدة
     * Enter a state
     */
    private fun enterState(state: EnemyState) {
        when (state) {
            EnemyState.IDLE -> {
                enemy.position.velocityX = 0f
                enemy.position.velocityY = 0f
            }
            
            EnemyState.PATROL -> {
                if (patrolWaypoints.isEmpty()) {
                    generatePatrolWaypoints()
                }
                currentWaypointIndex = 0
            }
            
            EnemyState.ALERT -> {
                // Face investigation point
            }
            
            EnemyState.CHASING -> {
                chasePath = null
                lastPathUpdateTime = 0L
            }
            
            EnemyState.ATTACKING -> {
                attackPhase = AttackPhase.IDLE
                currentAttack = null
            }
            
            EnemyState.FLEEING -> {
                // Clear attack
                currentAttack = null
            }
            
            EnemyState.STUNNED -> {
                enemy.position.velocityX = 0f
                currentAttack = null
            }
            
            EnemyState.RETURNING -> {
                returnPath = null
                healthBeforeReturn = enemy.stats.currentHp.toFloat()
            }
            
            EnemyState.DEAD -> {
                enemy.position.velocityX = 0f
                enemy.position.velocityY = 0f
                currentAttack = null
            }
        }
    }
    
    /**
     * الخروج من حالة
     * Exit a state
     */
    private fun exitState(state: EnemyState) {
        when (state) {
            EnemyState.ATTACKING -> {
                currentAttack = null
                attackPhase = AttackPhase.IDLE
            }
            EnemyState.PATROL -> {
                waypointReachedTime = 0L
            }
            else -> {}
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - State Update Functions
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * تحديث حالة الخمول
     * Update idle state
     */
    private fun updateIdle(deltaTime: Float, detectionData: DetectionData) {
        // Look around occasionally
        if (Random.nextFloat() < 0.01f) {
            enemy.position.isFacingRight = !enemy.position.isFacingRight
        }
        
        // Apply gravity if not grounded
        if (!enemy.position.isGrounded) {
            enemy.position.velocityY += GameConfig.PhysicsConfig.GRAVITY * deltaTime
        }
    }
    
    /**
     * تحديث حالة الدورية
     * Update patrol state
     */
    private fun updatePatrol(
        deltaTime: Float,
        detectionData: DetectionData,
        physicsEngine: PhysicsEngine
    ) {
        if (patrolWaypoints.isEmpty()) {
            generatePatrolWaypoints()
            return
        }
        
        val currentWaypoint = patrolWaypoints[currentWaypointIndex]
        val position = enemy.position
        
        val dx = currentWaypoint.x - position.x
        val dy = currentWaypoint.y - position.y
        val distance = sqrt(dx * dx + dy * dy)
        
        // Reached waypoint
        if (distance < 32f) {
            if (waypointReachedTime == 0L) {
                waypointReachedTime = System.currentTimeMillis()
                position.velocityX = 0f
            } else if (System.currentTimeMillis() - waypointReachedTime > currentWaypoint.waitTimeMs) {
                // Move to next waypoint
                currentWaypointIndex = (currentWaypointIndex + 1) % patrolWaypoints.size
                waypointReachedTime = 0L
            }
            return
        }
        
        // Move toward waypoint
        val direction = if (dx > 0) 1f else -1f
        position.isFacingRight = direction > 0
        position.velocityX = direction * enemy.definition.patrolSpeed
        
        // Jump if needed
        if (abs(dy) > 64f && position.isGrounded && config.canJumpWhileChasing) {
            position.velocityY = -sqrt(2 * GameConfig.PhysicsConfig.GRAVITY * abs(dy)).toFloat()
        }
    }
    
    /**
     * تحديث حالة التنبيه
     * Update alert state
     */
    private fun updateAlert(
        deltaTime: Float,
        detectionData: DetectionData,
        physicsEngine: PhysicsEngine
    ) {
        val investigationPoint = detectionData.investigationPoint ?: return
        val position = enemy.position
        
        val dx = investigationPoint.first - position.x
        val dy = investigationPoint.second - position.y
        val distance = sqrt(dx * dx + dy * dy)
        
        // Face investigation point
        position.isFacingRight = dx > 0
        
        // Move toward investigation point slowly
        if (distance > 48f) {
            val direction = if (dx > 0) 1f else -1f
            position.velocityX = direction * enemy.definition.patrolSpeed * 0.6f
        } else {
            position.velocityX = 0f
            // Look around
            if (Random.nextFloat() < 0.02f) {
                position.isFacingRight = !position.isFacingRight
            }
        }
    }
    
    /**
     * تحديث حالة الملاحقة
     * Update chasing state
     */
    private fun updateChasing(
        deltaTime: Float,
        playerStateManager: PlayerStateManager,
        physicsEngine: PhysicsEngine
    ) {
        val playerPos = playerStateManager.getPosition()
        val position = enemy.position
        
        // Update path periodically
        val now = System.currentTimeMillis()
        if (chasePath == null || 
            now - lastPathUpdateTime > config.chaseUpdateIntervalMs ||
            !chasePath!!.isValid) {
            chasePath = findPath(
                position.x, position.y,
                playerPos.x, playerPos.y,
                physicsEngine
            )
            lastPathUpdateTime = now
        }
        
        val path = chasePath
        if (path != null && !path.isComplete) {
            followPath(path, deltaTime, physicsEngine)
        } else {
            // Direct chase if no path
            val dx = playerPos.x - position.x
            val direction = if (dx > 0) 1f else -1f
            position.isFacingRight = direction > 0
            position.velocityX = direction * enemy.definition.chaseSpeed
            
            // Jump if player is higher
            if (playerPos.y < position.y - 64f && position.isGrounded && config.canJumpWhileChasing) {
                position.velocityY = -sqrt(2 * GameConfig.PhysicsConfig.GRAVITY * abs(playerPos.y - position.y)).toFloat()
            }
        }
    }
    
    /**
     * تحديث حالة الهجوم
     * Update attacking state
     */
    private fun updateAttacking(
        deltaTime: Float,
        playerStateManager: PlayerStateManager,
        combatEngine: CombatEngine
    ) {
        val now = System.currentTimeMillis()
        
        when (attackPhase) {
            AttackPhase.IDLE -> {
                // Select attack
                currentAttack = selectAttack(playerStateManager)
                if (currentAttack != null) {
                    attackPhase = AttackPhase.WINDUP
                    attackStartTime = now
                    
                    EventBus.emit(GameEvent.Enemy.AttackStarted(
                        enemyId = enemy.id,
                        enemyType = enemy.definition.type,
                        attackName = currentAttack!!.name
                    ))
                }
            }
            
            AttackPhase.WINDUP -> {
                val elapsed = now - attackStartTime
                if (elapsed >= currentAttack!!.windupMs) {
                    attackPhase = AttackPhase.EXECUTE
                    executeAttack(currentAttack!!, playerStateManager, combatEngine)
                }
                
                // Face player during windup
                val playerPos = playerStateManager.getPosition()
                enemy.position.isFacingRight = playerPos.x > enemy.position.x
            }
            
            AttackPhase.EXECUTE -> {
                val elapsed = now - attackStartTime - currentAttack!!.windupMs
                if (elapsed >= 200L) { // Attack execution duration
                    attackPhase = AttackPhase.RECOVERY
                }
            }
            
            AttackPhase.RECOVERY -> {
                val elapsed = now - attackStartTime - currentAttack!!.windupMs
                if (elapsed >= currentAttack!!.recoveryMs) {
                    // Attack complete
                    attackPhase = AttackPhase.IDLE
                    currentAttack = null
                    enemy.lastAttackTime = now
                }
            }
        }
        
        // Stop movement during attack (for most enemies)
        if (!enemy.definition.canMoveWhileAttacking) {
            enemy.position.velocityX = 0f
        }
    }
    
    /**
     * تحديث حالة الهروب
     * Update fleeing state
     */
    private fun updateFleeing(
        deltaTime: Float,
        playerStateManager: PlayerStateManager,
        physicsEngine: PhysicsEngine
    ) {
        val playerPos = playerStateManager.getPosition()
        val position = enemy.position
        
        // Run away from player
        val dx = position.x - playerPos.x
        val direction = if (dx > 0) 1f else -1f
        position.isFacingRight = direction > 0
        position.velocityX = direction * enemy.definition.chaseSpeed * 1.2f // Run faster
        
        // Jump if blocked
        if (position.isOnWall && position.isGrounded) {
            position.velocityY = com.erygra.maskoflight.core.GameConfig.PhysicsConfig.JUMP_FORCE
        }
        
        // Try to increase distance
        val distance = enemy.distanceTo(playerPos.x, playerPos.y)
        if (distance > enemy.definition.aggroRange * 1.5f) {
            // Safe distance reached, stop fleeing
            EventBus.emit(GameEvent.Enemy.StateChanged(
                enemyId = enemy.id,
                enemyType = enemy.definition.type,
                oldState = EnemyState.FLEEING,
                newState = EnemyState.RETURNING
            ))
        }
    }
    
    /**
     * تحديث حالة الذهول
     * Update stunned state
     */
    private fun updateStunned(deltaTime: Float) {
        // Can't do anything while stunned
        enemy.position.velocityX = 0f
        
        // Particle effects handled by renderer
    }
    
    /**
     * تحديث حالة العودة
     * Update returning state
     */
    private fun updateReturning(
        deltaTime: Float,
        physicsEngine: PhysicsEngine
    ) {
        val spawnPoint = enemy.spawnPoint
        val position = enemy.position
        
        // Update path
        val now = System.currentTimeMillis()
        if (returnPath == null || now - lastPathUpdateTime > 1000L) {
            returnPath = findPath(
                position.x, position.y,
                spawnPoint.first, spawnPoint.second,
                physicsEngine
            )
            lastPathUpdateTime = now
        }
        
        val path = returnPath
        if (path != null && !path.isComplete) {
            followPath(path, deltaTime, physicsEngine)
        } else {
            // Direct movement
            val dx = spawnPoint.first - position.x
            val direction = if (dx > 0) 1f else -1f
            position.isFacingRight = direction > 0
            position.velocityX = direction * enemy.definition.patrolSpeed
        }
        
        // Regenerate health slowly
        if (enemy.definition.regeneratesHealth) {
            val regenAmount = enemy.stats.maxHp * 0.01f * deltaTime // 1% per second
            enemy.heal(regenAmount.toInt())
        }
        
        // Check if reached spawn
        val distance = sqrt(
            (position.x - spawnPoint.first).pow(2) +
            (position.y - spawnPoint.second).pow(2)
        )
        
        if (distance < 32f) {
            enemy.resetToSpawn()
        }
    }
    
    /**
     * تحديث حالة الموت
     * Update dead state
     */
    private fun updateDead(deltaTime: Float) {
        // Death animation handled by renderer
        // This state just ensures no further updates occur
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Helper Functions
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * توليد نقاط الدورية
     * Generate patrol waypoints
     */
    private fun generatePatrolWaypoints() {
        patrolWaypoints.clear()
        val spawnX = enemy.spawnPoint.first
        val spawnY = enemy.spawnPoint.second
        
        for (i in 0 until config.patrolWaypointCount) {
            val offsetX = Random.nextFloat() * 400f - 200f
            val offsetY = if (enemy.definition.canFly) Random.nextFloat() * 200f - 100f else 0f
            
            patrolWaypoints.add(
                Waypoint(
                    x = spawnX + offsetX,
                    y = spawnY + offsetY,
                    waitTimeMs = config.patrolWaitTimeMs + Random.nextLong(-500L, 500L)
                )
            )
        }
    }
    
    /**
     * التحقق من إمكانية الهجوم
     * Check if enemy can attack
     */
    private fun canAttack(): Boolean {
        val now = System.currentTimeMillis()
        if (now - enemy.lastAttackTime < config.minTimeBetweenAttacksMs) {
            return false
        }
        
        if (attackPhase != AttackPhase.IDLE) {
            return false
        }
        
        return enemy.definition.attacks.any { attack ->
            now - (enemy.attackCooldowns[attack.name] ?: 0L) >= attack.cooldownMs
        }
    }
    
    /**
     * اختيار الهجوم المناسب
     * Select appropriate attack
     */
    private fun selectAttack(playerStateManager: PlayerStateManager): EnemyAttack? {
        val playerPos = playerStateManager.getPosition()
        val distance = enemy.distanceTo(playerPos.x, playerPos.y)
        val now = System.currentTimeMillis()
        
        // Filter available attacks
        val availableAttacks = enemy.definition.attacks.filter { attack ->
            val cooldownExpired = now - (enemy.attackCooldowns[attack.name] ?: 0L) >= attack.cooldownMs
            val inRange = distance >= attack.minRange && distance <= attack.range
            cooldownExpired && inRange
        }
        
        if (availableAttacks.isEmpty()) return null
        
        // Weight attacks by priority
        val weights = availableAttacks.map { attack ->
            var weight = 1f
            
            // Prefer attacks on cooldown less recently
            val timeSinceUse = now - (enemy.attackCooldowns[attack.name] ?: 0L)
            weight *= (timeSinceUse / attack.cooldownMs.toFloat()).coerceIn(1f, 2f)
            
            // Prefer higher damage
            weight *= (attack.damage / 10f)
            
            // Special attacks get bonus
            if (attack.type != AttackType.MELEE_BASIC && Random.nextFloat() < config.useSpecialAttackChance) {
                weight *= 1.5f
            }
            
            weight
        }
        
        // Select weighted random
        val totalWeight = weights.sum()
        var random = Random.nextFloat() * totalWeight
        
        for (i in availableAttacks.indices) {
            random -= weights[i]
            if (random <= 0f) {
                return availableAttacks[i]
            }
        }
        
        return availableAttacks.first()
    }
    
    /**
     * تنفيذ الهجوم
     * Execute attack
     */
    private fun executeAttack(
        attack: EnemyAttack,
        playerStateManager: PlayerStateManager,
        combatEngine: CombatEngine
    ) {
        val playerPos = playerStateManager.getPosition()
        val enemyPos = enemy.position
        
        when (attack.type) {
            AttackType.MELEE_BASIC, AttackType.MELEE_HEAVY -> {
                // Check if player is in range and direction
                val dx = playerPos.x - enemyPos.x
                val distance = abs(dx)
                val correctDirection = (dx > 0) == enemyPos.isFacingRight
                
                if (distance <= attack.range && correctDirection) {
                    val damage = attack.damage
                    
                    playerStateManager.takeDamage(
                        damage = damage,
                        source = "Enemy_${enemy.definition.type}"
                    )
                    
                    playerStateManager.updatePosition(
                        velocityX = if (enemyPos.isFacingRight) attack.knockback else -attack.knockback,
                        velocityY = -attack.knockback * 0.5f
                    )
                    
                    // Apply status effects
                    attack.statusEffects.forEach { effect ->
                        val efType = try {
                            com.erygra.maskoflight.player.EffectType.valueOf(effect.uppercase())
                        } catch (e: Exception) {
                            null
                        }
                        if (efType != null) {
                            playerStateManager.addEffect(
                                com.erygra.maskoflight.player.Effect(
                                    type = efType,
                                    strength = 1f,
                                    duration = 2000L,
                                    remaining = 2000L,
                                    source = "Enemy_${enemy.definition.type}"
                                )
                            )
                        }
                    }
                }
            }
            
            AttackType.RANGED_PROJECTILE -> {
                // Spawn projectile
                val direction = if (enemyPos.isFacingRight) 1f else -1f
                val angle = atan2(playerPos.y - enemyPos.y, playerPos.x - enemyPos.x)
                
                EventBus.emit(GameEvent.Enemy.ProjectileSpawned(
                    enemyId = enemy.id,
                    projectileType = attack.name,
                    x = enemyPos.x + direction * 32f,
                    y = enemyPos.y - 16f,
                    velocityX = cos(angle) * attack.projectileSpeed,
                    velocityY = sin(angle) * attack.projectileSpeed,
                    damage = attack.damage.toFloat()
                ))
            }
            
            AttackType.AOE_EXPLOSION -> {
                // Damage all in radius
                val distance = enemy.distanceTo(playerPos.x, playerPos.y)
                if (distance <= attack.aoeRadius) {
                    val damage = attack.damage
                    
                    playerStateManager.takeDamage(
                        damage = damage,
                        source = "Enemy_${enemy.definition.type}_AOE"
                    )
                    
                    playerStateManager.updatePosition(
                        velocityX = if (playerPos.x > enemyPos.x) attack.knockback else -attack.knockback,
                        velocityY = -attack.knockback
                    )
                }
            }
            
            AttackType.GRAB -> {
                // Grab attack (stun + damage over time)
                val distance = enemy.distanceTo(playerPos.x, playerPos.y)
                if (distance <= attack.range) {
                    playerStateManager.addEffect(
                        com.erygra.maskoflight.player.Effect(
                            type = com.erygra.maskoflight.player.EffectType.STUNNED,
                            duration = 2000L,
                            remaining = 2000L,
                            source = "Enemy_Grab"
                        )
                    )
                }
            }
            
            else -> {
                // Other attack types handled by specific enemy implementations
            }
        }
        
        // Update cooldown
        enemy.attackCooldowns[attack.name] = System.currentTimeMillis()
        
        EventBus.emit(GameEvent.Enemy.AttackExecuted(
            enemyId = enemy.id,
            enemyType = enemy.definition.type,
            attackName = attack.name,
            damage = attack.damage.toFloat()
        ))
    }
    
    /**
     * اتباع المسار
     * Follow path
     */
    private fun followPath(
        path: Path,
        deltaTime: Float,
        physicsEngine: PhysicsEngine
    ) {
        val currentNode = path.currentNode ?: return
        val position = enemy.position
        
        val dx = currentNode.x - position.x
        val dy = currentNode.y - position.y
        val distance = sqrt(dx * dx + dy * dy)
        
        // Reached node, advance to next
        if (distance < 16f) {
            if (!path.isComplete) {
                chasePath = path.copy(currentIndex = path.currentIndex + 1)
            }
            return
        }
        
        // Move toward node
        val direction = if (dx > 0) 1f else -1f
        position.isFacingRight = direction > 0
        position.velocityX = direction * enemy.definition.chaseSpeed
        
        // Jump if node requires it
        if (currentNode.requiresJump && position.isGrounded && config.canJumpWhileChasing) {
            position.velocityY = com.erygra.maskoflight.core.GameConfig.PhysicsConfig.JUMP_FORCE
        }
    }
    
    /**
     * إيجاد مسار باستخدام A*
     * Find path using A* algorithm
     */
    private fun findPath(
        startX: Float,
        startY: Float,
        goalX: Float,
        goalY: Float,
        physicsEngine: PhysicsEngine
    ): Path? {
        // Simplified A* for performance
        // Grid size: 32x32 pixels per node
        val gridSize = 32f
        
        val startNode = PathNode(
            x = (startX / gridSize).roundToInt() * gridSize,
            y = (startY / gridSize).roundToInt() * gridSize,
            gCost = 0f,
            hCost = manhattanDistance(startX, startY, goalX, goalY)
        )
        
        val goalNode = PathNode(
            x = (goalX / gridSize).roundToInt() * gridSize,
            y = (goalY / gridSize).roundToInt() * gridSize
        )
        
        // If goal is very close, return direct path
        val directDistance = sqrt((goalX - startX).pow(2) + (goalY - startY).pow(2))
        if (directDistance < 128f) {
            return Path(
                nodes = listOf(startNode, goalNode),
                currentIndex = 0
            )
        }
        
        val openSet = mutableListOf(startNode)
        val closedSet = mutableSetOf<PathNode>()
        val cameFrom = mutableMapOf<PathNode, PathNode>()
        
        var iterations = 0
        val maxIterations = 100 // Limit for performance
        
        while (openSet.isNotEmpty() && iterations < maxIterations) {
            iterations++
            
            // Get node with lowest fCost
            val current = openSet.minByOrNull { it.fCost } ?: break
            
            // Reached goal
            if (current == goalNode) {
                return reconstructPath(cameFrom, current)
            }
            
            openSet.remove(current)
            closedSet.add(current)
            
            // Check neighbors (8 directions)
            val neighbors = getNeighbors(current, gridSize, physicsEngine)
            
            for (neighbor in neighbors) {
                if (neighbor in closedSet) continue
                
                val tentativeGCost = current.gCost + 
                    sqrt((neighbor.x - current.x).pow(2) + (neighbor.y - current.y).pow(2))
                
                if (neighbor !in openSet) {
                    openSet.add(neighbor)
                } else if (tentativeGCost >= neighbor.gCost) {
                    continue
                }
                
                cameFrom[neighbor] = current
                val updatedNeighbor = neighbor.copy(
                    gCost = tentativeGCost,
                    hCost = manhattanDistance(neighbor.x, neighbor.y, goalX, goalY),
                    parent = current
                )
                
                openSet.remove(neighbor)
                openSet.add(updatedNeighbor)
            }
        }
        
        // No path found, return null
        return null
    }
    
    /**
     * إعادة بناء المسار
     * Reconstruct path from A* result
     */
    private fun reconstructPath(cameFrom: Map<PathNode, PathNode>, current: PathNode): Path {
        val path = mutableListOf(current)
        var node = current
        
        while (cameFrom.containsKey(node)) {
            node = cameFrom[node]!!
            path.add(0, node)
        }
        
        return Path(nodes = path, currentIndex = 0)
    }
    
    /**
     * الحصول على الجيران في الشبكة
     * Get neighbor nodes
     */
    private fun getNeighbors(node: PathNode, gridSize: Float, physicsEngine: PhysicsEngine): List<PathNode> {
        val neighbors = mutableListOf<PathNode>()
        val directions = listOf(
            Pair(-1, 0), Pair(1, 0), // Left, Right
            Pair(0, -1), Pair(0, 1), // Up, Down
            Pair(-1, -1), Pair(1, -1), // Diagonals
            Pair(-1, 1), Pair(1, 1)
        )
        
        for ((dx, dy) in directions) {
            val newX = node.x + dx * gridSize
            val newY = node.y + dy * gridSize
            
            // Simple walkability check (would need proper collision detection)
            val isWalkable = true // Placeholder
            
            if (isWalkable) {
                neighbors.add(
                    PathNode(
                        x = newX,
                        y = newY,
                        isWalkable = true,
                        requiresJump = dy < 0 // Going up requires jump
                    )
                )
            }
        }
        
        return neighbors
    }
    
    /**
     * حساب المسافة Manhattan
     * Calculate Manhattan distance
     */
    private fun manhattanDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return abs(x2 - x1) + abs(y2 - y1)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Public Interface
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun getCurrentState(): EnemyState = currentState
    fun getTimeInState(): Long = System.currentTimeMillis() - stateEnterTime
    fun getStateHistory(): List<Pair<EnemyState, Long>> = stateHistory.toList()
    fun getCurrentAttack(): EnemyAttack? = currentAttack
    fun getAttackPhase(): AttackPhase = attackPhase
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Detection System
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نظام الكشف للأعداء
 * Enemy detection system
 */
class EnemyDetectionSystem(
    private val enemy: Enemy,
    private val config: EnemyAIConfig
) {
    private val detectionData = DetectionData()
    
    /**
     * تحديث نظام الكشف
     * Update detection system
     */
    fun update(
        deltaTime: Float,
        playerStateManager: PlayerStateManager,
        physicsEngine: PhysicsEngine
    ): DetectionData {
        val playerPos = playerStateManager.getPosition()
        val enemyPos = enemy.position
        
        val distance = enemy.distanceTo(playerPos.x, playerPos.y)
        
        // Vision check
        val hasLOS = checkLineOfSight(playerPos.x, playerPos.y, physicsEngine)
        val inVisionCone = checkVisionCone(playerPos.x, playerPos.y)
        val canSeePlayer = hasLOS && (inVisionCone || config.hasPeripheralVision) && 
                          distance <= config.visionRange
        
        // Hearing check
        val canHearPlayer = checkHearing(playerPos.x, playerPos.y, playerStateManager)
        
        detectionData.hasLineOfSight = hasLOS
        detectionData.canHearPlayer = canHearPlayer
        
        // Update alert level
        when (detectionData.alertLevel) {
            AlertLevel.UNAWARE -> {
                if (canSeePlayer || canHearPlayer) {
                    detectionData.alertLevel = AlertLevel.SUSPICIOUS
                    detectionData.alertProgress = 0f
                }
            }
            
            AlertLevel.SUSPICIOUS -> {
                if (canSeePlayer) {
                    detectionData.alertProgress += deltaTime * config.detectionSpeed
                    if (detectionData.alertProgress >= 1f) {
                        detectionData.alertLevel = AlertLevel.ALERTED
                        detectionData.lastKnownPlayerX = playerPos.x
                        detectionData.lastKnownPlayerY = playerPos.y
                        
                        EventBus.emit(GameEvent.Enemy.PlayerDetected(
                            enemyId = enemy.id,
                            enemyType = enemy.definition.type,
                            playerX = playerPos.x,
                            playerY = playerPos.y
                        ))
                    }
                } else if (canHearPlayer) {
                    detectionData.investigationPoint = Pair(playerPos.x, playerPos.y)
                    detectionData.alertLevel = AlertLevel.INVESTIGATING
                } else {
                    // Lose suspicion
                    detectionData.alertProgress -= deltaTime * 0.5f
                    if (detectionData.alertProgress <= 0f) {
                        detectionData.alertLevel = AlertLevel.UNAWARE
                    }
                }
            }
            
            AlertLevel.INVESTIGATING -> {
                if (canSeePlayer) {
                    detectionData.alertLevel = AlertLevel.ALERTED
                    detectionData.lastKnownPlayerX = playerPos.x
                    detectionData.lastKnownPlayerY = playerPos.y
                } else {
                    // Return to unaware after investigation
                    val investigationPoint = detectionData.investigationPoint
                    if (investigationPoint != null) {
                        val distToPoint = sqrt(
                            (enemyPos.x - investigationPoint.first).pow(2) +
                            (enemyPos.y - investigationPoint.second).pow(2)
                        )
                        if (distToPoint < 48f) {
                            detectionData.alertLevel = AlertLevel.UNAWARE
                            detectionData.investigationPoint = null
                        }
                    }
                }
            }
            
            AlertLevel.ALERTED -> {
                if (canSeePlayer) {
                    detectionData.alertLevel = AlertLevel.COMBAT
                    detectionData.lastKnownPlayerX = playerPos.x
                    detectionData.lastKnownPlayerY = playerPos.y
                    detectionData.timeSinceLastSeen = 0L
                } else {
                    detectionData.timeSinceLastSeen += (deltaTime * 1000).toLong()
                    if (detectionData.timeSinceLastSeen > 5000L) {
                        detectionData.alertLevel = AlertLevel.INVESTIGATING
                        detectionData.investigationPoint = Pair(
                            detectionData.lastKnownPlayerX,
                            detectionData.lastKnownPlayerY
                        )
                    }
                }
            }
            
            AlertLevel.COMBAT -> {
                if (canSeePlayer) {
                    detectionData.lastKnownPlayerX = playerPos.x
                    detectionData.lastKnownPlayerY = playerPos.y
                    detectionData.timeSinceLastSeen = 0L
                } else {
                    detectionData.timeSinceLastSeen += (deltaTime * 1000).toLong()
                    if (detectionData.timeSinceLastSeen > 3000L) {
                        detectionData.alertLevel = AlertLevel.ALERTED
                    }
                }
            }
        }
        
        return detectionData
    }
    
    /**
     * التحقق من خط البصر
     * Check line of sight to player
     */
    private fun checkLineOfSight(playerX: Float, playerY: Float, physicsEngine: PhysicsEngine): Boolean {
        // Simplified raycast check
        // In real implementation, would use physics engine raycast
        val enemyPos = enemy.position
        
        // For now, assume LOS if no major obstacles
        // This would be replaced with proper raycast
        return true // Placeholder
    }
    
    /**
     * التحقق من مخروط الرؤية
     * Check if player is in vision cone
     */
    private fun checkVisionCone(playerX: Float, playerY: Float): Boolean {
        val enemyPos = enemy.position
        
        val dx = playerX - enemyPos.x
        val dy = playerY - enemyPos.y
        
        // Calculate angle to player
        val angleToPlayer = atan2(dy, dx) * 180f / PI.toFloat()
        
        // Enemy facing direction
        val facingAngle = if (enemyPos.isFacingRight) 0f else 180f
        
        // Calculate angle difference
        var angleDiff = abs(angleToPlayer - facingAngle)
        if (angleDiff > 180f) angleDiff = 360f - angleDiff
        
        return angleDiff <= config.visionAngle / 2f
    }
    
    /**
     * التحقق من السمع
     * Check hearing
     */
    private fun checkHearing(
        playerX: Float,
        playerY: Float,
        playerStateManager: PlayerStateManager
    ): Boolean {
        val distance = enemy.distanceTo(playerX, playerY)
        if (distance > config.hearingRange) return false
        
        // Check if player is making noise (airborne or moving fast)
        val isNoisy = playerStateManager.currentState.position.isAirborne || 
                kotlin.math.abs(playerStateManager.currentState.position.velocityX) > 200f
        
        return isNoisy
    }
    
    /**
     * إعادة تعيين الكشف
     * Reset detection
     */
    fun reset() {
        detectionData.alertLevel = AlertLevel.UNAWARE
        detectionData.alertProgress = 0f
        detectionData.timeSinceLastSeen = 0L
        detectionData.hasLineOfSight = false
        detectionData.canHearPlayer = false
        detectionData.investigationPoint = null
    }
    
    fun getDetectionData(): DetectionData = detectionData
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Group Behavior System
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نظام السلوك الجماعي
 * Group behavior coordinator
 */
class GroupBehaviorSystem {
    val groups = mutableMapOf<String, GroupData>()
    
    /**
     * إنشاء أو الانضمام لمجموعة
     * Create or join a group
     */
    fun formGroup(enemies: List<Enemy>, formationType: FormationType): GroupData {
        val groupId = "group_${System.currentTimeMillis()}_${Random.nextInt(1000)}"
        
        // Select leader (highest level or elite)
        val leader = enemies.maxByOrNull { 
            it.definition.rank.ordinal * 100 + it.level 
        }
        
        val group = GroupData(
            groupId = groupId,
            leaderId = leader?.id,
            memberIds = enemies.map { it.id }.toMutableList(),
            formationType = formationType
        )
        
        groups[groupId] = group
        
        EventBus.emit(GameEvent.Enemy.GroupFormed(
            groupId = groupId,
            leaderType = leader?.definition?.type,
            memberCount = enemies.size,
            formationType = formationType
        ))
        
        return group
    }
    
    /**
     * تحديث السلوك الجماعي
     * Update group behavior
     */
    fun updateGroup(
        groupId: String,
        enemies: List<Enemy>,
        playerStateManager: PlayerStateManager
    ): GroupData? {
        val group = groups[groupId] ?: return null
        
        // Remove dead or far enemies
        group.memberIds.removeAll { memberId ->
            val member = enemies.find { it.id == memberId }
            member == null || member.currentState == EnemyState.DEAD ||
            member.distanceTo(
                enemies.first { it.id == group.leaderId }.position.x,
                enemies.first { it.id == group.leaderId }.position.y
            ) > 500f
        }
        
        // Disband if too few members
        if (group.memberIds.size < 2) {
            groups.remove(groupId)
            return null
        }
        
        // Update formation
        when (group.formationType) {
            FormationType.SURROUND -> updateSurroundFormation(group, enemies, playerStateManager)
            FormationType.VANGUARD -> updateVanguardFormation(group, enemies, playerStateManager)
            FormationType.PINCER -> updatePincerFormation(group, enemies, playerStateManager)
            else -> {}
        }
        
        return group
    }
    
    /**
     * تحديث تشكيل الإحاطة
     * Update surround formation
     */
    private fun updateSurroundFormation(
        group: GroupData,
        enemies: List<Enemy>,
        playerStateManager: PlayerStateManager
    ) {
        val playerPos = playerStateManager.getPosition()
        val memberCount = group.memberIds.size
        val radius = 150f
        
        group.memberIds.forEachIndexed { index, memberId ->
            val angle = (index.toFloat() / memberCount) * 2 * PI.toFloat()
            val targetX = playerPos.x + cos(angle) * radius
            val targetY = playerPos.y + sin(angle) * radius
            
            // Set target position for this member
            // Would be used by their AI to move to position
        }
    }
    
    /**
     * تحديث تشكيل الطليعة
     * Update vanguard formation
     */
    private fun updateVanguardFormation(
        group: GroupData,
        enemies: List<Enemy>,
        playerStateManager: PlayerStateManager
    ) {
        val playerPos = playerStateManager.getPosition()
        
        // Separate melee and ranged
        val meleeEnemies = enemies.filter { enemy ->
            group.memberIds.contains(enemy.id) &&
            enemy.definition.category in listOf(EnemyCategory.MELEE, EnemyCategory.HEAVY)
        }
        
        val rangedEnemies = enemies.filter { enemy ->
            group.memberIds.contains(enemy.id) &&
            enemy.definition.category == EnemyCategory.RANGED
        }
        
        // Position melee in front, ranged in back
        meleeEnemies.forEachIndexed { index, enemy ->
            // Move to front line
        }
        
        rangedEnemies.forEachIndexed { index, enemy ->
            // Stay at distance
        }
    }
    
    /**
     * تحديث تشكيل الكماشة
     * Update pincer formation
     */
    private fun updatePincerFormation(
        group: GroupData,
        enemies: List<Enemy>,
        playerStateManager: PlayerStateManager
    ) {
        val playerPos = playerStateManager.getPosition()
        val halfCount = group.memberIds.size / 2
        
        // Split into two groups attacking from sides
        group.memberIds.take(halfCount).forEach { memberId ->
            // Attack from left
        }
        
        group.memberIds.drop(halfCount).forEach { memberId ->
            // Attack from right
        }
    }
    
    /**
     * الحصول على معلومات المجموعة
     * Get group info for enemy
     */
    fun getGroupForEnemy(enemyId: String): GroupData? {
        return groups.values.find { it.memberIds.contains(enemyId) }
    }
    
    /**
     * التحقق من كون العدو قائداً
     * Check if enemy is group leader
     */
    fun isLeader(enemyId: String): Boolean {
        return groups.values.any { it.leaderId == enemyId }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Boss AI System
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نظام ذكاء الـ Boss
 * Boss AI controller
 */
class BossAIController(
    private val enemy: Enemy,
    private val phases: List<BossPhase>
) {
    private var currentPhaseIndex = 0
    private var isTransitioning = false
    private var transitionStartTime = 0L
    
    private val attackPattern = mutableListOf<EnemyAttack>()
    private var patternIndex = 0
    
    private val summonedEnemies = mutableListOf<String>()
    
    /**
     * تحديث Boss AI
     * Update boss AI
     */
    fun update(
        deltaTime: Float,
        playerStateManager: PlayerStateManager,
        combatEngine: CombatEngine
    ) {
        val currentPhase = phases[currentPhaseIndex]
        val hpPercent = enemy.stats.currentHp / enemy.stats.maxHp
        
        // Check for phase transition
        if (currentPhaseIndex < phases.size - 1) {
            val nextPhase = phases[currentPhaseIndex + 1]
            if (hpPercent <= nextPhase.hpThreshold && !isTransitioning) {
                startPhaseTransition(nextPhase)
            }
        }
        
        // Handle transition
        if (isTransitioning) {
            updatePhaseTransition(deltaTime)
            return
        }
        
        // Execute phase behavior
        executePhaseLogic(currentPhase, playerStateManager, combatEngine, deltaTime)
    }
    
    /**
     * بدء انتقال المرحلة
     * Start phase transition
     */
    private fun startPhaseTransition(newPhase: BossPhase) {
        isTransitioning = true
        transitionStartTime = System.currentTimeMillis()
        
        if (newPhase.invulnerableDuringTransition) {
            enemy.activeEffects.add(
                EnemyEffect(
                    type = EnemyEffectType.INVULNERABLE,
                    duration = newPhase.transitionDurationMs,
                    startTime = System.currentTimeMillis(),
                    value = 0f
                )
            )
        }
        
        EventBus.emit(GameEvent.Boss.PhaseTransitionStarted(
            bossId = enemy.id,
            bossType = enemy.definition.type,
            fromPhase = currentPhaseIndex,
            toPhase = currentPhaseIndex + 1,
            phaseName = newPhase.name
        ))
    }
    
    /**
     * تحديث انتقال المرحلة
     * Update phase transition
     */
    private fun updatePhaseTransition(deltaTime: Float) {
        val elapsed = System.currentTimeMillis() - transitionStartTime
        val nextPhase = phases[currentPhaseIndex + 1]
        
        if (elapsed >= nextPhase.transitionDurationMs) {
            // Transition complete
            isTransitioning = false
            currentPhaseIndex++
            
            // Apply phase modifiers
            applyPhaseModifiers(nextPhase)
            
            // Generate new attack pattern
            generateAttackPattern(nextPhase)
            
            EventBus.emit(GameEvent.Boss.PhaseChanged(
                bossId = enemy.id,
                bossType = enemy.definition.type,
                phaseNumber = currentPhaseIndex,
                phaseName = nextPhase.name,
                remainingHpPercent = enemy.stats.currentHp.toFloat() / enemy.stats.maxHp.toFloat()
            ))
        }
    }
    
    /**
     * تطبيق تعديلات المرحلة
     * Apply phase modifiers to stats
     */
    private fun applyPhaseModifiers(phase: BossPhase) {
        // Speed (handled by states and movement speed modifiers)
        // Damage (would need to modify base stats or attack values)
        // Defense
        // etc.
    }
    
    /**
     * توليد نمط الهجوم
     * Generate attack pattern for phase
     */
    private fun generateAttackPattern(phase: BossPhase) {
        attackPattern.clear()
        patternIndex = 0
        
        // Create a sequence of attacks
        // Example: Basic -> Basic -> Special -> Heavy -> AOE
        val basicAttacks = phase.attacks.filter { it.type == AttackType.MELEE_BASIC }
        val specialAttacks = phase.attacks.filter { it.type != AttackType.MELEE_BASIC }
        
        repeat(2) {
            basicAttacks.randomOrNull()?.let { attackPattern.add(it) }
        }
        specialAttacks.randomOrNull()?.let { attackPattern.add(it) }
        
        // Shuffle for variety
        attackPattern.shuffle()
    }
    
    /**
     * تنفيذ منطق المرحلة
     * Execute phase logic
     */
    private fun executePhaseLogic(
        phase: BossPhase,
        playerStateManager: PlayerStateManager,
        combatEngine: CombatEngine,
        deltaTime: Float
    ) {
        // Summon minions if applicable
        if (phase.canSummon && summonedEnemies.size < phase.summonCount) {
            if (Random.nextFloat() < 0.01f) { // 1% chance per frame
                summonMinion(phase)
            }
        }
        
        // Environmental attacks
        if (phase.environmentalAttacks.isNotEmpty() && Random.nextFloat() < 0.005f) {
            triggerEnvironmentalAttack(phase.environmentalAttacks.random())
        }
        
        // Follow attack pattern
        if (attackPattern.isNotEmpty()) {
            // Pattern-based attacking handled by main state machine
        }
    }
    
    /**
     * استدعاء عدو صغير
     * Summon a minion
     */
    private fun summonMinion(phase: BossPhase) {
        val summonType = phase.summonType ?: return
        val enemyPos = enemy.position
        
        // Random position near boss
        val offsetX = Random.nextFloat() * 200f - 100f
        val offsetY = -50f
        
        EventBus.emit(GameEvent.Enemy.Summoned(
            summonerId = enemy.id,
            summonType = summonType,
            x = enemyPos.x + offsetX,
            y = enemyPos.y + offsetY
        ))
    }
    
    /**
     * تفعيل هجوم بيئي
     * Trigger environmental attack
     */
    private fun triggerEnvironmentalAttack(attackName: String) {
        // Examples:
        // - "FALLING_ROCKS": Spawn falling hazards
        // - "FIRE_PILLARS": Create fire columns
        // - "POISON_CLOUDS": Spawn poison AoE
        
        EventBus.emit(GameEvent.Boss.EnvironmentalAttack(
            bossId = enemy.id,
            bossType = enemy.definition.type,
            attackName = attackName,
            durationMs = 3000L
        ))
    }
    
    /**
     * الحصول على الهجوم التالي في النمط
     * Get next attack in pattern
     */
    fun getNextAttackInPattern(): EnemyAttack? {
        if (attackPattern.isEmpty()) return null
        
        val attack = attackPattern[patternIndex]
        patternIndex = (patternIndex + 1) % attackPattern.size
        return attack
    }
    
    fun getCurrentPhase(): BossPhase = phases[currentPhaseIndex]
    fun getPhaseIndex(): Int = currentPhaseIndex
    fun isInTransition(): Boolean = isTransitioning
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - AI Configuration Database
// ════════════════════════════════════════════════════════════════════════════════

/**
 * قاعدة بيانات إعدادات الذكاء الاصطناعي
 * AI configuration database
 */
object EnemyAIConfigDatabase {
    private val configs = mutableMapOf<EnemyType, EnemyAIConfig>()
    
    init {
        // Ashen Sprawl Enemies
        configs[EnemyType.SCRAB_SCAVENGER] = EnemyAIConfig(
            enemyType = EnemyType.SCRAB_SCAVENGER,
            visionRange = 250f,
            visionAngle = 90f,
            hearingRange = 150f,
            aggressionLevel = 0.3f,
            cowardiceThreshold = 0.4f,
            preferredDistance = 80f,
            patrolRandomness = 0.5f,
            canCoordinate = true,
            formationPreference = FormationType.SCATTER
        )
        
        configs[EnemyType.ASHWARDEN] = EnemyAIConfig(
            enemyType = EnemyType.ASHWARDEN,
            visionRange = 300f,
            visionAngle = 120f,
            hearingRange = 200f,
            aggressionLevel = 0.7f,
            cowardiceThreshold = 0.15f,
            preferredDistance = 120f,
            minAttackDistance = 80f,
            canJumpWhileChasing = true,
            canCoordinate = true,
            formationPreference = FormationType.LINE
        )
        
        configs[EnemyType.RAG_CHILDREN] = EnemyAIConfig(
            enemyType = EnemyType.RAG_CHILDREN,
            visionRange = 200f,
            visionAngle = 110f,
            hearingRange = 250f,
            aggressionLevel = 0.4f,
            cowardiceThreshold = 0.5f,
            preferredDistance = 150f,
            useSpecialAttackChance = 0.4f, // Trap laying
            canCoordinate = false,
            formationPreference = FormationType.NONE
        )
        
        configs[EnemyType.COUNCIL_ENFORCER] = EnemyAIConfig(
            enemyType = EnemyType.COUNCIL_ENFORCER,
            visionRange = 350f,
            visionAngle = 140f,
            hearingRange = 220f,
            hasPeripheralVision = true,
            aggressionLevel = 0.8f,
            cowardiceThreshold = 0.2f,
            callForHelpThreshold = 0.4f,
            preferredDistance = 200f,
            minAttackDistance = 100f,
            maxAttackDistance = 350f,
            comboChance = 0.4f,
            canCoordinate = true,
            canUseTactics = true,
            formationPreference = FormationType.VANGUARD
        )
        
        configs[EnemyType.PYRE_HARROW] = EnemyAIConfig(
            enemyType = EnemyType.PYRE_HARROW,
            visionRange = 400f,
            visionAngle = 160f,
            hearingRange = 300f,
            hasPeripheralVision = true,
            canSeeInDark = true,
            aggressionLevel = 0.9f,
            cowardiceThreshold = 0.1f,
            preferredDistance = 180f,
            useSpecialAttackChance = 0.5f,
            comboChance = 0.6f,
            hasPhases = true,
            phaseCount = 2,
            isAdaptive = true,
            canUseTactics = true
        )
        
        // Veiled Archives Enemies
        configs[EnemyType.PAGE_SCRAPER] = EnemyAIConfig(
            enemyType = EnemyType.PAGE_SCRAPER,
            visionRange = 280f,
            visionAngle = 100f,
            hearingRange = 180f,
            aggressionLevel = 0.5f,
            cowardiceThreshold = 0.35f,
            preferredDistance = 200f,
            minAttackDistance = 150f,
            maxAttackDistance = 300f,
            canCoordinate = true,
            formationPreference = FormationType.SCATTER
        )
        
        configs[EnemyType.VAULT_SENTINEL] = EnemyAIConfig(
            enemyType = EnemyType.VAULT_SENTINEL,
            visionRange = 320f,
            visionAngle = 130f,
            hearingRange = 200f,
            aggressionLevel = 0.6f,
            cowardiceThreshold = 0.2f,
            preferredDistance = 100f,
            canJumpWhileChasing = false,
            canUseTactics = true
        )
        
        configs[EnemyType.ECHO_SHADE] = EnemyAIConfig(
            enemyType = EnemyType.ECHO_SHADE,
            visionRange = 300f,
            visionAngle = 360f, // Can sense all around
            hearingRange = 250f,
            hasPeripheralVision = true,
            canSeeInDark = true,
            aggressionLevel = 0.5f,
            cowardiceThreshold = 0.3f,
            preferredDistance = 120f,
            useSpecialAttackChance = 0.6f, // MF drain
            canCoordinate = false
        )
        
        configs[EnemyType.LEDGER_WARDEN] = EnemyAIConfig(
            enemyType = EnemyType.LEDGER_WARDEN,
            visionRange = 350f,
            visionAngle = 150f,
            hearingRange = 220f,
            hasPeripheralVision = true,
            aggressionLevel = 0.75f,
            cowardiceThreshold = 0.25f,
            preferredDistance = 150f,
            comboChance = 0.5f,
            useSpecialAttackChance = 0.4f,
            canUseTactics = true,
            isAdaptive = true
        )
        
        configs[EnemyType.THE_INDEXER] = EnemyAIConfig(
            enemyType = EnemyType.THE_INDEXER,
            visionRange = 450f,
            visionAngle = 180f,
            hearingRange = 350f,
            hasPeripheralVision = true,
            canSeeInDark = true,
            aggressionLevel = 0.85f,
            cowardiceThreshold = 0.15f,
            preferredDistance = 200f,
            comboChance = 0.7f,
            useSpecialAttackChance = 0.6f,
            hasPhases = true,
            phaseCount = 2,
            isAdaptive = true,
            canUseTactics = true
        )
        
        // Hollowed Archipelago Enemies
        configs[EnemyType.ROPE_CROAKER] = EnemyAIConfig(
            enemyType = EnemyType.ROPE_CROAKER,
            visionRange = 260f,
            visionAngle = 110f,
            hearingRange = 200f,
            aggressionLevel = 0.4f,
            cowardiceThreshold = 0.4f,
            preferredDistance = 180f,
            minAttackDistance = 120f,
            maxAttackDistance = 250f,
            useSpecialAttackChance = 0.5f,
            canCoordinate = true
        )
        
        configs[EnemyType.DRIFT_KNIGHT] = EnemyAIConfig(
            enemyType = EnemyType.DRIFT_KNIGHT,
            visionRange = 350f,
            visionAngle = 140f,
            hearingRange = 220f,
            aggressionLevel = 0.7f,
            cowardiceThreshold = 0.2f,
            preferredDistance = 100f,
            canJumpWhileChasing = true,
            comboChance = 0.4f,
            canCoordinate = true,
            formationPreference = FormationType.PINCER
        )
        
        configs[EnemyType.BARGAIN_PIRATE] = EnemyAIConfig(
            enemyType = EnemyType.BARGAIN_PIRATE,
            visionRange = 290f,
            visionAngle = 120f,
            hearingRange = 190f,
            aggressionLevel = 0.6f,
            cowardiceThreshold = 0.35f,
            callForHelpThreshold = 0.5f,
            preferredDistance = 90f,
            useSpecialAttackChance = 0.3f,
            canCoordinate = true,
            canUseTactics = true
        )
        
        configs[EnemyType.SKY_SCAVENGER] = EnemyAIConfig(
            enemyType = EnemyType.SKY_SCAVENGER,
            visionRange = 400f,
            visionAngle = 160f,
            hearingRange = 250f,
            hasPeripheralVision = true,
            aggressionLevel = 0.65f,
            cowardiceThreshold = 0.3f,
            preferredDistance = 220f,
            minAttackDistance = 150f,
            maxAttackDistance = 350f,
            useSpecialAttackChance = 0.5f,
            canCoordinate = true,
            formationPreference = FormationType.SURROUND
        )
        
        configs[EnemyType.BRIDGEMASTER] = EnemyAIConfig(
            enemyType = EnemyType.BRIDGEMASTER,
            visionRange = 420f,
            visionAngle = 170f,
            hearingRange = 300f,
            hasPeripheralVision = true,
            aggressionLevel = 0.8f,
            cowardiceThreshold = 0.15f,
            preferredDistance = 160f,
            comboChance = 0.6f,
            useSpecialAttackChance = 0.7f,
            hasPhases = true,
            phaseCount = 2,
            canUseTactics = true,
            isAdaptive = true
        )
    }
    
    /**
     * الحصول على إعدادات الذكاء الاصطناعي
     * Get AI config for enemy type
     */
    fun getConfig(type: EnemyType): EnemyAIConfig {
        return configs[type] ?: EnemyAIConfig(enemyType = type) // Default config
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Enemy AI Manager
// ════════════════════════════════════════════════════════════════════════════════

/**
 * مدير الذكاء الاصطناعي للأعداء
 * Main AI manager for all enemies
 */
class EnemyAIManager(
    private val playerStateManager: PlayerStateManager,
    private val physicsEngine: PhysicsEngine,
    private val combatEngine: CombatEngine
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // Enemy systems
    private val stateMachines = mutableMapOf<String, EnemyStateMachine>()
    private val detectionSystems = mutableMapOf<String, EnemyDetectionSystem>()
    private val bossControllers = mutableMapOf<String, BossAIController>()
    
    // Group behavior
    private val groupBehavior = GroupBehaviorSystem()
    
    // Active enemies (for optimization)
    private val _activeEnemies = MutableStateFlow<List<Enemy>>(emptyList())
    val activeEnemies: StateFlow<List<Enemy>> = _activeEnemies.asStateFlow()
    
    // Performance settings
    private var updateRadius = 800f // Only update enemies within this radius
    private var updateInterval = 16L // ~60 FPS
    private var lastUpdateTime = 0L
    
    // Debug
    private var debugMode = false
    
    /**
     * تسجيل عدو
     * Register an enemy for AI processing
     */
    fun registerEnemy(enemy: Enemy) {
        val config = EnemyAIConfigDatabase.getConfig(enemy.definition.type)
        
        stateMachines[enemy.id] = EnemyStateMachine(enemy, config)
        detectionSystems[enemy.id] = EnemyDetectionSystem(enemy, config)
        
        // Register boss if applicable
        if (enemy.definition.rank == EnemyRank.BOSS || enemy.definition.rank == EnemyRank.MINIBOSS) {
            val phases = createBossPhases(enemy)
            if (phases.isNotEmpty()) {
                bossControllers[enemy.id] = BossAIController(enemy, phases)
            }
        }
        
        // Update active list
        val current = _activeEnemies.value.toMutableList()
        current.add(enemy)
        _activeEnemies.value = current
        
        EventBus.emit(GameEvent.Enemy.Spawned(
            enemyId = enemy.id,
            enemyType = enemy.definition.type,
            x = enemy.position.x,
            y = enemy.position.y
        ))
    }
    
    /**
     * إلغاء تسجيل عدو
     * Unregister an enemy
     */
    fun unregisterEnemy(enemyId: String) {
        stateMachines.remove(enemyId)
        detectionSystems.remove(enemyId)
        bossControllers.remove(enemyId)
        
        val current = _activeEnemies.value.toMutableList()
        current.removeAll { it.id == enemyId }
        _activeEnemies.value = current
    }
    
    /**
     * تحديث جميع الأعداء
     * Update all enemy AI
     */
    fun update(deltaTime: Float) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < updateInterval) return
        
        val playerPos = playerStateManager.getPosition()
        val enemies = _activeEnemies.value
        
        // Update each enemy
        enemies.forEach { enemy ->
            // Distance culling
            val distance = enemy.distanceTo(playerPos.x, playerPos.y)
            if (distance > updateRadius && enemy.currentState != EnemyState.DEAD) {
                // Skip update for far enemies (except bosses)
                if (enemy.definition.rank != EnemyRank.BOSS) {
                    return@forEach
                }
            }
            
            // Get systems
            val stateMachine = stateMachines[enemy.id] ?: return@forEach
            val detectionSystem = detectionSystems[enemy.id] ?: return@forEach
            val bossController = bossControllers[enemy.id]
            
            // Update detection
            val detectionData = detectionSystem.update(deltaTime, playerStateManager, physicsEngine)
            
            // Get group data
            val groupData = groupBehavior.getGroupForEnemy(enemy.id)
            
            // Update state machine
            stateMachine.update(
                deltaTime,
                playerStateManager,
                detectionData,
                groupData,
                physicsEngine,
                combatEngine
            )
            
            // Update boss AI if applicable
            bossController?.update(deltaTime, playerStateManager, combatEngine)
        }
        
        // Update groups
        groupBehavior.groups.keys.toList().forEach { groupId ->
            groupBehavior.updateGroup(groupId, enemies, playerStateManager)
        }
        
        lastUpdateTime = now
    }
    
    /**
     * إنشاء مجموعة
     * Create enemy group
     */
    fun createGroup(enemyIds: List<String>, formationType: FormationType): GroupData? {
        val enemies = _activeEnemies.value.filter { it.id in enemyIds }
        if (enemies.size < 2) return null
        
        return groupBehavior.formGroup(enemies, formationType)
    }
    
    /**
     * إنشاء مراحل الـ Boss
     * Create boss phases
     */
    private fun createBossPhases(enemy: Enemy): List<BossPhase> {
        return when (enemy.definition.type) {
            EnemyType.PYRE_HARROW -> listOf(
                BossPhase(
                    phaseNumber = 1,
                    hpThreshold = 1.0f,
                    name = "Ignition",
                    attacks = enemy.definition.attacks.take(3),
                    speedMultiplier = 1.0f,
                    damageMultiplier = 1.0f,
                    defenseMultiplier = 1.0f
                ),
                BossPhase(
                    phaseNumber = 2,
                    hpThreshold = 0.5f,
                    name = "Inferno",
                    attacks = enemy.definition.attacks,
                    speedMultiplier = 1.3f,
                    damageMultiplier = 1.5f,
                    defenseMultiplier = 0.8f,
                    canSummon = true,
                    summonType = EnemyType.SCRAB_SCAVENGER,
                    summonCount = 3,
                    environmentalAttacks = listOf("FIRE_PILLARS", "LAVA_POOLS")
                )
            )
            
            EnemyType.THE_INDEXER -> listOf(
                BossPhase(
                    phaseNumber = 1,
                    hpThreshold = 1.0f,
                    name = "Cataloging",
                    attacks = enemy.definition.attacks.take(3),
                    speedMultiplier = 1.0f,
                    damageMultiplier = 1.0f
                ),
                BossPhase(
                    phaseNumber = 2,
                    hpThreshold = 0.5f,
                    name = "Purging",
                    attacks = enemy.definition.attacks,
                    speedMultiplier = 1.2f,
                    damageMultiplier = 1.4f,
                    canSummon = true,
                    summonType = EnemyType.PAGE_SCRAPER,
                    summonCount = 4,
                    environmentalAttacks = listOf("FALLING_BOOKS", "INK_BLAST")
                )
            )
            
            EnemyType.BRIDGEMASTER -> listOf(
                BossPhase(
                    phaseNumber = 1,
                    hpThreshold = 1.0f,
                    name = "Construction",
                    attacks = enemy.definition.attacks.take(3),
                    speedMultiplier = 1.0f,
                    damageMultiplier = 1.0f
                ),
                BossPhase(
                    phaseNumber = 2,
                    hpThreshold = 0.5f,
                    name = "Demolition",
                    attacks = enemy.definition.attacks,
                    speedMultiplier = 1.25f,
                    damageMultiplier = 1.3f,
                    environmentalAttacks = listOf("BRIDGE_COLLAPSE", "FALLING_DEBRIS")
                )
            )
            
            else -> emptyList()
        }
    }
    
    /**
     * الحصول على آلة حالة العدو
     * Get enemy state machine
     */
    fun getStateMachine(enemyId: String): EnemyStateMachine? = stateMachines[enemyId]
    
    /**
     * الحصول على نظام الكشف
     * Get detection system
     */
    fun getDetectionSystem(enemyId: String): EnemyDetectionSystem? = detectionSystems[enemyId]
    
    /**
     * الحصول على متحكم الـ Boss
     * Get boss controller
     */
    fun getBossController(enemyId: String): BossAIController? = bossControllers[enemyId]
    
    /**
     * تعيين نصف قطر التحديث
     * Set update radius for optimization
     */
    fun setUpdateRadius(radius: Float) {
        updateRadius = radius
    }
    
    /**
     * تعيين فاصل التحديث
     * Set update interval
     */
    fun setUpdateInterval(intervalMs: Long) {
        updateInterval = intervalMs
    }
    
    /**
     * تفعيل/تعطيل وضع التصحيح
     * Toggle debug mode
     */
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
    }
    
    /**
     * تنظيف الموارد
     * Cleanup resources
     */
    fun cleanup() {
        stateMachines.clear()
        detectionSystems.clear()
        bossControllers.clear()
        _activeEnemies.value = emptyList()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// تم الانتهاء من EnemyAI.kt
// EnemyAI.kt Complete
// ════════════════════════════════════════════════════════════════════════════════