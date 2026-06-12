package com.erygra.maskoflight.enemy

import com.erygra.maskoflight.core.EventBus
import com.erygra.maskoflight.core.GameConfig
import com.erygra.maskoflight.core.GameEvent
import com.erygra.maskoflight.engine.PhysicsEngine
import com.erygra.maskoflight.engine.CombatEngine
import com.erygra.maskoflight.engine.ParticleEngine
import com.erygra.maskoflight.engine.ParticleType
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
 * EnemyManager.kt — مدير الأعداء الرئيسي
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * المدير الرئيسي لنظام الأعداء في لعبة قِنَاعُ النُّور
 * يحتوي على:
 * - نظام إنشاء الأعداء (Spawning)
 * - نظام إعادة استخدام الكائنات (Object Pooling)
 * - إدارة الموجات (Wave Management)
 * - تدريج صعوبة الأعداء (Enemy Scaling)
 * - حفظ/تحميل الحالة (Save/Load)
 * - التكامل مع AI والرسم
 * - إحصائيات وتحليلات
 * - نظام المكافآت
 * 
 * Main Enemy Management System for Mask of Light
 * Features:
 * - Enemy spawning system
 * - Object pooling for performance
 * - Wave management
 * - Dynamic difficulty scaling
 * - Save/Load state persistence
 * - AI & Renderer integration
 * - Statistics & analytics
 * - Reward system
 * 
 * @author M5ham3d
 * @version 2.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نقطة الإنشاء
 * Spawn point
 */
data class SpawnPoint(
    val id: String,
    val x: Float,
    val y: Float,
    val region: String,
    val enemyTypes: List<EnemyType>,
    val minLevel: Int = 1,
    val maxLevel: Int = 10,
    val maxEnemies: Int = 5,
    val respawnTimeMs: Long = 30000L,
    val isActive: Boolean = true,
    val isBossSpawn: Boolean = false,
    val triggerRadius: Float = 400f, // Player must be within this range
    val deactivateRadius: Float = 800f, // Deactivate if player too far
    val conditions: List<SpawnCondition> = emptyList()
)

/**
 * شرط الإنشاء
 * Spawn condition
 */
sealed class SpawnCondition {
    data class PlayerLevelRange(val minLevel: Int, val maxLevel: Int) : SpawnCondition()
    data class TimeOfDay(val startHour: Int, val endHour: Int) : SpawnCondition()
    data class QuestActive(val questId: String) : SpawnCondition()
    data class BossDefeated(val bossType: EnemyType) : SpawnCondition()
    data class RegionCleared(val region: String) : SpawnCondition()
    object PlayerHasMask : SpawnCondition()
}

/**
 * موجة الأعداء
 * Enemy wave
 */
data class EnemyWave(
    val waveNumber: Int,
    val enemies: List<WaveEnemy>,
    val delayMs: Long = 0L,
    val onComplete: (() -> Unit)? = null,
    val isBossWave: Boolean = false
)

/**
 * عدو في الموجة
 * Enemy in wave
 */
data class WaveEnemy(
    val type: EnemyType,
    val count: Int,
    val level: Int,
    val spawnDelayMs: Long = 0L,
    val spawnPattern: SpawnPattern = SpawnPattern.RANDOM
)

/**
 * نمط الإنشاء
 * Spawn pattern
 */
enum class SpawnPattern {
    RANDOM, // Random positions
    CIRCLE, // Circle around player
    LINE, // Line formation
    BEHIND, // Behind player
    SIDES, // Left and right
    ABOVE, // From above
    PORTAL // From portal effect
}

/**
 * إحصائيات الأعداء
 * Enemy statistics
 */
data class EnemyStatistics(
    var totalSpawned: Int = 0,
    var totalKilled: Int = 0,
    var totalDamageDealt: Float = 0f,
    var totalDamageTaken: Float = 0f,
    var bossesKilled: Int = 0,
    var elitesKilled: Int = 0,
    var longestKillStreak: Int = 0,
    var currentKillStreak: Int = 0,
    var fastestKillTimeMs: Long = Long.MAX_VALUE,
    val killsByType: MutableMap<EnemyType, Int> = mutableMapOf(),
    val deathsByType: MutableMap<EnemyType, Int> = mutableMapOf()
)

