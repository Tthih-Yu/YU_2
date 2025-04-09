package com.tthih.yu.about

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tthih.yu.R

class AppIntroActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_intro)
        
        // 初始化视图
        initViews()
        
        // 加载应用介绍内容
        loadAppIntroContent()
    }
    
    private fun initViews() {
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "应用介绍"
        
        // 初始化 WebView
        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = false  // 出于安全考虑禁用 JavaScript
    }
    
    private fun loadAppIntroContent() {
        val introHtml = """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: sans-serif;
                        padding: 16px;
                        line-height: 1.5;
                        color: #333;
                        background-color: #FFF8EE;
                    }
                    .card {
                        background-color: #FFFFFF;
                        border-radius: 16px;
                        padding: 24px;
                        margin-bottom: 24px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    }
                    h1 {
                        font-size: 24px;
                        color: #FF8C00;
                        margin-top: 0;
                    }
                    h2 {
                        font-size: 20px;
                        color: #FF8C00;
                        margin-top: 24px;
                        margin-bottom: 16px;
                    }
                    p {
                        margin-bottom: 16px;
                    }
                    .feature {
                        display: flex;
                        align-items: center;
                        margin-bottom: 16px;
                    }
                    .feature-icon {
                        width: 40px;
                        height: 40px;
                        margin-right: 16px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        border-radius: 50%;
                    }
                    .electricity-icon {
                        background-color: #FFE6E6;
                        color: #FF5252;
                    }
                    .schedule-icon {
                        background-color: #E6F4FF;
                        color: #2196F3;
                    }
                    .card-icon {
                        background-color: #FFF6E0;
                        color: #FFC107;
                    }
                    .version-info {
                        margin-top: 24px;
                        font-size: 14px;
                        color: #666;
                    }
                    .version-item {
                        margin-bottom: 8px;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>应用简介</h1>
                    <p>YU是专为徽程打造的智能工具，致力于提供高效、便捷的日常体验，让生活更轻松。</p>
                </div>
                
                <div class="card">
                    <h1>核心功能</h1>
                    
                    <div class="feature">
                        <div class="feature-icon electricity-icon">%</div>
                        <div>
                            <strong>宿舍电费查询于提醒</strong>
                        </div>
                    </div>
                    
                    <div class="feature">
                        <div class="feature-icon schedule-icon">📅</div>
                        <div>
                            <strong>课程表导入与提醒</strong>
                        </div>
                    </div>
                    
                    <div class="feature">
                        <div class="feature-icon card-icon">💳</div>
                        <div>
                            <strong>校园消费账单</strong>
                        </div>
                    </div>
                </div>
                
                <div class="card">
                    <h1>版本信息</h1>
                    <div class="version-info">
                        <div class="version-item">当前版本：v1.0</div>
                        <div class="version-item">更新时间：2025年4月8日</div>
                        <div class="version-item">支持系统：Android 8.0+</div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, introHtml, "text/html", "UTF-8", null)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 