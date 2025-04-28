package com.tthih.yu.campuscard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.* // For Calendar, Date, Locale
import java.text.SimpleDateFormat
import java.text.ParseException
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.entryOf

class CampusCardViewModel : ViewModel() {
    private val TAG = "CampusCardViewModel"
    private lateinit var repository: CampusCardRepository

    // 预定义的分类，参考自可视化.html中的JavaScript实现
    private val PREDEFINED_CATEGORIES = mapOf(
        "食堂" to listOf("食堂", "餐厅", "餐饮"),
        "超市购物" to listOf("超市", "便利", "小卖部", "商贸", "购物"),
        "水" to listOf("饮水", "水控", "水费", "冷水", "热水", "纯水", "矿泉水"),
        "电费" to listOf("电控", "电费"),
        "洗浴" to listOf("浴", "淋浴", "澡堂"),
        "吹风机" to listOf("吹风", "风筒", "电吹风"),
        "其他" to listOf<String>()
    )

    // --- StateFlows for UI State ---
    private val _uiState = MutableStateFlow(CampusCardUiState())
    val uiState: StateFlow<CampusCardUiState> = _uiState.asStateFlow()

    // Keep track of current pagination for transactions
    private var currentPage = 1
    private var isFetchingTransactions = false
    private var allTransactionsLoaded = false
    private var isInitialized = false // Ensure initialize is called only once

    fun initialize(context: Context) {
        if (isInitialized) return
        // Get Repository instance via Singleton
        repository = CampusCardRepository.getInstance(context)
        isInitialized = true
        // Load cached data initially
        loadCachedData()
        // Trigger initial data refresh only if account exists
        if (repository.getAccount() != null) {
            // 使用本地存储的账号创建一个基本的卡片信息对象显示
            createLocalCardInfo()
            // 然后再尝试刷新远程数据
            refreshAllData()
        } else {
            // If no account, indicate login is needed
            _uiState.update { it.copy(isLoading = false, requiresLogin = true, errorMessage = "请先登录校园卡系统") }
        }
    }

    // 新增：从本地账号创建基本卡片信息
    private fun createLocalCardInfo() {
        val account = repository.getAccount()
        if (account != null) {
            val cardInfo = CardInfo(
                cardNumber = account,
                balance = 0.0, // 无法获知余额，先设为0
                status = "正常",
                expiryDate = "" // 无过期日期信息
            )
            _uiState.update { it.copy(cardInfo = cardInfo) }
        }
    }

    // --- Credential Management ---
    // Removed saveCredentials and getUsername functions as login is handled by WebView

    // --- Data Fetching Logic ---
    private fun loadCachedData() {
        viewModelScope.launch {
            val cachedTransactions = repository.getCachedTransactions()
            _uiState.update {
                it.copy(transactions = cachedTransactions)
            }
            
            // 加载缓存数据后立即处理图表数据
            if (cachedTransactions.isNotEmpty()) {
                processTransactionsForCharts(cachedTransactions)
            }
        }
    }

