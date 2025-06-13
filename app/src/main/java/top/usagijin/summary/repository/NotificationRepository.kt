package top.usagijin.summary.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import top.usagijin.summary.data.NotificationData
import top.usagijin.summary.data.SummaryData
import top.usagijin.summary.database.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 通知数据仓库，管理通知和摘要的本地存储
 * 使用单例模式确保全局唯一
 */
class NotificationRepository private constructor(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val notificationDao = database.notificationDao()
    private val summaryDao = database.summaryDao()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        @Volatile
        private var INSTANCE: NotificationRepository? = null
        
        /**
         * 获取仓库实例（单例模式）
         */
        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    // ========== 通知相关操作 ==========
    
    /**
     * 添加通知（带重复检查）
     */
    suspend fun addNotification(notification: NotificationData) {
        withContext(Dispatchers.IO) {
            // 检查是否已存在相同ID的通知
            if (notificationDao.isNotificationExists(notification.id) > 0) {
                return@withContext
            }
            
            // 检查是否有相似的最近通知（1分钟内）
            val similarNotification = notificationDao.findSimilarRecentNotification(
                notification.packageName,
                notification.title,
                notification.content
            )
            
            if (similarNotification != null) {
                // 如果找到相似通知，不重复插入
                return@withContext
            }
            
            notificationDao.insertNotification(notification.toEntity())
        }
    }
    
    /**
     * 批量添加通知
     */
    suspend fun addNotifications(notifications: List<NotificationData>) {
        withContext(Dispatchers.IO) {
            val entities = notifications.map { it.toEntity() }
            notificationDao.insertNotifications(entities)
        }
    }
    
    /**
     * 根据包名和时间范围获取通知
     * @param packageName 应用包名
     * @param timeRangeMs 时间范围（毫秒）
     */
    fun getNotificationsByPackage(packageName: String, timeRangeMs: Long): Flow<List<NotificationData>> {
        val threshold = calculateTimeThreshold(timeRangeMs)
        return notificationDao.getNotificationsByPackageAndTime(packageName, threshold)
            .map { entities -> entities.map { it.toNotificationData() } }
    }
    
    /**
     * 获取最近的通知
     */
    fun getRecentNotifications(limit: Int): Flow<List<NotificationData>> {
        return notificationDao.getRecentNotifications(limit)
            .map { entities -> entities.map { it.toNotificationData() } }
    }
    
    /**
     * 获取未处理的通知
     */
    fun getUnprocessedNotifications(limit: Int): Flow<List<NotificationData>> {
        return notificationDao.getUnprocessedNotifications(limit)
            .map { entities -> entities.map { it.toNotificationData() } }
    }
    
    /**
     * 根据包名获取所有通知
     */
    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationData>> {
        return notificationDao.getNotificationsByPackage(packageName)
            .map { entities -> entities.map { it.toNotificationData() } }
    }
    
    /**
     * 标记通知为已处理
     */
    suspend fun markNotificationsAsProcessed(notificationIds: List<String>) {
        withContext(Dispatchers.IO) {
            notificationDao.markNotificationsAsProcessed(notificationIds)
        }
    }
    
    /**
     * 根据ID获取通知
     */
    suspend fun getNotificationById(id: String): NotificationData? {
        return withContext(Dispatchers.IO) {
            notificationDao.getNotificationById(id)?.toNotificationData()
        }
    }
    
    // ========== 摘要相关操作 ==========
    
    /**
     * 添加摘要
     */
    suspend fun addSummary(summary: SummaryData) {
        withContext(Dispatchers.IO) {
            summaryDao.insertSummary(summary.toEntity())
        }
    }
    
    /**
     * 批量添加摘要
     */
    suspend fun addSummaries(summaries: List<SummaryData>) {
        withContext(Dispatchers.IO) {
            val entities = summaries.map { it.toEntity() }
            summaryDao.insertSummaries(entities)
        }
    }
    
    /**
     * 获取最近的摘要
     */
    fun getRecentSummaries(limit: Int): Flow<List<SummaryData>> {
        return summaryDao.getRecentSummaries(limit)
            .map { entities -> entities.map { it.toSummaryData() } }
    }
    
    /**
     * 根据包名获取摘要
     */
    fun getSummariesByPackage(packageName: String): Flow<List<SummaryData>> {
        return summaryDao.getSummariesByPackage(packageName)
            .map { entities -> entities.map { it.toSummaryData() } }
    }
    
    /**
     * 获取高重要性摘要（用于持久化通知）
     */
    fun getHighImportanceSummaries(): Flow<List<SummaryData>> {
        return summaryDao.getHighImportanceSummaries()
            .map { entities -> entities.map { it.toSummaryData() } }
    }
    
    /**
     * 根据ID获取摘要
     */
    suspend fun getSummaryById(id: String): SummaryData? {
        return withContext(Dispatchers.IO) {
            summaryDao.getSummaryById(id)?.toSummaryData()
        }
    }
    
    // ========== 数据清理 ==========
    
    /**
     * 清理旧数据（7天前的通知和摘要）
     */
    suspend fun cleanOldData() {
        withContext(Dispatchers.IO) {
            notificationDao.deleteOldNotifications()
            summaryDao.deleteOldSummaries()
        }
    }
    
    /**
     * 获取数据统计
     */
    suspend fun getDataStats(): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val notificationCount = notificationDao.getNotificationCount()
            val summaryCount = summaryDao.getSummaryCount()
            Pair(notificationCount, summaryCount)
        }
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 计算时间阈值
     */
    private fun calculateTimeThreshold(timeRangeMs: Long): String {
        val threshold = System.currentTimeMillis() - timeRangeMs
        return dateFormat.format(Date(threshold))
    }
} 