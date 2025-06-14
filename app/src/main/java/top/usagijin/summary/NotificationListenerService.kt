package top.usagijin.summary

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import top.usagijin.summary.api.ApiService
import top.usagijin.summary.data.NotificationData
import top.usagijin.summary.repository.NotificationRepository
import top.usagijin.summary.utils.NotificationDisplayManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * 通知监听服务
 * 实现通知拦截、触发规则判断和摘要生成逻辑
 */
class NotificationListenerService : NotificationListenerService() {
    
    private val TAG = "NotificationListenerService"
    
    // 协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 数据仓库和API服务
    private lateinit var repository: NotificationRepository
    private lateinit var apiService: ApiService
    private lateinit var displayManager: NotificationDisplayManager
    
    // 日期格式化器
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    // 通知计数器（包名 -> 计数）
    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts
    
    // 高频限制标记（包名 -> 暂停结束时间）
    private val highFrequencyPause = ConcurrentHashMap<String, Long>()
    
    // 待处理任务（包名 -> Job）
    private val pendingJobs = ConcurrentHashMap<String, Job>()
    
    // 违禁词集合 - 一般违禁词
    private val bannedWords = setOf(
        // 政治敏感词
        "政治", "政府", "领导人", "总统", "主席", "总理", "党", "政党", "选举", "投票", "政策",
        "政治制度", "民主", "专制", "独裁", "政变", "革命", "抗议", "示威", "游行", "罢工",
        
        // 暴力恐怖词汇
        "暴力", "恐怖", "炸弹", "爆炸", "枪支", "武器", "杀害", "谋杀", "恐怖主义", "恐怖分子",
        "暴徒", "袭击", "攻击", "刺杀", "绑架", "劫持", "威胁", "恐吓",
        
        // 色情违法词汇
        "色情", "裸体", "性爱", "成人", "淫秽", "猥亵", "性交", "做爱", "性器官",
        "卖淫", "嫖娼", "性服务", "援交", "包养", "一夜情", "约炮",
        
        // 毒品相关
        "毒品", "吸毒", "贩毒", "毒贩", "海洛因", "可卡因", "冰毒", "摇头丸", "大麻", "鸦片",
        "吗啡", "芬太尼", "K粉", "麻古", "毒针", "注射器",
        
        // 赌博相关
        "赌博", "赌场", "赌注", "下注", "押注", "博彩", "彩票", "六合彩", "赌球", "赌马",
        "老虎机", "轮盘", "德州扑克", "百家乐", "21点", "麻将赌博", "网络赌博",
        
        // 诈骗相关
        "诈骗", "骗子", "骗局", "传销", "庞氏骗局", "网络诈骗", "电信诈骗", "金融诈骗",
        "虚假广告", "假冒产品", "山寨", "盗版", "非法集资", "洗钱",
        
        // 仇恨言论
        "种族歧视", "性别歧视", "宗教歧视", "仇恨", "歧视", "偏见", "侮辱", "谩骂",
        "人身攻击", "网络暴力", "霸凌", "欺凌",
        
        // 其他违法行为
        "偷盗", "抢劫", "盗窃", "走私", "逃税", "贪污", "腐败", "受贿", "行贿",
        "伪造", "仿冒", "假证", "假币", "非法经营", "违法", "犯罪"
    )
    
    // 英文违禁词
    private val bannedWordsEnglish = setOf(
        // Political sensitive words
        "politics", "government", "president", "chairman", "minister", "party", "election", "vote",
        "democracy", "dictatorship", "revolution", "protest", "demonstration", "strike",
        
        // Violence and terrorism
        "violence", "terror", "bomb", "explosion", "gun", "weapon", "kill", "murder", "terrorism",
        "terrorist", "attack", "assassination", "kidnap", "hijack", "threat",
        
        // Adult content
        "porn", "pornography", "nude", "naked", "sex", "adult", "erotic", "obscene", "prostitution",
        "escort", "hookup",
        
        // Drugs
        "drug", "drugs", "heroin", "cocaine", "marijuana", "cannabis", "opium", "morphine",
        "fentanyl", "methamphetamine", "ecstasy", "narcotics",
        
        // Gambling
        "gambling", "casino", "bet", "betting", "lottery", "poker", "blackjack", "roulette",
        "slot machine", "online gambling",
        
        // Fraud
        "fraud", "scam", "ponzi", "pyramid scheme", "fake", "counterfeit", "money laundering",
        
        // Hate speech
        "racism", "sexism", "discrimination", "hate", "insult", "abuse", "bullying", "harassment",
        
        // Other illegal activities
        "theft", "robbery", "smuggling", "tax evasion", "corruption", "bribery", "forgery",
        "illegal", "crime", "criminal"
    )
    
