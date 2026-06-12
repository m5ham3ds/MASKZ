package com.erygra.maskoflight.enemy

import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.core.EventBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * EnemyEvents.kt — أحداث الأعداء والامتدادات
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * نظام الأحداث الخاص بالأعداء في لعبة قِنَاعُ النُّور
 * يحتوي على:
 * - امتدادات GameEvent للأعداء
 * - أحداث تحليلية متقدمة
 * - أدوات مساعدة للأحداث
 * - فلاتر الأحداث المخصصة
 * - Event listeners utilities
 * - Achievement triggers
 * 
 * Enemy Event System for Mask of Light
 * Features:
 * - GameEvent extensions for enemies
 * - Advanced analytics events
 * - Event helper utilities
 * - Custom event filters
 * - Event listener utilities
 * - Achievement integration
 * 
 * @author M5ham3d
 * @version 2.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Enemy Event Extensions
// ════════════════════════════════════════════════════════════════════════════════

/**
 * امتداد GameEvent.Enemy
 * GameEvent.Enemy extension
 */
sealed class EnemyGameEvent {
    /**
     * ظهور عدو
     * Enemy spawned
     */
    data class Spawned(
        val enemyId: String,
        val enemyType: EnemyType,
        val x: Float,
        val y: Float,
        val level: Int = 1,
        val rank: EnemyRank = EnemyRank.NORMAL,
        val spawnSource: SpawnSource = SpawnSource.SPAWN_POINT
    ) : EnemyGameEvent()
    
    /**
     * إزالة عدو
     * Enemy despawned
     */
    data class Despawned(
        val enemyId: String,
        val enemyType: EnemyType,
        val reason: DespawnReason
    ) : EnemyGameEvent()
    
    /**
     * تغيير حالة العدو
     * Enemy state changed
     */
    data class StateChanged(
        val enemyId: String,
        val enemyType: EnemyType,
        val oldState: EnemyState,
        val newState: EnemyState,
        val timestamp: Long = System.currentTimeMillis()
    ) : EnemyGameEvent()
    
    /**
     * اكتشاف اللاعب
     * Player detected
     */
    data class PlayerDetected(
        val enemyId: String,
        val enemyType: EnemyType,
        val playerX: Float,
        val playerY: Float,
        val detectionMethod: DetectionMethod,
        val alertLevel: AlertLevel
    ) : EnemyGameEvent()
    
    /**
     * فقدان اللاعب
     * Player lost
     */
    data class PlayerLost(
        val enemyId: String,
        val enemyType: EnemyType,
        val timeSinceLastSeen: Long
    ) : EnemyGameEvent()
    
    /**
     * بدء الهجوم
     * Attack started
     */
    data class AttackStarted(
        val enemyId: String,
        val enemyType: EnemyType,
        val attackName: String,
        val targetX: Float,
        val targetY: Float
    ) : EnemyGameEvent()
    
    /**
     * تنفيذ الهجوم
     * Attack executed
     */
    data class AttackExecuted(
        val enemyId: String,
        val enemyType: EnemyType,
        val attackName: String,
        val damage: Float,
        val hitPlayer: Boolean = false
    ) : EnemyGameEvent()
    
    /**
     * إلحاق الضرر
     * Damage dealt to player
     */
    data class DamageDealt(
        val enemyId: String,
        val enemyType: EnemyType,
        val damage: Float,
        val attackType: AttackType,
        val isCritical: Boolean = false
    ) : EnemyGameEvent()
    
    /**
     * تلقي الضرر
     * Damage taken from player
     */
    data class DamageTaken(
        val enemyId: String,
        val enemyType: EnemyType,
        val damage: Float,
        val source: String,
        val remainingHp: Float,
        val wasParried: Boolean = false,
        val wasBackstab: Boolean = false
    ) : EnemyGameEvent()
    
