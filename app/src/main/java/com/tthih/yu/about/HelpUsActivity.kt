package com.tthih.yu.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.tthih.yu.R

class HelpUsActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var feedbackCard: CardView
    private lateinit var shareCard: CardView
    private lateinit var donateCard: CardView
    private lateinit var rateButton: MaterialButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_us)
        
        // 初始化视图
        initViews()
        
        // 设置点击事件
        setupClickListeners()
        
        // 应用动画效果
        applyAnimations()
    }
    
    private fun initViews() {
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "帮助我们"
        
        // 设置导航图标颜色为橙色
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        
        // 初始化其他视图
        feedbackCard = findViewById(R.id.card_feedback)
        shareCard = findViewById(R.id.card_share)
        donateCard = findViewById(R.id.card_donate)
        rateButton = findViewById(R.id.btn_rate)
    }
    
    private fun setupClickListeners() {
        // 反馈卡片点击事件
        feedbackCard.setOnClickListener {
            animateCard(it)
            // 跳转到邮件联系页面
            val intent = Intent(this, EmailContactActivity::class.java)
            startActivity(intent)
        }
        
        // 分享卡片点击事件
        shareCard.setOnClickListener {
            animateCard(it)
            shareApp()
        }
        
        // 捐赠卡片点击事件
        donateCard.setOnClickListener {
            animateCard(it)
            // 跳转到捐赠页面
            val intent = Intent(this, DonateActivity::class.java)
            startActivity(intent)
        }
        
        // 评分按钮点击事件
        rateButton.setOnClickListener {
            // 应用按钮动画
            val buttonAnimation = AnimationUtils.loadAnimation(this, R.anim.btn_scale)
            it.startAnimation(buttonAnimation)
            
            rateApp()
        }
    }
    
    private fun animateCard(view: View) {
        val cardAnimation = AnimationUtils.loadAnimation(this, R.anim.btn_scale)
        view.startAnimation(cardAnimation)
    }
    
    private fun applyAnimations() {
        // 获取卡片动画
        val cardSlideUp = AnimationUtils.loadAnimation(this, R.anim.card_slide_up)
        
        // 应用延迟动画到每个卡片
        feedbackCard.visibility = View.VISIBLE
        feedbackCard.startAnimation(cardSlideUp)
        
        val shareCardAnim = AnimationUtils.loadAnimation(this, R.anim.card_slide_up)
        shareCardAnim.startOffset = 150
        shareCard.visibility = View.VISIBLE
        shareCard.startAnimation(shareCardAnim)
        
        val donateCardAnim = AnimationUtils.loadAnimation(this, R.anim.card_slide_up)
        donateCardAnim.startOffset = 300
        donateCard.visibility = View.VISIBLE
        donateCard.startAnimation(donateCardAnim)
        
        val buttonAnim = AnimationUtils.loadAnimation(this, R.anim.card_slide_up)
        buttonAnim.startOffset = 450
        rateButton.visibility = View.VISIBLE
        rateButton.startAnimation(buttonAnim)
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