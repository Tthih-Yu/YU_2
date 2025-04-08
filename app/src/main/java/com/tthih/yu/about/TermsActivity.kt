package com.tthih.yu.about

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tthih.yu.R

class TermsActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)
        
        // 初始化视图
        initViews()
        
        // 加载使用条款
        loadTermsContent()
    }
    
    private fun initViews() {
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "使用条款"
        
        // 初始化 WebView
        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = false  // 出于安全考虑禁用 JavaScript
    }
    
    private fun loadTermsContent() {
        val termsHtml = """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: sans-serif;
                        padding: 16px;
                        line-height: 1.5;
                        color: #333;
                    }
                    h1 {
                        font-size: 22px;
                        color: #000;
                    }
                    h2 {
                        font-size: 18px;
                        color: #000;
                        margin-top: 20px;
                    }
                    p {
                        margin-bottom: 16px;
                    }
                </style>
            </head>
            <body>
                <h1>使用条款</h1>
                <p>最后更新日期：2023年8月1日</p>
                
                <p>欢迎使用YU应用。请在使用我们的服务前仔细阅读以下条款。</p>
                
                <h2>1. 接受条款</h2>
                <p>通过访问或使用YU应用，您同意受到此处列出的使用条款的约束。如果您不同意所有这些条款，则不得使用或访问本应用。</p>
                
                <h2>2. 使用许可</h2>
                <p>在您遵守这些条款的情况下，我们授予您访问和使用本应用的个人的、非独占的、不可转让的许可。</p>
                
                <h2>3. 用户账户</h2>
                <p>使用某些功能可能需要注册账户。您负责维护您的账户安全，并对与您账户相关的所有活动负全责。</p>
                
                <h2>4. 使用规则</h2>
                <p>您同意不会：</p>
                <ul>
                    <li>使用本应用进行任何违法或未经授权的目的</li>
                    <li>干扰或中断本应用或连接到本应用的服务器和网络</li>
                    <li>尝试未经授权访问本应用的任何部分、功能或系统</li>
                    <li>收集或跟踪其他用户的个人信息</li>
                </ul>
                
                <h2>5. 知识产权</h2>
                <p>本应用及其原始内容、功能和设计受著作权、商标和其他法律保护。</p>
                
                <h2>6. 免责声明</h2>
                <p>本应用按"现状"和"可用"的基础提供，不作任何形式的明示或暗示保证。</p>
                
                <h2>7. 责任限制</h2>
                <p>在任何情况下，我们对于因使用或无法使用本应用而导致的任何损失或损害不承担责任。</p>
                
                <h2>8. 条款变更</h2>
                <p>我们保留随时修改或替换这些条款的权利。修改后的条款在发布到本应用后立即生效。</p>
                
                <h2>9. 联系我们</h2>
                <p>如果您对这些条款有任何疑问，请联系我们：developer@yucampus.app</p>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, termsHtml, "text/html", "UTF-8", null)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 