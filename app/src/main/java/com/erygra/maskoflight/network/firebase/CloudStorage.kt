package com.erygra.maskoflight.network.firebase

import android.content.Context
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.InputStream

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * CloudStorage.kt - مدير Firebase Storage
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة رفع وتحميل الملفات من Firebase Storage
 * - دعم الصور، الصوتيات، بيانات الحفظ
 * - إدارة metadata والتخزين
 * 
 * المكونات الرئيسية:
 * - Upload operations
 * - Download operations
 * - File management
 * - Progress tracking
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class CloudStorage(
    private val context: Context
) {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Firebase Storage instance
     */
    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }
    
    /**
     * Storage reference الجذر
     */
    private val storageRef: StorageReference by lazy {
        storage.reference
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Upload Methods
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * رفع ملف من URI
     *
     * @param fileUri URI الملف
     * @param path المسار في Storage
     * @param metadata البيانات الوصفية
     * @param onProgress callback للتقدم
     * @return رابط التحميل
     */
    suspend fun uploadFile(
        fileUri: Uri,
        path: String,
        metadata: StorageMetadata? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> {
        return try {
            val fileRef = storageRef.child(path)
            val uploadTask = if (metadata != null) {
                fileRef.putFile(fileUri, metadata)
            } else {
                fileRef.putFile(fileUri)
            }
            
            // Track progress
            onProgress?.let { callback ->
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat()
                    callback(progress)
                }
            }
            
            uploadTask.await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            
            Timber.d("File uploaded: $path")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Timber.e(e, "Error uploading file")
            Result.failure(e)
        }
    }
    
    /**
     * رفع ملف من byte array
     *
     * @param data البيانات
     * @param path المسار في Storage
     * @param metadata البيانات الوصفية
     * @param onProgress callback للتقدم
     * @return رابط التحميل
     */
    suspend fun uploadBytes(
        data: ByteArray,
        path: String,
        metadata: StorageMetadata? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> {
        return try {
            val fileRef = storageRef.child(path)
            val uploadTask = if (metadata != null) {
                fileRef.putBytes(data, metadata)
            } else {
                fileRef.putBytes(data)
            }
            
            // Track progress
            onProgress?.let { callback ->
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat()
                    callback(progress)
                }
            }
            
            uploadTask.await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            
            Timber.d("Bytes uploaded: $path")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Timber.e(e, "Error uploading bytes")
            Result.failure(e)
        }
    }
    
    /**
     * رفع ملف من InputStream
     *
     * @param stream الـ stream
     * @param path المسار في Storage
     * @param metadata البيانات الوصفية
     * @param onProgress callback للتقدم
     * @return رابط التحميل
     */
    suspend fun uploadStream(
        stream: InputStream,
        path: String,
        metadata: StorageMetadata? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> {
        return try {
            val fileRef = storageRef.child(path)
            val uploadTask = if (metadata != null) {
                fileRef.putStream(stream, metadata)
            } else {
                fileRef.putStream(stream)
            }
            
            // Track progress
            onProgress?.let { callback ->
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat()
                    callback(progress)
                }
            }
            
            uploadTask.await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            
            Timber.d("Stream uploaded: $path")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Timber.e(e, "Error uploading stream")
            Result.failure(e)
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Download Methods
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تحميل ملف إلى File محلي
     *
     * @param path المسار في Storage
     * @param destinationFile الملف المستهدف
     * @param onProgress callback للتقدم
     * @return File المحمل
     */
    suspend fun downloadFile(
        path: String,
        destinationFile: File,
        onProgress: ((Float) -> Unit)? = null
    ): Result<File> {
        return try {
            val fileRef = storageRef.child(path)
            val downloadTask = fileRef.getFile(destinationFile)
            
            // Track progress
            onProgress?.let { callback ->
                downloadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat()
                    callback(progress)
                }
            }
            
            downloadTask.await()
            
            Timber.d("File downloaded: $path")
            Result.success(destinationFile)
        } catch (e: Exception) {
            Timber.e(e, "Error downloading file")
            Result.failure(e)
        }
    }
    
    /**
     * تحميل ملف كـ ByteArray
     *
     * @param path المسار في Storage
     * @param maxSize الحجم الأقصى (بايت)
     * @return البيانات
     */
    suspend fun downloadBytes(
        path: String,
        maxSize: Long = MAX_DOWNLOAD_SIZE
    ): Result<ByteArray> {
        return try {
            val fileRef = storageRef.child(path)
            val bytes = fileRef.getBytes(maxSize).await()
            
            Timber.d("Bytes downloaded: $path (${bytes.size} bytes)")
            Result.success(bytes)
        } catch (e: Exception) {
            Timber.e(e, "Error downloading bytes")
            Result.failure(e)
        }
    }
    
    /**
     * الحصول على رابط التحميل
     *
     * @param path المسار في Storage
     * @return رابط التحميل
     */
    suspend fun getDownloadUrl(path: String): Result<String> {
        return try {
            val fileRef = storageRef.child(path)
            val url = fileRef.downloadUrl.await().toString()
            
            Timber.d("Download URL retrieved: $path")
            Result.success(url)
        } catch (e: Exception) {
            Timber.e(e, "Error getting download URL")
            Result.failure(e)
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // File Management Methods
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حذف ملف من Storage
     *
     * @param path المسار في Storage
     */
    suspend fun deleteFile(path: String): Result<Unit> {
        return try {
            val fileRef = storageRef.child(path)
            fileRef.delete().await()
            
            Timber.d("File deleted: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting file")
            Result.failure(e)
        }
    }
    
    /**
     * الحصول على metadata ملف
     *
     * @param path المسار في Storage
     * @return metadata الملف
     */
    suspend fun getMetadata(path: String): Result<StorageMetadata> {
        return try {
            val fileRef = storageRef.child(path)
            val metadata = fileRef.metadata.await()
            
            Timber.d("Metadata retrieved: $path")
            Result.success(metadata)
        } catch (e: Exception) {
            Timber.e(e, "Error getting metadata")
            Result.failure(e)
        }
    }
    
    /**
     * تحديث metadata ملف
     *
     * @param path المسار في Storage
     * @param metadata البيانات الوصفية الجديدة
     */
    suspend fun updateMetadata(
        path: String,
        metadata: StorageMetadata
    ): Result<StorageMetadata> {
        return try {
            val fileRef = storageRef.child(path)
            val updatedMetadata = fileRef.updateMetadata(metadata).await()
            
            Timber.d("Metadata updated: $path")
            Result.success(updatedMetadata)
        } catch (e: Exception) {
            Timber.e(e, "Error updating metadata")
            Result.failure(e)
        }
    }
    
    /**
     * سرد الملفات في مجلد
     *
     * @param path المسار في Storage
     * @param maxResults الحد الأقصى للنتائج
     * @return قائمة المراجع
     */
    suspend fun listFiles(
        path: String,
        maxResults: Long = 100
    ): Result<List<StorageReference>> {
        return try {
            val folderRef = storageRef.child(path)
            val listResult = folderRef.listAll().await()
            
            Timber.d("Files listed: $path (${listResult.items.size} items)")
            Result.success(listResult.items)
        } catch (e: Exception) {
            Timber.e(e, "Error listing files")
            Result.failure(e)
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Specialized Upload Methods
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * رفع صورة ملف تعريف
     *
     * @param userId معرف المستخدم
     * @param imageUri URI الصورة
     * @return رابط التحميل
     */
    suspend fun uploadProfileImage(
        userId: String,
        imageUri: Uri
    ): Result<String> {
        val path = "$PATH_PROFILE_IMAGES/$userId.jpg"
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("userId", userId)
            .build()
        
        return uploadFile(imageUri, path, metadata)
    }
    
    /**
     * رفع بيانات حفظ
     *
     * @param userId معرف المستخدم
     * @param slotId رقم الخانة
     * @param saveData البيانات
     * @return رابط التحميل
     */
    suspend fun uploadSaveData(
        userId: String,
        slotId: Int,
        saveData: ByteArray
    ): Result<String> {
        val path = "$PATH_SAVE_DATA/$userId/slot_$slotId.dat"
        val metadata = StorageMetadata.Builder()
            .setContentType("application/octet-stream")
            .setCustomMetadata("userId", userId)
            .setCustomMetadata("slotId", slotId.toString())
            .build()
        
        return uploadBytes(saveData, path, metadata)
    }
    
    /**
     * تحميل بيانات حفظ
     *
     * @param userId معرف المستخدم
     * @param slotId رقم الخانة
     * @return البيانات
     */
    suspend fun downloadSaveData(
        userId: String,
        slotId: Int
    ): Result<ByteArray> {
        val path = "$PATH_SAVE_DATA/$userId/slot_$slotId.dat"
        return downloadBytes(path)
    }
    
    /**
     * حذف بيانات حفظ
     *
     * @param userId معرف المستخدم
     * @param slotId رقم الخانة
     */
    suspend fun deleteSaveData(
        userId: String,
        slotId: Int
    ): Result<Unit> {
        val path = "$PATH_SAVE_DATA/$userId/slot_$slotId.dat"
        return deleteFile(path)
    }
    
    /**
     * رفع لقطة شاشة
     *
     * @param userId معرف المستخدم
     * @param imageUri URI الصورة
     * @return رابط التحميل
     */
    suspend fun uploadScreenshot(
        userId: String,
        imageUri: Uri
    ): Result<String> {
        val timestamp = System.currentTimeMillis()
        val path = "$PATH_SCREENSHOTS/$userId/$timestamp.jpg"
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("userId", userId)
            .setCustomMetadata("timestamp", timestamp.toString())
            .build()
        
        return uploadFile(imageUri, path, metadata)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إنشاء metadata مخصص
     *
     * @param contentType نوع المحتوى
     * @param customMetadata البيانات المخصصة
     * @return StorageMetadata
     */
    fun createMetadata(
        contentType: String,
        customMetadata: Map<String, String> = emptyMap()
    ): StorageMetadata {
        val builder = StorageMetadata.Builder().setContentType(contentType)
        customMetadata.forEach { (key, value) ->
            builder.setCustomMetadata(key, value)
        }
        return builder.build()
    }
    
    /**
     * الحصول على حجم ملف
     *
     * @param path المسار في Storage
     * @return الحجم بالبايت
     */
    suspend fun getFileSize(path: String): Result<Long> {
        return try {
            val metadata = getMetadata(path).getOrThrow()
            Result.success(metadata.sizeBytes)
        } catch (e: Exception) {
            Timber.e(e, "Error getting file size")
            Result.failure(e)
        }
    }
    
    /**
     * التحقق من وجود ملف
     *
     * @param path المسار في Storage
     * @return true إذا كان الملف موجود
     */
    suspend fun fileExists(path: String): Boolean {
        return try {
            getMetadata(path).isSuccess
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        // Storage paths
        const val PATH_PROFILE_IMAGES = "profile_images"
        const val PATH_SAVE_DATA = "save_data"
        const val PATH_SCREENSHOTS = "screenshots"
        const val PATH_REPLAYS = "replays"
        
        // Size limits
        const val MAX_DOWNLOAD_SIZE = 10L * 1024 * 1024 // 10 MB
        const val MAX_PROFILE_IMAGE_SIZE = 5L * 1024 * 1024 // 5 MB
        const val MAX_SAVE_DATA_SIZE = 50L * 1024 * 1024 // 50 MB
    }
}