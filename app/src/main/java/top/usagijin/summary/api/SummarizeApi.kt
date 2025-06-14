package top.usagijin.summary.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import top.usagijin.summary.data.*
import top.usagijin.summary.private_config.ApiConfig

/**
 * 摘要API接口
 * 使用Retrofit定义网络请求接口，符合API文档要求
 */
interface SummarizeApi {
    
    /**
     * 获取JWT Token
     * @param request 包含客户端ID和密钥的请求体
     * @return JWT Token响应
     */
    @POST(ApiConfig.Endpoints.AUTH_TOKEN)
    suspend fun getToken(@Body request: TokenRequest): Response<TokenResponse>
    
    /**
     * 获取Apply ID
     * @return Apply ID响应
     */
    @POST(ApiConfig.Endpoints.API_CONNECT)
    suspend fun getApplyId(): Response<ConnectResponse>
    
    /**
     * 生成摘要
     * @param request 包含通知数据、Apply ID和验证哈希的请求体
     * @return 摘要响应
     */
    @POST(ApiConfig.Endpoints.API_GENERATE)
    suspend fun generateSummary(@Body request: SummarizeApiRequest): Response<SummarizeApiResponse>
}