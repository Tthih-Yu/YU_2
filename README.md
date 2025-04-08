# 安工大助手(YU)

## 项目概述
安工大助手(YU)是一款为安徽工程大学学生开发的Android应用，旨在提供校园生活便利工具。目前已实现宿舍电费监控、课表查询和校园卡管理功能，为学生提供一站式校园生活服务。

## 主要功能

### 宿舍电费监控
- **实时查询**: 查询宿舍剩余电量
- **数据分析**: 统计日均用电量，预估剩余可用天数
- **历史记录**: 记录并展示电费使用历史，支持日历视图
- **自定义设置**: 可设置宿舍楼栋、房间号和刷新间隔
- **数据可视化**: 直观展示用电趋势和消费情况
- **桌面小部件**: 支持在桌面添加小组件，实时显示电量余额和预计剩余天数

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
│   │   │   │   ├── ElectricityWidgetProvider.kt    # 电费桌面小部件提供者
│   │   │   │   └── ElectricityWidgetWorker.kt      # 电费小部件更新工作类
│   │   │   ├── schedule/                # 课表相关功能模块
│   │   │   │   ├── ScheduleActivity.kt             # 课表主界面
│   │   │   │   └── ScheduleViewModel.kt            # 课表视图模型
│   │   │   ├── campuscard/              # 校园卡相关功能模块
│   │   │   │   ├── CampusCardActivity.kt           # 校园卡主界面 
│   │   │   │   └── CampusCardViewModel.kt          # 校园卡视图模型
│   │   │   ├── component/               # 共用组件
│   │   │   │   └── Components.kt                    # 通用UI组件
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
│   │       │   └── widget_gradient_background.xml   # 小部件渐变背景
│   │       ├── layout/                  # 布局文件
│   │       │   ├── activity_about.xml               # 关于页面布局
│   │       │   ├── activity_electricity.xml         # 电费页面布局
│   │       │   ├── electricity_widget.xml           # 电费小部件布局
│   │       │   └── activity_schedule.xml            # 课表页面布局
│   │       ├── values/                  # 值资源
│   │       │   ├── colors.xml                       # 颜色资源
│   │       │   ├── strings.xml                      # 字符串资源
│   │       │   ├── styles.xml                       # 样式资源
│   │       │   └── themes.xml                       # 主题资源
│   │       ├── xml/                     # XML配置
│   │       │   ├── electricity_widget_info.xml      # 电费小部件信息配置
│   │       │   ├── network_security_config.xml      # 网络安全配置
│   │       │   ├── backup_rules.xml                 # 备份规则配置
│   │       │   └── data_extraction_rules.xml        # 数据提取规则
│   │       └── mipmap/                  # 应用图标
│   │           ├── ic_launcher.png                  # 应用图标（各分辨率）
│   │           └── ic_launcher_round.png            # 圆形应用图标
│   ├── androidTest/                     # Android测试目录
│   │   └── java/                        # 测试代码
│   └── test/                            # 单元测试目录
│       └── java/                        # 测试代码
├── build.gradle.kts                     # 应用级Gradle构建脚本
└── proguard-rules.pro                   # ProGuard规则配置
```

### 模块详解

#### 电费模块 (`electricity/`)
- **ElectricityActivity.kt**: 电费查询主界面实现
- **ElectricityHistoryActivity.kt**: 电费历史记录界面实现
- **ElectricityViewModel.kt**: 电费数据视图模型，处理业务逻辑
- **ElectricityRepository.kt**: 电费数据仓库，处理数据获取和存储
- **ElectricityDatabase.kt**: 电费本地数据库定义
- **ElectricityData.kt**: 电费数据模型
- **ElectricityHistoryData.kt**: 电费历史记录数据模型
- **ElectricityDao.kt/ElectricityHistoryDao.kt**: 数据访问对象，定义数据库操作
- **BuildingSelectorDialog.kt**: 宿舍楼栋选择对话框
- **SettingsActivity.kt**: 电费设置界面
- **CalendarDayAdapter.kt**: 历史记录日历视图适配器
- **DateConverter.kt**: 日期转换工具类
- **ElectricityWidgetProvider.kt**: 电费桌面小部件提供者
- **ElectricityWidgetWorker.kt**: 电费小部件后台更新工作类

#### 课表模块 (`schedule/`)
- **ScheduleActivity.kt**: 课表查询主界面实现
- **ScheduleViewModel.kt**: 课表数据视图模型
- **ScheduleRepository.kt**: 课表数据仓库
- **ScheduleDatabase.kt**: 课表本地数据库定义
- **ScheduleData.kt**: 课表数据模型
- **ScheduleDao.kt**: 课表数据访问对象

#### 校园卡模块 (`campuscard/`)
- **CampusCardActivity.kt**: 校园卡管理主界面实现
- **CampusCardViewModel.kt**: 校园卡数据视图模型
- **CampusCardComponents.kt**: 校园卡UI组件集合
- **CampusCardRepository.kt**: 校园卡数据仓库
- **CampusCardDatabase.kt**: 校园卡本地数据库定义
- **CampusCardTransaction.kt**: 校园卡交易数据模型
- **CampusCardDao.kt**: 校园卡数据访问对象

#### 通用组件 (`component/`)
- **timer.js**: 定时器实现
- **provider.js**: 数据提供者实现
- **parser.js**: 数据解析工具

#### 其他关键文件
- **MainActivity.kt**: 应用程序主入口，负责导航和管理主页面
- **源码目录**: 包含学校各系统接口的请求和响应样本，用于开发和测试

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
- 新增电费桌面小部件功能，实时显示电量余额和预计剩余天数
- 优化小部件自动更新机制，确保数据时效性
- 改进UI界面，提供更好的用户体验

### v1.0.0
- 实现宿舍电费查询和历史记录功能
- 新增课表查询功能
- 添加校园卡管理功能
- 优化UI界面和用户体验

### 未来计划
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