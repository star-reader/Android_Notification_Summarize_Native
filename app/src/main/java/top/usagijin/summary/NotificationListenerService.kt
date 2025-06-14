package top.usagijin.summary

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import top.usagijin.summary.api.ApiService
import top.usagijin.summary.data.NotificationData
import top.usagijin.summary.repository.NotificationRepository
import top.usagijin.summary.utils.NotificationDisplayManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 通知监听服务
 * 实现通知拦截、触发规则判断和摘要生成逻辑
 */
class NotificationListenerService : NotificationListenerService() {
    
    private val TAG = "NotificationListenerService"
    
    // 协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 数据仓库和API服务
    private lateinit var repository: NotificationRepository
    private lateinit var apiService: ApiService
    private lateinit var displayManager: NotificationDisplayManager
    
    // 日期格式化器
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    // 通知计数器（包名 -> 计数）
    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts
    
    // 高频限制标记（包名 -> 暂停结束时间）
    private val highFrequencyPause = ConcurrentHashMap<String, Long>()
    
    // 待处理任务（包名 -> Job）
    private val pendingJobs = ConcurrentHashMap<String, Job>()
    
    // 常量定义
    companion object {
        private const val SINGLE_NOTIFICATION_DELAY = 5000L // 5秒
        private const val MULTIPLE_NOTIFICATIONS_DELAY = 10000L // 10秒
        private const val HIGH_FREQUENCY_PAUSE_DELAY = 30000L // 30秒
        private const val HIGH_FREQUENCY_THRESHOLD = 10 // 10条通知
        private const val SINGLE_NOTIFICATION_LENGTH_THRESHOLD = 26 // 26字符
        private const val MAX_SINGLE_NOTIFICATION_CHARS = 1000 // 单条通知最大字符数
        private const val MAX_MULTIPLE_NOTIFICATIONS_CHARS = 2000 // 多条通知最大字符数
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "NotificationListenerService created")
        
        // 初始化组件
        repository = NotificationRepository.getInstance(this)
        apiService = ApiService.getInstance(this)
        displayManager = NotificationDisplayManager.getInstance(this)
        
