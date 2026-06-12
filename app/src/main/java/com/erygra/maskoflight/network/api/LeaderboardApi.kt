package com.erygra.maskoflight.network.api

import com.erygra.maskoflight.network.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * LeaderboardApi.kt - واجهة API للوحة المتصدرين
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة جميع endpoints الخاصة بلوحة المتصدرين
 * - دعم أنواع مختلفة من اللوحات (سرعة، نقاط، إنجاز)
 * - pagination و filtering
 * 
 * المكونات الرئيسية:
 * - Global leaderboards
 * - Friends leaderboards
 * - Score submission
 * - Ranking queries
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

interface LeaderboardApi {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Global Leaderboard Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على لوحة المتصدرين العالمية
     *
     * @param token رمز المصادقة
     * @param type نوع اللوحة
     * @param page رقم الصفحة
     * @param pageSize عدد العناصر في الصفحة
     * @param timeRange الفترة الزمنية (daily, weekly, monthly, all-time)
     * @return لوحة المتصدرين مع pagination
     */
    @GET("leaderboard/global")
    suspend fun getGlobalLeaderboard(
        @Header("Authorization") token: String,
        @Query("type") type: LeaderboardType,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("time_range") timeRange: String = "all-time"
    ): Response<ApiResponse<PaginatedResponse<LeaderboardEntry>>>
    
    /**
     * الحصول على Top N من المتصدرين
     *
     * @param token رمز المصادقة
     * @param type نوع اللوحة
     * @param limit عدد المتصدرين
     * @param timeRange الفترة الزمنية
     * @return قائمة المتصدرين
     */
    @GET("leaderboard/top")
    suspend fun getTopPlayers(
        @Header("Authorization") token: String,
        @Query("type") type: LeaderboardType,
        @Query("limit") limit: Int = 10,
        @Query("time_range") timeRange: String = "all-time"
    ): Response<ApiResponse<LeaderboardResponse>>
    
    /**
     * الحصول على ترتيب اللاعب الحالي
     *
     * @param token رمز المصادقة
     * @param type نوع اللوحة
     * @param timeRange الفترة الزمنية
     * @return معلومات ترتيب اللاعب
     */
    @GET("leaderboard/my-rank")
    suspend fun getMyRank(
        @Header("Authorization") token: String,
        @Query("type") type: LeaderboardType,
        @Query("time_range") timeRange: String = "all-time"
    ): Response<ApiResponse<LeaderboardEntry>>
    
    /**
     * الحصول على اللاعبين المحيطين بترتيب اللاعب الحالي
     *
     * @param token رمز المصادقة
     * @param type نوع اللوحة
     * @param range عدد اللاعبين أعلى وأسفل
     * @param timeRange الفترة الزمنية
     * @return قائمة اللاعبين المحيطين
     */
    @GET("leaderboard/nearby")
    suspend fun getNearbyPlayers(
        @Header("Authorization") token: String,
        @Query("type") type: LeaderboardType,
        @Query("range") range: Int = 5,
        @Query("time_range") timeRange: String = "all-time"
    ): Response<ApiResponse<LeaderboardResponse>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Friends Leaderboard Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على لوحة متصدري الأصدقاء
     *
     * @param token رمز المصادقة
     * @param type نوع اللوحة
     * @param timeRange الفترة الزمنية
     * @return لوحة متصدري الأصدقاء
     */
    @GET("leaderboard/friends")
    suspend fun getFriendsLeaderboard(
        @Header("Authorization") token: String,
        @Query("type") type: LeaderboardType,
        @Query("time_range") timeRange: String = "all-time"
    ): Response<ApiResponse<LeaderboardResponse>>
    
    /**
     * مقارنة النتيجة مع صديق محدد
     *
     * @param token رمز المصادقة
     * @param friendId معرف الصديق
     * @param type نوع اللوحة
     * @return مقارنة النتائج
     */
    @GET("leaderboard/compare/{friendId}")
    suspend fun compareWithFriend(
        @Header("Authorization") token: String,
        @Path("friendId") friendId: String,
        @Query("type") type: LeaderboardType
    ): Response<ApiResponse<Map<String, LeaderboardEntry>>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Score Submission Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * رفع نتيجة جديدة
     *
     * @param token رمز المصادقة
     * @param request بيانات النتيجة
     * @return النتيجة المرفوعة مع الترتيب الجديد
     */
    @POST("leaderboard/submit")
    suspend fun submitScore(
        @Header("Authorization") token: String,
        @Body request: SubmitScoreRequest
    ): Response<ApiResponse<LeaderboardEntry>>
    
    /**
     * رفع عدة نتائج دفعة واحدة
     *
     * @param token رمز المصادقة
     * @param scores قائمة النتائج
     * @return النتائج المرفوعة
     */
    @POST("leaderboard/submit-batch")
    suspend fun submitScoreBatch(
        @Header("Authorization") token: String,
        @Body scores: List<SubmitScoreRequest>
    ): Response<ApiResponse<List<LeaderboardEntry>>>
    
