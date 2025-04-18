package com.tthih.yu.about

data class UpdateInfo(
    val versionName: String, // 版本名称，例如 "1.2.0"
    val versionCode: Int, // 版本代码，例如 120
    val forceUpdate: Boolean, // 是否强制更新
    val updateContent: String, // 更新内容
    val downloadUrl: String, // 下载地址
    val publishDate: String // 发布日期
) 