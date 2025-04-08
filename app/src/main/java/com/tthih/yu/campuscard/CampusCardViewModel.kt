package com.tthih.yu.campuscard

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

class CampusCardViewModel : ViewModel() {
    // 用于日志标记
    private val TAG = "CampusCardViewModel"
    
    private val _transactionData = MutableLiveData<List<CampusCardTransaction>>(emptyList())
    val transactionData: LiveData<List<CampusCardTransaction>> = _transactionData
    
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _loginStatus = MutableLiveData<String>("")
    val loginStatus: LiveData<String> = _loginStatus
    
    private val _cardInfo = MutableLiveData<CardInfo?>(null)
    val cardInfo: LiveData<CardInfo?> = _cardInfo
    
    private val _monthBill = MutableLiveData<MonthBill?>(null)
    val monthBill: LiveData<MonthBill?> = _monthBill
    
    private val _trends = MutableLiveData<ConsumeTrend?>(null)
    val trends: LiveData<ConsumeTrend?> = _trends
    
    private val repository = CampusCardRepository()
    
    fun initializeRepository(context: Context) {
        repository.initialize(context)
    }
    
    fun updateLoginStatus(status: String) {
        _loginStatus.postValue(status)
    }
    
    fun setDataLoaded(loaded: Boolean) {
        _isLoading.postValue(!loaded)
    }
    
