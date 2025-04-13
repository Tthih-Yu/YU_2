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
    
    // 完善电费数据更新和存储逻辑
    private suspend fun updateElectricityData(result: ElectricityData): Boolean {
        try {
            // 获取上一次保存的数据
            val lastData = withContext(Dispatchers.IO) {
                repository.getLastElectricityData()
            }
            
            _electricityData.value = result
            
            // 保存电费数据
            repository.saveElectricityData(result)
            
            // 获取当前日期和时间
            val currentDate = Date()
            
            // 创建新的历史记录
            // 如果有上一次查询数据，比较余额变化
            var recharge = 0.0
            var usage = 0.0
            
            if (lastData != null) {
                val balanceChange = result.balance - lastData.balance
                
                if (balanceChange > 0) {
                    // 余额增加，记录为充值
                    recharge = balanceChange.toDouble()
                    usage = 0.0
                    Log.d("ElectricityViewModel", "检测到充值: +$recharge 元")
                } else if (balanceChange < 0) {
                    // 余额减少，记录为用电
                    usage = abs(balanceChange.toDouble())
                    recharge = 0.0
                    Log.d("ElectricityViewModel", "检测到用电: -$usage 元")
                } else {
                    // 余额无变化
                    recharge = 0.0
                    usage = 0.0
                    Log.d("ElectricityViewModel", "余额无变化")
                }
            } else {
                // 首次查询，不记录变化
                recharge = 0.0
                usage = 0.0
                Log.d("ElectricityViewModel", "首次查询，不记录变化")
            }
            
            // 创建历史记录
            val historyData = ElectricityHistoryData(
                id = 0,  // 自动生成ID
                date = currentDate,
                balance = result.balance.toDouble(),
                building = result.building,
                roomId = result.roomId,
                usage = usage,
                recharge = recharge
            )
            
            // 保存到数据库
            withContext(Dispatchers.IO) {
                repository.saveElectricityHistoryData(historyData)
            }
            
            // 记录详细日志
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(currentDate)
            Log.d("ElectricityViewModel", "添加电费历史记录: 日期=$dateStr, 余额=${result.balance}, 充值=$recharge, 用电=$usage")
            
            // 刷新当月历史数据
            val yearMonth = _selectedYearMonth.value ?: getCurrentYearMonth()
            loadMonthHistory(yearMonth.first, yearMonth.second)
            
            // 添加日志记录电量使用趋势
            getElectricityTrend { trend, percentage ->
                if (trend != null) {
                    val trendStr = when (trend) {
                        UsageTrend.INCREASING -> "上升"
                        UsageTrend.DECREASING -> "下降"
                        UsageTrend.STABLE -> "稳定"
                    }
                    Log.d("ElectricityVM", "电量使用趋势: $trendStr, 变化百分比: $percentage%")
                }
            }
            
            return true
        } catch (e: Exception) {
            Log.e("ElectricityVM", "更新电费数据失败: ${e.message}")
            return false
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
                    val success = updateElectricityData(result)
                    if (!success) {
                        _errorMessage.value = "更新电费数据失败"
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
                    val success = updateElectricityData(result)
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