package com.erygra.maskoflight.network.api

import com.erygra.maskoflight.network.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * GameApiService.kt - واجهة API الرئيسية للعبة
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - يحتوي على جميع endpoints الرئيسية للعبة
 * - المصادقة، الحفظ السحابي، الإحصائيات
 * - إدارة المستخدمين والإعدادات
 * 
 * المكونات الرئيسية:
 * - Authentication endpoints
 * - User management endpoints
 * - Settings sync endpoints
 * - Stats and achievements endpoints
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

interface GameApiService {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Authentication Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل دخول المستخدم
     *
     * @param request بيانات تسجيل الدخول
     * @return استجابة تحتوي على token وبيانات المستخدم
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>
    
    /**
     * تسجيل مستخدم جديد
     *
     * @param email البريد الإلكتروني
     * @param password كلمة المرور
     * @param username اسم المستخدم
     * @param displayName الاسم المعروض
     * @param deviceId معرف الجهاز
     * @return استجابة تحتوي على بيانات المستخدم الجديد
     */
    @FormUrlEncoded
    @POST("auth/register")
    suspend fun register(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("username") username: String,
        @Field("display_name") displayName: String,
        @Field("device_id") deviceId: String
    ): Response<ApiResponse<LoginResponse>>
    
    /**
     * تسجيل الخروج
     *
     * @param token رمز المصادقة
     * @return استجابة نجاح/فشل
     */
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Unit>>
    
    /**
     * تحديث رمز المصادقة
     *
     * @param refreshToken رمز التحديث
     * @return استجابة تحتوي على رمز جديد
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body refreshToken: Map<String, String>
    ): Response<ApiResponse<LoginResponse>>
    
    /**
     * التحقق من صلاحية الرمز
     *
     * @param token رمز المصادقة
     * @return استجابة تحتوي على حالة الرمز
     */
    @GET("auth/verify")
    suspend fun verifyToken(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Boolean>>
    
    /**
     * طلب إعادة تعيين كلمة المرور
     *
     * @param email البريد الإلكتروني
     * @return استجابة نجاح/فشل
     */
    @FormUrlEncoded
    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Field("email") email: String
    ): Response<ApiResponse<Unit>>
    
