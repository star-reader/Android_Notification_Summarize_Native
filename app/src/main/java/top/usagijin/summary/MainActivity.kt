package top.usagijin.summary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.elevation.SurfaceColors
import top.usagijin.summary.ui.fragment.NotificationsFragment
import top.usagijin.summary.ui.fragment.SettingsFragment
import top.usagijin.summary.ui.fragment.SummariesFragment
import top.usagijin.summary.utils.PermissionHelper
import top.usagijin.summary.service.NotificationSenderService
import top.usagijin.summary.api.ApiService
import top.usagijin.summary.utils.TestNotificationSender
import top.usagijin.summary.utils.MemoryUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.lifecycleScope
import android.widget.Button
import top.usagijin.summary.api.NotificationInput

/**
 * 主界面Activity
 * 使用Material Design 3和底部导航
 */
class MainActivity : AppCompatActivity() {
    
    private val TAG = "MainActivity"
    
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    
    // 权限请求启动器
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "POST_NOTIFICATIONS permission granted")
            Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "POST_NOTIFICATIONS permission denied")
            Toast.makeText(this, "需要通知权限才能显示摘要通知", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "MainActivity onCreate started")
        
        try {
            setupUI()
            setupBottomNavigation()
            checkAndRequestPermissions()
            initializeApiService()
            initializeTestNotifications()
            
            // 默认显示通知页面
            if (savedInstanceState == null) {
                showFragment(NotificationsFragment.newInstance())
            }
            
            // 添加测试按钮
            val testButton = findViewById<Button>(R.id.test_inference_button)
            testButton?.setOnClickListener {
                testInference()
            }
            
            Log.d(TAG, "MainActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in MainActivity onCreate", e)
        }
    }
    
    /**
     * 设置UI布局
     */
    private fun setupUI() {
        // 设置状态栏颜色和样式
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)
        
        // 设置状态栏文字颜色为白色
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0 // 清除SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        
        // 创建主容器
        val mainContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(SurfaceColors.SURFACE_0.getColor(this@MainActivity))
        }
        
        // 创建Fragment容器
        fragmentContainer = FrameLayout(this).apply {
            id = android.R.id.content + 1 // 生成唯一ID
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_navigation_height)
            }
        }
        mainContainer.addView(fragmentContainer)
        
        // 创建底部导航
        bottomNavigation = BottomNavigationView(this).apply {
            id = android.R.id.content + 2 // 生成唯一ID
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.bottom_navigation_height)
            ).apply {
                gravity = Gravity.BOTTOM
            }
            
            // 设置菜单
            inflateMenu(R.menu.bottom_navigation)
            
            // 设置样式
            setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this@MainActivity))
            itemIconTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.bottom_nav_item_color)
            itemTextColor = ContextCompat.getColorStateList(this@MainActivity, R.color.bottom_nav_item_color)
        }
        mainContainer.addView(bottomNavigation)
        
        setContentView(mainContainer)
    }
    
    /**
     * 设置底部导航行为
     */
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notifications -> {
                    Log.d(TAG, "Navigating to Notifications")
                    showFragment(NotificationsFragment.newInstance())
                    true
                }
                R.id.nav_summaries -> {
                    Log.d(TAG, "Navigating to Summaries")
                    showFragment(SummariesFragment.newInstance())
                    true
                }
                R.id.nav_settings -> {
                    Log.d(TAG, "Navigating to Settings")
                    showFragment(SettingsFragment.newInstance())
                    true
                }
                else -> {
                    Log.w(TAG, "Unknown navigation item: ${item.itemId}")
                    false
                }
            }
        }
        
        // 设置默认选中项
        bottomNavigation.selectedItemId = R.id.nav_notifications
    }
    
    /**
     * 显示Fragment
     */
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment)
            .commitAllowingStateLoss()
    }
    
    /**
     * 检查并请求权限
     */
    private fun checkAndRequestPermissions() {
        // 检查发送通知权限 (Android 13+)
        if (PermissionHelper.shouldRequestPostNotificationPermission()) {
            if (!PermissionHelper.canPostNotifications(this)) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // 检查通知监听权限
        if (!PermissionHelper.isNotificationListenerEnabled(this)) {
            Log.d(TAG, "Notification listener permission not granted")
            // 这个权限需要用户手动在设置中开启，不能通过代码直接请求
        }
    }
    
    /**
     * 初始化本地模型服务
     */
    private fun initializeApiService() {
        Log.d(TAG, "Initializing local model service...")
        
        val apiService = ApiService.getInstance(this)
        
        // 监听模型加载状态
        lifecycleScope.launch {
            apiService.modelState.collectLatest { state ->
                when (state) {
                    is ApiService.ModelState.NotLoaded -> {
                        Log.d(TAG, "NDK模型未加载")
                    }
                    is ApiService.ModelState.Loading -> {
                        Log.i(TAG, "正在加载NDK ONNX模型，请稍候...")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "正在加载NDK ONNX模型，请稍候...", Toast.LENGTH_LONG).show()
                        }
                    }
                    is ApiService.ModelState.Loaded -> {
                        Log.i(TAG, "NDK ONNX模型加载成功！")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "NDK ONNX模型加载成功！", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is ApiService.ModelState.Error -> {
                        Log.e(TAG, "NDK模型加载失败: ${state.message}")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "NDK模型加载失败: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        
        // 异步初始化模型
        lifecycleScope.launch {
            try {
                apiService.initializeModel()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing local model", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "模型初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    /**
     * 初始化测试通知
     */
    private fun initializeTestNotifications() {
        TestNotificationSender.initializeChannel(this)
        
        // 注释掉自动测试通知，等待真实通知触发
        // if (top.usagijin.summary.private_config.ApiTestConfig.ENABLE_TEST_MODE) {
        //     CoroutineScope(Dispatchers.Main).launch {
        //         kotlinx.coroutines.delay(5000) // 等待5秒
        //         Log.d(TAG, "Sending test notification...")
        //         TestNotificationSender.sendLongNotification(this@MainActivity)
        //     }
        // }
    }
    
    /**
     * 导航到指定标签页
     * @param tabIndex 标签页索引 (0=通知, 1=摘要, 2=设置)
     */
    fun navigateToTab(tabIndex: Int) {
        val menuItemId = when (tabIndex) {
            0 -> R.id.nav_notifications
            1 -> R.id.nav_summaries
            2 -> R.id.nav_settings
            else -> R.id.nav_notifications
        }
        bottomNavigation.selectedItemId = menuItemId
    }
    
    private fun testInference() {
        lifecycleScope.launch {
            Log.i("MainActivity", "开始测试推理功能...")
            
            val apiService = ApiService.getInstance(this@MainActivity)
            
            // 测试数据
            val testNotifications = listOf(
                NotificationInput(
                    title = "工作群",
                    content = "张三: 明天的项目会议改到下午3点，请大家准时参加。李四: 收到，我会准备好相关资料。王五: 好的，没问题。",
                    time = "2024-06-15 14:55:00",
                    packageName = "com.tencent.mm"
                ),
                NotificationInput(
                    title = "重要邮件",
                    content = "来自manager@company.com: 关于Q4季度总结会议安排 - 定于本周五上午10点在大会议室举行，请各部门负责人准备汇报材料，包括业绩数据和下季度计划。",
                    time = "2024-06-15 14:56:00", 
                    packageName = "com.google.android.gm"
                ),
                NotificationInput(
                    title = "银行通知",
                    content = "【工商银行】您的账户于12月15日14:30发生一笔转账交易，金额5000.00元，余额12345.67元。如非本人操作请及时联系客服。",
                    time = "2024-06-15 14:57:00",
                    packageName = "com.android.mms"
                )
            )
            
            // 逐个测试
            testNotifications.forEachIndexed { index, notification ->
                Log.i("MainActivity", "测试通知 ${index + 1}: ${notification.title}")
                
                try {
                    val request = top.usagijin.summary.api.SummarizeRequest(
                        currentTime = notification.time,
                        data = listOf(notification)
                    )
                    val result = apiService.summarize(request)
                    
                    Log.i("MainActivity", "测试结果 ${index + 1}:")
                    Log.i("MainActivity", "  标题: ${result.title}")
                    Log.i("MainActivity", "  摘要: ${result.summary}")
                    Log.i("MainActivity", "  重要级别: ${result.importanceLevel}")
                    Log.i("MainActivity", "  摘要长度: ${result.summary.length} 字符")
                    
                } catch (e: Exception) {
                    Log.e("MainActivity", "测试失败 ${index + 1}: ${e.message}", e)
                }
                
                // 间隔1秒
                kotlinx.coroutines.delay(1000)
            }
            
            Log.i("MainActivity", "所有测试完成！")
        }
    }
}

