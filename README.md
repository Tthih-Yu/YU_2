# 安工大助手(YU)

## 项目概述
安工大助手(YU)是一款为安徽工程大学学生开发的Android应用，旨在提供校园生活便利工具。目前已实现宿舍电费监控、课表查询管理(含桌面小部件)、校园卡管理、待办事项等功能，为学生提供一站式校园生活服务。

## 主要功能

### 宿舍电费监控
- **实时查询**: 查询宿舍剩余电量，支持自定义刷新间隔
- **智能数据分析**: 
  - 多层次分析：短期(7天)、中期(30天)和长期(90天)数据综合分析
  - 季节性调整：根据春夏秋冬不同季节自动调整用电预测
  - 充值模式识别：在常规数据不足时，通过充值频率和金额分析用电习惯
- **精确预测**: 
  - 动态预测剩余可用天数，考虑季节变化因素
  - 智能权重合成，优先使用短期数据，提高预测准确性
- **趋势分析**:
  - 分析用电趋势（增加、减少、稳定）及变化百分比
  - 提供个性化用电建议和节电提醒
- **历史记录**: 
  - 记录并展示电费使用历史，支持日历视图
  - 自动识别充值记录和用电模式变化
- **异常处理**:
  - 数据不足时智能估算
  - 考虑充值情况对用电量计算的影响
- **定时记录**: 每日在关键时间点（23:50和00:00:01）自动记录电费数据
- **桌面小部件**: 
  - 支持在桌面添加小组件，实时显示电量余额和预计剩余天数
  - 每3小时自动更新，保持数据时效性

### 课表查询与管理
- **个人课表**: 获取和显示个人课程表
- **周次切换**: 支持按周查看课表
- **课程详情**: 显示课程时间、地点、教师等详细信息
- **手动添加/编辑课程**: 支持在周视图手动添加、修改和删除课程信息
- **周视图滑动切换**: 支持在周视图左右滑动切换周次
- **开课时间设置**: 支持自定义每节课的开始时间
- **课表导出**: 支持课表数据本地存储和导出
- **今日课表桌面小部件**:
  - 提供两种尺寸(4x2, 4x4)，满足不同桌面布局需求
  - 自动适配系统深色/浅色模式，优化视觉体验
  - 无课程时显示友好空状态提示
  - 点击课程项可直接跳转到应用内对应周次的周视图
  - 支持手动刷新数据
  - 可通过应用内设置添加小部件到桌面

### 校园卡管理
- **余额查询**: 查询校园卡余额
- **消费记录**: 查看校园卡消费明细
- **交易历史**: 展示历史交易记录
- **消费分析**: 分析消费类别和消费习惯

### 待办事项
- **任务管理**: 添加、编辑、删除待办任务
- **优先级设置**: 设置任务优先级
- **提醒功能**: 支持设置任务提醒

### 规划中的功能
- **成绩查询**: 集成教务系统成绩查询
- **校园通知**: 校园公告和通知集成
- **图书馆服务**: 图书查询、借阅管理
- **一卡通充值**: 校园卡在线充值功能
- **社交互动**: 校园社区和信息分享

## 技术架构
- **前端**: 使用Kotlin开发，采用MVVM架构模式
- **UI框架**: 混合使用Jetpack Compose和传统XML布局构建UI界面
- **本地存储**: 使用Room数据库进行本地数据持久化
- **网络请求**: 采用Retrofit和OkHttp处理网络通信
- **并发处理**: 使用Kotlin协程(Coroutines)处理异步操作
- **依赖注入**: 可能集成Hilt进行依赖管理 (当前未明确使用)
- **数据可视化**: 使用自定义View和图表库展示数据分析结果
- **后台任务**: 使用WorkManager执行定时任务 (电费记录、小部件更新)
- **桌面小部件**: 使用AppWidgetProvider、RemoteViewsService、RemoteViewsFactory、PendingIntent实现桌面组件
- **快捷方式**: 使用ShortcutManagerCompat实现应用内添加桌面小部件功能

## 项目结构说明

