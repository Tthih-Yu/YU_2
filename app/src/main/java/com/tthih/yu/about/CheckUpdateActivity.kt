package com.tthih.yu.about

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.tthih.yu.R
import com.tthih.yu.util.NetworkUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckUpdateActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var currentVersionTextView: TextView
    private lateinit var refreshIcon: ImageView
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var updateButton: Button
    private lateinit var lastCheckedTextView: TextView
    private lateinit var updateContentTextView: TextView
    
    // 控制更新状态
    private var isChecking = false
    private var updateAvailable = false
    private var downloadID: Long = -1
    private var currentVersionName: String = ""
    private var currentVersionCode: Int = 0
    private var latestUpdateInfo: UpdateInfo? = null
    
    // 下载完成的广播接收器
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                if (latestUpdateInfo != null) {
                    installAPK()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_update)
        
        // 注册下载完成广播接收器
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        
        // 初始化视图
        initViews()
        
        // 设置当前版本
        setCurrentVersion()
        
        // 设置点击事件
        setupClickListeners()
        
        // 自动检查更新
        checkForUpdates()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 取消注册广播接收器
        unregisterReceiver(onDownloadComplete)
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
        updateContentTextView = findViewById(R.id.tv_update_content)
        
        // 初始状态
        progressBar.visibility = View.GONE
        updateButton.visibility = View.GONE
        updateContentTextView.visibility = View.GONE
    }
    
    private fun setCurrentVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            currentVersionName = packageInfo.versionName ?: "1.0.0"
            currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            currentVersionTextView.text = "当前版本: $currentVersionName"
        } catch (e: PackageManager.NameNotFoundException) {
            currentVersionTextView.text = "当前版本: 未知"
            currentVersionCode = 0
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
            if (updateAvailable && latestUpdateInfo != null) {
                if (latestUpdateInfo!!.forceUpdate) {
                    downloadAndInstallUpdate(latestUpdateInfo!!)
                } else {
                    showUpdateConfirmDialog(latestUpdateInfo!!)
                }
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
        updateContentTextView.visibility = View.GONE
        
        // 开始旋转刷新图标
        val rotateAnimation = ObjectAnimator.ofFloat(refreshIcon, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        rotateAnimation.start()
        
        // 使用NetworkUtil检查更新
        NetworkUtil.checkForUpdates(currentVersionCode) { hasUpdate, updateInfo ->
            // 结束刷新状态
            isChecking = false
            rotateAnimation.end()
            progressBar.visibility = View.GONE
            
            // 更新状态
            updateAvailable = hasUpdate
            latestUpdateInfo = updateInfo
            
            if (hasUpdate && updateInfo != null) {
                statusTextView.text = "发现新版本! ${updateInfo.versionName}"
                updateButton.visibility = View.VISIBLE
                updateButton.text = if (updateInfo.forceUpdate) "强制更新" else "立即更新"
                
                // 显示更新内容
                updateContentTextView.visibility = View.VISIBLE
                updateContentTextView.text = "更新内容:\n${updateInfo.updateContent}"
                
                // 如果是强制更新，显示提示对话框
                if (updateInfo.forceUpdate) {
                    showForceUpdateDialog(updateInfo)
                }
            } else {
                statusTextView.text = "已是最新版本"
                updateButton.visibility = View.GONE
                updateContentTextView.visibility = View.GONE
            }
            
            // 更新最后检查时间
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            lastCheckedTextView.text = "上次检查: $currentDate"
        }
    }
    
    private fun showUpdateConfirmDialog(updateInfo: UpdateInfo) {
        AlertDialog.Builder(this)
            .setTitle("发现新版本")
            .setMessage("当前版本: $currentVersionName\n最新版本: ${updateInfo.versionName}\n\n${updateInfo.updateContent}")
            .setPositiveButton("立即更新") { _, _ ->
                downloadAndInstallUpdate(updateInfo)
            }
            .setNegativeButton("稍后再说", null)
            .show()
    }
    
    private fun showForceUpdateDialog(updateInfo: UpdateInfo) {
        AlertDialog.Builder(this)
            .setTitle("强制更新")
            .setMessage("当前版本: $currentVersionName\n最新版本: ${updateInfo.versionName}\n\n此更新为强制更新，请立即更新以继续使用应用。\n\n${updateInfo.updateContent}")
            .setPositiveButton("立即更新") { _, _ ->
                downloadAndInstallUpdate(updateInfo)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun downloadAndInstallUpdate(updateInfo: UpdateInfo) {
        try {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(updateInfo.downloadUrl)
            val request = DownloadManager.Request(uri)
                .setTitle("YU校园助手更新")
                .setDescription("正在下载 ${updateInfo.versionName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "YU_${updateInfo.versionName}.apk")
                .setMimeType("application/vnd.android.package-archive")
            
            statusTextView.text = "正在下载更新..."
            updateButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
            
            downloadID = downloadManager.enqueue(request)
            Toast.makeText(this, "开始下载更新", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            updateButton.isEnabled = true
            progressBar.visibility = View.GONE
        }
    }
    
    private fun installAPK() {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "YU_${latestUpdateInfo?.versionName}.apk"
            )
            
            if (!file.exists()) {
                Toast.makeText(this, "安装文件不存在", Toast.LENGTH_SHORT).show()
                return
            }
            
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".provider",
                    file
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                uri = Uri.fromFile(file)
            }
            
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            // 如果是强制更新，安装后退出应用
            if (latestUpdateInfo?.forceUpdate == true) {
                finish()
            }
            
            updateButton.isEnabled = true
            progressBar.visibility = View.GONE
            statusTextView.text = "下载完成，请安装"
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "安装失败: ${e.message}", Toast.LENGTH_SHORT).show()
            updateButton.isEnabled = true
            progressBar.visibility = View.GONE
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 