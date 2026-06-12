package com.erygra.maskoflight.core

import com.erygra.maskoflight.world.HazardType
import com.erygra.maskoflight.world.RegionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  EventBus.kt — نظام الأحداث المركزي (Observer Pattern)
 *  Erygra Universe 2.0 | Mask of Light
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  يتيح التواصل بين الأنظمة المختلفة (Physics, Combat, AI, UI...)
 *  دون اقتران مباشر (Tight Coupling).
 *
 *  الاستخدام:
 *  // إرسال حدث:
 *  EventBus.emit(GameEvent.PlayerDamaged(damage = 15, source = "Enemy"))
 *
 *  // الاستماع لحدث:
 *  EventBus.events.filterIsInstance<GameEvent.PlayerDamaged>().collect { event ->
 *      // handle event
 *  }
 * ══════════════════════════════════════════════════════════════════════════
 */

// ─────────────────────────────────────────────────────────────────────────────
// تعريف الأحداث
// ─────────────────────────────────────────────────────────────────────────────

/** GameEvent — الأحداث المركزية في اللعبة */
open class GameEvent {

    // ─── أحداث اللاعب ────────────────────────────────────────────────────

    data class PlayerDamaged(
        val damage: Int,
        val source: String,
        val currentHp: Int = 0,
        val maxHp: Int = 0,
        val isCritical: Boolean = false
    ) : GameEvent()

    data class PlayerHealed(val amount: Int) : GameEvent()

    data class PlayerDied(
        val cause: String = "Unknown",
        val x: Float = 0f,
        val y: Float = 0f,
        val deaths: Int = 0
    ) : GameEvent()

    data class PlayerRespawned(
        val spawnX: Float,
        val spawnY: Float
    ) : GameEvent()

    data class PlayerLevelUp(val newLevel: Int, val xpGained: Int) : GameEvent()

    data class PlayerMemoryFragmentGained(
        val amount: Int,
        val source: String
    ) : GameEvent()

    data class PlayerForgetfulnessChanged(
        val newValue: Int,
        val change: Int,
        val reason: String
    ) : GameEvent()

    data class PlayerAbilityUsed(
        val abilityId: String,
        val mfCost: Int,
        val fmGain: Int
    ) : GameEvent()

    data class PlayerReachedSanctuary(
        val sanctuaryId: String,
        val isNew: Boolean
    ) : GameEvent()

    data class PlayerEnteredRegion(
        val regionId: String,
        val fromRegionId: String?
    ) : GameEvent()

    object UI {
        object PauseRequested : GameEvent()
        object InventoryOpened : GameEvent()
        object MapOpened : GameEvent()
    }
    
    object PlayerActions {
        data class Jumped(val x: Float, val y: Float) : GameEvent()
        data class WallJumped(val x: Float, val y: Float) : GameEvent()
        data class Dashed(val x: Float, val y: Float, val direction: String) : GameEvent()
        data class Dodged(val x: Float, val y: Float) : GameEvent()
        data class Climbing(val x: Float, val y: Float) : GameEvent()
        data class GroundSlammed(val x: Float, val y: Float) : GameEvent()
        data class MemoryPulseUsed(val x: Float, val y: Float, val radius: Float = 0f) : GameEvent()
        data class EchoRecallUsed(val x: Float, val y: Float) : GameEvent()
        data class MaskShardBlasted(val x: Float, val y: Float) : GameEvent()
        data class InteractionAttempted(val x: Float, val y: Float) : GameEvent()
    }
    
    object CombatActions {
        data class LightAttackStarted(val x: Float, val y: Float, val facingRight: Boolean = true, val comboCount: Int = 0) : GameEvent()
        data class HeavyAttackStarted(val x: Float, val y: Float, val facingRight: Boolean = true) : GameEvent()
        data class ParryStarted(val x: Float, val y: Float) : GameEvent()
    }

    object Inventory {
        data class SatchelUpgraded(val newLevel: Int) : GameEvent()
    }

    object World {
        data class FastTravel(val pointId: String) : GameEvent()
        