### 主要目录结构
```
app/                                    # 应用程序主目录
├── src/                                # 源代码目录
│   ├── main/                           # 主要代码
│   │   ├── java/com/tthih/yu/          # Java/Kotlin代码目录
│   │   │   ├── electricity/            # 电费相关功能模块
│   │   │   │   ├── ElectricityActivity.kt          # 电费主界面
│   │   │   │   ├── ElectricityHistoryActivity.kt   # 电费历史记录界面
│   │   │   │   ├── ElectricityViewModel.kt         # 电费视图模型
│   │   │   │   ├── ElectricityRepository.kt        # 电费数据仓库
│   │   │   │   ├── ElectricityDatabase.kt          # 电费本地数据库定义
│   │   │   │   ├── ElectricityData.kt              # 电费数据模型
│   │   │   │   ├── ElectricityHistoryData.kt       # 电费历史记录数据模型
│   │   │   │   ├── ElectricityDao.kt               # 电费数据访问对象
│   │   │   │   ├── ElectricityHistoryDao.kt        # 电费历史记录数据访问对象
│   │   │   │   ├── ElectricityScheduledWorker.kt   # 定时电费数据记录工作
│   │   │   │   ├── ElectricityWidgetProvider.kt    # 电费桌面小部件提供者
│   │   │   │   ├── ElectricityWidgetWorker.kt      # 电费小部件更新工作类
│   │   │   │   └── ElectricityApplication.kt       # 电费应用程序组件
│   │   │   ├── schedule/                # 课表相关功能模块
│   │   │   │   ├── ScheduleActivity.kt             # 课表主界面
│   │   │   │   ├── ScheduleViewModel.kt            # 课表视图模型
│   │   │   │   ├── ScheduleRepository.kt           # 课表数据仓库
│   │   │   │   ├── ScheduleDatabase.kt             # 课表本地数据库定义
│   │   │   │   ├── ScheduleData.kt                 # 课表数据模型
│   │   │   │   ├── ScheduleTimeNode.kt             # 上课时间节点模型
│   │   │   │   ├── ScheduleDao.kt                  # 课表数据访问对象
│   │   │   │   ├── ScheduleImportActivity.kt       # 课表导入界面
│   │   │   │   ├── ScheduleWidgetProvider.kt       # 课表小部件提供者 (4x2)
│   │   │   │   ├── ScheduleWidgetProviderLarge.kt  # 课表小部件提供者 (4x4)
│   │   │   │   ├── ScheduleWidgetService.kt        # 课表小部件后台服务
│   │   │   │   └── ScheduleWidgetViewsFactory.kt   # 课表小部件列表项视图工厂
│   │   │   ├── campuscard/              # 校园卡相关功能模块
│   │   │   │   ├── CampusCardActivity.kt           # 校园卡主界面
│   │   │   │   ├── CampusCardViewModel.kt          # 校园卡视图模型
│   │   │   │   ├── CampusCardComponents.kt         # 校园卡UI组件集合
│   │   │   │   ├── CampusCardRepository.kt         # 校园卡数据仓库
│   │   │   │   ├── CampusCardDatabase.kt           # 校园卡本地数据库定义
│   │   │   │   ├── CampusCardTransaction.kt        # 校园卡交易数据模型
│   │   │   │   ├── CampusCardDao.kt                # 校园卡数据访问对象
│   │   │   │   ├── CampusCardAnalytics.kt          # 消费数据分析工具
│   │   │   │   └── CampusCardChart.kt              # 自定义图表组件
│   │   │   ├── library/                # 图书馆相关功能模块
│   │   │   ├── todo/                   # 待办事项相关功能模块
│   │   │   ├── component/              # 共用组件
│   │   │   │   ├── NetworkUtil.kt                  # 网络工具类
│   │   │   │   ├── LocalStorage.kt                 # 本地存储工具
│   │   │   │   ├── DateTimeUtil.kt                 # 日期时间工具
│   │   │   │   ├── PermissionUtil.kt               # 权限管理工具
│   │   │   │   ├── NotificationUtil.kt             # 通知工具类
│   │   │   │   ├── EncryptionUtil.kt               # 加密工具
│   │   │   │   ├── UIComponents.kt                 # 通用UI组件集合
│   │   │   │   └── ErrorHandler.kt                 # 错误处理工具
│   │   │   ├── util/                   # 工具类
│   │   │   └── main/                   # 主界面相关
│   │   │       ├── MainActivity.kt                 # 主活动
│   │   │       ├── SplashActivity.kt               # 启动页面
│   │   │       └── ShortcutBroadcastReceiver.kt    # 桌面快捷方式创建回调接收器
│   │   └── about/                  # 关于页面功能
│   │       ├── AboutActivity.kt                # 关于页面活动类
│   │       ├── CheckUpdateActivity.kt          # 检查更新活动类
│   │       ├── UpdateLogActivity.kt            # 更新日志活动类
│   │       └── ui/                     # UI相关组件
│   │           └── theme/                          # 主题相关
│   └── res/                         # 资源文件目录
│       ├── anim/                    # 动画资源
│       │   └── btn_rotate.xml                   # 按钮旋转动画定义
│       ├── drawable/                # 图形资源
│       │   ├── ic_arrow_right.xml               # 右箭头图标
│       │   ├── ic_info.xml                      # 信息图标
│       │   ├── ic_electricity.xml               # 电费图标
│       │   ├── ic_schedule.xml                  # 课表图标
│       │   ├── ic_card.xml                      # 校园卡图标
│       │   ├── ic_library.xml                   # 图书馆图标
│       │   ├── ic_update.xml                    # 更新图标
│       │   ├── ic_feedback.xml                  # 反馈图标
│       │   ├── card_background.xml              # 卡片背景
│       │   └── widget_gradient_background.xml   # 小部件渐变背景
│       ├── layout/                  # 布局文件
│       │   ├── activity_main.xml                # 主页面布局
│       │   ├── activity_login.xml               # 登录页面布局
│       │   ├── activity_splash.xml              # 启动页面布局
│       │   ├── activity_about.xml               # 关于页面布局
│       │   ├── activity_privacy_policy.xml      # 隐私政策页面布局
│       │   ├── activity_terms.xml               # 使用条款页面布局
│       │   ├── activity_electricity.xml         # 电费页面布局
│       │   ├── activity_electricity_history.xml # 电费历史页面布局
│       │   ├── electricity_widget.xml           # 电费小部件布局
│       │   ├── activity_schedule.xml            # 课表页面布局
│       │   ├── schedule_widget_layout.xml          # 课表小部件布局 (4x2)
│       │   ├── schedule_widget_layout_large.xml    # 课表小部件布局 (4x4)
│       │   ├── schedule_widget_list_item.xml     # 课表小部件列表项布局
│       │   ├── schedule_widget_empty_view.xml      # 课表小部件空状态布局
│       │   ├── activity_update_log.xml           # 更新日志页面布局
│       │   ├── item_version_history.xml          # 更新日志版本历史项布局
│       │   └── item_version_change.xml           # 更新日志版本变更项布局
│       ├── values/                  # 值资源
│       │   ├── colors.xml                       # 颜色资源
│       │   ├── strings.xml                      # 字符串资源
│       │   ├── styles.xml                       # 样式资源
│       │   ├── dimens.xml                       # 尺寸资源
│       │   └── themes.xml                       # 主题资源
│       ├── values-night/           # 夜间模式值资源
│       │   └── colors.xml                      # 夜间模式颜色资源
│       ├── xml/                     # XML配置
│       │   ├── electricity_widget_info.xml      # 电费小部件信息配置
│       │   ├── schedule_widget_info.xml         # 课表小部件信息配置
│       │   ├── network_security_config.xml      # 网络安全配置
│       │   ├── backup_rules.xml                 # 备份规则配置
│       │   ├── data_extraction_rules.xml        # 数据提取规则
│       │   └── file_paths.xml                  # FileProvider路径配置 (用于APK安装)
│       └── mipmap/                  # 应用图标
│           ├── ic_launcher.png                  # 应用图标（各分辨率）
│           └── ic_launcher_round.png            # 圆形应用图标
├── androidTest/                     # Android测试目录
│   └── java/com/tthih/yu/test/      # 测试代码
│       ├── ElectricityTest.kt                   # 电费模块测试
│       ├── ScheduleTest.kt                      # 课表模块测试
│       └── CampusCardTest.kt                    # 校园卡模块测试
└── test/                            # 单元测试目录
    └── java/com/tthih/yu/unittest/  # 单元测试代码
        ├── RepositoryTest.kt                    # 数据仓库测试
        ├── ViewModelTest.kt                     # 视图模型测试
        └── UtilTest.kt                          # 工具类测试
├── build.gradle.kts                     # 应用级Gradle构建脚本
├── proguard-rules.pro                   # ProGuard规则配置
└── README.md                            # 项目说明文档
```

