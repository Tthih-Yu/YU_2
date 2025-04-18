# 安工大助手(YU)

## 项目概述
安工大助手(YU)是一款为安徽工程大学学生开发的Android应用，旨在提供校园生活便利工具。目前已实现宿舍电费监控、课表查询和校园卡管理功能，为学生提供一站式校园生活服务。

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

### 课表查询
- **个人课表**: 获取和显示个人课程表
- **周次切换**: 支持按周查看课表
- **课程详情**: 显示课程时间、地点、教师等详细信息
- **课表导出**: 支持课表数据本地存储和导出

### 校园卡管理
- **余额查询**: 查询校园卡余额
- **消费记录**: 查看校园卡消费明细
- **交易历史**: 展示历史交易记录
- **消费分析**: 分析消费类别和消费习惯

### 规划中的功能
- **校园通知**: 校园公告和通知集成
- **图书馆服务**: 图书查询、借阅管理
- **一卡通充值**: 校园卡在线充值功能
- **社交互动**: 校园社区和信息分享

## 技术架构
- **前端**: 使用Kotlin开发，采用MVVM架构模式
- **UI框架**: 采用Jetpack Compose构建现代化UI界面
- **本地存储**: 使用Room数据库进行本地数据持久化
- **网络请求**: 采用Retrofit和OkHttp处理网络通信
- **并发处理**: 使用Kotlin协程(Coroutines)处理异步操作
- **依赖注入**: 集成Hilt进行依赖管理
- **数据可视化**: 使用自定义View和图表库展示数据分析结果
- **桌面小部件**: 使用AppWidgetProvider和WorkManager实现桌面组件

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
│   │   │   │   ├── ScheduleDao.kt                  # 课表数据访问对象
│   │   │   │   ├── ScheduleWorker.kt               # 课表定时更新工作类
│   │   │   │   ├── ScheduleWidget.kt               # 课表桌面小部件
│   │   │   │   └── ScheduleNotification.kt         # 课程提醒通知服务
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
│   │   │   ├── component/               # 共用组件
│   │   │   │   ├── NetworkUtil.kt                  # 网络工具类
│   │   │   │   ├── LocalStorage.kt                 # 本地存储工具
│   │   │   │   ├── DateTimeUtil.kt                 # 日期时间工具
│   │   │   │   ├── PermissionUtil.kt               # 权限管理工具
│   │   │   │   ├── NotificationUtil.kt             # 通知工具类
│   │   │   │   ├── EncryptionUtil.kt               # 加密工具
│   │   │   │   ├── UIComponents.kt                 # 通用UI组件集合
│   │   │   │   └── ErrorHandler.kt                 # 错误处理工具
│   │   │   ├── main/                    # 主界面相关
│   │   │   │   ├── MainActivity.kt                 # 主活动
│   │   │   │   ├── MainViewModel.kt                # 主界面视图模型
│   │   │   │   ├── SplashActivity.kt               # 启动页面实现
│   │   │   │   └── LoginActivity.kt                # 登录界面实现
│   │   │   ├── about/                   # 关于页面功能
│   │   │   │   ├── AboutActivity.kt                # 关于页面活动类
│   │   │   │   ├── UpdateChecker.kt                # 应用更新检查器
│   │   │   │   ├── FeedbackManager.kt              # 用户反馈管理器
│   │   │   │   ├── PrivacyPolicyActivity.kt        # 隐私政策展示页面
│   │   │   │   ├── TermsActivity.kt                # 使用条款展示页面
│   │   │   │   └── DeveloperInfoActivity.kt        # 开发者信息展示页面
│   │   │   ├── ui/                      # UI相关组件
│   │   │   │   └── theme/                           # 主题相关
│   │   │   │       ├── Color.kt                     # 颜色定义
│   │   │   │       ├── Theme.kt                     # 应用主题
│   │   │   │       └── Type.kt                      # 字体排版
│   │   │   ├── AboutActivity.kt         # 关于页面活动类
│   │   │   └── MainActivity.kt          # 主活动
│   │   └── res/                         # 资源文件目录
│   │       ├── anim/                    # 动画资源
│   │       │   └── btn_rotate.xml                   # 按钮旋转动画定义
│   │       ├── drawable/                # 图形资源
│   │       │   ├── ic_arrow_right.xml               # 右箭头图标
│   │       │   ├── ic_info.xml                      # 信息图标
│   │       │   ├── ic_electricity.xml               # 电费图标
│   │       │   ├── ic_schedule.xml                  # 课表图标
│   │       │   ├── ic_card.xml                      # 校园卡图标
│   │       │   ├── ic_library.xml                   # 图书馆图标
│   │       │   ├── ic_update.xml                    # 更新图标
│   │       │   ├── ic_feedback.xml                  # 反馈图标
│   │       │   ├── card_background.xml              # 卡片背景
│   │       │   └── widget_gradient_background.xml   # 小部件渐变背景
│   │       ├── layout/                  # 布局文件
│   │       │   ├── activity_main.xml                # 主页面布局
│   │       │   ├── activity_login.xml               # 登录页面布局
│   │       │   ├── activity_splash.xml              # 启动页面布局
│   │       │   ├── activity_about.xml               # 关于页面布局
│   │       │   ├── activity_privacy_policy.xml      # 隐私政策页面布局
│   │       │   ├── activity_terms.xml               # 使用条款页面布局
│   │       │   ├── activity_electricity.xml         # 电费页面布局
│   │       │   ├── activity_electricity_history.xml # 电费历史页面布局
│   │       │   ├── electricity_widget.xml           # 电费小部件布局
│   │       │   ├── activity_schedule.xml            # 课表页面布局
│   │       │   ├── schedule_widget.xml              # 课表小部件布局
│   │       │   ├── activity_campus_card.xml         # 校园卡页面布局
│   │       │   ├── item_transaction.xml             # 交易项布局
│   │       │   └── dialog_feedback.xml              # 反馈对话框布局
│   │       ├── values/                  # 值资源
│   │       │   ├── colors.xml                       # 颜色资源
│   │       │   ├── strings.xml                      # 字符串资源
│   │       │   ├── styles.xml                       # 样式资源
│   │       │   ├── dimens.xml                       # 尺寸资源
│   │       │   └── themes.xml                       # 主题资源
│   │       ├── xml/                     # XML配置
│   │       │   ├── electricity_widget_info.xml      # 电费小部件信息配置
│   │       │   ├── schedule_widget_info.xml         # 课表小部件信息配置
│   │       │   ├── network_security_config.xml      # 网络安全配置
│   │       │   ├── backup_rules.xml                 # 备份规则配置
│   │       │   └── data_extraction_rules.xml        # 数据提取规则
│   │       └── mipmap/                  # 应用图标
│   │           ├── ic_launcher.png                  # 应用图标（各分辨率）
│   │           └── ic_launcher_round.png            # 圆形应用图标
│   ├── androidTest/                     # Android测试目录
│   │   └── java/com/tthih/yu/test/      # 测试代码
│   │       ├── ElectricityTest.kt                   # 电费模块测试
│   │       ├── ScheduleTest.kt                      # 课表模块测试
│   │       └── CampusCardTest.kt                    # 校园卡模块测试
│   └── test/                            # 单元测试目录
│       └── java/com/tthih/yu/unittest/  # 单元测试代码
│           ├── RepositoryTest.kt                    # 数据仓库测试
│           ├── ViewModelTest.kt                     # 视图模型测试
│           └── UtilTest.kt                          # 工具类测试
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
- **ScheduleActivity.kt**: 课表查询主界面实现，显示周课表视图和日课表详情
- **ScheduleViewModel.kt**: 课表数据视图模型，处理课程数据加载和转换
- **ScheduleRepository.kt**: 课表数据仓库，负责从网络或本地获取课表数据
  - 实现教务系统API接口对接
  - 处理课表数据的解析和格式化
  - 管理本地缓存与同步
