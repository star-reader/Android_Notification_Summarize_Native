package top.usagijin.summary.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import top.usagijin.summary.data.NotificationData

/**
 * 通知实体类，用于Room数据库存储
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val content: String?,
    val time: String,
    val isOngoing: Boolean,
    val isProcessed: Boolean = false // 是否已处理，用于标记是否已生成摘要
) {
    /**
     * 转换为NotificationData
     */
    fun toNotificationData(): NotificationData.Standard {
        return NotificationData.Standard(
            id = id,
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            time = time,
            isOngoing = isOngoing
        )
    }
}

/**
 * NotificationData扩展函数，转换为NotificationEntity
 */
fun NotificationData.toEntity(isProcessed: Boolean = false): NotificationEntity {
    return NotificationEntity(
        id = id,
        packageName = packageName,
        appName = appName,
        title = title,
        content = content,
        time = time,
        isOngoing = isOngoing,
        isProcessed = isProcessed
    )
} 