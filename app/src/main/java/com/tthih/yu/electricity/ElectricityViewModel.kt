package com.tthih.yu.electricity

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class ElectricityViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ElectricityRepository(application)
    private val preferences: SharedPreferences = application.getSharedPreferences("electricity_prefs", Context.MODE_PRIVATE)
    
    // 电费数据
    private val _electricityData = MutableLiveData<ElectricityData>()
    val electricityData: LiveData<ElectricityData> = _electricityData
    
    // 历史数据
    private val _monthHistoryData = MutableLiveData<List<ElectricityHistoryData>>()
    val monthHistoryData: LiveData<List<ElectricityHistoryData>> = _monthHistoryData
    
    // 当前选中的年月
    private val _selectedYearMonth = MutableLiveData<Pair<Int, Int>>()
    val selectedYearMonth: LiveData<Pair<Int, Int>> = _selectedYearMonth
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误消息
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // JSESSIONID状态
    private val _isJsessionIdSet = MutableLiveData<Boolean>()
    val isJsessionIdSet: LiveData<Boolean> = _isJsessionIdSet
    
    // 用电趋势定义
    enum class UsageTrend {
        INCREASING,  // 用电量增加
        DECREASING,  // 用电量减少
        STABLE       // 用电量稳定
    }
    
    // 日均用电数据来源
    enum class DataSource {
        SHORT_TERM,       // 基于最近7天数据
        MEDIUM_TERM,      // 基于最近30天数据
        LONG_TERM,        // 基于长期数据
        RECHARGE_PATTERN, // 基于充值模式分析
        ESTIMATED         // 基于智能估算
    }
    
    // 当前使用的数据源
    private var currentDataSource: DataSource = DataSource.ESTIMATED
    
    // 获取日均用电数据的来源
    fun getDataSource(callback: (DataSource) -> Unit) {
        callback(currentDataSource)
    }
    
    init {
        // 初始化时检查JSESSIONID是否设置
        checkJsessionId()
        
        // 加载历史数据
        loadSavedData()
        
        // 初始化选中的年月为当前年月
        val calendar = Calendar.getInstance()
        _selectedYearMonth.value = Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        
        // 加载当月历史数据
        loadMonthHistory(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
    
    // 计算电量使用趋势分析
    fun getElectricityTrend(callback: (trend: UsageTrend?, percentage: Float) -> Unit) {
        viewModelScope.launch {
            try {
                // 获取最近14天的数据
                val recentData = withContext(Dispatchers.IO) {
                    repository.getHistoryData(14)
                }
                
                if (recentData.size < 4) {
                    // 数据不足，无法分析趋势
                    callback(null, 0f)
                    return@launch
                }
                
                // 将数据按日期分组
                val sortedData = recentData.sortedBy { 
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(it.timestamp)?.time ?: 0
                }
                
                // 将数据分为前半部分和后半部分
                val midPoint = sortedData.size / 2
                val olderHalf = sortedData.subList(0, midPoint)
                val newerHalf = sortedData.subList(midPoint, sortedData.size)
                
                // 计算两个时间段的平均日用电量
                val olderAvgUsage = calculateAverageDailyUsage(olderHalf)
                val newerAvgUsage = calculateAverageDailyUsage(newerHalf)
                
                // 计算变化百分比
                val percentageChange = if (olderAvgUsage > 0) {
                    ((newerAvgUsage - olderAvgUsage) / olderAvgUsage) * 100
                } else {
                    0f
                }
                
                // 确定趋势
                val trend = when {
                    percentageChange > 10 -> UsageTrend.INCREASING
                    percentageChange < -10 -> UsageTrend.DECREASING
                    else -> UsageTrend.STABLE
                }
                
                callback(trend, Math.abs(percentageChange))
                
            } catch (e: Exception) {
                Log.e("ElectricityVM", "计算电量趋势时出错: ${e.message}")
                callback(null, 0f)
            }
        }
    }
    
    // 计算时间段内的平均日用电量
    private fun calculateAverageDailyUsage(data: List<ElectricityData>): Float {
        if (data.size < 2) return 0f
        
        val first = data.first()
        val last = data.last()
        
        val firstDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(first.timestamp)
        val lastDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(last.timestamp)
        
        if (firstDate != null && lastDate != null) {
            val diffInMillis = lastDate.time - firstDate.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toFloat()
            
            if (diffInDays > 0) {
                // 计算期间的用电量（余额减少量）
                val usedAmount = first.balance - last.balance
                
                // 只考虑用电情况（余额减少）
                return if (usedAmount > 0) {
                    usedAmount / diffInDays
                } else {
                    0f
                }
            }
        }
        
        return 0f
    }
    
    // 完善电费数据更新和存储逻辑 (修改为 suspend 函数)
    private suspend fun updateElectricityData(result: ElectricityData): Boolean {
        return try {
            // 设置当前电费数据 (需要在主线程更新LiveData)
            withContext(Dispatchers.Main) {
                _electricityData.value = result
            }
            
            // 先检查变化，再保存新数据
            val changed = withContext(Dispatchers.IO) { 
                // 1. 获取旧的记录用于比较
                val previousRecord = repository.getLastElectricityData()

                // 2. 检查是否有变化
                val hasChanged = previousRecord == null || previousRecord.balance != result.balance

                // 3. 如果有变化，添加历史记录 (addElectricityHistory 内部会再次获取 previousRecord，但此时数据还没被覆盖)
                if (hasChanged) {
                    repository.addElectricityHistory(result) // 使用新数据添加历史
                }

                // 4. 保存最新的数据到 current data table
                repository.saveElectricityData(result)
                
                // 记录日志
                if (hasChanged) {
                    Log.d("ElectricityViewModel", "电费数据有变化，已更新历史记录")
                } else {
                    Log.d("ElectricityViewModel", "电费数据无变化")
                }
                hasChanged // 返回变化状态
            }
            
            // 刷新月度历史数据 (如果发生了变化)
            // refreshMonthHistoryData() 会更新 LiveData，确保在主线程或其内部处理好线程切换
            if (changed) {
                refreshMonthHistoryData()
            }

            // 记录电量使用趋势分析 (这部分逻辑可以保持异步，因为它不影响核心数据流)
             viewModelScope.launch(Dispatchers.IO) { // 保持这个分析的异步性
                try {
                    val currentMonth = getCurrentMonth()
                    val thisMonthData = repository.getMonthHistory(getCurrentYear(), currentMonth)
                    if (thisMonthData.isNotEmpty()) {
                        // 统计本月用电总量
                        val totalUsage = thisMonthData.sumOf { it.usage }
                        // 计算平均日用电量
                        val days = thisMonthData.map { it.getDayOfMonth() }.distinct().size
                        val avgDailyUsage = if (days > 0) totalUsage / days else 0.0
                        
                        Log.d("ElectricityViewModel", "本月至今用电: ${totalUsage}元，已记录${days}天，日均: ${avgDailyUsage}元/天")
                    }
                } catch (e: Exception) {
                    Log.e("ElectricityViewModel", "分析电量趋势异常: ${e.message}")
                }
            }
            
            true // 表示更新流程成功
        } catch (e: Exception) {
            Log.e("ElectricityViewModel", "更新电费数据异常: ${e.message}", e)
            // 更新 LiveData 需要在主线程
             withContext(Dispatchers.Main) {
                 _errorMessage.value = "更新电费数据异常: ${e.message}"
            }
            false // 表示更新流程失败
        }
    }

    fun refreshData() {
        if (!repository.isJsessionIdSet()) {
            _isJsessionIdSet.value = false
            _errorMessage.value = "请先设置JSESSIONID"
            return
        }
        
        _isJsessionIdSet.value = true
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getElectricityData()
                }
                
                if (result != null) {
                    // 直接调用 suspend 函数
                    val success = updateElectricityData(result)
                    if (!success) {
                        // _errorMessage 会在 updateElectricityData 内部设置
                        // _errorMessage.value = "更新电费数据失败" 
                    }
                } else {
                    _errorMessage.value = "获取数据失败"
                }
            } catch (e: Exception) {
                Log.e("ElectricityViewModel", "刷新数据失败: ${e.message}", e)
                _errorMessage.value = "刷新数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // 带回调的刷新数据方法
    fun refreshDataWithCallback(callback: ((Boolean) -> Unit)? = null) {
        if (!repository.isJsessionIdSet()) {
            _isJsessionIdSet.value = false
            _errorMessage.value = "请先设置JSESSIONID"
            callback?.invoke(false)
            return
        }
        
        _isJsessionIdSet.value = true
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getElectricityData()
                }
                
                if (result != null) {
                    // 调用 suspend 函数并等待其完成
                    val success = updateElectricityData(result) 
                    // 在 suspend 函数完成后调用回调
                    callback?.invoke(success) 
                } else {
                    _errorMessage.value = "获取数据失败"
                    callback?.invoke(false)
                }
            } catch (e: Exception) {
                Log.e("ElectricityViewModel", "刷新数据失败: ${e.message}", e)
                _errorMessage.value = "刷新数据失败: ${e.message}"
                callback?.invoke(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadSavedData() {
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    repository.getLastElectricityData()
                }
                
                if (data != null) {
                    _electricityData.value = data
                }
            } catch (e: Exception) {
                Log.e("ElectricityViewModel", "加载保存数据失败: ${e.message}")
            }
        }
    }
    
    fun loadMonthHistory(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                val historyData = withContext(Dispatchers.IO) {
                    repository.getMonthHistory(year, month)
                }
                _monthHistoryData.value = historyData
                
                // 更新选中的年月
                _selectedYearMonth.value = Pair(year, month)
            } catch (e: Exception) {
                Log.e("ElectricityViewModel", "加载月度历史数据失败: ${e.message}")
                _errorMessage.value = "加载历史数据失败"
            }
        }
    }
    
    // 获取当前年月
    private fun getCurrentYearMonth(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
    
    // 获取当前年份
    private fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }
    
    // 获取当前月份
    private fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1
    }
    
    // 刷新当月历史数据
    private fun refreshMonthHistoryData() {
        val yearMonth = _selectedYearMonth.value ?: getCurrentYearMonth()
        loadMonthHistory(yearMonth.first, yearMonth.second)
    }
    
    // 切换到下一个月
    fun goToNextMonth() {
        val currentYearMonth = _selectedYearMonth.value ?: getCurrentYearMonth()
        val calendar = Calendar.getInstance()
        calendar.set(currentYearMonth.first, currentYearMonth.second - 1, 1)
        calendar.add(Calendar.MONTH, 1)
        
        val nextYear = calendar.get(Calendar.YEAR)
        val nextMonth = calendar.get(Calendar.MONTH) + 1
        
        loadMonthHistory(nextYear, nextMonth)
    }
    
    // 切换到上一个月
    fun goToPreviousMonth() {
        val currentYearMonth = _selectedYearMonth.value ?: getCurrentYearMonth()
        val calendar = Calendar.getInstance()
        calendar.set(currentYearMonth.first, currentYearMonth.second - 1, 1)
        calendar.add(Calendar.MONTH, -1)
        
        val prevYear = calendar.get(Calendar.YEAR)
        val prevMonth = calendar.get(Calendar.MONTH) + 1
        
        loadMonthHistory(prevYear, prevMonth)
    }
    
    private fun checkJsessionId() {
        _isJsessionIdSet.value = repository.isJsessionIdSet()
    }
    
    fun getLowBalanceThreshold(): Float {
        return repository.getLowBalanceThreshold()
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = ""
    }
    
    fun updateJsessionId(jsessionId: String) {
        repository.updateJsessionId(jsessionId)
        _isJsessionIdSet.value = jsessionId.isNotEmpty()
    }
    
    fun updateBuilding(building: String) {
        repository.updateBuilding(building)
    }
    
    fun updateRoom(roomId: String) {
        repository.updateRoomId(roomId)
    }
    
    fun updateRefreshInterval(interval: Int) {
        repository.updateRefreshInterval(interval)
    }
    
    fun updateLowBalanceThreshold(threshold: Float) {
        repository.updateLowBalanceThreshold(threshold)
    }
    
    // 获取设置相关方法
    fun getJsessionId(): String {
        return repository.getJsessionId()
    }
    
    fun getCurrentBuilding(): String {
        return repository.getCurrentBuilding()
    }
    
    fun getRoomId(): String {
        return repository.getRoomId()
    }
    
    fun getRefreshInterval(): Int {
        return repository.getRefreshInterval()
    }
    
    // 清除历史数据
    fun clearHistoryData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.clearHistoryData()
                    repository.clearAllHistoryData()
                }
                
                // 重新加载当月数据（现在应该是空的）
                val yearMonth = _selectedYearMonth.value ?: getCurrentYearMonth()
                loadMonthHistory(yearMonth.first, yearMonth.second)
            } catch (e: Exception) {
                Log.e("ElectricityViewModel", "清除历史数据失败: ${e.message}")
                _errorMessage.value = "清除历史数据失败"
            }
        }
    }
    
    // 添加在Repository类中获取定时刷新设置的方法
    fun isScheduledRefreshEnabled(): Boolean {
        return repository.isScheduledRefreshEnabled()
    }
    
    // 添加在Repository类中更新定时刷新设置的方法
    fun updateScheduledRefreshEnabled(enabled: Boolean) {
        repository.updateScheduledRefreshEnabled(enabled)
    }
} 