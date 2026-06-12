package com.erygra.maskoflight.data.dao

import androidx.room.*
import com.erygra.maskoflight.data.entities.QuestEntity
import kotlinx.coroutines.flow.Flow

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * QuestDao - واجهة الوصول لبيانات المهام
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Data Access Object لإدارة عمليات قاعدة البيانات المتعلقة بالمهام
 * 
 * Data Access Object for managing quest database operations
 * 
 * العمليات المدعومة / Supported Operations:
 * - إدارة المهام (Quest management)
 * - تتبع التقدم (Progress tracking)
 * - تصفية المهام (Quest filtering)
 * - إحصائيات المهام (Quest statistics)
 * 
 * @author Erygra Studio
 * @since 1.0.0
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Dao
interface QuestDao {

    // ═══════════════════════════════════════════════════════════════════════
    // Insert Operations - عمليات الإدراج
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * إدراج مهمة جديدة
     * Insert new quest
     * 
     * @param quest بيانات المهمة / Quest data
     * @return معرف السطر المُدرج / Inserted row ID
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuest(quest: QuestEntity): Long

    /**
     * إدراج أو استبدال مهمة
     * Insert or replace quest
     * 
     * @param quest بيانات المهمة / Quest data
     * @return معرف السطر / Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceQuest(quest: QuestEntity): Long

    /**
     * إدراج عدة مهام
     * Insert multiple quests
     * 
     * @param quests قائمة المهام / Quests list
     * @return قائمة معرفات الصفوف / Row IDs list
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuests(quests: List<QuestEntity>): List<Long>

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations - عمليات التحديث
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * تحديث مهمة
     * Update quest
     * 
     * @param quest بيانات المهمة المحدثة / Updated quest data
     * @return عدد الصفوف المحدثة / Number of updated rows
     */
    @Update
    suspend fun updateQuest(quest: QuestEntity): Int

    /**
     * تحديث عدة مهام
     * Update multiple quests
     * 
     * @param quests قائمة المهام / Quests list
     * @return عدد الصفوف المحدثة / Number of updated rows
     */
    @Update
    suspend fun updateQuests(quests: List<QuestEntity>): Int