    /**
     * التحقق من صحة النتيجة قبل الرفع
     *
     * @param token رمز المصادقة
     * @param request بيانات النتيجة
     * @return نتيجة التحقق
     */
    @POST("leaderboard/validate")
    suspend fun validateScore(
        @Header("Authorization") token: String,
        @Body request: SubmitScoreRequest
    ): Response<ApiResponse<Map<String, Any>>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Category-Specific Leaderboards
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * لوحة متصدري السرعة (Speedrun)
     *
     * @param token رمز المصادقة
     * @param category الفئة (any%, 100%, boss-rush, etc.)
     * @param page رقم الصفحة
     * @param pageSize عدد العناصر
     * @return لوحة المتصدرين
     */
    @GET("leaderboard/speedrun")
    suspend fun getSpeedrunLeaderboard(
        @Header("Authorization") token: String,
        @Query("category") category: String = "any-percent",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): Response<ApiResponse<PaginatedResponse<LeaderboardEntry>>>
    
    /**
     * لوحة متصدري النقاط
     *
     * @param token رمز المصادقة
     * @param mode وضع اللعب
     * @param page رقم الصفحة
     * @param pageSize عدد العناصر
     * @return لوحة المتصدرين
     */
    @GET("leaderboard/score")
    suspend fun getScoreLeaderboard(
        @Header("Authorization") token: String,
        @Query("mode") mode: String = "normal",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): Response<ApiResponse<PaginatedResponse<LeaderboardEntry>>>
    
    /**
     * لوحة متصدري الإنجاز
     *
     * @param token رمز المصادقة
     * @param page رقم الصفحة
     * @param pageSize عدد العناصر
     * @return لوحة المتصدرين
     */
    @GET("leaderboard/completion")
    suspend fun getCompletionLeaderboard(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): Response<ApiResponse<PaginatedResponse<LeaderboardEntry>>>
    
    /**
     * لوحة متصدري وقت الزعماء
     *
     * @param token رمز المصادقة
     * @param bossId معرف الزعيم
     * @param page رقم الصفحة
     * @param pageSize عدد العناصر
     * @return لوحة المتصدرين
     */
    @GET("leaderboard/boss-time/{bossId}")
    suspend fun getBossTimeLeaderboard(
        @Header("Authorization") token: String,
        @Path("bossId") bossId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): Response<ApiResponse<PaginatedResponse<LeaderboardEntry>>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Leaderboard Management Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على تاريخ نتائج اللاعب
     *
     * @param token رمز المصادقة
     * @param type نوع اللوحة
     * @param limit عدد السجلات
     * @return تاريخ النتائج
     */
    @GET("leaderboard/history")
    suspend fun getScoreHistory(
        @Header("Authorization") token: String,
        @Query("type") type: LeaderboardType,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<LeaderboardEntry>>>
    
    /**
     * حذف نتيجة (للمشرفين فقط)
     *
     * @param token رمز المصادقة
     * @param entryId معرف السجل
     * @return استجابة نجاح/فشل
     */
    @DELETE("leaderboard/entry/{entryId}")
    suspend fun deleteEntry(
        @Header("Authorization") token: String,
        @Path("entryId") entryId: String
    ): Response<ApiResponse<Unit>>
    
    /**
     * الإبلاغ عن نتيجة مشبوهة
     *
     * @param token رمز المصادقة
     * @param entryId معرف السجل
     * @param reason سبب الإبلاغ
     * @return استجابة نجاح/فشل
     */
    @POST("leaderboard/report/{entryId}")
    suspend fun reportEntry(
        @Header("Authorization") token: String,
        @Path("entryId") entryId: String,
        @Body reason: Map<String, String>
    ): Response<ApiResponse<Unit>>
    
    /**
     * الحصول على إحصائيات لوحة المتصدرين
     *
     * @param token رمز المصادقة
     * @param type نوع اللوحة
     * @return إحصائيات عامة
     */
    @GET("leaderboard/stats")
    suspend fun getLeaderboardStats(
        @Header("Authorization") token: String,
        @Query("type") type: LeaderboardType
    ): Response<ApiResponse<Map<String, Any>>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Regional Leaderboards
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على لوحة متصدري منطقة محددة
     *
     * @param token رمز المصادقة
     * @param region المنطقة (ISO country code)
     * @param type نوع اللوحة
     * @param page رقم الصفحة
     * @param pageSize عدد العناصر
     * @return لوحة المتصدرين الإقليمية
     */
    @GET("leaderboard/regional/{region}")
    suspend fun getRegionalLeaderboard(
        @Header("Authorization") token: String,
        @Path("region") region: String,
        @Query("type") type: LeaderboardType,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): Response<ApiResponse<PaginatedResponse<LeaderboardEntry>>>
    
    /**
     * الحصول على ترتيب اللاعب في منطقته
     *
     * @param token رمز المصادقة
     * @param type نوع اللوحة
     * @return ترتيب اللاعب الإقليمي
     */
    @GET("leaderboard/regional-rank")
    suspend fun getRegionalRank(
        @Header("Authorization") token: String,
        @Query("type") type: LeaderboardType
    ): Response<ApiResponse<LeaderboardEntry>>
    
    companion object {
        /**
         * عدد العناصر الافتراضي في الصفحة
         */
        const val DEFAULT_PAGE_SIZE = 50
        
        /**
         * الحد الأقصى لعدد العناصر في الصفحة
         */
        const val MAX_PAGE_SIZE = 100
        
        /**
         * الحد الأقصى للاعبين المحيطين
         */
        const val MAX_NEARBY_RANGE = 10
    }
}