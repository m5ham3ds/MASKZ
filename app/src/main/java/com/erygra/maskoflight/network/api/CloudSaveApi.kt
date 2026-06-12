package com.erygra.maskoflight.network.api

import com.erygra.maskoflight.network.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * CloudSaveApi.kt - واجهة API للحفظ السحابي
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة جميع endpoints الخاصة بالحفظ السحابي
 * - رفع وتحميل بيانات الحفظ
 * - مزامنة الحفوظات بين الأجهزة
 * - إدارة خانات الحفظ المتعددة
 * 
 * المكونات الرئيسية:
 * - Upload/Download saves
 * - Save synchronization
 * - Conflict resolution
 * - Save slots management
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

interface CloudSaveApi {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save Upload/Download Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * رفع بيانات حفظ إلى السحابة
     *
     * @param token رمز المصادقة
     * @param request بيانات الحفظ
     * @return بيانات الحفظ المرفوعة
     */
    @POST("cloud-save/upload")
    suspend fun uploadSave(
        @Header("Authorization") token: String,
        @Body request: UploadSaveRequest
    ): Response<ApiResponse<CloudSaveData>>
    
    /**
     * تحميل بيانات حفظ من السحابة
     *
     * @param token رمز المصادقة
     * @param slotId رقم خانة الحفظ
     * @return بيانات الحفظ
     */
    @GET("cloud-save/download/{slotId}")
    suspend fun downloadSave(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int
    ): Response<ApiResponse<CloudSaveData>>
    
    /**
     * تحميل أحدث حفظ
     *
     * @param token رمز المصادقة
     * @return أحدث بيانات حفظ
     */
    @GET("cloud-save/latest")
    suspend fun downloadLatestSave(
        @Header("Authorization") token: String
    ): Response<ApiResponse<CloudSaveData>>
    
    /**
     * الحصول على جميع الحفوظات السحابية
     *
     * @param token رمز المصادقة
     * @return قائمة جميع الحفوظات
     */
    @GET("cloud-save/list")
    suspend fun listAllSaves(
        @Header("Authorization") token: String
    ): Response<ApiResponse<CloudSavesList>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save Synchronization Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * مزامنة حفظ محدد مع السحابة
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @param localSave بيانات الحفظ المحلية
     * @param lastSyncTime آخر وقت مزامنة
     * @return بيانات الحفظ المحدثة
     */
    @POST("cloud-save/sync/{slotId}")
    suspend fun syncSave(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int,
        @Body localSave: UploadSaveRequest,
        @Query("last_sync") lastSyncTime: Long
    ): Response<ApiResponse<CloudSaveData>>
    
    /**
     * مزامنة جميع الحفوظات
     *
     * @param token رمز المصادقة
     * @param localSaves الحفوظات المحلية
     * @param lastSyncTime آخر وقت مزامنة
     * @return الحفوظات المحدثة
     */
    @POST("cloud-save/sync-all")
    suspend fun syncAllSaves(
        @Header("Authorization") token: String,
        @Body localSaves: List<UploadSaveRequest>,
        @Query("last_sync") lastSyncTime: Long
    ): Response<ApiResponse<CloudSavesList>>
    
    /**
     * التحقق من وجود تحديثات في السحابة
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @param localTimestamp التوقيت المحلي
     * @return معلومات التحديث
     */
    @GET("cloud-save/check-update/{slotId}")
    suspend fun checkForUpdates(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int,
        @Query("local_timestamp") localTimestamp: Long
    ): Response<ApiResponse<Map<String, Any>>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Conflict Resolution Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على معلومات التعارض
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @return معلومات التعارض إن وجدت
     */
    @GET("cloud-save/conflict/{slotId}")
    suspend fun getConflictInfo(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int
    ): Response<ApiResponse<Map<String, CloudSaveData>>>
    
    /**
     * حل التعارض باختيار نسخة
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @param choice الاختيار (local/cloud)
     * @return بيانات الحفظ المختارة
     */
    @POST("cloud-save/resolve-conflict/{slotId}")
    suspend fun resolveConflict(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int,
        @Body choice: Map<String, String>
    ): Response<ApiResponse<CloudSaveData>>
    
    /**
     * دمج التعارضات
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @param mergedData البيانات المدمجة
     * @return بيانات الحفظ المدمجة
     */
    @POST("cloud-save/merge/{slotId}")
    suspend fun mergeSaves(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int,
        @Body mergedData: UploadSaveRequest
    ): Response<ApiResponse<CloudSaveData>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save Slot Management Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حذف حفظ من السحابة
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @return استجابة نجاح/فشل
     */
    @DELETE("cloud-save/delete/{slotId}")
    suspend fun deleteSave(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int
    ): Response<ApiResponse<Unit>>
    