    // --- Data Refreshing Logic ---
    fun refreshAllData(forceLogin: Boolean = false) { // Keep forceLogin for now, though unused
        if (!isInitialized) {
            Log.w(TAG, "ViewModel not initialized, cannot refresh.")
            return
        }
        val account = repository.getAccount()
        if (account == null) {
             Log.w(TAG, "No account found, triggering login requirement.")
             _uiState.update { it.copy(isLoading = false, requiresLogin = true, errorMessage = "请先登录校园卡系统") }
            return
        }

        currentPage = 1 // Reset pagination
        allTransactionsLoaded = false
        isFetchingTransactions = false
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingProgress = null, loadingProgressText = null, errorMessage = null, requiresLogin = false) }

            // Removed the explicit login block here. Login status is determined by cookie presence
            // and API call success. If calls fail later due to auth, requiresLogin might be set.

            // --- Fetch all data, including looping through all transaction pages ---
            var fetchError: String? = null
            var totalPages = 1 // Assume at least one page initially
            
            // Fetch other data concurrently (optional, can be sequential too)
            // Consider using async/await if you want them to run truly in parallel
            fetchCardInfoInternal()
            fetchCurrentMonthBillInternal()
            fetchCurrentMonthTrendInternal()

            // Fetch first page of transactions to get total pages
            Log.d(TAG, "Refreshing data: Fetching page 1...")
            when (val firstPageResult = repository.fetchTransactions(page = 1)) {
                is CampusCardRepository.Result.Success -> {
                    totalPages = firstPageResult.data.second // Get total pages from the Pair
                    Log.i(TAG, "Page 1 fetched successfully. Total pages: $totalPages")
                    // Repository already saves page 1 data
                     // Set initial progress after page 1 success (if multiple pages)
                     if (totalPages > 1) {
                         val progress = (1 * 100 / totalPages)
                         _uiState.update { it.copy(loadingProgress = progress, loadingProgressText = "正在获取 1 / $totalPages 页...") }
                     } else {
                         // If only 1 page, loading is essentially done after this
                         _uiState.update { it.copy(loadingProgress = 100, loadingProgressText = "正在获取 1 / 1 页...") }
                     }
                }
                is CampusCardRepository.Result.Error -> {
                    Log.e(TAG, "Error fetching page 1: ${firstPageResult.message}")
                    fetchError = "获取第一页交易记录失败: ${firstPageResult.message}"
                    // If page 1 fails, we might not proceed, or rely on cache
                }
            }

            // If page 1 was successful and there are more pages, fetch them sequentially
            if (fetchError == null && totalPages > 1) {
                Log.i(TAG, "Fetching remaining pages from 2 to $totalPages...")
                for (page in 2..totalPages) {
                     Log.d(TAG, "Fetching page $page of $totalPages...")
                     // Update progress state
                     val progress = (page * 100 / totalPages)
                     _uiState.update { it.copy(loadingProgress = progress, loadingProgressText = "正在获取 $page / $totalPages 页...") }
                     when (val pageResult = repository.fetchTransactions(page = page)) {
                         is CampusCardRepository.Result.Success -> {
                             // Data is saved by repository
                             Log.d(TAG, "Page $page fetched successfully.")
                             // Add a small delay to avoid overwhelming the server
                             kotlinx.coroutines.delay(1) // 300ms delay between requests
                         }
                         is CampusCardRepository.Result.Error -> {
                             Log.e(TAG, "Error fetching page $page: ${pageResult.message}")
                             fetchError = "获取第 $page 页交易记录失败: ${pageResult.message}"
                             // Optional: Break the loop on error?
                             // break 
                         }
                     }
                     if (fetchError != null) break // Stop if an error occurred in the loop
                }
            }
            // --- End of fetching all data ---

            // After fetching all pages (or if an error occurred), reload the full list from DB
             Log.i(TAG, "Reloading all transactions from database after refresh cycle.")
             val allCachedTransactions = repository.getCachedTransactions()
             _uiState.update {
                 it.copy(
                     isLoading = false, // Finished loading
                     loadingProgress = null, // Clear progress
                     loadingProgressText = null, // Clear progress text
                     transactions = allCachedTransactions,
                     errorMessage = fetchError // Show the last error, if any
                 )
             }
             Log.d(TAG, "Updated UI with ${allCachedTransactions.size} transactions from DB.")
             // Process data for charts after transactions are updated
             processTransactionsForCharts(allCachedTransactions)
        }
    }
    
    // Method to update card info directly from login activity data
    fun updateCardInfoDirectly(cardNumber: String, balance: Double) {
        if (cardNumber.isNotBlank()) {
            val cardInfo = CardInfo(
                cardNumber = cardNumber,
                balance = balance,
                expiryDate = "",  // We don't have this yet
                status = "正常"    // Assume normal status
            )
            
            _uiState.update { it.copy(cardInfo = cardInfo) }
        }
    }

    private suspend fun fetchCardInfoInternal() {
        when (val result = repository.fetchCardInfo()) {
            is CampusCardRepository.Result.Success<CardInfo> -> _uiState.update { it.copy(cardInfo = result.data) }
            is CampusCardRepository.Result.Error -> {
                // 如果当前没有cardInfo，则创建一个本地的
                if (_uiState.value.cardInfo == null) {
                    createLocalCardInfo()
                }
                _uiState.update { 
                // Only update the error message, don't clear cardInfo if it exists
                it.copy(errorMessage = "获取卡片信息失败: ${result.message}") 
                }
            }
        }
    }

    private suspend fun fetchCurrentMonthBillInternal() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        
        when (val result = repository.fetchMonthBillSummary(year, month)) {
            is CampusCardRepository.Result.Success<MonthBill> -> _uiState.update { it.copy(monthBill = result.data) }
            is CampusCardRepository.Result.Error -> _uiState.update { 
                // Only update the error message, don't clear monthBill if it exists
                it.copy(errorMessage = "获取月账单失败: ${result.message}")
            }
        }
    }

    private suspend fun fetchCurrentMonthTrendInternal() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        
        when (val result = repository.fetchConsumeTrend(year, month)) {
            is CampusCardRepository.Result.Success<ConsumeTrend> -> _uiState.update { it.copy(consumeTrend = result.data) }
            is CampusCardRepository.Result.Error -> _uiState.update { 
                // Only update the error message, don't clear trend data if it exists
                it.copy(errorMessage = "获取消费趋势失败: ${result.message}")
            }
        }
    }

    // --- Transaction Pagination Logic ---
    fun fetchMoreTransactions() {
        if (isFetchingTransactions || allTransactionsLoaded) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingMore = true) }
            fetchTransactionsInternal(currentPage + 1, replaceExisting = false)
            _uiState.update { it.copy(isFetchingMore = false) }
        }
    }
    
    private suspend fun fetchTransactionsInternal(page: Int, replaceExisting: Boolean) {
        // This function is now primarily used by fetchMoreTransactions
        // The main refresh logic handles fetching multiple pages directly in refreshAllData
        if (replaceExisting) {
             Log.w(TAG, "fetchTransactionsInternal called with replaceExisting=true. This should normally be handled by refreshAllData.")
             // Fallback: Call refreshAllData if this happens unexpectedly?
             // Or just fetch page 1 and reload from DB as originally intended for a single page fetch.
             refreshAllData()
             return
        }
        
        isFetchingTransactions = true
        Log.d(TAG, "Fetching more transactions: page $page")
        
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        
        when (val result = repository.fetchTransactions(page = page)) {
            is CampusCardRepository.Result.Success -> {
                // Result data is now Pair<List<CampusCardTransaction>, Int>
                // The repository now saves the data automatically.
                val newlyFetchedTransactions = result.data.first // Access the list from the pair
                // val totalPages = result.data.second // Use this if needed for pagination logic

                currentPage = page
                Log.d(TAG, "Fetched page $page with ${newlyFetchedTransactions.size} transactions. Data saved by repository.")
                
                if (newlyFetchedTransactions.isEmpty() && page > 1) { // Check if empty on subsequent pages
                    allTransactionsLoaded = true
                    Log.d(TAG, "All transactions loaded.")
                                                            } else {
                        if (replaceExisting) {
                        // After refresh (fetching page 1), reload the *entire* list from DB
                        // This ensures UI shows the complete, updated list including newly saved items.
                         val allCachedTransactions = repository.getCachedTransactions()
                         _uiState.update { it.copy(transactions = allCachedTransactions) }
                         Log.d(TAG, "Reloaded ${allCachedTransactions.size} transactions from DB after refresh.")
                                            } else {
                        // When fetching more (page > 1), append only the newly fetched items to the UI
                        _uiState.update { currentState ->
                            val currentTransactions = currentState.transactions
                            val existingIds = currentTransactions.map { tx -> tx.id }.toSet()
                            val uniqueNewTransactions = newlyFetchedTransactions.filterNot { tx -> existingIds.contains(tx.id) }
                            currentState.copy(transactions = currentTransactions + uniqueNewTransactions)
                        }
                    }
                    // Update allTransactionsLoaded logic if totalPages is reliable
                    // if (page >= totalPages) { allTransactionsLoaded = true }
                }
            }
            is CampusCardRepository.Result.Error -> {
                Log.e(TAG, "Error fetching transactions page $page: ${result.message}")
                // Check if the error suggests an authentication issue
                if (result.message.contains("登录", ignoreCase = true) || result.message.contains("权限", ignoreCase = true)) {
                     _uiState.update {
                         it.copy(
                             isFetchingMore = false, // Stop pagination loading indicator
                             errorMessage = "获取交易记录失败: ${result.message}",
                             requiresLogin = true // Indicate login might be needed
                         )
                     }
                 } else {
                     _uiState.update {
                         it.copy(
                            isFetchingMore = false, // Stop pagination loading indicator
                            errorMessage = "获取交易记录失败: ${result.message}"
                         )
                     }
                 }
            }
        }
        
        isFetchingTransactions = false
    }

    // --- Chart Data Processing ---
    private fun processTransactionsForCharts(transactions: List<CampusCardTransaction>) {
        viewModelScope.launch(Dispatchers.Default) { // Use Default dispatcher for processing
            if (transactions.isEmpty()) {
                _uiState.update { it.copy(
                    dailySpendingData = null,
                    monthlySpendingData = null,
                    categorySpendingData = null,
                    merchantTopSpendingData = null,
                    dayOfWeekSpendingData = null,
                    hourOfDaySpendingData = null,
                    transactionTypeData = null,
                    monthlyIncomeExpenseData = null,
                    balanceTrendData = null,
                    spendingRangeData = null,
                    predefinedCategoryData = null,
                    avgDayOfWeekSpendingData = null,
                    monthlyTransactionCountData = null,
                    tranNameDistributionData = null
                ) }
                return@launch
            }

            // === 1. 原有的每日消费数据处理 ===
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val startDate = calendar.time

            // Define both possible date formats
            val dateFormatWithSeconds = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateFormatWithoutSeconds = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // For grouping by day

            val dailyTotals = transactions
                .mapNotNull { tx ->
                    var date: Date? = null
                    try {
                        // Try parsing with seconds first
                        date = dateFormatWithSeconds.parse(tx.time)
                    } catch (e1: ParseException) {
                        try {
                            // If that fails, try parsing without seconds
                            date = dateFormatWithoutSeconds.parse(tx.time)
                        } catch (e2: ParseException) {
                            // If both fail, log the error
                            Log.e(TAG, "Error parsing date with both formats: ${tx.time}", e2)
                        }
                    }

                    if (date != null && date.after(startDate) && date.before(endDate) && tx.amount < 0) {
                        dayFormat.format(date) to -tx.amount // Group by day string, use positive amount for spending
                    } else {
                        null // Return null if date parsing failed or conditions not met
                    }
                }
                .groupBy { it.first } // Group by yyyy-MM-dd string
                .mapValues { entry -> entry.value.sumOf { it.second } } // Sum amounts for each day
                .toList() // Convert Map to List<Pair<String, Double>>
                .sortedBy { it.first } // Sort by date string

            // Convert to Vico ChartEntry list (needs Float values)
            // We might use day index (0-29) or day of month as X axis
            val chartEntries = dailyTotals.mapIndexedNotNull { index, pair ->
                try {
                    // Use entryOf helper function
                    entryOf(index.toFloat(), pair.second.toFloat())
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating chart entry for ${pair.first}", e)
                    null
                }
            }

            Log.d(TAG, "Processed ${chartEntries.size} entries for daily spending chart.")
            
            // === 2. 开始处理多样化图表数据 ===
            
            // 先对所有交易进行排序（按时间先后）用于某些分析
            val sortedTransactions = transactions.sortedBy { tx -> 
                try { 
                    dateFormatWithSeconds.parse(tx.time)?.time ?: 0L
                } catch (e: Exception) {
                    try {
                        dateFormatWithoutSeconds.parse(tx.time)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }
            }

            // === 收集各种统计数据 ===
            val monthlySummary = mutableMapOf<String, Pair<Double, Double>>() // 月份 -> (收入, 支出)
            val monthlyCategories = mutableMapOf<String, MutableMap<String, Double>>() // 月份 -> (商户 -> 金额)
            
            val overallStats = object {
                var totalIncome = 0.0
                var totalExpense = 0.0
                var incomeCount = 0
                var expenseCount = 0
                val merchantSpending = mutableMapOf<String, Double>() // 商户 -> 总支出
                val predefinedCategorySpending = mutableMapOf<String, Double>() // 预定义分类 -> 总支出
                val dayOfWeekSpending = DoubleArray(7) // 周一到周日的支出
                val hourOfDaySpending = DoubleArray(24) // 0-23小时的支出
                val spendingRanges = mutableMapOf(
                    "0-10" to 0,
                    "10-50" to 0,
                    "50-100" to 0,
                    "100-500" to 0,
                    "500+" to 0
                )
                val avgDayOfWeekSpending = Array(7) { 
                    object { var total = 0.0; var count = 0 }
                }
                val tranNameCounts = mutableMapOf<String, Int>() // 交易名称 -> 次数
            }
            
            val monthlyCount = mutableMapOf<String, Int>() // 月份 -> 交易次数
            val balanceTrend = mutableListOf<Pair<Long, Double>>() // 时间戳 -> 余额

            // 初始化预定义分类
            PREDEFINED_CATEGORIES.keys.forEach { category ->
                overallStats.predefinedCategorySpending[category] = 0.0
            }

            // === 处理每个交易记录 ===
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            transactions.forEach { tx ->
                try {
                    var date: Date? = null
                    try {
                        date = dateFormatWithSeconds.parse(tx.time)
                    } catch (e: Exception) {
                        try {
                            date = dateFormatWithoutSeconds.parse(tx.time)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing date for transaction: ${tx.time}", e)
                            return@forEach
                        }
                    }

                    if (date == null) return@forEach

                    val cal = Calendar.getInstance()
                    cal.time = date
                    
                    val month = monthFormat.format(date)
                    val amount = tx.amount
                    val merchantName = tx.location.trim()
                    val weekday = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday in Calendar, but we want 0=Monday
                    val weekdayAdjusted = (weekday + 6) % 7 // Convert to 0=Monday, 6=Sunday
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val balance = tx.balance
                    val tranName = tx.type.trim()

                    // 按月汇总
                    if (!monthlySummary.containsKey(month)) {
                        monthlySummary[month] = Pair(0.0, 0.0)
                    }
                    
                    // 记录月度交易次数
                    monthlyCount[month] = (monthlyCount[month] ?: 0) + 1
                    
                    // 处理收入和支出
                    if (amount > 0) {
                        // 收入
                        monthlySummary[month] = monthlySummary[month]!!.let { 
                            Pair(it.first + amount, it.second) 
                        }
                        overallStats.totalIncome += amount
                        overallStats.incomeCount++
                    } else if (amount < 0) {
                        // 支出 (使用绝对值)
                        val absAmount = Math.abs(amount)
                        monthlySummary[month] = monthlySummary[month]!!.let { 
                            Pair(it.first, it.second + absAmount) 
                        }
                        overallStats.totalExpense += absAmount
                        overallStats.expenseCount++
                        
                        // 按星期分析支出
                        if (weekdayAdjusted in 0..6) {
                            overallStats.dayOfWeekSpending[weekdayAdjusted] += absAmount
                            
                            // 月份检查 (排除假期月)
                            val monthNumber = cal.get(Calendar.MONTH) + 1
                            val isHolidayMonth = monthNumber in listOf(1, 2, 7, 8)
                            
                            if (!isHolidayMonth) {
                                overallStats.avgDayOfWeekSpending[weekdayAdjusted].total += absAmount
                                overallStats.avgDayOfWeekSpending[weekdayAdjusted].count++
                            }
                        }
                        
                        // 按小时分析支出
                        if (hour in 0..23) {
                            overallStats.hourOfDaySpending[hour] += absAmount
                        }
                        
                        // 按商户统计月度支出
                        if (!monthlyCategories.containsKey(month)) {
                            monthlyCategories[month] = mutableMapOf()
                        }
                        monthlyCategories[month]!![merchantName] = 
                            (monthlyCategories[month]!![merchantName] ?: 0.0) + absAmount
                        
                        // 按商户统计总支出
                        overallStats.merchantSpending[merchantName] = 
                            (overallStats.merchantSpending[merchantName] ?: 0.0) + absAmount
                        
                        // 按预定义分类统计支出
                        val predefinedCategory = mapMerchantToPredefinedCategory(merchantName)
                        overallStats.predefinedCategorySpending[predefinedCategory] = 
                            (overallStats.predefinedCategorySpending[predefinedCategory] ?: 0.0) + absAmount
                            
                        // 按支出金额范围统计
                        when {
                            absAmount < 10 -> overallStats.spendingRanges["0-10"] = overallStats.spendingRanges["0-10"]!! + 1
                            absAmount < 50 -> overallStats.spendingRanges["10-50"] = overallStats.spendingRanges["10-50"]!! + 1
                            absAmount < 100 -> overallStats.spendingRanges["50-100"] = overallStats.spendingRanges["50-100"]!! + 1
                            absAmount < 500 -> overallStats.spendingRanges["100-500"] = overallStats.spendingRanges["100-500"]!! + 1
                            else -> overallStats.spendingRanges["500+"] = overallStats.spendingRanges["500+"]!! + 1
                        }
                        
                        // 按交易名称统计
                        overallStats.tranNameCounts[tranName] = (overallStats.tranNameCounts[tranName] ?: 0) + 1
                    }
                    
                    // 记录余额变化趋势
                    balanceTrend.add(Pair(date.time, balance))
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing transaction: $e", e)
                }
            }
            
            // === 计算平均每日支出 ===
            val avgDayOfWeekSpending = overallStats.avgDayOfWeekSpending.map { day ->
                if (day.count > 0) day.total / day.count else 0.0
            }
            
            // === 转换数据为UI可用格式 ===
            
            // 1. 商户Top10支出
            val topMerchants = overallStats.merchantSpending.entries
                .map { Pair(it.key, it.value.toFloat()) }
            
            // 2. 预定义分类支出 (过滤掉金额为0的分类)
            val predefinedCategories = overallStats.predefinedCategorySpending.entries
                .filter { it.value > 0.001 }
                .map { Pair(it.key, it.value.toFloat()) }
            
            // 3. 日期分布支出
            val dayOfWeekLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
            val dayOfWeekSpending = overallStats.dayOfWeekSpending.mapIndexed { index, value ->
                Pair(dayOfWeekLabels[index], value.toFloat())
            }
            
            // 4. 小时分布支出
            val hourOfDaySpending = overallStats.hourOfDaySpending.mapIndexed { index, value ->
                Pair("${String.format("%02d", index)}:00", value.toFloat())
            }
            
            // 5. 交易类型分布 (收入/支出次数)
            val transactionTypeData = listOf(
                Pair("消费", overallStats.expenseCount.toFloat()),
                Pair("收入/充值", overallStats.incomeCount.toFloat())
            )
            
            // 6. 月度收支趋势
            val monthlyIncomeExpense = monthlySummary.entries
                .sortedBy { it.key }
                .map { Triple(it.key, it.value.first.toFloat(), it.value.second.toFloat()) }
            
            // 7. 余额变化趋势（排序并转换为ChartEntry）
            // 首先按照日期分组余额数据，只保留每天的最后一笔交易记录的余额
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dailyBalances = balanceTrend
                .groupBy { pair -> 
                    dateFormat.format(Date(pair.first))
                }
                .mapValues { (_, values) -> 
                    // 取每天最后一笔交易的余额
                    values.maxByOrNull { it.first }?.second ?: 0.0
                }
            
            // 将日期字符串转回为日期对象并排序
            val sortedDailyBalances = dailyBalances.entries
                .map { (dateStr, balance) ->
                    val date = dateFormat.parse(dateStr)
                    Pair(date.time, balance)
                }
                .sortedBy { it.first }

            // 计算相对天数 - 修改这里以计算 'daysAgo'
            // val firstDate = sortedDailyBalances.firstOrNull()?.first ?: 0L 
            val lastDate = sortedDailyBalances.lastOrNull()?.first ?: System.currentTimeMillis() // Get the timestamp of the latest balance entry
            val oneDayInMillis = 24 * 60 * 60 * 1000L

            val balanceTrendEntries = sortedDailyBalances.map { (timestamp, balance) ->
                // 计算与最后一天的天数差 (daysAgo)
                val daysAgo = (lastDate - timestamp) / oneDayInMillis.toFloat()
                // X 轴值现在是 daysAgo (0 for latest, positive for older)
                entryOf(daysAgo, balance.toFloat())
            }.sortedBy { it.x } // Ensure entries are sorted by 'daysAgo'
            
            // 8. 支出金额分布
            val spendingRangeData = overallStats.spendingRanges.entries
                .map { Pair(it.key, it.value.toFloat()) }
            
            // 9. 按星期的平均支出
            val avgDayOfWeekData = avgDayOfWeekSpending.mapIndexed { index, value ->
                Pair(dayOfWeekLabels[index], value.toFloat())
            }
            
            // 10. 月度交易次数
            val monthlyTransactionCount = monthlyCount.entries
                .sortedBy { it.key }
                .map { Pair(it.key, it.value.toFloat()) }
            
            // 11. 交易名称分布
            val tranNameDistribution = overallStats.tranNameCounts.entries
                .sortedByDescending { it.value }
                .take(10) // 取前10个最常见的交易名称
                .map { Pair(it.key, it.value.toFloat()) }
                
            // 12. 最近一个月的每个商户支出（用于月度商户支出饼图）
            val recentMonthMerchantSpending = mutableMapOf<String, Double>()
            val latestMonth = monthlySummary.keys.maxOrNull()
            if (latestMonth != null && monthlyCategories.containsKey(latestMonth)) {
                recentMonthMerchantSpending.putAll(monthlyCategories[latestMonth]!!)
            }
            val categorySpendingData = recentMonthMerchantSpending.entries
                .map { Pair(it.key, it.value.toFloat()) }
                .sortedByDescending { it.second }
            
            // 更新UI状态
            _uiState.update { currentState ->
                currentState.copy(
                    dailySpendingData = chartEntries,
                    monthlySpendingData = monthlyIncomeExpense,
                    categorySpendingData = categorySpendingData,
                    merchantTopSpendingData = topMerchants,
                    dayOfWeekSpendingData = dayOfWeekSpending,
                    hourOfDaySpendingData = hourOfDaySpending,
                    transactionTypeData = transactionTypeData,
                    monthlyIncomeExpenseData = monthlyIncomeExpense,
                    balanceTrendData = balanceTrendEntries,
                    spendingRangeData = spendingRangeData,
                    predefinedCategoryData = predefinedCategories,
                    avgDayOfWeekSpendingData = avgDayOfWeekData,
                    monthlyTransactionCountData = monthlyTransactionCount,
                    tranNameDistributionData = tranNameDistribution
                )
            }
            
            Log.d(TAG, "所有图表数据处理完成，共有 ${transactions.size} 条交易记录")
        }
    }

    // 将商户名称映射到预定义分类
    private fun mapMerchantToPredefinedCategory(merchantName: String?): String {
        if (merchantName.isNullOrBlank()) return "其他"
        val name = merchantName.trim().lowercase()

        // 特殊商户名称精确匹配处理
        if (name == "同创洗浴热水" || (name.contains("同创") && name.contains("洗浴"))) {
            return "洗浴"
        }
        if (name == "同创直饮水" || (name.contains("同创") && name.contains("饮水"))) {
            return "水"
        }

        for ((category, keywords) in PREDEFINED_CATEGORIES) {
            if (category == "其他") continue
            for (keyword in keywords) {
                if (name.contains(keyword.lowercase())) {
                    return category
                }
            }
        }
        return "其他"
    }

    // Helper function to cancel login attempt
    fun cancelLogin() {
        _uiState.update { it.copy(isLoading = false, requiresLogin = false, errorMessage = "登录已取消") }
    }

    // --- Add the new function to clear cache --- 
    fun clearCache() {
        if (!isInitialized) return
        viewModelScope.launch(Dispatchers.IO) { // Use IO dispatcher for DB operations
            try {
                Log.i(TAG, "Clearing cached campus card transactions...")
                repository.clearAllTransactions()
                Log.i(TAG, "Cache cleared successfully.")

                // Update UI state after clearing
                 _uiState.update {
                     it.copy(
                         transactions = emptyList(), // Clear transactions list
                         monthBill = null, // Clear related summaries
                         consumeTrend = null,
                         dailySpendingData = null, // Clear chart data
                         monthlyIncomeExpenseData = null,
                         predefinedCategoryData = null,
                         merchantTopSpendingData = null,
                         dayOfWeekSpendingData = null,
                         hourOfDaySpendingData = null,
                         transactionTypeData = null,
                         spendingRangeData = null,
                         balanceTrendData = null,
                         errorMessage = null, // Clear any previous errors
                         // Keep cardInfo and requiresLogin status as they are not cache
                     )
                 }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
                 _uiState.update { it.copy(errorMessage = "清除缓存失败: ${e.message}") }
            }
        }
    }

    // Helper function to format Double to 2 decimal places
    fun Double.format(digits: Int) = "%.${digits}f".format(this)
}

