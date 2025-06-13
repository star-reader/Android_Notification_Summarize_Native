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
import top.usagijin.summary.data.NotificationData
import java.text.SimpleDateFormat
import java.util.*

/**
 * 通知列表适配器
 */
class NotificationAdapter(
    private val onItemClick: (NotificationData) -> Unit
) : ListAdapter<NotificationData, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = createItemView(parent)
        return NotificationViewHolder(itemView)
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
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
        }
        
        // 应用图标
        val ivAppIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                rightMargin = 12
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            tag = "ivAppIcon"
        }
        container.addView(ivAppIcon)
        
        // 内容区域
        val contentLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = LinearLayout.VERTICAL
        }
        
        // 顶部：应用名称和时间
        val topLayout = LinearLayout(context).apply {
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
        topLayout.addView(tvAppName)
        
        val tvTime = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            tag = "tvTime"
        }
        topLayout.addView(tvTime)
        
        contentLayout.addView(topLayout)
        
        // 标题
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
        contentLayout.addView(tvTitle)
        
        // 内容
        val tvContent = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 2
            }
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            maxLines = 2
            tag = "tvContent"
        }
        contentLayout.addView(tvContent)
        
        container.addView(contentLayout)
        
        return container
    }
    
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class NotificationViewHolder(
        private val itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        
        // 查找子视图
        private val ivAppIcon: ImageView = itemView.findViewWithTag("ivAppIcon")
        private val tvAppName: TextView = itemView.findViewWithTag("tvAppName")
        private val tvTime: TextView = itemView.findViewWithTag("tvTime")
        private val tvTitle: TextView = itemView.findViewWithTag("tvTitle")
        private val tvContent: TextView = itemView.findViewWithTag("tvContent")
        
        fun bind(notification: NotificationData) {
            // 设置应用图标
            try {
                val packageManager = itemView.context.packageManager
                val appIcon = packageManager.getApplicationIcon(notification.packageName)
                ivAppIcon.setImageDrawable(appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                // 使用默认图标
                ivAppIcon.setImageResource(R.drawable.ic_notification)
            }
            
            // 设置应用名称
            tvAppName.text = notification.appName
            
            // 设置时间（仅显示时分）
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = dateFormat.parse(notification.time)
                tvTime.text = if (date != null) timeFormat.format(date) else notification.time
            } catch (e: Exception) {
                tvTime.text = notification.time
            }
            
            // 设置标题
            tvTitle.text = notification.title ?: "无标题"
            
            // 设置内容
            tvContent.text = notification.content ?: "无内容"
            
            // 设置点击事件
            itemView.setOnClickListener {
                onItemClick(notification)
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