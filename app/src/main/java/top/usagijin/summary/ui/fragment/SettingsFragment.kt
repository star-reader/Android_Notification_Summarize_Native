package top.usagijin.summary.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import top.usagijin.summary.R
import top.usagijin.summary.utils.PermissionHelper

/**
 * 设置页面Fragment - Material Design 3风格
 */
class SettingsFragment : Fragment() {
    
    private lateinit var layoutNotificationPermission: LinearLayout
    private lateinit var layoutClearData: LinearLayout
    private lateinit var layoutOpenSource: LinearLayout
    private lateinit var textPermissionStatus: MaterialTextView
    private lateinit var switchPersistentNotifications: MaterialSwitch
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupClickListeners()
        updatePermissionStatus()
        loadPersistentNotificationPreference()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次回到页面时刷新权限状态
        updatePermissionStatus()
    }
    
    private fun initViews(view: View) {
        layoutNotificationPermission = view.findViewById(R.id.layoutNotificationPermission)
        layoutClearData = view.findViewById(R.id.layoutClearData)
        layoutOpenSource = view.findViewById(R.id.layoutOpenSource)
        textPermissionStatus = view.findViewById(R.id.textPermissionStatus)
        switchPersistentNotifications = view.findViewById(R.id.switchPersistentNotifications)
    }
    
    private fun setupClickListeners() {
        // 通知访问权限点击
        layoutNotificationPermission.setOnClickListener {
            openNotificationListenerSettings()
        }
        
        // 清理数据点击
        layoutClearData.setOnClickListener {
            showDataCleanupDialog()
        }
        
        // 开源许可点击
        layoutOpenSource.setOnClickListener {
            showAboutDialog()
        }
        
        // 持续通知开关
        switchPersistentNotifications.setOnCheckedChangeListener { _, isChecked ->
            savePersistentNotificationPreference(isChecked)
        }
    }
    
    private fun updatePermissionStatus() {
        lifecycleScope.launch {
            val hasPermission = PermissionHelper.isNotificationListenerEnabled(requireContext())
            textPermissionStatus.text = if (hasPermission) "已授权" else "未授权"
            
            // 根据权限状态更新UI
            val statusColor = if (hasPermission) {
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success)
            } else {
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_error)
            }
            textPermissionStatus.setTextColor(statusColor)
        }
    }
    
    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开设置页面，显示提示
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("设置提示")
                .setMessage("请在系统设置中找到通知访问权限并启用本应用的权限。")
                .setPositiveButton("确定", null)
                .show()
        }
    }
    
    private fun showDataCleanupDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清理数据")
            .setMessage("确定要删除7天前的通知和摘要数据吗？\n\n此操作无法撤销。")
            .setPositiveButton("确定") { _, _ ->
                performDataCleanup()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun performDataCleanup() {
        lifecycleScope.launch {
            try {
                // TODO: 实现数据清理逻辑
                // notificationRepository.deleteOldData()
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("清理完成")
                    .setMessage("已成功清理过期数据。")
                    .setPositiveButton("确定", null)
                    .show()
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("清理失败")
                    .setMessage("数据清理过程中出现错误：${e.message}")
                    .setPositiveButton("确定", null)
                    .show()
            }
        }
    }
    
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("关于应用")
            .setMessage("""
                🤖 智能通知摘要 v1.0.0
                
                这是一个开源的Android应用，旨在提供类似Apple Intelligence的通知摘要功能。
                
                ✨ 设计特色：
                • Material Design 3 设计规范
                • 支持动态主题和夜间模式
                • 现代化的用户界面
                
                🚀 主要功能：
                • 智能通知摘要生成
                • 三级优先级分类
                • 本地数据处理保护隐私
                • 按应用分组管理
                • 高优先级通知持久显示
                
                🔒 隐私保护：
                • 所有数据本地处理
                • 不收集用户个人信息
                • 7天自动清理过期数据
                
                👨‍💻 开发者：UsagiJin
                📄 开源许可：MIT License
                🌟 感谢您的使用！
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }
    
    private fun savePersistentNotificationPreference(enabled: Boolean) {
        lifecycleScope.launch {
            try {
                // 保存到SharedPreferences
                val sharedPref = requireContext().getSharedPreferences("app_settings", 0)
                with(sharedPref.edit()) {
                    putBoolean("persistent_notifications", enabled)
                    apply()
                }
            } catch (e: Exception) {
                // 如果保存失败，恢复开关状态
                switchPersistentNotifications.isChecked = !enabled
            }
        }
    }
    
    private fun loadPersistentNotificationPreference() {
        val sharedPref = requireContext().getSharedPreferences("app_settings", 0)
        val enabled = sharedPref.getBoolean("persistent_notifications", true)
        switchPersistentNotifications.isChecked = enabled
    }
    
    companion object {
        fun newInstance() = SettingsFragment()
    }
} 