// --- UI State Data Class ---
data class CampusCardUiState(
    val isLoading: Boolean = true,
    val isFetchingMore: Boolean = false, // For pagination indicator
    val loadingProgress: Int? = null, // Progress for multi-page loading (0-100)
    val loadingProgressText: String? = null, // Text for multi-page loading (e.g., "Fetching 5/50 pages...")
    val errorMessage: String? = null,
    val cardInfo: CardInfo? = null,
    val monthBill: MonthBill? = null,
    val consumeTrend: ConsumeTrend? = null,
    val transactions: List<CampusCardTransaction> = emptyList(),
    val requiresLogin: Boolean = false, // Flag to potentially show a login screen

    // --- Chart Data ---
    val dailySpendingData: List<ChartEntry>? = null, // 原有的每日支出图表
    
    // === 新增的图表数据字段 ===
    val monthlySpendingData: List<Triple<String, Float, Float>>? = null, // 月份, 收入, 支出
    val categorySpendingData: List<Pair<String, Float>>? = null, // 商户-金额（月度商户分类）
    val merchantTopSpendingData: List<Pair<String, Float>>? = null, // Top10商户支出
    val dayOfWeekSpendingData: List<Pair<String, Float>>? = null, // 星期几-支出金额
    val hourOfDaySpendingData: List<Pair<String, Float>>? = null, // 小时-支出金额
    val transactionTypeData: List<Pair<String, Float>>? = null, // 交易类型-次数
    val monthlyIncomeExpenseData: List<Triple<String, Float, Float>>? = null, // 月份, 收入, 支出
    val balanceTrendData: List<ChartEntry>? = null, // 余额变化趋势
    val spendingRangeData: List<Pair<String, Float>>? = null, // 消费金额范围-次数
    val predefinedCategoryData: List<Pair<String, Float>>? = null, // 预定义分类-金额
    val avgDayOfWeekSpendingData: List<Pair<String, Float>>? = null, // 星期几-平均支出
    val monthlyTransactionCountData: List<Pair<String, Float>>? = null, // 月份-交易次数
    val tranNameDistributionData: List<Pair<String, Float>>? = null // 交易名称-次数
)

// Removed duplicate data class definitions below 