    /**
     * إعادة تعيين كلمة المرور
     *
     * @param token رمز إعادة التعيين
     * @param newPassword كلمة المرور الجديدة
     * @return استجابة نجاح/فشل
     */
    @FormUrlEncoded
    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Field("token") token: String,
        @Field("new_password") newPassword: String
    ): Response<ApiResponse<Unit>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // User Management Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على بيانات المستخدم الحالي
     *
     * @param token رمز المصادقة
     * @return بيانات المستخدم
     */
    @GET("user/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<ApiUser>>
    
    /**
     * تحديث بيانات المستخدم
     *
     * @param token رمز المصادقة
     * @param updates البيانات المراد تحديثها
     * @return بيانات المستخدم المحدثة
     */
    @PATCH("user/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Body updates: Map<String, Any>
    ): Response<ApiResponse<ApiUser>>
    
    /**
     * رفع صورة شخصية
     *
     * @param token رمز المصادقة
     * @param avatarUrl رابط الصورة
     * @return بيانات المستخدم المحدثة
     */
    @FormUrlEncoded
    @POST("user/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Field("avatar_url") avatarUrl: String
    ): Response<ApiResponse<ApiUser>>
    
    /**
     * حذف الحساب
     *
     * @param token رمز المصادقة
     * @param password كلمة المرور للتأكيد
     * @return استجابة نجاح/فشل
     */
    @HTTP(method = "DELETE", path = "user/account", hasBody = true)
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Body password: Map<String, String>
    ): Response<ApiResponse<Unit>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Stats and Achievements Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إحصائيات اللاعب
     *
     * @param token رمز المصادقة
     * @return إحصائيات اللاعب
     */
    @GET("stats")
    suspend fun getPlayerStats(
        @Header("Authorization") token: String
    ): Response<ApiResponse<PlayerStats>>
    
    /**
     * تحديث إحصائيات اللاعب
     *
     * @param token رمز المصادقة
     * @param stats الإحصائيات الجديدة
     * @return الإحصائيات المحدثة
     */
    @POST("stats")
    suspend fun updatePlayerStats(
        @Header("Authorization") token: String,
        @Body stats: PlayerStats
    ): Response<ApiResponse<PlayerStats>>
    
    /**
     * الحصول على قائمة الإنجازات
     *
     * @param token رمز المصادقة
     * @return قائمة الإنجازات
     */
    @GET("achievements")
    suspend fun getAchievements(
        @Header("Authorization") token: String
    ): Response<ApiResponse<AchievementsList>>
    
    /**
     * فتح إنجاز
     *
     * @param token رمز المصادقة
     * @param request بيانات الإنجاز
     * @return الإنجاز المفتوح
     */
    @POST("achievements/unlock")
    suspend fun unlockAchievement(
        @Header("Authorization") token: String,
        @Body request: UnlockAchievementRequest
    ): Response<ApiResponse<Achievement>>
    
    /**
     * تحديث تقدم إنجاز
     *
     * @param token رمز المصادقة
     * @param achievementId معرف الإنجاز
     * @param progress التقدم (0-100)
     * @return الإنجاز المحدث
     */
    @PATCH("achievements/{achievementId}/progress")
    suspend fun updateAchievementProgress(
        @Header("Authorization") token: String,
        @Path("achievementId") achievementId: String,
        @Body progress: Map<String, Float>
    ): Response<ApiResponse<Achievement>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Settings Sync Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على الإعدادات السحابية
     *
     * @param token رمز المصادقة
     * @return الإعدادات السحابية
     */
    @GET("settings")
    suspend fun getCloudSettings(
        @Header("Authorization") token: String
    ): Response<ApiResponse<CloudSettings>>
    
    /**
     * تحديث الإعدادات السحابية
     *
     * @param token رمز المصادقة
     * @param settings الإعدادات الجديدة
     * @return الإعدادات المحدثة
     */
    @PUT("settings")
    suspend fun updateCloudSettings(
        @Header("Authorization") token: String,
        @Body settings: CloudSettings
    ): Response<ApiResponse<CloudSettings>>
    
    /**
     * مزامنة الإعدادات
     *
     * @param token رمز المصادقة
     * @param localSettings الإعدادات المحلية
     * @param lastSyncTime آخر وقت مزامنة
     * @return الإعدادات المحدثة
     */
    @POST("settings/sync")
    suspend fun syncSettings(
        @Header("Authorization") token: String,
        @Body localSettings: CloudSettings,
        @Query("last_sync") lastSyncTime: Long
    ): Response<ApiResponse<CloudSettings>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Analytics Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إرسال دفعة من الأحداث التحليلية
     *
     * @param token رمز المصادقة
     * @param batch دفعة الأحداث
     * @return استجابة نجاح/فشل
     */
    @POST("analytics/events")
    suspend fun sendAnalyticsBatch(
        @Header("Authorization") token: String,
        @Body batch: AnalyticsBatch
    ): Response<ApiResponse<Unit>>
    
    /**
     * إرسال حدث واحد
     *
     * @param token رمز المصادقة
     * @param event الحدث
     * @return استجابة نجاح/فشل
     */
    @POST("analytics/event")
    suspend fun sendAnalyticsEvent(
        @Header("Authorization") token: String,
        @Body event: AnalyticsEvent
    ): Response<ApiResponse<Unit>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Game Data Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على إصدار بيانات اللعبة
     *
     * @return رقم الإصدار
     */
    @GET("game/version")
    suspend fun getGameDataVersion(): Response<ApiResponse<Int>>
    
    /**
     * التحقق من وجود تحديثات
     *
     * @param currentVersion الإصدار الحالي
     * @return معلومات التحديث
     */
    @GET("game/check-update")
    suspend fun checkForUpdates(
        @Query("current_version") currentVersion: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    /**
     * الحصول على بيانات اللعبة (items, enemies, quests, etc.)
     *
     * @param dataType نوع البيانات
     * @param version الإصدار المطلوب
     * @return البيانات المطلوبة
     */
    @GET("game/data/{dataType}")
    suspend fun getGameData(
        @Path("dataType") dataType: String,
        @Query("version") version: Int? = null
    ): Response<ApiResponse<Map<String, Any>>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Health Check Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * فحص صحة الخادم
     *
     * @return حالة الخادم
     */
    @GET("health")
    suspend fun healthCheck(): Response<ApiResponse<Map<String, Any>>>
    
    /**
     * الحصول على حالة الخدمات
     *
     * @return حالة جميع الخدمات
     */
    @GET("status")
    suspend fun getServiceStatus(): Response<ApiResponse<Map<String, Any>>>
    
    companion object {
        /**
         * Base URL للـ API
         * يجب تغييره للإنتاج
         */
        const val BASE_URL = "https://api.erygra.com/v1/"
        
        /**
         * Base URL للتطوير
         */
        const val DEV_BASE_URL = "https://dev-api.erygra.com/v1/"
        
        /**
         * Timeout للاتصال (ثواني)
         */
        const val CONNECT_TIMEOUT = 30L
        
        /**
         * Timeout للقراءة (ثواني)
         */
        const val READ_TIMEOUT = 30L
        
        /**
         * Timeout للكتابة (ثواني)
         */
        const val WRITE_TIMEOUT = 30L
    }
}