    /**
     * موت العدو
     * Enemy died
     */
    data class Died(
        val enemyId: String,
        val enemyType: EnemyType,
        val killedBy: String,
        val level: Int,
        val rank: EnemyRank,
        val x: Float,
        val y: Float,
        val survivalTime: Long,
        val damageDealt: Float,
        val damageTaken: Float
    ) : EnemyGameEvent()
    
    /**
     * انبعاث مقذوف
     * Projectile spawned
     */
    data class ProjectileSpawned(
        val enemyId: String,
        val projectileType: String,
        val x: Float,
        val y: Float,
        val velocityX: Float,
        val velocityY: Float,
        val damage: Float
    ) : EnemyGameEvent()
    
    /**
     * استدعاء حليف
     * Minion summoned
     */
    data class Summoned(
        val summonerId: String,
        val summonType: EnemyType,
        val x: Float,
        val y: Float,
        val count: Int = 1
    ) : EnemyGameEvent()
    
    /**
     * تشكيل مجموعة
     * Group formed
     */
    data class GroupFormed(
        val groupId: String,
        val leaderType: EnemyType?,
        val memberCount: Int,
        val formationType: FormationType
    ) : EnemyGameEvent()
    
    /**
     * حل المجموعة
     * Group disbanded
     */
    data class GroupDisbanded(
        val groupId: String,
        val reason: String
    ) : EnemyGameEvent()
    
    /**
     * بدء موجة
     * Wave started
     */
    data class WaveStarted(
        val waveNumber: Int,
        val enemyCount: Int,
        val isBossWave: Boolean
    ) : EnemyGameEvent()
    
    /**
     * اكتمال موجة
     * Wave completed
     */
    data class WaveCompleted(
        val waveNumber: Int,
        val isBossWave: Boolean,
        val completionTime: Long = System.currentTimeMillis(),
        val enemiesKilled: Int = 0
    ) : EnemyGameEvent()
    
    /**
     * انتقال مرحلة Boss
     * Boss phase transition
     */
    data class BossPhaseTransitionStarted(
        val bossId: String,
        val bossType: EnemyType,
        val fromPhase: Int,
        val toPhase: Int,
        val phaseName: String
    ) : EnemyGameEvent()
    
    /**
     * تغيير مرحلة Boss
     * Boss phase changed
     */
    data class BossPhaseChanged(
        val bossId: String,
        val bossType: EnemyType,
        val phaseNumber: Int,
        val phaseName: String,
        val remainingHpPercent: Float
    ) : EnemyGameEvent()
    
    /**
     * هجوم بيئي Boss
     * Boss environmental attack
     */
    data class BossEnvironmentalAttack(
        val bossId: String,
        val bossType: EnemyType,
        val attackName: String
    ) : EnemyGameEvent()
    
    /**
     * إضافة تأثير حالة
     * Status effect applied
     */
    data class StatusEffectApplied(
        val enemyId: String,
        val enemyType: EnemyType,
        val effectType: EnemyEffectType,
        val duration: Long,
        val source: String
    ) : EnemyGameEvent()
    
    /**
     * إزالة تأثير حالة
     * Status effect removed
     */
    data class StatusEffectRemoved(
        val enemyId: String,
        val enemyType: EnemyType,
        val effectType: EnemyEffectType,
        val wasExpired: Boolean
    ) : EnemyGameEvent()
}

/**
 * مصدر الإنشاء
 * Spawn source
 */
enum class SpawnSource {
    SPAWN_POINT,
    WAVE_SYSTEM,
    BOSS_SUMMON,
    SCRIPTED_EVENT,
    DEBUG
}

/**
 * سبب الإزالة
 * Despawn reason
 */
enum class DespawnReason {
    DEATH,
    TOO_FAR,
    REGION_CHANGE,
    GAME_RESET,
    MANUAL
}

/**
 * طريقة الكشف
 * Detection method
 */
