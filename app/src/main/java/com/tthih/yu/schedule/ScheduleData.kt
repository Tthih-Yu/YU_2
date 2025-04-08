package com.tthih.yu.schedule

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 课程数据模型
 */
@Entity(tableName = "schedules")
data class ScheduleData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,                // 课程名称
    val classroom: String,           // 教室
    val teacher: String,             // 教师
    val weekDay: Int,                // 星期几 (1-7，1代表周一)
    val startNode: Int,              // 开始节次
    val endNode: Int,                // 结束节次
    val startWeek: Int,              // 开始周次
    val endWeek: Int,                // 结束周次
    val type: Int = 0                // 课程类型 (0-普通课程，1-实验课，2-考试)
)

/**
 * 课程节次时间配置
 */
data class ScheduleTimeNode(
    val node: Int,                   // 节次编号
    val startTime: String,           // 开始时间 "08:00"
    val endTime: String              // 结束时间 "08:45"
)

/**
 * 周次信息
 */
data class WeekInfo(
    val currentWeek: Int,            // 当前周次
    val totalWeeks: Int,             // 总周数
    val startDate: Date              // 开学日期
) 