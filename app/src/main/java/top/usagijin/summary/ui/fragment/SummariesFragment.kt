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
import top.usagijin.summary.data.SummaryData
import top.usagijin.summary.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * iOS风格摘要页面 Fragment
 * 完全模仿最新iOS设计风格
 */
class SummariesFragment : Fragment() {
    
    private val TAG = "SummariesFragment"
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
        loadSummaries()
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
        rootLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.background))
        
        // 创建滚动视图
        val scrollView = ScrollView(context)
        scrollView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
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
        errorLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.background))
        
        val errorText = TextView(context)
        errorText.text = "⚠️ 加载摘要时出错"
        errorText.textSize = 17f
        errorText.setTextColor(ContextCompat.getColor(context, R.color.error))
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
            viewModel.summaries.collect { summaries ->
                Log.d(TAG, "Received ${summaries.size} summaries")
                updateUI(summaries)
            }
        }
    }
    
    /**
     * 加载摘要数据
     */
    private fun loadSummaries() {
        Log.d(TAG, "Loading summaries...")
        viewModel.refreshData()
    }
    
    /**
     * 更新UI
     */
    private fun updateUI(summaries: List<SummaryData>) {
        try {
            contentContainer.removeAllViews()
            
            if (summaries.isEmpty()) {
                val emptyView = createiOSEmptyStateView()
                contentContainer.addView(emptyView)
            } else {
                summaries.forEachIndexed { index, summary ->
                    val summaryCard = createiOSSummaryCard(summary)
                    contentContainer.addView(summaryCard)
                    
                    // 添加间距
                    if (index < summaries.size - 1) {
                        val spacer = View(requireContext())
                        val spacerParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            16 // iOS风格间距
                        )
                        spacer.layoutParams = spacerParams
                        contentContainer.addView(spacer)
                    }
                    
                    // 添加入场动画
                    animateiOSCardEntrance(summaryCard, index)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI", e)
        }
    }
    
    /**
     * 创建iOS风格摘要卡片
     */
    private fun createiOSSummaryCard(summary: SummaryData): View {
        val context = requireContext()
        
        // 创建卡片
        val card = MaterialCardView(context)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        card.radius = 16f // iOS风格的大圆角
        card.cardElevation = 0f // iOS不使用阴影
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.secondary_system_grouped_background))
        card.strokeWidth = 0
        
        // 添加点击效果
        card.foreground = ContextCompat.getDrawable(context, R.drawable.ripple_effect)
        card.isClickable = true
        card.isFocusable = true
        
        // 创建卡片内容
        val cardContent = LinearLayout(context)
        cardContent.orientation = LinearLayout.VERTICAL
        cardContent.setPadding(20, 16, 20, 16) // iOS标准内边距
        
        // 顶部区域：应用信息和重要性
        val headerArea = createiOSHeaderArea(summary)
        cardContent.addView(headerArea)
        
        // 中间区域：摘要内容
        val contentArea = createiOSContentArea(summary)
        cardContent.addView(contentArea)
        
        // 底部区域：时间和状态
        val footerArea = createiOSFooterArea(summary)
        cardContent.addView(footerArea)
        
        card.addView(cardContent)
        
        return card
    }
    
    /**
     * 创建iOS风格头部区域
     */
    private fun createiOSHeaderArea(summary: SummaryData): View {
        val context = requireContext()
        
        val headerContainer = LinearLayout(context)
        headerContainer.orientation = LinearLayout.HORIZONTAL
        headerContainer.gravity = Gravity.CENTER_VERTICAL
        val headerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        headerParams.bottomMargin = 12
        headerContainer.layoutParams = headerParams
        
        // 左侧：应用图标和名称
        val leftArea = LinearLayout(context)
        leftArea.orientation = LinearLayout.HORIZONTAL
        leftArea.gravity = Gravity.CENTER_VERTICAL
        val leftParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        leftArea.layoutParams = leftParams
        
        // 应用图标
        val appIcon = ImageView(context)
        val iconSize = 32 // iOS标准小图标大小
        val iconParams = LinearLayout.LayoutParams(iconSize, iconSize)
        iconParams.rightMargin = 10
        appIcon.layoutParams = iconParams
        appIcon.scaleType = ImageView.ScaleType.CENTER_CROP
        
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(summary.packageName, 0)
            val icon = packageManager.getApplicationIcon(appInfo)
            appIcon.setImageDrawable(icon)
        } catch (e: Exception) {
            appIcon.setImageResource(R.drawable.ic_summarize)
            appIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.gray)
        }
        
        // iOS风格的圆角图标
        val iconDrawable = GradientDrawable()
        iconDrawable.cornerRadius = 8f // iOS小图标圆角
        iconDrawable.setColor(Color.TRANSPARENT)
        appIcon.background = iconDrawable
        appIcon.clipToOutline = true
        
        leftArea.addView(appIcon)
        
        // 应用名称
        val appNameView = TextView(context)
        appNameView.text = summary.appName
        appNameView.textSize = 15f // iOS标准字体大小
        appNameView.setTextColor(ContextCompat.getColor(context, R.color.label))
        appNameView.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        appNameView.maxLines = 1
        appNameView.ellipsize = android.text.TextUtils.TruncateAt.END
        leftArea.addView(appNameView)
        
        headerContainer.addView(leftArea)
        
        // 右侧：重要性指示器
        val importanceIndicator = createiOSImportanceIndicator(summary.importanceLevel)
        headerContainer.addView(importanceIndicator)
        
        return headerContainer
    }
    
    /**
     * 创建iOS风格重要性指示器
     */
    private fun createiOSImportanceIndicator(importanceLevel: Int): View {
        val context = requireContext()
        
        val indicatorContainer = LinearLayout(context)
        indicatorContainer.orientation = LinearLayout.HORIZONTAL
        indicatorContainer.gravity = Gravity.CENTER_VERTICAL
        
        // 重要性标签和颜色
        val (label, colorRes, emoji) = when (importanceLevel) {
            3 -> Triple("重要", R.color.importance_high, "🔴")
            2 -> Triple("中等", R.color.importance_medium, "🟡")
            else -> Triple("普通", R.color.importance_low, "🟢")
        }
        
        // 创建iOS风格的标签
        val labelContainer = LinearLayout(context)
        labelContainer.orientation = LinearLayout.HORIZONTAL
        labelContainer.gravity = Gravity.CENTER
        labelContainer.setPadding(8, 4, 8, 4)
        
        // 设置背景
        val labelBackground = GradientDrawable()
        labelBackground.cornerRadius = 12f // iOS标签圆角
        labelBackground.setColor(ContextCompat.getColor(context, colorRes))
        labelBackground.alpha = 30 // 半透明背景
        labelContainer.background = labelBackground
        
        // 表情符号
        val emojiView = TextView(context)
        emojiView.text = emoji
        emojiView.textSize = 12f
        val emojiParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        emojiParams.rightMargin = 4
        emojiView.layoutParams = emojiParams
        labelContainer.addView(emojiView)
        
        // 标签文本
        val labelText = TextView(context)
        labelText.text = label
        labelText.textSize = 11f
        labelText.setTextColor(ContextCompat.getColor(context, colorRes))
        labelText.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        labelContainer.addView(labelText)
        
        indicatorContainer.addView(labelContainer)
        
        return indicatorContainer
    }
    
    /**
     * 创建iOS风格内容区域
     */
    private fun createiOSContentArea(summary: SummaryData): View {
        val context = requireContext()
        
        val contentArea = LinearLayout(context)
        contentArea.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        contentParams.bottomMargin = 12
        contentArea.layoutParams = contentParams
        
        // 摘要标题
        if (!summary.title.isNullOrBlank()) {
            val titleView = TextView(context)
            titleView.text = summary.title
            titleView.textSize = 17f // iOS标题字体大小
            titleView.setTextColor(ContextCompat.getColor(context, R.color.label))
            titleView.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            titleView.setLineSpacing(0f, 1.2f)
            titleView.maxLines = 2
            titleView.ellipsize = android.text.TextUtils.TruncateAt.END
            val titleParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            titleParams.bottomMargin = 8
            titleView.layoutParams = titleParams
            contentArea.addView(titleView)
        }
        
        // 摘要内容
        val summaryView = TextView(context)
        summaryView.text = summary.summary
        summaryView.textSize = 15f // iOS正文字体大小
        summaryView.setTextColor(ContextCompat.getColor(context, R.color.secondary_label))
        summaryView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        summaryView.setLineSpacing(0f, 1.3f) // iOS行间距
        summaryView.maxLines = 4
        summaryView.ellipsize = android.text.TextUtils.TruncateAt.END
        contentArea.addView(summaryView)
        
        return contentArea
    }
    
    /**
     * 创建iOS风格底部区域
     */
    private fun createiOSFooterArea(summary: SummaryData): View {
        val context = requireContext()
        
        val footerContainer = LinearLayout(context)
        footerContainer.orientation = LinearLayout.HORIZONTAL
        footerContainer.gravity = Gravity.CENTER_VERTICAL
        
        // 时间
        val timeView = TextView(context)
        timeView.text = formatiOSTime(summary.time)
        timeView.textSize = 13f // iOS时间字体大小
        timeView.setTextColor(ContextCompat.getColor(context, R.color.tertiary_label))
        timeView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val timeParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        timeView.layoutParams = timeParams
        footerContainer.addView(timeView)
        
        // iOS风格的更多按钮
        val moreButton = TextView(context)
        moreButton.text = "···"
        moreButton.textSize = 16f
        moreButton.setTextColor(ContextCompat.getColor(context, R.color.tertiary_label))
        moreButton.gravity = Gravity.CENTER
        moreButton.setPadding(8, 4, 8, 4)
        
        // 设置点击背景
        val buttonBackground = GradientDrawable()
        buttonBackground.cornerRadius = 8f
        buttonBackground.setColor(Color.TRANSPARENT)
        moreButton.background = buttonBackground
        moreButton.isClickable = true
        moreButton.isFocusable = true
        
        footerContainer.addView(moreButton)
        
        return footerContainer
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
                    val outputFormat = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault())
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
        emptyIcon.text = "📝"
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
        emptyTitle.text = "无摘要"
        emptyTitle.textSize = 22f // iOS大标题字体
        emptyTitle.setTextColor(ContextCompat.getColor(context, R.color.label))
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
        hintText.text = "当通知被汇总后，摘要会显示在这里"
        hintText.textSize = 15f
        hintText.setTextColor(ContextCompat.getColor(context, R.color.secondary_label))
        hintText.gravity = Gravity.CENTER
        hintText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        emptyContainer.addView(hintText)
        
        return emptyContainer
    }
    
    companion object {
        fun newInstance(): SummariesFragment {
            return SummariesFragment()
        }
    }
} 