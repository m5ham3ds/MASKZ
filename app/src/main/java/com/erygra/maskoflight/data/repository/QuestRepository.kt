package com.erygra.maskoflight.data.repository

import com.erygra.maskoflight.data.dao.QuestDao
import com.erygra.maskoflight.data.entities.QuestEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * QuestRepository - مستودع بيانات المهام
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * طبقة المستودع لإدارة بيانات المهام والتحديات
 * 
 * Repository layer for managing quest and challenge data
 * 
 * المسؤوليات / Responsibilities:
 * - إدارة دورة حياة المهام (Quest lifecycle management)
 * - تتبع التقدم (Progress tracking)
 * - إدارة المكافآت (Rewards management)
 * - تصفية وبحث المهام (Quest filtering & search)
 * - التخزين المؤقت (Caching)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Singleton
class QuestRepository @Inject constructor(
    private val questDao: QuestDao
) {

    // ═══════════════════════════════════════════════════════════════════════
    // Cache - التخزين المؤقت
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * كاش المهام النشطة
     * Active quests cache
     */
    private var cachedActiveQuests: List<QuestEntity>? = null

    /**
     * وقت آخر تحديث للكاش
     * Last cache update time
     */
    private var lastCacheUpdate: Long = 0L

    /**
     * مدة صلاحية الكاش (3 دقائق)
     * Cache validity duration (3 minutes)
     */
    private val cacheValidityDuration = 3 * 60 * 1000L

    // ═══════════════════════════════════════════════════════════════════════
    // Create Operations - عمليات الإنشاء
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إضافة مهمة جديدة
     * Add new quest
     * 
     * @param quest بيانات المهمة / Quest data
     * @return Result<Long> معرف الصف المُدرج / Inserted row ID
     */
    suspend fun addQuest(quest: QuestEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val rowId = questDao.insertQuest(quest)
            invalidateCache()
            Result.success(rowId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * إضافة عدة مهام
     * Add multiple quests
     * 
     * @param quests قائمة المهام / Quests list
     * @return Result<List<Long>> قائمة معرفات الصفوف / Row IDs list
     */
    suspend fun addQuests(quests: List<QuestEntity>): Result<List<Long>> = 
        withContext(Dispatchers.IO) {
            try {
                val rowIds = questDao.insertQuests(quests)
                invalidateCache()
                Result.success(rowIds)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // Read Operations - عمليات القراءة
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على مهمة بمعرفها (مع المراقبة)
     * Get quest by ID (with observation)
     * 
     * @param questId معرف المهمة / Quest ID
     * @return Flow<QuestEntity?> تدفق بيانات المهمة / Flow of quest data
     */
    fun getQuestFlow(questId: String): Flow<QuestEntity?> {
        return questDao.getQuestFlow(questId)
            .catch { e ->
                e.printStackTrace()
                emit(null)
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * الحصول على مهمة بمعرفها
     * Get quest by ID
     * 
     * @param questId معرف المهمة / Quest ID
     * @return Result<QuestEntity?> بيانات المهمة / Quest data
     */
    suspend fun getQuest(questId: String): Result<QuestEntity?> = withContext(Dispatchers.IO) {
        try {
            val quest = questDao.getQuest(questId)
            Result.success(quest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على جميع المهام
     * Get all quests
     * 
     * @return Result<List<QuestEntity>> قائمة المهام / Quests list
     */
    suspend fun getAllQuests(): Result<List<QuestEntity>> = withContext(Dispatchers.IO) {
        try {
            val quests = questDao.getAllQuests()
            Result.success(quests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على المهام النشطة (مع التخزين المؤقت)
     * Get active quests (with caching)
     * 
     * @param forceRefresh إجبار التحديث / Force refresh
     * @return Result<List<QuestEntity>> المهام النشطة / Active quests
     */
    suspend fun getActiveQuests(forceRefresh: Boolean = false): Result<List<QuestEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                // التحقق من الكاش
                // Check cache
                if (!forceRefresh && isCacheValid()) {
                    return@withContext Result.success(cachedActiveQuests ?: emptyList())
                }

                // جلب من قاعدة البيانات
                // Fetch from database
                val quests = questDao.getActiveQuests()

                // تحديث الكاش
                // Update cache
                cachedActiveQuests = quests
                lastCacheUpdate = System.currentTimeMillis()

                Result.success(quests)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على المهام النشطة (مع المراقبة)
     * Get active quests (with observation)
     * 
     * @return Flow<List<QuestEntity>> تدفق المهام النشطة / Flow of active quests
     */
    fun getActiveQuestsFlow(): Flow<List<QuestEntity>> {
        return questDao.getActiveQuestsFlow()
            .catch { e ->
                e.printStackTrace()
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * الحصول على المهام المتاحة
     * Get available quests
     * 
     * @return Result<List<QuestEntity>> المهام المتاحة / Available quests
     */
    suspend fun getAvailableQuests(): Result<List<QuestEntity>> = withContext(Dispatchers.IO) {
        try {
            val quests = questDao.getAvailableQuests()
            Result.success(quests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على المهام المكتملة
     * Get completed quests
     * 
     * @return Result<List<QuestEntity>> المهام المكتملة / Completed quests
     */
    suspend fun getCompletedQuests(): Result<List<QuestEntity>> = withContext(Dispatchers.IO) {
        try {
            val quests = questDao.getCompletedQuests()
            Result.success(quests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على المهام حسب النوع
     * Get quests by type
     * 
     * @param type نوع المهمة / Quest type
     * @return Result<List<QuestEntity>> قائمة المهام / Quests list
     */
    suspend fun getQuestsByType(type: String): Result<List<QuestEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val quests = questDao.getQuestsByType(type)
                Result.success(quests)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على المهام حسب المنطقة
     * Get quests by region
     * 
     * @param region المنطقة / Region
     * @return Result<List<QuestEntity>> قائمة المهام / Quests list
     */
    suspend fun getQuestsByRegion(region: String): Result<List<QuestEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val quests = questDao.getQuestsByRegion(region)
                Result.success(quests)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * الحصول على المهام اليومية
     * Get daily quests
     * 
     * @return Result<List<QuestEntity>> المهام اليومية / Daily quests
     */
    suspend fun getDailyQuests(): Result<List<QuestEntity>> = withContext(Dispatchers.IO) {
        try {
            val quests = questDao.getDailyQuests()
            Result.success(quests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على المهام الأسبوعية
     * Get weekly quests
     * 
     * @return Result<List<QuestEntity>> المهام الأسبوعية / Weekly quests
     */
    suspend fun getWeeklyQuests(): Result<List<QuestEntity>> = withContext(Dispatchers.IO) {
        try {
            val quests = questDao.getWeeklyQuests()
            Result.success(quests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * البحث عن مهام بالاسم
     * Search quests by name
     * 
     * @param searchQuery نص البحث / Search query
     * @return Result<List<QuestEntity>> نتائج البحث / Search results
     */
    suspend fun searchQuestsByName(searchQuery: String): Result<List<QuestEntity>> = 
        withContext(Dispatchers.IO) {
            try {
                val quests = questDao.searchQuestsByName(searchQuery)
                Result.success(quests)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations - عمليات التحديث
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث مهمة
     * Update quest
     * 
     * @param quest بيانات المهمة المحدثة / Updated quest data
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateQuest(quest: QuestEntity): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = questDao.updateQuest(quest)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * تحديث حالة مهمة
     * Update quest status
     * 
     * @param questId معرف المهمة / Quest ID
     * @param status الحالة الجديدة / New status
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateQuestStatus(questId: String, status: String): Result<Int> = 
        withContext(Dispatchers.IO) {
            try {
                val updatedRows = questDao.updateQuestStatus(questId, status)
                invalidateCache()
                Result.success(updatedRows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * تحديث تقدم مهمة
     * Update quest progress
     * 
     * @param questId معرف المهمة / Quest ID
     * @param progress نسبة التقدم / Progress percentage
     * @param progressData بيانات التقدم / Progress data
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun updateQuestProgress(
        questId: String,
        progress: Int,
        progressData: Map<String, Int>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = questDao.updateQuestProgress(questId, progress, progressData)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * قبول مهمة
     * Accept quest
     * 
     * @param questId معرف المهمة / Quest ID
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun acceptQuest(questId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = questDao.acceptQuest(questId)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * إكمال مهمة
     * Complete quest
     * 
     * @param questId معرف المهمة / Quest ID
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun completeQuest(questId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = questDao.completeQuest(questId)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * فشل مهمة
     * Fail quest
     * 
     * @param questId معرف المهمة / Quest ID
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun failQuest(questId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = questDao.failQuest(questId)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * التخلي عن مهمة
     * Abandon quest
     * 
     * @param questId معرف المهمة / Quest ID
     * @return Result<Int> عدد الصفوف المحدثة / Updated rows count
     */
    suspend fun abandonQuest(questId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val updatedRows = questDao.abandonQuest(questId)
            invalidateCache()
            Result.success(updatedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Delete Operations - عمليات الحذف
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حذف مهمة
     * Delete quest
     * 
     * @param quest بيانات المهمة / Quest data
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteQuest(quest: QuestEntity): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = questDao.deleteQuest(quest)
            invalidateCache()
            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف مهمة بمعرفها
     * Delete quest by ID
     * 
     * @param questId معرف المهمة / Quest ID
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteQuestById(questId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = questDao.deleteQuestById(questId)
            invalidateCache()
            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف المهام المكتملة
     * Delete completed quests
     * 
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteCompletedQuests(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = questDao.deleteCompletedQuests()
            invalidateCache()
            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * حذف المهام المنتهية
     * Delete expired quests
     * 
     * @return Result<Int> عدد الصفوف المحذوفة / Deleted rows count
     */
    suspend fun deleteExpiredQuests(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val deletedRows = questDao.deleteExpiredQuests()
            invalidateCache()
            Result.success(deletedRows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Analytics Operations - عمليات التحليلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على نسبة الإكمال
     * Get completion rate
     * 
     * @return Result<Float> نسبة الإكمال / Completion rate
     */
    suspend fun getCompletionRate(): Result<Float> = withContext(Dispatchers.IO) {
        try {
            val rate = questDao.getCompletionRate()
            Result.success(rate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على عدد المهام حسب الحالة
     * Get quest count by status
     * 
     * @param status الحالة / Status
     * @return Result<Int> عدد المهام / Quest count
     */
    suspend fun getQuestCountByStatus(status: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = questDao.getQuestCountByStatus(status)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الحصول على عدد المهام النشطة (مع المراقبة)
     * Get active quest count (with observation)
     * 
     * @return Flow<Int> تدفق عدد المهام / Flow of quest count
     */
    fun getActiveQuestCountFlow(): Flow<Int> {
        return questDao.getActiveQuestCountFlow()
            .catch { e ->
                e.printStackTrace()
                emit(0)
            }
            .flowOn(Dispatchers.IO)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cache Management - إدارة الكاش
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * التحقق من صلاحية الكاش
     * Check cache validity
     */
    private fun isCacheValid(): Boolean {
        return cachedActiveQuests != null &&
               (System.currentTimeMillis() - lastCacheUpdate) < cacheValidityDuration
    }

    /**
     * إبطال الكاش
     * Invalidate cache
     */
    private fun invalidateCache() {
        cachedActiveQuests = null
        lastCacheUpdate = 0L
    }

    /**
     * مسح الكاش يدوياً
     * Clear cache manually
     */
    fun clearCache() {
        invalidateCache()
    }
}