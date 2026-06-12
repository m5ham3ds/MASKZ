package com.erygra.maskoflight.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * NetworkResponse.kt - نماذج الاستجابة الشبكية
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - يحتوي على sealed class للاستجابات الشبكية
 * - يوفر error handling موحد
 * - يدعم pagination و metadata
 * 
 * المكونات الرئيسية:
 * - NetworkResponse: sealed class للنتائج
 * - ApiError: نموذج الأخطاء
 * - Pagination: نموذج الصفحات
 * - ApiMetadata: البيانات الوصفية
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

/**
 * Sealed class يمثل جميع حالات الاستجابة الشبكية
 *
 * @param T نوع البيانات في حالة النجاح
 */
sealed class NetworkResponse<out T> {
    
    /**
     * استجابة ناجحة مع البيانات
     *
     * @property data البيانات المستلمة
     * @property metadata البيانات الوصفية الاختيارية
     */
    data class Success<T>(
        val data: T,
        val metadata: ApiMetadata? = null
    ) : NetworkResponse<T>()
    
    /**
     * خطأ في الاستجابة
     *
     * @property error تفاصيل الخطأ
     * @property code كود الخطأ HTTP
     */
    data class Error(
        val error: ApiError,
        val code: Int? = null
    ) : NetworkResponse<Nothing>()
    
    /**
     * استثناء غير متوقع
     *
     * @property exception الاستثناء الذي حدث
     * @property message رسالة الخطأ
     */
    data class Exception(
        val exception: Throwable,
        val message: String = exception.localizedMessage ?: "Unknown error"
    ) : NetworkResponse<Nothing>()
    
    /**
     * حالة التحميل
     */
    object Loading : NetworkResponse<Nothing>()
}

/**
 * نموذج بيانات الخطأ من API
 *
 * @property message رسالة الخطأ
 * @property code كود الخطأ الخاص بالتطبيق
 * @property field الحقل المسبب للخطأ (في حالة validation)
 * @property details تفاصيل إضافية
 * @property timestamp وقت حدوث الخطأ
 */
@JsonClass(generateAdapter = true)
data class ApiError(
    @Json(name = "message")
    val message: String,
    
    @Json(name = "code")
    val code: String? = null,
    
    @Json(name = "field")
    val field: String? = null,
    
    @Json(name = "details")
    val details: Map<String, Any>? = null,
    
    @Json(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * إنشاء خطأ عام
         */
        fun general(message: String) = ApiError(
            message = message,
            code = "GENERAL_ERROR"
        )
        
        /**
         * خطأ في الشبكة
         */
        fun network(message: String = "Network connection failed") = ApiError(
            message = message,
            code = "NETWORK_ERROR"
        )
        
        /**
         * خطأ في المصادقة
         */
        fun authentication(message: String = "Authentication failed") = ApiError(
            message = message,
            code = "AUTH_ERROR"
        )
        
        /**
         * خطأ في التفويض
         */
        fun authorization(message: String = "Access denied") = ApiError(
            message = message,
            code = "AUTHORIZATION_ERROR"
        )
        
        /**
         * خطأ في التحقق من البيانات
         */
        fun validation(field: String, message: String) = ApiError(
            message = message,
            code = "VALIDATION_ERROR",
            field = field
        )
        
        /**
         * خطأ في الخادم
         */
        fun server(message: String = "Server error occurred") = ApiError(
            message = message,
            code = "SERVER_ERROR"
        )
    }
}

/**
 * نموذج البيانات الوصفية للاستجابة
 *
 * @property requestId معرف الطلب
 * @property timestamp وقت الاستجابة
 * @property version إصدار API
 * @property serverTime وقت الخادم
 */
