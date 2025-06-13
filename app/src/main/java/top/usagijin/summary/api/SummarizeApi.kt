package top.usagijin.summary.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import top.usagijin.summary.data.SummarizeRequest
import top.usagijin.summary.data.SummarizeResponse

/**
 * 摘要API接口
 * 使用Retrofit定义网络请求接口
 */
interface SummarizeApi {
    
    /**
     * 调用摘要API
     * @param request 包含通知数据的请求体
     * @return 包含摘要和重要性级别的响应
     */
    @POST("summarize")
    suspend fun summarize(@Body request: SummarizeRequest): Response<SummarizeResponse>
}