### 模块详解

#### 电费模块 (`electricity/`)
- **ElectricityActivity.kt**: 电费查询主界面实现，显示余额、日均用电量、预计可用天数和用电趋势
- **ElectricityHistoryActivity.kt**: 电费历史记录界面实现，支持日历视图和数据统计
- **ElectricityViewModel.kt**: 电费数据视图模型，处理业务逻辑和数据分析
- **ElectricityRepository.kt**: 电费数据仓库，处理数据获取、存储和智能分析算法实现
  - 包含多层次分析算法（短期、中期、长期）
  - 实现季节性调整和充值模式识别
  - 提供智能预测和趋势分析
- **ElectricityDatabase.kt**: 电费本地数据库定义，使用Room实现
- **ElectricityData.kt**: 电费数据模型，存储当前电费信息
- **ElectricityHistoryData.kt**: 电费历史记录数据模型，用于统计和趋势分析
- **ElectricityDao.kt/ElectricityHistoryDao.kt**: 数据访问对象，定义数据库操作
- **ElectricityScheduledWorker.kt**: 定时执行电费数据记录工作，在特定时间点（23:50和00:00:01）自动保存数据
- **ElectricityWidgetProvider.kt**: 电费桌面小部件提供者，显示实时电费信息
- **ElectricityWidgetWorker.kt**: 电费小部件后台更新工作类，每3小时自动刷新一次
- **ElectricityApplication.kt**: 应用程序组件，负责初始化和配置电费工作管理器

