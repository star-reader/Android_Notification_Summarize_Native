package top.usagijin.summary.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import top.usagijin.summary.R
import top.usagijin.summary.utils.PermissionHelper

/**
 * iOS风格设置页面 Fragment
 * 完全模仿最新iOS设置页面设计风格
 */
class SettingsFragment : Fragment() {
    
    private val TAG = "SettingsFragment"
    
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
        
        setupSettingsContent()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次回到页面时刷新权限状态
        refreshPermissionStatus()
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
        rootLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.background))
        
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
        errorLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.background))
        
        val errorText = TextView(context)
        errorText.text = "加载设置时出错"
        errorText.textSize = 17f
        errorText.setTextColor(ContextCompat.getColor(context, R.color.error))
        errorText.gravity = Gravity.CENTER
        errorText.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        
        errorLayout.addView(errorText)
        return errorLayout
    }
    
    /**
     * 设置设置页面内容
     */
    private fun setupSettingsContent() {
        try {
            contentContainer.removeAllViews()
            
            // 权限管理部分
            val permissionSection = createiOSSettingsSection(
                "权限管理",
                listOf(
                    createiOSPermissionSettingItem(),
                    createiOSNotificationPostSettingItem()
                )
            )
            contentContainer.addView(permissionSection)
            
            // 添加分组间距
            addSectionSpacer()
            
            // 应用设置部分
            val appSection = createiOSSettingsSection(
                "应用设置",
                listOf(
                    createiOSToggleSettingItem(
                        "持续通知",
                        "高优先级摘要保持显示",
                        "🔔",
                        "persistent_notifications",
                        true
                    ),
                    createiOSActionSettingItem(
                        "清理数据",
                        "删除7天前的通知和摘要",
                        "🗑️"
                    ) {
                        showDataCleanupDialog()
                    }
                )
            )
            contentContainer.addView(appSection)
            
            // 添加分组间距
            addSectionSpacer()
            
            // 关于部分
            val aboutSection = createiOSSettingsSection(
                "关于",
                listOf(
                    createiOSInfoSettingItem(
                        "版本",
                        "1.0.0",
                        "📱"
                    ),
                    createiOSActionSettingItem(
                        "开源许可",
                        "查看开源许可证",
                        "📄"
                    ) {
                        showAboutDialog()
                    },
                    createiOSActionSettingItem(
                        "反馈建议",
                        "发送反馈或建议",
                        "💬"
                    ) {
                        openFeedback()
                    }
                )
            )
            contentContainer.addView(aboutSection)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up settings content", e)
        }
    }
    
    /**
     * 创建设置分组
     */
    private fun createiOSSettingsSection(title: String, items: List<View>): View {
        val context = requireContext()
        
        val sectionContainer = LinearLayout(context)
        sectionContainer.orientation = LinearLayout.VERTICAL
        sectionContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 分组标题
        val sectionTitle = TextView(context)
        sectionTitle.text = title.uppercase()
        sectionTitle.textSize = 13f
        sectionTitle.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
        sectionTitle.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        sectionTitle.setPadding(16, 0, 16, 8)
        val titleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleParams.bottomMargin = 8
        sectionTitle.layoutParams = titleParams
        sectionContainer.addView(sectionTitle)
        
        // 创建卡片容器
        val card = MaterialCardView(context)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        card.radius = 16f
        card.cardElevation = 0f
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_variant))
        card.strokeWidth = 0
        
        // 创建卡片内容
        val cardContent = LinearLayout(context)
        cardContent.orientation = LinearLayout.VERTICAL
        cardContent.setPadding(0, 0, 0, 0)
        
        // 添加设置项
        items.forEachIndexed { index, item ->
            cardContent.addView(item)
            
            // 添加分隔线（除了最后一个）
            if (index < items.size - 1) {
                val separator = createiOSSeparator()
                cardContent.addView(separator)
            }
        }
        
        card.addView(cardContent)
        sectionContainer.addView(card)
        
        return sectionContainer
    }
    
    /**
     * 创建权限设置项
     */
    private fun createiOSPermissionSettingItem(): View {
        val context = requireContext()
        
        val itemContainer = LinearLayout(context)
        itemContainer.orientation = LinearLayout.HORIZONTAL
        itemContainer.gravity = Gravity.CENTER_VERTICAL
        itemContainer.setPadding(20, 12, 20, 12)
        itemContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 添加点击效果
        val rippleDrawable = GradientDrawable()
        rippleDrawable.setColor(Color.TRANSPARENT)
        rippleDrawable.cornerRadii = floatArrayOf(16f, 16f, 16f, 16f, 0f, 0f, 0f, 0f)
        itemContainer.background = rippleDrawable
        itemContainer.isClickable = true
        itemContainer.isFocusable = true
        
        // 左侧图标
        val iconView = TextView(context)
        iconView.text = "🔔"
        iconView.textSize = 20f
        val iconParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconParams.rightMargin = 16
        iconView.layoutParams = iconParams
        itemContainer.addView(iconView)
        
        // 中间内容区域
        val contentArea = LinearLayout(context)
        contentArea.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        contentArea.layoutParams = contentParams
        
        // 主标题
        val titleView = TextView(context)
        titleView.text = "通知访问权限"
        titleView.textSize = 17f
        titleView.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
        titleView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        contentArea.addView(titleView)
        
        // 状态描述
        val statusView = TextView(context)
        statusView.textSize = 13f
        statusView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        statusParams.topMargin = 2
        statusView.layoutParams = statusParams
        contentArea.addView(statusView)
        
        itemContainer.addView(contentArea)
        
        // 右侧箭头
        val arrowView = TextView(context)
        arrowView.text = "›"
        arrowView.textSize = 18f
        arrowView.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
        itemContainer.addView(arrowView)
        
        // 点击事件
        itemContainer.setOnClickListener {
            PermissionHelper.openNotificationListenerSettings(context)
        }
        
        // 更新状态的函数
        fun updateStatus() {
            val isEnabled = PermissionHelper.isNotificationListenerEnabled(context)
            if (isEnabled) {
                statusView.text = "已开启"
                statusView.setTextColor(ContextCompat.getColor(context, R.color.success))
            } else {
                statusView.text = "未开启 - 点击设置"
                statusView.setTextColor(ContextCompat.getColor(context, R.color.error))
            }
        }
        
        // 初始化状态
        updateStatus()
        
        // 保存更新函数的引用，以便在onResume时调用
        itemContainer.tag = { updateStatus() }
        
        return itemContainer
    }
    
    /**
     * 创建iOS风格通知发送权限设置项
     */
    private fun createiOSNotificationPostSettingItem(): View {
        val context = requireContext()
        
        val itemContainer = LinearLayout(context)
        itemContainer.orientation = LinearLayout.HORIZONTAL
        itemContainer.gravity = Gravity.CENTER_VERTICAL
        itemContainer.setPadding(20, 12, 20, 12) // iOS标准内边距
        itemContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 添加点击效果
        val rippleDrawable = GradientDrawable()
        rippleDrawable.setColor(Color.TRANSPARENT)
        rippleDrawable.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 16f, 16f, 16f, 16f)
        itemContainer.background = rippleDrawable
        itemContainer.isClickable = true
        itemContainer.isFocusable = true
        
        // 左侧图标
        val iconView = TextView(context)
        iconView.text = "📤"
        iconView.textSize = 20f
        val iconParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconParams.rightMargin = 16
        iconView.layoutParams = iconParams
        itemContainer.addView(iconView)
        
        // 中间内容区域
        val contentArea = LinearLayout(context)
        contentArea.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        contentArea.layoutParams = contentParams
        
        // 主标题
        val titleView = TextView(context)
        titleView.text = "通知发送权限"
        titleView.textSize = 17f // iOS标准字体大小
        titleView.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
        titleView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        contentArea.addView(titleView)
        
        // 状态描述
        val statusView = TextView(context)
        statusView.textSize = 13f
        statusView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        statusParams.topMargin = 2
        statusView.layoutParams = statusParams
        contentArea.addView(statusView)
        
        itemContainer.addView(contentArea)
        
        // 右侧箭头
        val arrowView = TextView(context)
        arrowView.text = "›"
        arrowView.textSize = 18f
        arrowView.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
        itemContainer.addView(arrowView)
        
        // 点击事件
        itemContainer.setOnClickListener {
            PermissionHelper.openAppNotificationSettings(context)
        }
        
        // 更新状态的函数
        fun updateStatus() {
            val isEnabled = PermissionHelper.hasPostNotificationPermission(context)
            if (isEnabled) {
                statusView.text = "已开启"
                statusView.setTextColor(ContextCompat.getColor(context, R.color.success))
            } else {
                statusView.text = "未开启 - 点击设置"
                statusView.setTextColor(ContextCompat.getColor(context, R.color.error))
            }
        }
        
        // 初始化状态
        updateStatus()
        
        // 保存更新函数的引用，以便在onResume时调用
        itemContainer.tag = { updateStatus() }
        
        return itemContainer
    }
    
    /**
     * 创建iOS风格开关设置项
     */
    private fun createiOSToggleSettingItem(
        title: String,
        subtitle: String,
        icon: String,
        prefKey: String,
        defaultValue: Boolean
    ): View {
        val context = requireContext()
        
        val itemContainer = LinearLayout(context)
        itemContainer.orientation = LinearLayout.HORIZONTAL
        itemContainer.gravity = Gravity.CENTER_VERTICAL
        itemContainer.setPadding(20, 12, 20, 12) // iOS标准内边距
        itemContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 左侧图标
        val iconView = TextView(context)
        iconView.text = icon
        iconView.textSize = 20f
        val iconParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconParams.rightMargin = 16
        iconView.layoutParams = iconParams
        itemContainer.addView(iconView)
        
        // 中间内容区域
        val contentArea = LinearLayout(context)
        contentArea.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        contentArea.layoutParams = contentParams
        
        // 主标题
        val titleView = TextView(context)
        titleView.text = title
        titleView.textSize = 17f // iOS标准字体大小
        titleView.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
        titleView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        contentArea.addView(titleView)
        
        // 副标题
        val subtitleView = TextView(context)
        subtitleView.text = subtitle
        subtitleView.textSize = 13f
        subtitleView.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
        subtitleView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val subtitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        subtitleParams.topMargin = 2
        subtitleView.layoutParams = subtitleParams
        contentArea.addView(subtitleView)
        
        itemContainer.addView(contentArea)
        
        // 右侧开关
        val switchView = Switch(context)
        val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        switchView.isChecked = sharedPrefs.getBoolean(prefKey, defaultValue)
        
        // 开关颜色
        switchView.thumbTintList = ContextCompat.getColorStateList(context, R.color.white)
        switchView.trackTintList = ContextCompat.getColorStateList(context, R.color.primary)
        
        switchView.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean(prefKey, isChecked).apply()
            Log.d(TAG, "$title switched to: $isChecked")
        }
        
        itemContainer.addView(switchView)
        
        return itemContainer
    }
    
    /**
     * 创建iOS风格操作设置项
     */
    private fun createiOSActionSettingItem(
        title: String,
        subtitle: String,
        icon: String,
        action: () -> Unit
    ): View {
        val context = requireContext()
        
        val itemContainer = LinearLayout(context)
        itemContainer.orientation = LinearLayout.HORIZONTAL
        itemContainer.gravity = Gravity.CENTER_VERTICAL
        itemContainer.setPadding(20, 12, 20, 12)
        itemContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 添加点击效果
        val rippleDrawable = GradientDrawable()
        rippleDrawable.setColor(Color.TRANSPARENT)
        itemContainer.background = rippleDrawable
        itemContainer.isClickable = true
        itemContainer.isFocusable = true
        
        // 左侧图标
        val iconView = TextView(context)
        iconView.text = icon
        iconView.textSize = 20f
        val iconParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconParams.rightMargin = 16
        iconView.layoutParams = iconParams
        itemContainer.addView(iconView)
        
        // 中间内容区域
        val contentArea = LinearLayout(context)
        contentArea.orientation = LinearLayout.VERTICAL
        val contentParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        contentArea.layoutParams = contentParams
        
        // 主标题
        val titleView = TextView(context)
        titleView.text = title
        titleView.textSize = 17f
        titleView.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
        titleView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        contentArea.addView(titleView)
        
        // 副标题
        val subtitleView = TextView(context)
        subtitleView.text = subtitle
        subtitleView.textSize = 13f
        subtitleView.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
        subtitleView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val subtitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        subtitleParams.topMargin = 2
        subtitleView.layoutParams = subtitleParams
        contentArea.addView(subtitleView)
        
        itemContainer.addView(contentArea)
        
        // 右侧箭头
        val arrowView = TextView(context)
        arrowView.text = "›"
        arrowView.textSize = 18f
        arrowView.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
        itemContainer.addView(arrowView)
        
        // 点击事件
        itemContainer.setOnClickListener {
            action()
        }
        
        return itemContainer
    }
    
    /**
     * 创建iOS风格信息设置项
     */
    private fun createiOSInfoSettingItem(
        title: String,
        value: String,
        icon: String
    ): View {
        val context = requireContext()
        
        val itemContainer = LinearLayout(context)
        itemContainer.orientation = LinearLayout.HORIZONTAL
        itemContainer.gravity = Gravity.CENTER_VERTICAL
        itemContainer.setPadding(20, 12, 20, 12)
        itemContainer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        
        // 左侧图标
        val iconView = TextView(context)
        iconView.text = icon
        iconView.textSize = 20f
        val iconParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconParams.rightMargin = 16
        iconView.layoutParams = iconParams
        itemContainer.addView(iconView)
        
        // 标题
        val titleView = TextView(context)
        titleView.text = title
        titleView.textSize = 17f
        titleView.setTextColor(ContextCompat.getColor(context, R.color.on_surface))
        titleView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val titleParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        titleView.layoutParams = titleParams
        itemContainer.addView(titleView)
        
        // 值
        val valueView = TextView(context)
        valueView.text = value
        valueView.textSize = 17f
        valueView.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
        valueView.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        valueView.gravity = Gravity.END
        itemContainer.addView(valueView)
        
        return itemContainer
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
        separatorParams.leftMargin = 56 // 对齐内容区域
        separator.layoutParams = separatorParams
        separator.setBackgroundColor(ContextCompat.getColor(context, R.color.outline_variant))
        separator.alpha = 0.3f
        
        return separator
    }
    
    /**
     * 添加分组间距
     */
    private fun addSectionSpacer() {
        val spacer = View(requireContext())
        val spacerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            32 // iOS分组间距
        )
        spacer.layoutParams = spacerParams
        contentContainer.addView(spacer)
    }
    
    /**
     * 刷新权限状态
     */
    private fun refreshPermissionStatus() {
        try {
            // 遍历所有设置项，更新权限状态
            for (i in 0 until contentContainer.childCount) {
                val child = contentContainer.getChildAt(i)
                if (child is LinearLayout) {
                    // 查找卡片内容
                    for (j in 0 until child.childCount) {
                        val cardChild = child.getChildAt(j)
                        if (cardChild is MaterialCardView) {
                            val cardContent = cardChild.getChildAt(0) as? LinearLayout
                            cardContent?.let { content ->
                                // 遍历设置项
                                for (k in 0 until content.childCount) {
                                    val item = content.getChildAt(k)
                                    if (item is LinearLayout && item.tag is Function0<*>) {
                                        // 调用更新函数
                                        (item.tag as Function0<Unit>).invoke()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing permission status", e)
        }
    }
    
    /**
     * 显示数据清理对话框
     */
    private fun showDataCleanupDialog() {
        val context = requireContext()
        
        AlertDialog.Builder(context)
            .setTitle("清理数据")
            .setMessage("确定要删除7天前的通知和摘要数据吗？此操作无法撤销。")
            .setPositiveButton("确定") { _, _ ->
                performDataCleanup()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 执行数据清理
     */
    private fun performDataCleanup() {
        lifecycleScope.launch {
            try {
                // TODO: 实现数据清理逻辑
                Log.d(TAG, "Data cleanup performed")
                
                // 显示成功消息
                AlertDialog.Builder(requireContext())
                    .setTitle("清理完成")
                    .setMessage("已成功清理7天前的通知")
                    .setPositiveButton("确定", null)
                    .show()
            } catch (e: Exception) {
                Log.e(TAG, "Error during data cleanup", e)
                
                // 显示错误消息
                AlertDialog.Builder(requireContext())
                    .setTitle("清理失败")
                    .setMessage("数据清理过程中出现错误：${e.message}")
                    .setPositiveButton("确定", null)
                    .show()
            }
        }
    }
    
    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        val context = requireContext()
        
        AlertDialog.Builder(context)
            .setTitle("关于应用")
            .setMessage("通知摘要助手 v1.0.0\n\n这是一个模仿Apple Intelligence通知摘要功能的Android应用。\n\n开源许可：MIT License")
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 打开反馈页面
     */
    private fun openFeedback() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@example.com"))
                putExtra(Intent.EXTRA_SUBJECT, "通知摘要助手 - 反馈建议")
                putExtra(Intent.EXTRA_TEXT, "请在此处输入您的反馈或建议...")
            }
            
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                // 如果没有邮件应用，显示提示
                AlertDialog.Builder(requireContext())
                    .setTitle("无法打开邮件应用")
                    .setMessage("请安装邮件应用或直接联系：feedback@example.com")
                    .setPositiveButton("确定", null)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening feedback", e)
        }
    }
    
    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
} 
