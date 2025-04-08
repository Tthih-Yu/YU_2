package com.tthih.yu.about

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tthih.yu.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class CheckUpdateActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var currentVersionTextView: TextView
    private lateinit var refreshIcon: ImageView
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var updateButton: Button
    private lateinit var lastCheckedTextView: TextView
    
    // 控制更新状态
    private var isChecking = false
    private var updateAvailable = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_update)
        
        // 初始化视图
        initViews()
        
        // 设置当前版本
        setCurrentVersion()
        
        // 设置点击事件
        setupClickListeners()
    }
    
    private fun initViews() {
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "检查更新"
        
        // 初始化其他视图
        currentVersionTextView = findViewById(R.id.tv_current_version)
        refreshIcon = findViewById(R.id.iv_refresh)
        statusTextView = findViewById(R.id.tv_status)
        progressBar = findViewById(R.id.progress_bar)
        updateButton = findViewById(R.id.btn_update)
        lastCheckedTextView = findViewById(R.id.tv_last_checked)
        
        // 初始状态
        progressBar.visibility = View.GONE
        updateButton.visibility = View.GONE
    }
    
    private fun setCurrentVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            currentVersionTextView.text = "当前版本: $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            currentVersionTextView.text = "当前版本: 未知"
        }
    }
    
    private fun setupClickListeners() {
        // 刷新图标点击事件
        refreshIcon.setOnClickListener {
            if (!isChecking) {
                checkForUpdates()
            }
        }
        
        // 更新按钮点击事件
        updateButton.setOnClickListener {
            if (updateAvailable) {
                openDownloadLink()
            }
        }
    }
    
    private fun checkForUpdates() {
        // 标记为正在检查
        isChecking = true
        
        // 显示进度条并隐藏其他元素
        progressBar.visibility = View.VISIBLE
        statusTextView.text = "正在检查更新..."
        updateButton.visibility = View.GONE
        
        // 开始旋转刷新图标
        val rotateAnimation = ObjectAnimator.ofFloat(refreshIcon, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        rotateAnimation.start()
        
        // 模拟检查更新过程
        Handler(Looper.getMainLooper()).postDelayed({
            // 结束刷新状态
            isChecking = false
            rotateAnimation.end()
            progressBar.visibility = View.GONE
            
            // 根据随机结果决定是否有更新
            updateAvailable = Random().nextBoolean()
            
            if (updateAvailable) {
                statusTextView.text = "发现新版本! 1.2.0"
                updateButton.visibility = View.VISIBLE
                updateButton.text = "立即更新"
            } else {
                statusTextView.text = "已是最新版本"
                updateButton.visibility = View.GONE
            }
            
            // 更新最后检查时间
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            lastCheckedTextView.text = "上次检查: $currentDate"
            
        }, 2000) // 模拟2秒的检查时间
    }
    
    private fun openDownloadLink() {
        // 打开下载链接或应用市场
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://yucampus.app/download")
            startActivity(intent)
        } catch (e: Exception) {
            statusTextView.text = "无法打开下载页面，请重试"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 