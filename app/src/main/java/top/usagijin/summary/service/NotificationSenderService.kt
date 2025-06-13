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
            val notificationId = generateNotificationId(summary)
            
            val notification = createSummaryNotification(context, summary, channelId)
            
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notification.build())
            
            Log.d(TAG, "Summary notification sent for ${summary.appName}")
            
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
            
            // 发送各个摘要通知
            summaries.forEach { summary ->
                val channelId = getChannelIdForImportance(summary.importanceLevel)
                val notificationId = generateNotificationId(summary)
                
                val notification = createSummaryNotification(context, summary, channelId)
                    .setGroup(GROUP_SUMMARY)
                
                notificationManager.notify(notificationId, notification.build())
            }
            
            // 发送分组摘要通知
            val groupSummaryNotification = createGroupSummaryNotification(context, summaries)
            notificationManager.notify(GROUP_SUMMARY.hashCode(), groupSummaryNotification.build())
            
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
            .setContentTitle("${getImportanceEmoji(summary.importanceLevel)} ${summary.title}")
            .setContentText(summary.summary)
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
            .setSubText(summary.appName)
            .addAction(createViewDetailsAction(context, summary))
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
        
        // 创建收件箱样式
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("智能摘要 (${summaries.size}条)")
            .setSummaryText("点击查看详情")
        
        // 添加摘要行（最多5条）
        summaries.take(5).forEach { summary ->
            inboxStyle.addLine("${getImportanceEmoji(summary.importanceLevel)} ${summary.appName}: ${summary.title}")
        }
        
        if (summaries.size > 5) {
            inboxStyle.addLine("还有 ${summaries.size - 5} 条摘要...")
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
     * 生成通知ID
     */
    private fun generateNotificationId(summary: SummaryData): Int {
        return "${summary.packageName}_${summary.time}".hashCode()
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
            notificationManager.cancelAll()
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
            val notificationId = generateNotificationId(summary)
            notificationManager.cancel(notificationId)
            Log.d(TAG, "Summary notification cleared for ${summary.appName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing notification", e)
        }
    }
} 