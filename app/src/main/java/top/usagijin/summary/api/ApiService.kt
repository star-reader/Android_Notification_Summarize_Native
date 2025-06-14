package top.usagijin.summary.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import top.usagijin.summary.data.*
import top.usagijin.summary.private_config.ApiConfig
import top.usagijin.summary.private_config.ApiTestConfig
import top.usagijin.summary.utils.CryptoUtils
import top.usagijin.summary.utils.SecureTokenStorage
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * API服务类，提供完整的API调用功能
 * 包含Token认证、Apply ID获取和摘要生成
 */
class ApiService private constructor(private val context: Context) {
    
    private val TAG = "ApiService"
    
    private val tokenStorage = SecureTokenStorage.getInstance(context)
    private val gson = Gson()
    
    // 创建认证拦截器
    private val authInterceptor = Interceptor { chain ->
        val token = tokenStorage.getToken()
        val requestBuilder = chain.request().newBuilder()
        
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        chain.proceed(requestBuilder.build())
    }
    
    // 创建OkHttp客户端
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .apply {
            if (ApiConfig.Debug.ENABLE_LOGGING) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }
        .connectTimeout(ApiConfig.Timeouts.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.Timeouts.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.Timeouts.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    // 创建Retrofit实例
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // 创建API接口实例
    private val api = retrofit.create(SummarizeApi::class.java)
    
    companion object {
        @Volatile
        private var INSTANCE: ApiService? = null
        
        /**
         * 获取API服务实例（单例模式）
         */
        fun getInstance(context: Context): ApiService {
            return INSTANCE ?: synchronized(this) {
                val instance = ApiService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * 刷新JWT Token
     * @return 是否成功
     */
    suspend fun refreshToken(): Boolean {
        return try {
            Log.d(TAG, "Refreshing JWT token...")
            
            val request = TokenRequest(
                clientId = ApiConfig.CLIENT_ID,
                clientSecret = ApiConfig.CLIENT_SECRET
            )
            
            val response = api.getToken(request)
            
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!.token
                tokenStorage.saveToken(token)
                Log.i(TAG, "Token refreshed successfully")
                true
            } else {
                Log.e(TAG, "Token refresh failed: ${response.code()} ${response.message()}")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error", e)
            false
        }
    }
    
    /**
     * 获取Apply ID
     * @return Apply ID或null
     */
    private suspend fun getApplyId(): String? {
        return try {
            Log.d(TAG, "Getting apply ID...")
            
            val response = api.getApplyId()
            
            if (response.isSuccessful && response.body() != null) {
                val applyId = response.body()!!.applyId
                Log.d(TAG, "Apply ID obtained successfully")
                applyId
            } else {
                Log.e(TAG, "Get apply ID failed: ${response.code()} ${response.message()}")
                null
            }
            
        } catch (e: HttpException) {
            if (e.code() == 401) {
                Log.w(TAG, "Token expired, refreshing...")
                if (refreshToken()) {
                    // 重试获取Apply ID
                    return getApplyId()
                }
            }
            Log.e(TAG, "Get apply ID HTTP error", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Get apply ID error", e)
            null
        }
    }
    
    /**
     * 调用摘要API（带重试机制）
     * @param notifications 要摘要的通知列表
     * @return 摘要结果或null（如果失败）
     */
    suspend fun getSummary(notifications: List<NotificationData>): SummaryData? {
        if (notifications.isEmpty()) {
            Log.w(TAG, "Empty notifications list provided")
            return null
        }
        
        // 检查是否使用测试模式
        if (ApiTestConfig.ENABLE_TEST_MODE && ApiTestConfig.TestMode.USE_MOCK_RESPONSES) {
            return generateMockSummary(notifications)
        }
        
        // 确保有Token
        if (!tokenStorage.hasToken() || tokenStorage.isTokenExpired()) {
            Log.d(TAG, "Token missing or expired, refreshing...")
            if (!refreshToken()) {
                Log.e(TAG, "Failed to get token, cannot proceed with summary")
                return null
            }
        }
        
        // 首次尝试
        val result = attemptSummarize(notifications)
        if (result != null) {
            return result
        }
        
        // 等待5秒后重试一次
        Log.i(TAG, "First attempt failed, retrying after 5 seconds...")
        delay(ApiConfig.Retry.RETRY_DELAY_MS)
        
        return attemptSummarize(notifications)
    }
    
    /**
     * 尝试调用摘要API
     */
    private suspend fun attemptSummarize(notifications: List<NotificationData>): SummaryData? {
        return try {
            // 检查是否使用测试模式
            if (ApiTestConfig.ENABLE_TEST_MODE && ApiTestConfig.TestMode.USE_MOCK_RESPONSES) {
                return generateMockSummary(notifications)
            }
            
            // 获取Apply ID
            val applyId = getApplyId()
            if (applyId == null) {
                Log.e(TAG, "Failed to get apply ID")
                return null
            }
            
            // 准备通知数据
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val notificationDataWrapper = NotificationDataWrapper(
                currentTime = currentTime,
                data = notifications.map { notification ->
                    NotificationApiInput(
                        title = notification.title,
                        content = notification.content?.take(1000), // 限制长度
                        time = notification.time,
                        packageName = notification.packageName
                    )
                }
            )
            
            // 计算验证哈希
            val dataJson = gson.toJson(notificationDataWrapper)
            val verify = CryptoUtils.computeSHA256(dataJson)
            
            // 添加调试日志
            Log.d(TAG, "NotificationDataWrapper JSON: $dataJson")
            Log.d(TAG, "SHA256 verify hash: $verify")
            
            // 构建请求
            val request = SummarizeApiRequest(
                data = notificationDataWrapper,
                applyId = applyId,
                verify = verify
            )
            
            Log.d(TAG, "Calling generate summary API...")
            Log.d(TAG, "Request applyId: $applyId")
            Log.d(TAG, "Full request JSON: ${gson.toJson(request)}")
            
            // 调用API
            val response = api.generateSummary(request)
            
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                
                Log.i(TAG, "API call successful: ${apiResponse.title}")
                
                // 生成摘要ID
                val summaryId = "${notifications.first().packageName}_summary_${System.currentTimeMillis()}"
                
                SummaryData(
                    id = summaryId,
                    packageName = notifications.first().packageName,
                    appName = notifications.first().appName,
                    title = apiResponse.title,
                    summary = apiResponse.summary,
                    importanceLevel = apiResponse.importanceLevel,
                    time = currentTime
                )
            } else {
                Log.e(TAG, "Generate summary failed: ${response.code()} ${response.message()}")
                null
            }
            
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> {
                    Log.w(TAG, "Token expired during summary generation")
                    if (refreshToken()) {
                        Log.d(TAG, "Token refreshed, retrying summary generation...")
                        return attemptSummarize(notifications)
                    }
                }
                400 -> {
                    Log.e(TAG, "Bad request - invalid applyId or verify")
                }
            }
            Log.e(TAG, "HTTP error during summary generation", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Summary generation error", e)
            null
        }
    }
    
    /**
     * 初始化API服务
     * 在应用启动时调用，确保有有效的Token
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing API service...")
            
            // 检查是否使用测试模式
            if (ApiTestConfig.ENABLE_TEST_MODE && ApiTestConfig.TestMode.USE_MOCK_RESPONSES) {
                Log.i(TAG, "Test mode enabled, using mock token")
                tokenStorage.saveToken(ApiTestConfig.TestMode.MOCK_TOKEN)
                return true
            }
            
            if (!tokenStorage.hasToken() || tokenStorage.isTokenExpired()) {
                Log.d(TAG, "No valid token found, getting new token...")
                refreshToken()
            } else {
                Log.d(TAG, "Valid token found")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "API service initialization failed", e)
            false
        }
    }
    
    /**
     * 检查API服务状态
     */
    fun isReady(): Boolean {
        return tokenStorage.hasToken() && !tokenStorage.isTokenExpired()
    }
    
    /**
     * 生成模拟摘要（测试模式）
     */
    private suspend fun generateMockSummary(notifications: List<NotificationData>): SummaryData? {
        return try {
            Log.d(TAG, "Generating mock summary for testing...")
            
            // 模拟网络延迟
            delay(ApiTestConfig.TestMode.MOCK_NETWORK_DELAY)
            
            // 模拟API失败
            if (ApiTestConfig.shouldMockFailure()) {
                Log.w(TAG, "Simulating API failure for testing")
                return null
            }
            
            val packageName = notifications.first().packageName
            val mockResponse = ApiTestConfig.getMockSummaryResponse(packageName)
            
            // 生成摘要ID
            val summaryId = "${packageName}_mock_${System.currentTimeMillis()}"
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            Log.i(TAG, "Mock API call successful: ${mockResponse.title}")
            
            SummaryData(
                id = summaryId,
                packageName = packageName,
                appName = notifications.first().appName,
                title = mockResponse.title,
                summary = mockResponse.summary,
                importanceLevel = mockResponse.importanceLevel,
                time = currentTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Mock summary generation error", e)
            null
        }
    }
} 