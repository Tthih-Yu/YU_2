package com.tthih.yu.schedule

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 课程表数据仓库
 */
class ScheduleRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    
    private val database: ScheduleDatabase by lazy {
        ScheduleDatabase.getDatabase(context)
    }
    
    // 获取所有课程
    suspend fun getAllSchedules(): List<ScheduleData> {
        return database.scheduleDao().getAllSchedules()
    }
    
    // 获取指定周次和星期的课程
    suspend fun getSchedulesByWeekAndDay(week: Int, weekDay: Int): List<ScheduleData> {
        return database.scheduleDao().getSchedulesByWeekAndDay(week, weekDay)
    }
    
    // 获取指定星期的所有课程（不考虑周次）
    suspend fun getSchedulesByWeekDay(weekDay: Int): List<ScheduleData> {
        return database.scheduleDao().getSchedulesByWeekDay(weekDay)
    }
    
    // 添加新课程
    suspend fun addSchedule(schedule: ScheduleData): Long {
        return database.scheduleDao().insertSchedule(schedule)
    }
    
    // 更新课程
    suspend fun updateSchedule(schedule: ScheduleData) {
        database.scheduleDao().updateSchedule(schedule)
    }
    
    // 删除课程
    suspend fun deleteSchedule(schedule: ScheduleData) {
        database.scheduleDao().deleteSchedule(schedule)
    }
    
    // 删除课程通过ID
    suspend fun deleteScheduleById(id: Int) {
        database.scheduleDao().deleteScheduleById(id)
    }
    
    // 清除所有课程
    suspend fun clearAllSchedules() {
        database.scheduleDao().deleteAllSchedules()
    }
    
    // 获取课程节次时间配置
    fun getTimeNodes(): List<ScheduleTimeNode> {
        return listOf(
            ScheduleTimeNode(1, "08:00", "08:45"),
            ScheduleTimeNode(2, "08:50", "09:35"),
            ScheduleTimeNode(3, "09:55", "10:40"),
            ScheduleTimeNode(4, "10:45", "11:30"),
            ScheduleTimeNode(5, "11:35", "12:20"),
            ScheduleTimeNode(6, "14:30", "15:15"),
            ScheduleTimeNode(7, "15:20", "16:05"),
            ScheduleTimeNode(8, "16:15", "17:00"),
            ScheduleTimeNode(9, "17:05", "17:50"),
            ScheduleTimeNode(10, "19:00", "19:45"),
            ScheduleTimeNode(11, "19:50", "20:35")
        )
    }
    
    // 获取开学日期
    fun getStartDate(): Date {
        val dateString = sharedPreferences.getString(KEY_START_DATE, "2024-02-26") ?: "2024-02-26"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.parse(dateString) ?: Date()
    }
    
    // 设置开学日期
    fun setStartDate(date: Date) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sharedPreferences.edit().putString(KEY_START_DATE, dateFormat.format(date)).apply()
    }
    
    // 获取当前周次
    fun getCurrentWeek(): Int {
        val startDate = getStartDate()
        val currentDate = Date()
        
        // 计算周数差
        val diffInMillis = currentDate.time - startDate.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        val diffInWeeks = diffInDays / 7 + 1
        
        return sharedPreferences.getInt(KEY_CURRENT_WEEK, diffInWeeks)
    }
    
    // 手动设置当前周次
    fun setCurrentWeek(week: Int) {
        sharedPreferences.edit().putInt(KEY_CURRENT_WEEK, week).apply()
    }
    
    // 获取总周数
    fun getTotalWeeks(): Int {
        return sharedPreferences.getInt(KEY_TOTAL_WEEKS, 18)
    }
    
    // 设置总周数
    fun setTotalWeeks(totalWeeks: Int) {
        sharedPreferences.edit().putInt(KEY_TOTAL_WEEKS, totalWeeks).apply()
    }
    
    // 获取今天是星期几（1-7，周一到周日）
    fun getTodayWeekDay(): Int {
        val calendar = Calendar.getInstance()
        // Calendar中周日是1，周一是2，所以需要转换
        var weekDay = calendar.get(Calendar.DAY_OF_WEEK) - 1
        if (weekDay == 0) weekDay = 7
        return weekDay
    }
    
    companion object {
        private const val PREF_NAME = "schedule_prefs"
        private const val KEY_START_DATE = "start_date"
        private const val KEY_CURRENT_WEEK = "current_week"
        private const val KEY_TOTAL_WEEKS = "total_weeks"
    }
} 