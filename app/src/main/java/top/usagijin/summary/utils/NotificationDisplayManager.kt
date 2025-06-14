package top.usagijin.summary.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import top.usagijin.summary.MainActivity
import top.usagijin.summary.NotificationDetailActivity
import top.usagijin.summary.R
import top.usagijin.summary.data.SummaryData

/**
 * 通知显示管理器
 * 负责显示摘要通知，支持分组、优先级和自定义样式
 */
class NotificationDisplayManager private constructor(private val context: Context) {
    
    private val TAG = "NotificationDisplayManager"
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    
    // 通知渠道ID
    companion object {
        private const val CHANNEL_ID_COMMON = "summary_common"
        private const val CHANNEL_ID_PRIOR = "summary_prior"
        
        // 默认颜色配置
        private val DEFAULT_COLORS = mapOf(
            "com.tencent.mm" to Color.parseColor("#FF4CAF50"), // WeChat 绿色
            "com.google.android.gm" to Color.parseColor("#FF2196F3"), // Gmail 蓝色
            "com.whatsapp" to Color.parseColor("#FF4CAF50"), // WhatsApp 绿色
            "org.telegram.messenger" to Color.parseColor("#FF2196F3"), // Telegram 蓝色
            "com.facebook.katana" to Color.parseColor("#FF3F51B5"), // Facebook 蓝色
            "com.twitter.android" to Color.parseColor("#FF00BCD4"), // Twitter 青色
            "com.instagram.android" to Color.parseColor("#FFE91E63"), // Instagram 粉色
        )
        
        @Volatile
        private var INSTANCE: NotificationDisplayManager? = null
        
        fun getInstance(context: Context): NotificationDisplayManager {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationDisplayManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_PRIOR,
                    "优先摘要通知",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "高优先级摘要通知（5级），常驻显示"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    setBypassDnd(true)
                },
                
                NotificationChannel(
                    CHANNEL_ID_COMMON,
                    "普通摘要通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "普通摘要通知（1-4级）"
                    enableLights(true)
                    lightColor = Color.BLUE
                }
            )
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                systemNotificationManager.createNotificationChannel(channel)
            }
            
            Log.i(TAG, "Notification channels created")
        }
    }
    
    /**
     * 显示摘要通知
     */
    fun showSummaryNotification(summary: SummaryData) {
        try {
            Log.d(TAG, "showSummaryNotification called with: ${summary.title}, importance: ${summary.importanceLevel}")
            
            val channelId = getChannelIdByImportance(summary.importanceLevel)
            val color = getAppColor(summary.packageName)
            
            Log.d(TAG, "Using channel: $channelId, color: $color")
            
            // 5级优先通知：独立显示，常驻
            if (summary.importanceLevel == 5) {
                Log.d(TAG, "Displaying prior notification (level 5)")
                displayPriorNotification(summary, channelId, color)
            } else {
                Log.d(TAG, "Displaying common notification (level ${summary.importanceLevel})")
                // 1-4级普通通知：按应用合并，替换之前的通知
                displayCommonNotification(summary, channelId, color)
            }
            
            Log.i(TAG, "Summary notification displayed: ${summary.title} (Level: ${summary.importanceLevel})")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying summary notification", e)
        }
    }
    
    /**
     * 显示优先通知（5级，独立显示，常驻）
     */
    private fun displayPriorNotification(summary: SummaryData, channelId: String, color: Int) {
        val notificationId = generateNotificationId(summary)
        
        // 创建意图
        val detailIntent = Intent(context, NotificationDetailActivity::class.java).apply {
            putExtra("summary_id", summary.id)
            putExtra("package_name", summary.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 使用纯文本摘要
        val summaryText = "【优先通知】${summary.summary}"
        
        // 构建优先通知
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(summary.title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(summaryText)
                .setBigContentTitle("${summary.appName} - ${summary.title}")
                .setSummaryText("优先摘要"))
            .setColor(color)
            .setColorized(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true) // 持久化显示
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .addAction(createViewDetailsAction(summary))
            .build()
        
        // 显示通知
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * 显示普通通知（1-4级，按应用合并替换）
     */
    private fun displayCommonNotification(summary: SummaryData, channelId: String, color: Int) {
        // 使用固定的通知ID，这样同一应用的通知会相互替换
        val notificationId = summary.packageName.hashCode()
        
        // 先取消该应用之前的普通通知
        cancelCommonNotificationsForPackage(summary.packageName)
        
        // 创建意图
        val detailIntent = Intent(context, NotificationDetailActivity::class.java).apply {
            putExtra("summary_id", summary.id)
            putExtra("package_name", summary.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 使用纯文本摘要
        val summaryText = summary.summary
        
        // 构建普通通知
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(summary.title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(summaryText)
                .setBigContentTitle("${summary.appName} - ${summary.title}")
                .setSummaryText("普通摘要"))
            .setColor(color)
            .setColorized(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(summary.packageName) // 按应用分组
            .setPriority(getNotificationPriority(summary.importanceLevel))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .addAction(createViewDetailsAction(summary))
            .build()
        
        // 显示通知
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * 创建分组摘要通知
     */
    private fun createGroupSummaryIfNeeded(packageName: String, appName: String) {
        try {
            val groupSummaryId = packageName.hashCode()
            val color = getAppColor(packageName)
            
            // 获取该应用的活跃通知数量
            val activeNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.activeNotifications.count { 
                    it.tag == null && it.groupKey?.contains(packageName) == true 
                }
            } else {
                1 // 早期版本无法获取活跃通知，假设有1个
            }
            
            if (activeNotifications >= 2) {
                val groupSummary = NotificationCompat.Builder(context, CHANNEL_ID_COMMON)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("$appName 摘要")
                    .setContentText("${activeNotifications}条摘要")
                    .setStyle(NotificationCompat.InboxStyle()
                        .setBigContentTitle("$appName 摘要")
                        .setSummaryText("${activeNotifications}条摘要"))
                    .setColor(color)
                    .setColorized(true)
                    .setGroup(packageName)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .build()
                
                notificationManager.notify(groupSummaryId, groupSummary)
                Log.d(TAG, "Group summary created for $packageName")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group summary", e)
        }
    }
    
    /**
     * 创建"查看详情"操作
     */
    private fun createViewDetailsAction(summary: SummaryData): NotificationCompat.Action {
        val intent = Intent(context, NotificationDetailActivity::class.java).apply {
            putExtra("summary_id", summary.id)
            putExtra("package_name", summary.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            summary.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "查看详情",
            pendingIntent
        ).build()
    }
    

    

    
    /**
     * 根据重要性获取通知渠道ID
     */
    private fun getChannelIdByImportance(importanceLevel: Int): String {
        return when (importanceLevel) {
            5 -> CHANNEL_ID_PRIOR // 5级：优先通知
            else -> CHANNEL_ID_COMMON // 1-4级：普通通知
        }
    }
    
    /**
     * 根据重要性获取通知优先级
     */
    private fun getNotificationPriority(importanceLevel: Int): Int {
        return when (importanceLevel) {
            5 -> NotificationCompat.PRIORITY_HIGH // 5级：高优先级
            4 -> NotificationCompat.PRIORITY_DEFAULT // 4级：默认优先级
            3 -> NotificationCompat.PRIORITY_DEFAULT // 3级：默认优先级
            2 -> NotificationCompat.PRIORITY_DEFAULT // 2级：低优先级
            else -> NotificationCompat.PRIORITY_DEFAULT // 1级：最低优先级
        }
    }
    
    /**
     * 获取应用颜色
     */
    fun getAppColor(packageName: String): Int {
        // 首先尝试从SharedPreferences获取自定义颜色
        val customColor = sharedPreferences.getInt("color_$packageName", 0)
        if (customColor != 0) {
            return customColor
        }
        
        // 使用默认颜色
        return DEFAULT_COLORS[packageName] ?: Color.parseColor("#FF2196F3")
    }
    
    /**
     * 设置应用颜色
     */
    fun setAppColor(packageName: String, color: Int) {
        sharedPreferences.edit()
            .putInt("color_$packageName", color)
            .apply()
        Log.d(TAG, "Color set for $packageName: $color")
    }
    
    /**
     * 生成通知ID
     */
    private fun generateNotificationId(summary: SummaryData): Int {
        return "${summary.packageName}_${summary.time}".hashCode()
    }
    
    /**
     * 取消指定应用的普通通知（1-4级）
     */
    private fun cancelCommonNotificationsForPackage(packageName: String) {
        try {
            // 取消该应用的普通通知（使用固定ID）
            notificationManager.cancel(packageName.hashCode())
            
            Log.d(TAG, "Cancelled common notifications for $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling common notifications", e)
        }
    }
    
    /**
     * 取消指定应用的所有摘要通知
     */
    fun cancelNotificationsForPackage(packageName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.activeNotifications
                    .filter { it.groupKey?.contains(packageName) == true }
                    .forEach { notificationManager.cancel(it.id) }
            }
            
            // 取消分组摘要
            notificationManager.cancel(packageName.hashCode())
            
            Log.d(TAG, "Cancelled notifications for $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notifications", e)
        }
    }
    
    /**
     * 取消所有摘要通知
     */
    fun cancelAllSummaryNotifications() {
        try {
            notificationManager.cancelAll()
            Log.d(TAG, "All summary notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling all notifications", e)
        }
    }
    
    /**
     * 检查是否启用了持久化高优先级通知
     */
    fun isPersistentHighPriorityEnabled(): Boolean {
        return sharedPreferences.getBoolean("persistent_high_priority", true)
    }
    
    /**
     * 设置持久化高优先级通知开关
     */
    fun setPersistentHighPriorityEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("persistent_high_priority", enabled)
            .apply()
    }
} 