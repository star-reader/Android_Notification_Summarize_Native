package top.usagijin.summary.adapter

import android.content.pm.PackageManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.usagijin.summary.R
import top.usagijin.summary.data.SummaryData
import top.usagijin.summary.utils.NotificationDisplayManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * 摘要列表适配器
 */
class SummaryAdapter(
    private val onItemClick: (SummaryData) -> Unit
) : ListAdapter<SummaryData, SummaryAdapter.SummaryViewHolder>(SummaryDiffCallback()) {
    
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val itemView = createItemView(parent)
        return SummaryViewHolder(itemView)
    }
    
    /**
     * 创建列表项视图
     */
    private fun createItemView(parent: ViewGroup): View {
        val context = parent.context
        
        // 创建主容器
        val container = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
        }
        
        // 顶部颜色条
        val colorBar = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                6
            )
            tag = "colorBar"
        }
        container.addView(colorBar)
        
        // 内容区域
        val contentLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        // 应用图标
        val ivAppIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                rightMargin = 12
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            tag = "ivAppIcon"
        }
        contentLayout.addView(ivAppIcon)
        
        // 文本内容区域
        val textLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = LinearLayout.VERTICAL
        }
        
        // 顶部行：应用名称、时间和置顶图标
        val topRow = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val tvAppName = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            tag = "tvAppName"
        }
        topRow.addView(tvAppName)
        
        val ivPersistent = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                rightMargin = 8
            }
            setImageResource(R.drawable.ic_pin)
            visibility = View.GONE
            tag = "ivPersistent"
        }
        topRow.addView(ivPersistent)
        
        val tvTime = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            tag = "tvTime"
        }
        topRow.addView(tvTime)
        
        textLayout.addView(topRow)
        
        // 摘要标题
        val tvTitle = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
            }
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            maxLines = 1
            tag = "tvTitle"
        }
        textLayout.addView(tvTitle)
        
        // 摘要内容
        val tvSummary = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 2
            }
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            maxLines = 2
            tag = "tvSummary"
        }
        textLayout.addView(tvSummary)
        
        contentLayout.addView(textLayout)
        
        // 重要性指示器
        val importanceIndicator = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(8, 40).apply {
                leftMargin = 8
            }
            tag = "importanceIndicator"
        }
        contentLayout.addView(importanceIndicator)
        
        container.addView(contentLayout)
        
        return container
    }
    
    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class SummaryViewHolder(
        private val itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        
        // 查找子视图
        private val colorBar: View = itemView.findViewWithTag("colorBar")
        private val ivAppIcon: ImageView = itemView.findViewWithTag("ivAppIcon")
        private val tvAppName: TextView = itemView.findViewWithTag("tvAppName")
        private val tvTime: TextView = itemView.findViewWithTag("tvTime")
        private val tvTitle: TextView = itemView.findViewWithTag("tvTitle")
        private val tvSummary: TextView = itemView.findViewWithTag("tvSummary")
        private val ivPersistent: ImageView = itemView.findViewWithTag("ivPersistent")
        private val importanceIndicator: View = itemView.findViewWithTag("importanceIndicator")
        
        fun bind(summary: SummaryData) {
            // 设置应用图标
            try {
                val packageManager = itemView.context.packageManager
                val appIcon = packageManager.getApplicationIcon(summary.packageName)
                ivAppIcon.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                // 使用默认图标
                ivAppIcon.setImageResource(R.drawable.ic_notification)
            }
            
            // 设置应用名称
            tvAppName.text = summary.appName
            
            // 设置时间
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = dateFormat.parse(summary.time)
                tvTime.text = if (date != null) timeFormat.format(date) else summary.time
            } catch (e: Exception) {
                tvTime.text = summary.time
            }
            
            // 设置摘要标题
            tvTitle.text = summary.title
            
            // 设置摘要内容
            tvSummary.text = summary.summary
            
            // 设置重要性指示器
            val importanceColor = when (summary.importanceLevel) {
                3 -> ContextCompat.getColor(itemView.context, R.color.importance_high)
                2 -> ContextCompat.getColor(itemView.context, R.color.importance_medium)
                else -> ContextCompat.getColor(itemView.context, R.color.importance_low)
            }
            importanceIndicator.setBackgroundColor(importanceColor)
            
            // 设置颜色条
            val displayManager = NotificationDisplayManager.getInstance(itemView.context)
            val appColor = displayManager.getAppColor(summary.packageName)
            colorBar.setBackgroundColor(appColor)
            
            // 设置持久化标识
            if (summary.importanceLevel == 3) {
                ivPersistent.visibility = View.VISIBLE
            } else {
                ivPersistent.visibility = View.GONE
            }
            
            // 设置点击事件
            itemView.setOnClickListener {
                onItemClick(summary)
            }
        }
    }
    
    /**
     * DiffUtil回调用于高效更新列表
     */
    private class SummaryDiffCallback : DiffUtil.ItemCallback<SummaryData>() {
        override fun areItemsTheSame(oldItem: SummaryData, newItem: SummaryData): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: SummaryData, newItem: SummaryData): Boolean {
            return oldItem == newItem
        }
    }
} 