    /**
     * تحديث حالة مهمة
     * Update quest status
     * 
     * @param questId معرف المهمة / Quest ID
     * @param status الحالة الجديدة / New status
     */
    @Query("""
        UPDATE quests 
        SET status = :status,
            updatedAt = :updateTime
        WHERE questId = :questId
    """)
    suspend fun updateQuestStatus(
        questId: String,
        status: String,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث تقدم مهمة
     * Update quest progress
     * 
     * @param questId معرف المهمة / Quest ID
     * @param progress نسبة التقدم / Progress percentage
     * @param progressData بيانات التقدم التفصيلية / Detailed progress data
     */
    @Query("""
        UPDATE quests 
        SET progress = :progress,
            progressData = :progressData,
            updatedAt = :updateTime
        WHERE questId = :questId
    """)
    suspend fun updateQuestProgress(
        questId: String,
        progress: Int,
        progressData: Map<String, Int>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث أهداف مهمة مكتملة
     * Update completed quest objectives
     * 
     * @param questId معرف المهمة / Quest ID
     * @param completedObjectives الأهداف المكتملة / Completed objectives
     */
    @Query("""
        UPDATE quests 
        SET completedObjectives = :completedObjectives,
            updatedAt = :updateTime
        WHERE questId = :questId
    """)
    suspend fun updateCompletedObjectives(
        questId: String,
        completedObjectives: List<String>,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * قبول مهمة (تغيير الحالة من available إلى active)
     * Accept quest (change status from available to active)
     * 
     * @param questId معرف المهمة / Quest ID
     */
    @Query("""
        UPDATE quests 
        SET status = 'active',
            acceptedAt = :acceptTime,
            updatedAt = :acceptTime
        WHERE questId = :questId AND status = 'available'
    """)
    suspend fun acceptQuest(
        questId: String,
        acceptTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * إكمال مهمة
     * Complete quest
     * 
     * @param questId معرف المهمة / Quest ID
     */
    @Query("""
        UPDATE quests 
        SET status = 'completed',
            completedAt = :completionTime,
            progress = 100,
            updatedAt = :completionTime
        WHERE questId = :questId
    """)
    suspend fun completeQuest(
        questId: String,
        completionTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * فشل مهمة
     * Fail quest
     * 
     * @param questId معرف المهمة / Quest ID
     */
    @Query("""
        UPDATE quests 
        SET status = 'failed',
            updatedAt = :updateTime
        WHERE questId = :questId
    """)
    suspend fun failQuest(
        questId: String,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * التخلي عن مهمة (إعادة إلى available)
     * Abandon quest (reset to available)
     * 
     * @param questId معرف المهمة / Quest ID
     */
    @Query("""
        UPDATE quests 
        SET status = 'available',
            acceptedAt = NULL,
            progress = 0,
            completedObjectives = '[]',
            progressData = '{}',
            updatedAt = :updateTime
        WHERE questId = :questId
    """)
    suspend fun abandonQuest(
        questId: String,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    /**
     * تحديث عداد التكرار
     * Update repeat count
     * 
     * @param questId معرف المهمة / Quest ID
     */
    @Query("""
        UPDATE quests 
        SET repeatCount = repeatCount + 1,
            updatedAt = :updateTime
        WHERE questId = :questId
    """)
    suspend fun incrementRepeatCount(
        questId: String,
        updateTime: Long = System.currentTimeMillis()
    ): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Query Operations - عمليات الاستعلام
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على مهمة بمعرفها (مع المراقبة)
     * Get quest by ID (with observation)
     * 
     * @param questId معرف المهمة / Quest ID
     * @return Flow من بيانات المهمة / Flow of quest data
     */
    @Query("SELECT * FROM quests WHERE questId = :questId")
    fun getQuestFlow(questId: String): Flow<QuestEntity?>

    /**
     * الحصول على مهمة بمعرفها (استعلام مباشر)
     * Get quest by ID (direct query)
     * 
     * @param questId معرف المهمة / Quest ID
     * @return بيانات المهمة / Quest data
     */
    @Query("SELECT * FROM quests WHERE questId = :questId")
    suspend fun getQuest(questId: String): QuestEntity?

    /**
     * الحصول على جميع المهام
     * Get all quests
     * 
     * @return قائمة المهام / Quests list
     */
    @Query("SELECT * FROM quests ORDER BY priority DESC, updatedAt DESC")
    suspend fun getAllQuests(): List<QuestEntity>

    /**
     * الحصول على جميع المهام (مع المراقبة)
     * Get all quests (with observation)
     * 
     * @return Flow من قائمة المهام / Flow of quests list
     */
    @Query("SELECT * FROM quests ORDER BY priority DESC, updatedAt DESC")
    fun getAllQuestsFlow(): Flow<List<QuestEntity>>

    /**
     * الحصول على المهام حسب الحالة
     * Get quests by status
     * 
     * @param status الحالة / Status
     * @return قائمة المهام / Quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE status = :status 
        ORDER BY priority DESC, updatedAt DESC
    """)
    suspend fun getQuestsByStatus(status: String): List<QuestEntity>

    /**
     * الحصول على المهام حسب الحالة (مع المراقبة)
     * Get quests by status (with observation)
     * 
     * @param status الحالة / Status
     * @return Flow من قائمة المهام / Flow of quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE status = :status 
        ORDER BY priority DESC, updatedAt DESC
    """)
    fun getQuestsByStatusFlow(status: String): Flow<List<QuestEntity>>

    /**
     * الحصول على المهام النشطة
     * Get active quests
     * 
     * @return قائمة المهام النشطة / Active quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE status = 'active' 
        ORDER BY priority DESC
    """)
    suspend fun getActiveQuests(): List<QuestEntity>

    /**
     * الحصول على المهام النشطة (مع المراقبة)
     * Get active quests (with observation)
     * 
     * @return Flow من المهام النشطة / Flow of active quests
     */
    @Query("""
        SELECT * FROM quests 
        WHERE status = 'active' 
        ORDER BY priority DESC
    """)
    fun getActiveQuestsFlow(): Flow<List<QuestEntity>>

    /**
     * الحصول على المهام المتاحة
     * Get available quests
     * 
     * @return قائمة المهام المتاحة / Available quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE status = 'available' 
        ORDER BY priority DESC
    """)
    suspend fun getAvailableQuests(): List<QuestEntity>

    /**
     * الحصول على المهام المكتملة
     * Get completed quests
     * 
     * @return قائمة المهام المكتملة / Completed quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE status = 'completed' 
        ORDER BY completedAt DESC
    """)
    suspend fun getCompletedQuests(): List<QuestEntity>

    /**
     * الحصول على المهام حسب النوع
     * Get quests by type
     * 
     * @param type نوع المهمة / Quest type
     * @return قائمة المهام / Quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE type = :type 
        ORDER BY priority DESC
    """)
    suspend fun getQuestsByType(type: String): List<QuestEntity>

    /**
     * الحصول على المهام حسب المنطقة
     * Get quests by region
     * 
     * @param region المنطقة / Region
     * @return قائمة المهام / Quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE region = :region 
        ORDER BY priority DESC
    """)
    suspend fun getQuestsByRegion(region: String): List<QuestEntity>

    /**
     * الحصول على المهام حسب المنطقة والحالة
     * Get quests by region and status
     * 
     * @param region المنطقة / Region
     * @param status الحالة / Status
     * @return قائمة المهام / Quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE region = :region AND status = :status
        ORDER BY priority DESC
    """)
    suspend fun getQuestsByRegionAndStatus(region: String, status: String): List<QuestEntity>

    /**
     * الحصول على المهام اليومية
     * Get daily quests
     * 
     * @return قائمة المهام اليومية / Daily quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE type = 'daily' AND status IN ('available', 'active')
        ORDER BY priority DESC
    """)
    suspend fun getDailyQuests(): List<QuestEntity>

    /**
     * الحصول على المهام الأسبوعية
     * Get weekly quests
     * 
     * @return قائمة المهام الأسبوعية / Weekly quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE type = 'weekly' AND status IN ('available', 'active')
        ORDER BY priority DESC
    """)
    suspend fun getWeeklyQuests(): List<QuestEntity>

    /**
     * الحصول على المهام الرئيسية
     * Get main quests
     * 
     * @return قائمة المهام الرئيسية / Main quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE type = 'main'
        ORDER BY priority DESC
    """)
    suspend fun getMainQuests(): List<QuestEntity>

    /**
     * الحصول على المهام الجانبية
     * Get side quests
     * 
     * @return قائمة المهام الجانبية / Side quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE type = 'side'
        ORDER BY priority DESC
    """)
    suspend fun getSideQuests(): List<QuestEntity>

    /**
     * البحث عن مهام بالاسم
     * Search quests by name
     * 
     * @param searchQuery نص البحث / Search query
     * @return قائمة المهام المطابقة / Matching quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE nameAr LIKE '%' || :searchQuery || '%' 
           OR nameEn LIKE '%' || :searchQuery || '%'
        ORDER BY priority DESC
    """)
    suspend fun searchQuestsByName(searchQuery: String): List<QuestEntity>

    /**
     * فحص وجود مهمة
     * Check if quest exists
     * 
     * @param questId معرف المهمة / Quest ID
     * @return true إذا كانت موجودة / true if exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM quests WHERE questId = :questId)")
    suspend fun questExists(questId: String): Boolean

    /**
     * الحصول على عدد المهام
     * Get quest count
     * 
     * @return عدد المهام / Quest count
     */
    @Query("SELECT COUNT(*) FROM quests")
    suspend fun getQuestCount(): Int

    /**
     * الحصول على عدد المهام حسب الحالة
     * Get quest count by status
     * 
     * @param status الحالة / Status
     * @return عدد المهام / Quest count
     */
    @Query("SELECT COUNT(*) FROM quests WHERE status = :status")
    suspend fun getQuestCountByStatus(status: String): Int

    /**
     * الحصول على عدد المهام النشطة (مع المراقبة)
     * Get active quest count (with observation)
     * 
     * @return Flow من عدد المهام / Flow of quest count
     */
    @Query("SELECT COUNT(*) FROM quests WHERE status = 'active'")
    fun getActiveQuestCountFlow(): Flow<Int>

    // ═══════════════════════════════════════════════════════════════════════
    // Delete Operations - عمليات الحذف
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * حذف مهمة
     * Delete quest
     * 
     * @param quest بيانات المهمة / Quest data
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Delete
    suspend fun deleteQuest(quest: QuestEntity): Int

    /**
     * حذف مهمة بمعرفها
     * Delete quest by ID
     * 
     * @param questId معرف المهمة / Quest ID
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM quests WHERE questId = :questId")
    suspend fun deleteQuestById(questId: String): Int

    /**
     * حذف جميع المهام
     * Delete all quests
     * 
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM quests")
    suspend fun deleteAllQuests(): Int

    /**
     * حذف المهام المكتملة
     * Delete completed quests
     * 
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM quests WHERE status = 'completed'")
    suspend fun deleteCompletedQuests(): Int

    /**
     * حذف المهام الفاشلة
     * Delete failed quests
     * 
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("DELETE FROM quests WHERE status = 'failed'")
    suspend fun deleteFailedQuests(): Int

    /**
     * حذف المهام المنتهية
     * Delete expired quests
     * 
     * @param currentTime الوقت الحالي / Current time
     * @return عدد الصفوف المحذوفة / Number of deleted rows
     */
    @Query("""
        DELETE FROM quests 
        WHERE isTimed = 1 AND expiresAt < :currentTime
    """)
    suspend fun deleteExpiredQuests(currentTime: Long = System.currentTimeMillis()): Int

    // ═══════════════════════════════════════════════════════════════════════
    // Chain & Sequence Operations - عمليات السلاسل
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على مهام في سلسلة
     * Get quests in chain
     * 
     * @param chainId معرف السلسلة / Chain ID
     * @return قائمة المهام / Quests list
     */
    @Query("""
        SELECT * FROM quests 
        WHERE chainId = :chainId 
        ORDER BY chainOrder ASC
    """)
    suspend fun getQuestsInChain(chainId: String): List<QuestEntity>

    /**
     * الحصول على المهمة التالية في السلسلة
     * Get next quest in chain
     * 
     * @param currentQuestId معرف المهمة الحالية / Current quest ID
     * @return المهمة التالية / Next quest
     */
    @Query("""
        SELECT * FROM quests 
        WHERE questId = (
            SELECT nextQuest FROM quests WHERE questId = :currentQuestId
        )
    """)
    suspend fun getNextQuestInChain(currentQuestId: String): QuestEntity?

    // ═══════════════════════════════════════════════════════════════════════
    // Analytics Queries - استعلامات التحليلات
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * الحصول على نسبة الإكمال الإجمالية
     * Get overall completion rate
     * 
     * @return نسبة الإكمال (0-100) / Completion rate (0-100)
     */
    @Query("""
        SELECT 
            CAST(COUNT(CASE WHEN status = 'completed' THEN 1 END) AS FLOAT) / 
            CAST(COUNT(*) AS FLOAT) * 100
        FROM quests
    """)
    suspend fun getCompletionRate(): Float

    /**
     * الحصول على أكثر منطقة بها مهام
     * Get region with most quests
     * 
     * @return المنطقة / Region
     */
    @Query("""
        SELECT region, COUNT(*) as count 
        FROM quests 
        GROUP BY region 
        ORDER BY count DESC 
        LIMIT 1
    """)
    @MapInfo(keyColumn = "region", valueColumn = "count")
    suspend fun getRegionWithMostQuests(): Map<String, Int>

    /**
     * الحصول على متوسط تقدم المهام النشطة
     * Get average progress of active quests
     * 
     * @return متوسط التقدم / Average progress
     */
    @Query("SELECT AVG(progress) FROM quests WHERE status = 'active'")
    suspend fun getAverageActiveQuestProgress(): Float

    /**
     * الحصول على توزيع المهام حسب النوع
     * Get quest distribution by type
     * 
     * @return خريطة النوع -> العدد / Type -> Count map
     */
    @Query("""
        SELECT type, COUNT(*) as count 
        FROM quests 
        GROUP BY type 
        ORDER BY count DESC
    """)
    @MapInfo(keyColumn = "type", valueColumn = "count")
    suspend fun getQuestDistributionByType(): Map<String, Int>

    /**
     * الحصول على توزيع المهام حسب الحالة
     * Get quest distribution by status
     * 
     * @return خريطة الحالة -> العدد / Status -> Count map
     */
    @Query("""
        SELECT status, COUNT(*) as count 
        FROM quests 
        GROUP BY status 
        ORDER BY count DESC
    """)
    @MapInfo(keyColumn = "status", valueColumn = "count")
    suspend fun getQuestDistributionByStatus(): Map<String, Int>
}