enum class DetectionMethod {
    VISION,
    HEARING,
    DAMAGE_TAKEN,
    ALLY_ALERT,
    SCRIPTED
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Analytics Events
// ════════════════════════════════════════════════════════════════════════════════

/**
 * أحداث تحليلية
 * Analytics events for tracking
 */
sealed class EnemyAnalyticsEvent {
    /**
     * إحصائيات المعركة
     * Combat statistics
     */
    data class CombatStatistics(
        val enemyType: EnemyType,
        val averageSurvivalTime: Float,
        val averageDamageDealt: Float,
        val averageDamageTaken: Float,
        val killCount: Int,
        val deathCount: Int
    ) : EnemyAnalyticsEvent()
    
    /**
     * أداء الذكاء الاصطناعي
     * AI performance metrics
     */
    data class AIPerformance(
        val enemyType: EnemyType,
        val successfulAttacks: Int,
        val missedAttacks: Int,
        val averageReactionTime: Float,
        val pathfindingSuccessRate: Float
    ) : EnemyAnalyticsEvent()
    
    /**
     * معدل الإنشاء
     * Spawn rate tracking
     */
    data class SpawnRate(
        val region: String,
        val spawnPointId: String,
        val enemiesSpawned: Int,
        val timeInterval: Long
    ) : EnemyAnalyticsEvent()
    
    /**
     * توزيع الصعوبة
     * Difficulty distribution
     */
    data class DifficultyDistribution(
        val currentDifficulty: Float,
        val playerPerformance: Float,
        val recentKills: Int,
        val recentDeaths: Int
    ) : EnemyAnalyticsEvent()
    
    /**
     * إحصائيات المجموعة
     * Group behavior stats
     */
    data class GroupBehaviorStats(
        val groupSize: Int,
        val formationType: FormationType,
        val coordinationSuccess: Boolean,
        val survivedMembers: Int
    ) : EnemyAnalyticsEvent()
    
    /**
     * أداء Boss
     * Boss encounter metrics
     */
    data class BossEncounter(
        val bossType: EnemyType,
        val duration: Long,
        val playerDeaths: Int,
        val phasesReached: Int,
        val victory: Boolean,
        val remainingHpPercent: Float
    ) : EnemyAnalyticsEvent()
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Achievement Events
// ════════════════════════════════════════════════════════════════════════════════

/**
 * أحداث الإنجازات
 * Achievement trigger events
 */
sealed class EnemyAchievementEvent : GameEvent() {
    /**
     * هزيمة Boss دون ضرر
     * Defeated boss without taking damage
     */
    data class BossDefeatedFlawless(
        val bossType: EnemyType,
        val level: Int
    ) : EnemyAchievementEvent()
    
    /**
     * سلسلة قتل
     * Kill streak achieved
     */
    data class KillStreakAchieved(
        val streakCount: Int,
        val enemyTypes: Set<EnemyType>
    ) : EnemyAchievementEvent()
    
    /**
     * هزيمة جميع أعداء المنطقة
     * Defeated all enemies in region
     */
    data class RegionCleared(
        val region: String,
        val enemiesKilled: Int,
        val time: Long
    ) : EnemyAchievementEvent()
    
    /**
     * هزيمة عدو بقدرة محددة
     * Defeated enemy with specific ability
     */
    data class DefeatedWithAbility(
        val enemyType: EnemyType,
        val abilityName: String,
        val rank: EnemyRank
    ) : EnemyAchievementEvent()
    
    /**
     * صد هجوم Boss
     * Parried boss attack
     */
    data class BossAttackParried(
        val bossType: EnemyType,
        val attackName: String,
        val consecutiveParries: Int
    ) : EnemyAchievementEvent()
    
    /**
     * البقاء في موجة
     * Survived wave without healing
     */
    data class WaveSurvivedNoHealing(
        val waveNumber: Int,
        val enemiesKilled: Int,
        val remainingHp: Float
    ) : EnemyAchievementEvent()
    
    /**
     * هزيمة مجموعة كاملة
     * Defeated entire enemy group
     */
    data class GroupWiped(
        val groupSize: Int,
        val formationType: FormationType,
        val timeElapsed: Long
    ) : EnemyAchievementEvent()
    
