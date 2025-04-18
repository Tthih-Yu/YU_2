package com.tthih.yu.about

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tthih.yu.R

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "隐私政策"
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // 设置WebView加载隐私政策内容
        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = false
        
        // 这里使用本地HTML文件，也可以加载远程URL
        loadPrivacyPolicyContent()
    }
    
    private fun loadPrivacyPolicyContent() {
        // 使用HTML字符串直接加载内容
        val privacyContent = """
            <!DOCTYPE html>
<html>

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            font-family: sans-serif;
            padding: 16px;
            line-height: 1.6;
            color: #333;
        }
        
        h1 {
            font-size: 20px;
            color: #d06464;
        }
        
        h2 {
            font-size: 18px;
            color: #ea0505;
            margin-top: 24px;
        }
        
        p {
            margin-bottom: 16px;
        }
        
        ul {
            padding-left: 20px;
        }
        
        li {
            margin-bottom: 8px;
        }
        
        .date {
            color: #666;
            font-style: italic;
        }
    </style>
</head>

<body>
    <h1>YU隐私政策</h1>
    <p class="date">最后更新日期：2025年4月14日</p>
    <p>联系方式：2190638246@qq.com</p>
    <p>我们非常重视您的隐私。本隐私政策说明了YU（以下简称"我们"或"本应用"）收集、使用和保护您的个人信息的方式。请您在使用我们的服务前仔细阅读本隐私政策。</p>

    <h2>1. 我们收集的信息</h2>
    <p>为了提供和改进我们的服务，我们提供以下服务：</p>
    <ul>
        <li><strong>个人信息</strong>：我们不会收集您的任何个人信息，包括但不限于您的学号、密码（经加密处理）、电子邮件地址等。</li>
        <li><strong>设备信息</strong>：我们不会收集您的任何设备信息，包括但不限于设备型号、操作系统版本、设备设置、应用版本等信息。</li>
        <li><strong>日志信息</strong>：我们不会收集您的任何日志信息，包括您使用我们服务的详细信息，如访问时间、使用的功能等。</li>
        <li><strong>位置信息</strong>：我们不会收集您的任何位置信息，包括但不限于您的位置信息、IP地址等。</li>
    </ul>

    <h2>2.用户可以向我们提供信息</h2>
    <p>用户可以向我们提供信息，包括但不限于：</p>
    <ul>
        <li>使用过程中产生的bug反馈</li>
        <li>APP优化建议</li>
        <li>对APP未来的期待</li>
    </ul>

    <h2>3. 信息的存储和保护</h2>
    <p>我们非常重视您的个人信息安全，注重您的个人信息不被未经授权的访问、使用或泄露：</p>
    <ul>
        <li>APP使用过程中所有的信息均存储本地；</li>
        <li>我们不会收集任何个人信息；</li>
    </ul>

    <h2>4. 权限说明</h2>
    <p>APP需要使用以下权限：</p>
    <ul>
        <li>1. 获取网络状态：用于APP基础功能，以便于用户正常使用APP</li>
        <li>2. 本地存储：用于APP基础功能，存储APP运行过程中产生的信息</li>
        <li>3. 通知权限：用于APP的信息推送</li>
        <li>4.添加桌面快捷方式：用于APP的快捷方式</li>
    </ul>

    <h2>5. 您的权利</h2>
    <p>您对自己的个人信息拥有以下权利：</p>
    <ul>
        <li>访问和更正您的个人信息；</li>
        <li>删除您的个人信息；</li>
    </ul>


    <h2>6. 隐私政策的变更</h2>
    <p>我们可能会不时更新本隐私政策。当我们更新隐私政策时，我们会在本页面上发布新的隐私政策，并更新"最后更新日期"。在重大变更的情况下，我们还可能会通过应用内通知或其他方式通知您。</p>

    <h2>7. 联系我们</h2>
    <p>如果您对本隐私政策有任何疑问或建议，请通过应用内的"联系我们"功能与我们联系。</p>

    <p>感谢您对YU的信任和支持！</p>

</html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, privacyContent, "text/html", "UTF-8", null)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 