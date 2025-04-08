package com.tthih.yu.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.tthih.yu.R

class DonateActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var wechatPayCard: CardView
    private lateinit var alipayCard: CardView
    
    // 支付链接 - 实际使用时请替换为您的二维码链接或支付链接
    private val wechatPayUrl = "wxp://f2f0K0CawVCsf_-8DGjlNvKe9DwpJnYgFddcuTfmkkN4y9A" // 微信支付示例链接
    private val alipayUrl = "https://qr.alipay.com/fkx199332ygwmgxllj5ys0d" // 支付宝示例链接
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)
        
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
        supportActionBar?.title = "支持开发"
        
        // 初始化其他视图
        wechatPayCard = findViewById(R.id.card_wechat_pay)
        alipayCard = findViewById(R.id.card_alipay)
    }
    
    private fun setupClickListeners() {
        // 微信支付点击事件
        wechatPayCard.setOnClickListener {
            openWechatPay()
        }
        
        // 支付宝点击事件
        alipayCard.setOnClickListener {
            openAlipay()
        }
    }
    
    // 打开微信支付
    private fun openWechatPay() {
        try {
            // 尝试打开微信支付
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wechatPayUrl))
            intent.setPackage("com.tencent.mm") // 指定微信包名
            startActivity(intent)
        } catch (e: Exception) {
            // 如果没有安装微信或无法打开链接
            Toast.makeText(this, "无法打开微信，请确保已安装微信应用", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 打开支付宝
    private fun openAlipay() {
        try {
            // 尝试打开支付宝
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(alipayUrl))
            intent.setPackage("com.eg.android.AlipayGphone") // 指定支付宝包名
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // 如果无法直接打开支付宝，尝试通过浏览器打开
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(alipayUrl))
                startActivity(webIntent)
            } catch (e2: Exception) {
                Toast.makeText(this, "无法打开支付宝，请确保已安装支付宝应用或浏览器", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 