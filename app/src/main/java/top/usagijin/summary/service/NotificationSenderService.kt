package top.usagijin.summary.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import top.usagijin.summary.MainActivity
import top.usagijin.summary.R
import top.usagijin.summary.data.SummaryData
import top.usagijin.summary.utils.PermissionHelper
import java.util.concurrent.atomic.AtomicInteger

/**
 * 通知发送服务
 * 负责发送美化的摘要通知
 */
object NotificationSenderService {
    
    private const val TAG = "NotificationSenderService"
    
    // 通知渠道
    private const val CHANNEL_ID_SUMMARY = "summary_notifications"
    private const val CHANNEL_ID_HIGH_PRIORITY = "high_priority_summary"
    private const val CHANNEL_ID_MEDIUM_PRIORITY = "medium_priority_summary"
    private const val CHANNEL_ID_LOW_PRIORITY = "low_priority_summary"
    
    // 通知组
    private const val GROUP_SUMMARY = "summary_group"
    
    // 通知ID计数器，确保每个通知都有唯一ID
    private val notificationIdCounter = AtomicInteger(1000)
    
    // 存储已发送的通知ID，用于管理
    private val activeNotificationIds = mutableSetOf<Int>()
    
    /**
     * 初始化通知渠道
     */
    fun initializeNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 创建高优先级通知渠道
            val highPriorityChannel = NotificationChannel(
                CHANNEL_ID_HIGH_PRIORITY,
                "高优先级摘要",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "重要的通知摘要"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }
            
            // 创建中优先级通知渠道
            val mediumPriorityChannel = NotificationChannel(
                CHANNEL_ID_MEDIUM_PRIORITY,
                "中优先级摘要",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "一般的通知摘要"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setShowBadge(true)
            }
            