    /**
     * حذف جميع الحفوظات
     *
     * @param token رمز المصادقة
     * @param confirmation كلمة التأكيد
     * @return استجابة نجاح/فشل
     */
    @HTTP(method = "DELETE", path = "cloud-save/delete-all", hasBody = true)
    suspend fun deleteAllSaves(
        @Header("Authorization") token: String,
        @Body confirmation: Map<String, String>
    ): Response<ApiResponse<Unit>>
    
    /**
     * نسخ حفظ إلى خانة أخرى
     *
     * @param token رمز المصادقة
     * @param sourceSlot الخانة المصدر
     * @param targetSlot الخانة الهدف
     * @return بيانات الحفظ المنسوخة
     */
    @POST("cloud-save/copy")
    suspend fun copySave(
        @Header("Authorization") token: String,
        @Query("source") sourceSlot: Int,
        @Query("target") targetSlot: Int
    ): Response<ApiResponse<CloudSaveData>>
    
    /**
     * إعادة تسمية حفظ
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @param newName الاسم الجديد
     * @return بيانات الحفظ المحدثة
     */
    @PATCH("cloud-save/rename/{slotId}")
    suspend fun renameSave(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int,
        @Body newName: Map<String, String>
    ): Response<ApiResponse<CloudSaveData>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save Backup Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إنشاء نسخة احتياطية من حفظ
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @return معلومات النسخة الاحتياطية
     */
    @POST("cloud-save/backup/{slotId}")
    suspend fun createBackup(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int
    ): Response<ApiResponse<Map<String, Any>>>
    
    /**
     * الحصول على قائمة النسخ الاحتياطية
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @return قائمة النسخ الاحتياطية
     */
    @GET("cloud-save/backups/{slotId}")
    suspend fun listBackups(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int
    ): Response<ApiResponse<List<Map<String, Any>>>>
    
    /**
     * استعادة من نسخة احتياطية
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @param backupId معرف النسخة الاحتياطية
     * @return بيانات الحفظ المستعادة
     */
    @POST("cloud-save/restore/{slotId}")
    suspend fun restoreBackup(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int,
        @Body backupId: Map<String, String>
    ): Response<ApiResponse<CloudSaveData>>
    
    /**
     * حذف نسخة احتياطية
     *
     * @param token رمز المصادقة
     * @param backupId معرف النسخة الاحتياطية
     * @return استجابة نجاح/فشل
     */
    @DELETE("cloud-save/backup/{backupId}")
    suspend fun deleteBackup(
        @Header("Authorization") token: String,
        @Path("backupId") backupId: String
    ): Response<ApiResponse<Unit>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save Validation Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من صحة بيانات الحفظ
     *
     * @param token رمز المصادقة
     * @param saveData بيانات الحفظ
     * @param checksum الـ checksum
     * @return نتيجة التحقق
     */
    @POST("cloud-save/validate")
    suspend fun validateSaveData(
        @Header("Authorization") token: String,
        @Body saveData: Map<String, String>
    ): Response<ApiResponse<Map<String, Any>>>
    
    /**
     * إصلاح بيانات حفظ تالفة
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @return بيانات الحفظ المصلحة
     */
    @POST("cloud-save/repair/{slotId}")
    suspend fun repairSave(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int
    ): Response<ApiResponse<CloudSaveData>>
    
    // ════════════════════════════════════════════════════════════════════════════
    // Save Metadata Endpoints
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على metadata لحفظ محدد
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @return metadata الحفظ
     */
    @GET("cloud-save/metadata/{slotId}")
    suspend fun getSaveMetadata(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int
    ): Response<ApiResponse<Map<String, Any>>>
    
    /**
     * تحديث metadata لحفظ
     *
     * @param token رمز المصادقة
     * @param slotId رقم الخانة
     * @param metadata البيانات الوصفية الجديدة
     * @return metadata المحدثة
     */
    @PATCH("cloud-save/metadata/{slotId}")
    suspend fun updateSaveMetadata(
        @Header("Authorization") token: String,
        @Path("slotId") slotId: Int,
        @Body metadata: Map<String, Any>
    ): Response<ApiResponse<Map<String, Any>>>
    
    /**
     * الحصول على معلومات التخزين
     *
     * @param token رمز المصادقة
     * @return معلومات التخزين (المساحة المستخدمة، المتاحة، إلخ)
     */
    @GET("cloud-save/storage-info")
    suspend fun getStorageInfo(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    companion object {
        /**
         * الحد الأقصى لحجم بيانات الحفظ (بالبايت)
         */
        const val MAX_SAVE_SIZE = 10 * 1024 * 1024 // 10 MB
        
        /**
         * عدد خانات الحفظ المتاحة
         */
        const val MAX_SAVE_SLOTS = 3
        
        /**
         * عدد النسخ الاحتياطية لكل خانة
         */
        const val MAX_BACKUPS_PER_SLOT = 5
        
        /**
         * مدة الاحتفاظ بالنسخ الاحتياطية (أيام)
         */
        const val BACKUP_RETENTION_DAYS = 30
    }
}