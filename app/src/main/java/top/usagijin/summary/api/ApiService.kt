package top.usagijin.summary.api

import android.util.Log
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import top.usagijin.summary.data.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * API服务类，提供摘要API调用功能
 * 包含模拟API逻辑和1秒延迟
 */
class ApiService private constructor() {
    
    private val TAG = "ApiService"
    
    // 模拟API的基础URL
    private val baseUrl = "https://api.example.com/"
    
    // 创建OkHttp客户端，添加延迟拦截器
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // 模拟1秒网络延迟
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Network delay interrupted", e)
            }
            chain.proceed(chain.request())
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // 创建Retrofit实例
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
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
        fun getInstance(): ApiService {
            return INSTANCE ?: synchronized(this) {
                val instance = ApiService()
                INSTANCE = instance
                instance
            }
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
        
        val currentTime = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())
        
        val request = SummarizeRequest(
            currentTime = currentTime,
            data = notifications.map { notification ->
                NotificationInput(
                    title = notification.title,
                    content = notification.content,
                    time = notification.time,
                    packageName = notification.packageName
                )
            }
        )
        
        // 首次尝试
        val result = attemptSummarize(request, notifications)
        if (result != null) {
            return result
        }
        
        // 等待5秒后重试一次
        Log.i(TAG, "First attempt failed, retrying after 5 seconds...")
        delay(5000)
        
        return attemptSummarize(request, notifications)
    }
    
    /**
     * 尝试调用摘要API
     */
    private suspend fun attemptSummarize(
        @Suppress("UNUSED_PARAMETER") request: SummarizeRequest,
        notifications: List<NotificationData>
    ): SummaryData? {
        return try {
            // 由于这是模拟API，我们直接生成模拟响应而不是真正的网络调用
            // 在真实实现中，这里会使用request参数进行网络调用
            val mockResponse = generateMockResponse(notifications)
            
            Log.i(TAG, "API call successful: ${mockResponse.title}")
            
            // 生成摘要ID
            val summaryId = "${notifications.first().packageName}_summary_${System.currentTimeMillis()}"
            
            SummaryData(
                id = summaryId,
                packageName = notifications.first().packageName,
                appName = notifications.first().appName,
                title = mockResponse.title,
                summary = mockResponse.summary,
                importanceLevel = mockResponse.importanceLevel,
                time = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())
            )
        } catch (e: Exception) {
            Log.e(TAG, "API call failed", e)
            null
        }
    }
    
    /**
     * 生成模拟API响应
     * 根据应用包名和通知内容生成合适的摘要
     */
    private suspend fun generateMockResponse(notifications: List<NotificationData>): SummarizeResponse {
        // 添加1秒延迟模拟网络请求
        delay(1000)
        
        val packageName = notifications.first().packageName
        val totalNotifications = notifications.size
        
        return when {
            packageName.contains("wechat") || packageName.contains("tencent.mm") -> {
                SummarizeResponse(
                    title = "微信消息",
                    summary = if (totalNotifications > 1) 
                        "收到${totalNotifications}条微信消息，包含群聊和私聊内容" 
                    else 
                        "收到新的微信消息",
                    importanceLevel = if (totalNotifications > 3) 3 else 2
                )
            }
            packageName.contains("gmail") || packageName.contains("email") -> {
                SummarizeResponse(
                    title = "邮件摘要",
                    summary = if (totalNotifications > 1) 
                        "收到${totalNotifications}封新邮件，包含工作和个人邮件" 
                    else 
                        "收到新的邮件",
                    importanceLevel = determineEmailImportance(notifications)
                )
            }
            packageName.contains("whatsapp") -> {
                SummarizeResponse(
                    title = "WhatsApp消息",
                    summary = "收到${totalNotifications}条WhatsApp消息",
                    importanceLevel = 2
                )
            }
            packageName.contains("telegram") -> {
                SummarizeResponse(
                    title = "Telegram消息",
                    summary = "收到Telegram消息和更新",
                    importanceLevel = 2
                )
            }
            packageName.contains("facebook") -> {
                SummarizeResponse(
                    title = "Facebook通知",
                    summary = "Facebook社交动态和消息",
                    importanceLevel = 1
                )
            }
            packageName.contains("twitter") || packageName.contains("x.com") -> {
                SummarizeResponse(
                    title = "Twitter/X通知",
                    summary = "Twitter/X动态和互动",
                    importanceLevel = 1
                )
            }
            packageName.contains("instagram") -> {
                SummarizeResponse(
                    title = "Instagram通知",
                    summary = "Instagram动态和消息",
                    importanceLevel = 1
                )
            }
            packageName.contains("calendar") -> {
                SummarizeResponse(
                    title = "日历提醒",
                    summary = "即将到来的日程安排",
                    importanceLevel = 3
                )
            }
            packageName.contains("banking") || packageName.contains("pay") -> {
                SummarizeResponse(
                    title = "支付通知",
                    summary = "账户交易和支付提醒",
                    importanceLevel = 3
                )
            }
            else -> {
                // 通用摘要
                val contentLength = notifications.sumOf { (it.content?.length ?: 0) + (it.title?.length ?: 0) }
                SummarizeResponse(
                    title = "应用通知",
                    summary = if (totalNotifications > 1) 
                        "收到${totalNotifications}条${notifications.first().appName}通知" 
                    else 
                        "${notifications.first().appName}发送了新通知",
                    importanceLevel = when {
                        contentLength > 200 -> 3
                        contentLength > 100 -> 2
                        else -> 1
                    }
                )
            }
        }
    }
    
    /**
     * 根据邮件内容判断重要性
     */
    private fun determineEmailImportance(notifications: List<NotificationData>): Int {
        val importantKeywords = listOf("urgent", "important", "meeting", "deadline", "紧急", "重要", "会议", "截止")
        
        val hasImportantContent = notifications.any { notification ->
            val content = "${notification.title} ${notification.content}".lowercase()
            importantKeywords.any { keyword -> content.contains(keyword.lowercase()) }
        }
        
        return when {
            hasImportantContent -> 3
            notifications.size > 3 -> 2
            else -> 1
        }
    }
} 