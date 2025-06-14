package top.usagijin.summary.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.io.File

/**
 * 内存管理工具类
 */
object MemoryUtils {
    private const val TAG = "MemoryUtils"
    
    /**
     * 获取详细的内存信息
     */
    fun getMemoryInfo(context: Context): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        return MemoryInfo(
            maxHeapMB = runtime.maxMemory() / (1024 * 1024),
            totalHeapMB = runtime.totalMemory() / (1024 * 1024),
            freeHeapMB = runtime.freeMemory() / (1024 * 1024),
            usedHeapMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
            availableHeapMB = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / (1024 * 1024),
            totalRAMMB = memInfo.totalMem / (1024 * 1024),
            availableRAMMB = memInfo.availMem / (1024 * 1024),
            isLowMemory = memInfo.lowMemory,
            memoryClass = activityManager.memoryClass,
            largeMemoryClass = activityManager.largeMemoryClass
        )
    }
    
    /**
     * 检查是否有足够内存加载大型模型
     */
    fun checkMemoryForLargeModel(context: Context, requiredMB: Long = 2048): Boolean {
        val memInfo = getMemoryInfo(context)
        
        Log.i(TAG, "内存检查 - 需要: ${requiredMB}MB")
        Log.i(TAG, "可用堆内存: ${memInfo.availableHeapMB}MB")
        Log.i(TAG, "可用系统内存: ${memInfo.availableRAMMB}MB")
        Log.i(TAG, "内存类别: ${memInfo.memoryClass}MB (大堆: ${memInfo.largeMemoryClass}MB)")
        
        return memInfo.availableHeapMB >= requiredMB && 
               memInfo.availableRAMMB >= requiredMB && 
               !memInfo.isLowMemory
    }
    
    /**
     * 强制垃圾回收
     */
    fun forceGarbageCollection() {
        System.gc()
        Thread.sleep(100)
        System.runFinalization()
        System.gc()
    }
    
    /**
     * 获取可用存储空间
     */
    fun getAvailableStorageSpace(context: Context): Long {
        val internalDir = context.filesDir
        return internalDir.usableSpace / (1024 * 1024) // MB
    }
    
    /**
     * 内存信息数据类
     */
    data class MemoryInfo(
        val maxHeapMB: Long,
        val totalHeapMB: Long,
        val freeHeapMB: Long,
        val usedHeapMB: Long,
        val availableHeapMB: Long,
        val totalRAMMB: Long,
        val availableRAMMB: Long,
        val isLowMemory: Boolean,
        val memoryClass: Int,
        val largeMemoryClass: Int
    ) {
        override fun toString(): String {
            return "堆内存: ${usedHeapMB}MB/${maxHeapMB}MB (可用: ${availableHeapMB}MB), " +
                   "系统内存: ${(totalRAMMB - availableRAMMB)}MB/${totalRAMMB}MB (可用: ${availableRAMMB}MB), " +
                   "内存类别: ${memoryClass}MB/${largeMemoryClass}MB, " +
                   "内存压力: ${if (isLowMemory) "高" else "正常"}"
        }
    }
} 