#### 课表模块 (`schedule/`)
- **ScheduleActivity.kt**: 课表主界面，使用Compose实现，包含日视图、周视图和设置页面。支持周次切换、课程详情查看、手动添加/编辑课程、周视图滑动切换、开课时间设置等。
- **ScheduleViewModel.kt**: 课表数据视图模型，处理课程数据加载、转换和本地数据库交互。
- **ScheduleRepository.kt**: 课表数据仓库，负责从网络或本地获取课表数据，处理数据解析和缓存。
- **ScheduleDatabase.kt / ScheduleDao.kt**: Room数据库相关，用于本地存储课表和时间节点数据。
- **ScheduleData.kt / ScheduleTimeNode.kt**: 数据模型。
- **ScheduleImportActivity.kt**: 用于从教务系统导入课表的界面。
- **Widget相关**:
  - `ScheduleWidgetProvider.kt` / `ScheduleWidgetProviderLarge.kt`: AppWidgetProvider实现，负责处理小部件更新、点击事件等。
  - `ScheduleWidgetService.kt`: RemoteViewsService，为小部件的ListView提供数据。
  - `ScheduleWidgetViewsFactory.kt`: RemoteViewsFactory，创建和绑定小部件ListView中每个列表项的视图。

#### 校园卡模块 (`campuscard/`)
- **CampusCardActivity.kt**: 校园卡管理主界面实现，显示余额、消费记录和充值入口
- **CampusCardViewModel.kt**: 校园卡数据视图模型，处理数据加载和分析
- **CampusCardComponents.kt**: 校园卡UI组件集合，包含自定义卡片视图和消费记录项
- **CampusCardRepository.kt**: 校园卡数据仓库
  - 对接校园卡系统API
  - 处理消费数据的分类和统计
  - 管理本地缓存与同步
- **CampusCardDatabase.kt**: 校园卡本地数据库定义，使用Room实现
- **CampusCardTransaction.kt**: 校园卡交易数据模型，记录交易时间、金额、类型和地点
- **CampusCardDao.kt**: 校园卡数据访问对象，定义数据库操作
- **CampusCardAnalytics.kt**: 消费数据分析工具，生成消费趋势和分类统计
- **CampusCardChart.kt**: 自定义图表组件，可视化展示消费数据

