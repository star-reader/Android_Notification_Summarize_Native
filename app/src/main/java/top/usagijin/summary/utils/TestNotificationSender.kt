package top.usagijin.summary.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import top.usagijin.summary.R
import kotlinx.coroutines.delay

/**
 * 测试通知发送器
 * 用于发送测试通知来验证摘要功能
 */
object TestNotificationSender {
    
    private const val CHANNEL_ID = "test_notifications"
    private const val CHANNEL_NAME = "测试通知"
    
    /**
     * 初始化通知渠道
     */
    fun initializeChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "用于测试摘要功能的通知渠道"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 发送单条长通知（测试场景1）
     */
    fun sendLongNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("长通知测试")
            .setContentText("这是一条很长的测试通知内容，用于测试单条长通知的摘要功能。内容超过26个字符，应该会触发摘要生成。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(1001, notification)
    }
    
    /**
     * 发送多条短通知（测试场景2）
     */
    suspend fun sendMultipleNotifications(context: Context) {
        val notifications = listOf(
            "第一条测试消息",
            "第二条测试消息",
            "第三条测试消息"
        )
        
        notifications.forEachIndexed { index, content ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("多通知测试 ${index + 1}")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(2000 + index, notification)
            
            // 间隔2秒发送
            if (index < notifications.size - 1) {
                delay(2000)
            }
        }
    }
    
    /**
     * 发送高频通知（测试场景5）
     */
    suspend fun sendHighFrequencyNotifications(context: Context) {
        repeat(12) { index ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("高频通知 ${index + 1}")
                .setContentText("高频测试消息 ${index + 1}")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(3000 + index, notification)
            
            // 间隔500毫秒发送
            delay(500)
        }
    }
    
    /**
     * 模拟微信通知
     */
    fun sendWeChatLikeNotification(context: Context, content: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("微信")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * 模拟邮件通知
     */
    fun sendEmailLikeNotification(context: Context, subject: String, content: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(subject)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
} 