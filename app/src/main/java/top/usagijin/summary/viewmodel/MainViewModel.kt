package top.usagijin.summary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import top.usagijin.summary.data.NotificationData
import top.usagijin.summary.data.SummaryData
import top.usagijin.summary.repository.NotificationRepository
import top.usagijin.summary.utils.PermissionHelper

/**
 * 主界面ViewModel
 * 管理通知和摘要数据的加载和状态
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = NotificationRepository.getInstance(application)
    
    // 权限状态
    private val _permissionStatus = MutableStateFlow(PermissionHelper.getPermissionStatus(application))
    val permissionStatus: StateFlow<PermissionHelper.PermissionStatus> = _permissionStatus.asStateFlow()
    
    // 通知数据
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications.asStateFlow()
    
    // 摘要数据
    private val _summaries = MutableStateFlow<List<SummaryData>>(emptyList())
    val summaries: StateFlow<List<SummaryData>> = _summaries.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // 数据统计
    private val _dataStats = MutableStateFlow(Pair(0, 0)) // (通知数, 摘要数)
    val dataStats: StateFlow<Pair<Int, Int>> = _dataStats.asStateFlow()
    
    // 排序方式
    private val _sortBy = MutableStateFlow(SortBy.TIME)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()
    
    init {
        // 初始化时检查权限并加载数据
        checkPermissionAndLoadData()
        
        // 监听数据变化
        observeDataChanges()
    }
    
    /**
     * 检查权限并加载数据
     */
    fun checkPermissionAndLoadData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // 更新权限状态
                _permissionStatus.value = PermissionHelper.getPermissionStatus(getApplication())
                
                if (_permissionStatus.value == PermissionHelper.PermissionStatus.GRANTED) {
                    loadData()
                }
                
                // 加载数据统计
                loadDataStats()
                
            } catch (e: Exception) {
                _errorMessage.value = "加载数据时出错: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载通知和摘要数据
     */
    private fun loadData() {
        // 不需要在这里直接collect，因为在observeDataChanges()中已经处理了
        // 这里只是触发数据更新
    }
    
    /**
     * 监听数据变化
     */
    private fun observeDataChanges() {
        viewModelScope.launch {
            // 监听通知数据变化
            repository.getRecentNotifications(50)
                .catch { e -> _errorMessage.value = "通知数据加载失败: ${e.message}" }
                .collect { notificationList ->
                    _notifications.value = sortNotifications(notificationList)
                }
        }
        
        viewModelScope.launch {
            // 监听摘要数据变化
            repository.getRecentSummaries(50)
                .catch { e -> _errorMessage.value = "摘要数据加载失败: ${e.message}" }
                .collect { summaryList ->
                    _summaries.value = sortSummaries(summaryList)
                }
        }
    }
    
    /**
     * 加载数据统计
     */
    private suspend fun loadDataStats() {
        try {
            val stats = repository.getDataStats()
            _dataStats.value = stats
        } catch (e: Exception) {
            _errorMessage.value = "统计数据加载失败: ${e.message}"
        }
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        checkPermissionAndLoadData()
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 设置排序方式
     */
    fun setSortBy(sortBy: SortBy) {
        if (_sortBy.value != sortBy) {
            _sortBy.value = sortBy
            
            // 重新排序当前数据
            _notifications.value = sortNotifications(_notifications.value)
            _summaries.value = sortSummaries(_summaries.value)
        }
    }
    
    /**
     * 排序通知列表
     */
    private fun sortNotifications(notifications: List<NotificationData>): List<NotificationData> {
        return when (_sortBy.value) {
            SortBy.TIME -> notifications.sortedByDescending { it.time }
            SortBy.APP_NAME -> notifications.sortedBy { it.appName }
        }
    }
    
    /**
     * 排序摘要列表
     */
    private fun sortSummaries(summaries: List<SummaryData>): List<SummaryData> {
        return when (_sortBy.value) {
            SortBy.TIME -> summaries.sortedByDescending { it.time }
            SortBy.APP_NAME -> summaries.sortedBy { it.appName }
        }
    }
    
    /**
     * 打开权限设置
     */
    fun openPermissionSettings() {
        PermissionHelper.openNotificationListenerSettings(getApplication())
    }
    
    /**
     * 清理旧数据
     */
    fun cleanOldData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.cleanOldData()
                loadDataStats()
                _errorMessage.value = "旧数据清理完成"
            } catch (e: Exception) {
                _errorMessage.value = "清理数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 排序方式枚举
     */
    enum class SortBy {
        TIME,      // 按时间排序
        APP_NAME   // 按应用名排序
    }
} 