/**
 * بيانات الصعوبة الديناميكية
 * Dynamic difficulty data
 */
data class DifficultyScaling(
    var difficultyLevel: Float = 1.0f, // 0.5 = Easy, 1.0 = Normal, 2.0 = Hard
    var playerPerformance: Float = 1.0f, // 0.0 = Poor, 1.0 = Good, 2.0 = Excellent
    var recentDeaths: Int = 0,
    var recentKills: Int = 0,
    var avgKillTime: Float = 10000f, // Milliseconds
    var lastAdjustmentTime: Long = 0L,
    val adjustmentIntervalMs: Long = 60000L // Adjust every minute
) {
    /**
     * حساب مضاعف الصعوبة
     * Calculate difficulty multiplier
     */
    fun getDifficultyMultiplier(): Float {
        return difficultyLevel * (0.5f + playerPerformance * 0.5f)
    }
    
    /**
     * تعديل الصعوبة بناءً على الأداء
     * Adjust difficulty based on performance
     */
    fun adjustDifficulty() {
        val now = System.currentTimeMillis()
        if (now - lastAdjustmentTime < adjustmentIntervalMs) return
        
        // Calculate performance
        val killDeathRatio = if (recentDeaths > 0) {
            recentKills.toFloat() / recentDeaths
        } else {
            recentKills.toFloat()
        }
        
        playerPerformance = when {
            killDeathRatio > 10f -> 2.0f // Excellent
            killDeathRatio > 5f -> 1.5f // Very good
            killDeathRatio > 2f -> 1.2f // Good
            killDeathRatio > 1f -> 1.0f // Normal
            killDeathRatio > 0.5f -> 0.8f // Below average
            else -> 0.5f // Poor
        }
        
        // Adjust difficulty level slightly
        difficultyLevel = when {
            playerPerformance > 1.5f -> (difficultyLevel + 0.1f).coerceAtMost(2.5f)
            playerPerformance < 0.7f -> (difficultyLevel - 0.1f).coerceAtLeast(0.5f)
            else -> difficultyLevel
        }
        
        // Reset counters
        recentDeaths = 0
        recentKills = 0
        lastAdjustmentTime = now
    }
}

/**
 * حالة حوض الكائنات
 * Object pool state
 */
data class EnemyPool(
    val type: EnemyType,
    val available: MutableList<Enemy> = mutableListOf(),
    val active: MutableList<Enemy> = mutableListOf(),
    var totalCreated: Int = 0,
    val maxPoolSize: Int = 20
)

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Enemy Manager
// ════════════════════════════════════════════════════════════════════════════════

/**
 * مدير الأعداء الرئيسي
 * Main enemy manager
 */
