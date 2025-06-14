// 测试推理功能的示例代码
// 这些是我们要测试的通知内容

val testNotifications = listOf(
    // 1. 微信群聊 - 应该生成18字以内摘要，重要级别4-5
    NotificationInput(
        title = "工作群",
        content = "张三: 明天的项目会议改到下午3点，请大家准时参加。李四: 收到，我会准备好相关资料。王五: 好的，没问题。",
        time = "2024-06-15 14:55:00",
        packageName = "com.tencent.mm"
    ),
    
    // 2. Gmail邮件 - 应该生成18字以内摘要，重要级别4-5
    NotificationInput(
        title = "重要邮件",
        content = "来自manager@company.com: 关于Q4季度总结会议安排 - 定于本周五上午10点在大会议室举行，请各部门负责人准备汇报材料，包括业绩数据和下季度计划。",
        time = "2024-06-15 14:56:00", 
        packageName = "com.google.android.gm"
    ),
    
    // 3. 短信通知 - 应该生成12字以内摘要，重要级别4-5
    NotificationInput(
        title = "银行通知",
        content = "【工商银行】您的账户于12月15日14:30发生一笔转账交易，金额5000.00元，余额12345.67元。如非本人操作请及时联系客服。",
        time = "2024-06-15 14:57:00",
        packageName = "com.android.mms"
    ),
    
    // 4. 新闻通知 - 应该生成10字以内摘要，重要级别最高2
    NotificationInput(
        title = "今日头条",
        content = "突发新闻：某科技公司发布最新AI产品，预计将改变行业格局。该产品采用先进的机器学习技术，在多个测试中表现优异。",
        time = "2024-06-15 14:58:00",
        packageName = "com.ss.android.article.news"
    ),
    
    // 5. QQ消息 - 应该生成18字以内摘要，重要级别3-4
    NotificationInput(
        title = "QQ好友消息", 
        content = "小明: 周末一起去看电影吗？新上映的科幻片评价很不错。小红: 好啊，什么时间？小明: 周六下午2点场次怎么样？",
        time = "2024-06-15 14:59:00",
        packageName = "com.tencent.mobileqq"
    )
)

// 期望的输出格式：
// {
//   "title": "工作群消息",
//   "summary": "【首要通知】会议改期至明天下午3点",  // 18字以内，重要级别4-5加前缀
//   "importanceLevel": 4
// }

// {
//   "title": "重要邮件", 
//   "summary": "【首要通知】Q4总结会议周五10点大会议室",  // 18字以内
//   "importanceLevel": 5
// }

// {
//   "title": "银行通知",
//   "summary": "【首要通知】转账5000元余额12345元",  // 12字以内
//   "importanceLevel": 5
// }

// {
//   "title": "科技新闻",
//   "summary": "AI产品发布改变格局",  // 10字以内，无前缀
//   "importanceLevel": 2
// }

// {
//   "title": "QQ消息",
//   "summary": "周末看电影约定时间",  // 18字以内
//   "importanceLevel": 3
// } 