#### 通用组件 (`component/`)
- **NetworkUtil.kt**: 网络工具类，处理HTTP请求和响应
- **LocalStorage.kt**: 本地存储工具，管理SharedPreferences和加密数据
- **DateTimeUtil.kt**: 日期时间工具，提供各种日期格式化和计算函数
- **PermissionUtil.kt**: 权限管理工具，简化权限请求和检查流程
- **NotificationUtil.kt**: 通知工具类，创建和管理应用内通知
- **EncryptionUtil.kt**: 加密工具，保护敏感数据
- **UIComponents.kt**: 通用UI组件集合，包括加载指示器、对话框等
- **ErrorHandler.kt**: 错误处理工具，统一处理异常情况

#### 主界面实现 (`main/`)
- **MainActivity.kt**: 应用程序主入口和导航中心
  - 实现底部导航栏和功能选择
  - 处理模块间的导航和数据传递
  - 管理应用全局状态
- **MainViewModel.kt**: 主界面视图模型，处理共享数据和全局事件
- **SplashActivity.kt**: 启动页面实现，处理初始化和登录状态检查
- **ShortcutBroadcastReceiver.kt**: 桌面快捷方式创建回调接收器

#### 关于页面功能 (`about/`)
- **AboutActivity.kt**: 关于页面的主活动 (XML布局)
- **CheckUpdateActivity.kt**: 检查更新页面 (XML布局)
- **UpdateLogActivity.kt**: 更新日志页面 (XML布局 + RecyclerView)
- **VersionHistory.kt / VersionHistoryItem.kt**: 定义和存储应用的版本历史数据
- **UpdateInfo.kt**: 从服务器获取的更新信息数据类
- **EmailContactActivity.kt**: 邮件联系活动类
- **QQGroupActivity.kt**: QQ群活动类

## 系统要求
- Android 8.0 (API级别26)或更高版本
- 网络连接(用于查询校园服务数据、检查更新)
- 存储权限(用于保存本地数据、导出、下载更新APK)
- 通知权限(Android 13+，用于推送提醒和更新通知)

## 权限说明
应用需要以下权限才能正常工作:
- **网络访问权限** (`INTERNET`, `ACCESS_NETWORK_STATE`): 用于获取电费、课表、校园卡数据、检查更新等。
- **存储权限**:
  - `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` (Android 10及以下): 用于保存本地数据、导出、下载更新APK。
  - `READ_MEDIA_IMAGES` (Android 13+): 替代读取外部存储的部分权限。
  - `MANAGE_EXTERNAL_STORAGE` (Android 11+): 请求此权限以确保在特定场景下文件访问的兼容性，应用可不授予此权限运行。
- **通知权限** (`POST_NOTIFICATIONS`, Android 13+): 用于发送课程提醒、电量提醒、更新通知等。
- **桌面快捷方式/小部件**:
  - 添加桌面快捷方式: 在Android 8.0以下使用`com.android.launcher.action.INSTALL_SHORTCUT`广播；Android 8.0及以上使用`ShortcutManagerCompat.requestPinShortcut`，由系统处理权限请求。
  - 添加小部件: 通常由启动器处理，应用内添加(如课表设置)使用`AppWidgetManager.requestPinAppWidget`，由系统处理权限请求。

## 安装指南
1. 从应用商店下载安装包或直接下载APK文件。
2. 允许安装来自此来源的应用(如果是APK安装)。
3. 打开应用，首次启动会显示介绍页面，点击"立即开始"。
4. 根据系统提示授予必要的权限（网络、通知、存储）。
5. **添加小部件**:
   - **电费小部件**: 长按桌面空白处，选择"小部件"，找到"宿舍电费"小部件并添加到桌面。
   - **课表小部件**:
     - 方法一：长按桌面空白处，选择"小部件"，找到"今日课表(4x2)"或"今日课表(4x4)"并添加到桌面。
     - 方法二：在应用内进入"课表" -> "设置"，点击"添加桌面小部件"按钮，根据系统提示选择尺寸并添加。

## 使用方法
1. **电费查询**: 在主页点击"宿舍电费"，设置宿舍楼栋和房间号后即可查看。
2. **电费小部件**: 添加后自动显示电量，点击可进入电费详情页。
3. **课表查询**: 在主页点击"课程表"，首次使用可能需要登录教务系统或手动导入，之后可查看、编辑、设置课程。
4. **课表小部件**: 添加后显示当天课程，点击课程项可跳转到应用内周视图，点击刷新按钮可手动更新。
5. **校园卡管理**: 在主页点击"校园卡"，绑定账号后可查询余额和消费记录。
6. **待办事项**: 在主页点击"待办事项"，可添加、管理任务。
7. **检查更新/更新日志**: 在主页点击右下角"关于"按钮，进入关于页面，点击"检查更新"，可查看版本信息、检查更新、查看更新日志。

