package com.tthih.yu.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.tthih.yu.R

class HelpUsActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var feedbackCard: CardView
    private lateinit var shareCard: CardView
    private lateinit var donateCard: CardView
    private lateinit var rateButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_us)
        
        // 初始化视图
        initViews()
        
        // 设置点击事件
        setupClickListeners()
    }
    
    private fun initViews() {
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "帮助我们"
        
        // 初始化其他视图
        feedbackCard = findViewById(R.id.card_feedback)
        shareCard = findViewById(R.id.card_share)
        donateCard = findViewById(R.id.card_donate)
        rateButton = findViewById(R.id.btn_rate)
    }
    
    private fun setupClickListeners() {
        // 反馈卡片点击事件
        feedbackCard.setOnClickListener {
            // 跳转到邮件联系页面
            val intent = Intent(this, EmailContactActivity::class.java)
            startActivity(intent)
        }
        
        // 分享卡片点击事件
        shareCard.setOnClickListener {
            shareApp()
        }
        
        // 捐赠卡片点击事件
        donateCard.setOnClickListener {
            // 跳转到捐赠页面
            val intent = Intent(this, DonateActivity::class.java)
            startActivity(intent)
        }
        
        // 评分按钮点击事件
        rateButton.setOnClickListener {
            rateApp()
        }
    }
    
    // 分享应用
    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "YU校园助手")
            putExtra(Intent.EXTRA_TEXT, "推荐一款非常好用的校园应用 - YU校园助手，下载链接: https://yucampus.app/download")
        }
        startActivity(Intent.createChooser(shareIntent, "分享应用"))
    }
    
    // 评分应用
    private fun rateApp() {
        try {
            // 尝试打开应用市场
            val marketUri = Uri.parse("market://details?id=$packageName")
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
            startActivity(marketIntent)
        } catch (e: Exception) {
            // 如果没有安装应用市场，则打开网页
            val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 