@JsonClass(generateAdapter = true)
data class ApiMetadata(
    @Json(name = "request_id")
    val requestId: String? = null,
    
    @Json(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @Json(name = "version")
    val version: String? = null,
    
    @Json(name = "server_time")
    val serverTime: Long? = null
)

/**
 * نموذج الاستجابة المغلفة من API
 *
 * @param T نوع البيانات
 * @property success هل الطلب ناجح
 * @property data البيانات
 * @property error تفاصيل الخطأ في حالة الفشل
 * @property metadata البيانات الوصفية
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success")
    val success: Boolean,
    
    @Json(name = "data")
    val data: T? = null,
    
    @Json(name = "error")
    val error: ApiError? = null,
    
    @Json(name = "metadata")
    val metadata: ApiMetadata? = null
)

/**
 * نموذج الاستجابة مع pagination
 *
 * @param T نوع البيانات في القائمة
 * @property items القائمة
 * @property pagination معلومات الصفحات
 */
@JsonClass(generateAdapter = true)
data class PaginatedResponse<T>(
    @Json(name = "items")
    val items: List<T>,
    
    @Json(name = "pagination")
    val pagination: Pagination
)

/**
 * نموذج معلومات الصفحات
 *
 * @property page الصفحة الحالية
 * @property pageSize عدد العناصر في الصفحة
 * @property totalItems إجمالي العناصر
 * @property totalPages إجمالي الصفحات
 * @property hasNext هل يوجد صفحة تالية
 * @property hasPrevious هل يوجد صفحة سابقة
 */
@JsonClass(generateAdapter = true)
data class Pagination(
    @Json(name = "page")
    val page: Int,
    
    @Json(name = "page_size")
    val pageSize: Int,
    
    @Json(name = "total_items")
    val totalItems: Int,
    
    @Json(name = "total_pages")
    val totalPages: Int,
    
    @Json(name = "has_next")
    val hasNext: Boolean,
    
    @Json(name = "has_previous")
    val hasPrevious: Boolean
) {
    /**
     * الصفحة التالية
     */
    val nextPage: Int?
        get() = if (hasNext) page + 1 else null
    
    /**
     * الصفحة السابقة
     */
    val previousPage: Int?
        get() = if (hasPrevious) page - 1 else null
}

/**
 * Extension functions لتحويل NetworkResponse
 */

/**
 * تحويل ApiResponse إلى NetworkResponse
 */
fun <T> ApiResponse<T>.toNetworkResponse(): NetworkResponse<T> {
    return when {
        success && data != null -> NetworkResponse.Success(data, metadata)
        !success && error != null -> NetworkResponse.Error(error)
        else -> NetworkResponse.Error(ApiError.general("Invalid response format"))
    }
}

/**
 * التحقق من نجاح الاستجابة
 */
fun <T> NetworkResponse<T>.isSuccess(): Boolean {
    return this is NetworkResponse.Success
}

/**
 * التحقق من فشل الاستجابة
 */
fun <T> NetworkResponse<T>.isError(): Boolean {
    return this is NetworkResponse.Error || this is NetworkResponse.Exception
}

/**
 * الحصول على البيانات أو null
 */
fun <T> NetworkResponse<T>.getOrNull(): T? {
    return when (this) {
        is NetworkResponse.Success -> data
        else -> null
    }
}

/**
 * الحصول على رسالة الخطأ
 */
fun <T> NetworkResponse<T>.getErrorMessage(): String? {
    return when (this) {
        is NetworkResponse.Error -> error.message
        is NetworkResponse.Exception -> message
        else -> null
    }
}

/**
 * تنفيذ action عند النجاح
 */
inline fun <T> NetworkResponse<T>.onSuccess(action: (T) -> Unit): NetworkResponse<T> {
    if (this is NetworkResponse.Success) {
        action(data)
    }
    return this
}

/**
 * تنفيذ action عند الفشل
 */
inline fun <T> NetworkResponse<T>.onError(action: (ApiError) -> Unit): NetworkResponse<T> {
    if (this is NetworkResponse.Error) {
        action(error)
    }
    return this
}

/**
 * تنفيذ action عند الاستثناء
 */
inline fun <T> NetworkResponse<T>.onException(action: (Throwable) -> Unit): NetworkResponse<T> {
    if (this is NetworkResponse.Exception) {
        action(exception)
    }
    return this
}

/**
 * تحويل NetworkResponse إلى نوع آخر
 */
inline fun <T, R> NetworkResponse<T>.map(transform: (T) -> R): NetworkResponse<R> {
    return when (this) {
        is NetworkResponse.Success -> NetworkResponse.Success(transform(data), metadata)
        is NetworkResponse.Error -> this
        is NetworkResponse.Exception -> this
        is NetworkResponse.Loading -> this
    }
}

/**
 * تحويل NetworkResponse مع إمكانية الفشل
 */
inline fun <T, R> NetworkResponse<T>.flatMap(
    transform: (T) -> NetworkResponse<R>
): NetworkResponse<R> {
    return when (this) {
        is NetworkResponse.Success -> transform(data)
        is NetworkResponse.Error -> this
        is NetworkResponse.Exception -> this
        is NetworkResponse.Loading -> this
    }
}