    // 违禁词正则表达式模式（用于更复杂的匹配）
    private val bannedPatterns = listOf(
        // QQ号码模式
        Pattern.compile("[1-9][0-9]{4,10}"),
        // 微信号模式
        Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]{5,19}"),
        // 银行卡号模式
        Pattern.compile("\\d{16,19}"),
        // 身份证号模式
        Pattern.compile("\\d{17}[\\dXx]"),
        // 可疑链接模式
        Pattern.compile("(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/\\S*)?"),
        // 金钱诱惑模式
        Pattern.compile("\\d+万?元?\\s*(奖金|现金|红包|收益|利润|回报)"),

    )
    
    // 常量定义
    companion object {
        private const val SINGLE_NOTIFICATION_DELAY = 5000L // 5秒
        private const val MULTIPLE_NOTIFICATIONS_DELAY = 10000L // 10秒
        private const val HIGH_FREQUENCY_PAUSE_DELAY = 30000L // 30秒
        private const val HIGH_FREQUENCY_THRESHOLD = 10 // 10条通知
        private const val SINGLE_NOTIFICATION_LENGTH_THRESHOLD = 26 // 26字符
        private const val MAX_SINGLE_NOTIFICATION_CHARS = 1000 // 单条通知最大字符数
        private const val MAX_MULTIPLE_NOTIFICATIONS_CHARS = 2000 // 多条通知最大字符数
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "NotificationListenerService created")
        
        // 初始化组件
        repository = NotificationRepository.getInstance(this)
        apiService = ApiService.getInstance(this)
        displayManager = NotificationDisplayManager.getInstance(this)
        
        // 启动批量处理工作
        startBatchProcessingWork()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "NotificationListenerService destroyed")
        
        // 取消所有协程
        serviceScope.cancel()
        
        // 取消所有待处理任务
        pendingJobs.values.forEach { it.cancel() }
        pendingJobs.clear()
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        // 异步处理通知
        serviceScope.launch {
            handleNotificationPosted(sbn)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }
    
    /**
     * 处理新到达的通知
     */
    private suspend fun handleNotificationPosted(sbn: StatusBarNotification) {
        try {
            // 解析通知数据
            val notificationData = parseNotification(sbn) ?: return

            if (notificationData.title == null && notificationData.content == null) {
                Log.d(TAG, "Ignoring notification with no title or content: ${notificationData.packageName}")
                return
            }

            if (notificationData.packageName == "top.usagijin.summary") {
                Log.d(TAG, "Ignoring notification from our own app: ${notificationData.packageName}")
                return
            }

            // 清理敏感词内容
            val sanitizedNotification = sanitizeNotificationContent(notificationData)
            
            // 如果清理后内容为空或过短，则忽略
            if (sanitizedNotification == null) {
                Log.d(TAG, "Notification content too short after sanitization, ignoring: ${notificationData.packageName}")
                return
            }

            // 忽略音乐软件的播放通知，还要设备互联等
            if (shouldIgnoreNotification(sanitizedNotification)) {
                Log.d(TAG, "Ignoring filtered notification from ${sanitizedNotification.packageName}: ${sanitizedNotification.title}")
                return
            }

            Log.i(TAG, "Received notification from ${sanitizedNotification.appName}: ${sanitizedNotification.title}")
            
            // 保存清理后的通知到数据库
            repository.addNotification(sanitizedNotification)
            
            // 更新通知计数
            updateNotificationCount(sanitizedNotification.packageName)
            
            // 检查是否需要触发摘要
            checkAndTriggerSummarization(sanitizedNotification)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling notification", e)
        }
    }
    
    /**
     * 清理通知内容中的敏感词
     */
    private fun sanitizeNotificationContent(notification: NotificationData.Standard): NotificationData.Standard? {
        val originalTitle = notification.title ?: ""
        val originalContent = notification.content ?: ""
        
        // 清理标题和内容中的敏感词
        val sanitizedTitle = removeBannedWords(originalTitle)
        val sanitizedContent = removeBannedWords(originalContent)
        
        // 检查清理后的内容长度
        val totalLength = sanitizedTitle.length + sanitizedContent.length
        if (totalLength < 3) {
            Log.d(TAG, "Content too short after sanitization: ${notification.packageName}")
            return null
        }
        
        // 如果内容有变化，记录日志
        if (sanitizedTitle != originalTitle || sanitizedContent != originalContent) {
            Log.i(TAG, "Sanitized notification content from ${notification.packageName}")
        }
        
        return notification.copy(
            title = sanitizedTitle.ifBlank { null },
            content = sanitizedContent.ifBlank { null }
        )
    }
    
    /**
     * 移除文本中的违禁词
     */
    private fun removeBannedWords(text: String): String {
        if (text.isBlank()) return text
        
        var sanitizedText = text
        
        // 1. 移除中文违禁词
        bannedWords.forEach { word ->
            sanitizedText = sanitizedText.replace(word, "***", ignoreCase = true)
        }
        
        // 2. 移除英文违禁词
        bannedWordsEnglish.forEach { word ->
            sanitizedText = sanitizedText.replace(word, "***", ignoreCase = true)
        }
        
        // 3. 处理正则表达式模式
        bannedPatterns.forEach { pattern ->
            sanitizedText = pattern.matcher(sanitizedText).replaceAll("***")
        }
        
        // 4. 清理可疑模式
        sanitizedText = cleanSuspiciousPatterns(sanitizedText)
        
        // 5. 清理多余的星号和空格
        sanitizedText = sanitizedText
            .replace(Regex("\\*{4,}"), "***")  // 将4个以上的星号替换为3个
            .replace(Regex("\\s+"), " ")       // 将多个空格替换为单个空格
            .trim()
        
        return sanitizedText
    }
    
    /**
     * 清理可疑模式
     */
    private fun cleanSuspiciousPatterns(text: String): String {
        var cleanedText = text
        
        // 1. 处理手机号码模式
        cleanedText = cleanedText.replace(Regex("1[3-9]\\d{9}"), "***")
        
        // 2. 处理QQ号码模式
        cleanedText = cleanedText.replace(Regex("(?<!\\d)[1-9]\\d{4,10}(?!\\d)"), "***")
        
        // 3. 处理银行卡号模式
        cleanedText = cleanedText.replace(Regex("\\d{16,19}"), "***")
        
        // 4. 处理身份证号模式
        cleanedText = cleanedText.replace(Regex("\\d{17}[\\dXx]"), "***")
        
        // 5. 处理可疑链接
        cleanedText = cleanedText.replace(
            Regex("(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/\\S*)?"), 
            "[链接已屏蔽]"
        )
        
        // 6. 处理金钱诱惑内容
        cleanedText = cleanedText.replace(
            Regex("\\d+万?元?\\s*(奖金|现金|红包|收益|利润|回报)"), 
            "***"
        )
        
        // 7. 处理紧急诱导内容
        cleanedText = cleanedText.replace(
            Regex("(紧急|急|立即|马上|速度|快速)\\s*(联系|转账|汇款|支付)"), 
            "***"
        )
        
        // 8. 处理过多特殊字符
        if (hasExcessiveSpecialChars(cleanedText)) {
            cleanedText = cleanedText.replace(Regex("[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]{3,}"), "***")
        }
        
        return cleanedText
    }
    
    /**
     * 检查是否有过多特殊字符
     */
    private fun hasExcessiveSpecialChars(text: String): Boolean {
        val specialCharCount = text.count { it in "!@#$%^&*()_+-=[]{}|;':\",./<>?" }
        return specialCharCount > text.length * 0.3
    }
    
    /**
     * 清理重复字符
     */
    private fun cleanRepeatedChars(text: String): String {
        if (text.length < 4) return text
        
        var cleanedText = text
        var i = 0
        
        while (i < cleanedText.length - 3) {
            val char = cleanedText[i]
            var repeatCount = 1
            var j = i + 1
            
            // 计算连续重复字符数量
            while (j < cleanedText.length && cleanedText[j] == char) {
                repeatCount++
                j++
            }
            
            // 如果重复超过4次，替换为3个字符
            if (repeatCount > 4) {
                val replacement = char.toString().repeat(3)
                cleanedText = cleanedText.substring(0, i) + replacement + cleanedText.substring(j)
                i += 3
            } else {
                i++
            }
        }
        
        return cleanedText
    }
    
    /**
     * 判断是否应该忽略该通知
     */
    private fun shouldIgnoreNotification(notification: NotificationData.Standard): Boolean {
        val packageName = notification.packageName
        val title = notification.title?.toLowerCase() ?: ""
        val content = notification.content?.toLowerCase() ?: ""
        
        // 注意：这里不再检查违禁词，因为已经在sanitizeNotificationContent中处理了
        
        // 1. 忽略音乐播放相关的通知内容
        val musicKeywords = setOf(
            "正在播放", "now playing", "playing", "paused", "暂停",
            "上一首", "下一首", "previous", "next", "skip",
            "音乐", "music", "song", "歌曲", "专辑", "album",
            "艺术家", "artist", "播放器", "player"
        )
        
        if (musicKeywords.any { keyword -> title.contains(keyword) || content.contains(keyword) }) {
            return true
        }
        
        // 2. 忽略系统设备互联通知
        val systemPackages = setOf(
            "com.android.systemui",        // 系统UI
            "com.android.system",          // 系统
            "com.miui.mishare.connectivity", // 小米互传
            "com.huawei.nearby",           // 华为畅连
            "com.oppo.nearlink",           // OPPO互联
            "com.vivo.easyshare",          // vivo互传
            "com.samsung.android.beaconmanager", // 三星设备连接
            "com.google.android.gms",      // Google服务
            "com.android.bluetooth",       // 蓝牙
            "com.android.wifi.resources",  // WiFi
            "com.android.settings",        // 设置
            "com.miui.securitycenter",     // 小米安全中心
            "com.huawei.systemmanager",    // 华为手机管家
            "com.coloros.safecenter",      // ColorOS安全中心
            "com.vivo.abe"                 // vivo系统管理
        )
        
        if (packageName in systemPackages) {
            return true
        }
        
        // 3. 忽略空内容或过短的通知
        val totalLength = (notification.title?.length ?: 0) + (notification.content?.length ?: 0)
        if (totalLength < 3) {
            return true
        }
        
        // 4. 忽略持续性通知（如下载进度、播放状态等）
        if (notification.isOngoing) {
            return true
        }
        
        return false
    }
    
    /**
     * 解析通知数据
     */
    private fun parseNotification(sbn: StatusBarNotification): NotificationData.Standard? {
        return runCatching {
            val notification = sbn.notification
            val extras = notification.extras
            
            // 提取基本信息
            val packageName = sbn.packageName
            
            // 过滤掉自己应用的通知
            // if (packageName == "top.usagijin.summary") {
            //     Log.d(TAG, "Ignoring notification from our own app: $packageName")
            //     return null
            // }
            
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            
            // 过滤掉没有标题或内容的通知
            // if (title.isNullOrBlank() && content.isNullOrBlank()) {
            //     Log.d(TAG, "Ignoring notification with no title or content: $packageName")
            //     return null
            // }
            
            val time = dateFormat.format(Date(sbn.postTime))
            val isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0
            
            // 获取应用名称
            val appName = try {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
            
            // 生成唯一ID - 使用更可靠的方式避免重复
            val id = "${packageName}_${sbn.key}_${sbn.postTime}"
            
            NotificationData.Standard(
                id = id,
                packageName = packageName,
                appName = appName,
                title = title,
                content = content,
                time = time,
                isOngoing = isOngoing
            )
        }.getOrElse { e ->
            Log.e(TAG, "Failed to parse notification", e)
            null
        }
    }
    
    /**
     * 更新通知计数
     */
    private fun updateNotificationCount(packageName: String) {
        val currentCounts = _notificationCounts.value.toMutableMap()
        currentCounts[packageName] = (currentCounts[packageName] ?: 0) + 1
        _notificationCounts.value = currentCounts
        
        Log.d(TAG, "Updated count for $packageName: ${currentCounts[packageName]}")
    }
    
    /**
     * 检查并触发摘要生成
     */
    private suspend fun checkAndTriggerSummarization(notification: NotificationData.Standard) {
        val packageName = notification.packageName
        val currentCount = notificationCounts.value[packageName] ?: 0
        
        // 检查是否在高频限制期间
        val pauseEndTime = highFrequencyPause[packageName]
        if (pauseEndTime != null && System.currentTimeMillis() < pauseEndTime) {
            Log.d(TAG, "Package $packageName is in high-frequency pause")
            return
        }
        
        // 高频限制：>10条通知在10秒内
        if (currentCount > HIGH_FREQUENCY_THRESHOLD) {
            triggerHighFrequencyRestriction(packageName)
            return
        }
        
        val contentLength = (notification.content?.length ?: 0)
        
        when {
            // 场景1：单条长通知 (> 26字符)
            contentLength > SINGLE_NOTIFICATION_LENGTH_THRESHOLD -> {
                scheduleSingleNotificationSummarization(packageName)
            }
            
            // 场景2：多条通知 (≥2条在10秒内)
            currentCount >= 2 -> {
                scheduleMultipleNotificationsSummarization(packageName)
            }
            
            // 场景3：短通知 (≤ 26字符) - 仅存储，不触发摘要
            else -> {
                Log.d(TAG, "Short notification stored without summarization: $packageName")
            }
        }
    }
    
    /**
     * 调度单条通知摘要（5秒延迟）
     */
    private fun scheduleSingleNotificationSummarization(packageName: String) {
        // 取消之前的任务
        pendingJobs[packageName]?.cancel()
        
        val job = serviceScope.launch {
            try {
                delay(SINGLE_NOTIFICATION_DELAY)
                
                // 检查是否有新通知
                if (hasNewNotificationsSince(packageName, SINGLE_NOTIFICATION_DELAY)) {
                    Log.d(TAG, "New notifications received, skipping single notification summarization")
                    return@launch
                }
                
                // 获取最新通知
                val notifications = repository.getNotificationsByPackage(
                    packageName, 
                    SINGLE_NOTIFICATION_DELAY + 1000
                ).first().take(1)
                
                if (notifications.isNotEmpty()) {
                    generateSummary(notifications, "Single Long Notification")
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "Single notification summarization cancelled for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error in single notification summarization", e)
            } finally {
                pendingJobs.remove(packageName)
                resetNotificationCount(packageName)
            }
        }
        
        pendingJobs[packageName] = job
    }
    
    /**
     * 调度多条通知摘要（10秒延迟）
     */
    private fun scheduleMultipleNotificationsSummarization(packageName: String) {
        // 取消之前的任务
        pendingJobs[packageName]?.cancel()
        
        val job = serviceScope.launch {
            try {
                delay(MULTIPLE_NOTIFICATIONS_DELAY)
                
                // 获取最近10秒内的通知
                val notifications = repository.getNotificationsByPackage(
                    packageName, 
                    MULTIPLE_NOTIFICATIONS_DELAY + 1000
                ).first().take(5)
                
                if (notifications.size >= 2) {
                    generateSummary(notifications, "Multiple Notifications")
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "Multiple notifications summarization cancelled for $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error in multiple notifications summarization", e)
            } finally {
                pendingJobs.remove(packageName)
                resetNotificationCount(packageName)
            }
        }
        
        pendingJobs[packageName] = job
    }
    
    /**
     * 触发高频限制
     */
    private fun triggerHighFrequencyRestriction(packageName: String) {
        Log.i(TAG, "High frequency restriction triggered for $packageName")
        
        // 设置暂停结束时间
        highFrequencyPause[packageName] = System.currentTimeMillis() + HIGH_FREQUENCY_PAUSE_DELAY
        
        serviceScope.launch {
            try {
                delay(HIGH_FREQUENCY_PAUSE_DELAY)
                
                // 获取最近的通知
                val notifications = repository.getNotificationsByPackage(
                    packageName, 
                    HIGH_FREQUENCY_PAUSE_DELAY + 1000
                ).first().take(10)
                
                if (notifications.isNotEmpty()) {
                    generateSummary(notifications, "High-Frequency Restriction")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in high frequency restriction handling", e)
            } finally {
                highFrequencyPause.remove(packageName)
                resetNotificationCount(packageName)
            }
        }
    }
    
    /**
     * 启动批量处理工作（每2分钟执行一次）
     */
    private fun startBatchProcessingWork() {
        serviceScope.launch {
            while (isActive) {
                try {
                    delay(120000) // 2分钟
                    processBatchNotifications()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in batch processing", e)
                }
            }
        }
    }
    
    /**
     * 批量处理未处理的通知
     */
    private suspend fun processBatchNotifications() {
        try {
            val unprocessedNotifications = repository.getUnprocessedNotifications(10).first()
            
            if (unprocessedNotifications.size >= 3) {
                Log.i(TAG, "Processing batch of ${unprocessedNotifications.size} notifications")
                generateSummary(unprocessedNotifications, "Low-Frequency Batch")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch processing", e)
        }
    }
    
    /**
     * 生成摘要
     */
    private suspend fun generateSummary(notifications: List<NotificationData>, scenario: String) {
        if (notifications.isEmpty()) return
        
        try {
            Log.i(TAG, "Generating summary for $scenario: ${notifications.size} notifications")
            
            // 截断通知内容以符合API限制
            val truncatedNotifications = truncateNotifications(notifications, scenario)
            
            // 调用API生成摘要
            val summary = apiService.getSummary(truncatedNotifications)
            
            if (summary != null) {
                // 保存摘要
                repository.addSummary(summary)
                
                // 标记通知为已处理
                val notificationIds = notifications.map { it.id }
                repository.markNotificationsAsProcessed(notificationIds)
                
                // 显示摘要通知
                displayManager.showSummaryNotification(summary)
                
                Log.i(TAG, "Summary generated successfully: ${summary.title}")
            } else {
                Log.w(TAG, "Failed to generate summary for $scenario")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating summary", e)
        }
    }
    
    /**
     * 截断通知内容
     */
    private fun truncateNotifications(notifications: List<NotificationData>, scenario: String): List<NotificationData> {
        val maxChars = when (scenario) {
            "Single Long Notification" -> MAX_SINGLE_NOTIFICATION_CHARS
            else -> MAX_MULTIPLE_NOTIFICATIONS_CHARS
        }
        
        var totalChars = 0
        val result = mutableListOf<NotificationData>()
        
        for (notification in notifications) {
            val notificationLength = (notification.title?.length ?: 0) + (notification.content?.length ?: 0)
            
            if (totalChars + notificationLength <= maxChars) {
                result.add(notification)
                totalChars += notificationLength
            } else {
                // 截断最后一条通知
                val remainingChars = maxChars - totalChars
                if (remainingChars > 50) { // 至少保留50个字符
                    val truncatedContent = truncateToLastSentence(notification.content, remainingChars)
                    val truncatedNotification = when (notification) {
                        is NotificationData.Standard -> notification.copy(content = truncatedContent)
                    }
                    result.add(truncatedNotification)
                }
                break
            }
        }
        
        return result
    }
    
    /**
     * 截断到最后一个完整句子
     */
    private fun truncateToLastSentence(content: String?, maxLength: Int): String? {
        if (content == null || content.length <= maxLength) return content
        
        val truncated = content.substring(0, maxLength)
        val lastSentenceEnd = truncated.lastIndexOfAny(charArrayOf('.', '!', '?', '。', '！', '？'))
        
        return if (lastSentenceEnd > 0) {
            truncated.substring(0, lastSentenceEnd + 1)
        } else {
            truncated
        }
    }
    
    /**
     * 检查是否有新通知
     */
    private suspend fun hasNewNotificationsSince(packageName: String, timeMs: Long): Boolean {
        val recentNotifications = repository.getNotificationsByPackage(packageName, timeMs).first()
        return recentNotifications.size > 1
    }
    
    /**
     * 重置通知计数
     */
    private fun resetNotificationCount(packageName: String) {
        val currentCounts = _notificationCounts.value.toMutableMap()
        currentCounts.remove(packageName)
        _notificationCounts.value = currentCounts
    }
    
    /**
     * 动态添加违禁词（可供外部调用）
     */
    fun addBannedWord(word: String) {
        if (word.isNotBlank()) {
            (bannedWords as MutableSet).add(word.toLowerCase())
            Log.i(TAG, "Added banned word: $word")
        }
    }
    
    /**
     * 动态移除违禁词（可供外部调用）
     */
    fun removeBannedWord(word: String) {
        if ((bannedWords as MutableSet).remove(word.toLowerCase())) {
            Log.i(TAG, "Removed banned word: $word")
        }
    }
    
    /**
     * 获取当前违禁词列表
     */
    fun getBannedWords(): Set<String> {
        return bannedWords.toSet()
    }
    
    /**
     * 获取清理统计信息
     */
    fun getSanitizationStats(): Map<String, Int> {
        // 这里可以添加统计逻辑，记录清理的词汇数量等
        return emptyMap()
    }
}