class EnemyManager(
    private val playerStateManager: PlayerStateManager,
    private val physicsEngine: PhysicsEngine,
    private val combatEngine: CombatEngine,
    private val particleEngine: ParticleEngine
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // Core systems
    private val aiManager = EnemyAIManager(playerStateManager, physicsEngine, combatEngine)
    private val batchRenderer = EnemyBatchRenderer(particleEngine)
    
    // Enemy storage
    private val _activeEnemies = MutableStateFlow<List<Enemy>>(emptyList())
    val activeEnemies: StateFlow<List<Enemy>> = _activeEnemies.asStateFlow()
    
    private val enemyPools = mutableMapOf<EnemyType, EnemyPool>()
    
    // Spawn points
    private val spawnPoints = mutableMapOf<String, SpawnPoint>()
    private val spawnTimers = mutableMapOf<String, Long>()
    
    // Wave system
    private var currentWave: EnemyWave? = null
    private var waveStartTime = 0L
    private val pendingWaves = mutableListOf<EnemyWave>()
    
    // Statistics
    private val _statistics = MutableStateFlow(EnemyStatistics())
    val statistics: StateFlow<EnemyStatistics> = _statistics.asStateFlow()
    
    // Difficulty scaling
    private val difficultyScaling = DifficultyScaling()
    
    // Settings
    private var maxActiveEnemies = 50
    private var enableDynamicDifficulty = true
    private var enableObjectPooling = true
    
    init {
        setupEventListeners()
        initializePools()
    }
    
    /**
     * إعداد مستمعي الأحداث
     * Setup event listeners
     */
    private fun setupEventListeners() {
        scope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is GameEvent.Enemy.Died -> handleEnemyDeath(event)
                    is GameEvent.Enemy.DamageDealt -> handleEnemyDamageDealt(event)
                    is GameEvent.Enemy.DamageTaken -> handleEnemyDamageTaken(event)
                    is GameEvent.PlayerDied -> handlePlayerDeath()
                    else -> {}
                }
            }
        }
    }
    
    /**
     * تهيئة أحواض الكائنات
     * Initialize object pools
     */
    private fun initializePools() {
        if (!enableObjectPooling) return
        
        EnemyType.values().forEach { type ->
            enemyPools[type] = EnemyPool(type = type)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Update Loop
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * تحديث جميع الأعداء
     * Update all enemies
     */
    fun update(deltaTime: Float) {
        // Update AI
        aiManager.update(deltaTime)
        
        // Update renderers
        batchRenderer.updateAll(deltaTime)
        
        // Update spawn points
        updateSpawnPoints(deltaTime)
        
        // Update waves
        updateWaves(deltaTime)
        
        // Update difficulty
        if (enableDynamicDifficulty) {
            difficultyScaling.adjustDifficulty()
        }
        
        // Remove dead enemies
        cleanupDeadEnemies()
    }
    
    /**
     * تحديث نقاط الإنشاء
     * Update spawn points
     */
    private fun updateSpawnPoints(deltaTime: Float) {
        val playerPos = playerStateManager.getPosition()
        val now = System.currentTimeMillis()
        
        spawnPoints.values.forEach { spawnPoint ->
            if (!spawnPoint.isActive) return@forEach
            
            // Check distance to player
            val distance = sqrt(
                (spawnPoint.x - playerPos.x).pow(2) +
                (spawnPoint.y - playerPos.y).pow(2)
            )
            
            // Too far, skip
            if (distance > spawnPoint.deactivateRadius) return@forEach
            
            // Not close enough to trigger
            if (distance > spawnPoint.triggerRadius) return@forEach
            
            // Check conditions
            if (!checkSpawnConditions(spawnPoint)) return@forEach
            
            // Check respawn timer
            val lastSpawnTime = spawnTimers[spawnPoint.id] ?: 0L
            if (now - lastSpawnTime < spawnPoint.respawnTimeMs) return@forEach
            
            // Count enemies from this spawn point
            val enemiesFromSpawn = _activeEnemies.value.count { enemy ->
                val spawnDistance = sqrt(
                    (enemy.spawnPoint.first - spawnPoint.x).pow(2) +
                    (enemy.spawnPoint.second - spawnPoint.y).pow(2)
                )
                spawnDistance < 50f
            }
            
            // Check max enemies
            if (enemiesFromSpawn >= spawnPoint.maxEnemies) return@forEach
            
            // Spawn enemy
            spawnFromPoint(spawnPoint)
            spawnTimers[spawnPoint.id] = now
        }
    }
    
    /**
     * تحديث الموجات
     * Update waves
     */
    private fun updateWaves(deltaTime: Float) {
        val currentWave = this.currentWave
        
        if (currentWave != null) {
            // Check if wave is complete
            val waveEnemiesAlive = _activeEnemies.value.any { enemy ->
                enemy.customData["waveNumber"] == currentWave.waveNumber
            }
            
            if (!waveEnemiesAlive) {
                // Wave complete
                currentWave.onComplete?.invoke()
                this.currentWave = null
                
                EventBus.emit(GameEvent.Enemy.WaveCompleted(
                    waveNumber = currentWave.waveNumber,
                    isBossWave = currentWave.isBossWave
                ))
                
                // Start next wave
                if (pendingWaves.isNotEmpty()) {
                    startWave(pendingWaves.removeAt(0))
                }
            }
        } else if (pendingWaves.isNotEmpty()) {
            // Start next pending wave
            startWave(pendingWaves.removeAt(0))
        }
    }
    
    /**
     * تنظيف الأعداء الميتين
     * Cleanup dead enemies
     */
    private fun cleanupDeadEnemies() {
        val deadEnemies = _activeEnemies.value.filter { enemy ->
            enemy.currentState == EnemyState.DEAD &&
            System.currentTimeMillis() - (enemy.customData["deathTime"] as? Long ?: 0L) > 3000L
        }
        
        deadEnemies.forEach { enemy ->
            despawnEnemy(enemy.id)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Spawning System
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * إنشاء عدو
     * Spawn an enemy
     */
    fun spawnEnemy(
        type: EnemyType,
        x: Float,
        y: Float,
        level: Int = playerStateManager.currentState.stats.level,
        rank: EnemyRank? = null
    ): Enemy? {
        // Check max active enemies
        if (_activeEnemies.value.size >= maxActiveEnemies) {
            return null
        }
        
        // Get or create enemy
        val rawEnemy = if (enableObjectPooling) {
            getFromPool(type, x, y, level, rank)
        } else {
            createEnemy(type, x, y, level, rank)
        }
        
        // Scale enemy based on difficulty
        val enemy = scaleEnemy(rawEnemy)
        
        // Register with systems
        aiManager.registerEnemy(enemy)
        batchRenderer.registerEnemy(enemy)
        
        // Add to active list
        val current = _activeEnemies.value.toMutableList()
        current.add(enemy)
        _activeEnemies.value = current
        
        // Update statistics
        val stats = _statistics.value
        stats.totalSpawned++
        _statistics.value = stats
        
        // Emit event
        EventBus.emit(GameEvent.Enemy.Spawned(
            enemyId = enemy.id,
            enemyType = type,
            x = x,
            y = y
        ))
        
        return enemy
    }
    
    /**
     * إنشاء عدو من نقطة إنشاء
     * Spawn enemy from spawn point
     */
    private fun spawnFromPoint(spawnPoint: SpawnPoint) {
        val enemyType = spawnPoint.enemyTypes.random()
        val level = Random.nextInt(spawnPoint.minLevel, spawnPoint.maxLevel + 1)
        
        spawnEnemy(
            type = enemyType,
            x = spawnPoint.x + Random.nextFloat() * 100f - 50f,
            y = spawnPoint.y,
            level = level
        )
    }
    
    /**
     * إزالة عدو
     * Despawn an enemy
     */
    fun despawnEnemy(enemyId: String) {
        val enemy = _activeEnemies.value.find { it.id == enemyId } ?: return
        
        // Unregister from systems
        aiManager.unregisterEnemy(enemyId)
        batchRenderer.unregisterEnemy(enemyId)
        
        // Return to pool
        if (enableObjectPooling) {
            returnToPool(enemy)
        }
        
        // Remove from active list
        val current = _activeEnemies.value.toMutableList()
        current.removeAll { it.id == enemyId }
        _activeEnemies.value = current
    }
    
    /**
     * إنشاء عدو جديد
     * Create new enemy instance
     */
    private fun createEnemy(
        type: EnemyType,
        x: Float,
        y: Float,
        level: Int,
        rank: EnemyRank?
    ): Enemy {
        val definition = EnemyDatabase.getEnemy(type) ?: throw IllegalArgumentException("Unknown enemy type: $type")
        val actualRank = rank ?: definition.rank
        
        val enemy = EnemyFactory.createEnemy(
            type = type,
            x = x,
            y = y,
            level = level
        ) ?: throw IllegalStateException("Failed to create enemy: $type")
        
        return if (actualRank != definition.rank) {
            // Adjust stats for different rank
            val rankMultiplier = when (actualRank) {
                EnemyRank.NORMAL -> 1.0f
                EnemyRank.ELITE -> 1.5f
                EnemyRank.MINIBOSS -> 2.0f
                EnemyRank.BOSS -> 3.0f
            }
            
            enemy.copy(
                definition = definition.copy(rank = actualRank),
                stats = enemy.stats.copy(
                    maxHp = (enemy.stats.maxHp * rankMultiplier).toInt(),
                    currentHp = (enemy.stats.maxHp * rankMultiplier).toInt(),
                    damage = (enemy.stats.damage * rankMultiplier).toInt(),
                    defense = (enemy.stats.defense * rankMultiplier).toInt()
                )
            )
        } else {
            enemy
        }
    }
    
    /**
     * تدريج العدو حسب الصعوبة
     * Scale enemy based on difficulty
     */
    private fun scaleEnemy(enemy: Enemy): Enemy {
        if (!enableDynamicDifficulty) return enemy
        
        val multiplier = difficultyScaling.getDifficultyMultiplier()
        
        return enemy.copy(
            stats = enemy.stats.copy(
                maxHp = (enemy.stats.maxHp * multiplier).toInt(),
                currentHp = (enemy.stats.maxHp * multiplier).toInt(),
                damage = (enemy.stats.damage * multiplier).toInt(),
                defense = (enemy.stats.defense * multiplier).toInt()
            )
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Object Pooling
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على عدو من الحوض
     * Get enemy from pool
     */
    private fun getFromPool(
        type: EnemyType,
        x: Float,
        y: Float,
        level: Int,
        rank: EnemyRank?
    ): Enemy {
        val pool = enemyPools[type] ?: return createEnemy(type, x, y, level, rank)
        
        return if (pool.available.isNotEmpty()) {
            // Reuse from pool
            val enemy = pool.available.removeAt(0)
            
            // Reset enemy
            enemy.apply {
                this.position.x = x
                this.position.y = y
                this.level = level
                this.spawnPoint = Pair(x, y)
                this.stats = this.stats.copy(currentHp = this.stats.maxHp)
                this.currentState = EnemyState.IDLE
                this.activeEffects.clear()
                this.customData.clear()
            }
            
            pool.active.add(enemy)
            enemy
        } else if (pool.totalCreated < pool.maxPoolSize) {
            // Create new
            val enemy = createEnemy(type, x, y, level, rank)
            pool.active.add(enemy)
            pool.totalCreated++
            enemy
        } else {
            // Pool exhausted, create temporary
            createEnemy(type, x, y, level, rank)
        }
    }
    
    /**
     * إعادة العدو إلى الحوض
     * Return enemy to pool
     */
    private fun returnToPool(enemy: Enemy) {
        val pool = enemyPools[enemy.definition.type] ?: return
        
        pool.active.remove(enemy)
        
        if (pool.available.size < pool.maxPoolSize) {
            pool.available.add(enemy)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Wave System
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * بدء موجة
     * Start wave
     */
    fun startWave(wave: EnemyWave) {
        currentWave = wave
        waveStartTime = System.currentTimeMillis()
        
        EventBus.emit(GameEvent.Enemy.WaveStarted(
            waveNumber = wave.waveNumber,
            enemyCount = wave.enemies.sumOf { it.count },
            isBossWave = wave.isBossWave
        ))
        
        // Spawn wave enemies
        scope.launch {
            kotlinx.coroutines.delay(wave.delayMs)
            
            wave.enemies.forEach { waveEnemy ->
                repeat(waveEnemy.count) { index ->
                    kotlinx.coroutines.delay(waveEnemy.spawnDelayMs * index)
                    
                    val spawnPos = calculateWaveSpawnPosition(
                        waveEnemy.spawnPattern,
                        index,
                        waveEnemy.count
                    )
                    
                    spawnEnemy(
                        type = waveEnemy.type,
                        x = spawnPos.first,
                        y = spawnPos.second,
                        level = waveEnemy.level
                    )?.apply {
                        customData["waveNumber"] = wave.waveNumber
                    }
                }
            }
        }
    }
    
    /**
     * إضافة موجات
     * Queue waves
     */
    fun queueWaves(waves: List<EnemyWave>) {
        pendingWaves.addAll(waves)
    }
    
    /**
     * حساب موقع الإنشاء للموجة
     * Calculate spawn position for wave
     */
    private fun calculateWaveSpawnPosition(
        pattern: SpawnPattern,
        index: Int,
        totalCount: Int
    ): Pair<Float, Float> {
        val playerPos = playerStateManager.getPosition()
        val radius = 400f
        
        return when (pattern) {
            SpawnPattern.RANDOM -> {
                val angle = Random.nextFloat() * 2 * PI.toFloat()
                val distance = Random.nextFloat() * radius
                Pair(
                    playerPos.x + cos(angle) * distance,
                    playerPos.y + sin(angle) * distance
                )
            }
            
            SpawnPattern.CIRCLE -> {
                val angle = (index.toFloat() / totalCount) * 2 * PI.toFloat()
                Pair(
                    playerPos.x + cos(angle) * radius,
                    playerPos.y + sin(angle) * radius
                )
            }
            
            SpawnPattern.LINE -> {
                val spacing = 100f
                Pair(
                    playerPos.x + (index - totalCount / 2) * spacing,
                    playerPos.y + radius
                )
            }
            
            SpawnPattern.BEHIND -> {
                val isFacingRight = playerStateManager.currentState.position.facingRight
                val behindX = if (isFacingRight) playerPos.x - radius else playerPos.x + radius
                Pair(behindX, playerPos.y)
            }
            
            SpawnPattern.SIDES -> {
                val side = if (index % 2 == 0) -1f else 1f
                Pair(playerPos.x + side * radius, playerPos.y)
            }
            
            SpawnPattern.ABOVE -> {
                Pair(
                    playerPos.x + (index - totalCount / 2) * 80f,
                    playerPos.y - radius
                )
            }
            
            SpawnPattern.PORTAL -> {
                // Random position with portal effect
                val angle = Random.nextFloat() * 2 * PI.toFloat()
                val pos = Pair(
                    playerPos.x + cos(angle) * radius * 0.7f,
                    playerPos.y + sin(angle) * radius * 0.7f
                )
                
                // Emit portal particle effect
                particleEngine.emitSanctuaryLight(
                    com.erygra.maskoflight.engine.Vector2D(pos.first, pos.second)
                )
                
                pos
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Spawn Point Management
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل نقطة إنشاء
     * Register spawn point
     */
    fun registerSpawnPoint(spawnPoint: SpawnPoint) {
        spawnPoints[spawnPoint.id] = spawnPoint
    }
    
    /**
     * إلغاء تسجيل نقطة إنشاء
     * Unregister spawn point
     */
    fun unregisterSpawnPoint(id: String) {
        spawnPoints.remove(id)
        spawnTimers.remove(id)
    }
    
    /**
     * تفعيل/تعطيل نقطة إنشاء
     * Toggle spawn point
     */
    fun setSpawnPointActive(id: String, active: Boolean) {
        spawnPoints[id]?.let { point ->
            spawnPoints[id] = point.copy(isActive = active)
        }
    }
    
    /**
     * التحقق من شروط الإنشاء
     * Check spawn conditions
     */
    private fun checkSpawnConditions(spawnPoint: SpawnPoint): Boolean {
        if (spawnPoint.conditions.isEmpty()) return true
        
        return spawnPoint.conditions.all { condition ->
            when (condition) {
                is SpawnCondition.PlayerLevelRange -> {
                    val playerLevel = playerStateManager.currentState.stats.level
                    playerLevel in condition.minLevel..condition.maxLevel
                }
                is SpawnCondition.TimeOfDay -> {
                    // Would need time system
                    true // Placeholder
                }
                is SpawnCondition.QuestActive -> {
                    // Would need quest system
                    true // Placeholder
                }
                is SpawnCondition.BossDefeated -> {
                    _statistics.value.killsByType[condition.bossType] ?: 0 > 0
                }
                is SpawnCondition.RegionCleared -> {
                    // Would need region system
                    true // Placeholder
                }
                SpawnCondition.PlayerHasMask -> {
                    // Check if player has mask equipped
                    true // Placeholder
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Event Handlers
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * معالجة موت العدو
     * Handle enemy death
     */
    private fun handleEnemyDeath(event: GameEvent.Enemy.Died) {
        val enemy = _activeEnemies.value.find { it.id == event.enemyId } ?: return
        
        // Update statistics
        val stats = _statistics.value
        stats.totalKilled++
        stats.currentKillStreak++
        stats.killsByType[enemy.definition.type] = 
            (stats.killsByType[enemy.definition.type] ?: 0) + 1
        
        if (stats.currentKillStreak > stats.longestKillStreak) {
            stats.longestKillStreak = stats.currentKillStreak
        }
        
        when (enemy.definition.rank) {
            EnemyRank.BOSS -> stats.bossesKilled++
            EnemyRank.ELITE, EnemyRank.MINIBOSS -> stats.elitesKilled++
            else -> {}
        }
        
        _statistics.value = stats
        
        // Update difficulty
        difficultyScaling.recentKills++
        
        // Drop loot
        dropLoot(enemy)
        
        // Mark death time
        enemy.customData["deathTime"] = System.currentTimeMillis()
    }
    
    /**
     * معالجة ضرر العدو
     * Handle enemy damage dealt
     */
    private fun handleEnemyDamageDealt(event: GameEvent.Enemy.DamageDealt) {
        val stats = _statistics.value
        stats.totalDamageDealt += event.damage
        _statistics.value = stats
    }
    
    /**
     * معالجة تلقي العدو للضرر
     * Handle enemy damage taken
     */
    private fun handleEnemyDamageTaken(event: GameEvent.Enemy.DamageTaken) {
        val stats = _statistics.value
        stats.totalDamageTaken += event.damage
        _statistics.value = stats
    }
    
    /**
     * معالجة موت اللاعب
     * Handle player death
     */
    private fun handlePlayerDeath() {
        // Reset kill streak
        val stats = _statistics.value
        stats.currentKillStreak = 0
        _statistics.value = stats
        
        // Update difficulty
        difficultyScaling.recentDeaths++
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Loot System
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * إسقاط المكافآت
     * Drop loot from enemy
     */
    private fun dropLoot(enemy: Enemy) {
        val lootTable = enemy.definition.lootTable
        val position = enemy.position
        
        // Coins
        val coinAmount = Random.nextInt(lootTable.coinMin, lootTable.coinMax + 1)
        if (coinAmount > 0) {
            playerStateManager.addCurrency(coinAmount.toFloat())
            
            EventBus.emit(GameEvent.Dropped(
                itemId = "coin",
                quantity = coinAmount,
                x = position.x,
                y = position.y
            ))
        }
        
        // XP
        playerStateManager.addXP(lootTable.xpReward.toFloat())
        
        // Memory Fragments
        if (Random.nextFloat() < lootTable.mfChance) {
            val mfAmount = Random.nextInt(1, 4)
            playerStateManager.addMemoryFragments(mfAmount)
            
            EventBus.emit(GameEvent.Dropped(
                itemId = "memory_fragment",
                quantity = mfAmount,
                x = position.x,
                y = position.y - 20f
            ))
        }
        
        // Item drops
        dropItems(lootTable.commonDrops, position, "common")
        dropItems(lootTable.rareDrops, position, "rare")
        dropItems(lootTable.epicDrops, position, "epic")
        
        // Guaranteed drops
        lootTable.guaranteedDrops.forEach { drop ->
            val quantity = Random.nextInt(drop.quantityMin, drop.quantityMax + 1)
            EventBus.emit(GameEvent.Dropped(
                itemId = drop.itemId,
                quantity = quantity,
                x = position.x + Random.nextFloat() * 60f - 30f,
                y = position.y
            ))
        }
    }
    
    /**
     * إسقاط عناصر
     * Drop items from list
     */
    private fun dropItems(drops: List<ItemDrop>, position: EnemyPosition, rarity: String) {
        drops.forEach { drop ->
            if (Random.nextFloat() < drop.chance) {
                val quantity = Random.nextInt(drop.quantityMin, drop.quantityMax + 1)
                EventBus.emit(GameEvent.Dropped(
                    itemId = drop.itemId,
                    quantity = quantity,
                    x = position.x + Random.nextFloat() * 80f - 40f,
                    y = position.y
                ))
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Save/Load
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ الحالة
     * Save state
     */
    fun saveState(): EnemyManagerState {
        return EnemyManagerState(
            activeEnemies = _activeEnemies.value.map { enemy ->
                SavedEnemy(
                    id = enemy.id,
                    type = enemy.definition.type,
                    x = enemy.position.x,
                    y = enemy.position.y,
                    level = enemy.level,
                    currentHp = enemy.stats.currentHp,
                    currentState = enemy.currentState,
                    customData = enemy.customData.toMap()
                )
            },
            spawnTimers = spawnTimers.toMap(),
            statistics = _statistics.value,
            difficultyLevel = difficultyScaling.difficultyLevel,
            currentWaveNumber = currentWave?.waveNumber ?: 0
        )
    }
    
    /**
     * تحميل الحالة
     * Load state
     */
    fun loadState(state: EnemyManagerState) {
        // Clear current enemies
        _activeEnemies.value.forEach { despawnEnemy(it.id) }
        
        // Restore enemies
        state.activeEnemies.forEach { saved ->
            spawnEnemy(
                type = saved.type,
                x = saved.x,
                y = saved.y,
                level = saved.level
            )?.apply {
                this.stats.currentHp = saved.currentHp
                this.currentState = saved.currentState
                this.customData.putAll(saved.customData)
            }
        }
        
        // Restore timers
        spawnTimers.clear()
        spawnTimers.putAll(state.spawnTimers)
        
        // Restore statistics
        _statistics.value = state.statistics
        
        // Restore difficulty
        difficultyScaling.difficultyLevel = state.difficultyLevel
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MARK: - Public Interface
    // ═══════════════════════════════════════════════════════════════════════════
    
    fun getEnemy(id: String): Enemy? = _activeEnemies.value.find { it.id == id }
    fun getEnemiesInRadius(x: Float, y: Float, radius: Float): List<Enemy> {
        return _activeEnemies.value.filter { enemy ->
            enemy.distanceTo(x, y) <= radius
        }
    }
    
    fun getAIManager(): EnemyAIManager = aiManager
    fun getBatchRenderer(): EnemyBatchRenderer = batchRenderer
    
    fun setMaxActiveEnemies(max: Int) { maxActiveEnemies = max }
    fun setDynamicDifficulty(enabled: Boolean) { enableDynamicDifficulty = enabled }
    fun setObjectPooling(enabled: Boolean) { enableObjectPooling = enabled }
    
    fun clearAllEnemies() {
        _activeEnemies.value.forEach { despawnEnemy(it.id) }
    }
    
    fun cleanup() {
        clearAllEnemies()
        aiManager.cleanup()
        batchRenderer.cleanup()
        spawnPoints.clear()
        spawnTimers.clear()
        pendingWaves.clear()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// MARK: - Save State Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * حالة المدير المحفوظة
 * Saved manager state
 */
data class EnemyManagerState(
    val activeEnemies: List<SavedEnemy>,
    val spawnTimers: Map<String, Long>,
    val statistics: EnemyStatistics,
    val difficultyLevel: Float,
    val currentWaveNumber: Int
)

/**
 * عدو محفوظ
 * Saved enemy
 */
data class SavedEnemy(
    val id: String,
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val level: Int,
    val currentHp: Int,
    val currentState: EnemyState,
    val customData: Map<String, Any>
)

// ════════════════════════════════════════════════════════════════════════════════
// تم الانتهاء من EnemyManager.kt
// EnemyManager.kt Complete
// ════════════════════════════════════════════════════════════════════════════════