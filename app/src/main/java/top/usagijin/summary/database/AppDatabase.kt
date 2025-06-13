package top.usagijin.summary.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

/**
 * 应用数据库
 */
@Database(
    entities = [NotificationEntity::class, SummaryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun notificationDao(): NotificationDao
    abstract fun summaryDao(): SummaryDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例（单例模式）
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_summary_database"
                )
                .fallbackToDestructiveMigration() // 开发阶段允许破坏性迁移
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 