        // 启动批量处理工作
        startBatchProcessingWork()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "NotificationListenerService destroyed")
        
        // 取消所有协程
        serviceScope.cancel()
        
        // 取消所有待处理任务
        pendingJobs.values.forEach { it.cancel() }
        pendingJobs.clear()
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        // 异步处理通知
        serviceScope.launch {
            handleNotificationPosted(sbn)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }
    
    /**
     * 处理新到达的通知
     */
    private suspend fun handleNotificationPosted(sbn: StatusBarNotification) {
        try {
            // 解析通知数据
            val notificationData = parseNotification(sbn) ?: return

            if (notificationData.title == null && notificationData.content == null) {
                Log.d(TAG, "Ignoring notification with no title or content: ${notificationData.packageName}")
                return
            }

            if (notificationData.packageName == "top.usagijin.summary") {
                Log.d(TAG, "Ignoring notification from our own app: ${notificationData.packageName}")
                return
            }

            Log.i(TAG, "Received notification from ${notificationData.appName}: ${notificationData.title}")
            
            // 保存通知到数据库
            repository.addNotification(notificationData)
            
            // 更新通知计数
            updateNotificationCount(notificationData.packageName)
            
            // 检查是否需要触发摘要
            checkAndTriggerSummarization(notificationData)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification", e)
        }
    }
    
    /**
     * 解析通知数据
     */
    private fun parseNotification(sbn: StatusBarNotification): NotificationData.Standard? {
        return runCatching {
            val notification = sbn.notification
            val extras = notification.extras
            
            // 提取基本信息
            val packageName = sbn.packageName
            
            // 过滤掉自己应用的通知
            // if (packageName == "top.usagijin.summary") {
            //     Log.d(TAG, "Ignoring notification from our own app: $packageName")
            //     return null
            // }
            
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            
            // 过滤掉没有标题或内容的通知
            // if (title.isNullOrBlank() && content.isNullOrBlank()) {
            //     Log.d(TAG, "Ignoring notification with no title or content: $packageName")
            //     return null
            // }
            
            val time = dateFormat.format(Date(sbn.postTime))
            val isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0
            
            // 获取应用名称
            val appName = try {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
            
            // 生成唯一ID - 使用更可靠的方式避免重复
            val id = "${packageName}_${sbn.key}_${sbn.postTime}"
            
            NotificationData.Standard(
                id = id,
                packageName = packageName,
                appName = appName,
                title = title,
                content = content,
                time = time,
                isOngoing = isOngoing
            )
        }.getOrElse { e ->
            Log.e(TAG, "Failed to parse notification", e)
            null
        }
    }
    
    /**
     * 更新通知计数
     */
    private fun updateNotificationCount(packageName: String) {
        val currentCounts = _notificationCounts.value.toMutableMap()
        currentCounts[packageName] = (currentCounts[packageName] ?: 0) + 1
        _notificationCounts.value = currentCounts
        
        Log.d(TAG, "Updated count for $packageName: ${currentCounts[packageName]}")
    }
    
    /**
     * 检查并触发摘要生成
     */
    private suspend fun checkAndTriggerSummarization(notification: NotificationData.Standard) {
        val packageName = notification.packageName
        val currentCount = notificationCounts.value[packageName] ?: 0
        
        // 检查是否在高频限制期间
        val pauseEndTime = highFrequencyPause[packageName]
        if (pauseEndTime != null && System.currentTimeMillis() < pauseEndTime) {
            Log.d(TAG, "Package $packageName is in high-frequency pause")
            return
        }
        
        // 高频限制：>10条通知在10秒内
        if (currentCount > HIGH_FREQUENCY_THRESHOLD) {
            triggerHighFrequencyRestriction(packageName)
            return
        }
        
        val contentLength = (notification.content?.length ?: 0)
        
        when {
            // 场景1：单条长通知 (> 26字符)
            contentLength > SINGLE_NOTIFICATION_LENGTH_THRESHOLD -> {
                scheduleSingleNotificationSummarization(packageName)
            }
            
            // 场景2：多条通知 (≥2条在10秒内)
            currentCount >= 2 -> {
                scheduleMultipleNotificationsSummarization(packageName)
            }
            
            // 场景3：短通知 (≤ 26字符) - 仅存储，不触发摘要
            else -> {
                Log.d(TAG, "Short notification stored without summarization: $packageName")
            }
        }
    }
    
    /**
     * 调度单条通知摘要（5秒延迟）
     */
    private fun scheduleSingleNotificationSummarization(packageName: String) {
        // 取消之前的任务
        pendingJobs[packageName]?.cancel()
        
        val job = serviceScope.launch {
            try {
                delay(SINGLE_NOTIFICATION_DELAY)
                
                // 检查是否有新通知
                if (hasNewNotificationsSince(packageName, SINGLE_NOTIFICATION_DELAY)) {
                    Log.d(TAG, "New notifications received, skipping single notification summarization")
                    return@launch
                }
                
                // 获取最新通知
                val notifications = repository.getNotificationsByPackage(
                    packageName, 
                    SINGLE_NOTIFICATION_DELAY + 1000
                ).first().take(1)
                
                if (notifications.isNotEmpty()) {
                    generateSummary(notifications, "Single Long Notification")
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "Single notification summarization cancelled for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error in single notification summarization", e)
            } finally {
                pendingJobs.remove(packageName)
                resetNotificationCount(packageName)
            }
        }
        
        pendingJobs[packageName] = job
    }
    
    /**
     * 调度多条通知摘要（10秒延迟）
     */
    private fun scheduleMultipleNotificationsSummarization(packageName: String) {
        // 取消之前的任务
        pendingJobs[packageName]?.cancel()
        
        val job = serviceScope.launch {
            try {
                delay(MULTIPLE_NOTIFICATIONS_DELAY)
                
                // 获取最近10秒内的通知
                val notifications = repository.getNotificationsByPackage(
                    packageName, 
                    MULTIPLE_NOTIFICATIONS_DELAY + 1000
                ).first().take(5)
                
                if (notifications.size >= 2) {
                    generateSummary(notifications, "Multiple Notifications")
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "Multiple notifications summarization cancelled for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error in multiple notifications summarization", e)
            } finally {
                pendingJobs.remove(packageName)
                resetNotificationCount(packageName)
            }
        }
        
        pendingJobs[packageName] = job
    }
    
    /**
     * 触发高频限制
     */
    private fun triggerHighFrequencyRestriction(packageName: String) {
        Log.i(TAG, "High frequency restriction triggered for $packageName")
        
        // 设置暂停结束时间
        highFrequencyPause[packageName] = System.currentTimeMillis() + HIGH_FREQUENCY_PAUSE_DELAY
        
        serviceScope.launch {
            try {
                delay(HIGH_FREQUENCY_PAUSE_DELAY)
                
                // 获取最近的通知
                val notifications = repository.getNotificationsByPackage(
                    packageName, 
                    HIGH_FREQUENCY_PAUSE_DELAY + 1000
                ).first().take(10)
                
                if (notifications.isNotEmpty()) {
                    generateSummary(notifications, "High-Frequency Restriction")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in high frequency restriction handling", e)
            } finally {
                highFrequencyPause.remove(packageName)
                resetNotificationCount(packageName)
            }
        }
    }
    
    /**
     * 启动批量处理工作（每2分钟执行一次）
     */
    private fun startBatchProcessingWork() {
        serviceScope.launch {
            while (isActive) {
                try {
                    delay(120000) // 2分钟
                    processBatchNotifications()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in batch processing", e)
                }
            }
        }
    }
    
    /**
     * 批量处理未处理的通知
     */
    private suspend fun processBatchNotifications() {
        try {
            val unprocessedNotifications = repository.getUnprocessedNotifications(10).first()
            
            if (unprocessedNotifications.size >= 3) {
                Log.i(TAG, "Processing batch of ${unprocessedNotifications.size} notifications")
                generateSummary(unprocessedNotifications, "Low-Frequency Batch")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch processing", e)
        }
    }
    
    /**
     * 生成摘要
     */
    private suspend fun generateSummary(notifications: List<NotificationData>, scenario: String) {
        if (notifications.isEmpty()) return
        
        try {
            Log.i(TAG, "Generating summary for $scenario: ${notifications.size} notifications")
            
            // 截断通知内容以符合API限制
            val truncatedNotifications = truncateNotifications(notifications, scenario)
            
            // 调用API生成摘要
            val summary = apiService.getSummary(truncatedNotifications)
            
            if (summary != null) {
                // 保存摘要
                repository.addSummary(summary)
                
                // 标记通知为已处理
                val notificationIds = notifications.map { it.id }
                repository.markNotificationsAsProcessed(notificationIds)
                
                // 显示摘要通知
                displayManager.showSummaryNotification(summary)
                
                Log.i(TAG, "Summary generated successfully: ${summary.title}")
            } else {
                Log.w(TAG, "Failed to generate summary for $scenario")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating summary", e)
        }
    }
    
    /**
     * 截断通知内容
     */
    private fun truncateNotifications(notifications: List<NotificationData>, scenario: String): List<NotificationData> {
        val maxChars = when (scenario) {
            "Single Long Notification" -> MAX_SINGLE_NOTIFICATION_CHARS
            else -> MAX_MULTIPLE_NOTIFICATIONS_CHARS
        }
        
        var totalChars = 0
        val result = mutableListOf<NotificationData>()
        
        for (notification in notifications) {
            val notificationLength = (notification.title?.length ?: 0) + (notification.content?.length ?: 0)
            
            if (totalChars + notificationLength <= maxChars) {
                result.add(notification)
                totalChars += notificationLength
            } else {
                // 截断最后一条通知
                val remainingChars = maxChars - totalChars
                if (remainingChars > 50) { // 至少保留50个字符
                    val truncatedContent = truncateToLastSentence(notification.content, remainingChars)
                    val truncatedNotification = when (notification) {
                        is NotificationData.Standard -> notification.copy(content = truncatedContent)
                    }
                    result.add(truncatedNotification)
                }
                break
            }
        }
        
        return result
    }
    
    /**
     * 截断到最后一个完整句子
     */
    private fun truncateToLastSentence(content: String?, maxLength: Int): String? {
        if (content == null || content.length <= maxLength) return content
        
        val truncated = content.substring(0, maxLength)
        val lastSentenceEnd = truncated.lastIndexOfAny(charArrayOf('.', '!', '?', '。', '！', '？'))
        
        return if (lastSentenceEnd > 0) {
            truncated.substring(0, lastSentenceEnd + 1)
        } else {
            truncated
        }
    }
    
    /**
     * 检查是否有新通知
     */
    private suspend fun hasNewNotificationsSince(packageName: String, timeMs: Long): Boolean {
        val recentNotifications = repository.getNotificationsByPackage(packageName, timeMs).first()
        return recentNotifications.size > 1
    }
    
    /**
     * 重置通知计数
     */
    private fun resetNotificationCount(packageName: String) {
        val currentCounts = _notificationCounts.value.toMutableMap()
        currentCounts.remove(packageName)
        _notificationCounts.value = currentCounts
    }
}