package top.usagijin.summary.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 * 处理通知监听权限的检查和请求
 */
object PermissionHelper {
    
    private const val TAG = "PermissionHelper"
    
    /**
     * 权限状态枚举
     */
    enum class PermissionStatus {
        GRANTED,    // 已授权
        DENIED      // 未授权
    }
    
    /**
     * 完整权限状态
     */
    data class FullPermissionStatus(
        val notificationListener: PermissionStatus,
        val postNotifications: PermissionStatus,
        val allGranted: Boolean
    )
    
    /**
     * 检查通知监听权限是否已授权
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val componentName = ComponentName.unflattenFromString(name)
                if (componentName != null) {
                    if (TextUtils.equals(packageName, componentName.packageName)) {
                        Log.d(TAG, "Notification listener permission is granted")
                        return true
                    }
                }
            }
        }
        
        Log.d(TAG, "Notification listener permission is not granted")
        return false
    }
    
    /**
     * 检查发送通知权限是否已授权
     */
    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    /**
     * 获取通知监听权限状态
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        return if (isNotificationListenerEnabled(context)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }
    
    /**
     * 获取完整权限状态
     */
    fun getFullPermissionStatus(context: Context): FullPermissionStatus {
        val listenerStatus = getPermissionStatus(context)
        val postStatus = if (canPostNotifications(context)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
        
        return FullPermissionStatus(
            notificationListener = listenerStatus,
            postNotifications = postStatus,
            allGranted = listenerStatus == PermissionStatus.GRANTED && 
                        postStatus == PermissionStatus.GRANTED
        )
    }
    
    /**
     * 打开通知监听设置页面
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Opened notification listener settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification listener settings", e)
            // 备用方案：打开应用设置页面
            openAppSettings(context)
        }
    }
    
    /**
     * 打开应用设置页面
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened app settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
        }
    }
    
    /**
     * 检查是否需要显示权限说明
     */
    fun shouldShowPermissionRationale(context: Context): Boolean {
        // 如果用户之前拒绝过权限，显示说明
        val prefs = context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("permission_requested_before", false) && 
               !isNotificationListenerEnabled(context)
    }
    
    /**
     * 标记权限已请求过
     */
    fun markPermissionRequested(context: Context) {
        val prefs = context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("permission_requested_before", true).apply()
    }
    
    /**
     * 打开应用通知设置页面
     */
    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened notification settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification settings", e)
            openAppSettings(context)
        }
    }
    
    /**
     * 打开应用通知设置页面（别名方法）
     */
    fun openAppNotificationSettings(context: Context) {
        openNotificationSettings(context)
    }
    
    /**
     * 检查发送通知权限是否已授权（别名方法）
     */
    fun hasPostNotificationPermission(context: Context): Boolean {
        return canPostNotifications(context)
    }
    
    /**
     * 检查是否需要请求POST_NOTIFICATIONS权限
     */
    fun shouldRequestPostNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
    
    /**
     * 获取权限说明文本
     */
    fun getPermissionRationaleText(): String {
        return "为了正常工作，本应用需要以下权限：" +
                "\n\n📱 通知访问权限：读取和汇总您的通知" +
                "\n🔔 发送通知权限：显示智能摘要通知" +
                "\n\n请在设置中授予这些权限。" +
                "\n\n我们承诺：" +
                "\n• 只读取通知内容用于生成摘要" +
                "\n• 不会上传或分享您的个人信息" +
                "\n• 所有数据仅在本地处理"
    }
} 