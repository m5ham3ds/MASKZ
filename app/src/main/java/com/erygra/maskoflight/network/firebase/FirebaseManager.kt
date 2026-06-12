package com.erygra.maskoflight.network.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * FirebaseManager.kt - مدير Firebase الرئيسي
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - إدارة جميع خدمات Firebase
 * - المصادقة (Authentication)
 * - Firestore للبيانات
 * - إدارة المستخدمين
 * 
 * المكونات الرئيسية:
 * - Firebase Authentication
 * - Firestore operations
 * - User management
 * - Error handling
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

class FirebaseManager(
    private val context: Context
) {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Firebase Authentication instance
     */
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    
    /**
     * Firestore database instance
     */
    private val firestore: FirebaseFirestore by lazy {
        Firebase.firestore
    }
    
    /**
     * المستخدم الحالي
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    /**
     * معرف المستخدم الحالي
     */
    val currentUserId: String?
        get() = currentUser?.uid
    
    /**
     * هل المستخدم مسجل دخول
     */
    val isUserSignedIn: Boolean
        get() = currentUser != null
    
    // ════════════════════════════════════════════════════════════════════════════
    // Initialization
    // ════════════════════════════════════════════════════════════════════════════
    
    init {
        initializeFirebase()
    }
    
    /**
     * تهيئة Firebase
     */
    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Timber.d("Firebase initialized successfully")
            }
            
            // تفعيل offline persistence لـ Firestore
            firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            
        } catch (e: Exception) {
            Timber.e(e, "Error initializing Firebase")
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Authentication Methods
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تسجيل دخول بالبريد الإلكتروني وكلمة المرور
     *
     * @param email البريد الإلكتروني
     * @param password كلمة المرور
     * @return FirebaseUser في حالة النجاح
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                Timber.d("User signed in: ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed: user is null"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error signing in")
            Result.failure(e)
        }
    }
    
    /**
     * إنشاء حساب جديد
     *
     * @param email البريد الإلكتروني
     * @param password كلمة المرور
     * @return FirebaseUser في حالة النجاح
     */
    suspend fun createAccount(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                Timber.d("Account created: ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(Exception("Account creation failed: user is null"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating account")
            Result.failure(e)
        }
    }
    
    /**
     * تسجيل دخول كضيف (Anonymous)
     *
     * @return FirebaseUser في حالة النجاح
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user
            
            if (user != null) {
                Timber.d("Anonymous sign in: ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(Exception("Anonymous sign in failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error signing in anonymously")
            Result.failure(e)
        }
    }
    
    /**
     * تسجيل الخروج
     */
    fun signOut() {
        try {
            auth.signOut()
            Timber.d("User signed out")
        } catch (e: Exception) {
            Timber.e(e, "Error signing out")
        }
    }
    
    /**
     * إرسال بريد إعادة تعيين كلمة المرور
     *
     * @param email البريد الإلكتروني
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Timber.d("Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending password reset email")
            Result.failure(e)
        }
    }
    
    /**
     * تحديث البريد الإلكتروني
     *
     * @param newEmail البريد الإلكتروني الجديد
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            currentUser?.updateEmail(newEmail)?.await()
            Timber.d("Email updated to: $newEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating email")
            Result.failure(e)
        }
    }
    
    /**
     * تحديث كلمة المرور
     *
     * @param newPassword كلمة المرور الجديدة
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            currentUser?.updatePassword(newPassword)?.await()
            Timber.d("Password updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating password")
            Result.failure(e)
        }
    }
    
    /**
     * إرسال بريد التحقق
     */
    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            currentUser?.sendEmailVerification()?.await()
            Timber.d("Verification email sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending verification email")
            Result.failure(e)
        }
    }
    
    /**
     * حذف الحساب
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            currentUser?.delete()?.await()
            Timber.d("Account deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting account")
            Result.failure(e)
        }
    }
    
    /**
     * تحديث معلومات المستخدم
     *
     * @param displayName الاسم المعروض
     * @param photoUrl رابط الصورة
     */
    suspend fun updateUserProfile(
        displayName: String? = null,
        photoUrl: String? = null
    ): Result<Unit> {
        return try {
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            displayName?.let { profileUpdates.setDisplayName(it) }
            photoUrl?.let { profileUpdates.setPhotoUri(android.net.Uri.parse(it)) }
            
            currentUser?.updateProfile(profileUpdates.build())?.await()
            Timber.d("User profile updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating user profile")
            Result.failure(e)
        }
    }
    
    /**
     * إعادة تحميل بيانات المستخدم
     */
    suspend fun reloadUser(): Result<Unit> {
        return try {
            currentUser?.reload()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error reloading user")
            Result.failure(e)
        }
    }
    
    /**
     * الحصول على token المستخدم
     */
    suspend fun getUserToken(forceRefresh: Boolean = false): Result<String> {
        return try {
            val result = currentUser?.getIdToken(forceRefresh)?.await()
            val token = result?.token
            
            if (token != null) {
                Result.success(token)
            } else {
                Result.failure(Exception("Failed to get user token"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user token")
            Result.failure(e)
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Firestore Operations
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * حفظ وثيقة في Firestore
     *
     * @param collection اسم المجموعة
     * @param documentId معرف الوثيقة
     * @param data البيانات
     */
    suspend fun saveDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any>
    ): Result<Unit> {
        return try {
            firestore.collection(collection)
                .document(documentId)
                .set(data)
                .await()
            
            Timber.d("Document saved: $collection/$documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving document")
            Result.failure(e)
        }
    }
    
    /**
     * تحديث وثيقة في Firestore
     *
     * @param collection اسم المجموعة
     * @param documentId معرف الوثيقة
     * @param updates التحديثات
     */
    suspend fun updateDocument(
        collection: String,
        documentId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            firestore.collection(collection)
                .document(documentId)
                .update(updates)
                .await()
            
            Timber.d("Document updated: $collection/$documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating document")
            Result.failure(e)
        }
    }
    
    /**
     * الحصول على وثيقة من Firestore
     *
     * @param collection اسم المجموعة
     * @param documentId معرف الوثيقة
     * @return البيانات كـ Map
     */
    suspend fun getDocument(
        collection: String,
        documentId: String
    ): Result<Map<String, Any>> {
        return try {
            val document = firestore.collection(collection)
                .document(documentId)
                .get()
                .await()
            
            if (document.exists()) {
                val data = document.data ?: emptyMap()
                Timber.d("Document retrieved: $collection/$documentId")
                Result.success(data)
            } else {
                Result.failure(Exception("Document not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting document")
            Result.failure(e)
        }
    }
    
    /**
     * حذف وثيقة من Firestore
     *
     * @param collection اسم المجموعة
     * @param documentId معرف الوثيقة
     */
    suspend fun deleteDocument(
        collection: String,
        documentId: String
    ): Result<Unit> {
        return try {
            firestore.collection(collection)
                .document(documentId)
                .delete()
                .await()
            
            Timber.d("Document deleted: $collection/$documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting document")
            Result.failure(e)
        }
    }
    
    /**
     * الاستماع لتغييرات وثيقة في الوقت الفعلي
     *
     * @param collection اسم المجموعة
     * @param documentId معرف الوثيقة
     * @return Flow من البيانات
     */
    fun observeDocument(
        collection: String,
        documentId: String
    ): Flow<Map<String, Any>?> = callbackFlow {
        val listener = firestore.collection(collection)
            .document(documentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing document")
                    close(error)
                    return@addSnapshotListener
                }
                
                val data = snapshot?.data
                trySend(data)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * الحصول على مجموعة من Firestore
     *
     * @param collection اسم المجموعة
     * @return قائمة الوثائق
     */
    suspend fun getCollection(
        collection: String
    ): Result<List<Map<String, Any>>> {
        return try {
            val documents = firestore.collection(collection)
                .get()
                .await()
            
            val data = documents.documents.mapNotNull { it.data }
            Timber.d("Collection retrieved: $collection (${data.size} documents)")
            Result.success(data)
        } catch (e: Exception) {
            Timber.e(e, "Error getting collection")
            Result.failure(e)
        }
    }
    
    /**
     * استعلام مخصص من Firestore
     *
     * @param collection اسم المجموعة
     * @param whereClause شرط الاستعلام
     * @param limit الحد الأقصى للنتائج
     * @return قائمة الوثائق
     */
    suspend fun queryCollection(
        collection: String,
        whereClause: Triple<String, String, Any>? = null,
        limit: Long? = null
    ): Result<List<Map<String, Any>>> {
        return try {
            var query = firestore.collection(collection) as com.google.firebase.firestore.Query
            
            // إضافة شرط where
            whereClause?.let { (field, operator, value) ->
                query = when (operator) {
                    "==" -> query.whereEqualTo(field, value)
                    "!=" -> query.whereNotEqualTo(field, value)
                    "<" -> query.whereLessThan(field, value)
                    "<=" -> query.whereLessThanOrEqualTo(field, value)
                    ">" -> query.whereGreaterThan(field, value)
                    ">=" -> query.whereGreaterThanOrEqualTo(field, value)
                    else -> query
                }
            }
            
            // إضافة limit
            limit?.let {
                query = query.limit(it)
            }
            
            val documents = query.get().await()
            val data = documents.documents.mapNotNull { it.data }
            
            Timber.d("Query executed: $collection (${data.size} results)")
            Result.success(data)
        } catch (e: Exception) {
            Timber.e(e, "Error querying collection")
            Result.failure(e)
        }
    }
    
    /**
     * دفعة عمليات Firestore
     *
     * @param operations قائمة العمليات
     */
    suspend fun batchOperation(
        operations: List<FirestoreOperation>
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()
            
            operations.forEach { operation ->
                val docRef = firestore.collection(operation.collection)
                    .document(operation.documentId)
                
                when (operation.type) {
                    OperationType.SET -> batch.set(docRef, operation.data)
                    OperationType.UPDATE -> batch.update(docRef, operation.data)
                    OperationType.DELETE -> batch.delete(docRef)
                }
            }
            
            batch.commit().await()
            Timber.d("Batch operation completed: ${operations.size} operations")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error in batch operation")
            Result.failure(e)
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Helper Classes
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * نوع العملية في Firestore
     */
    enum class OperationType {
        SET, UPDATE, DELETE
    }
    
    /**
     * عملية Firestore
     */
    data class FirestoreOperation(
        val collection: String,
        val documentId: String,
        val type: OperationType,
        val data: Map<String, Any> = emptyMap()
    )
    
    companion object {
        // Collection names
        const val COLLECTION_USERS = "users"
        const val COLLECTION_SAVES = "saves"
        const val COLLECTION_STATS = "stats"
        const val COLLECTION_ACHIEVEMENTS = "achievements"
        const val COLLECTION_LEADERBOARD = "leaderboard"
        const val COLLECTION_SETTINGS = "settings"
        
        // Field names
        const val FIELD_USER_ID = "userId"
        const val FIELD_EMAIL = "email"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_LAST_LOGIN = "lastLogin"
    }
}