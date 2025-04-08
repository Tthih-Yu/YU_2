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
    private lateinit var btnSaveSettings: Button
    private lateinit var btnBack: ImageButton
    private lateinit var btnAddWidget: MaterialButton
    
    // 当前选中的宿舍楼
    private var currentBuilding = ""
    
    // 宿舍楼列表（按分类）
    private val buildingsByCategory = mapOf(
        "男生宿舍" to listOf(
            "男25#楼", "男22#楼", "男21#楼", "男20#楼", "男19#楼", 
            "男18#楼", "男17#楼", "男16#楼", "男15#楼", "男14#楼", 
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
        btnSaveSettings = findViewById(R.id.btn_save_settings)
        btnBack = findViewById(R.id.btn_back)
        btnAddWidget = findViewById(R.id.btn_add_widget)
        
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
            viewModel.clearHistoryData()
            Toast.makeText(this, "历史数据已清除", Toast.LENGTH_SHORT).show()
        }
        
        // 保存设置按钮点击事件
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        // 添加桌面小部件按钮点击事件
        btnAddWidget.setOnClickListener {
            navigateToWidgetSelection()
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
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
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