## 开发者信息
- 开发者: tthih
- 开源协议: MIT
- 项目仓库: [待添加]
- 联系方式: [待添加]

## 贡献指南
欢迎对项目提供建议或贡献代码，请遵循以下步骤:
1. Fork本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 免责声明
本应用是非官方应用，与安徽工程大学官方无关。数据来源于学校相关系统，仅供个人使用，不对数据准确性做保证。使用本应用产生的任何问题与开发者无关。

## 更新日志
(与 `app/src/main/java/com/tthih/yu/about/VersionHistory.kt` 同步)

### v1.1.0 (2025-4-22)
- **课表功能增强**:
  - 新增今日课表桌面小部件，支持4x2和4x4两种尺寸
  - 小部件支持深色模式自适应，优化空状态显示
  - 小部件课程项点击可跳转至应用内周视图
  - 新增应用内添加课表小部件功能
  - 周视图页面支持手动添加/编辑/删除课程信息
  - 周视图页面支持左右滑动切换周次
  - 新增自定义开课时间选项
- **其他**:
  - 新增更新日志页面
  - 修复了一些已知问题和闪退

### v1.0.0 (2025-4-8)
- YU校园助手首次发布
- 支持课表查询与管理
- 支持宿舍电量查询与分析预测、历史记录、桌面小部件
- 支持电费查询与充值 (基础对接)
- 支持待办事项管理

### 未来计划
- **电费功能增强**:
  - 添加机器学习模型，提高用电预测准确性
  - 集成气温数据，优化季节性预测
  - 提供用电行为模式识别（周末vs工作日模式）
  - 增加更丰富的数据可视化图表，支持同比/环比分析
  - 智能充值提醒功能，基于预测准确推送通知
- **课表功能完善**:
  - 优化手动添加课程的交互体验
  - 支持导入/导出自定义课程数据
- **成绩查询**: 集成教务系统成绩查询功能
- 增加图书馆服务功能
- 添加校园通知和公告集成
- 实现校园卡在线充值功能
- 优化性能和减少资源占用

# YU校园助手 - 关于页面功能说明

本文档详细描述了YU校园助手应用的"关于"页面功能及相关实现文件。

## 功能概述

"关于"功能通过主页面右下角的一个旋转按钮访问，提供应用的版本信息、联系方式、使用条款等内容，让用户了解应用的基本信息并获取支持。

## 文件结构及作用

### 1. Java/Kotlin 代码文件

#### `app/src/main/java/com/tthih/yu/AboutActivity.kt`
- **作用**：关于页面的主要活动类 (XML布局)
- **功能**：
  - 显示应用版本信息
  - 处理各种选项的点击事件（检查更新、加入社群、邮件联系等）
  - 实现使用协议、隐私政策等链接的跳转
  - 提供导航返回功能

#### `app/src/main/java/com/tthih/yu/MainActivity.kt`
- **作用**：主界面中添加关于按钮 (Compose UI)
- **功能**：
  - 在主界面右下角添加一个带旋转动画效果的圆形关于按钮
  - 实现点击按钮时跳转到AboutActivity的逻辑
  - 使用Compose动画API实现按钮的360度旋转效果

#### `app/src/main/java/com/tthih/yu/about/CheckUpdateActivity.kt`
- **作用**: 检查更新页面活动类 (XML布局)
- **功能**:
  - 显示当前版本和上次检查时间
  - 调用`NetworkUtil`检查服务器是否有新版本
  - 显示更新内容和更新按钮（如果可用）
  - 提供"查看更新日志"按钮入口，跳转到`UpdateLogActivity`

#### `app/src/main/java/com/tthih/yu/about/UpdateLogActivity.kt`
- **作用**: 更新日志页面活动类 (XML布局 + RecyclerView)
- **功能**:
  - 从`VersionHistory`加载版本历史数据
  - 使用`VersionHistoryAdapter`在RecyclerView中显示版本列表
  - 标记当前安装的版本

### 2. XML 布局文件

