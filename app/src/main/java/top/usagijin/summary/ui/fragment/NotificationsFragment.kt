package top.usagijin.summary.ui.fragment

import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import top.usagijin.summary.R
import top.usagijin.summary.data.NotificationData
import top.usagijin.summary.utils.PermissionHelper
import top.usagijin.summary.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * iOS风格通知页面 Fragment
 * 完全模仿最新iOS设计风格
 */
class NotificationsFragment : Fragment() {
    
    private val TAG = "NotificationsFragment"
    private val viewModel: MainViewModel by viewModels()
    
    private lateinit var rootLayout: LinearLayout
    private lateinit var contentContainer: LinearLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        
        return try {
            createiOSStyleRootView()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating view", e)
            createErrorView()
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")
        
        setupObservers()
        loadNotifications()
    }
    
    /**
     * 创建iOS风格根视图
     */
    private fun createiOSStyleRootView(): View {
        val context = requireContext()
        
        // 创建根布局
        rootLayout = LinearLayout(context)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        // iOS风格的分组背景色
        rootLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.ios_system_grouped_background))
        
        // 创建滚动视图
        val scrollView = ScrollView(context)
        scrollView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        // iOS风格的边距
        scrollView.setPadding(16, 16, 16, 16)
        scrollView.isVerticalScrollBarEnabled = false
        scrollView.overScrollMode = View.OVER_SCROLL_NEVER
        
        // 创建内容容器
        contentContainer = LinearLayout(context)
        contentContainer.orientation = LinearLayout.VERTICAL
        contentContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        scrollView.addView(contentContainer)
        rootLayout.addView(scrollView)
        
        return rootLayout
    }
    
    /**
     * 创建错误视图
     */
    private fun createErrorView(): View {
        val context = requireContext()
        
        val errorLayout = LinearLayout(context)
        errorLayout.orientation = LinearLayout.VERTICAL
        errorLayout.gravity = Gravity.CENTER
        errorLayout.setPadding(32, 32, 32, 32)
        errorLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.ios_system_grouped_background))
        
        val errorText = TextView(context)
        errorText.text = "⚠️ 加载通知时出错"
        errorText.textSize = 17f // iOS标准字体大小
        errorText.setTextColor(ContextCompat.getColor(context, R.color.ios_red))
        errorText.gravity = Gravity.CENTER
        errorText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        
        errorLayout.addView(errorText)
        return errorLayout
    }
    
    /**
     * 设置观察者
     */
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notifications.collect { notifications ->
                Log.d(TAG, "Received ${notifications.size} notifications")
                updateUI(notifications)
            }
        }
    }
    
    /**
     * 加载通知数据
     */
    private fun loadNotifications() {
        Log.d(TAG, "Loading notifications...")
        viewModel.refreshData()
    }
    
    /**
     * 更新UI
     */
    private fun updateUI(notifications: List<NotificationData>) {
        try {
            contentContainer.removeAllViews()
            
            // 检查权限状态
            if (!PermissionHelper.isNotificationListenerEnabled(requireContext())) {
                val permissionCard = createiOSPermissionCard()
                contentContainer.addView(permissionCard)
                
                // iOS风格的间距
                val spacer = View(requireContext())
                val spacerParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    20
                )
                spacer.layoutParams = spacerParams
                contentContainer.addView(spacer)
            }
            
            if (notifications.isEmpty()) {
                val emptyView = createiOSEmptyStateView()
                contentContainer.addView(emptyView)
            } else {
                // 按应用分组显示通知（iOS风格）
                val groupedNotifications = notifications.groupBy { it.packageName }
                
                groupedNotifications.entries.forEachIndexed { groupIndex, (packageName, notificationList) ->
                    val groupCard = createiOSNotificationGroup(packageName, notificationList)
                    contentContainer.addView(groupCard)
                    
                    // 添加组间距
                    if (groupIndex < groupedNotifications.size - 1) {
                        val spacer = View(requireContext())
                        val spacerParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            20
                        )
                        spacer.layoutParams = spacerParams
                        contentContainer.addView(spacer)
                    }
                    
                    // 添加入场动画
                    animateiOSCardEntrance(groupCard, groupIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI", e)
        }
    }
    
    /**
     * 创建iOS风格通知组
     */
    private fun createiOSNotificationGroup(packageName: String, notifications: List<NotificationData>): View {
        val context = requireContext()
        
        // 创建组容器
        val groupContainer = LinearLayout(context)
        groupContainer.orientation = LinearLayout.VERTICAL
        groupContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 创建iOS风格的卡片
        val card = MaterialCardView(context)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        card.radius = 16f // iOS风格的大圆角
        card.cardElevation = 0f // iOS不使用阴影
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.ios_secondary_system_grouped_background))
        card.strokeWidth = 0
        
        // 创建卡片内容
        val cardContent = LinearLayout(context)
        cardContent.orientation = LinearLayout.VERTICAL
        cardContent.setPadding(0, 0, 0, 0) // iOS风格无内边距
        
        // 添加每个通知
        notifications.forEachIndexed { index, notification ->
            val notificationItem = createiOSNotificationItem(notification, index == 0, index == notifications.size - 1)
            cardContent.addView(notificationItem)
            
            // 添加分隔线（除了最后一个）
            if (index < notifications.size - 1) {
                val separator = createiOSSeparator()
                cardContent.addView(separator)
            }
        }
        
        card.addView(cardContent)
        groupContainer.addView(card)
        
        return groupContainer
    }
    
    /**
     * 创建iOS风格通知项
     */
    private fun createiOSNotificationItem(notification: NotificationData, isFirst: Boolean, isLast: Boolean): View {
        val context = requireContext()
        
        val itemContainer = LinearLayout(context)
        itemContainer.orientation = LinearLayout.HORIZONTAL
        itemContainer.gravity = Gravity.CENTER_VERTICAL
        itemContainer.setPadding(16, 12, 16, 12) // iOS标准内边距
        itemContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 添加点击效果
        val rippleDrawable = GradientDrawable()
        rippleDrawable.setColor(Color.TRANSPARENT)
        if (isFirst && isLast) {
            rippleDrawable.cornerRadius = 16f
        } else if (isFirst) {
            rippleDrawable.cornerRadii = floatArrayOf(16f, 16f, 16f, 16f, 0f, 0f, 0f, 0f)
        } else if (isLast) {
            rippleDrawable.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 16f, 16f, 16f, 16f)
        }
        itemContainer.background = rippleDrawable
        itemContainer.isClickable = true
        itemContainer.isFocusable = true
        
        // 左侧：应用图标
        val appIcon = createiOSAppIcon(notification)
        itemContainer.addView(appIcon)
        
        // 中间：内容区域
        val contentArea = createiOSContentArea(notification)
        itemContainer.addView(contentArea)
        
        // 右侧：时间和状态
        val rightArea = createiOSRightArea(notification)
        itemContainer.addView(rightArea)
        
        return itemContainer
    }
    
    /**
     * 创建iOS风格应用图标
     */
    private fun createiOSAppIcon(notification: NotificationData): View {
        val context = requireContext()
        
        val iconContainer = LinearLayout(context)
        iconContainer.orientation = LinearLayout.VERTICAL
        iconContainer.gravity = Gravity.CENTER
        val containerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        containerParams.rightMargin = 12
        iconContainer.layoutParams = containerParams
        
        // 应用图标
        val appIcon = ImageView(context)
        val iconSize = 40 // iOS标准图标大小
        val iconParams = LinearLayout.LayoutParams(iconSize, iconSize)
        appIcon.layoutParams = iconParams
        appIcon.scaleType = ImageView.ScaleType.CENTER_CROP
        
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(notification.packageName, 0)
            val icon = packageManager.getApplicationIcon(appInfo)
            appIcon.setImageDrawable(icon)
        } catch (e: Exception) {
            appIcon.setImageResource(R.drawable.ic_notification)
            appIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.ios_gray)
        }
        
        // iOS风格的圆角图标
        val iconDrawable = GradientDrawable()
        iconDrawable.cornerRadius = 10f // iOS应用图标圆角
        iconDrawable.setColor(Color.TRANSPARENT)
        appIcon.background = iconDrawable
        appIcon.clipToOutline = true
        
        iconContainer.addView(appIcon)
        
        return iconContainer
    }
    
    /**
     * 创建iOS风格内容区域
     */
    private fun createiOSContentArea(notification: NotificationData): View {
        val context = requireContext()
        
        val contentArea = LinearLayout(context)
        contentArea.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        contentArea.layoutParams = contentParams
        
        // 应用名称
        val appNameView = TextView(context)
        appNameView.text = notification.appName
        appNameView.textSize = 15f // iOS标准字体大小
        appNameView.setTextColor(ContextCompat.getColor(context, R.color.ios_label))
        appNameView.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        appNameView.maxLines = 1
        appNameView.ellipsize = android.text.TextUtils.TruncateAt.END
        val appNameParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        appNameParams.bottomMargin = 2
        appNameView.layoutParams = appNameParams
        contentArea.addView(appNameView)
        
        // 通知内容（合并标题和内容）
        val contentText = buildString {
            if (!notification.title.isNullOrBlank()) {
                append(notification.title)
                if (!notification.content.isNullOrBlank()) {
                    append(": ")
                }
            }
            if (!notification.content.isNullOrBlank()) {
                append(notification.content)
            }
        }
        
        if (contentText.isNotBlank()) {
            val contentView = TextView(context)
            contentView.text = contentText
            contentView.textSize = 13f // iOS副标题字体大小
            contentView.setTextColor(ContextCompat.getColor(context, R.color.ios_secondary_label))
            contentView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            contentView.setLineSpacing(0f, 1.2f)
            contentView.maxLines = 2
            contentView.ellipsize = android.text.TextUtils.TruncateAt.END
            contentArea.addView(contentView)
        }
        
        return contentArea
    }
    
    /**
     * 创建iOS风格右侧区域
     */
    private fun createiOSRightArea(notification: NotificationData): View {
        val context = requireContext()
        
        val rightArea = LinearLayout(context)
        rightArea.orientation = LinearLayout.VERTICAL
        rightArea.gravity = Gravity.TOP or Gravity.END
        val rightParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rightParams.leftMargin = 8
        rightArea.layoutParams = rightParams
        
        // 时间
        val timeView = TextView(context)
        timeView.text = formatiOSTime(notification.time)
        timeView.textSize = 13f // iOS时间字体大小
        timeView.setTextColor(ContextCompat.getColor(context, R.color.ios_secondary_label))
        timeView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        timeView.gravity = Gravity.END
        rightArea.addView(timeView)
        
        // iOS风格的状态指示器
        if (notification.isOngoing) {
            val statusView = TextView(context)
            statusView.text = "●"
            statusView.textSize = 8f
            statusView.setTextColor(ContextCompat.getColor(context, R.color.ios_blue))
            statusView.gravity = Gravity.END
            val statusParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            statusParams.topMargin = 4
            statusView.layoutParams = statusParams
            rightArea.addView(statusView)
        }
        
        return rightArea
    }
    
    /**
     * 创建iOS风格分隔线
     */
    private fun createiOSSeparator(): View {
        val context = requireContext()
        
        val separator = View(context)
        val separatorParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            1
        )
        separatorParams.leftMargin = 68 // 对齐内容区域
        separator.layoutParams = separatorParams
        separator.setBackgroundColor(ContextCompat.getColor(context, R.color.ios_separator))
        separator.alpha = 0.3f // iOS分隔线透明度
        
        return separator
    }
    
    /**
     * 创建iOS风格权限提示卡片
     */
    private fun createiOSPermissionCard(): View {
        val context = requireContext()
        
        val card = MaterialCardView(context)
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        card.layoutParams = cardParams
        card.radius = 16f // iOS大圆角
        card.cardElevation = 0f // iOS无阴影
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.ios_blue))
        card.strokeWidth = 0
        
        val cardContent = LinearLayout(context)
        cardContent.orientation = LinearLayout.HORIZONTAL
        cardContent.gravity = Gravity.CENTER_VERTICAL
        cardContent.setPadding(20, 16, 20, 16)
        
        // 图标
        val icon = TextView(context)
        icon.text = "🔔"
        icon.textSize = 24f
        val iconParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconParams.rightMargin = 16
        icon.layoutParams = iconParams
        cardContent.addView(icon)
        
        // 文本容器
        val textContainer = LinearLayout(context)
        textContainer.orientation = LinearLayout.VERTICAL
        val textParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        textContainer.layoutParams = textParams
        
        val titleText = TextView(context)
        titleText.text = "开启通知权限"
        titleText.textSize = 17f // iOS标准字体
        titleText.setTextColor(Color.WHITE)
        titleText.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textContainer.addView(titleText)
        
        val descText = TextView(context)
        descText.text = "允许访问通知以查看和汇总"
        descText.textSize = 13f
        descText.setTextColor(Color.WHITE)
        descText.alpha = 0.8f
        val descParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        descParams.topMargin = 2
        descText.layoutParams = descParams
        textContainer.addView(descText)
        
        cardContent.addView(textContainer)
        
        // iOS风格箭头
        val arrow = TextView(context)
        arrow.text = "›"
        arrow.textSize = 20f
        arrow.setTextColor(Color.WHITE)
        arrow.alpha = 0.6f
        cardContent.addView(arrow)
        
        card.addView(cardContent)
        
        // 点击事件
        card.setOnClickListener {
            PermissionHelper.openNotificationListenerSettings(context)
        }
        
        return card
    }
    
    /**
     * iOS风格卡片入场动画
     */
    private fun animateiOSCardEntrance(card: View, index: Int) {
        card.alpha = 0f
        card.translationY = 30f
        card.scaleX = 0.95f
        card.scaleY = 0.95f
        
        val animator = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f)
        animator.duration = 400 // iOS风格较慢的动画
        animator.startDelay = (index * 100).toLong()
        animator.interpolator = DecelerateInterpolator()
        
        val translateAnimator = ObjectAnimator.ofFloat(card, "translationY", 30f, 0f)
        translateAnimator.duration = 400
        translateAnimator.startDelay = (index * 100).toLong()
        translateAnimator.interpolator = DecelerateInterpolator()
        
        val scaleXAnimator = ObjectAnimator.ofFloat(card, "scaleX", 0.95f, 1f)
        scaleXAnimator.duration = 400
        scaleXAnimator.startDelay = (index * 100).toLong()
        scaleXAnimator.interpolator = DecelerateInterpolator()
        
        val scaleYAnimator = ObjectAnimator.ofFloat(card, "scaleY", 0.95f, 1f)
        scaleYAnimator.duration = 400
        scaleYAnimator.startDelay = (index * 100).toLong()
        scaleYAnimator.interpolator = DecelerateInterpolator()
        
        animator.start()
        translateAnimator.start()
        scaleXAnimator.start()
        scaleYAnimator.start()
    }
    
    /**
     * iOS风格时间格式化
     */
    private fun formatiOSTime(timeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(timeString)
            
            val now = Date()
            val diff = now.time - (date?.time ?: 0)
            
            when {
                diff < 60 * 1000 -> "现在"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                else -> {
                    val outputFormat = SimpleDateFormat("M月d日", Locale.getDefault())
                    outputFormat.format(date ?: Date())
                }
            }
        } catch (e: Exception) {
            timeString
        }
    }
    
    /**
     * 创建iOS风格空状态视图
     */
    private fun createiOSEmptyStateView(): View {
        val context = requireContext()
        
        val emptyContainer = LinearLayout(context)
        emptyContainer.orientation = LinearLayout.VERTICAL
        emptyContainer.gravity = Gravity.CENTER
        emptyContainer.setPadding(40, 60, 40, 60)
        
        // iOS风格的空状态图标
        val emptyIcon = TextView(context)
        emptyIcon.text = "📱"
        emptyIcon.textSize = 64f
        emptyIcon.gravity = Gravity.CENTER
        val iconParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconParams.bottomMargin = 20
        emptyIcon.layoutParams = iconParams
        emptyContainer.addView(emptyIcon)
        
        // iOS风格的空状态标题
        val emptyTitle = TextView(context)
        emptyTitle.text = "无通知"
        emptyTitle.textSize = 22f // iOS大标题字体
        emptyTitle.setTextColor(ContextCompat.getColor(context, R.color.ios_label))
        emptyTitle.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        emptyTitle.gravity = Gravity.CENTER
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleParams.bottomMargin = 8
        emptyTitle.layoutParams = titleParams
        emptyContainer.addView(emptyTitle)
        
        // iOS风格的提示文本
        val hintText = TextView(context)
        hintText.text = "当有新通知时，它们会显示在这里"
        hintText.textSize = 15f
        hintText.setTextColor(ContextCompat.getColor(context, R.color.ios_secondary_label))
        hintText.gravity = Gravity.CENTER
        hintText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        emptyContainer.addView(hintText)
        
        return emptyContainer
    }
    
    companion object {
        fun newInstance(): NotificationsFragment {
            return NotificationsFragment()
        }
    }
} 