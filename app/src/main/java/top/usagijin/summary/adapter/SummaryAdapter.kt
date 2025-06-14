package top.usagijin.summary.adapter

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import top.usagijin.summary.R
import top.usagijin.summary.data.SummaryData
import java.text.SimpleDateFormat
import java.util.*

/**
 * 摘要适配器 - Material Design 3风格
 */
class SummaryAdapter(
    private val onItemClick: (SummaryData) -> Unit
) : ListAdapter<SummaryData, SummaryAdapter.SummaryViewHolder>(SummaryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary, parent, false)
        return SummaryViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class SummaryViewHolder(
        itemView: View,
        private val onItemClick: (SummaryData) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardSummary)
        private val appIcon: ImageView = itemView.findViewById(R.id.imageAppIcon)
        private val appName: MaterialTextView = itemView.findViewById(R.id.textAppName)
        private val summaryTitle: MaterialTextView = itemView.findViewById(R.id.textSummaryTitle)
        private val summaryContent: MaterialTextView = itemView.findViewById(R.id.textSummaryContent)
        private val timestamp: MaterialTextView = itemView.findViewById(R.id.textTimestamp)
        private val importanceIndicator: View = itemView.findViewById(R.id.viewImportanceIndicator)

        fun bind(summary: SummaryData) {
            // 设置应用图标
            try {
                val packageManager = itemView.context.packageManager
                val appInfo = packageManager.getApplicationInfo(summary.packageName, 0)
                val icon = packageManager.getApplicationIcon(appInfo)
                appIcon.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                // 使用更合适的默认应用图标
                appIcon.setImageResource(R.drawable.ic_app_default)
            } catch (e: Exception) {
                // 其他异常也使用默认图标
                appIcon.setImageResource(R.drawable.ic_app_default)
            }

            // 设置应用名称
            appName.text = summary.appName

            // 设置摘要标题
            summaryTitle.text = summary.title ?: "智能摘要"

            // 设置摘要内容
            summaryContent.text = summary.summary

            // 设置时间戳
            timestamp.text = formatTime(summary.time)

            // 设置重要性指示器
            val importanceColor = when (summary.importanceLevel) {
                3 -> androidx.core.content.ContextCompat.getColor(itemView.context, R.color.importance_high)
                2 -> androidx.core.content.ContextCompat.getColor(itemView.context, R.color.importance_medium)
                else -> androidx.core.content.ContextCompat.getColor(itemView.context, R.color.importance_low)
            }
            importanceIndicator.setBackgroundColor(importanceColor)

            // 设置点击事件
            cardView.setOnClickListener {
                onItemClick(summary)
            }
        }

        private fun formatTime(timeString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(timeString)
                
                val now = Date()
                val diff = now.time - (date?.time ?: 0)
                
                when {
                    diff < 60 * 1000 -> "刚刚"
                    diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                    diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                    else -> {
                        val outputFormat = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault())
                        outputFormat.format(date ?: Date())
                    }
                }
            } catch (e: Exception) {
                timeString
            }
        }
    }
    
    class SummaryDiffCallback : DiffUtil.ItemCallback<SummaryData>() {
        override fun areItemsTheSame(oldItem: SummaryData, newItem: SummaryData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SummaryData, newItem: SummaryData): Boolean {
            return oldItem == newItem
        }
    }
} 