#### `app/src/main/res/layout/activity_about.xml`
- **作用**：定义关于页面的UI布局
- **内容**：
  - 实现顶部工具栏
  - 显示应用图标和版本号
  - 创建多个卡片样式的选项（检查更新、社群、联系方式等）
  - 底部显示版权信息

#### `app/src/main/res/layout/activity_check_update.xml`
- **作用**: 定义检查更新页面的UI布局
- **内容**:
  - 顶部工具栏
  - 版本信息卡片（含当前版本、上次检查时间、查看更新日志按钮）
  - 检查更新卡片（含刷新按钮、状态文本、更新内容区域、更新按钮）

#### `app/src/main/res/layout/activity_update_log.xml` / `item_version_history.xml` / `item_version_change.xml`
- **作用**: 定义更新日志页面的布局和列表项布局。

### 3. 图标和动画资源

#### `app/src/main/res/drawable/ic_arrow_right.xml`
- **作用**：关于页面列表项右侧的箭头图标
- **样式**：浅灰色的右箭头，指示可点击进入下一级

#### `app/src/main/res/drawable/ic_info.xml`
- **作用**：主页面右下角关于按钮上的信息图标
- **样式**：白色信息图标，包含字母"i"的圆形设计

#### `app/src/main/res/anim/btn_rotate.xml`
- **作用**：关于按钮的旋转动画定义
- **效果**：点击时按钮进行300毫秒的360度平滑旋转

### 4. 字符串和清单文件

#### `app/src/main/res/values/strings.xml`
- **作用**：存储关于页面使用的所有文本字符串
- **内容**：包含页面标题、选项名称、版权信息等多语言文本

#### `app/src/main/AndroidManifest.xml`
- **作用**：注册AboutActivity组件
- **配置**：
  - 声明活动名称和标签为"关于"
  - 指定父活动为MainActivity（支持向上导航）
  - 应用统一主题样式

## 功能实现细节

### 主页面关于按钮

主页面右下角的关于按钮使用Jetpack Compose实现，具有以下特点：
- 使用圆形设计，带有主题色背景
- 点击时执行360度旋转动画（使用animateFloatAsState实现）
- 使用graphicsLayer修饰符应用旋转效果

```kotlin
// 关于按钮实现代码片段
var rotationState by remember { mutableStateOf(0f) }
val rotation by animateFloatAsState(
    targetValue = rotationState,
    animationSpec = tween(durationMillis = 300),
    label = "rotation"
)

IconButton(
    onClick = {
        // 触发旋转动画
        rotationState += 360f
        onAboutClick()
    },
    modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
        .graphicsLayer(rotationZ = rotation)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_info),
        contentDescription = "关于",
        tint = Color.White,
        modifier = Modifier.size(20.dp)
    )
}
```

### 关于页面布局

关于页面使用传统XML布局设计，包含以下部分：
- 顶部带有返回按钮的工具栏
- 居中显示的应用图标和版本信息
- 使用CardView实现的选项列表，每个选项包含:
  - 选项名称（左对齐）
  - 右箭头图标（右对齐）
  - 点击效果（涟漪效果）
- 底部居中显示的备案号

### 点击处理和页面跳转

每个选项卡的点击事件在AboutActivity中处理：
- 使用findViewById获取各CardView对象
- 设置点击监听器执行相应操作：
  - 打开网页链接（Telegram群组、使用协议等）
  - 发送邮件（联系我们）
  - 显示Toast提示（检查更新、微信公众号等）

## 使用流程

1. 用户在主页面右下角点击旋转的"关于"按钮
2. 按钮执行360度旋转动画
3. 跳转到AboutActivity，显示应用信息和选项列表
4. 用户点击"检查更新"选项，跳转到CheckUpdateActivity
5. 在CheckUpdateActivity中，用户可以点击刷新图标检查更新，或点击"查看更新日志"按钮跳转到UpdateLogActivity查看历史版本信息
6. 用户可通过顶部返回按钮逐级返回

## 注意事项

- 所有文本内容使用strings.xml资源文件，方便后续多语言支持
- 图标使用矢量图形（Vector Drawable）以适应不同屏幕分辨率
- 关于页面中的外部链接（如隐私政策）需更新为实际可用的URL 
- `VersionHistory.kt` 需要在每次发布新版本时手动更新