        data class HazardRegistered(val hazardId: String, val type: HazardType, val region: RegionType) : GameEvent()
        data class HazardUnregistered(val hazardId: String, val type: HazardType, val region: RegionType) : GameEvent()
        data class HazardActivated(val hazardId: String, val type: HazardType? = null) : GameEvent()
        data class HazardDeactivated(val hazardId: String, val type: HazardType? = null) : GameEvent()
        data class TriggerActivated(val triggerId: String) : GameEvent()
        data class TriggerDeactivated(val triggerId: String) : GameEvent()
    }

    object Shop {
        data class ItemPurchased(val itemId: String, val quantity: Int) : GameEvent()
        data class ItemSold(val itemId: String, val quantity: Int) : GameEvent()
        data class ItemBoughtBack(val itemId: String, val quantity: Int) : GameEvent()
    }

    object Skill {
        data class SkillUpgraded(val skillId: String, val newLevel: Int) : GameEvent()
        data class SkillUnlocked(
            val skillId: String, 
            val skillName: String = "", 
            val rank: Int = 1
        ) : GameEvent()
        data class SkillsRespecced(val reason: String = "Respec") : GameEvent()
        data class SkillPointsEarned(val amount: Int) : GameEvent()
    }

    data class MechanicUnlocked(val mechanicId: String) : GameEvent()

    // ─── أحداث القتال ────────────────────────────────────────────────────

    data class EnemyDamaged(
        val enemyId: String,
        val damage: Int,
        val isCritical: Boolean = false
    ) : GameEvent()

    data class EnemyDied(
        val enemyId: String,
        val enemyType: String,
        val xpReward: Int,
        val coinsReward: Int
    ) : GameEvent()

    data class BossPhaseChanged(
        val bossId: String,
        val newPhase: Int
    ) : GameEvent()

    data class BossDefeated(
        val bossId: String,
        val regionId: String
    ) : GameEvent()

    data class ParrySuccess(
        val enemyId: String,
        val counterDamage: Int
    ) : GameEvent()

    data class ComboCountChanged(val count: Int) : GameEvent()

    // ─── أحداث العالم ────────────────────────────────────────────────────

    data class WorldEventStarted(
        val eventType: WorldEventType,
        val durationMinutes: Int
    ) : GameEvent()

    data class WorldEventEnded(val eventType: WorldEventType) : GameEvent()

    data class MapTileDiscovered(
        val regionId: String,
        val tileX: Int,
        val tileY: Int
    ) : GameEvent()

    data class MapShardCollected(
        val regionId: String,
        val shardIndex: Int
    ) : GameEvent()

    data class FastTravelUsed(
        val fromSanctuaryId: String,
        val toSanctuaryId: String,
        val cost: Int
    ) : GameEvent()

    // ─── أحداث المهام ────────────────────────────────────────────────────

    data class QuestStarted(val questId: String) : GameEvent()
    data class QuestAccepted(val questId: String) : GameEvent()
    data class QuestRejected(val questId: String) : GameEvent()
    data class QuestAbandoned(val questId: String) : GameEvent()
    data class QuestReady(val questId: String) : GameEvent()

    data class QuestStepCompleted(
        val questId: String,
        val stepIndex: Int
    ) : GameEvent()

    data class ObjectiveCompleted(
        val questId: String,
        val objectiveId: String
    ) : GameEvent()

    data class ObjectiveProgress(
        val questId: String,
        val objectiveId: String,
        val progress: Int
    ) : GameEvent()

    data class QuestCompleted(
        val questId: String,
        val xpReward: Int,
        val coinsReward: Int
    ) : GameEvent()

    data class QuestFailed(val questId: String, val reason: String) : GameEvent()

    // ─── أحداث NPC والحوار ───────────────────────────────────────────────

    data class DialogueStarted(val npcId: String) : GameEvent()

    data class DialogueChoiceMade(
        val npcId: String,
        val choiceIndex: Int,
        val choiceText: String
    ) : GameEvent()

    data class DialogueEnded(val npcId: String) : GameEvent()

    data class NPCRelationshipChanged(
        val npcId: String,
        val change: Int,
        val newValue: Int
    ) : GameEvent()

    // ─── أحداث المخزون ───────────────────────────────────────────────────

    data class ItemPickedUp(val itemId: String, val quantity: Int) : GameEvent()

    data class ItemUsed(val itemId: String) : GameEvent()

    data class ItemPurchased(
        val itemId: String,
        val cost: Int,
        val currency: String
    ) : GameEvent()