- **ScheduleDatabase.kt**: 课表本地数据库定义，使用Room实现
- **ScheduleData.kt**: 课表数据模型，包含课程名称、时间、地点、教师等信息
- **ScheduleDao.kt**: 课表数据访问对象，定义数据库操作
- **ScheduleWorker.kt**: 定时后台更新课表的工作类，确保课表数据及时更新
- **ScheduleWidget.kt**: 课表桌面小部件，显示当日课程
- **ScheduleNotification.kt**: 课程提醒通知服务，在课前发送提醒

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
- **LoginActivity.kt**: 登录界面实现，支持账号密码和指纹验证

#### 关于页面功能 (`about/`)
- **AboutActivity.kt**: 关于页面的主要活动类
  - 显示应用版本信息和开发者联系方式
  - 处理检查更新、隐私政策查看等功能
  - 实现分享应用和反馈建议渠道
- **UpdateChecker.kt**: 应用更新检查器，检测新版本并提示下载
- **FeedbackManager.kt**: 用户反馈管理器，收集和提交用户反馈
- **PrivacyPolicyActivity.kt**: 隐私政策展示页面
- **TermsActivity.kt**: 使用条款展示页面
- **DeveloperInfoActivity.kt**: 开发者信息展示页面

## 系统要求
- Android 8.0 (API级别26)或更高版本
- 网络连接(用于查询校园服务数据)
- 存储权限(用于保存本地数据)

