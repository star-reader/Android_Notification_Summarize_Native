package top.usagijin.summary.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import top.usagijin.summary.data.SummaryData

/**
 * 摘要实体类，用于Room数据库存储
 */
@Entity(tableName = "summaries")
data class SummaryEntity(
    @PrimaryKey
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val summary: String,
    val importanceLevel: Int, // 1=低, 2=中, 3=高
    val time: String,
    val timestamp: Long = System.currentTimeMillis() // 用于排序和清理
) {
    /**
     * 转换为SummaryData
     */
    fun toSummaryData(): SummaryData {
        return SummaryData(
            id = id,
            packageName = packageName,
            appName = appName,
            title = title,
            summary = summary,
            importanceLevel = importanceLevel,
            time = time
        )
    }
}

/**
 * SummaryData扩展函数，转换为SummaryEntity
 */
fun SummaryData.toEntity(): SummaryEntity {
    return SummaryEntity(
        id = id,
        packageName = packageName,
        appName = appName,
        title = title,
        summary = summary,
        importanceLevel = importanceLevel,
        time = time
    )
}