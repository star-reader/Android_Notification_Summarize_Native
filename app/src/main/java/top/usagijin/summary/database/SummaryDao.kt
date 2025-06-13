package top.usagijin.summary.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 摘要数据访问对象
 */
@Dao
interface SummaryDao {
    
    /**
     * 插入摘要
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)
    
    /**
     * 批量插入摘要
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<SummaryEntity>)
    
    /**
     * 获取最近的摘要
     */
    @Query("""
        SELECT * FROM summaries 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """)
    fun getRecentSummaries(limit: Int): Flow<List<SummaryEntity>>
    
    /**
     * 根据包名获取摘要
     */
    @Query("""
        SELECT * FROM summaries 
        WHERE packageName = :packageName 
        ORDER BY timestamp DESC
    """)
    fun getSummariesByPackage(packageName: String): Flow<List<SummaryEntity>>
    
    /**
     * 根据重要性级别获取摘要
     */
    @Query("""
        SELECT * FROM summaries 
        WHERE importanceLevel >= :minImportanceLevel 
        ORDER BY timestamp DESC
    """)
    fun getSummariesByImportance(minImportanceLevel: Int): Flow<List<SummaryEntity>>
    
    /**
     * 获取高重要性摘要（用于持久化通知）
     */
    @Query("""
        SELECT * FROM summaries 
        WHERE importanceLevel = 3 
        ORDER BY timestamp DESC
    """)
    fun getHighImportanceSummaries(): Flow<List<SummaryEntity>>
    
    /**
     * 删除旧摘要（7天前的）
     */
    @Query("""
        DELETE FROM summaries 
        WHERE datetime(time) < datetime('now', '-7 days')
    """)
    suspend fun deleteOldSummaries()
    
    /**
     * 获取摘要总数
     */
    @Query("SELECT COUNT(*) FROM summaries")
    suspend fun getSummaryCount(): Int
    
    /**
     * 根据ID获取摘要
     */
    @Query("SELECT * FROM summaries WHERE id = :id")
    suspend fun getSummaryById(id: String): SummaryEntity?
    
    /**
     * 删除指定包名的摘要
     */
    @Query("DELETE FROM summaries WHERE packageName = :packageName")
    suspend fun deleteSummariesByPackage(packageName: String)
    
    /**
     * 获取所有不同的包名
     */
    @Query("SELECT DISTINCT packageName FROM summaries")
    suspend fun getAllPackageNames(): List<String>
} 