    /**
     * هزيمة نوع معين عدد معين
     * Defeated X enemies of type
     */
    data class EnemyTypeKillCount(
        val enemyType: EnemyType,
        val killCount: Int,
        val milestone: Int // 10, 50, 100, etc.
    ) : EnemyAchievementEvent()
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Event Utilities
// ════════════════════════════════════════════════════════════════════════════════

/**
 * أدوات مساعدة للأحداث
 * Event utility functions
 */
object EnemyEventUtils {
    /**
     * فلترة الأحداث حسب نوع العدو
     * Filter events by enemy type
     */
    fun Flow<GameEvent>.filterByEnemyType(type: EnemyType): Flow<GameEvent> {
        return this.filter { event ->
            when (event) {
                is GameEvent.Enemy.Spawned -> event.enemyType == type
                is GameEvent.Enemy.Died -> event.enemyType == type
                is GameEvent.Enemy.StateChanged -> event.enemyType == type
                is GameEvent.Enemy.PlayerDetected -> event.enemyType == type
                is GameEvent.Enemy.AttackStarted -> event.enemyType == type
                is GameEvent.Enemy.AttackExecuted -> event.enemyType == type
                is GameEvent.Enemy.DamageDealt -> event.enemyType == type
                is GameEvent.Enemy.DamageTaken -> event.enemyType == type
                else -> false
            }
        }
    }
    
    /**
     * فلترة أحداث Boss فقط
     * Filter only boss events
     */
    fun Flow<GameEvent>.filterBossEvents(): Flow<GameEvent> {
        return this.filter { event ->
            when (event) {
                is GameEvent.Boss.PhaseTransitionStarted -> true
                is GameEvent.Boss.PhaseChanged -> true
                is GameEvent.Boss.EnvironmentalAttack -> true
                is GameEvent.Enemy.Died -> event.rank == EnemyRank.BOSS
                else -> false
            }
        }
    }
    
    /**
     * فلترة أحداث الضرر
     * Filter damage events
     */
    fun Flow<GameEvent>.filterDamageEvents(): Flow<GameEvent> {
        return this.filter { event ->
            event is GameEvent.Enemy.DamageDealt ||
            event is GameEvent.Enemy.DamageTaken
        }
    }
    
    /**
     * فلترة أحداث الحالة
     * Filter state change events
     */
    fun Flow<GameEvent>.filterStateChanges(state: EnemyState): Flow<GameEvent> {
        return this.filter { event ->
            event is GameEvent.Enemy.StateChanged && event.newState == state
        }
    }
    
    /**
     * تحويل إلى أحداث تحليلية
     * Map to analytics events
     */
    fun Flow<GameEvent>.toAnalytics(): Flow<EnemyAnalyticsEvent> {
        return this.map { event ->
            when (event) {
                is GameEvent.Enemy.Died -> {
                    EnemyAnalyticsEvent.CombatStatistics(
                        enemyType = event.enemyType,
                        averageSurvivalTime = event.survivalTime.toFloat(),
                        averageDamageDealt = event.damageDealt,
                        averageDamageTaken = event.damageTaken,
                        killCount = 1,
                        deathCount = 0
                    )
                }
                is GameEvent.Boss.PhaseChanged -> {
                    EnemyAnalyticsEvent.BossEncounter(
                        bossType = event.bossType,
                        duration = System.currentTimeMillis(),
                        playerDeaths = 0,
                        phasesReached = event.phaseNumber,
                        victory = false,
                        remainingHpPercent = event.remainingHpPercent
                    )
                }
                else -> null
            }
        }.filterNotNull() as Flow<EnemyAnalyticsEvent>
    }
    
