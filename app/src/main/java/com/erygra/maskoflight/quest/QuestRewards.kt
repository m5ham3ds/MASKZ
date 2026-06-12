package com.erygra.maskoflight.quest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * QuestRewards.kt - نظام توزيع المكافآت
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة توزيع مكافآت المهام
 * - تتبع المكافآت المتراكمة
 * - نظام مضاعفات المكافآت
 * - معالجة الأشياء والخبرة والذهب
 * 
 * المكونات الرئيسية:
 * - Reward distribution
 * - Multipliers system
 * - Reward history tracking
 * - Special rewards handling
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class QuestRewardsManager {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إجمالي الخبرة المكتسبة
     */
    private val _totalExperienceGained = MutableStateFlow(0L)
    val totalExperienceGained: StateFlow<Long> = _totalExperienceGained.asStateFlow()
    
    /**
     * إجمالي الذهب المكتسب
     */
    private val _totalGoldGained = MutableStateFlow(0L)
    val totalGoldGained: StateFlow<Long> = _totalGoldGained.asStateFlow()
    
    /**
     * الأشياء المكتسبة
     */
    private val _itemsObtained = MutableStateFlow<List<RewardItem>>(emptyList())
    val itemsObtained: StateFlow<List<RewardItem>> = _itemsObtained.asStateFlow()
    
    /**
     * الإنجازات المفتوحة
     */
    private val _achievementsUnlocked = MutableStateFlow<List<String>>(emptyList())
    val achievementsUnlocked: StateFlow<List<String>> = _achievementsUnlocked.asStateFlow()
    
    /**
     * تاريخ المكافآت
     */
    private val _rewardHistory = MutableStateFlow<List<RewardRecord>>(emptyList())
    val rewardHistory: StateFlow<List<RewardRecord>> = _rewardHistory.asStateFlow()
    
    /**
     * مضاعفات المكافآت الحالية
     */
    private val _currentMultipliers = MutableStateFlow<Map<RewardType, Float>>(
        mapOf(
            RewardType.EXPERIENCE to 1.0f,
            RewardType.GOLD to 1.0f,
            RewardType.ITEMS to 1.0f
        )
    )
    val currentMultipliers: StateFlow<Map<RewardType, Float>> = _currentMultipliers.asStateFlow()
    
    /**
     * السمعة المراكمة
     */
    private val _reputationGains = MutableStateFlow<Map<String, Int>>(emptyMap())
    val reputationGains: StateFlow<Map<String, Int>> = _reputationGains.asStateFlow()
    
    // ════════════════════════════════════════════════════════════════════════════
    // Reward Distribution
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * توزيع مكافآت المهمة
     *
     * @param questId معرف المهمة
     * @param rewards المكافآت
     * @return معلومات التوزيع
     */
    fun distributeRewards(
        questId: String,
        rewards: QuestRewards
    ): DistributionResult {
        val result = DistributionResult(questId)
        
        // توزيع الخبرة
        val experienceAmount = applyMultiplier(
            rewards.experience,
            RewardType.EXPERIENCE
        )
        _totalExperienceGained.value += experienceAmount
        result.experienceGained = experienceAmount
        
        // توزيع الذهب
        val goldAmount = applyMultiplier(
            rewards.gold,
            RewardType.GOLD
        )
        _totalGoldGained.value += goldAmount
        result.goldGained = goldAmount
        
        // توزيع الأشياء
        rewards.items.forEach { itemId ->
            val rewardItem = RewardItem(
                itemId = itemId,
                questId = questId,
                obtainedAt = System.currentTimeMillis()
            )
            _itemsObtained.value = _itemsObtained.value + rewardItem
            result.itemsGained.add(rewardItem)
        }
        
        // فتح الإنجازات
        rewards.achievements.forEach { achievementId ->
            _achievementsUnlocked.value = _achievementsUnlocked.value + achievementId
            result.achievementsUnlocked.add(achievementId)
        }
        
        // إضافة السمعة
        rewards.reputation.forEach { (faction, amount) ->
            val currentReputation = _reputationGains.value[faction] ?: 0
            val newReputation = currentReputation + amount
            
            _reputationGains.value = _reputationGains.value.toMutableMap().apply {
                put(faction, newReputation)
            }
            result.reputationGains[faction] = newReputation
        }
        
        // إضافة لسجل التاريخ
        val record = RewardRecord(
            questId = questId,
            experience = experienceAmount,
            gold = goldAmount,
            items = result.itemsGained,
            achievements = result.achievementsUnlocked,
            reputation = result.reputationGains,
            timestamp = System.currentTimeMillis()
        )
        _rewardHistory.value = _rewardHistory.value + record
        
        Timber.d("Rewards distributed for quest: $questId")
        return result
    }
    
    /**
     * تطبيق المضاعفات على القيمة
     *
     * @param value القيمة الأصلية
     * @param type نوع المكافأة
     * @return القيمة بعد تطبيق المضاعف
     */
    private fun applyMultiplier(value: Long, type: RewardType): Long {
        val multiplier = _currentMultipliers.value[type] ?: 1.0f
        return (value * multiplier).toLong()
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Multiplier Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تعيين مضاعف للمكافآت
     *
     * @param type نوع المكافأة
     * @param multiplier قيمة المضاعف
     */
    fun setMultiplier(type: RewardType, multiplier: Float) {
        require(multiplier > 0) { "Multiplier must be greater than 0" }
        
        _currentMultipliers.value = _currentMultipliers.value.toMutableMap().apply {
            put(type, multiplier)
        }
        
        Timber.d("Multiplier set for $type: $multiplier")
    }
    
    /**
     * تعيين جميع المضاعفات
     *
     * @param multipliers خريطة المضاعفات
     */
    fun setAllMultipliers(multipliers: Map<RewardType, Float>) {
        _currentMultipliers.value = multipliers
        Timber.d("All multipliers updated")
    }
    
    /**
     * إضافة مضاعف مؤقت
     *
     * @param type نوع المكافأة
     * @param bonus قيمة الإضافة
     * @param durationMs مدة التطبيق (ميلي ثانية)
     */
    fun applyTemporaryBonus(
        type: RewardType,
        bonus: Float,
        durationMs: Long
    ) {
        val currentMultiplier = _currentMultipliers.value[type] ?: 1.0f
        val newMultiplier = currentMultiplier + bonus
        
        setMultiplier(type, newMultiplier)
        
        // إزالة المضاعف بعد الوقت المحدد
        // هذا يجب أن يتم عبر coroutine scope في التطبيق الفعلي
        Timber.d("Temporary bonus applied for $type: +$bonus (${durationMs}ms)")
    }
    
    /**
     * إعادة تعيين المضاعفات
     */
    fun resetMultipliers() {
        _currentMultipliers.value = mapOf(
            RewardType.EXPERIENCE to 1.0f,
            RewardType.GOLD to 1.0f,
            RewardType.ITEMS to 1.0f
        )
        Timber.d("Multipliers reset to default")
    }
    
    /**
     * الحصول على مضاعف محدد
     *
     * @param type نوع المكافأة
     */
    fun getMultiplier(type: RewardType): Float {
        return _currentMultipliers.value[type] ?: 1.0f
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Reward Queries
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إجمالي المكافآت لمهمة محددة
     *
     * @param questId معرف المهمة
     */
    fun getQuestRewardTotal(questId: String): RewardSummary? {
        val record = _rewardHistory.value.find { it.questId == questId } ?: return null
        
        return RewardSummary(
            experience = record.experience,
            gold = record.gold,
            itemCount = record.items.size,
            achievementCount = record.achievements.size
        )
    }
    
    /**
     * الحصول على مكافآت فترة زمنية
     *
     * @param startTime الوقت البداية
     * @param endTime الوقت النهاية
     */
    fun getRewardsInTimeRange(startTime: Long, endTime: Long): RewardSummary {
        val records = _rewardHistory.value.filter {
            it.timestamp in startTime..endTime
        }
        
        var totalExp = 0L
        var totalGold = 0L
        var totalItems = 0
        var totalAchievements = 0
        
        records.forEach {
            totalExp += it.experience
            totalGold += it.gold
            totalItems += it.items.size
            totalAchievements += it.achievements.size
        }
        
        return RewardSummary(
            experience = totalExp,
            gold = totalGold,
            itemCount = totalItems,
            achievementCount = totalAchievements
        )
    }
    
    /**
     * الحصول على الأشياء من مهمة محددة
     *
     * @param questId معرف المهمة
     */
    fun getItemsFromQuest(questId: String): List<RewardItem> {
        return _itemsObtained.value.filter { it.questId == questId }
    }
    
    /**
     * الحصول على الأشياء حسب نوعها
     *
     * @param itemType نوع الشيء
     */
    fun getItemsByType(itemType: String): List<RewardItem> {
        return _itemsObtained.value.filter { it.itemId == itemType }
    }
    
    /**
     * إجمالي السمعة مع جزء معين
     *
     * @param faction الفصيل
     */
    fun getReputation(faction: String): Int {
        return _reputationGains.value[faction] ?: 0
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Statistics
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إحصائيات المكافآت
     */
    fun getRewardStats(): RewardStats {
        val totalItems = _itemsObtained.value.size
        val uniqueItems = _itemsObtained.value.distinctBy { it.itemId }.size
        val totalAchievements = _achievementsUnlocked.value.size
        val totalFactions = _reputationGains.value.size
        
        return RewardStats(
            totalExperience = _totalExperienceGained.value,
            totalGold = _totalGoldGained.value,
            totalItems = totalItems,
            uniqueItems = uniqueItems,
            totalAchievements = totalAchievements,
            totalFactions = totalFactions,
            averageExperiencePerQuest = if (_rewardHistory.value.isNotEmpty()) {
                _totalExperienceGained.value / _rewardHistory.value.size
            } else {
                0L
            }
        )
    }
    
    /**
     * الحصول على أفضل المكافآت
     *
     * @param count عدد أفضل المكافآت
     */
    fun getTopRewards(count: Int = 10): List<RewardRecord> {
        return _rewardHistory.value
            .sortedByDescending { it.experience + it.gold }
            .take(count)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save/Load
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ حالة النظام
     */
    fun saveState(): RewardManagerState {
        return RewardManagerState(
            totalExperienceGained = _totalExperienceGained.value,
            totalGoldGained = _totalGoldGained.value,
            itemsObtained = _itemsObtained.value,
            achievementsUnlocked = _achievementsUnlocked.value,
            rewardHistory = _rewardHistory.value,
            currentMultipliers = _currentMultipliers.value,
            reputationGains = _reputationGains.value
        )
    }
    
    /**
     * استعادة حالة النظام
     *
     * @param state الحالة المحفوظة
     */
    fun loadState(state: RewardManagerState) {
        _totalExperienceGained.value = state.totalExperienceGained
        _totalGoldGained.value = state.totalGoldGained
        _itemsObtained.value = state.itemsObtained
        _achievementsUnlocked.value = state.achievementsUnlocked
        _rewardHistory.value = state.rewardHistory
        _currentMultipliers.value = state.currentMultipliers
        _reputationGains.value = state.reputationGains
        
        Timber.d("RewardManager state loaded")
    }
    
    /**
     * إعادة تعيين جميع البيانات
     */
    fun reset() {
        _totalExperienceGained.value = 0L
        _totalGoldGained.value = 0L
        _itemsObtained.value = emptyList()
        _achievementsUnlocked.value = emptyList()
        _rewardHistory.value = emptyList()
        _reputationGains.value = emptyMap()
        resetMultipliers()
        
        Timber.d("RewardManager reset")
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// Data Classes
// ════════════════════════════════════════════════════════════════════════════════

/**
 * نوع المكافأة
 */
enum class RewardType {
    EXPERIENCE, GOLD, ITEMS
}

/**
 * شيء المكافأة
 *
 * @property itemId معرف الشيء
 * @property questId معرف المهمة
 * @property obtainedAt وقت الحصول عليه
 */
data class RewardItem(
    val itemId: String,
    val questId: String,
    val obtainedAt: Long
)

/**
 * سجل المكافآت
 *
 * @property questId معرف المهمة
 * @property experience الخبرة المكتسبة
 * @property gold الذهب المكتسب
 * @property items الأشياء المكتسبة
 * @property achievements الإنجازات المفتوحة
 * @property reputation السمعة المكتسبة
 * @property timestamp وقت التسجيل
 */
data class RewardRecord(
    val questId: String,
    val experience: Long,
    val gold: Long,
    val items: List<RewardItem>,
    val achievements: List<String>,
    val reputation: Map<String, Int>,
    val timestamp: Long
)

/**
 * نتيجة التوزيع
 *
 * @property questId معرف المهمة
 * @property experienceGained الخبرة المكتسبة
 * @property goldGained الذهب المكتسب
 * @property itemsGained الأشياء المكتسبة
 * @property achievementsUnlocked الإنجازات المفتوحة
 * @property reputationGains السمعة المكتسبة
 */
data class DistributionResult(
    val questId: String,
    var experienceGained: Long = 0L,
    var goldGained: Long = 0L,
    val itemsGained: MutableList<RewardItem> = mutableListOf(),
    val achievementsUnlocked: MutableList<String> = mutableListOf(),
    val reputationGains: MutableMap<String, Int> = mutableMapOf()
)

/**
 * ملخص المكافآت
 *
 * @property experience الخبرة
 * @property gold الذهب
 * @property itemCount عدد الأشياء
 * @property achievementCount عدد الإنجازات
 */
data class RewardSummary(
    val experience: Long,
    val gold: Long,
    val itemCount: Int,
    val achievementCount: Int
)

/**
 * إحصائيات المكافآت
 *
 * @property totalExperience إجمالي الخبرة
 * @property totalGold إجمالي الذهب
 * @property totalItems إجمالي الأشياء
 * @property uniqueItems عدد الأشياء الفريدة
 * @property totalAchievements إجمالي الإنجازات
 * @property totalFactions عدد الفصائل
 * @property averageExperiencePerQuest متوسط الخبرة لكل مهمة
 */
data class RewardStats(
    val totalExperience: Long,
    val totalGold: Long,
    val totalItems: Int,
    val uniqueItems: Int,
    val totalAchievements: Int,
    val totalFactions: Int,
    val averageExperiencePerQuest: Long
)

/**
 * حالة مدير المكافآت (للحفظ)
 */
data class RewardManagerState(
    val totalExperienceGained: Long,
    val totalGoldGained: Long,
    val itemsObtained: List<RewardItem>,
    val achievementsUnlocked: List<String>,
    val rewardHistory: List<RewardRecord>,
    val currentMultipliers: Map<RewardType, Float>,
    val reputationGains: Map<String, Int>
)