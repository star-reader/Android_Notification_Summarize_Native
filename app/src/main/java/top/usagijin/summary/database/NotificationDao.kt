package top.usagijin.summary.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 通知数据访问对象
 */
@Dao
interface NotificationDao {
    
    /**
     * 插入通知
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)
    
    /**
     * 批量插入通知
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)
    
    /**
     * 根据包名和时间范围获取通知
     */
    @Query("""
        SELECT * FROM notifications 
        WHERE packageName = :packageName 
        AND datetime(time) >= datetime(:timeThreshold)
        ORDER BY time DESC
    """)
    fun getNotificationsByPackageAndTime(
        packageName: String, 
        timeThreshold: String
    ): Flow<List<NotificationEntity>>
    
    /**
     * 获取最近的通知
     */
    @Query("""
        SELECT * FROM notifications 
        ORDER BY time DESC 
        LIMIT :limit
    """)
    fun getRecentNotifications(limit: Int): Flow<List<NotificationEntity>>
    
    /**
     * 获取未处理的通知
     */
    @Query("""
        SELECT * FROM notifications 
        WHERE isProcessed = 0 
        ORDER BY time DESC 
        LIMIT :limit
    """)
    fun getUnprocessedNotifications(limit: Int): Flow<List<NotificationEntity>>
    
    /**
     * 根据包名获取通知
     */
    @Query("""
        SELECT * FROM notifications 
        WHERE packageName = :packageName 
        ORDER BY time DESC
    """)
    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationEntity>>
    
    /**
     * 标记通知为已处理
     */
    @Query("UPDATE notifications SET isProcessed = 1 WHERE id IN (:ids)")
    suspend fun markNotificationsAsProcessed(ids: List<String>)
    
    /**
     * 删除旧通知（7天前的）
     */
    @Query("""
        DELETE FROM notifications 
        WHERE datetime(time) < datetime('now', '-7 days')
    """)
    suspend fun deleteOldNotifications()
    
    /**
     * 获取通知总数
     */
    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getNotificationCount(): Int
    
    /**
     * 根据ID获取通知
     */
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationEntity?
    
    /**
     * 检查通知是否已存在
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE id = :id")
    suspend fun isNotificationExists(id: String): Int
    
    /**
     * 根据包名、标题和内容查找相似通知（防止重复）
     */
    @Query("""
        SELECT * FROM notifications 
        WHERE packageName = :packageName 
        AND title = :title 
        AND content = :content 
        AND datetime(time) >= datetime('now', '-1 minutes')
        LIMIT 1
    """)
    suspend fun findSimilarRecentNotification(
        packageName: String, 
        title: String?, 
        content: String?
    ): NotificationEntity?
} 