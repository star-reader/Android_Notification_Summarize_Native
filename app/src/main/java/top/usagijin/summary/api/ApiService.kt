package top.usagijin.summary.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import top.usagijin.summary.service.NativeModelLoader
import top.usagijin.summary.utils.MemoryUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * API服务类 - 使用NDK本地ONNX模型进行通知摘要
 * 完全重写以支持原生推理，保持相同的接口
 */
class ApiService private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ApiService"
        
        @Volatile
        private var INSTANCE: ApiService? = null
        
        fun getInstance(context: Context): ApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 模型状态枚举
    sealed class ModelState {
        object NotLoaded : ModelState()
        object Loading : ModelState()
        object Loaded : ModelState()
        data class Error(val message: String) : ModelState()
    }
    
    // 状态管理
    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotLoaded)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()
    
    // Native模型加载器
    private val nativeLoader = NativeModelLoader.getInstance()
    
    // 注意：系统提示词现在在native_inference.cpp中定义，这里不再需要

    /**
     * 初始化模型
     */
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        if (_modelState.value is ModelState.Loaded) {
            return@withContext true
        }
        
        _modelState.value = ModelState.Loading
        Log.i(TAG, "开始初始化NDK模型...")
        
        try {
            // 1. 检查系统内存
            val memoryInfo = MemoryUtils.getMemoryInfo(context)
            Log.i(TAG, "系统内存信息: $memoryInfo")
            
            // 2. 强制垃圾回收
            MemoryUtils.forceGarbageCollection()
            
            // 3. 使用NDK加载器初始化
            Log.i(TAG, "使用NDK加载器初始化模型和分词器...")
            val success = nativeLoader.initialize(context)
            
            if (success) {
                _modelState.value = ModelState.Loaded
                Log.i(TAG, "✓ NDK模型初始化成功！")
                Log.i(TAG, "词汇表大小: ${nativeLoader.getVocabularySize()}")
                return@withContext true
            } else {
                throw Exception("NDK模型初始化失败")
            }
            
        } catch (e: Exception) {
            val errorMsg = "NDK模型初始化失败: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _modelState.value = ModelState.Error(errorMsg)
            return@withContext false
        }
    }
    
    /**
     * 摘要API - 保持原有接口，使用NDK推理
     */
    suspend fun summarize(request: SummarizeRequest): SummarizeResponse = withContext(Dispatchers.IO) {
        // 确保模型已加载
        if (_modelState.value !is ModelState.Loaded) {
            if (!initializeModel()) {
                throw Exception("模型未加载")
            }
        }
        
        Log.i(TAG, "开始NDK推理，通知数量: ${request.data.size}")
        
        try {
            // 构建输入文本
            val inputText = buildInputText(request)
            Log.d(TAG, "输入文本长度: ${inputText.length}")
            
            // 执行NDK推理
            val jsonResult = nativeLoader.performSummarization(inputText)
            
            if (jsonResult.isNullOrEmpty()) {
                throw Exception("NDK推理返回空结果")
            }
            
            // 解析JSON结果
            val result = parseJsonResponse(jsonResult)
            
            Log.i(TAG, "NDK推理成功: ${result.summary}")
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "NDK推理失败", e)
            
            // 降级到规则引擎
            Log.w(TAG, "降级到规则引擎...")
            return@withContext fallbackToRuleEngine(request)
        }
    }
    
    /**
     * 构建输入文本 - 生成JSON格式供NDK解析
     */
    private fun buildInputText(request: SummarizeRequest): String {
        try {
            // 构建JSON格式的输入
            val jsonObject = JSONObject()
            jsonObject.put("currentTime", request.currentTime)
            
            val dataArray = org.json.JSONArray()
            request.data.forEach { notification ->
                val notifObj = JSONObject()
                notifObj.put("title", notification.title ?: "")
                notifObj.put("content", notification.content ?: "")
                notifObj.put("time", notification.time)
                notifObj.put("packageName", notification.packageName)
                dataArray.put(notifObj)
            }
            jsonObject.put("data", dataArray)
            
            // 不再添加systemPrompt，让native_inference.cpp使用内置的系统提示词
            
            val jsonString = jsonObject.toString()
            Log.d(TAG, "构建的JSON输入: $jsonString")
            return jsonString
            
        } catch (e: Exception) {
            Log.e(TAG, "构建JSON输入失败: ${e.message}", e)
            // 如果JSON构建失败，返回简单的JSON格式
            return """{"currentTime":"${request.currentTime}","data":[]}"""
        }
    }
    
    /**
     * 解析JSON响应
     */
    private fun parseJsonResponse(jsonResult: String): SummarizeResponse {
        return try {
            val jsonObject = JSONObject(jsonResult)
            SummarizeResponse(
                title = jsonObject.optString("title", "通知摘要"),
                summary = jsonObject.optString("summary", "收到新通知"),
                importanceLevel = jsonObject.optInt("importanceLevel", 2)
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON解析失败: $jsonResult", e)
            // 返回默认响应
            SummarizeResponse(
                title = "通知摘要",
                summary = "收到新通知",
                importanceLevel = 2
            )
        }
    }
    
    /**
     * 降级到规则引擎（当NDK推理失败时）
     */
    private fun fallbackToRuleEngine(request: SummarizeRequest): SummarizeResponse {
        Log.i(TAG, "使用规则引擎生成摘要...")
        
        val notifications = request.data
        
        return when {
            notifications.isEmpty() -> {
                SummarizeResponse("通知摘要", "收到新通知", 2)
            }
            notifications.size == 1 -> {
                generateSingleNotificationSummary(notifications[0])
            }
            else -> {
                generateMultipleNotificationsSummary(notifications)
            }
        }
    }
    
    /**
     * 生成单个通知摘要
     */
    private fun generateSingleNotificationSummary(notification: NotificationInput): SummarizeResponse {
        val appName = getAppNameFromPackage(notification.packageName)
        val content = notification.content ?: notification.title ?: "新通知"
        
        return when {
            notification.packageName.contains("tencent.mm") -> {
                SummarizeResponse(
                    title = "微信消息",
                    summary = if (content.length > 100) "${content.take(97)}..." else content,
                    importanceLevel = if (content.contains("@") || content.contains("紧急")) 5 else 3
                )
            }
            notification.packageName.contains("tencent.mobileqq") -> {
                SummarizeResponse(
                    title = "QQ消息",
                    summary = if (content.length > 100) "${content.take(97)}..." else content,
                    importanceLevel = 3
                )
            }
            notification.packageName.contains("mail") || notification.packageName.contains("gmail") -> {
                SummarizeResponse(
                    title = "邮件通知",
                    summary = "收到新邮件: ${if (content.length > 80) "${content.take(77)}..." else content}",
                    importanceLevel = 4
                )
            }
            notification.packageName.contains("sms") -> {
                SummarizeResponse(
                    title = "短信通知",
                    summary = if (content.length > 100) "${content.take(97)}..." else content,
                    importanceLevel = 4
                )
            }
            else -> {
                SummarizeResponse(
                    title = "${appName}通知",
                    summary = if (content.length > 100) "${content.take(97)}..." else content,
                    importanceLevel = 2
                )
            }
        }
    }
    
    /**
     * 生成多个通知摘要
     */
    private fun generateMultipleNotificationsSummary(notifications: List<NotificationInput>): SummarizeResponse {
        val appGroups = notifications.groupBy { it.packageName }
        
        return if (appGroups.size == 1) {
            // 同一应用的多个通知
            val packageName = appGroups.keys.first()
            val appName = getAppNameFromPackage(packageName)
            val count = notifications.size
            
            SummarizeResponse(
                title = "${appName}消息",
                summary = "收到${count}条${appName}消息",
                importanceLevel = kotlin.math.min(count, 5)
            )
        } else {
            // 多个应用的通知
            val summary = appGroups.map { (pkg, notifs) ->
                "${getAppNameFromPackage(pkg)}(${notifs.size})"
            }.joinToString(", ")
            
            SummarizeResponse(
                title = "多条通知",
                summary = "收到通知: $summary",
                importanceLevel = 3
            )
        }
    }
    
    /**
     * 根据包名获取应用名称
     */
    private fun getAppNameFromPackage(packageName: String): String {
        return when {
            packageName.contains("tencent.mm") -> "微信"
            packageName.contains("tencent.mobileqq") -> "QQ"
            packageName.contains("gmail") -> "Gmail"
            packageName.contains("mail") -> "邮件"
            packageName.contains("sms") -> "短信"
            packageName.contains("phone") -> "电话"
            packageName.contains("calendar") -> "日历"
            packageName.contains("clock") -> "时钟"
            else -> {
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    "应用"
                }
            }
        }
    }
    
    /**
     * 检查模型是否已加载
     */
    fun isModelLoaded(): Boolean {
        return _modelState.value is ModelState.Loaded && nativeLoader.isFullyInitialized()
    }
    
    /**
     * 获取模型状态
     */
    fun getModelState(): ModelState {
        return _modelState.value
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            Log.i(TAG, "清理ApiService资源...")
            nativeLoader.cleanup()
            _modelState.value = ModelState.NotLoaded
            Log.d(TAG, "ApiService资源清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "资源清理异常: ${e.message}", e)
        }
    }
}

// 请求和响应数据类
data class SummarizeRequest(
    val currentTime: String,
    val data: List<NotificationInput>
)

data class NotificationInput(
    val title: String?,
    val content: String?,
    val time: String,
    val packageName: String
)

data class SummarizeResponse(
    val title: String,
    val summary: String,
    val importanceLevel: Int
) 