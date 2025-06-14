package top.usagijin.summary.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.usagijin.summary.api.ApiService
import top.usagijin.summary.utils.SecureTokenStorage

/**
 * Token刷新Worker
 * 使用WorkManager定期刷新JWT Token
 */
class TokenRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "TokenRefreshWorker"
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting token refresh...")
            
            val apiService = ApiService.getInstance(applicationContext)
            val tokenStorage = SecureTokenStorage.getInstance(applicationContext)
            
            // 尝试刷新Token
            val success = apiService.refreshToken()
            
            if (success) {
                Log.i(TAG, "Token refresh successful")
                Result.success()
            } else {
                Log.w(TAG, "Token refresh failed, will retry")
                Result.retry()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh worker failed", e)
            Result.retry()
        }
    }
} 