            // 创建低优先级通知渠道
            val lowPriorityChannel = NotificationChannel(
                CHANNEL_ID_LOW_PRIORITY,
                "低优先级摘要",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "不重要的通知摘要"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            // 注册通知渠道
            notificationManager.createNotificationChannel(highPriorityChannel)
            notificationManager.createNotificationChannel(mediumPriorityChannel)
            notificationManager.createNotificationChannel(lowPriorityChannel)
            
            Log.d(TAG, "Notification channels initialized")
        }
    }
    
    /**
     * 发送摘要通知
     */
    fun sendSummaryNotification(context: Context, summary: SummaryData) {
        if (!PermissionHelper.canPostNotifications(context)) {
            Log.w(TAG, "Cannot post notifications - permission not granted")
            return
        }
        
        try {
            val channelId = getChannelIdForImportance(summary.importanceLevel)
            val notificationId = generateUniqueNotificationId(summary)
            
            val notification = createSummaryNotification(context, summary, channelId)
                .setGroup(GROUP_SUMMARY) // 将所有摘要通知加入同一组
            
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notification.build())
            
            // 记录活跃的通知ID
            synchronized(activeNotificationIds) {
                activeNotificationIds.add(notificationId)
            }
            
            Log.d(TAG, "Summary notification sent for ${summary.appName} with ID: $notificationId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending summary notification", e)
        }
    }
    
    /**
     * 发送分组摘要通知
     */
    fun sendGroupedSummaryNotifications(context: Context, summaries: List<SummaryData>) {
        if (!PermissionHelper.canPostNotifications(context) || summaries.isEmpty()) {
            Log.w(TAG, "Cannot post notifications or no summaries to send")
            return
        }
        
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val sentNotificationIds = mutableListOf<Int>()
            
            // 发送各个摘要通知
            summaries.forEach { summary ->
                val channelId = getChannelIdForImportance(summary.importanceLevel)
                val notificationId = generateUniqueNotificationId(summary)
                
                val notification = createSummaryNotification(context, summary, channelId)
                    .setGroup(GROUP_SUMMARY)
                
                notificationManager.notify(notificationId, notification.build())
                sentNotificationIds.add(notificationId)
            }
            
            // 记录活跃的通知ID
            synchronized(activeNotificationIds) {
                activeNotificationIds.addAll(sentNotificationIds)
            }
            
            // 发送分组摘要通知
            val groupSummaryNotification = createGroupSummaryNotification(context, summaries)
            val groupNotificationId = GROUP_SUMMARY.hashCode()
            notificationManager.notify(groupNotificationId, groupSummaryNotification.build())
            
            Log.d(TAG, "Grouped summary notifications sent for ${summaries.size} summaries")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending grouped summary notifications", e)
        }
    }
    
    /**
     * 创建摘要通知
     */
    private fun createSummaryNotification(
        context: Context,
        summary: SummaryData,
        channelId: String
    ): NotificationCompat.Builder {
        
        // 创建点击意图
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "summaries")
            putExtra("summary_id", summary.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            summary.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 获取重要性相关的颜色和图标
        val (color, icon) = getNotificationStyleForImportance(summary.importanceLevel)
        
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle("${getImportanceEmoji(summary.importanceLevel)} ${summary.appName}")
            .setContentText(summary.title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(summary.summary)
                    .setBigContentTitle("${summary.appName} - ${summary.title}")
                    .setSummaryText("智能摘要 • ${formatTime(summary.time)}")
            )
            .setColor(color)
            .setColorized(true)
            .setPriority(getPriorityForImportance(summary.importanceLevel))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(parseTimeToMillis(summary.time))
            .setSubText("${summary.appName} • 智能摘要")
            .addAction(createViewDetailsAction(context, summary))
            .setOngoing(summary.importanceLevel == 3) // 高优先级设为持续通知
    }
    
    /**
     * 创建分组摘要通知
     */
    private fun createGroupSummaryNotification(
        context: Context,
        summaries: List<SummaryData>
    ): NotificationCompat.Builder {
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "summaries")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            GROUP_SUMMARY.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 按重要性分组统计
        val highPriorityCount = summaries.count { it.importanceLevel == 3 }
        val mediumPriorityCount = summaries.count { it.importanceLevel == 2 }
        val lowPriorityCount = summaries.count { it.importanceLevel == 1 }
        
        // 创建收件箱样式
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("智能摘要 (${summaries.size}条)")
            .setSummaryText("${highPriorityCount} ${mediumPriorityCount} ${lowPriorityCount}")
        
        // 按重要性排序并添加摘要行（最多7条）
        val sortedSummaries = summaries.sortedByDescending { it.importanceLevel }
        sortedSummaries.take(7).forEach { summary ->
            val timeStr = formatTime(summary.time)
            inboxStyle.addLine("${getImportanceEmoji(summary.importanceLevel)} ${summary.appName} • $timeStr")
        }
        
        if (summaries.size > 7) {
            inboxStyle.addLine("还有 ${summaries.size - 7} 条摘要...")
        }
        
        return NotificationCompat.Builder(context, CHANNEL_ID_MEDIUM_PRIORITY)
            .setSmallIcon(R.drawable.ic_summarize)
            .setContentTitle("智能摘要")
            .setContentText("收到 ${summaries.size} 条新摘要")
            .setStyle(inboxStyle)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_SUMMARY)
            .setGroupSummary(true)
            .setShowWhen(true)
            .setNumber(summaries.size)
    }
    
    /**
     * 创建查看详情操作
     */
    private fun createViewDetailsAction(context: Context, summary: SummaryData): NotificationCompat.Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "summaries")
            putExtra("summary_id", summary.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            summary.hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_info,
            "查看详情",
            pendingIntent
        ).build()
    }
    
    /**
     * 根据重要性获取通知渠道ID
     */
    private fun getChannelIdForImportance(importanceLevel: Int): String {
        return when (importanceLevel) {
            3 -> CHANNEL_ID_HIGH_PRIORITY
            2 -> CHANNEL_ID_MEDIUM_PRIORITY
            else -> CHANNEL_ID_LOW_PRIORITY
        }
    }
    
    /**
     * 根据重要性获取通知样式
     */
    private fun getNotificationStyleForImportance(importanceLevel: Int): Pair<Int, Int> {
        return when (importanceLevel) {
            3 -> Pair(Color.parseColor("#F44336"), R.drawable.ic_warning) // 红色，警告图标
            2 -> Pair(Color.parseColor("#FF9800"), R.drawable.ic_notification) // 橙色，通知图标
            else -> Pair(Color.parseColor("#4CAF50"), R.drawable.ic_info) // 绿色，信息图标
        }
    }
    
    /**
     * 根据重要性获取通知优先级
     */
    private fun getPriorityForImportance(importanceLevel: Int): Int {
        return when (importanceLevel) {
            3 -> NotificationCompat.PRIORITY_HIGH
            2 -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
    }
    
    /**
     * 根据重要性获取emoji
     */
    private fun getImportanceEmoji(importanceLevel: Int): String {
        return when (importanceLevel) {
            3 -> "🔴"
            2 -> "🟡"
            else -> "🟢"
        }
    }
    
    /**
     * 生成唯一通知ID
     */
    private fun generateUniqueNotificationId(summary: SummaryData): Int {
        // 方法1：使用摘要的唯一ID
        if (summary.id.isNotEmpty()) {
            return summary.id.hashCode()
        }
        
        // 方法2：使用多个字段组合生成唯一ID
        val uniqueString = "${summary.packageName}_${summary.time}_${summary.summary.hashCode()}_${System.nanoTime()}"
        return uniqueString.hashCode()
        
        // 方法3：如果以上都不行，使用计数器（备用方案）
        // return notificationIdCounter.getAndIncrement()
    }
    
    /**
     * 解析时间字符串为毫秒
     */
    private fun parseTimeToMillis(timeString: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            format.parse(timeString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    /**
     * 格式化时间显示
     */
    private fun formatTime(timeString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val date = inputFormat.parse(timeString)
            
            val now = java.util.Date()
            val diff = now.time - (date?.time ?: 0)
            
            when {
                diff < 60 * 1000 -> "刚刚"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                else -> {
                    val outputFormat = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                    outputFormat.format(date ?: java.util.Date())
                }
            }
        } catch (e: Exception) {
            timeString
        }
    }
    
    /**
     * 清除所有摘要通知
     */
    fun clearAllSummaryNotifications(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            
            // 清除所有活跃的通知
            synchronized(activeNotificationIds) {
                activeNotificationIds.forEach { notificationId ->
                    notificationManager.cancel(notificationId)
                }
                activeNotificationIds.clear()
            }
            
            // 清除分组摘要通知
            notificationManager.cancel(GROUP_SUMMARY.hashCode())
            
            Log.d(TAG, "All summary notifications cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notifications", e)
        }
    }
    
    /**
     * 清除特定摘要通知
     */
    fun clearSummaryNotification(context: Context, summary: SummaryData) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = generateUniqueNotificationId(summary)
            notificationManager.cancel(notificationId)
            
            // 从活跃通知列表中移除
            synchronized(activeNotificationIds) {
                activeNotificationIds.remove(notificationId)
            }
            
            Log.d(TAG, "Summary notification cleared for ${summary.appName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notification", e)
        }
    }
    
    /**
     * 清除老旧通知（可选功能）
     */
    fun clearOldNotifications(context: Context, maxAge: Long = 24 * 60 * 60 * 1000) {
        try {
            // 这里可以实现清除超过指定时间的通知的逻辑
            // 需要配合数据库来跟踪通知的时间
            Log.d(TAG, "Old notifications cleanup requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing old notifications", e)
        }
    }
    
    /**
     * 获取活跃通知数量
     */
    fun getActiveNotificationCount(): Int {
        return synchronized(activeNotificationIds) {
            activeNotificationIds.size
        }
    }
} 