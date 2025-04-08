package com.tthih.yu.electricity

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ElectricityRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    
    private val database: ElectricityDatabase by lazy {
        ElectricityDatabase.getDatabase(context)
    }
    
    // 获取电费数据
    suspend fun getElectricityData(): ElectricityData? {
        return try {
            val jsessionId = getJsessionId()
            if (jsessionId.isEmpty()) {
                Log.e("ElectricityRepo", "JSESSIONID为空，无法请求数据")
                return null
            }
            
            withContext(Dispatchers.IO) {
                try {
                    Log.d("ElectricityRepo", "开始获取电费数据...")
                    Log.d("ElectricityRepo", "API地址: $BASE_URL$API_ENDPOINT")
                    Log.d("ElectricityRepo", "宿舍: ${getCurrentBuilding()} - ${getRoomId()}")
                    
                    // 构建请求
                    val url = URL("$BASE_URL$API_ENDPOINT")
                    val connection = url.openConnection() as HttpURLConnection
                    
                    // 增加连接超时设置
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    
                    connection.apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01")
                        setRequestProperty("User-Agent", USER_AGENT)
                        setRequestProperty("X-Requested-With", "XMLHttpRequest")
                        setRequestProperty("Origin", BASE_URL)
                        setRequestProperty("Referer", "$BASE_URL/web/common/checkEle.html")
                        setRequestProperty("Cookie", "JSESSIONID=$jsessionId")
                        setRequestProperty("Connection", "keep-alive")
                    }
                    
                    // 准备请求参数
                    val buildingId = getBuildingId(getCurrentBuilding())
                    val roomId = getRoomId()
                    
                    Log.d("ElectricityRepo", "使用BuildingID: $buildingId, RoomID: $roomId")
                    
                    // 构建请求数据
                    val queryData = JSONObject().apply {
                        put("query_elec_roominfo", JSONObject().apply {
                            put("aid", "0030000000002501")
                            put("account", "52885")
                            put("room", JSONObject().apply {
                                put("roomid", roomId)
                                put("room", roomId)
                            })
                            put("floor", JSONObject().apply {
                                put("floorid", "")
                                put("floor", "")
                            })
                            put("area", JSONObject().apply {
                                put("area", AREA_NAME)
                                put("areaname", AREA_NAME)
                            })
                            put("building", JSONObject().apply {
                                put("buildingid", buildingId)
                                put("building", getCurrentBuilding())
                            })
                        })
                    }
                    
                    // 准备表单数据
                    val formData = "jsondata=${URLEncoder.encode(queryData.toString(), "UTF-8")}" +
                            "&funname=synjones.onecard.query.elec.roominfo" +
                            "&json=true"
                    
                    Log.d("ElectricityRepo", "请求表单数据: $formData")
                    
                    try {
                        // 发送请求
                        val outputStream = connection.outputStream
                        val writer = OutputStreamWriter(outputStream)
                        writer.write(formData)
                        writer.flush()
                        writer.close()
                        
                        // 获取响应
                        val responseCode = connection.responseCode
                        Log.d("ElectricityRepo", "HTTP响应码: $responseCode")
                        
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            Log.d("ElectricityRepo", "获取到原始响应: ${response.take(200)}...")
                            parseResponse(response)
                        } else {
                            val errorStream = connection.errorStream
                            val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "无错误详情"
                            Log.e("ElectricityRepo", "HTTP错误: $responseCode, 错误详情: $errorResponse")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ElectricityRepo", "请求执行异常: ${e.message}", e)
                        null
                    } finally {
                        connection.disconnect()
                    }
                } catch (e: Exception) {
                    Log.e("ElectricityRepo", "发送请求异常: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "获取数据异常: ${e.message}", e)
            null
        }
    }
    
    // 解析响应数据
    private suspend fun parseResponse(response: String): ElectricityData? {
        return try {
            val jsonResponse = JSONObject(response)
            if (jsonResponse.has("query_elec_roominfo")) {
                val roomInfo = jsonResponse.getJSONObject("query_elec_roominfo")
                if (roomInfo.getString("retcode") == "0") {
                    // 从错误消息中提取余额
                    val errorMsg = roomInfo.getString("errmsg")
                    val balance = extractBalance(errorMsg)
                    
                    // 创建当前时间
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                    val currentTime = Date()
                    val timestamp = dateFormat.format(currentTime)
                    
                    // 计算下次刷新时间
                    val calendar = Calendar.getInstance()
                    calendar.time = currentTime
                    calendar.add(Calendar.MINUTE, getRefreshInterval())
                    val nextRefresh = dateFormat.format(calendar.time)
                    
                    // 计算日均用电量和预计可用天数
                    var dailyUsage = 0f
                    var estimatedDays = 0
                    
                    try {
                        dailyUsage = calculateDailyUsage()
                        // 确保得到有效的日均用电量
                        if (dailyUsage <= 0f) {
                            dailyUsage = 2.5f // 默认值，约75元/月
                            Log.d("ElectricityRepo", "使用默认日均用电量: ${dailyUsage}元/天")
                        }
                    } catch (e: Exception) {
                        Log.e("ElectricityRepo", "计算日均用电量异常: ${e.message}")
                        dailyUsage = 2.5f // 出错时使用默认值
                    }
                    
                    try {
                        estimatedDays = calculateEstimatedDays(balance, dailyUsage)
                        // 确保估计天数合理
                        if (estimatedDays <= 0 && balance > 0) {
                            estimatedDays = (balance / 2.5f).toInt() // 使用默认日均用电量计算
                            Log.d("ElectricityRepo", "使用默认方法计算估计天数: ${estimatedDays}天")
                        }
                    } catch (e: Exception) {
                        Log.e("ElectricityRepo", "计算估计天数异常: ${e.message}")
                        if (balance > 0) {
                            estimatedDays = (balance / 2.5f).toInt() // 出错时使用默认计算
                        }
                    }
                    
                    Log.d("ElectricityRepo", "解析结果: 余额=${balance}元, 日均用电=${dailyUsage}元/天, 估计可用=${estimatedDays}天")
                    
                    ElectricityData(
                        balance = balance,
                        timestamp = timestamp,
                        roomId = getRoomId(),
                        building = getCurrentBuilding(),
                        nextRefresh = nextRefresh,
                        refreshInterval = getRefreshInterval(),
                        dailyUsage = dailyUsage,
                        estimatedDays = estimatedDays
                    )
                } else {
                    Log.e("ElectricityRepo", "查询失败: ${roomInfo.optString("errmsg", "未知错误")}")
                    null
                }
            } else {
                Log.e("ElectricityRepo", "无效响应格式")
                null
            }
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "解析响应异常: ${e.message}")
            null
        }
    }
    
    // 从错误消息中提取余额
    private fun extractBalance(errorMsg: String): Float {
        try {
            val pattern = "剩余电量(\\d+\\.\\d+)".toRegex()
            val matchResult = pattern.find(errorMsg)
            
            val balanceStr = matchResult?.groupValues?.get(1)
            if (balanceStr != null) {
                Log.d("ElectricityRepo", "成功从消息中提取余额: $balanceStr 元")
                return balanceStr.toFloatOrNull() ?: 0f
            } else {
                // 尝试其他可能的格式
                val alternativePattern = "(\\d+\\.\\d+)度".toRegex()
                val altMatch = alternativePattern.find(errorMsg)
                if (altMatch != null) {
                    val altBalanceStr = altMatch.groupValues[1]
                    Log.d("ElectricityRepo", "使用备用模式提取余额: $altBalanceStr 元")
                    return altBalanceStr.toFloatOrNull() ?: 0f
                }
                
                // 如果还是没找到，记录原始消息并返回0
                Log.w("ElectricityRepo", "无法从消息中提取余额: $errorMsg")
                return 0f
            }
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "提取余额时出错: ${e.message}, 原始消息: $errorMsg", e)
            return 0f
        }
    }
    
    // 计算日均用电量
    private suspend fun calculateDailyUsage(): Float {
        try {
            // 使用最近14天的数据计算平均用电量，提高数据样本量
            val recentData = withContext(Dispatchers.IO) {
                database.electricityDao().getRecentData(14)
            }
            
            if (recentData.size < 2) {
                Log.d("ElectricityRepo", "计算日均用电: 历史数据不足，至少需要2条记录")
                return 0f
            }
            
            // 过滤掉充值记录的干扰，只考虑电费减少的部分
            val dataPoints = mutableListOf<Pair<Date, Float>>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            
            for (data in recentData) {
                try {
                    val date = dateFormat.parse(data.timestamp)
                    if (date != null) {
                        dataPoints.add(Pair(date, data.balance))
                    } else {
                        Log.e("ElectricityRepo", "日期解析失败: ${data.timestamp}")
                    }
                } catch (e: Exception) {
                    Log.e("ElectricityRepo", "处理日期时出错: ${e.message}")
                }
            }
            
            if (dataPoints.size < 2) {
                Log.d("ElectricityRepo", "计算日均用电: 有效数据点不足，至少需要2个有效数据点")
                return 0f
            }
            
            // 按时间排序
            dataPoints.sortBy { it.first.time }
            
            // 计算每天的用电量并累计
            var totalUsage = 0f
            var validDays = 0f
            
            for (i in 0 until dataPoints.size - 1) {
                val curr = dataPoints[i]
                val next = dataPoints[i + 1]
                
                // 计算时间差（天）
                val diffInMillis = next.first.time - curr.first.time
                val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toFloat()
                
                // 只计算余额减少的情况（用电）
                val balanceDiff = curr.second - next.second
                
                // 有效天数至少为0.5天，避免时间间隔过短导致计算偏差
                if (diffInDays >= 0.5 && balanceDiff > 0) {
                    val dailyRate = balanceDiff / diffInDays
                    totalUsage += balanceDiff
                    validDays += diffInDays
                    Log.d("ElectricityRepo", "计算日均用电: 从 ${SimpleDateFormat("MM-dd", Locale.CHINA).format(curr.first)} 至 ${SimpleDateFormat("MM-dd", Locale.CHINA).format(next.first)}, ${diffInDays}天消耗${balanceDiff}元, 日均${dailyRate}元")
                }
            }
            
            // 计算平均每天消耗电费
            val avgDailyUsage = if (validDays > 0) totalUsage / validDays else 0f
            
            // 如果计算结果为0，使用一个默认的估计值
            if (avgDailyUsage <= 0f) {
                Log.d("ElectricityRepo", "计算日均用电: 没有有效的用电记录，使用默认估计值")
                // 根据余额估算一个合理的日均用电量
                val latestBalance = dataPoints.lastOrNull()?.second ?: 0f
                return if (latestBalance > 0) {
                    // 估计一个月的电费约为50-100元
                    val defaultMonthlyUsage = 75f
                    defaultMonthlyUsage / 30f  // 每天大约2.5元
                } else {
                    0f
                }
            }
            
            // 根据时间段调整用电量预测（冬夏季用电高于春秋季）
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            
            // 季节调整系数：夏季(6-9月)和冬季(12-2月)用电量较高
            val seasonalFactor = when (month) {
                in 6..9 -> 1.2f  // 夏季
                in 12..12, in 1..2 -> 1.15f  // 冬季
                else -> 1.0f  // 春秋季
            }
            
            val adjustedDailyUsage = avgDailyUsage * seasonalFactor
            Log.d("ElectricityRepo", "日均用电计算: 原始=${avgDailyUsage}元/天, 季节调整后=${adjustedDailyUsage}元/天")
            
            return adjustedDailyUsage
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "计算日均用电异常: ${e.message}", e)
            // 返回一个默认估计值，避免返回0导致UI显示"未知"
            return 2.5f  // 每天大约2.5元
        }
    }
    
    // 计算剩余天数（按照当前用电量计算）
    private fun calculateEstimatedDays(balance: Float, dailyUsage: Float): Int {
        try {
            // 如果没有余额，直接返回0
            if (balance <= 0) {
                Log.d("ElectricityRepo", "计算剩余天数: 余额为0或负数，返回0天")
                return 0
            }
            
            // 如果日均用电量太小或为零，提供一个合理的估计值
            if (dailyUsage < 0.5f) {
                // 基于平均月用电75元来估算
                val estimatedDailyUsage = 75f / 30f  // 约2.5元/天
                val estimatedDays = (balance / estimatedDailyUsage).toInt()
                Log.d("ElectricityRepo", "计算剩余天数: 日均用电量过低(${dailyUsage}), 使用默认估计值(${estimatedDailyUsage}元/天), 估计可用${estimatedDays}天")
                return estimatedDays
            }
            
            val days = (balance / dailyUsage).toInt()
            Log.d("ElectricityRepo", "计算剩余天数: 余额${balance}元, 日均用电${dailyUsage}元/天, 估计可用${days}天")
            return days
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "计算剩余天数异常: ${e.message}", e)
            // 出现异常时，提供一个基于余额的粗略估计
            if (balance > 0) {
                // 假设每天使用2.5元
                val defaultEstimate = (balance / 2.5f).toInt()
                return maxOf(defaultEstimate, 1)  // 至少返回1天
            }
            return 0
        }
    }
    
    // 保存电费数据
    suspend fun saveElectricityData(data: ElectricityData) {
        withContext(Dispatchers.IO) {
            database.electricityDao().insert(data)
        }
    }
    
    // 获取最新的电费数据
    suspend fun getLastElectricityData(): ElectricityData? {
        return withContext(Dispatchers.IO) {
            database.electricityDao().getLastRecord()
        }
    }
    
    // 获取历史电费数据
    suspend fun getHistoryData(limit: Int): List<ElectricityData> {
        return withContext(Dispatchers.IO) {
            database.electricityDao().getRecentData(limit)
        }
    }
    
    // 清除历史数据
    suspend fun clearHistoryData() {
        withContext(Dispatchers.IO) {
            database.electricityDao().deleteAll()
        }
    }
    
    // 添加电费历史记录
    suspend fun addElectricityHistory(data: ElectricityData) {
        withContext(Dispatchers.IO) {
            try {
                // 确保数据有效性
                val validDailyUsage = if (data.dailyUsage > 0f) data.dailyUsage else calculateDailyUsage()
                val validBalance = if (data.balance >= 0f) data.balance else 0f
                
                val historyData = ElectricityHistoryData(
                    date = Date(System.currentTimeMillis()),
                    balance = validBalance.toDouble(),
                    building = data.building,
                    roomId = data.roomId,
                    usage = validDailyUsage.toDouble(),
                    // 保存当前估计剩余天数信息
                    recharge = data.estimatedDays.toDouble() 
                )
                
                Log.d("ElectricityRepo", "添加历史记录: 余额=${validBalance}元, 日均用电=${validDailyUsage}元/天, 估计剩余天数=${data.estimatedDays}天")
                
                val historyDao = ElectricityDatabase.getDatabase(context).electricityHistoryDao()
                historyDao.insertHistory(historyData)
            } catch (e: Exception) {
                Log.e("ElectricityRepo", "添加电费历史记录失败: ${e.message}", e)
            }
        }
    }
    
    // 保存电费历史记录数据
    suspend fun saveElectricityHistoryData(data: ElectricityHistoryData) {
        withContext(Dispatchers.IO) {
            try {
                val historyDao = ElectricityDatabase.getDatabase(context).electricityHistoryDao()
                historyDao.insertHistory(data)
                Log.d("ElectricityRepo", "保存电费历史记录成功: ${data.balance}元, 用电: ${data.usage}, 充值: ${data.recharge}")
            } catch (e: Exception) {
                Log.e("ElectricityRepo", "保存电费历史记录失败: ${e.message}", e)
            }
        }
    }
    
    // 获取当月电费历史记录
    suspend fun getMonthHistory(year: Int, month: Int): List<ElectricityHistoryData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ElectricityRepo", "获取 $year 年 $month 月的历史数据")
                val calendar = Calendar.getInstance()
                
                // 设置日历为当月第一天
                calendar.set(year, month - 1, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = Date(calendar.timeInMillis)
                
                // 设置日历为下个月第一天
                calendar.add(Calendar.MONTH, 1)
                val endTime = Date(calendar.timeInMillis - 1) // 减去1毫秒，即当月最后一毫秒
                
                Log.d("ElectricityRepo", "查询时间范围：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(startTime)} 至 ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(endTime)}")
                Log.d("ElectricityRepo", "当前宿舍楼: ${getCurrentBuilding()}, 房间: ${getRoomId()}")
                
                val historyDao = ElectricityDatabase.getDatabase(context).electricityHistoryDao()
                val result = historyDao.getHistoryByDateRange(
                    getCurrentBuilding(),
                    getRoomId(),
                    startTime,
                    endTime
                )
                
                Log.d("ElectricityRepo", "查询结果：获取到 ${result.size} 条记录")
                
                // 记录每天的数据情况
                val dailyRecords = result.groupBy { 
                    val cal = Calendar.getInstance()
                    cal.time = it.date
                    cal.get(Calendar.DAY_OF_MONTH)
                }
                
                dailyRecords.forEach { (day, records) ->
                    Log.d("ElectricityRepo", "日期 $day 日有 ${records.size} 条记录")
                }
                
                return@withContext result
            } catch (e: Exception) {
                Log.e("ElectricityRepo", "获取历史数据失败: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }
    
    // 清除历史记录
    suspend fun clearAllHistoryData() {
        withContext(Dispatchers.IO) {
            val historyDao = ElectricityDatabase.getDatabase(context).electricityHistoryDao()
            historyDao.clearHistory(getCurrentBuilding(), getRoomId())
        }
    }
    
    // 根据具体日期查询历史数据
    suspend fun getHistoryByDate(year: Int, month: Int, day: Int): List<ElectricityHistoryData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ElectricityRepo", "查询 $year-$month-$day 的历史数据")
                
                // 设置起始时间（当天0点）
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = Date(calendar.timeInMillis)
                
                // 设置结束时间（当天23:59:59.999）
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endTime = Date(calendar.timeInMillis)
                
                Log.d("ElectricityRepo", "查询时间范围：${startTime} 至 ${endTime}")
                
                val historyDao = ElectricityDatabase.getDatabase(context).electricityHistoryDao()
                val result = historyDao.getHistoryByDateRange(
                    getCurrentBuilding(),
                    getRoomId(),
                    startTime,
                    endTime
                )
                
                Log.d("ElectricityRepo", "查询结果：获取到 ${result.size} 条记录")
                return@withContext result
            } catch (e: Exception) {
                Log.e("ElectricityRepo", "获取日期历史数据失败: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }
    
    // SharedPreferences相关方法
    fun isJsessionIdSet(): Boolean {
        return getJsessionId().isNotEmpty()
    }
    
    fun getJsessionId(): String {
        return sharedPreferences.getString(KEY_JSESSION_ID, "") ?: ""
    }
    
    fun saveJsessionId(jsessionId: String) {
        sharedPreferences.edit {
            putString(KEY_JSESSION_ID, jsessionId)
        }
    }
    
    fun getCurrentBuilding(): String {
        return sharedPreferences.getString(KEY_CURRENT_BUILDING, DEFAULT_BUILDING) ?: DEFAULT_BUILDING
    }
    
    fun saveCurrentBuilding(building: String) {
        sharedPreferences.edit {
            putString(KEY_CURRENT_BUILDING, building)
        }
    }
    
    fun getRoomId(): String {
        return sharedPreferences.getString(KEY_ROOM_ID, DEFAULT_ROOM_ID) ?: DEFAULT_ROOM_ID
    }
    
    fun saveRoomId(roomId: String) {
        sharedPreferences.edit {
            putString(KEY_ROOM_ID, roomId)
        }
    }
    
    fun getRefreshInterval(): Int {
        return sharedPreferences.getInt(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)
    }
    
    fun saveRefreshInterval(minutes: Int) {
        sharedPreferences.edit {
            putInt(KEY_REFRESH_INTERVAL, minutes)
        }
    }
    
    fun getLowBalanceThreshold(): Float {
        return sharedPreferences.getFloat(KEY_LOW_BALANCE_THRESHOLD, DEFAULT_LOW_BALANCE_THRESHOLD)
    }
    
    fun saveLowBalanceThreshold(threshold: Float) {
        sharedPreferences.edit {
            putFloat(KEY_LOW_BALANCE_THRESHOLD, threshold)
        }
    }
    
    // 根据宿舍楼名称获取宿舍楼ID
    private fun getBuildingId(buildingName: String): String {
        return BUILDINGS[buildingName] ?: DEFAULT_BUILDING_ID
    }
    
    companion object {
        private const val PREF_NAME = "electricity_prefs"
        private const val KEY_JSESSION_ID = "jsession_id"
        private const val KEY_CURRENT_BUILDING = "current_building"
        private const val KEY_ROOM_ID = "room_id"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval"
        private const val KEY_LOW_BALANCE_THRESHOLD = "low_balance_threshold"
        
        // 默认值
        private const val DEFAULT_BUILDING = "女05#楼"
        private const val DEFAULT_ROOM_ID = "324"
        private const val DEFAULT_BUILDING_ID = "34"
        private const val DEFAULT_REFRESH_INTERVAL = 30
        private const val DEFAULT_LOW_BALANCE_THRESHOLD = 20f
        
        // API配置
        private const val BASE_URL = "http://tysf.ahpu.edu.cn:8063"
        private const val API_ENDPOINT = "/web/Common/Tsm.html"
        private const val AREA_NAME = "安徽工程大学"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15; 23127PN0CC Build/AQ3A.240627.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/134.0.6998.39 Mobile Safari/537.36"
        
        // 宿舍楼信息（从config.py复制）
        private val BUILDINGS = mapOf(
            // 男生宿舍楼
            "男25#楼" to "6",
            "男22#楼" to "4",
            "男21#楼" to "2",
            "男20#楼" to "8",
            "男19#楼" to "10",
            "男18#楼" to "12",
            "男17#楼" to "14",
            "男16#楼" to "52",
            "男15#楼" to "50",
            "男14#楼" to "49",
            "男12#楼" to "44",
            "男11#楼" to "42",
            "男06#楼" to "21",
            "男05#楼" to "25",
            
            // 女生宿舍楼
            "女13#楼" to "61",
            "女12#楼" to "60",
            "女11#楼" to "19",
            "女10#楼" to "38",
            "女09#楼" to "57",
            "女08#楼" to "17",
            "女07#楼" to "36",
            "女06#楼" to "56",
            "女05#楼" to "34",
            "女04#楼" to "32",
            "女03#楼" to "30",
            "女02#楼" to "28",
            "女01#楼" to "26",
            
            // 研究生宿舍楼
            "研05#楼" to "63",
            "研04#楼" to "16",
            "研03#楼" to "15",
            "研02#楼" to "38",
            "研01#楼" to "37",
            
            // 梦溪7栋宿舍楼
            "梦溪7-1栋" to "65",
            "梦溪7-2栋" to "66",
            "梦溪7-3栋" to "67",
            "梦溪7-4栋" to "68",
            "梦溪7-5栋" to "69",
            "梦溪7-6栋" to "70",
            "梦溪7-7栋" to "71",
            "梦溪7-8栋" to "72",
            
            // 梦溪7-9栋宿舍楼
            "梦溪7-9-A栋" to "74",
            "梦溪7-9-B栋" to "73",
            "梦溪7-9-C栋" to "75"
        )
    }
} 