# Android 通知摘要助手

![Android](https://img.shields.io/badge/Android-API%2029+-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

一个模仿 Apple Intelligence 通知摘要功能的 Android 应用，使用现代化 Kotlin 开发，支持智能通知汇总、优先级显示和多应用管理。

## 🌟 功能特点

### 📱 核心功能

- **智能通知拦截**：实时监听所有应用通知
- **自动摘要生成**：使用模拟 API 生成 100-150 字符的简洁摘要
- **优先级管理**：支持高、中、低三级重要性分类
- **多应用支持**：同时处理多个应用的通知，支持分组显示
- **本地存储**：使用 Room 数据库本地存储通知和摘要数据

### 🎯 智能触发规则

| 场景                 | 触发条件        | 延迟时间  | 处理通知数量            | 描述                       |
| -------------------- | --------------- | --------- | ----------------------- | -------------------------- |
| **单条长通知** | 内容 > 26 字符  | 5 秒      | 1 条 (≤1000字符)       | 等待5秒无新通知后生成摘要  |
| **多条通知**   | ≥2条通知/10秒  | 10 秒     | 最近 5 条 (≤2000字符)  | 批量处理同应用通知         |
| **短通知**     | 内容 ≤ 26 字符 | -         | 仅存储                  | 不触发摘要，仅保存到数据库 |
| **低频批处理** | ≥3条未处理通知 | 2 分钟    | 最近 10 条 (≤2000字符) | 定期处理积压通知           |
| **高频限制**   | >10条通知/10秒  | 30 秒暂停 | 最近 10 条 (≤2000字符) | 防止频繁API调用            |

### 🎨 用户界面

- **Material Design 3**：现代化设计语言
- **双标签页结构**：通知历史 + 摘要列表
- **自定义颜色**：每个应用可配置专属颜色
- **响应式布局**：适配不同屏幕尺寸
- **权限引导**：友好的权限申请流程

### 📊 通知显示

- **分组通知**：按应用自动分组，支持展开
- **优先级显示**：高重要性摘要置顶显示
- **持久化通知**：重要摘要保持在通知栏
- **自定义样式**：BigTextStyle 和 InboxStyle 支持

## 🛠️ 技术栈

### 开发语言和框架

- **Kotlin** - 100% Kotlin 开发
- **Android SDK** - 最低支持 API 29 (Android 10)
- **Material Design 3** - 现代化UI设计

### 架构组件

- **MVVM 架构** - Model-View-ViewModel 设计模式
- **Coroutines & Flow** - 响应式编程和异步处理
- **Room Database** - 本地数据持久化
- **ViewModel & LiveData** - 数据绑定和状态管理
- **ViewBinding** - 类型安全的视图绑定

### 核心库

- **Retrofit** - 网络请求框架
- **OkHttp** - HTTP 客户端
- **WorkManager** - 后台任务调度
- **Navigation Component** - 页面导航
- **ViewPager2** - 滑动页面管理
- **RecyclerView** - 列表显示

## 🔧 安装和设置

### 系统要求

- Android 10 (API 29) 或更高版本
- 支持 HyperOS、HarmonyOS、ColorOS、OriginOS 等定制系统

### 安装步骤

1. **克隆项目**

```bash
git clone https://github.com/your-username/Android_Notification_Summarize_Native.git
cd Android_Notification_Summarize_Native
```

2. **使用 Android Studio 打开**

- 确保使用 Android Studio Hedgehog (2023.1.1) 或更新版本
- 等待 Gradle 同步完成

3. **构建和运行**

```bash
# 调试版本
./gradlew assembleDebug

# 发布版本
./gradlew assembleRelease
```

### 权限配置

应用需要以下权限：

1. **通知监听权限** (必需)

   - 设置 > 通知 > 特殊应用权限 > 通知使用权
   - 找到"通知摘要助手"并开启
2. **网络权限** (自动获取)

   - 用于模拟 API 调用
3. **前台服务权限** (自动获取)

   - 用于后台处理通知

## 📱 使用说明

### 首次使用

1. **启动应用**

   - 安装后首次打开会显示权限引导界面
2. **授予权限**

   - 点击"授予权限"按钮
   - 在系统设置中开启通知监听权限
   - 返回应用，权限状态会自动更新
3. **开始使用**

   - 权限授予后，应用会自动开始监听通知
   - 通知和摘要会实时显示在相应标签页

### 主要功能

#### 📋 通知列表

- 查看所有应用的通知历史
- 支持按时间或应用名排序
- 点击任意通知查看详情
- 下拉刷新获取最新数据

#### 📝 摘要列表

- 查看 AI 生成的通知摘要
- 重要性指示器显示优先级
- 应用颜色区分不同来源
- 持久化标识标记重要摘要

#### ⚙️ 设置选项

- **持久化通知**：控制高重要性摘要是否常驻通知栏
- **应用颜色**：为不同应用设置专属颜色
- **数据管理**：清理7天前的旧数据
- **关于信息**：查看应用版本和说明

### 快捷操作

- **排序**：使用主界面顶部的下拉菜单切换排序方式
- **刷新**：下拉任意列表页面刷新数据
- **设置**：点击浮动按钮快速进入设置
- **详情**：点击任意通知或摘要查看完整内容

## 🏗️ 项目结构

```
app/src/main/java/top/usagijin/summary/
├── data/                           # 数据模型
│   ├── NotificationData.kt         # 通知数据密封类
│   └── SummaryData.kt              # 摘要数据类
├── database/                       # 数据库相关
│   ├── NotificationEntity.kt       # 通知实体
│   ├── SummaryEntity.kt           # 摘要实体
│   ├── NotificationDao.kt         # 通知数据访问对象
│   ├── SummaryDao.kt              # 摘要数据访问对象
│   └── AppDatabase.kt             # 数据库定义
├── repository/                     # 数据仓库
│   └── NotificationRepository.kt   # 统一数据访问接口
├── api/                           # 网络API
│   ├── SummarizeApi.kt            # Retrofit API接口
│   └── ApiService.kt              # API服务实现
├── utils/                         # 工具类
│   ├── PermissionHelper.kt        # 权限管理工具
│   └── NotificationDisplayManager.kt # 通知显示管理
├── viewmodel/                     # 视图模型
│   └── MainViewModel.kt           # 主界面ViewModel
├── adapter/                       # 列表适配器
│   ├── NotificationAdapter.kt     # 通知列表适配器
│   └── SummaryAdapter.kt          # 摘要列表适配器
├── fragment/                      # Fragment组件
│   ├── NotificationsFragment.kt   # 通知列表Fragment
│   └── SummariesFragment.kt       # 摘要列表Fragment
├── MainActivity.kt                # 主界面Activity
├── NotificationDetailActivity.kt  # 详情页Activity
├── SettingsActivity.kt           # 设置页Activity
└── NotificationListenerService.kt # 通知监听服务
```

## 🔍 API 设计

### 模拟摘要 API

**请求格式:**

```json
{
  "currentTime": "2024-01-15 10:30:00",
  "data": [
    {
      "title": "新消息",
      "content": "今晚7点聚餐，地点在...",
      "time": "2024-01-15 10:30:00",
      "packageName": "com.tencent.mm"
    }
  ]
}
```

**响应格式:**

```json
{
  "title": "微信消息",
  "summary": "收到聚餐邀请，今晚7点餐厅见",
  "importanceLevel": 2
}
```

### 内置应用识别

应用支持主流应用的智能识别和分类：

- **即时通讯**: 微信、QQ、Telegram、WhatsApp
- **邮件**: Gmail、Outlook、网易邮箱
- **社交媒体**: Facebook、Twitter/X、Instagram
- **生产力**: 日历、银行、支付应用
- **通用**: 其他所有应用

## 🔒 隐私保护

- **本地处理**: 通知数据主要在本地处理，减少隐私泄露
- **数据加密**: 敏感信息存储时加密处理
- **自动清理**: 7天后自动删除旧数据
- **权限控制**: 仅申请必需的最小权限
- **无云同步**: 所有数据仅存储在本地设备

## 🧪 测试场景

### 功能测试

1. **单条长通知测试**

   - 发送超过26字符的通知
   - 验证5秒后生成摘要
2. **多条通知测试**

   - 10秒内发送多条通知
   - 验证批量处理功能
3. **高频限制测试**

   - 快速发送大量通知
   - 验证30秒暂停机制
4. **权限测试**

   - 测试权限申请流程
   - 验证权限状态检测

### 兼容性测试

在以下系统上验证功能：

- **原生 Android** (10+)
- **MIUI / HyperOS**
- **HarmonyOS**
- **ColorOS**
- **OriginOS**

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目使用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 📞 联系方式

- **项目主页**: [GitHub Repository](https://github.com/your-username/Android_Notification_Summarize_Native)
- **问题反馈**: [Issues](https://github.com/your-username/Android_Notification_Summarize_Native/issues)
- **邮箱**: your-email@example.com

## 🙏 致谢

- Material Design 团队提供的设计指南
- Android 开发团队提供的优秀框架
- 所有为开源社区贡献的开源社区的开发者们

---

**注意**: 这是一个演示项目，模拟 API 仅用于功能展示。实际部署时请替换为真实的摘要生成服务。
