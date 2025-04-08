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
                    // 创建一条新的历史记录
                    val historyData = ElectricityHistoryData(
                        id = 0,
                        date = Date(),
                        balance = electricityData.balance.toDouble(),
                        building = electricityData.building,
                        roomId = electricityData.roomId,
                        usage = 0.0,
                        recharge = 0.0
                    )
                    
                    // 保存到数据库
                    saveHistoryDataToDatabase(listOf(historyData))
                    
                    Log.d("ElectricityHistory", "启动时创建电费记录: 余额=${electricityData.balance}")
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
                
                // 更新月度总结信息
                updateMonthlySummary(totalUsage, totalRecharge)
                
                // 更新日历适配器
                updateCalendarAdapter()
                
                Log.d("ElectricityHistory", "日历数据加载完成，共有${historyMap.size}天的数据")
                
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
    
    private fun updateMonthlySummary(totalUsage: Double, totalRecharge: Double) {
        tvMonthlyUsage.text = String.format("%.1f度", totalUsage)
        tvMonthlyRecharge.text = String.format("¥%.2f", totalRecharge)
    }
    
    private suspend fun saveHistoryDataToDatabase(dataList: List<ElectricityHistoryData>) {
        try {
            val historyDao = ElectricityDatabase.getDatabase(this).electricityHistoryDao()
            for (data in dataList) {
                historyDao.insertHistory(data)
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
} 