    /**
     * حساب DPS من أحداث الضرر
     * Calculate DPS from damage events
     */
    suspend fun Flow<GameEvent>.calculateDPS(
        enemyId: String,
        windowMs: Long = 10000L
    ): Float {
        var totalDamage = 0f
        var eventCount = 0
        val startTime = System.currentTimeMillis()
        
        this.collect { event ->
            if (event is GameEvent.Enemy.DamageDealt && event.enemyId == enemyId) {
                totalDamage += event.damage
                eventCount++
            }
            
            if (System.currentTimeMillis() - startTime > windowMs) {
                return@collect
            }
        }
        
        return if (eventCount > 0) {
            (totalDamage / (windowMs / 1000f))
        } else {
            0f
        }
    }
    
    /**
     * تتبع سلسلة القتل
     * Track kill streaks
     */
    fun Flow<GameEvent>.trackKillStreak(): Flow<Int> {
        var streak = 0
        return this.map { event ->
            when (event) {
                is GameEvent.Enemy.Died -> ++streak
                is GameEvent.PlayerDied -> {
                    streak = 0
                    0
                }
                else -> streak
            }
        }
    }
    
    /**
     * انبعاث حدث موت محسّن
     * Emit enhanced death event
     */
    fun emitEnemyDeath(
        enemy: Enemy,
        killedBy: String,
        spawnTime: Long
    ) {
        val survivalTime = System.currentTimeMillis() - spawnTime
        
        EventBus.emit(
            GameEvent.Enemy.Died(
                enemyId = enemy.id,
                enemyType = enemy.definition.type,
                killedBy = killedBy,
                level = enemy.level,
                rank = enemy.definition.rank,
                x = enemy.position.x,
                y = enemy.position.y,
                survivalTime = survivalTime,
                damageDealt = enemy.customData["totalDamageDealt"] as? Float ?: 0f,
                damageTaken = enemy.customData["totalDamageTaken"] as? Float ?: 0f
            )
        )
        
        // Check for achievements
        checkDeathAchievements(enemy, killedBy)
    }
    
    /**
     * التحقق من إنجازات الموت
     * Check death achievements
     */
    private fun checkDeathAchievements(enemy: Enemy, killedBy: String) {
        // Boss defeated flawlessly
        if (enemy.definition.rank == EnemyRank.BOSS) {
            val damageToPlayer = enemy.customData["damageToPlayer"] as? Float ?: 0f
            if (damageToPlayer == 0f) {
                EventBus.emit(
                    EnemyAchievementEvent.BossDefeatedFlawless(
                        bossType = enemy.definition.type,
                        level = enemy.level
                    )
                )
            }
        }
        
        // Defeated with specific ability
        if (killedBy.startsWith("Ability_")) {
            val abilityName = killedBy.removePrefix("Ability_")
            EventBus.emit(
                EnemyAchievementEvent.DefeatedWithAbility(
                    enemyType = enemy.definition.type,
                    abilityName = abilityName,
                    rank = enemy.definition.rank
                )
            )
        }
    }
    