    data class ItemSold(val itemId: String, val coins: Int) : GameEvent()

    // ─── أحداث الألغاز ───────────────────────────────────────────────────

    data class PuzzleStarted(val puzzleId: String) : GameEvent()

    data class PuzzleSolved(
        val puzzleId: String,
        val timeSeconds: Int
    ) : GameEvent()

    data class PuzzleFailed(val puzzleId: String) : GameEvent()

    // ─── أحداث الحفظ ─────────────────────────────────────────────────────

    data class GameSaved(val slotId: Int, val isAutoSave: Boolean) : GameEvent()

    data class GameLoaded(val slotId: Int) : GameEvent()

    object CloudSyncStarted : GameEvent()
    data class CloudSyncCompleted(val success: Boolean) : GameEvent()

    // ─── أحداث UI ────────────────────────────────────────────────────────

    data class GameStateChanged(
        val from: GameState,
        val to: GameState
    ) : GameEvent()

    data class NotificationShown(
        val message: String,
        val type: NotificationType
    ) : GameEvent()

    data class AchievementUnlocked(val achievementId: String) : GameEvent()

    data class ScreenShakeRequested(
        val intensityX: Float,
        val intensityY: Float,
        val durationFrames: Int
    ) : GameEvent()

    // ─── أحداث الصوت ─────────────────────────────────────────────────────

    data class PlaySFX(val sfxId: String, val volume: Float = 1f) : GameEvent()

    data class RegionMusicChanged(val regionId: String) : GameEvent()

    object BossMusicStarted : GameEvent()
    object BossMusicEnded : GameEvent()

    sealed class Enemy : GameEvent() {
        data class Spawned(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val x: Float,
            val y: Float,
            val level: Int = 1,
            val rank: com.erygra.maskoflight.enemy.EnemyRank = com.erygra.maskoflight.enemy.EnemyRank.NORMAL,
            val spawnSource: com.erygra.maskoflight.enemy.SpawnSource = com.erygra.maskoflight.enemy.SpawnSource.SPAWN_POINT
        ) : Enemy()

        data class Despawned(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val reason: com.erygra.maskoflight.enemy.DespawnReason
        ) : Enemy()

        data class StateChanged(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val oldState: com.erygra.maskoflight.enemy.EnemyState,
            val newState: com.erygra.maskoflight.enemy.EnemyState,
            val timestamp: Long = System.currentTimeMillis()
        ) : Enemy()

        data class PlayerDetected(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val playerX: Float,
            val playerY: Float,
            val detectionMethod: com.erygra.maskoflight.enemy.DetectionMethod = com.erygra.maskoflight.enemy.DetectionMethod.VISION,
            val alertLevel: com.erygra.maskoflight.enemy.AlertLevel = com.erygra.maskoflight.enemy.AlertLevel.COMBAT
        ) : Enemy()

        data class PlayerLost(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val timeSinceLastSeen: Long
        ) : Enemy()

        data class AttackStarted(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val attackName: String,
            val targetX: Float = 0f,
            val targetY: Float = 0f
        ) : Enemy()

        data class AttackExecuted(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val attackName: String,
            val damage: Float,
            val hitPlayer: Boolean = false
        ) : Enemy()

        data class DamageDealt(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val damage: Float,
            val attackType: com.erygra.maskoflight.enemy.AttackType,
            val isCritical: Boolean = false
        ) : Enemy()

        data class DamageTaken(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val damage: Float,
            val source: String,
            val remainingHp: Float,
            val wasParried: Boolean = false,
            val wasBackstab: Boolean = false
        ) : Enemy()

        data class Died(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val killedBy: String,
            val level: Int,
            val rank: com.erygra.maskoflight.enemy.EnemyRank,
            val x: Float,
            val y: Float,
            val survivalTime: Long,
            val damageDealt: Float,
            val damageTaken: Float
        ) : Enemy()

        data class ProjectileSpawned(
            val enemyId: String,
            val projectileType: String,
            val x: Float,
            val y: Float,
            val velocityX: Float,
            val velocityY: Float,
            val damage: Float
        ) : Enemy()

        data class Summoned(
            val summonerId: String,
            val summonType: com.erygra.maskoflight.enemy.EnemyType,
            val x: Float,
            val y: Float,
            val count: Int = 1
        ) : Enemy()

