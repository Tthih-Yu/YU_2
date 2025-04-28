package com.tthih.yu.electricity

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tthih.yu.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.util.Log
import android.widget.Toast
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import android.provider.DocumentsContract
import android.os.Build
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.NumberPicker
import android.app.DatePickerDialog
import android.widget.ArrayAdapter

class ElectricityHistoryActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var monthYearText: TextView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton
    private lateinit var tvMonthlyUsage: TextView
    private lateinit var tvMonthlyRecharge: TextView
    
    private val calendar = Calendar.getInstance()
    private var historyData: Map<Int, ElectricityHistoryData> = mapOf()
    private var previousMonthData: Map<Int, ElectricityHistoryData> = mapOf()
    
    private val repository by lazy {
        ElectricityRepository(this)
    }
    
    // 导出CSV的ActivityResult处理器
    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val startDate = result.data?.getLongExtra("start_date", 0L) ?: 0L
                val endDate = result.data?.getLongExtra("end_date", Long.MAX_VALUE) ?: Long.MAX_VALUE
                exportHistoryCsv(uri, startDate, endDate)
            }
        }
    }
    
    // 导出JSON的ActivityResult处理器
    private val exportJsonLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val startDate = result.data?.getLongExtra("start_date", 0L) ?: 0L
                val endDate = result.data?.getLongExtra("end_date", Long.MAX_VALUE) ?: Long.MAX_VALUE
                exportHistoryJson(uri, startDate, endDate)
            }
        }
    }
    
    // 导入JSON的ActivityResult处理器
    private val importJsonLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importHistoryJson(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置状态栏为透明
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        setContentView(R.layout.activity_electricity_history)
        
        // 初始化视图
        initViews()
        
        // 设置当前月份
        updateMonthYearText()
        
        // 首次加载时强制刷新一次电费数据，确保有当天的记录
        lifecycleScope.launch {
            try {
                val electricityData = repository.getElectricityData()
                if (electricityData != null) {
                    // 检测电量变化并记录
                    val changed = repository.checkAndRecordElectricityChange(electricityData)
                    
                    if (changed) {
                        Log.d("ElectricityHistory", "检测到电费变化并记录到历史")
                    }
                }
            } catch (e: Exception) {
                Log.e("ElectricityHistory", "启动时刷新电费数据失败: ${e.message}", e)
            }
            
            // 加载当月数据
            loadMonthData()
        }
        
        // 设置月份切换监听
        setupMonthChangeListeners()
        
        // 设置返回按钮
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }
        
        // 设置导出按钮点击事件
        val btnExport = findViewById<ImageButton>(R.id.btn_export)
        btnExport.setOnClickListener {
            showExportDialog()
        }
        
        // 设置导入按钮点击事件
        val btnImport = findViewById<ImageButton>(R.id.btn_import)
        btnImport.setOnClickListener {
            showImportDialog()
        }
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.calendar_recycler_view)
        monthYearText = findViewById(R.id.tv_month_year)
        prevMonthButton = findViewById(R.id.btn_previous_month)
        nextMonthButton = findViewById(R.id.btn_next_month)
        tvMonthlyUsage = findViewById(R.id.tv_monthly_usage)
        tvMonthlyRecharge = findViewById(R.id.tv_monthly_recharge)
        
        // 设置RecyclerView为网格布局，7列
        recyclerView.layoutManager = GridLayoutManager(this, 7)
    }
    
    private fun updateMonthYearText() {
        val format = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        monthYearText.text = format.format(calendar.time)
    }
    
    private fun setupMonthChangeListeners() {
        prevMonthButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateMonthYearText()
            loadMonthData()
        }
        
        nextMonthButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateMonthYearText()
            loadMonthData()
        }
    }
    
    private fun loadMonthData() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        
        Log.d("ElectricityHistory", "开始加载月度数据: $year-$month")
        
        lifecycleScope.launch {
            try {
                // 从数据库获取本月历史数据
                Log.d("ElectricityHistory", "从数据库请求历史数据...")
                val monthlyHistoryData = repository.getMonthHistory(year, month)
                
                // 记录获取结果
                Log.d("ElectricityHistory", "获取到${monthlyHistoryData.size}条历史记录")
                
                // 按日期分组
                val groupedByDay = monthlyHistoryData.groupBy { it.getDayOfMonth() }
                
                // 转换为按天索引的Map，每天使用最新的一条记录
                val historyMap = mutableMapOf<Int, ElectricityHistoryData>()
                var totalUsage = 0.0
                var totalRecharge = 0.0
                
                // 处理每一天的数据
                for ((day, records) in groupedByDay) {
                    // 计算该天的用电量和充值金额总和
                    val dailyUsage = records.sumOf { it.usage }
                    val dailyRecharge = records.sumOf { it.recharge }
                    
                    // 使用该天最新的记录
                    val latestRecord = records.maxByOrNull { it.id }
                    if (latestRecord != null) {
                        // 创建包含日总用电量和充值金额的记录
                        val enhancedRecord = ElectricityHistoryData(
                            id = latestRecord.id,
                            date = latestRecord.date,
                            balance = latestRecord.balance,
                            building = latestRecord.building,
                            roomId = latestRecord.roomId,
                            usage = dailyUsage,
                            recharge = dailyRecharge
                        )
                        
                        historyMap[day] = enhancedRecord
                        
                        totalUsage += dailyUsage
                        totalRecharge += dailyRecharge
                        
                        // 详细记录每天的数据
                        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(latestRecord.date)
                        Log.d("ElectricityHistory", "日期: $formattedDate (${day}日), 用电: $dailyUsage, 充值: $dailyRecharge, 余额: ${latestRecord.balance}")
                        
                        // 检查是否有数据变化
                        if (dailyUsage > 0 || dailyRecharge > 0) {
                            Log.d("ElectricityHistory", "日期 $day 有数据变化 - 用电: $dailyUsage, 充值: $dailyRecharge")
                        }
                    }
                }
                
                historyData = historyMap
                
                // 获取上个月最后一天的数据
                val prevYear = if (month == 1) year - 1 else year
                val prevMonth = if (month == 1) 12 else month - 1
                
                Log.d("ElectricityHistory", "获取上个月($prevYear-$prevMonth)数据...")
                val prevMonthData = repository.getMonthHistory(prevYear, prevMonth)
                Log.d("ElectricityHistory", "获取到${prevMonthData.size}条上月记录")
                
                if (prevMonthData.isNotEmpty()) {
                    // 按日期分组
                    val prevMonthGrouped = prevMonthData.groupBy { it.getDayOfMonth() }
                    
                    // 找出上个月最后一天
                    val lastDay = prevMonthGrouped.keys.maxOrNull()
                    if (lastDay != null) {
                        // 获取该天最新的记录
                        val lastDayRecords = prevMonthGrouped[lastDay]!!
                        val lastDayLatestRecord = lastDayRecords.maxByOrNull { it.id }
                        
                        if (lastDayLatestRecord != null) {
                            // 计算该天总用电量和充值金额
                            val dailyUsage = lastDayRecords.sumOf { it.usage }
                            val dailyRecharge = lastDayRecords.sumOf { it.recharge }
                            
                            // 创建包含总用电量和充值金额的记录
                            val enhancedLastDayRecord = ElectricityHistoryData(
                                id = lastDayLatestRecord.id,
                                date = lastDayLatestRecord.date,
                                balance = lastDayLatestRecord.balance,
                                building = lastDayLatestRecord.building,
                                roomId = lastDayLatestRecord.roomId,
                                usage = dailyUsage,
                                recharge = dailyRecharge
                            )
                            
                            previousMonthData = mapOf(lastDay to enhancedLastDayRecord)
                            Log.d("ElectricityHistory", "上月最后一天: $lastDay, 用电: $dailyUsage, 充值: $dailyRecharge, 余额: ${lastDayLatestRecord.balance}")
                        }
                    }
                }
                
                // 更新月度总结信息 - 直接使用总用电金额
                updateMonthlySummary(totalUsage, totalRecharge)
                
                // 更新日历适配器
                updateCalendarAdapter()
                
                Log.d("ElectricityHistory", "日历数据加载完成，共有${historyMap.size}天的数据")
                Log.d("ElectricityHistory", "月度统计: 总用电金额=${totalUsage}元, 总充值=${totalRecharge}元")
                
            } catch (e: Exception) {
                Log.e("ElectricityHistory", "加载历史数据失败: ${e.message}", e)
                // 如果发生错误，显示空的数据
                historyData = mapOf()
                previousMonthData = mapOf()
                updateMonthlySummary(0.0, 0.0)
                updateCalendarAdapter()
                
                // 显示错误消息
                Toast.makeText(this@ElectricityHistoryActivity, "加载历史数据失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateMonthlySummary(totalUsageAmount: Double, totalRecharge: Double) {
        tvMonthlyUsage.text = String.format("%.2f元", totalUsageAmount)
        tvMonthlyRecharge.text = String.format("¥%.2f", totalRecharge)
    }
    
    private suspend fun saveHistoryDataToDatabase(dataList: List<ElectricityHistoryData>) {
        try {
            val historyDao = ElectricityDatabase.getDatabase(this).electricityHistoryDao()
            
            for (data in dataList) {
                // 查询同一天的历史记录
                val calendar = Calendar.getInstance()
                calendar.time = data.date
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                
                val todayRecords = repository.getHistoryByDate(year, month, day)
                var finalData = data
                
                // 如果同一天已有记录，检查是否需要合并变化数据
                if (todayRecords.isNotEmpty()) {
                    // 获取今天已有的用电和充值总和
                    val existingUsage = todayRecords.sumOf { it.usage }
                    val existingRecharge = todayRecords.sumOf { it.recharge }
                    
                    // 如果新数据有变化信息，与已有数据合并
                    if (data.usage > 0 || data.recharge > 0) {
                        finalData = ElectricityHistoryData(
                            id = data.id,
                            date = data.date,
                            balance = data.balance,
                            building = data.building,
                            roomId = data.roomId,
                            usage = existingUsage + data.usage,
                            recharge = existingRecharge + data.recharge
                        )
                        
                        Log.d("ElectricityHistory", "合并同一天的变化数据: 用电 ${existingUsage} + ${data.usage} = ${finalData.usage}, " +
                                "充值 ${existingRecharge} + ${data.recharge} = ${finalData.recharge}")
                    } else {
                        // 如果新数据没有变化，保留已有的变化数据
                        finalData = ElectricityHistoryData(
                            id = data.id,
                            date = data.date,
                            balance = data.balance,
                            building = data.building,
                            roomId = data.roomId,
                            usage = existingUsage,
                            recharge = existingRecharge
                        )
                        
                        Log.d("ElectricityHistory", "保留同一天的已有变化数据: 用电 ${existingUsage}, 充值 ${existingRecharge}")
                    }
                }
                
                // 保存到数据库
                historyDao.insertHistory(finalData)
            }
            
            Log.d("ElectricityHistory", "成功保存${dataList.size}条历史记录到数据库")
        } catch (e: Exception) {
            Log.e("ElectricityHistory", "保存历史数据到数据库失败: ${e.message}", e)
        }
    }
    
    private fun updateCalendarAdapter() {
        // 获取当月第一天是星期几
        val firstDayCalendar = calendar.clone() as Calendar
        firstDayCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = firstDayCalendar.get(Calendar.DAY_OF_WEEK) - 1
        
        // 获取当月天数
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // 创建适配器
        val adapter = CalendarDayAdapter(
            this,
            daysInMonth,
            firstDayOfWeek,
            historyData,
            previousMonthData,
            calendar
        )
        
        recyclerView.adapter = adapter
    }
    
    /**
     * 显示导出选项对话框
     */
    private fun showExportDialog() {
        val options = arrayOf("导出为CSV", "导出为JSON")
        val rangeOptions = arrayOf("当前月份", "指定月份", "指定年份", "指定日期", "全部数据")
        
        // 创建自定义适配器
        val adapter = ArrayAdapter(this, R.layout.dialog_item, rangeOptions)
        
        // 先选择导出范围
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("选择导出范围")
            .setAdapter(adapter) { _, rangeIndex ->
                when (rangeIndex) {
                    0 -> { // 当前月份
                        showFormatDialog(options, getCurrentMonthRange())
                    }
                    1 -> { // 指定月份
                        showMonthYearPicker { year, month ->
                            showFormatDialog(options, getMonthRange(year, month))
                        }
                    }
                    2 -> { // 指定年份
                        showYearPicker { year ->
                            showFormatDialog(options, getYearRange(year))
                        }
                    }
                    3 -> { // 指定日期
                        showDatePicker { year, month, day ->
                            showFormatDialog(options, getDateRange(year, month, day))
                        }
                    }
                    4 -> { // 全部数据
                        showFormatDialog(options, getAllDataRange())
                    }
                }
            }
            .show()
    }
    
    /**
     * 显示格式选择对话框
     */
    private fun showFormatDialog(options: Array<String>, dateRange: Pair<Long, Long>) {
        // 创建自定义适配器
        val adapter = ArrayAdapter(this, R.layout.dialog_item, options)
        
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("选择导出格式")
            .setAdapter(adapter) { _, formatIndex ->
                when (formatIndex) {
                    0 -> createCsvFile(dateRange)
                    1 -> createJsonFile(dateRange)
                }
            }
            .show()
    }
    
    /**
     * 显示月份和年份选择器
     */
    private fun showMonthYearPicker(onDateSelected: (year: Int, month: Int) -> Unit) {
        val yearMonthView = layoutInflater.inflate(R.layout.dialog_year_month_picker, null)
        val monthPicker = yearMonthView.findViewById<NumberPicker>(R.id.month_picker)
        val yearPicker = yearMonthView.findViewById<NumberPicker>(R.id.year_picker)
        
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = calendar.get(Calendar.MONTH) + 1
        
        val currentYear = calendar.get(Calendar.YEAR)
        yearPicker.minValue = 2020
        yearPicker.maxValue = currentYear
        yearPicker.value = currentYear
        
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("选择月份")
            .setView(yearMonthView)
            .setPositiveButton("确定") { _, _ ->
                onDateSelected(yearPicker.value, monthPicker.value)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示年份选择器
     */
    private fun showYearPicker(onYearSelected: (year: Int) -> Unit) {
        val yearView = layoutInflater.inflate(R.layout.dialog_year_picker, null)
        val yearPicker = yearView.findViewById<NumberPicker>(R.id.year_picker)
        
        val currentYear = calendar.get(Calendar.YEAR)
        yearPicker.minValue = 2020
        yearPicker.maxValue = currentYear
        yearPicker.value = currentYear
        
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("选择年份")
            .setView(yearView)
            .setPositiveButton("确定") { _, _ ->
                onYearSelected(yearPicker.value)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示日期选择器
     */
    private fun showDatePicker(onDateSelected: (year: Int, month: Int, day: Int) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.CustomDatePickerDialog,
            { _, year, month, dayOfMonth ->
                onDateSelected(year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    /**
     * 获取当前月份的日期范围
     */
    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return getMonthRange(year, month)
    }
    
    /**
     * 获取指定月份的日期范围
     */
    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val startCalendar = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val endCalendar = Calendar.getInstance().apply {
            set(year, month - 1, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        return Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
    }
    
    /**
     * 获取指定年份的日期范围
     */
    private fun getYearRange(year: Int): Pair<Long, Long> {
        val startCalendar = Calendar.getInstance().apply {
            set(year, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val endCalendar = Calendar.getInstance().apply {
            set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        return Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
    }
    
    /**
     * 获取指定日期的日期范围
     */
    private fun getDateRange(year: Int, month: Int, day: Int): Pair<Long, Long> {
        val startCalendar = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val endCalendar = Calendar.getInstance().apply {
            set(year, month - 1, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        return Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
    }
    
    /**
     * 获取全部数据的日期范围
     */
    private fun getAllDataRange(): Pair<Long, Long> {
        return Pair(0L, Long.MAX_VALUE)
    }
    
    /**
     * 创建CSV文件并触发导出
     */
    private fun createCsvFile(dateRange: Pair<Long, Long> = Pair(0L, Long.MAX_VALUE)) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            
            // 建议的文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            putExtra(Intent.EXTRA_TITLE, "electricity_history_$timestamp.csv")
            
            // 初始位置（可选）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"))
            }
        }
        
        // 保存日期范围到Intent中
        intent.putExtra("start_date", dateRange.first)
        intent.putExtra("end_date", dateRange.second)
        
        exportCsvLauncher.launch(intent)
    }
    
    /**
     * 创建JSON文件并触发导出
     */
    private fun createJsonFile(dateRange: Pair<Long, Long> = Pair(0L, Long.MAX_VALUE)) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            
            // 建议的文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            putExtra(Intent.EXTRA_TITLE, "electricity_history_$timestamp.json")
            
            // 初始位置（可选）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"))
            }
        }
        
        // 保存日期范围到Intent中
        intent.putExtra("start_date", dateRange.first)
        intent.putExtra("end_date", dateRange.second)
        
        exportJsonLauncher.launch(intent)
    }
    
    /**
     * 导出历史数据为CSV
     */
    private fun exportHistoryCsv(uri: Uri, startDate: Long, endDate: Long) {
        lifecycleScope.launch {
            // 根据日期范围过滤数据
            val allHistoryData = repository.getAllHistoryData()
            val filteredData = allHistoryData.filter { data ->
                val timestamp = data.date.time
                timestamp in startDate..endDate
            }
            
            val success = ElectricityDataExportImport.exportToCsv(
                filteredData,
                uri,
                contentResolver
            )
            if (success) {
                Toast.makeText(this@ElectricityHistoryActivity, "导出CSV成功，共${filteredData.size}条记录", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ElectricityHistoryActivity, "导出CSV失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 导出历史数据为JSON
     */
    private fun exportHistoryJson(uri: Uri, startDate: Long, endDate: Long) {
        lifecycleScope.launch {
            // 根据日期范围过滤数据
            val allHistoryData = repository.getAllHistoryData()
            val filteredData = allHistoryData.filter { data ->
                val timestamp = data.date.time
                timestamp in startDate..endDate
            }
            
            val success = ElectricityDataExportImport.exportToJson(
                filteredData,
                uri,
                contentResolver
            )
            if (success) {
                Toast.makeText(this@ElectricityHistoryActivity, "导出JSON成功，共${filteredData.size}条记录", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ElectricityHistoryActivity, "导出JSON失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 从JSON导入历史数据
     */
    private fun importHistoryJson(uri: Uri) {
        lifecycleScope.launch {
            try {
                // 解析JSON数据
                val importedData = ElectricityDataExportImport.importFromJson(uri, contentResolver)
                
                if (importedData.isEmpty()) {
                    Toast.makeText(this@ElectricityHistoryActivity, "未能从JSON导入任何数据", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // 获取当前宿舍楼和房间号
                val building = repository.getCurrentBuilding()
                val roomId = repository.getRoomId()
                
                // 更新导入数据的宿舍楼和房间号
                val updatedData = importedData.map { data ->
                    data.copy(building = building, roomId = roomId)
                }
                
                // 显示确认对话框
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@ElectricityHistoryActivity, R.style.CustomAlertDialog)
                        .setTitle("导入确认")
                        .setMessage("确定要导入${updatedData.size}条历史数据吗？这可能会覆盖相同日期的现有数据。将使用当前宿舍楼: $building, 房间号: $roomId")
                        .setPositiveButton("确定") { _, _ ->
                            lifecycleScope.launch {
                                try {
                                    // 保存到数据库
                                    saveHistoryDataToDatabase(updatedData)
                                    
                                    // 刷新显示
                                    loadMonthData()
                                    
                                    Toast.makeText(this@ElectricityHistoryActivity, "成功导入${updatedData.size}条历史数据", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("ElectricityHistory", "导入数据保存异常: ${e.message}", e)
                                    Toast.makeText(this@ElectricityHistoryActivity, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                
            } catch (e: Exception) {
                Log.e("ElectricityHistory", "导入JSON异常: ${e.message}", e)
                Toast.makeText(this@ElectricityHistoryActivity, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 显示导入选项对话框
     */
    private fun showImportDialog() {
        val options = arrayOf("从JSON导入")
        
        // 创建自定义适配器
        val adapter = ArrayAdapter(this, R.layout.dialog_item, options)
        
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle("选择导入来源")
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> openJsonFile()
                }
            }
            .show()
    }
    
    /**
     * 打开JSON文件并触发导入
     */
    private fun openJsonFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        
        importJsonLauncher.launch(intent)
    }
} 