#!/bin/bash

echo "开始发送测试通知..."

# 1. 微信群聊通知
echo "发送微信群聊通知..."
adb shell "am broadcast -a com.tencent.mm.NOTIFICATION --es title '工作群' --es content '张三: 明天的项目会议改到下午3点，请大家准时参加。李四: 收到，我会准备好相关资料。王五: 好的，没问题。' --es package 'com.tencent.mm'"

sleep 2

# 2. Gmail邮件通知  
echo "发送Gmail邮件通知..."
adb shell "am broadcast -a com.google.android.gm.NOTIFICATION --es title '重要邮件' --es content '来自manager@company.com: 关于Q4季度总结会议安排 - 定于本周五上午10点在大会议室举行，请各部门负责人准备汇报材料，包括业绩数据和下季度计划。' --es package 'com.google.android.gm'"

sleep 2

# 3. 短信通知
echo "发送短信通知..."
adb shell "am broadcast -a android.provider.Telephony.SMS_RECEIVED --es title '银行通知' --es content '【工商银行】您的账户于12月15日14:30发生一笔转账交易，金额5000.00元，余额12345.67元。如非本人操作请及时联系客服。' --es package 'com.android.mms'"

sleep 2

# 4. 新闻通知
echo "发送新闻通知..."
adb shell "am broadcast -a com.tencent.news.NOTIFICATION --es title '今日头条' --es content '突发新闻：某科技公司发布最新AI产品，预计将改变行业格局。该产品采用先进的机器学习技术，在多个测试中表现优异。' --es package 'com.ss.android.article.news'"

sleep 2

# 5. QQ消息通知
echo "发送QQ消息通知..."
adb shell "am broadcast -a com.tencent.mobileqq.NOTIFICATION --es title 'QQ好友消息' --es content '小明: 周末一起去看电影吗？新上映的科幻片评价很不错。小红: 好啊，什么时间？小明: 周六下午2点场次怎么样？' --es package 'com.tencent.mobileqq'"

echo "所有测试通知发送完成！" 