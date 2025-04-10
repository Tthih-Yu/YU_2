package com.tthih.yu.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.tthih.yu.R

class WechatOfficialActivity : AppCompatActivity() {

    private lateinit var ivQrCode: ImageView
    private lateinit var tvWechatId: TextView
    private lateinit var btnCopyId: MaterialButton
    private var cardQrcode: CardView? = null
    private var cardFeatures: CardView? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var scrollView: NestedScrollView
    
    private val TAG = "WechatOfficialActivity"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_wechat_official)
            
            // 找到进度条和错误文本
            progressBar = findViewById(R.id.progress_bar)
            tvError = findViewById(R.id.tv_error)
            scrollView = findViewById(R.id.nested_scroll_view)
            
            // 显示加载状态
            showLoading(true)
            
            // 设置工具栏
            setupToolbar()
            
            // 初始化视图 (延迟一点执行，模拟加载过程)
            handler.postDelayed({
                try {
                    initViews()
                    setupClickListeners()
                    applyCardAnimations()
                    showLoading(false)
                } catch (e: Exception) {
                    Log.e(TAG, "Delayed initialization error: ${e.message}", e)
                    showError("初始化失败：${e.message}")
                }
            }, 500)
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            showError("页面加载失败：${e.message}")
        }
    }
    
    private fun showLoading(show: Boolean) {
        try {
            if (show) {
                progressBar.visibility = View.VISIBLE
                scrollView.visibility = View.GONE
                tvError.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
                scrollView.visibility = View.VISIBLE
                tvError.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "showLoading error: ${e.message}", e)
        }
    }
    
    private fun showError(message: String) {
        try {
            progressBar.visibility = View.GONE
            scrollView.visibility = View.GONE
            tvError.visibility = View.VISIBLE
            tvError.text = message
            
            // 添加重试点击事件
            tvError.setOnClickListener {
                recreate() // 重新创建活动
            }
        } catch (e: Exception) {
            Log.e(TAG, "showError error: ${e.message}", e)
            Toast.makeText(this, "页面加载失败，请重试", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupToolbar() {
        try {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "微信公众号"
            toolbar.setNavigationOnClickListener {
                finish()
            }
            
            // 设置导航图标颜色为橙色
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        } catch (e: Exception) {
            Log.e(TAG, "setupToolbar error: ${e.message}", e)
            showError("工具栏初始化失败")
        }
    }
    
    private fun initViews() {
        try {
            // 初始化卡片视图（使用安全的查找方式）
            cardQrcode = findViewById(R.id.card_qrcode)
            cardFeatures = findViewById(R.id.card_features)
            
            // 初始化其他视图
            ivQrCode = findViewById(R.id.iv_qrcode)
            tvWechatId = findViewById(R.id.tv_wechat_id)
            btnCopyId = findViewById(R.id.btn_copy_id)
            
            // 设置微信公众号ID
            val wechatId = "YU校园助手"
            tvWechatId.text = wechatId
        } catch (e: Exception) {
            Log.e(TAG, "initViews error: ${e.message}", e)
            throw e // 向上抛出异常让调用者处理
        }
    }
    
    private fun setupClickListeners() {
        try {
            // 设置复制ID按钮点击事件
            btnCopyId.setOnClickListener {
                try {
                    // 应用按钮动画
                    val buttonAnimation = safelyLoadAnimation(R.anim.btn_scale)
                    buttonAnimation?.let { anim ->
                        it.startAnimation(anim)
                    }
                    
                    val wechatId = tvWechatId.text.toString()
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Wechat Official ID", wechatId)
                    clipboard.setPrimaryClip(clip)
                    
                    // 使用暖橙色的Toast样式
                    val toast = Toast.makeText(this, "公众号ID已复制到剪贴板", Toast.LENGTH_SHORT)
                    try {
                        toast.view?.background?.setTint(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                    } catch (e: Exception) {
                        Log.e(TAG, "Toast style error: ${e.message}", e)
                    }
                    toast.show()
                } catch (e: Exception) {
                    Log.e(TAG, "Copy button click error: ${e.message}", e)
                    Toast.makeText(this, "复制失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupClickListeners error: ${e.message}", e)
            throw e // 向上抛出异常让调用者处理
        }
    }

    private fun applyCardAnimations() {
        try {
            // 安全地应用动画到卡片，防止空指针异常
            cardQrcode?.let { card ->
                card.visibility = View.VISIBLE
                val delayedAnimation = safelyLoadAnimation(R.anim.card_slide_up)
                delayedAnimation?.let { anim ->
                    card.startAnimation(anim)
                }
            }
            
            cardFeatures?.let { card ->
                card.visibility = View.VISIBLE
                val delayedAnimation = safelyLoadAnimation(R.anim.card_slide_up)
                delayedAnimation?.let { anim ->
                    anim.startOffset = 150
                    card.startAnimation(anim)
                }
            }
            
            // 二维码图片单独应用淡入动画
            val fadeIn = safelyLoadAnimation(android.R.anim.fade_in)
            fadeIn?.let { anim ->
                anim.startOffset = 400
                ivQrCode.startAnimation(anim)
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyCardAnimations error: ${e.message}", e)
            throw e // 向上抛出异常让调用者处理
        }
    }
    
    private fun safelyLoadAnimation(animRes: Int): Animation? {
        return try {
            AnimationUtils.loadAnimation(this, animRes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load animation: $animRes", e)
            null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 