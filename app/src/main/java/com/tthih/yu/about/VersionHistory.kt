package com.tthih.yu.about

/**
 * 表示一个版本的历史信息
 */
data class VersionHistoryItem(
    val versionName: String,       // 版本名称，如 "1.1.0"
    val versionCode: Int,          // 版本代码，如 2
    val releaseDate: String,       // 发布日期，如 "2023-12-28"
    val changes: List<String>      // 版本变更列表
)

/**
 * 版本历史管理类，提供应用所有历史版本信息
 */
object VersionHistory {
    // 应用的版本历史记录，最新版本放在最前面
    val history = listOf(
        VersionHistoryItem(
            versionName = "1.1.6",
            versionCode = 4,
            releaseDate = "2025-04-27",
            changes = listOf(
                "图表分析：为星期分布、时段分布、消费区间、图表添加滚动功能。",
                "图表分析：优化：余额趋势 和 每日消费 图表的X轴标签，显示实际日期并提高可读性。",
                "图表分析：移除了图表选择区域多余的文字标签。",
                "登录流程：完善登陆步骤。",
                "修复：解决了图表X轴标签在部分情况下显示不全或截断的问题。"
            )
        ),
        VersionHistoryItem(
            versionName = "1.1.5",
            versionCode = 3,
            releaseDate = "2025-04-26",
            changes = listOf(
                "优化了课程表功能，优化了导入逻辑",
                "新增了电费数据导入/出功能，支持从json文件导入数据",
                "大幅度优化APP安装包体量",
                "强烈建议使用新版本"
            )
        ),
        VersionHistoryItem(
            versionName = "1.1.0",
            versionCode = 2,
            releaseDate = "2025-4-22",
            changes = listOf(
                "新增今日课表桌面小部件，支持4x2和4x4两种尺寸",
                "小部件支持深色模式自适应",
                "优化了课表空状态显示效果",
                "添加了周视图页面可以手动添加课程信息",
                "周视图也买那新增支持手动滑动",
                "新增开课时间选项",
                "修复了一些已知问题和闪退"
            )
        ),
        VersionHistoryItem(
            versionName = "1.0",
            versionCode = 1,
            releaseDate = "2025-4-8",
            changes = listOf(
                "YU校园助手首次发布",
                "支持课表查询与管理",
                "支持宿舍电量查询",
                "支持电费查询与充值",
                "支持待办事项管理"
            )
        )
    )
} 