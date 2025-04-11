package com.tthih.yu.schedule

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * 课程表视图模型
 */
class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ScheduleRepository(application)
    
    // 当前选中的周次
    val selectedWeek = MutableLiveData<Int>()
    
    // 当前周次
    val currentWeek = MutableLiveData<Int>()
    
    // 总周数
    val totalWeeks = MutableLiveData<Int>()
    
    // 开学日期
    val startDate = MutableLiveData<Date>()
    
    // 选中的日期
    val selectedDate = MutableLiveData<Date>()
    
    // 当天的课程列表
    val dailySchedules = MutableLiveData<List<ScheduleData>>()
    
    // 周视图的课程列表（按星期几分组）
    val weeklySchedules = MutableLiveData<Map<Int, List<ScheduleData>>>()
    
    init {
        // 初始化周次信息
        loadWeekInfo()
        
        // 初始化当前选中的日期为今天
        selectedDate.value = Calendar.getInstance().time
        
        // 加载当天的课程
        loadTodaySchedules()
    }
    
    // 加载周次信息
    fun loadWeekInfo() {
        currentWeek.value = repository.getCurrentWeek()
        totalWeeks.value = repository.getTotalWeeks()
        startDate.value = repository.getStartDate()
        selectedWeek.value = currentWeek.value
    }
    
    // 加载当天的课程
    fun loadTodaySchedules() {
        viewModelScope.launch {
            // 获取今天是星期几
            val todayWeekDay = repository.getTodayWeekDay()
            // 获取当前周次
            val week = currentWeek.value ?: 1
            
            // 添加日志
            Log.d("ScheduleViewModel", "加载今日课程 - 周次: $week, 星期: $todayWeekDay")
            
            // 根据当前周次和今天的星期几加载课程
            val schedules = repository.getSchedulesByWeekAndDay(week, todayWeekDay)
            dailySchedules.postValue(schedules)
            
            // 记录加载的课程数
            Log.d("ScheduleViewModel", "今日课程加载完成 - 数量: ${schedules.size}")
        }
    }
    
    // 加载选中周次的所有课程（按星期几分组）
    fun loadWeekSchedules(week: Int) {
        viewModelScope.launch {
            val allSchedulesMap = mutableMapOf<Int, List<ScheduleData>>()
            
            // 对每一天加载课程
            for (day in 1..7) {
                val schedulesForDay = repository.getSchedulesByWeekAndDay(week, day)
                allSchedulesMap[day] = schedulesForDay
            }
            
            weeklySchedules.postValue(allSchedulesMap)
        }
    }
    
    // 切换到上一周
    fun previousWeek() {
        val current = selectedWeek.value ?: 1
        if (current > 1) {
            selectedWeek.value = current - 1
            loadWeekSchedules(current - 1)
        }
    }
    
    // 切换到下一周
    fun nextWeek() {
        val current = selectedWeek.value ?: 1
        val total = totalWeeks.value ?: 18
        if (current < total) {
            selectedWeek.value = current + 1
            loadWeekSchedules(current + 1)
        }
    }
    
    // 切换到当前周
    fun jumpToCurrentWeek() {
        val current = currentWeek.value ?: 1
        selectedWeek.value = current
        loadWeekSchedules(current)
    }
    
    // 加载特定日期的课程
    fun loadSchedulesByDate(date: Date) {
        selectedDate.value = date
        
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        // 获取这一天是星期几
        var weekDay = calendar.get(Calendar.DAY_OF_WEEK) - 1
        if (weekDay == 0) weekDay = 7
        
        // 计算这一天属于第几周
        val startCal = Calendar.getInstance()
        startCal.time = startDate.value ?: Date()
        
        val diffInMillis = date.time - startCal.time.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        val weekNumber = diffInDays / 7 + 1
        
        viewModelScope.launch {
            val schedules = repository.getSchedulesByWeekAndDay(weekNumber, weekDay)
            dailySchedules.postValue(schedules)
        }
    }
    
    // 设置开学日期
    fun setStartDate(date: Date) {
        repository.setStartDate(date)
        startDate.value = date
        
        // 重新计算当前周次
        currentWeek.value = repository.getCurrentWeek()
        selectedWeek.value = currentWeek.value
        
        // 重新加载课程
        loadWeekSchedules(currentWeek.value ?: 1)
    }
    
    // 手动设置当前周次
    fun setCurrentWeek(week: Int) {
        repository.setCurrentWeek(week)
        currentWeek.value = week
        selectedWeek.value = week
        
        // 重新加载课程
        loadWeekSchedules(week)
    }
    
    // 设置总周数
    fun setTotalWeeks(weeks: Int) {
        repository.setTotalWeeks(weeks)
        totalWeeks.value = weeks
    }
    
    // 添加课程
    fun addSchedule(schedule: ScheduleData) {
        viewModelScope.launch {
            repository.addSchedule(schedule)
            
            // 刷新课程数据
            val week = selectedWeek.value ?: 1
            loadWeekSchedules(week)
            
            // 如果添加的是今天的课程，同时刷新今天的课程列表
            if (schedule.weekDay == repository.getTodayWeekDay()) {
                loadTodaySchedules()
            }
        }
    }
    
    // 更新课程
    fun updateSchedule(schedule: ScheduleData) {
        viewModelScope.launch {
            repository.updateSchedule(schedule)
            
            // 刷新课程数据
            val week = selectedWeek.value ?: 1
            loadWeekSchedules(week)
            
            // 如果更新的是今天的课程，同时刷新今天的课程列表
            if (schedule.weekDay == repository.getTodayWeekDay()) {
                loadTodaySchedules()
            }
        }
    }
    
    // 删除课程
    fun deleteSchedule(schedule: ScheduleData) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
            
            // 刷新课程数据
            val week = selectedWeek.value ?: 1
            loadWeekSchedules(week)
            
            // 如果删除的是今天的课程，同时刷新今天的课程列表
            if (schedule.weekDay == repository.getTodayWeekDay()) {
                loadTodaySchedules()
            }
        }
    }
    
    // 清除所有课程数据
    fun clearAllSchedules(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllSchedules()
            
            // 刷新课程数据
            val week = selectedWeek.value ?: 1
            loadWeekSchedules(week)
            
            // 刷新今天的课程列表
            loadTodaySchedules()
            
            // 调用完成回调
            onComplete()
        }
    }
    
    // 获取课程节次时间配置
    fun getTimeNodes(): List<ScheduleTimeNode> {
        return repository.getTimeNodes()
    }
} 