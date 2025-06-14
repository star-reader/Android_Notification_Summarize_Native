package top.usagijin.summary.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import top.usagijin.summary.private_config.ApiConfig

/**
 * 安全Token存储工具类
 * 暂时使用普通SharedPreferences存储JWT Token
 * TODO: 网络问题解决后改为EncryptedSharedPreferences
 */
class SecureTokenStorage private constructor(private val context: Context) {
    
    private val TAG = "SecureTokenStorage"
    
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(ApiConfig.Token.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SecureTokenStorage? = null
        
        fun getInstance(context: Context): SecureTokenStorage {
            return INSTANCE ?: synchronized(this) {
                val instance = SecureTokenStorage(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * 保存JWT Token
     */
    fun saveToken(token: String) {
        try {
            sharedPrefs.edit()
                .putString(ApiConfig.Token.TOKEN_KEY, token)
                .putLong(ApiConfig.Token.TOKEN_TIMESTAMP_KEY, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "Token saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save token", e)
        }
    }
    
    /**
     * 获取存储的JWT Token
     */
    fun getToken(): String? {
        return try {
            sharedPrefs.getString(ApiConfig.Token.TOKEN_KEY, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get token", e)
            null
        }
    }
    
    /**
     * 检查Token是否存在
     */
    fun hasToken(): Boolean {
        return !getToken().isNullOrEmpty()
    }
    
    /**
     * 检查Token是否需要刷新
     * @return true如果需要刷新
     */
    fun isTokenExpired(): Boolean {
        return try {
            val timestamp = sharedPrefs.getLong(ApiConfig.Token.TOKEN_TIMESTAMP_KEY, 0)
            val currentTime = System.currentTimeMillis()
            val elapsedMinutes = (currentTime - timestamp) / (1000 * 60)
            
            elapsedMinutes >= ApiConfig.Token.REFRESH_INTERVAL_MINUTES
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check token expiration", e)
            true // 出错时认为已过期
        }
    }
    
    /**
     * 清除Token
     */
    fun clearToken() {
        try {
            sharedPrefs.edit()
                .remove(ApiConfig.Token.TOKEN_KEY)
                .remove(ApiConfig.Token.TOKEN_TIMESTAMP_KEY)
                .apply()
            Log.d(TAG, "Token cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear token", e)
        }
    }
    
    /**
     * 获取Token保存时间戳
     */
    fun getTokenTimestamp(): Long {
        return try {
            sharedPrefs.getLong(ApiConfig.Token.TOKEN_TIMESTAMP_KEY, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get token timestamp", e)
            0
        }
    }
} 