package top.usagijin.summary.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.usagijin.summary.api.ApiService

/**
 * 模型维护Worker
 * 定期检查和维护本地ONNX模型状态
 */
class TokenRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "ModelMaintenanceWorker"
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始模型状态检查...")
            
            val apiService = ApiService.getInstance(applicationContext)
            
            // 检查模型是否正常加载
            if (!apiService.isModelLoaded()) {
                Log.w(TAG, "模型未加载，尝试重新初始化...")
                val success = apiService.initializeModel()
                
                if (success) {
                    Log.i(TAG, "模型重新初始化成功")
                    Result.success()
                } else {
                    Log.e(TAG, "模型重新初始化失败")
                    Result.retry()
                }
            } else {
                Log.d(TAG, "模型状态正常")
                Result.success()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "模型维护任务失败", e)
            Result.retry()
        }
    }
} 