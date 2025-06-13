package top.usagijin.summary

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.AppBarLayout
import top.usagijin.summary.data.NotificationData
import top.usagijin.summary.data.SummaryData
import top.usagijin.summary.utils.NotificationDisplayManager

/**
 * 通知/摘要详情Activity
 * 显示通知或摘要的详细信息
 */
class NotificationDetailActivity : AppCompatActivity() {
    
    // UI组件
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var colorBar: View
    private lateinit var ivAppIcon: ImageView
    private lateinit var tvAppName: TextView
    private lateinit var tvTime: TextView
    private lateinit var titleSection: LinearLayout
    private lateinit var tvTitleLabel: TextView
    private lateinit var tvTitle: TextView
    private lateinit var contentSection: LinearLayout
    private lateinit var tvContentLabel: TextView
    private lateinit var tvContent: TextView
    private lateinit var summarySection: LinearLayout
    private lateinit var tvSummaryLabel: TextView
    private lateinit var tvSummary: TextView
    private lateinit var importanceSection: LinearLayout
    private lateinit var tvImportanceLabel: TextView
    private lateinit var tvImportance: TextView
    private lateinit var importanceIndicator: View
    
    private var currentType: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createLayout()
        setupToolbar()
        handleIntent()
    }
    
    /**
     * 创建布局
     */
    private fun createLayout() {
        // 创建根布局
        coordinatorLayout = CoordinatorLayout(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.background))
        }
        
        // 创建AppBarLayout
        appBarLayout = AppBarLayout(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.primary_blue))
        }
        
        // 创建Toolbar
        toolbar = Toolbar(this).apply {
            layoutParams = AppBarLayout.LayoutParams(
                AppBarLayout.LayoutParams.MATCH_PARENT,
                AppBarLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.primary_blue))
            setTitleTextColor(Color.WHITE)
        }
        appBarLayout.addView(toolbar)
        coordinatorLayout.addView(appBarLayout)
        
        // 创建滚动视图
        val scrollView = NestedScrollView(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.MATCH_PARENT
            ).apply {
                behavior = AppBarLayout.ScrollingViewBehavior()
            }
        }
        
        // 创建主内容布局
        val mainContent = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }
        
        // 创建头部区域
        createHeaderSection(mainContent)
        
        // 创建详情区域
        createDetailsSection(mainContent)
        
        scrollView.addView(mainContent)
        coordinatorLayout.addView(scrollView)
        
        setContentView(coordinatorLayout)
    }
    
    /**
     * 创建头部区域
     */
    private fun createHeaderSection(parent: LinearLayout) {
        // 颜色条
        colorBar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8
            )
            setBackgroundColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.primary_blue))
        }
        parent.addView(colorBar)
        
        // 头部信息容器
        val headerContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        // 应用图标
        ivAppIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                rightMargin = 16
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        headerContainer.addView(ivAppIcon)
        
        // 应用信息容器
        val appInfoContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = LinearLayout.VERTICAL
        }
        
        // 应用名称
        tvAppName = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.on_surface))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        appInfoContainer.addView(tvAppName)
        
        // 时间
        tvTime = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
            }
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.on_surface_variant))
        }
        appInfoContainer.addView(tvTime)
        
        headerContainer.addView(appInfoContainer)
        parent.addView(headerContainer)
    }
    
    /**
     * 创建详情区域
     */
    private fun createDetailsSection(parent: LinearLayout) {
        val detailsContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 0, 16, 16)
        }
        
        // 标题区域
        titleSection = createDetailSection("标题")
        tvTitleLabel = titleSection.getChildAt(0) as TextView
        tvTitle = titleSection.getChildAt(1) as TextView
        detailsContainer.addView(titleSection)
        
        // 内容区域
        contentSection = createDetailSection("内容")
        tvContentLabel = contentSection.getChildAt(0) as TextView
        tvContent = contentSection.getChildAt(1) as TextView
        detailsContainer.addView(contentSection)
        
        // 摘要区域
        summarySection = createDetailSection("摘要")
        tvSummaryLabel = summarySection.getChildAt(0) as TextView
        tvSummary = summarySection.getChildAt(1) as TextView
        detailsContainer.addView(summarySection)
        
        // 重要性区域
        importanceSection = createImportanceSection()
        detailsContainer.addView(importanceSection)
        
        parent.addView(detailsContainer)
    }
    
    /**
     * 创建详情区域
     */
    private fun createDetailSection(labelText: String): LinearLayout {
        val section = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            orientation = LinearLayout.VERTICAL
        }
        
        val label = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
            text = labelText
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.on_surface_variant))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        section.addView(label)
        
        val content = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.on_surface))
        }
        section.addView(content)
        
        return section
    }
    
    /**
     * 创建重要性区域
     */
    private fun createImportanceSection(): LinearLayout {
        importanceSection = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            orientation = LinearLayout.VERTICAL
        }
        
        tvImportanceLabel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
            text = "重要性"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.on_surface_variant))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        importanceSection.addView(tvImportanceLabel)
        
        val importanceContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        importanceIndicator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                rightMargin = 8
            }
        }
        importanceContainer.addView(importanceIndicator)
        
        tvImportance = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@NotificationDetailActivity, R.color.on_surface))
        }
        importanceContainer.addView(tvImportance)
        
        importanceSection.addView(importanceContainer)
        return importanceSection
    }
    
    /**
     * 设置工具栏
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }
    
    /**
     * 处理Intent数据
     */
    private fun handleIntent() {
        currentType = intent.getStringExtra("type")
        
        when (currentType) {
            "notification" -> {
                @Suppress("DEPRECATION")
                val notification = intent.getParcelableExtra<NotificationData>("notification")
                if (notification != null) {
                    displayNotificationDetails(notification)
                    supportActionBar?.title = getString(R.string.notification_details)
                }
            }
            "summary" -> {
                @Suppress("DEPRECATION")
                val summary = intent.getParcelableExtra<SummaryData>("summary")
                if (summary != null) {
                    displaySummaryDetails(summary)
                    supportActionBar?.title = getString(R.string.summary_details)
                }
            }
            else -> {
                finish() // 无效的类型，关闭Activity
            }
        }
    }
    
    /**
     * 显示通知详情
     */
    private fun displayNotificationDetails(notification: NotificationData) {
        // 设置应用图标
        try {
            val appIcon = packageManager.getApplicationIcon(notification.packageName)
            ivAppIcon.setImageDrawable(appIcon)
        } catch (e: PackageManager.NameNotFoundException) {
            ivAppIcon.setImageResource(R.drawable.ic_notification)
        }
        
        // 设置应用名称
        tvAppName.text = notification.appName
        
        // 设置时间
        tvTime.text = notification.time
        
        // 设置标题
        if (!notification.title.isNullOrBlank()) {
            tvTitleLabel.text = getString(R.string.title_label)
            tvTitle.text = notification.title
            titleSection.visibility = View.VISIBLE
        } else {
            titleSection.visibility = View.GONE
        }
        
        // 设置内容
        if (!notification.content.isNullOrBlank()) {
            tvContentLabel.text = getString(R.string.content_label)
            tvContent.text = notification.content
            contentSection.visibility = View.VISIBLE
        } else {
            contentSection.visibility = View.GONE
        }
        
        // 隐藏摘要相关的UI
        summarySection.visibility = View.GONE
        importanceSection.visibility = View.GONE
        
        // 设置颜色条
        val displayManager = NotificationDisplayManager.getInstance(this@NotificationDetailActivity)
        val appColor = displayManager.getAppColor(notification.packageName)
        colorBar.setBackgroundColor(appColor)
    }
    
    /**
     * 显示摘要详情
     */
    private fun displaySummaryDetails(summary: SummaryData) {
        // 设置应用图标
        try {
            val appIcon = packageManager.getApplicationIcon(summary.packageName)
            ivAppIcon.setImageDrawable(appIcon)
        } catch (e: PackageManager.NameNotFoundException) {
            ivAppIcon.setImageResource(R.drawable.ic_notification)
        }
        
        // 设置应用名称
        tvAppName.text = summary.appName
        
        // 设置时间
        tvTime.text = summary.time
        
        // 设置摘要标题
        tvTitleLabel.text = getString(R.string.title_label)
        tvTitle.text = summary.title
        titleSection.visibility = View.VISIBLE
        
        // 设置摘要内容
        tvSummaryLabel.text = getString(R.string.summary_label)
        tvSummary.text = summary.summary
        summarySection.visibility = View.VISIBLE
        
        // 设置重要性
        tvImportanceLabel.text = getString(R.string.importance_label)
        val importanceText = when (summary.importanceLevel) {
            1 -> getString(R.string.importance_low)
            2 -> getString(R.string.importance_medium)
            3 -> getString(R.string.importance_high)
            else -> getString(R.string.importance_low)
        }
        tvImportance.text = importanceText
        
        // 设置重要性指示器颜色
        val importanceColor = when (summary.importanceLevel) {
            3 -> ContextCompat.getColor(this@NotificationDetailActivity, R.color.importance_high)
            2 -> ContextCompat.getColor(this@NotificationDetailActivity, R.color.importance_medium)
            else -> ContextCompat.getColor(this@NotificationDetailActivity, R.color.importance_low)
        }
        importanceIndicator.setBackgroundColor(importanceColor)
        importanceSection.visibility = View.VISIBLE
        
        // 隐藏通知内容
        contentSection.visibility = View.GONE
        
        // 设置颜色条
        val displayManager = NotificationDisplayManager.getInstance(this@NotificationDetailActivity)
        val appColor = displayManager.getAppColor(summary.packageName)
        colorBar.setBackgroundColor(appColor)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 