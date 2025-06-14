package top.usagijin.summary.data

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

/**
 * 通知数据的密封类，确保类型安全
 */
sealed class NotificationData : Parcelable {
    abstract val id: String
    abstract val packageName: String
    abstract val appName: String
    abstract val title: String?
    abstract val content: String?
    abstract val time: String
    abstract val isOngoing: Boolean

    @Parcelize
    data class Standard(
        override val id: String, // 格式: "${packageName}_${sbn.id}_${sbn.postTime}"
        override val packageName: String,
        override val appName: String,
        override val title: String?,
        override val content: String?,
        override val time: String, // 格式: "yyyy-MM-dd HH:mm:ss"
        override val isOngoing: Boolean
    ) : NotificationData()
}

/**
 * 摘要数据类
 */
@Parcelize
data class SummaryData(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val summary: String,
    val importanceLevel: Int, // 1-5级，1=最低，5=最高（优先通知）
    val time: String // 格式: "yyyy-MM-dd HH:mm:ss"
) : Parcelable

/**
 * API请求数据类
 */
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

/**
 * API响应数据类
 */
data class SummarizeResponse(
    val title: String,
    val summary: String,
    val importanceLevel: Int
) 