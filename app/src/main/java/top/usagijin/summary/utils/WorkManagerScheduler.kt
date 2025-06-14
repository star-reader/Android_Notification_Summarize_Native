package top.usagijin.summary.utils

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.*
import top.usagijin.summary.private_config.ApiConfig
import top.usagijin.summary.service.TokenRefreshWorker
import java.util.concurrent.TimeUnit

/**
 * WorkManager调度器
 * 管理Token刷新等后台任务
 */
object WorkManagerScheduler {
    
    private const val TAG = "WorkManagerScheduler"
    private const val TOKEN_REFRESH_WORK_NAME = "token_refresh_work"
    
    /**
     * 启动Token刷新定期任务
     */
    fun scheduleTokenRefresh(context: Context) {
        try {
            Log.d(TAG, "Scheduling token refresh work...")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val refreshRequest = PeriodicWorkRequestBuilder<TokenRefreshWorker>(
                ApiConfig.Token.REFRESH_INTERVAL_MINUTES, 
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TOKEN_REFRESH_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                refreshRequest
            )
            
            Log.i(TAG, "Token refresh work scheduled successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule token refresh work", e)
        }
    }
    
    /**
     * 取消Token刷新任务
     */
    fun cancelTokenRefresh(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(TOKEN_REFRESH_WORK_NAME)
            Log.i(TAG, "Token refresh work cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel token refresh work", e)
        }
    }
    
    /**
     * 立即执行Token刷新
     */
    fun executeTokenRefreshNow(context: Context) {
        try {
            Log.d(TAG, "Executing immediate token refresh...")
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val immediateRequest = OneTimeWorkRequestBuilder<TokenRefreshWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueue(immediateRequest)
            
            Log.i(TAG, "Immediate token refresh work enqueued")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute immediate token refresh", e)
        }
    }
    
    /**
     * 获取Token刷新工作状态
     */
    fun getTokenRefreshWorkStatus(context: Context): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(TOKEN_REFRESH_WORK_NAME)
    }
} 