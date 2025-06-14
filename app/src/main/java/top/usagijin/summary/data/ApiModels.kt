package top.usagijin.summary.data

import com.google.gson.annotations.SerializedName

/**
 * Token认证相关数据模型
 */
data class TokenRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("client_secret")
    val clientSecret: String
)

data class TokenResponse(
    @SerializedName("token")
    val token: String
)

/**
 * Apply ID相关数据模型
 */
data class ConnectResponse(
    @SerializedName("applyId")
    val applyId: String
)

/**
 * 摘要API相关数据模型
 */
data class SummarizeApiRequest(
    @SerializedName("data")
    val data: NotificationDataWrapper,
    @SerializedName("applyId")
    val applyId: String,
    @SerializedName("verify")
    val verify: String
)

data class NotificationDataWrapper(
    @SerializedName("currentTime")
    val currentTime: String,
    @SerializedName("data")
    val data: List<NotificationApiInput>
)

data class NotificationApiInput(
    @SerializedName("title")
    val title: String?,
    @SerializedName("content")
    val content: String?,
    @SerializedName("time")
    val time: String,
    @SerializedName("packageName")
    val packageName: String
)

data class SummarizeApiResponse(
    @SerializedName("title")
    val title: String,
    @SerializedName("summary")
    val summary: String,
    @SerializedName("importanceLevel")
    val importanceLevel: Int // 1-5级，5级最高
)

/**
 * 错误响应数据模型
 */
data class ApiErrorResponse(
    @SerializedName("error")
    val error: String
) 