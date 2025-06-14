package top.usagijin.summary.adapter

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import top.usagijin.summary.R
import top.usagijin.summary.data.NotificationData
import java.text.SimpleDateFormat
import java.util.*

/**
 * 通知列表适配器 - Material Design 3风格
 */
class NotificationAdapter(
    private val onItemClick: (NotificationData) -> Unit
) : ListAdapter<NotificationData, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class NotificationViewHolder(
        private val itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val imageAppIcon: ImageView = itemView.findViewById(R.id.imageAppIcon)
        private val textAppName: MaterialTextView = itemView.findViewById(R.id.textAppName)
        private val textTime: MaterialTextView = itemView.findViewById(R.id.textTime)
        private val textTitle: MaterialTextView = itemView.findViewById(R.id.textTitle)
        private val textContent: MaterialTextView = itemView.findViewById(R.id.textContent)
        private val cardImportanceIndicator: MaterialCardView = itemView.findViewById(R.id.cardImportanceIndicator)
        private val buttonMore: MaterialButton = itemView.findViewById(R.id.buttonMore)
        
        fun bind(notification: NotificationData) {
            // 设置应用图标
            setAppIcon(notification.packageName)
            
            // 设置应用名称
            textAppName.text = notification.appName
            
            // 设置时间（仅显示时分）
            textTime.text = formatTime(notification.time)
            
            // 设置标题
            textTitle.text = notification.title?.takeIf { it.isNotBlank() } ?: "无标题"
            
            // 设置内容
            textContent.text = notification.content?.takeIf { it.isNotBlank() } ?: "无内容"
            
            // 设置重要性指示器
            setImportanceIndicator(notification)
            
            // 设置点击事件
            itemView.setOnClickListener {
                onItemClick(notification)
            }
            
            buttonMore.setOnClickListener {
                onItemClick(notification)
            }
        }
        
        private fun setAppIcon(packageName: String) {
            try {
                val packageManager = itemView.context.packageManager
                val appIcon = packageManager.getApplicationIcon(packageName)
                imageAppIcon.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                // 使用更合适的默认应用图标
                imageAppIcon.setImageResource(R.drawable.ic_app_default)
            } catch (e: Exception) {
                // 其他异常也使用默认图标
                imageAppIcon.setImageResource(R.drawable.ic_app_default)
            }
        }
        
        private fun formatTime(timeString: String): String {
            return try {
                val date = dateFormat.parse(timeString)
                if (date != null) {
                    val now = Calendar.getInstance()
                    val notificationTime = Calendar.getInstance().apply { time = date }
                    
                    when {
                        // 今天
                        now.get(Calendar.DAY_OF_YEAR) == notificationTime.get(Calendar.DAY_OF_YEAR) &&
                        now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
                            timeFormat.format(date)
                        }
                        // 昨天
                        now.get(Calendar.DAY_OF_YEAR) - 1 == notificationTime.get(Calendar.DAY_OF_YEAR) &&
                        now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
                            "昨天"
                        }
                        // 更早
                        else -> {
                            SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
                        }
                    }
                } else {
                    timeString
                }
            } catch (e: Exception) {
                timeString
            }
        }
        
        private fun setImportanceIndicator(notification: NotificationData) {
            // 根据通知内容长度和类型判断重要性
            val importance = when {
                notification.isOngoing -> 3 // 持续通知为高重要性
                (notification.content?.length ?: 0) > 100 -> 2 // 长内容为中等重要性
                else -> 1 // 其他为低重要性
            }
            
            when (importance) {
                3 -> {
                    cardImportanceIndicator.visibility = View.VISIBLE
                    cardImportanceIndicator.setCardBackgroundColor(
                        itemView.context.getColor(R.color.importance_high)
                    )
                }
                2 -> {
                    cardImportanceIndicator.visibility = View.VISIBLE
                    cardImportanceIndicator.setCardBackgroundColor(
                        itemView.context.getColor(R.color.importance_medium)
                    )
                }
                else -> {
                    cardImportanceIndicator.visibility = View.GONE
                }
            }
        }
    }
    
    /**
     * DiffUtil回调用于高效更新列表
     */
    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationData>() {
        override fun areItemsTheSame(oldItem: NotificationData, newItem: NotificationData): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: NotificationData, newItem: NotificationData): Boolean {
            return oldItem == newItem
        }
    }
} 