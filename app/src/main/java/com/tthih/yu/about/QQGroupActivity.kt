package com.tthih.yu.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.tthih.yu.R

class QQGroupActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var qqGroupNumberTextView: TextView
    private lateinit var copyButton: MaterialButton
    private lateinit var qrCodeImageView: ImageView
    private lateinit var btnJoinGroup: MaterialButton
    private lateinit var cardQQGroup: CardView

    private val TAG = "QQGroupActivity"

    // QQ群号，可以根据实际情况修改
    private val qqGroupNumber = "2190638246"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_qq_group)

            // 初始化视图
            initViews()

            // 设置点击事件
            setupClickListeners()
            
            // 应用动画效果
            applyAnimations()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            Toast.makeText(this, "页面加载失败，请重试", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        try {
            // 设置工具栏
            toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "QQ群"
            
            // 设置导航图标颜色为橙色
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.holo_orange_dark))

            // 初始化其他视图
            cardQQGroup = findViewById(R.id.card_qq_group)
            qqGroupNumberTextView = findViewById(R.id.tv_qq_group_number)
            copyButton = findViewById(R.id.btn_copy_number)
            qrCodeImageView = findViewById(R.id.iv_qq_qr_code)
            btnJoinGroup = findViewById(R.id.btn_join_group)

            // 设置QQ群号
            qqGroupNumberTextView.text = qqGroupNumber
        } catch (e: Exception) {
            Log.e(TAG, "initViews error: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        try {
            // 复制按钮点击事件
            copyButton.setOnClickListener {
                try {
                    val buttonAnimation = safelyLoadAnimation(R.anim.btn_scale)
                    buttonAnimation?.let { anim ->
                        it.startAnimation(anim)
                    }
                    
                    copyToClipboard(qqGroupNumber)
                    
                    // 使用暖橙色的Toast样式
                    val toast = Toast.makeText(this, "QQ群号已复制到剪贴板", Toast.LENGTH_SHORT)
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

            // 设置加入群组按钮点击事件
            btnJoinGroup.setOnClickListener {
                try {
                    val buttonAnimation = safelyLoadAnimation(R.anim.btn_scale)
                    buttonAnimation?.let { anim ->
                        it.startAnimation(anim)
                    }
                    
                    try {
                        // 使用新的链接格式
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://res.abeim.cn/api-qq?qq=2190638246")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to open URL: ${e.message}", e)
                        Toast.makeText(this, "无法打开链接，请确保您已安装浏览器", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Join group button click error: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupClickListeners error: ${e.message}", e)
        }
    }
    
    private fun applyAnimations() {
        try {
            // 获取卡片动画
            val cardSlideUp = safelyLoadAnimation(R.anim.card_slide_up)
            
            // 显示卡片并应用动画
            cardQQGroup.visibility = View.VISIBLE
            cardSlideUp?.let { anim ->
                cardQQGroup.startAnimation(anim)
            }
            
            // 二维码图片单独应用淡入动画
            val fadeIn = safelyLoadAnimation(android.R.anim.fade_in)
            fadeIn?.let { anim ->
                anim.startOffset = 400
                qrCodeImageView.startAnimation(anim)
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyAnimations error: ${e.message}", e)
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

    // 复制文本到剪贴板
    private fun copyToClipboard(text: String) {
        try {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("QQ群号", text)
            clipboardManager.setPrimaryClip(clipData)
        } catch (e: Exception) {
            Log.e(TAG, "copyToClipboard error: ${e.message}", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 