        data class GroupFormed(
            val groupId: String,
            val leaderType: com.erygra.maskoflight.enemy.EnemyType?,
            val memberCount: Int,
            val formationType: com.erygra.maskoflight.enemy.FormationType
        ) : Enemy()

        data class GroupDisbanded(
            val groupId: String,
            val reason: String
        ) : Enemy()

        data class WaveStarted(
            val waveNumber: Int,
            val enemyCount: Int,
            val isBossWave: Boolean
        ) : Enemy()

        data class WaveCompleted(
            val waveNumber: Int,
            val isBossWave: Boolean,
            val completionTime: Long = System.currentTimeMillis(),
            val enemiesKilled: Int = 0
        ) : Enemy()

        data class StatusEffectApplied(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val effectType: com.erygra.maskoflight.enemy.EnemyEffectType,
            val duration: Long,
            val source: String
        ) : Enemy()

        data class StatusEffectRemoved(
            val enemyId: String,
            val enemyType: com.erygra.maskoflight.enemy.EnemyType,
            val effectType: com.erygra.maskoflight.enemy.EnemyEffectType,
            val wasExpired: Boolean
        ) : Enemy()
    }

    sealed class Boss : GameEvent() {
        data class PhaseTransitionStarted(
            val bossId: String,
            val bossType: com.erygra.maskoflight.enemy.EnemyType,
            val fromPhase: Int,
            val toPhase: Int,
            val phaseName: String
        ) : Boss()

        data class PhaseChanged(
            val bossId: String,
            val bossType: com.erygra.maskoflight.enemy.EnemyType,
            val phaseNumber: Int,
            val phaseName: String,
            val remainingHpPercent: Float
        ) : Boss()

        data class EnvironmentalAttack(
            val bossId: String,
            val bossType: com.erygra.maskoflight.enemy.EnemyType,
            val attackName: String,
            val durationMs: Long
        ) : Boss()
        
        data class BossEnvironmentalAttack(
            val bossId: String,
            val bossType: com.erygra.maskoflight.enemy.EnemyType,
            val attackName: String
        ) : Boss()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// أنواع مساعدة
// ─────────────────────────────────────────────────────────────────────────────

enum class WorldEventType {
    MEMORY_STORM,
    REMNANT_UPRISING,
    NAME_AUCTION,
    CARAVAN_SPAWN,
    WANDERING_GEARWRIGHT,
    LOST_CHILD_ECHO
}

enum class NotificationType {
    INFO,
    WARNING,
    DANGER,
    SUCCESS,
    ACHIEVEMENT,
    QUEST,
    FM_WARNING
}

// ─────────────────────────────────────────────────────────────────────────────
// الـ EventBus نفسه
// ─────────────────────────────────────────────────────────────────────────────

/**
 * EventBus — Singleton يدير إرسال واستقبال الأحداث.
 *
 * يستخدم SharedFlow مع replay=0 (لا يحتفظ بالأحداث السابقة).
 * الـ extraBufferCapacity=64 لتجنب حجب الـ emitter.
 */
object EventBus {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _events = MutableSharedFlow<GameEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /** Flow يُستمع إليه لاستقبال الأحداث */
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    /**
     * إرسال حدث — آمن من أي خيط.
     * يُفضَّل استخدامه من Main thread.
     */
    fun emit(event: GameEvent) {
        scope.launch {
            _events.emit(event)
        }
    }

    /**
     * إرسال حدث مع انتظار الاستلام (Suspend).
     * للاستخدام من coroutines.
     */
    suspend fun emitSuspend(event: GameEvent) {
        _events.emit(event)
    }

    /**
     * إرسال SFX بسهولة
     */
    fun playSFX(sfxId: String, volume: Float = 1f) {
        emit(GameEvent.PlaySFX(sfxId, volume))
    }

    /**
     * طلب اهتزاز الشاشة
     */
    fun screenShake(
        intensityX: Float = 5f,
        intensityY: Float = 5f,
        durationFrames: Int = 12
    ) {
        emit(GameEvent.ScreenShakeRequested(intensityX, intensityY, durationFrames))
    }

    /**
     * عرض إشعار
     */
    fun notify(
        message: String,
        type: NotificationType = NotificationType.INFO
    ) {
        emit(GameEvent.NotificationShown(message, type))
    }
}