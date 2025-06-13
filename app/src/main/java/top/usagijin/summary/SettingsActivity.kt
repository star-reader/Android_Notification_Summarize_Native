package top.usagijin.summary

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import top.usagijin.summary.repository.NotificationRepository
import top.usagijin.summary.utils.PermissionHelper

/**
 * 设置页面Activity
 * 提供应用配置选项和数据管理功能
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var repository: NotificationRepository
    
    // UI组件
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var permissionStatusIndicator: View
    private lateinit var tvPermissionStatus: TextView
    private lateinit var switchPersistentNotifications: SwitchCompat
    private lateinit var tvDataStats: TextView
    private lateinit var cardAppColors: MaterialCardView
    private lateinit var cardCleanData: MaterialCardView
    private lateinit var cardPermissionSettings: MaterialCardView
    private lateinit var cardAbout: MaterialCardView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createLayout()
        setupToolbar()
        initializeComponents()
        setupViews()
        loadSettings()
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
            setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.background))
        }
        
        // 创建AppBarLayout
        appBarLayout = AppBarLayout(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.primary))
        }
        
        // 创建Toolbar
        toolbar = Toolbar(this).apply {
            layoutParams = AppBarLayout.LayoutParams(
                AppBarLayout.LayoutParams.MATCH_PARENT,
                AppBarLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.primary))
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
            setPadding(16, 16, 16, 16)
        }
        
        // 创建各个设置区域
        createPermissionSection(mainContent)
        createNotificationSection(mainContent)
        createDataSection(mainContent)
        createManageSection(mainContent)
        createAboutSection(mainContent)
        
        scrollView.addView(mainContent)
        coordinatorLayout.addView(scrollView)
        
        setContentView(coordinatorLayout)
    }
    
    /**
     * 创建权限状态区域
     */
    private fun createPermissionSection(parent: LinearLayout) {
        cardPermissionSettings = createCard("权限状态", parent)
        val cardContent = cardPermissionSettings.getChildAt(0) as LinearLayout
        
        val statusLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        permissionStatusIndicator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                rightMargin = 12
            }
        }
        statusLayout.addView(permissionStatusIndicator)
        
        tvPermissionStatus = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.on_surface))
        }
        statusLayout.addView(tvPermissionStatus)
        
        cardContent.addView(statusLayout)
    }
    
    /**
     * 创建通知设置区域
     */
    private fun createNotificationSection(parent: LinearLayout) {
        val card = createCard("通知设置", parent)
        val cardContent = card.getChildAt(0) as LinearLayout
        
        val switchLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val switchLabel = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = "高优先级通知置顶"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.on_surface))
        }
        switchLayout.addView(switchLabel)
        
        switchPersistentNotifications = SwitchCompat(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        switchLayout.addView(switchPersistentNotifications)
        
        cardContent.addView(switchLayout)
    }
    
    /**
     * 创建数据统计区域
     */
    private fun createDataSection(parent: LinearLayout) {
        val card = createCard("数据统计", parent)
        val cardContent = card.getChildAt(0) as LinearLayout
        
        tvDataStats = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.on_surface_variant))
            gravity = Gravity.CENTER
        }
        cardContent.addView(tvDataStats)
    }
    
    /**
     * 创建管理区域
     */
    private fun createManageSection(parent: LinearLayout) {
        cardAppColors = createCard("应用颜色设置", parent)
        cardCleanData = createCard("清理旧数据", parent)
    }
    
    /**
     * 创建关于区域
     */
    private fun createAboutSection(parent: LinearLayout) {
        cardAbout = createCard("关于", parent)
    }
    
    /**
     * 创建卡片
     */
    private fun createCard(title: String, parent: LinearLayout): MaterialCardView {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.surface))
        }
        
        val cardContent = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        val titleText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
            text = title
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.on_surface))
            setTypeface(typeface, Typeface.BOLD)
        }
        cardContent.addView(titleText)
        
        card.addView(cardContent)
        parent.addView(card)
        
        return card
    }
    
    /**
     * 设置工具栏
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
    }
    
    /**
     * 初始化组件
     */
    private fun initializeComponents() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        repository = NotificationRepository.getInstance(this)
    }
    
    /**
     * 设置视图
     */
    private fun setupViews() {
        // 权限状态
        updatePermissionStatus()
        
        // 持久化高优先级通知开关
        switchPersistentNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean("persistent_high_priority", isChecked)
                .apply()
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        }
        
        // 应用颜色设置
        cardAppColors.setOnClickListener {
            showAppColorsDialog()
        }
        
        // 清理旧数据
        cardCleanData.setOnClickListener {
            showCleanDataDialog()
        }
        
        // 权限设置
        cardPermissionSettings.setOnClickListener {
            PermissionHelper.openNotificationListenerSettings(this)
        }
        
        // 关于信息
        cardAbout.setOnClickListener {
            showAboutDialog()
        }
    }
    
    /**
     * 加载设置
     */
    private fun loadSettings() {
        // 加载持久化通知设置
        val persistentEnabled = sharedPreferences.getBoolean("persistent_high_priority", true)
        switchPersistentNotifications.isChecked = persistentEnabled
        
        // 加载数据统计
        loadDataStats()
    }
    
    /**
     * 更新权限状态
     */
    private fun updatePermissionStatus() {
        val hasPermission = PermissionHelper.isNotificationListenerEnabled(this)
        tvPermissionStatus.text = if (hasPermission) {
            getString(R.string.service_running)
        } else {
            getString(R.string.service_stopped)
        }
        
        // 设置状态指示器颜色
        val statusColor = if (hasPermission) {
            getColor(R.color.importance_low) // 绿色表示正常
        } else {
            getColor(R.color.importance_high) // 红色表示异常
        }
        permissionStatusIndicator.setBackgroundColor(statusColor)
    }
    
    /**
     * 加载数据统计
     */
    private fun loadDataStats() {
        lifecycleScope.launch {
            try {
                val stats = repository.getDataStats()
                tvDataStats.text = getString(R.string.stats_format, stats.first, stats.second)
            } catch (e: Exception) {
                tvDataStats.text = getString(R.string.stats_placeholder)
            }
        }
    }
    
    /**
     * 显示应用颜色设置对话框
     */
    private fun showAppColorsDialog() {
        val apps = arrayOf(
            "微信 (com.tencent.mm)",
            "QQ (com.tencent.mobileqq)", 
            "Gmail (com.google.android.gm)",
            "WhatsApp (com.whatsapp)",
            "Telegram (org.telegram.messenger)"
        )
        
        val packageNames = arrayOf(
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.google.android.gm", 
            "com.whatsapp",
            "org.telegram.messenger"
        )
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_colors))
            .setItems(apps) { _, which ->
                showColorPickerDialog(packageNames[which], apps[which])
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * 显示颜色选择对话框
     */
    private fun showColorPickerDialog(packageName: String, appName: String) {
        val colors = arrayOf(
            "蓝色" to R.color.primary,
            "绿色" to R.color.importance_low,
            "橙色" to R.color.importance_medium,
            "红色" to R.color.importance_high,
            "紫色" to R.color.purple_500,
            "青色" to R.color.secondary
        )
        
        val colorNames = colors.map { it.first }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("$appName - ${getString(R.string.dialog_title_color_picker)}")
            .setItems(colorNames) { _, which ->
                val colorRes = colors[which].second
                val color = getColor(colorRes)
                
                // 保存颜色设置
                sharedPreferences.edit()
                    .putInt("app_color_$packageName", color)
                    .apply()
                
                Toast.makeText(this, getString(R.string.color_updated_successfully), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * 显示清理数据对话框
     */
    private fun showCleanDataDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_title_clean_data))
            .setMessage(getString(R.string.dialog_message_clean_data))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                cleanOldData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * 清理旧数据
     */
    private fun cleanOldData() {
        lifecycleScope.launch {
            try {
                repository.cleanOldData()
                loadDataStats() // 重新加载统计数据
                Toast.makeText(this@SettingsActivity, getString(R.string.data_cleaned_successfully), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "清理失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0"
        }
        
        val aboutMessage = """
            ${getString(R.string.app_name)}
            
            ${getString(R.string.version)}: $versionName
            
            ${getString(R.string.help_content)}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.about))
            .setMessage(aboutMessage)
            .setPositiveButton(getString(R.string.confirm), null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次恢复时更新权限状态
        updatePermissionStatus()
        loadDataStats()
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