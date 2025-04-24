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
import kotlinx.coroutines.runBlocking

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
                    var dataSource = ElectricityViewModel.DataSource.ESTIMATED
                    
                    try {
                        // 获取日均用电量及其数据源
                        val usageResult = calculateDailyUsageWithSource()
                        dailyUsage = usageResult.first
                        dataSource = usageResult.second
                        
                        // 确保得到有效的日均用电量
                        if (dailyUsage <= 0f) {
                            val result = analysisBasedEstimate()
                            dailyUsage = result
                            dataSource = ElectricityViewModel.DataSource.ESTIMATED
                            Log.d("ElectricityRepo", "使用智能估算的日均用电量: ${dailyUsage}元/天")
                        }
                    } catch (e: Exception) {
                        Log.e("ElectricityRepo", "计算日均用电量异常: ${e.message}")
                        val result = analysisBasedEstimate()
                        dailyUsage = result
                        dataSource = ElectricityViewModel.DataSource.ESTIMATED
                    }
                    
                    try {
                        estimatedDays = calculateEstimatedDays(balance, dailyUsage)
                        // 确保估计天数合理
                        if (estimatedDays <= 0 && balance > 0) {
                            estimatedDays = (balance / dailyUsage).toInt()
                            Log.d("ElectricityRepo", "使用简单方法计算估计天数: ${estimatedDays}天")
                        }
                    } catch (e: Exception) {
                        Log.e("ElectricityRepo", "计算估计天数异常: ${e.message}")
                        if (balance > 0) {
                            estimatedDays = (balance / dailyUsage).toInt()
                        }
                    }
                    
                    // 保存当前数据源到Application级别变量
                    try {
                        val application = context.applicationContext
                        if (application is ElectricityApplication) {
                            application.currentDataSource = dataSource
                        }
                    } catch (e: Exception) {
                        Log.e("ElectricityRepo", "保存数据源信息失败: ${e.message}")
                    }
                    
                    Log.d("ElectricityRepo", "解析结果: 余额=${balance}元, 日均用电=${dailyUsage}元/天(${dataSource}), 估计可用=${estimatedDays}天")
                    
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
    
    // 计算日均用电量并返回数据源
    private suspend fun calculateDailyUsageWithSource(): Pair<Float, ElectricityViewModel.DataSource> {
        try {
            // 使用更全面的历史数据：短期(7天)、中期(30天)和长期(90天)
            val shortTermData = withContext(Dispatchers.IO) {
                database.electricityDao().getRecentData(7)
            }
            
            val mediumTermData = withContext(Dispatchers.IO) {
                database.electricityDao().getRecentData(30)
            }
            
            val longTermData = withContext(Dispatchers.IO) {
                database.electricityDao().getRecentData(90)
            }
            
            // 收集所有可用的数据点
            val allDataPoints = mutableListOf<Pair<Date, Float>>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            
            // 处理所有时间段的数据
            for (data in longTermData) {
                try {
                    val date = dateFormat.parse(data.timestamp)
                    if (date != null) {
                        allDataPoints.add(Pair(date, data.balance))
                    }
                } catch (e: Exception) {
                    Log.e("ElectricityRepo", "处理日期时出错: ${e.message}")
                }
            }
            
            if (allDataPoints.size < 2) {
                Log.d("ElectricityRepo", "计算日均用电: 历史数据不足，至少需要2条记录")
                return Pair(analysisBasedEstimate(), ElectricityViewModel.DataSource.ESTIMATED)
            }
            
            // 按时间排序
            allDataPoints.sortBy { it.first.time }
            
            // 检测充值和用电记录
            val rechargeRecords = mutableListOf<Triple<Date, Float, Float>>() // 日期、充值前余额、充值金额
            val usageRecords = mutableListOf<UsageRecord>() // 自定义数据类用于更详细的用电记录分析
            
            var lastBalance = allDataPoints.first().second
            var lastDate = allDataPoints.first().first
            
            for (i in 1 until allDataPoints.size) {
                val curr = allDataPoints[i]
                val balanceDiff = curr.second - lastBalance
                val timeDiff = curr.first.time - lastDate.time
                val diffInDays = TimeUnit.MILLISECONDS.toDays(timeDiff).toFloat()
                
                // 只考虑有效的时间间隔 (至少4小时)
                if (timeDiff >= 4 * 60 * 60 * 1000) {
                    // 余额增加，记录充值
                    if (balanceDiff > 0) {
                        rechargeRecords.add(Triple(curr.first, lastBalance, balanceDiff))
                        Log.d("ElectricityRepo", "检测到充值: ${dateFormat.format(curr.first)}, +${balanceDiff}元")
                    } 
                    // 余额减少，记录用电
                    else if (balanceDiff < 0 && diffInDays > 0) {
                        val dailyUsage = -balanceDiff / diffInDays
                        usageRecords.add(
                            UsageRecord(
                                startDate = lastDate,
                                endDate = curr.first,
                                startBalance = lastBalance,
                                endBalance = curr.second,
                                usedAmount = -balanceDiff,
                                daysElapsed = diffInDays,
                                dailyRate = dailyUsage
                            )
                        )
                        Log.d("ElectricityRepo", "检测到用电: ${dateFormat.format(lastDate)} - ${dateFormat.format(curr.first)}, " +
                                "${diffInDays}天消耗${-balanceDiff}元, 日均${dailyUsage}元/天")
                    }
                }
                
                lastBalance = curr.second
                lastDate = curr.first
            }
            
            // 没有用电记录时，尝试分析用户的充值行为
            if (usageRecords.isEmpty()) {
                if (rechargeRecords.isNotEmpty()) {
                    // 存在充值记录，分析充值模式
                    val result = analyzeRechargePattern(rechargeRecords)
                    return Pair(result, ElectricityViewModel.DataSource.RECHARGE_PATTERN)
                }
                return Pair(analysisBasedEstimate(), ElectricityViewModel.DataSource.ESTIMATED)
            }
            
            // 计算不同时段的平均用电量
            var shortTermAvg = 0f
            var shortTermDays = 0f
            var mediumTermAvg = 0f
            var mediumTermDays = 0f
            var longTermAvg = 0f
            var longTermDays = 0f
            
            val now = Date()
            
            for (record in usageRecords) {
                val daysSinceEnd = TimeUnit.MILLISECONDS.toDays(now.time - record.endDate.time).toFloat()
                
                // 所有记录都计入长期平均
                longTermAvg += record.usedAmount
                longTermDays += record.daysElapsed
                
                // 30天内的记录计入中期平均
                if (daysSinceEnd <= 30) {
                    mediumTermAvg += record.usedAmount
                    mediumTermDays += record.daysElapsed
                    
                    // 7天内的记录计入短期平均
                    if (daysSinceEnd <= 7) {
                        shortTermAvg += record.usedAmount
                        shortTermDays += record.daysElapsed
                    }
                }
            }
            
            // 计算不同时段的日均值
            val shortTermDailyAvg = if (shortTermDays > 0) shortTermAvg / shortTermDays else 0f
            val mediumTermDailyAvg = if (mediumTermDays > 0) mediumTermAvg / mediumTermDays else 0f
            val longTermDailyAvg = if (longTermDays > 0) longTermAvg / longTermDays else 0f
            
            Log.d("ElectricityRepo", "日均用电计算: 短期(7天)=${shortTermDailyAvg}元/天, 中期(30天)=${mediumTermDailyAvg}元/天, 长期=${longTermDailyAvg}元/天")
            
            // 根据可用数据智能合成最终的日均用电量和数据源
            var dataSource = ElectricityViewModel.DataSource.ESTIMATED
            val finalDailyUsage = when {
                // 短期数据可用且有意义，给予最高权重
                shortTermDays >= 2 && shortTermDailyAvg > 0 -> {
                    dataSource = ElectricityViewModel.DataSource.SHORT_TERM
                    if (mediumTermDays >= 7) {
                        // 有短期和中期数据，结合两者(短期权重更高)
                        shortTermDailyAvg * 0.7f + mediumTermDailyAvg * 0.3f
                    } else {
                        // 只有短期数据，偏向短期但略微结合长期以增加稳定性
                        shortTermDailyAvg * 0.8f + (if (longTermDailyAvg > 0) longTermDailyAvg * 0.2f else shortTermDailyAvg)
                    }
                }
                // 中期数据可用，作为主要参考
                mediumTermDays >= 5 && mediumTermDailyAvg > 0 -> {
                    dataSource = ElectricityViewModel.DataSource.MEDIUM_TERM
                    if (longTermDays >= 20) {
                        // 有中期和长期数据，结合两者
                        mediumTermDailyAvg * 0.6f + longTermDailyAvg * 0.4f
                    } else {
                        mediumTermDailyAvg
                    }
                }
                // 只有长期数据可用
                longTermDays > 0 && longTermDailyAvg > 0 -> {
                    dataSource = ElectricityViewModel.DataSource.LONG_TERM
                    longTermDailyAvg
                }
                // 没有足够的数据，使用最后一条记录的日均值
                else -> {
                    val lastRecordRate = usageRecords.lastOrNull()?.dailyRate
                    if (lastRecordRate != null && lastRecordRate > 0) {
                        dataSource = ElectricityViewModel.DataSource.LONG_TERM
                        lastRecordRate
                    } else {
                        dataSource = ElectricityViewModel.DataSource.ESTIMATED
                        analysisBasedEstimate()
                    }
                }
            }
            
            // 应用季节调整因子
            val seasonalFactor = getSeasonalFactor()
            val adjustedDailyUsage = finalDailyUsage * seasonalFactor
            
            Log.d("ElectricityRepo", "最终日均用电量: 原始=${finalDailyUsage}元/天, 季节调整=${adjustedDailyUsage}元/天, 数据源=$dataSource")
            
            return Pair(adjustedDailyUsage, dataSource)
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "计算日均用电异常: ${e.message}", e)
            return Pair(analysisBasedEstimate(), ElectricityViewModel.DataSource.ESTIMATED)
        }
    }
    
    // 计算日均用电量
    private suspend fun calculateDailyUsage(): Float {
        val result = calculateDailyUsageWithSource()
        return result.first
    }
    
    // 分析用户充值模式来估计用电量
    private fun analyzeRechargePattern(rechargeRecords: List<Triple<Date, Float, Float>>): Float {
        try {
            if (rechargeRecords.size < 2) {
                return analysisBasedEstimate()
            }
            
            // 按时间排序充值记录
            val sortedRecharges = rechargeRecords.sortedBy { it.first.time }
            
            // 分析充值频率和金额
            var totalAmount = 0f
            val intervalDays = mutableListOf<Int>()
            
            for (i in 1 until sortedRecharges.size) {
                val current = sortedRecharges[i]
                val previous = sortedRecharges[i - 1]
                
                // 计算充值间隔(天)
                val diffInMillis = current.first.time - previous.first.time
                val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
                
                if (diffInDays > 0) {
                    intervalDays.add(diffInDays)
                    totalAmount += previous.third
                }
            }
            
            // 添加最后一次充值
            totalAmount += sortedRecharges.last().third
            
            // 计算平均充值间隔和金额
            val avgInterval = if (intervalDays.isNotEmpty()) intervalDays.average().toFloat() else 30f
            val avgAmount = if (sortedRecharges.isNotEmpty()) totalAmount / sortedRecharges.size else 0f
            
            // 估计日均用电 = 平均充值金额 / 平均充值间隔
            val estimatedDaily = if (avgInterval > 0 && avgAmount > 0) {
                avgAmount / avgInterval
            } else {
                analysisBasedEstimate()
            }
            
            Log.d("ElectricityRepo", "通过充值模式估算日均用电: 平均${avgInterval}天充值一次${avgAmount}元, 估计日均${estimatedDaily}元/天")
            
            return estimatedDaily
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "分析充值模式异常: ${e.message}", e)
            return analysisBasedEstimate()
        }
    }
    
    // 获取季节性调整因子
    private fun getSeasonalFactor(): Float {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        
        // 根据月份返回相应的季节性因子
        return when (month) {
            in 6..8 -> 1.25f  // 夏季 - 空调使用高峰
            9 -> 1.1f         // 初秋 - 仍有空调使用
            in 12..12, in 1..2 -> 1.2f  // 冬季 - 取暖设备使用
            3 -> 1.1f         // 初春 - 可能有取暖需求
            else -> 1.0f      // 其他季节
        }
    }
    
    // 当没有足够历史数据时，使用更智能的估算方法
    private fun analysisBasedEstimate(): Float {
        // 获取最新的余额数据
        val latestBalance = runBlocking {
            val data = withContext(Dispatchers.IO) {
                database.electricityDao().getLastRecord()
            }
            data?.balance ?: 0f
        }
        
        // 考虑季节因素的基础用电量
        val baseDailyUsage = when (getSeasonForCurrentMonth()) {
            0 -> 2.0f  // 春季 - 基础用电
            1 -> 3.0f  // 夏季 - 空调用电高
            2 -> 2.0f  // 秋季 - 基础用电
            else -> 2.5f  // 冬季 - 取暖用电较高
        }
        
        // 根据余额调整估计值
        val adjustedUsage = when {
            latestBalance > 150 -> baseDailyUsage * 1.2f  // 余额充足，可能使用更多电器
            latestBalance > 100 -> baseDailyUsage * 1.1f  // 余额较多
            latestBalance > 50 -> baseDailyUsage          // 余额适中
            latestBalance > 20 -> baseDailyUsage * 0.9f   // 余额较少，可能会节约用电
            latestBalance > 0 -> baseDailyUsage * 0.8f    // 余额很少，可能会极度节约
            else -> baseDailyUsage                        // 无余额数据，使用基础值
        }
        
        Log.d("ElectricityRepo", "智能估算日均用电: 余额=${latestBalance}元, 基础用电=${baseDailyUsage}元/天, 调整后=${adjustedUsage}元/天")
        
        return adjustedUsage
    }
    
    // 获取当前月份的季节(0=春,1=夏,2=秋,3=冬)
    private fun getSeasonForCurrentMonth(): Int {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        
        return when (month) {
            in 3..5 -> 0   // 春季
            in 6..8 -> 1   // 夏季
            in 9..11 -> 2  // 秋季
            else -> 3      // 冬季
        }
    }
    
    // 用电记录数据类，更好地组织和分析用电数据
    private data class UsageRecord(
        val startDate: Date,
        val endDate: Date,
        val startBalance: Float,
        val endBalance: Float,
        val usedAmount: Float,
        val daysElapsed: Float,
        val dailyRate: Float
    )
    
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
                // 使用季节性默认日均用电量
                val estimatedDailyUsage = analysisBasedEstimate()
                val estimatedDays = (balance / estimatedDailyUsage).toInt()
                Log.d("ElectricityRepo", "计算剩余天数: 日均用电量过低(${dailyUsage}), 使用默认估计值(${estimatedDailyUsage}元/天), 估计可用${estimatedDays}天")
                return estimatedDays
            }
            
            // 考虑季节变化因素
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            
            // 检查未来日期是否会进入高耗电季节
            var adjustedBalance = balance
            var estimatedDays = 0
            var currentDailyUsage = dailyUsage
            var remainingBalance = adjustedBalance
            
            // 分阶段计算剩余天数，考虑季节变化
            while (remainingBalance > 0) {
                // 计算当前阶段可用天数
                val daysInCurrentStage = (remainingBalance / currentDailyUsage).coerceAtMost(30f) // 最多预测30天
                
                // 累加天数
                estimatedDays += daysInCurrentStage.toInt()
                
                // 扣除已使用的余额
                remainingBalance -= daysInCurrentStage * currentDailyUsage
                
                // 检查下一阶段是否进入了新的月份，调整用电量
                val nextStageMonth = getMonthAfterDays(currentMonth, estimatedDays)
                val seasonalFactor = when (nextStageMonth) {
                    in 6..9 -> 1.2f  // 夏季
                    in 12..12, in 1..2 -> 1.15f  // 冬季
                    else -> 1.0f  // 春秋季
                }
                
                // 如果季节变化，调整日均用电量
                if (getSeasonForMonth(nextStageMonth) != getSeasonForMonth(currentMonth)) {
                    currentDailyUsage = dailyUsage * seasonalFactor
                    Log.d("ElectricityRepo", "计算剩余天数: 预测进入新季节(${nextStageMonth}月), 调整日均用电量为${currentDailyUsage}元/天")
                }
                
                // 如果剩余不足一天使用量，结束计算
                if (remainingBalance < currentDailyUsage) {
                    break
                }
            }
            
            // 为避免过度乐观预测，设置上限为90天
            val cappedEstimatedDays = Math.min(estimatedDays, 90)
            Log.d("ElectricityRepo", "计算剩余天数: 原始预测=${estimatedDays}天, 上限限制后=${cappedEstimatedDays}天")
            
            return cappedEstimatedDays
        } catch (e: Exception) {
            Log.e("ElectricityRepo", "计算剩余天数异常: ${e.message}", e)
            // 出现异常时，提供一个基于余额的粗略估计
            if (balance > 0) {
                // 假设每天使用基于季节的默认值
                val defaultDailyUsage = analysisBasedEstimate()
                val defaultEstimate = (balance / defaultDailyUsage).toInt()
                return Math.min(defaultEstimate, 90)  // 同样限制最大预测为90天
            }
            return 0
        }
    }
    
    // 获取指定天数后的月份
    private fun getMonthAfterDays(startMonth: Int, days: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, startMonth - 1) // Calendar.MONTH是0-based
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.get(Calendar.MONTH) + 1 // 返回1-based月份
    }
    
    // 根据月份获取季节
    private fun getSeasonForMonth(month: Int): Int {
        return when (month) {
            in 3..5 -> 0  // 春季
            in 6..8 -> 1  // 夏季
            in 9..11 -> 2  // 秋季
            else -> 3      // 冬季
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
                // 获取前一条记录，计算余额变化
                val previousRecord = getLastElectricityData()
                var usage = 0.0
                var recharge = 0.0
                
                if (previousRecord != null) {
                    val balanceDiff = data.balance - previousRecord.balance
                    Log.d("ElectricityRepo", "余额变化: ${balanceDiff}元 (之前: ${previousRecord.balance}元, 当前: ${data.balance}元)")
                    
                    if (balanceDiff > 0) {
                        // 余额增加，说明充值了
                        recharge = balanceDiff.toDouble()
                        Log.d("ElectricityRepo", "检测到充值: ${recharge}元")
                    } else if (balanceDiff < 0) {
                        // 余额减少，说明用电了
                        usage = -balanceDiff.toDouble()
                        Log.d("ElectricityRepo", "检测到用电: ${usage}元")
                    }
                }
                
                // 确保数据有效性
                val validBalance = if (data.balance >= 0f) data.balance else 0f
                
                val historyData = ElectricityHistoryData(
                    date = Date(System.currentTimeMillis()),
                    balance = validBalance.toDouble(),
                    building = data.building,
                    roomId = data.roomId,
                    usage = usage,         // 实际用电变化量，而不是日均用电估计
                    recharge = recharge    // 实际充值金额，而不是估计剩余天数
                )
                
                Log.d("ElectricityRepo", "添加历史记录: 余额=${validBalance}元, 用电变化=${usage}元, 充值金额=${recharge}元")
                
                val historyDao = ElectricityDatabase.getDatabase(context).electricityHistoryDao()
                historyDao.insertHistory(historyData)
            } catch (e: Exception) {
                Log.e("ElectricityRepo", "添加电费历史记录失败: ${e.message}", e)
            }
        }
    }
    
    // 根据电费数据变化计算用电量和充值金额
    suspend fun calculateElectricityChanges(newData: ElectricityData): Pair<Double, Double> {
        return withContext(Dispatchers.IO) {
            try {
                // 获取前一条记录
                val previousRecord = getLastElectricityData()
                var usage = 0.0
                var recharge = 0.0
                
                if (previousRecord != null) {
                    val balanceDiff = newData.balance - previousRecord.balance
                    
                    if (balanceDiff > 0) {
                        // 余额增加，说明充值了
                        recharge = balanceDiff.toDouble()
                    } else if (balanceDiff < 0) {
                        // 余额减少，说明用电了
                        usage = -balanceDiff.toDouble()
                    }
                }
                
                Pair(usage, recharge)
            } catch (e: Exception) {
                Log.e("ElectricityRepo", "计算电量变化异常: ${e.message}", e)
                Pair(0.0, 0.0)
            }
        }
    }
    
    // 检测并记录电费变化
    suspend fun checkAndRecordElectricityChange(newData: ElectricityData): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val previousRecord = getLastElectricityData()
                
                // 如果没有之前的记录，或者余额有变化，则添加记录
                if (previousRecord == null || previousRecord.balance != newData.balance) {
                    addElectricityHistory(newData)
                    return@withContext true
                }
                
                return@withContext false
            } catch (e: Exception) {
                Log.e("ElectricityRepo", "检测电费变化异常: ${e.message}", e)
                return@withContext false
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
        val jsessionId = getJsessionId()
        return jsessionId.isNotEmpty()
    }
    
    fun getJsessionId(): String {
        return sharedPreferences.getString(PREF_JSESSION_ID, "") ?: ""
    }
    
    fun updateJsessionId(jsessionId: String) {
        sharedPreferences.edit {
            putString(PREF_JSESSION_ID, jsessionId)
        }
        Log.d("ElectricityRepo", "JSESSIONID已更新")
    }
    
    fun getCurrentBuilding(): String {
        return sharedPreferences.getString(PREF_BUILDING, DEFAULT_BUILDING) ?: DEFAULT_BUILDING
    }
    
    fun updateBuilding(building: String) {
        sharedPreferences.edit {
            putString(PREF_BUILDING, building)
        }
        Log.d("ElectricityRepo", "宿舍楼已更新为：$building")
    }
    
    fun getRoomId(): String {
        return sharedPreferences.getString(PREF_ROOM_ID, DEFAULT_ROOM_ID) ?: DEFAULT_ROOM_ID
    }
    
    fun updateRoomId(roomId: String) {
        sharedPreferences.edit {
            putString(PREF_ROOM_ID, roomId)
        }
        Log.d("ElectricityRepo", "房间号已更新为：$roomId")
    }
    
    fun getRefreshInterval(): Int {
        return sharedPreferences.getInt(PREF_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)
    }
    
    fun updateRefreshInterval(interval: Int) {
        sharedPreferences.edit {
            putInt(PREF_REFRESH_INTERVAL, interval)
        }
        Log.d("ElectricityRepo", "刷新间隔已更新为：$interval 分钟")
    }
    
    fun getLowBalanceThreshold(): Float {
        return sharedPreferences.getFloat(PREF_LOW_BALANCE_THRESHOLD, DEFAULT_LOW_BALANCE_THRESHOLD)
    }
    
    fun updateLowBalanceThreshold(threshold: Float) {
        sharedPreferences.edit {
            putFloat(PREF_LOW_BALANCE_THRESHOLD, threshold)
        }
        Log.d("ElectricityRepo", "低余额阈值已更新为：$threshold 元")
    }
    
    // 根据宿舍楼名称获取宿舍楼ID
    private fun getBuildingId(buildingName: String): String {
        return BUILDINGS[buildingName] ?: DEFAULT_BUILDING_ID
    }
    
    // 添加获取定时刷新设置方法
    fun isScheduledRefreshEnabled(): Boolean {
        return sharedPreferences.getBoolean(PREF_SCHEDULED_REFRESH_ENABLED, true)
    }
    
    // 添加更新定时刷新设置方法
    fun updateScheduledRefreshEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(PREF_SCHEDULED_REFRESH_ENABLED, enabled)
        }
        Log.d("ElectricityRepo", "定时刷新功能已${if (enabled) "启用" else "禁用"}")
    }
    
    companion object {
        private const val PREF_NAME = "electricity_preferences"
        private const val PREF_JSESSION_ID = "jsession_id"
        private const val PREF_BUILDING = "building"
        private const val PREF_ROOM_ID = "room_id"
        private const val PREF_REFRESH_INTERVAL = "refresh_interval"
        private const val PREF_LOW_BALANCE_THRESHOLD = "low_balance_threshold"
        private const val PREF_SCHEDULED_REFRESH_ENABLED = "scheduled_refresh_enabled"
        
        // 默认值
        private const val DEFAULT_BUILDING = "女05#楼"
        private const val DEFAULT_ROOM_ID = "324"
        private const val DEFAULT_REFRESH_INTERVAL = 30 // 30分钟
        private const val DEFAULT_LOW_BALANCE_THRESHOLD = 20f // 20元
        private const val DEFAULT_BUILDING_ID = "34"
        
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