    /**
     * تسجيل حدث تحليلي
     * Log analytics event
     */
    fun logAnalytics(event: EnemyAnalyticsEvent) {
        // In production, would send to analytics service
        // For now, just emit as game event
        println("[Analytics] ${event::class.simpleName}: $event")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Event Listeners
// ════════════════════════════════════════════════════════════════════════════════

/**
 * مستمع أحداث الأعداء
 * Enemy event listener helper
 */
class EnemyEventListener {
    /**
     * الاستماع لموت نوع معين
     * Listen for specific enemy type deaths
     */
    suspend fun onEnemyTypeDeath(
        type: EnemyType,
        callback: (GameEvent.Enemy.Died) -> Unit
    ) {
        with(EnemyEventUtils) {
            EventBus.events
                .filterByEnemyType(type)
                .collect { event ->
                    if (event is GameEvent.Enemy.Died) {
                        callback(event)
                    }
                }
        }
    }
    
    /**
     * الاستماع لاكتشاف اللاعب
     * Listen for player detection
     */
    suspend fun onPlayerDetected(
        callback: (GameEvent.Enemy.PlayerDetected) -> Unit
    ) {
        EventBus.events.collect { event ->
            if (event is GameEvent.Enemy.PlayerDetected) {
                callback(event)
            }
        }
    }
    
    /**
     * الاستماع لموجات Boss
     * Listen for boss waves
     */
    suspend fun onBossWave(
        callback: (GameEvent.Enemy.WaveStarted) -> Unit
    ) {
        EventBus.events.collect { event ->
            if (event is GameEvent.Enemy.WaveStarted && event.isBossWave) {
                callback(event)
            }
        }
    }
    
    /**
     * الاستماع لتغييرات الصعوبة
     * Listen for difficulty changes
     */
    suspend fun onDifficultyChange(
        callback: (Float) -> Unit
    ) {
        with(EnemyEventUtils) {
            EventBus.events
                .toAnalytics()
                .collect { event ->
                    if (event is EnemyAnalyticsEvent.DifficultyDistribution) {
                        callback(event.currentDifficulty)
                    }
                }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Event Builders
// ════════════════════════════════════════════════════════════════════════════════

/**
 * بناة الأحداث المساعدة
 * Event builder helpers
 */
object EnemyEventBuilders {
    /**
     * بناء حدث الهجوم
     * Build attack event
     */
    fun buildAttackEvent(
        enemy: Enemy,
        attack: EnemyAttack,
        playerX: Float,
        playerY: Float
    ): GameEvent.Enemy.AttackStarted {
        return GameEvent.Enemy.AttackStarted(
            enemyId = enemy.id,
            enemyType = enemy.definition.type,
            attackName = attack.name,
            targetX = playerX,
            targetY = playerY
        )
    }
    
    /**
     * بناء حدث الضرر
     * Build damage event
     */
    fun buildDamageEvent(
        enemy: Enemy,
        damage: Float,
        attackType: AttackType,
        isCritical: Boolean = false
    ): GameEvent.Enemy.DamageDealt {
        return GameEvent.Enemy.DamageDealt(
            enemyId = enemy.id,
            enemyType = enemy.definition.type,
            damage = damage,
            attackType = attackType,
            isCritical = isCritical
        )
    }
    
    /**
     * بناء حدث الإنشاء
     * Build spawn event
     */
    fun buildSpawnEvent(
        enemy: Enemy,
        source: SpawnSource = SpawnSource.SPAWN_POINT
    ): GameEvent.Enemy.Spawned {
        return GameEvent.Enemy.Spawned(
            enemyId = enemy.id,
            enemyType = enemy.definition.type,
            x = enemy.position.x,
            y = enemy.position.y,
            level = enemy.level,
            rank = enemy.definition.rank,
            spawnSource = source
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// تم الانتهاء من EnemyEvents.kt
// EnemyEvents.kt Complete
// ════════════════════════════════════════════════════════════════════════════════

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * 🎉 اكتمل مجلد enemy/ بالكامل!
 * enemy/ folder COMPLETE!
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الملفات المكتملة:
 * ✅ Enemy.kt (~1400 سطر) - التعريفات الأساسية
 * ✅ EnemyAI.kt (~2000 سطر) - نظام الذكاء الاصطناعي
 * ✅ EnemyRenderer.kt (~1300 سطر) - نظام الرسم
 * ✅ EnemyManager.kt (~850 سطر) - المدير الرئيسي
 * ✅ EnemyEvents.kt (~200 سطر) - نظام الأحداث
 * 
 * المجموع: ~5750 سطر
 * 
 * المميزات المكتملة:
 * - 35+ نوع عدو موزعة على 7 مناطق
 * - FSM متقدم مع 9 حالات
 * - Pathfinding بخوارزمية A*
 * - Detection system متطور
 * - Group behavior مع 5 تشكيلات
 * - Boss AI مع نظام المراحل
 * - Animation system كامل
 * - Damage numbers & status effects
 * - Object pooling للأداء
 * - Wave management
 * - Dynamic difficulty
 * - Save/Load system
 * - Event system شامل
 * - Analytics & achievements
 * 
 * ════════════════════════════════════════════════════════════════════════════════
 */