package com.tthih.yu.electricity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tthih.yu.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ElectricityActivity : AppCompatActivity() {
    
    private lateinit var viewModel: ElectricityViewModel
    
    // UI 组件
    private lateinit var tvBalance: TextView
    private lateinit var tvBuildingInfo: TextView
    private lateinit var tvRoomInfo: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var tvNextUpdate: TextView
    private lateinit var tvDailyUsage: TextView
    private lateinit var tvEstimatedUsage: TextView
    private lateinit var tvWarningMessage: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnViewHistory: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSettings: FloatingActionButton
    private lateinit var tvUsageTrend: TextView
    private lateinit var tvSuggestion: TextView
    
    // 添加广播接收器，用于接收小部件更新通知
    private val widgetUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ElectricityWidgetProvider.ACTION_REFRESH) {
                // 当小部件更新时，刷新Activity的数据
                viewModel.refreshData()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electricity)
        
        // 初始化ViewModel
        viewModel = ViewModelProvider(this).get(ElectricityViewModel::class.java)
        
        // 初始化视图
        initViews()
        
        // 设置观察者
        setupObservers()
        
        // 初始刷新数据
        viewModel.refreshData()
        
        // 注册广播接收器
        val filter = IntentFilter(ElectricityWidgetProvider.ACTION_REFRESH)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(widgetUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(widgetUpdateReceiver, filter)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 取消注册广播接收器
        unregisterReceiver(widgetUpdateReceiver)
    }
    
    private fun initViews() {
        tvBalance = findViewById(R.id.tv_balance)
        tvBuildingInfo = findViewById(R.id.tv_building_info)
        tvRoomInfo = findViewById(R.id.tv_room_info)
        tvLastUpdate = findViewById(R.id.tv_last_update)
        tvNextUpdate = findViewById(R.id.tv_next_update)
        tvDailyUsage = findViewById(R.id.tv_daily_usage)
        tvEstimatedUsage = findViewById(R.id.tv_estimated_usage)
        tvWarningMessage = findViewById(R.id.tv_warning_message)
        btnRefresh = findViewById(R.id.btn_refresh)
        btnViewHistory = findViewById(R.id.btn_view_history)
        progressBar = findViewById(R.id.progress_bar)
        btnSettings = findViewById(R.id.btn_settings)
        tvUsageTrend = findViewById(R.id.tv_usage_trend)
        tvSuggestion = findViewById(R.id.tv_suggestion)
        
        // 刷新按钮点击事件
        btnRefresh.setOnClickListener {
            viewModel.refreshData()
        }
        
        // 历史记录按钮点击事件
        btnViewHistory.setOnClickListener {
            // 先刷新数据再查看历史记录
            progressBar.visibility = View.VISIBLE
            btnViewHistory.isEnabled = false
            
            viewModel.refreshDataWithCallback { success ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnViewHistory.isEnabled = true
                    
                    if (success) {
                        // 直接跳转到历史记录页面，无需传递参数
                        val intent = Intent(this, ElectricityHistoryActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "获取电费数据失败，请稍后再试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        // 设置按钮点击事件
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupObservers() {
        // 观察电费数据
        viewModel.electricityData.observe(this) { data ->
            updateUI(data)
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnRefresh.isEnabled = !isLoading
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
        
        // 观察JSESSIONID状态
        viewModel.isJsessionIdSet.observe(this) { isSet ->
            if (!isSet) {
                tvBalance.text = "未设置JSESSIONID"
                tvWarningMessage.visibility = View.VISIBLE
                tvWarningMessage.text = "未设置JSESSIONID，请先在设置中配置"
                
                // 如果JSESSIONID未设置，自动跳转到设置页面
                Toast.makeText(this, "请先配置JSESSIONID和宿舍信息", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            } else {
                tvWarningMessage.visibility = View.GONE
            }
        }
    }
    
    private fun updateUI(data: ElectricityData) {
        // 更新电费余额信息
        if (data.balance > 0) {
            tvBalance.text = "${data.balance} 元"
            // 当余额低于阈值时，将余额文字颜色设为红色
            if (data.balance < viewModel.getLowBalanceThreshold()) {
                tvBalance.setTextColor(getColor(R.color.danger_red))
            } else {
                // 余额正常，使用明显的颜色（非白色）
                tvBalance.setTextColor(getColor(R.color.safe_green))
            }
        } else {
            tvBalance.text = "未获取到余额"
            tvBalance.setTextColor(getColor(R.color.warning_yellow))
            tvWarningMessage.visibility = View.VISIBLE
            tvWarningMessage.text = "无法获取电费余额，请检查JSESSIONID是否有效"
        }
        
        // 更新宿舍信息
        tvBuildingInfo.text = data.building
        tvRoomInfo.text = data.roomId
        tvLastUpdate.text = data.timestamp
        
        // 计算下次更新时间
        if (data.nextRefresh.isNotEmpty()) {
            tvNextUpdate.text = data.nextRefresh
        } else {
            tvNextUpdate.text = "未知"
        }
        
        // 更新用电统计 - 即使为0也显示估计值
        if (data.dailyUsage > 0) {
            tvDailyUsage.text = String.format("%.2f 元/天", data.dailyUsage)
        } else if (data.balance > 0) {
            // 如果有余额但没有计算出用电量，显示估计值
            tvDailyUsage.text = "约 2.5 元/天 (估计值)"
            tvDailyUsage.setTextColor(getColor(R.color.warning_yellow))
        } else {
            tvDailyUsage.text = "未知"
        }
        
        // 更新用电趋势
        viewModel.getElectricityTrend { trend, percentage ->
            if (trend != null && percentage > 0) {
                val trendText = when (trend) {
                    ElectricityViewModel.UsageTrend.INCREASING -> "↑ 上升"
                    ElectricityViewModel.UsageTrend.DECREASING -> "↓ 下降"
                    ElectricityViewModel.UsageTrend.STABLE -> "→ 稳定"
                }
                
                // 设置趋势信息
                tvUsageTrend.visibility = View.VISIBLE
                tvUsageTrend.text = "$trendText ${String.format("%.1f", percentage)}%"
                
                // 根据趋势设置颜色
                val color = when (trend) {
                    ElectricityViewModel.UsageTrend.INCREASING -> getColor(R.color.usage_increasing)
                    ElectricityViewModel.UsageTrend.DECREASING -> getColor(R.color.usage_decreasing)
                    ElectricityViewModel.UsageTrend.STABLE -> getColor(R.color.usage_stable)
                }
                tvUsageTrend.setTextColor(color)
            } else {
                tvUsageTrend.visibility = View.GONE
            }
        }
        
        // 更新预计可用天数 - 如果为0但有余额，显示估计值
        if (data.estimatedDays > 0) {
            tvEstimatedUsage.text = "${data.estimatedDays} 天"
            
            // 根据剩余天数设置不同的颜色
            val colorResId = when {
                data.estimatedDays <= 3 -> R.color.danger_red
                data.estimatedDays <= 7 -> R.color.warning_yellow
                else -> R.color.safe_green
            }
            tvEstimatedUsage.setTextColor(getColor(colorResId))
        } else if (data.balance > 0) {
            // 有余额但估计天数为0，显示基于余额的估计
            val estimatedDays = (data.balance / 2.5f).toInt()
            tvEstimatedUsage.text = "约 $estimatedDays 天 (估计值)"
            tvEstimatedUsage.setTextColor(getColor(R.color.warning_yellow))
        } else {
            tvEstimatedUsage.text = "未知"
        }
        
        // 添加建议信息
        if (data.estimatedDays <= 3 && data.estimatedDays > 0) {
            tvSuggestion.visibility = View.VISIBLE
            tvSuggestion.text = "电量极低，请立即充值！"
            tvSuggestion.setTextColor(getColor(R.color.danger_red))
        } else if ((data.estimatedDays <= 7 && data.estimatedDays > 0) || 
                  (data.balance > 0 && data.balance < viewModel.getLowBalanceThreshold())) {
            tvSuggestion.visibility = View.VISIBLE
            tvSuggestion.text = "电量偏低，建议近期充值"
            tvSuggestion.setTextColor(getColor(R.color.warning_yellow))
        } else if (data.balance <= 0 && viewModel.isJsessionIdSet.value == true) {
            tvSuggestion.visibility = View.VISIBLE
            tvSuggestion.text = "无法获取电费数据，可能需要重新配置或刷新"
            tvSuggestion.setTextColor(getColor(R.color.warning_yellow))
        } else {
            tvSuggestion.visibility = View.GONE
        }
        
        // 检查是否需要显示余额不足警告
        if (data.balance > 0 && data.balance < viewModel.getLowBalanceThreshold()) {
            tvWarningMessage.visibility = View.VISIBLE
            tvWarningMessage.text = "电费不足，请尽快充值！"
        } else if (viewModel.isJsessionIdSet.value == true && data.balance > 0) {
            tvWarningMessage.visibility = View.GONE
        }
    }
} 