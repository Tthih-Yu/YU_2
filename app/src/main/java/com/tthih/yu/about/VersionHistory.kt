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