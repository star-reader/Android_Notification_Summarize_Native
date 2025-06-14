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
import top.usagijin.summary.utils.WorkManagerScheduler
import top.usagijin.summary.utils.TestNotificationSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
     * 初始化API服务
     */
    private fun initializeApiService() {
        Log.d(TAG, "Initializing API service...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = ApiService.getInstance(this@MainActivity)
                val initialized = apiService.initialize()
                
                if (initialized) {
                    Log.i(TAG, "API service initialized successfully")
                    
                    // 启动Token刷新定期任务
                    WorkManagerScheduler.scheduleTokenRefresh(this@MainActivity)
                } else {
                    Log.w(TAG, "API service initialization failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing API service", e)
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
}

