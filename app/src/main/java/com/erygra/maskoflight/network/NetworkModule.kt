package com.erygra.maskoflight.network

import android.content.Context
import com.erygra.maskoflight.BuildConfig
import com.erygra.maskoflight.network.api.CloudSaveApi
import com.erygra.maskoflight.network.api.GameApiService
import com.erygra.maskoflight.network.api.LeaderboardApi
import com.erygra.maskoflight.network.firebase.Analytics
import com.erygra.maskoflight.network.firebase.CloudStorage
import com.erygra.maskoflight.network.firebase.FirebaseManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * NetworkModule.kt - وحدة الشبكة الرئيسية
 * ════════════════════════════════════════════════════════════════════════════════
 * 
 * الوصف:
 * - توفير جميع dependencies الشبكية
 * - إعداد Retrofit و OkHttp
 * - إدارة Firebase services
 * - Singleton instances
 * 
 * المكونات الرئيسية:
 * - Retrofit setup
 * - OkHttp configuration
 * - Firebase integration
 * - API services providers
 * 
 * @author Erygra Team
 * @since 2.0.0
 * ════════════════════════════════════════════════════════════════════════════════
 */

object NetworkModule {
    
    // ════════════════════════════════════════════════════════════════════════════
    // Properties
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Context المستخدم
     */
    private lateinit var appContext: Context
    
    /**
     * Authentication token
     */
    private var authToken: String? = null
    
    /**
     * هل تم التهيئة
     */
    private var isInitialized = false
    
