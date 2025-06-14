package top.usagijin.summary.utils

import android.util.Log
import java.security.MessageDigest

/**
 * 加密工具类
 * 提供SHA256哈希计算等加密功能
 */
object CryptoUtils {
    
    private const val TAG = "CryptoUtils"
    
    /**
     * 计算字符串的SHA256哈希值
     * @param data 要计算哈希的字符串
     * @return SHA256哈希值的十六进制字符串
     */
    fun computeSHA256(data: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute SHA256 hash", e)
            ""
        }
    }
    
    /**
     * 验证SHA256哈希值
     * @param data 原始数据
     * @param expectedHash 期望的哈希值
     * @return 是否匹配
     */
    fun verifySHA256(data: String, expectedHash: String): Boolean {
        return try {
            val computedHash = computeSHA256(data)
            computedHash.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify SHA256 hash", e)
            false
        }
    }
} 