## 权限说明
应用需要以下权限才能正常工作:
- 网络访问权限: 用于获取电费、课表和校园卡数据
- 存储权限: 用于保存本地数据和导出功能
- 通知权限: 用于推送电量不足等重要提醒
- 开机启动权限: 用于重启后恢复小部件更新服务

## 安装指南
1. 从应用商店下载安装包或直接下载APK文件
2. 允许安装来自此来源的应用(如果是APK安装)
3. 打开应用并完成初始设置
4. 根据提示授予必要权限
5. 长按桌面空白处，选择"小部件"，找到"宿舍电费"小部件并添加到桌面

## 使用方法
1. **电费查询**: 设置宿舍楼栋和房间号后即可查看电费情况
2. **桌面小部件**: 长按桌面空白处添加小部件，查看实时电费状态
3. **课表查询**: 首次使用需要登录教务系统，之后可自动同步
4. **校园卡管理**: 绑定校园卡账号后可查询余额和消费记录

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
### v1.1.0 (当前版本)
- **电费功能升级**:
  - 全新智能电费分析系统，支持多层次数据分析
  - 优化季节性调整算法，提高预测准确性
  - 新增趋势分析功能，直观显示用电变化
  - 增强异常情况处理，提供更可靠的预测
- 新增电费桌面小部件功能，实时显示电量余额和预计剩余天数
- 优化小部件自动更新机制，确保数据时效性
- 改进UI界面，提供更好的用户体验

### v1.0.0
- 实现宿舍电费查询和基础历史记录功能
- 新增课表查询功能
- 添加校园卡管理功能
- 优化UI界面和用户体验

### 未来计划
- **电费功能增强**:
  - 添加机器学习模型，提高用电预测准确性
  - 集成气温数据，优化季节性预测
  - 提供用电行为模式识别（周末vs工作日模式）
  - 增加更丰富的数据可视化图表，支持同比/环比分析
  - 智能充值提醒功能，基于预测准确推送通知
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
- **作用**：关于页面的主要活动类
- **功能**：
  - 显示应用版本信息
  - 处理各种选项的点击事件（检查更新、加入社群、邮件联系等）
  - 实现使用协议、隐私政策等链接的跳转
  - 提供导航返回功能

#### `app/src/main/java/com/tthih/yu/MainActivity.kt`
- **作用**：主界面中添加关于按钮
- **功能**：
  - 在主界面右下角添加一个带旋转动画效果的圆形关于按钮
  - 实现点击按钮时跳转到AboutActivity的逻辑
  - 使用Compose动画API实现按钮的360度旋转效果

### 2. XML 布局文件

#### `app/src/main/res/layout/activity_about.xml`
- **作用**：定义关于页面的UI布局
- **内容**：
  - 实现顶部工具栏
  - 显示应用图标和版本号
  - 创建多个卡片样式的选项（检查更新、社群、联系方式等）
  - 底部显示备案号信息

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
3. 跳转到关于页面，显示应用信息和选项列表
4. 用户可点击各选项查看详细信息或执行相应操作
5. 通过顶部返回按钮回到主页面

## 注意事项

- 所有文本内容使用strings.xml资源文件，方便后续多语言支持
- 图标使用矢量图形（Vector Drawable）以适应不同屏幕分辨率
- 关于页面中的外部链接（如隐私政策）需更新为实际可用的URL 