package com.tthih.yu.electricity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.tthih.yu.R
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import com.google.android.material.button.MaterialButton
import android.app.AlertDialog
import android.app.PendingIntent
import androidx.appcompat.widget.SwitchCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.DialogInterface

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var viewModel: ElectricityViewModel
    
    // UI组件
    private lateinit var etJsessionId: EditText
    private lateinit var buildingSelector: LinearLayout
    private lateinit var tvSelectedBuilding: TextView
    private lateinit var etRoomId: EditText
    private lateinit var seekBarRefreshInterval: SeekBar
    private lateinit var tvRefreshInterval: TextView
    private lateinit var seekBarLowBalanceThreshold: SeekBar
    private lateinit var tvLowBalanceThreshold: TextView
    private lateinit var btnClearHistory: Button
    private lateinit var btnBack: ImageButton
    private lateinit var btnAddWidget: Button
    private lateinit var switchScheduledRefresh: SwitchCompat
    private lateinit var tvScheduledRefreshDesc: TextView
    private lateinit var fabSaveSettings: FloatingActionButton
    
    // 当前选中的宿舍楼
    private var currentBuilding = ""
    
    // 宿舍楼列表（按分类）
    private val buildingsByCategory = mapOf(
        "男生宿舍" to listOf(
            "男25#楼", "男22#楼", "男21#楼", "男20#楼", "男19#楼", 
            "男18#楼", "男17#楼", "男16#楼", "男15#楼", "男14#楼", 
            "男13#楼",
            "男12#楼", "男11#楼", "男06#楼", "男05#楼"
        ),
        "女生宿舍" to listOf(
            "女13#楼", "女12#楼", "女11#楼", "女10#楼", "女09#楼", 
            "女08#楼", "女07#楼", "女06#楼", "女05#楼", "女04#楼", 
            "女03#楼", "女02#楼", "女01#楼"
        ),
        "研究生宿舍" to listOf(
            "研05#楼", "研04#楼", "研03#楼", "研02#楼", "研01#楼"
        ),
        "梦溪宿舍" to listOf(
            "梦溪7-1栋", "梦溪7-2栋", "梦溪7-3栋", "梦溪7-4栋", 
            "梦溪7-5栋", "梦溪7-6栋", "梦溪7-7栋", "梦溪7-8栋", 
            "梦溪7-9-A栋", "梦溪7-9-B栋", "梦溪7-9-C栋"
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // 初始化ViewModel
        viewModel = ViewModelProvider(this).get(ElectricityViewModel::class.java)
        
        // 初始化视图
        initViews()
        
        // 加载当前设置
        loadCurrentSettings()
    }
    
    private fun initViews() {
        etJsessionId = findViewById(R.id.et_jsession_id)
        buildingSelector = findViewById(R.id.building_selector)
        tvSelectedBuilding = findViewById(R.id.tv_selected_building)
        etRoomId = findViewById(R.id.et_room_id)
        seekBarRefreshInterval = findViewById(R.id.seek_bar_refresh_interval)
        tvRefreshInterval = findViewById(R.id.tv_refresh_interval)
        seekBarLowBalanceThreshold = findViewById(R.id.seek_bar_low_balance_threshold)
        tvLowBalanceThreshold = findViewById(R.id.tv_low_balance_threshold)
        btnClearHistory = findViewById(R.id.btn_clear_history)
        btnBack = findViewById(R.id.btn_back)
        btnAddWidget = findViewById(R.id.btn_add_widget)
        switchScheduledRefresh = findViewById(R.id.switch_scheduled_refresh)
        tvScheduledRefreshDesc = findViewById(R.id.tv_scheduled_refresh_desc)
        fabSaveSettings = findViewById(R.id.fab_save_settings)
        
        // 设置返回按钮
        btnBack.setOnClickListener {
            finish()
        }
        
        // 设置宿舍楼选择器点击事件
        buildingSelector.setOnClickListener {
            showBuildingSelectorDialog()
        }
        
        // 设置刷新间隔滑块
        seekBarRefreshInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val interval = progress + 1 // 最小1分钟
                tvRefreshInterval.text = "${interval}分钟"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 不做任何操作
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 不做任何操作
            }
        })
        
        // 设置低余额警告阈值滑块
        seekBarLowBalanceThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val threshold = progress.toFloat()
                tvLowBalanceThreshold.text = "${threshold}元"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 不做任何操作
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 不做任何操作
            }
        })
        
        // 清除历史数据按钮点击事件
        btnClearHistory.setOnClickListener {
            // 显示确认对话框，而不是直接清除
            showClearHistoryConfirmationDialog()
        }
        
        // 为新的悬浮按钮设置点击事件
        fabSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        // 添加桌面小部件按钮点击事件
        btnAddWidget.setOnClickListener {
            navigateToWidgetSelection()
        }
        
        // 设置开关状态变化监听
        switchScheduledRefresh.setOnCheckedChangeListener { _, isChecked ->
            tvScheduledRefreshDesc.text = if (isChecked) {
                "每天晚上23:50和早上0:0:1自动查询电量"
            } else {
                "关闭自动定时查询电量"
            }
        }
    }
    
    private fun showBuildingSelectorDialog() {
        val dialog = BuildingSelectorDialog(this, buildingsByCategory) { buildingName ->
            currentBuilding = buildingName
            tvSelectedBuilding.text = buildingName
        }
        dialog.show()
    }
    
    private fun loadCurrentSettings() {
        // 加载JSESSIONID
        etJsessionId.setText(viewModel.getJsessionId())
        
        // 加载宿舍楼设置
        currentBuilding = viewModel.getCurrentBuilding()
        tvSelectedBuilding.text = currentBuilding
        
        // 加载房间号
        etRoomId.setText(viewModel.getRoomId())
        
        // 加载刷新间隔
        val refreshInterval = viewModel.getRefreshInterval()
        seekBarRefreshInterval.progress = refreshInterval - 1
        tvRefreshInterval.text = "${refreshInterval}分钟"
        
        // 加载低余额警告阈值
        val lowBalanceThreshold = viewModel.getLowBalanceThreshold()
        seekBarLowBalanceThreshold.progress = lowBalanceThreshold.toInt()
        tvLowBalanceThreshold.text = "${lowBalanceThreshold}元"
        
        // 加载定时刷新设置
        val isScheduledRefreshEnabled = viewModel.isScheduledRefreshEnabled()
        switchScheduledRefresh.isChecked = isScheduledRefreshEnabled
        tvScheduledRefreshDesc.text = if (isScheduledRefreshEnabled) {
            "每天晚上23:50和早上0:0:1自动查询电量"
        } else {
            "关闭自动定时查询电量"
        }
    }
    
    private fun saveSettings() {
        // 保存JSESSIONID
        val jsessionId = etJsessionId.text.toString().trim()
        viewModel.updateJsessionId(jsessionId)
        
        // 保存宿舍楼设置
        viewModel.updateBuilding(currentBuilding)
        
        // 保存房间号
        val roomId = etRoomId.text.toString().trim()
        viewModel.updateRoom(roomId)
        
        // 保存刷新间隔
        val refreshInterval = seekBarRefreshInterval.progress + 1
        viewModel.updateRefreshInterval(refreshInterval)
        
        // 保存低余额警告阈值
        val lowBalanceThreshold = seekBarLowBalanceThreshold.progress.toFloat()
        viewModel.updateLowBalanceThreshold(lowBalanceThreshold)
        
        // 保存定时刷新设置
        val isScheduledRefreshEnabled = switchScheduledRefresh.isChecked
        viewModel.updateScheduledRefreshEnabled(isScheduledRefreshEnabled)
        
        // 根据设置启用或禁用定时任务
        if (isScheduledRefreshEnabled) {
            // 重启定时任务以确保设置生效
            WorkManager.getInstance(this).cancelUniqueWork("electricity_daily_work")
            val dailyWorkRequest = PeriodicWorkRequestBuilder<ElectricityScheduledWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "electricity_daily_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest
            )
            Toast.makeText(this, "已启用定时刷新电量", Toast.LENGTH_SHORT).show()
        } else {
            // 取消定时任务
            WorkManager.getInstance(this).cancelUniqueWork("electricity_daily_work")
            Toast.makeText(this, "已关闭定时刷新电量", Toast.LENGTH_SHORT).show()
        }
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    /**
     * 显示清除历史数据确认对话框
     */
    private fun showClearHistoryConfirmationDialog() {
        // Show the dialog and then get the buttons to set colors manually
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog) // 使用自定义样式
            .setTitle("确认清除历史数据？")
            .setMessage("清除操作不可逆，建议在清除前导出json数据备份。\n\n确定要清除所有历史记录吗？")
            .setIcon(R.drawable.ic_warning) // 添加警告图标
            .setPositiveButton("确定清除") { d: DialogInterface, w: Int ->
                // 用户确认清除
                viewModel.clearHistoryData()
                Toast.makeText(this, "历史数据已清除", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("取消") { d: DialogInterface, w: Int ->
                // 用户取消
                d.dismiss()
            }
            .show() // Show the dialog

        // Manually set button text colors after showing the dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.matcha_primary))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.matcha_text_secondary))
    }
    
    /**
     * 引导用户添加桌面小部件
     */
    private fun navigateToWidgetSelection() {
        // 针对Android 8.0及以上设备，直接使用requestPinAppWidget
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val myProvider = ComponentName(this, ElectricityWidgetProvider::class.java)
            
            try {
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    // 创建可选的PendingIntent用于配置
                    val successCallback = PendingIntent.getBroadcast(
                        this, 
                        0,
                        Intent(this, ElectricityWidgetProvider::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    // 请求添加小部件
                    appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                    Toast.makeText(this, "请在桌面上选择位置放置小部件", Toast.LENGTH_LONG).show()
                    return
                }
            } catch (e: Exception) {
                // 如果失败，继续尝试其他方法
            }
        }
        
        // 对于不支持自动添加的设备，打开添加小部件的说明
        showAddWidgetOptionsDialog()
    }
    
    /**
     * 显示添加小部件的选项对话框
     */
    private fun showAddWidgetOptionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("添加小部件")
            .setMessage("您的设备不支持自动添加小部件，请选择以下操作：")
            .setPositiveButton("查看添加指南") { dialog, _ -> 
                dialog.dismiss()
                showWidgetAddGuide()
            }
            .setNegativeButton("返回桌面") { dialog, _ ->
                dialog.dismiss()
                // 跳转到桌面
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                startActivity(homeIntent)
                // 显示提示
                Toast.makeText(this, "请在桌面长按添加小部件", Toast.LENGTH_LONG).show()
            }
            .show()
    }
    
    /**
     * 显示详细的添加小部件指南对话框
     */
    private fun showWidgetAddGuide() {
        val message = """
            请按照以下步骤手动添加小部件:
            
            1. 返回到手机桌面
            2. 长按桌面空白处
            3. 在弹出的菜单中选择"小部件"选项
            4. 在小部件列表中找到"电费小部件"
            5. 长按并拖动到桌面合适位置
            
            不同手机品牌的操作可能略有不同
            
            * 小米手机: 捏合桌面进入编辑模式
            * 华为手机: 双指捏合或长按桌面空白处
            * 三星手机: 长按桌面空白处选择"小部件"
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("如何添加桌面小部件")
            .setMessage(message)
            .setPositiveButton("我知道了") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}