    fun processCardInfo(jsonData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject(jsonData)
                if (jsonObject.optInt("status", 0) == 200) {
                    val cardNumber = jsonObject.optString("cardNumber", "")
                    val balance = jsonObject.optString("balance", "0").toDoubleOrNull() ?: 0.0
                    val expiryDate = jsonObject.optString("expiryDate", "")
                    val status = jsonObject.optString("cardStatus", "")
                    
                    val info = CardInfo(
                        cardNumber = cardNumber,
                        balance = balance,
                        expiryDate = expiryDate,
                        status = status
                    )
                    
                    _cardInfo.postValue(info)
                    updateLoginStatus("获取卡片信息成功")
                }
            } catch (e: Exception) {
                updateLoginStatus("处理卡片信息失败: ${e.message}")
            }
        }
    }
    
    fun processMonthBill(jsonData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject(jsonData)
                if (jsonObject.optInt("status", 0) == 200) {
                    val totalAmount = jsonObject.optString("totalAmount", "0").toDoubleOrNull() ?: 0.0
                    val inAmount = jsonObject.optString("inAmount", "0").toDoubleOrNull() ?: 0.0
                    
                    val bill = MonthBill(
                        totalAmount = totalAmount,
                        inAmount = inAmount
                    )
                    
                    _monthBill.postValue(bill)
                    updateLoginStatus("获取当月账单成功")
                }
            } catch (e: Exception) {
                updateLoginStatus("处理当月账单失败: ${e.message}")
            }
        }
    }
    
    fun processTrends(jsonData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonObject = JSONObject(jsonData)
                if (jsonObject.optInt("status", 0) == 200) {
                    // 尝试多种格式解析消费趋势数据
                    val trend = parseTrendData(jsonObject)
                    _trends.postValue(trend)
                    updateLoginStatus("获取消费趋势成功")
                } else {
                    updateLoginStatus("获取消费趋势失败: 状态不正确")
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理消费趋势数据失败: ${e.message}")
                updateLoginStatus("处理消费趋势失败: ${e.message}")
            }
        }
    }
    
    private fun parseTrendData(jsonObject: JSONObject): ConsumeTrend {
        try {
            // 解析完整的趋势数据
            if (jsonObject.has("xData") && jsonObject.has("yData")) {
                val xDataArray = jsonObject.getJSONArray("xData")
                val yDataArray = jsonObject.getJSONArray("yData")
                
                val dates = mutableListOf<String>()
                val amounts = mutableListOf<Double>()
                var totalAmount = 0.0
                
                for (i in 0 until xDataArray.length()) {
                    dates.add(xDataArray.getString(i))
                }
                
                for (i in 0 until yDataArray.length()) {
                    val amount = yDataArray.getDouble(i)
                    amounts.add(amount)
                    totalAmount += amount
                }
                
                val maxAmount = if (amounts.isEmpty()) 0.0 else amounts.maxOrNull() ?: 0.0
                val averageAmount = if (amounts.isEmpty()) 0.0 else totalAmount / amounts.size
                
                return ConsumeTrend(
                    dates = dates,
                    amounts = amounts,
                    maxAmount = maxAmount,
                    totalAmount = totalAmount,
                    averageAmount = averageAmount
                )
            }
            
            // 尝试解析serisData格式
            if (jsonObject.has("serisData")) {
                val serisData = jsonObject.getJSONArray("serisData")
                val dates = mutableListOf<String>()
                val amounts = mutableListOf<Double>()
                var totalAmount = 0.0
                
                for (i in 0 until serisData.length()) {
                    val item = serisData.getJSONArray(i)
                    if (item.length() >= 2) {
                        val date = item.getString(0)
                        val amount = item.getDouble(1)
                        
                        dates.add(date)
                        amounts.add(amount)
                        totalAmount += amount
                    }
                }
                
                val maxAmount = if (amounts.isEmpty()) 0.0 else amounts.maxOrNull() ?: 0.0
                val averageAmount = if (amounts.isEmpty()) 0.0 else totalAmount / amounts.size
                
                return ConsumeTrend(
                    dates = dates,
                    amounts = amounts,
                    maxAmount = maxAmount,
                    totalAmount = totalAmount,
                    averageAmount = averageAmount
                )
            }
            
            // 如果没有找到消费趋势数据，返回空对象
            return ConsumeTrend()
        } catch (e: Exception) {
            e.printStackTrace()
            return ConsumeTrend()
        }
    }
    
    fun processTransactionData(jsonData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = parseTransactionData(jsonData)
                
                // 如果解析到了数据，更新界面
                if (transactions.isNotEmpty()) {
                    _transactionData.postValue(transactions)
                    _isLoading.postValue(false)
                    
                    // 存储到本地数据库
                    repository.saveTransactions(transactions)
                    
                    updateLoginStatus("获取交易数据成功: ${transactions.size} 条记录")
                } else {
                    // 如果没有解析到数据，记录日志但不更新界面
                    // 这样可以避免覆盖之前可能存在的数据
                    updateLoginStatus("未能解析出交易数据")
                    _isLoading.postValue(false)
                }
            } catch (e: Exception) {
                updateLoginStatus("处理交易数据失败: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }
    
    private fun parseTransactionData(jsonData: String): List<CampusCardTransaction> {
        val result = mutableListOf<CampusCardTransaction>()
        
        try {
            val jsonObject = JSONObject(jsonData)
            
            // 检查数据来源
            val source = jsonObject.optString("source", "")
            
            // 从页面直接提取的数据
            if (source == "page-extraction") {
                val dataArray = jsonObject.optJSONArray("rows") ?: return emptyList()
                
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val transaction = CampusCardTransaction(
                        id = item.optString("id", ""),
                        time = item.optString("time", ""),
                        amount = item.optDouble("amount", 0.0),
                        balance = item.optDouble("balance", 0.0),
                        type = determineTransactionType(item.optDouble("amount", 0.0)),
                        location = item.optString("location", ""),
                        description = item.optString("description", "")
                    )
                    result.add(transaction)
                }
                _loginStatus.postValue("从页面提取到 ${result.size} 条交易记录")
                return result
            }
            
            // 处理API返回的数据
            // 先看rows格式 (getBillDetail.do API)
            if (jsonObject.has("rows")) {
                val dataArray = jsonObject.optJSONArray("rows")
                
                if (dataArray != null && dataArray.length() > 0) {
                    _loginStatus.postValue("API返回了 ${dataArray.length()} 条交易记录")
                    
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        
                        // 支持多种字段名格式
                        val id = item.optString("JYLSH", item.optString("jylsh", item.optString("id", "")))
                        val time = item.optString("JYSJ", item.optString("jysj", item.optString("time", "")))
                        val amountStr = item.optString("JYJE", item.optString("jyje", item.optString("amount", "0")))
                        val balanceStr = item.optString("ZHYE", item.optString("zhye", item.optString("balance", "0")))
                        val type = item.optString("JYLX", item.optString("jylx", item.optString("type", "")))
                        val location = item.optString("ZDMC", item.optString("zdmc", item.optString("location", "")))
                        val description = item.optString("JYMC", item.optString("jymc", item.optString("description", "")))
                        
                        // 解析金额和余额，处理不同格式
                        val amount = parseAmount(amountStr)
                        val balance = parseAmount(balanceStr)
                        
                        val transaction = CampusCardTransaction(
                            id = id.ifEmpty { "${System.currentTimeMillis()}-$i" },
                            time = time,
                            amount = amount,
                            balance = balance,
                            type = type.ifEmpty { determineTransactionType(amount) },
                            location = location,
                            description = description
                        )
                        
                        result.add(transaction)
                    }
                }
            }
            // 再看data格式 (可能是其他API)
            else if (jsonObject.has("data")) {
                val dataArray = jsonObject.optJSONArray("data")
                
                if (dataArray != null && dataArray.length() > 0) {
                    _loginStatus.postValue("API(data格式)返回了 ${dataArray.length()} 条交易记录")
                    
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        
                        // 支持多种字段名格式
                        val id = item.optString("JYLSH", item.optString("jylsh", item.optString("id", "")))
                        val time = item.optString("JYSJ", item.optString("jysj", item.optString("time", "")))
                        val amountStr = item.optString("JYJE", item.optString("jyje", item.optString("amount", "0")))
                        val balanceStr = item.optString("ZHYE", item.optString("zhye", item.optString("balance", "0")))
                        val type = item.optString("JYLX", item.optString("jylx", item.optString("type", "")))
                        val location = item.optString("ZDMC", item.optString("zdmc", item.optString("location", "")))
                        val description = item.optString("JYMC", item.optString("jymc", item.optString("description", "")))
                        
                        // 解析金额和余额，处理不同格式
                        val amount = parseAmount(amountStr)
                        val balance = parseAmount(balanceStr)
                        
                        val transaction = CampusCardTransaction(
                            id = id.ifEmpty { "${System.currentTimeMillis()}-$i" },
                            time = time,
                            amount = amount,
                            balance = balance,
                            type = type.ifEmpty { determineTransactionType(amount) },
                            location = location,
                            description = description
                        )
                        
                        result.add(transaction)
                    }
                }
            }
            // 检查datas格式
            else if (jsonObject.has("datas")) {
                val datasObj = jsonObject.optJSONObject("datas")
                if (datasObj != null) {
                    // 可能的格式1: datas.rows
                    if (datasObj.has("rows")) {
                        val dataArray = datasObj.optJSONArray("rows")
                        if (dataArray != null && dataArray.length() > 0) {
                            _loginStatus.postValue("API(datas.rows格式)返回了 ${dataArray.length()} 条交易记录")
                            
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                
                                // 尝试所有可能的字段名
                                val id = item.optString("JYLSH", item.optString("jylsh", item.optString("id", "")))
                                val time = item.optString("JYSJ", item.optString("jysj", item.optString("time", "")))
                                val amountStr = item.optString("JYJE", item.optString("jyje", item.optString("amount", "0")))
                                val balanceStr = item.optString("ZHYE", item.optString("zhye", item.optString("balance", "0")))
                                val type = item.optString("JYLX", item.optString("jylx", item.optString("type", "")))
                                val location = item.optString("ZDMC", item.optString("zdmc", item.optString("location", "")))
                                val description = item.optString("JYMC", item.optString("jymc", item.optString("description", "")))
                                
                                val amount = parseAmount(amountStr)
                                val balance = parseAmount(balanceStr)
                                
                                val transaction = CampusCardTransaction(
                                    id = id.ifEmpty { "${System.currentTimeMillis()}-$i" },
                                    time = time,
                                    amount = amount,
                                    balance = balance,
                                    type = type.ifEmpty { determineTransactionType(amount) },
                                    location = location,
                                    description = description
                                )
                                
                                result.add(transaction)
                            }
                        }
                    }
                    // 可能的格式2: datas本身作为数组
                    else {
                        // 尝试获取数据的所有键，看是否有可用数据
                        val keys = datasObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            try {
                                val dataArray = datasObj.optJSONArray(key)
                                if (dataArray != null && dataArray.length() > 0) {
                                    _loginStatus.postValue("API(datas." + key + "格式)返回了 ${dataArray.length()} 条交易记录")
                                    
                                    for (i in 0 until dataArray.length()) {
                                        try {
                                            val item = dataArray.getJSONObject(i)
                                            
                                            // 找到第一个看起来像日期的字段作为时间
                                            var time = ""
                                            var amount = 0.0
                                            var balance = 0.0
                                            var type = ""
                                            var location = ""
                                            var description = ""
                                            
                                            val itemKeys = item.keys()
                                            while (itemKeys.hasNext()) {
                                                val itemKey = itemKeys.next()
                                                val value = item.optString(itemKey, "")
                                                
                                                // 根据字段名或内容推断字段类型
                                                when {
                                                    // 时间字段通常包含日期格式
                                                    (itemKey.contains("time", ignoreCase = true) || 
                                                     itemKey.contains("date", ignoreCase = true) || 
                                                     itemKey.contains("sj", ignoreCase = true)) &&
                                                    (value.contains("-") || value.contains(":")) -> time = value
                                                    
                                                    // 金额字段通常包含"金额"或"amount"
                                                    (itemKey.contains("amount", ignoreCase = true) || 
                                                     itemKey.contains("je", ignoreCase = true) || 
                                                     itemKey.contains("jine", ignoreCase = true)) -> 
                                                        amount = parseAmount(value)
                                                    
                                                    // 余额字段通常包含"余额"或"balance"
                                                    (itemKey.contains("balance", ignoreCase = true) || 
                                                     itemKey.contains("ye", ignoreCase = true)) -> 
                                                        balance = parseAmount(value)
                                                    
                                                    // 交易类型字段
                                                    (itemKey.contains("type", ignoreCase = true) || 
                                                     itemKey.contains("lx", ignoreCase = true)) -> type = value
                                                    
                                                    // 地点字段
                                                    (itemKey.contains("location", ignoreCase = true) || 
                                                     itemKey.contains("place", ignoreCase = true) || 
                                                     itemKey.contains("dd", ignoreCase = true) || 
                                                     itemKey.contains("zdmc", ignoreCase = true)) -> location = value
                                                    
                                                    // 描述字段
                                                    (itemKey.contains("desc", ignoreCase = true) || 
                                                     itemKey.contains("name", ignoreCase = true) || 
                                                     itemKey.contains("mc", ignoreCase = true) || 
                                                     itemKey.contains("ms", ignoreCase = true)) -> description = value
                                                }
                                            }
                                            
                                            // 如果找到时间和金额，就认为是有效的交易记录
                                            if (time.isNotEmpty() && amount != 0.0) {
                                                val transaction = CampusCardTransaction(
                                                    id = "${System.currentTimeMillis()}-$i",
                                                    time = time,
                                                    amount = amount,
                                                    balance = balance,
                                                    type = type.ifEmpty { determineTransactionType(amount) },
                                                    location = location,
                                                    description = description
                                                )
                                                result.add(transaction)
                                            }
                                        } catch (e: Exception) {
                                            continue // 跳过解析失败的项
                                        }
                                    }
                                    
                                    if (result.isNotEmpty()) {
                                        break // 如果找到数据，跳出循环
                                    }
                                }
                            } catch (e: Exception) {
                                continue // 跳过解析失败的键
                            }
                        }
                    }
                }
            }
            
            // 如果API没有返回数据，尝试分析完整的响应
            if (result.isEmpty()) {
                _loginStatus.postValue("标准API格式未返回数据，尝试分析完整响应")
                
                // 遍历JSON对象的所有键，查找可能的数据数组
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    try {
                        val value = jsonObject.opt(key)
                        if (value is JSONObject) {
                            // 递归检查内部对象
                            val subKeys = value.keys()
                            while (subKeys.hasNext()) {
                                val subKey = subKeys.next()
                                try {
                                    val subValue = value.opt(subKey)
                                    if (subValue is JSONArray && subValue.length() > 0) {
                                        _loginStatus.postValue("在 " + key + "." + subKey + " 找到可能的交易数据")
                                        
                                        for (i in 0 until subValue.length()) {
                                            try {
                                                val item = subValue.getJSONObject(i)
                                                
                                                // 通过推断判断字段类型
                                                var timeField = ""
                                                var amountField = ""
                                                var balanceField = ""
                                                var typeField = ""
                                                var locationField = ""
                                                var descriptionField = ""
                                                
                                                // 先分析所有字段，判断它们的类型
                                                val itemKeys = item.keys()
                                                while (itemKeys.hasNext()) {
                                                    val itemKey = itemKeys.next()
                                                    val itemValue = item.optString(itemKey, "")
                                                    
                                                    if (itemValue.isEmpty()) continue
                                                    
                                                    // 根据字段内容特征推断类型
                                                    when {
                                                        // 时间字段通常包含日期分隔符
                                                        (itemValue.contains("-") && itemValue.length > 8) || 
                                                        (itemValue.contains(":") && itemValue.length > 5) -> 
                                                            timeField = itemKey
                                                        
                                                        // 金额/余额字段通常是数字
                                                        itemValue.replace("[^0-9.-]".toRegex(), "").isNotEmpty() && 
                                                        !timeField.equals(itemKey) && 
                                                        (amountField.isEmpty() || balanceField.isEmpty()) -> {
                                                            if (amountField.isEmpty()) {
                                                                amountField = itemKey
                                                            } else {
                                                                balanceField = itemKey
                                                            }
                                                        }
                                                        
                                                        // 描述字段通常较长
                                                        itemValue.length > 4 && !timeField.equals(itemKey) && 
                                                        !amountField.equals(itemKey) && !balanceField.equals(itemKey) -> 
                                                            descriptionField = itemKey
                                                        
                                                        // 其它字段可能是类型或地点
                                                        else -> {
                                                            if (typeField.isEmpty()) {
                                                                typeField = itemKey
                                                            } else if (locationField.isEmpty()) {
                                                                locationField = itemKey
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                // 如果找到了时间和金额字段，创建交易记录
                                                if (timeField.isNotEmpty() && amountField.isNotEmpty()) {
                                                    val time = item.optString(timeField, "")
                                                    val amountStr = item.optString(amountField, "0")
                                                    val balanceStr = if (balanceField.isNotEmpty()) 
                                                        item.optString(balanceField, "0") else "0"
                                                    val type = if (typeField.isNotEmpty()) 
                                                        item.optString(typeField, "") else ""
                                                    val location = if (locationField.isNotEmpty()) 
                                                        item.optString(locationField, "") else ""
                                                    val description = if (descriptionField.isNotEmpty()) 
                                                        item.optString(descriptionField, "") else ""
                                                    
                                                    val amount = parseAmount(amountStr)
                                                    val balance = parseAmount(balanceStr)
                                                    
                                                    val transaction = CampusCardTransaction(
                                                        id = "${System.currentTimeMillis()}-$i",
                                                        time = time,
                                                        amount = amount,
                                                        balance = balance,
                                                        type = type.ifEmpty { determineTransactionType(amount) },
                                                        location = location,
                                                        description = description
                                                    )
                                                    
                                                    result.add(transaction)
                                                }
                                            } catch (e: Exception) {
                                                continue // 跳过解析失败的项
                                            }
                                        }
                                        
                                        if (result.isNotEmpty()) {
                                            break // 如果找到数据，跳出循环
                                        }
                                    }
                                } catch (e: Exception) {
                                    continue // 跳过解析失败的键
                                }
                            }
                            
                            if (result.isNotEmpty()) {
                                break // 如果找到数据，跳出循环
                            }
                        } else if (value is JSONArray && value.length() > 0) {
                            _loginStatus.postValue("在 " + key + " 找到可能的交易数据")
                            
                            // 直接分析数组中的对象
                            for (i in 0 until value.length()) {
                                try {
                                    val item = value.getJSONObject(i)
                                    
                                    // 查找日期/时间字段
                                    var time = ""
                                    var amount = 0.0
                                    var balance = 0.0
                                    var type = ""
                                    var location = ""
                                    var description = ""
                                    
                                    val itemKeys = item.keys()
                                    while (itemKeys.hasNext()) {
                                        val itemKey = itemKeys.next()
                                        val itemValue = item.optString(itemKey, "")
                                        
                                        // 简单根据值特征判断类型
                                        if (itemValue.contains("-") || itemValue.contains(":")) {
                                            time = itemValue
                                        } else if (itemValue.replace("[^0-9.-]".toRegex(), "").isNotEmpty()) {
                                            val number = parseAmount(itemValue)
                                            if (amount == 0.0) {
                                                amount = number
                                            } else {
                                                balance = number
                                            }
                                        } else if (itemValue.length > 5) {
                                            description = itemValue
                                        } else if (location.isEmpty()) {
                                            location = itemValue
                                        } else {
                                            type = itemValue
                                        }
                                    }
                                    
                                    // 如果提取到时间和金额，创建交易记录
                                    if (time.isNotEmpty() && amount != 0.0) {
                                        val transaction = CampusCardTransaction(
                                            id = "${System.currentTimeMillis()}-$i",
                                            time = time,
                                            amount = amount,
                                            balance = balance,
                                            type = type.ifEmpty { determineTransactionType(amount) },
                                            location = location,
                                            description = description
                                        )
                                        result.add(transaction)
                                    }
                                } catch (e: Exception) {
                                    continue // 跳过解析失败的项
                                }
                            }
                            
                            if (result.isNotEmpty()) {
                                break // 如果找到数据，跳出循环
                            }
                        }
                    } catch (e: Exception) {
                        continue // 跳过解析失败的键
                    }
                }
            }
            
            // 如果至此仍未找到数据，记录错误
            if (result.isEmpty()) {
                _loginStatus.postValue("无法从返回数据中解析出交易记录")
            }
        } catch (e: Exception) {
            _loginStatus.postValue("解析交易数据异常: ${e.message}")
            e.printStackTrace()
        }
        
        return result.sortedByDescending { it.time }
    }
    
    // 根据金额判断交易类型
    private fun determineTransactionType(amount: Double): String {
        return when {
            amount > 0 -> "充值"
            amount < 0 -> "消费"
            else -> "其他"
        }
    }
    
    // 解析金额字符串为Double，处理不同格式
    private fun parseAmount(amountStr: String): Double {
        return try {
            // 处理可能的货币符号和格式问题
            val cleanedStr = amountStr.replace("[^0-9.-]".toRegex(), "")
            if (cleanedStr.isEmpty()) 0.0 else cleanedStr.toDouble()
        } catch (e: Exception) {
            0.0
        }
    }
    
    fun loadCachedTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedTransactions = repository.getTransactions()
            if (cachedTransactions.isNotEmpty()) {
                _transactionData.postValue(cachedTransactions)
                _isLoading.postValue(false)
            }
        }
    }
    
    /**
     * 重置所有数据并准备重新登录
     */
    fun prepareForRelogin() {
        _isLoading.value = true
        _loginStatus.value = "请登录您的校园账号，完成后点击右上角的返回按钮"
        
        // 清除现有数据，让用户重新登录
        _transactionData.value = emptyList()
        _cardInfo.value = null
        _monthBill.value = null
        _trends.value = ConsumeTrend()
    }
}

data class CardInfo(
    val cardNumber: String,
    val balance: Double,
    val expiryDate: String,
    val status: String
)

data class MonthBill(
    val totalAmount: Double,
    val inAmount: Double
)

data class ConsumeTrend(
    val dates: List<String> = emptyList(),
    val amounts: List<Double> = emptyList(),
    val maxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val averageAmount: Double = 0.0
) 