    // ════════════════════════════════════════════════════════════════════════════
    // Initialization
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تهيئة الوحدة
     *
     * @param context Application context
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Timber.w("NetworkModule already initialized")
            return
        }
        
        appContext = context.applicationContext
        isInitialized = true
        
        Timber.d("NetworkModule initialized")
    }
    
    /**
     * تعيين token المصادقة
     *
     * @param token رمز المصادقة
     */
    fun setAuthToken(token: String?) {
        authToken = token
        Timber.d("Auth token ${if (token != null) "set" else "cleared"}")
    }
    
    /**
     * الحصول على token المصادقة
     */
    fun getAuthToken(): String? = authToken
    
    /**
     * التحقق من التهيئة
     */
    private fun requireInitialized() {
        check(isInitialized) { "NetworkModule not initialized. Call initialize() first." }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Moshi - JSON Parser
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Moshi instance
     */
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // OkHttp - HTTP Client
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * HTTP Cache
     */
    private val httpCache: Cache by lazy {
        requireInitialized()
        val cacheSize = 10L * 1024 * 1024 // 10 MB
        val cacheDir = File(appContext.cacheDir, "http_cache")
        Cache(cacheDir, cacheSize)
    }
    
    /**
     * Logging Interceptor
     */
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    /**
     * Auth Interceptor - إضافة token للطلبات
     */
    private val authInterceptor: Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        val newRequest = if (authToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $authToken")
                .build()
        } else {
            originalRequest
        }
        
        chain.proceed(newRequest)
    }
    
    /**
     * Headers Interceptor - إضافة headers عامة
     */
    private val headersInterceptor: Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        val newRequest = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "MaskOfLight-Android/${BuildConfig.VERSION_NAME}")
            .header("X-App-Version", BuildConfig.VERSION_NAME)
            .header("X-Platform", "Android")
            .build()
        
        chain.proceed(newRequest)
    }
    
    /**
     * Network Interceptor - معالجة أخطاء الشبكة
     */
    private val networkInterceptor: Interceptor = Interceptor { chain ->
        try {
            val response = chain.proceed(chain.request())
            
            // Log response code
            Timber.d("Response: ${response.code} - ${response.request.url}")
            
            response
        } catch (e: Exception) {
            Timber.e(e, "Network error")
            throw e
        }
    }
    
    /**
     * Retry Interceptor - إعادة المحاولة عند الفشل
     */
    private val retryInterceptor: Interceptor = Interceptor { chain ->
        val request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0
        val maxRetries = 3
        
        while (!response.isSuccessful && tryCount < maxRetries) {
            tryCount++
            Timber.d("Retry attempt $tryCount for ${request.url}")
            
            response.close()
            Thread.sleep(1000L * tryCount) // Exponential backoff
            response = chain.proceed(request)
        }
        
        response
    }
    
    /**
     * OkHttp Client
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(httpCache)
            .connectTimeout(GameApiService.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(GameApiService.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(GameApiService.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(headersInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(networkInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(retryInterceptor)
            .build()
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Retrofit - REST Client
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Retrofit instance
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    /**
     * الحصول على Base URL حسب البيئة
     */
    private fun getBaseUrl(): String {
        return if (BuildConfig.DEBUG) {
            GameApiService.DEV_BASE_URL
        } else {
            GameApiService.BASE_URL
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // API Services
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Game API Service
     */
    val gameApiService: GameApiService by lazy {
        retrofit.create(GameApiService::class.java)
    }
    
    /**
     * Leaderboard API Service
     */
    val leaderboardApi: LeaderboardApi by lazy {
        retrofit.create(LeaderboardApi::class.java)
    }
    
    /**
     * Cloud Save API Service
     */
    val cloudSaveApi: CloudSaveApi by lazy {
        retrofit.create(CloudSaveApi::class.java)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Firebase Services
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Firebase Manager
     */
    val firebaseManager: FirebaseManager by lazy {
        requireInitialized()
        FirebaseManager(appContext)
    }
    
    /**
     * Cloud Storage
     */
    val cloudStorage: CloudStorage by lazy {
        requireInitialized()
        CloudStorage(appContext)
    }
    
    /**
     * Analytics
     */
    val analytics: Analytics by lazy {
        requireInitialized()
        Analytics(appContext)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Network Status
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من اتصال الإنترنت
     */
    fun isNetworkAvailable(): Boolean {
        requireInitialized()
        
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as android.net.ConnectivityManager
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * نوع الاتصال
     */
    enum class ConnectionType {
        WIFI, CELLULAR, ETHERNET, NONE
    }
    
    /**
     * الحصول على نوع الاتصال
     */
    fun getConnectionType(): ConnectionType {
        requireInitialized()
        
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as android.net.ConnectivityManager
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) 
                ?: return ConnectionType.NONE
            
            when {
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> 
                    ConnectionType.WIFI
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> 
                    ConnectionType.CELLULAR
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> 
                    ConnectionType.ETHERNET
                else -> ConnectionType.NONE
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                android.net.ConnectivityManager.TYPE_WIFI -> ConnectionType.WIFI
                android.net.ConnectivityManager.TYPE_MOBILE -> ConnectionType.CELLULAR
                android.net.ConnectivityManager.TYPE_ETHERNET -> ConnectionType.ETHERNET
                else -> ConnectionType.NONE
            }
        }
    }
    
    /**
     * هل الاتصال Wi-Fi
     */
    fun isWifiConnection(): Boolean {
        return getConnectionType() == ConnectionType.WIFI
    }
    
    /**
     * هل الاتصال بيانات خلوية
     */
    fun isCellularConnection(): Boolean {
        return getConnectionType() == ConnectionType.CELLULAR
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Cache Management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * مسح الذاكرة المؤقتة
     */
    fun clearCache() {
        try {
            httpCache.evictAll()
            Timber.d("HTTP cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing cache")
        }
    }
    
    /**
     * الحصول على حجم الذاكرة المؤقتة
     */
    fun getCacheSize(): Long {
        return try {
            httpCache.size()
        } catch (e: Exception) {
            Timber.e(e, "Error getting cache size")
            0L
        }
    }
    
    /**
     * الحصول على حجم الذاكرة المؤقتة (نص قابل للقراءة)
     */
    fun getCacheSizeReadable(): String {
        val bytes = getCacheSize()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Network Configuration
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إعدادات الشبكة
     */
    data class NetworkConfig(
        val connectTimeout: Long = GameApiService.CONNECT_TIMEOUT,
        val readTimeout: Long = GameApiService.READ_TIMEOUT,
        val writeTimeout: Long = GameApiService.WRITE_TIMEOUT,
        val enableLogging: Boolean = BuildConfig.DEBUG,
        val enableRetry: Boolean = true,
        val maxRetries: Int = 3,
        val cacheSize: Long = 10L * 1024 * 1024
    )
    
    /**
     * الإعدادات الحالية
     */
    private var currentConfig = NetworkConfig()
    
    /**
     * تحديث إعدادات الشبكة
     */
    fun updateConfig(config: NetworkConfig) {
        currentConfig = config
        // سيتطلب إعادة بناء OkHttpClient
        Timber.d("Network config updated")
    }
    
    /**
     * الحصول على الإعدادات الحالية
     */
    fun getConfig(): NetworkConfig = currentConfig
    
    // ════════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * إنشاء OkHttpClient مخصص
     */
    fun createCustomHttpClient(
        config: NetworkConfig = currentConfig,
        additionalInterceptors: List<Interceptor> = emptyList()
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cache(httpCache)
            .connectTimeout(config.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(config.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeout, TimeUnit.SECONDS)
            .addInterceptor(headersInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(networkInterceptor)
        
        if (config.enableLogging) {
            builder.addInterceptor(loggingInterceptor)
        }
        
        if (config.enableRetry) {
            builder.addInterceptor(retryInterceptor)
        }
        
        additionalInterceptors.forEach { interceptor ->
            builder.addInterceptor(interceptor)
        }
        
        return builder.build()
    }
    
    /**
     * إنشاء Retrofit مخصص
     */
    fun createCustomRetrofit(
        baseUrl: String,
        client: OkHttpClient = okHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    /**
     * إنشاء API service مخصص
     */
    inline fun <reified T> createApiService(
        baseUrl: String? = null,
        client: OkHttpClient? = null
    ): T {
        val retrofit = if (baseUrl != null || client != null) {
            createCustomRetrofit(
                baseUrl ?: getBaseUrl(),
                client ?: okHttpClient
            )
        } else {
            this.retrofit
        }
        
        return retrofit.create(T::class.java)
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // Cleanup
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        try {
            clearCache()
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            Timber.d("NetworkModule cleaned up")
        } catch (e: Exception) {
            Timber.e(e, "Error during cleanup")
        }
    }
    
    /**
     * إعادة التهيئة
     */
    fun reset() {
        cleanup()
        isInitialized = false
        authToken = null
        Timber.d("NetworkModule reset")
    }
}

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * Extension Functions
 * ════════════════════════════════════════════════════════════════════════════════
 */

/**
 * تنفيذ API call مع error handling
 */
suspend inline fun <T> safeApiCall(
    crossinline apiCall: suspend () -> retrofit2.Response<T>
): com.erygra.maskoflight.network.models.NetworkResponse<T> {
    return try {
        if (!NetworkModule.isNetworkAvailable()) {
            return com.erygra.maskoflight.network.models.NetworkResponse.Error(
                com.erygra.maskoflight.network.models.ApiError.network("No internet connection")
            )
        }
        
        val response = apiCall()
        
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                com.erygra.maskoflight.network.models.NetworkResponse.Success(body)
            } else {
                com.erygra.maskoflight.network.models.NetworkResponse.Error(
                    com.erygra.maskoflight.network.models.ApiError.general("Empty response body")
                )
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = errorBody ?: "Unknown error"
            
            com.erygra.maskoflight.network.models.NetworkResponse.Error(
                com.erygra.maskoflight.network.models.ApiError(
                    message = errorMessage,
                    code = "HTTP_${response.code()}"
                ),
                code = response.code()
            )
        }
    } catch (e: java.net.UnknownHostException) {
        com.erygra.maskoflight.network.models.NetworkResponse.Error(
            com.erygra.maskoflight.network.models.ApiError.network("Unable to resolve host")
        )
    } catch (e: java.net.SocketTimeoutException) {
        com.erygra.maskoflight.network.models.NetworkResponse.Error(
            com.erygra.maskoflight.network.models.ApiError.network("Connection timeout")
        )
    } catch (e: java.io.IOException) {
        com.erygra.maskoflight.network.models.NetworkResponse.Error(
            com.erygra.maskoflight.network.models.ApiError.network("Network I/O error: ${e.message}")
        )
    } catch (e: Exception) {
        Timber.e(e, "API call failed")
        com.erygra.maskoflight.network.models.NetworkResponse.Exception(e)
    }
}

/**
 * تنفيذ API call مع ApiResponse wrapper
 */
suspend inline fun <T> safeApiCallWithWrapper(
    crossinline apiCall: suspend () -> retrofit2.Response<com.erygra.maskoflight.network.models.ApiResponse<T>>
): com.erygra.maskoflight.network.models.NetworkResponse<T> {
    val response = safeApiCall(apiCall)
    
    return when (response) {
        is com.erygra.maskoflight.network.models.NetworkResponse.Success -> {
            response.data.toNetworkResponse()
        }
        is com.erygra.maskoflight.network.models.NetworkResponse.Error -> response
        is com.erygra.maskoflight.network.models.NetworkResponse.Exception -> response
        is com.erygra.maskoflight.network.models.NetworkResponse.Loading -> response
    }
}