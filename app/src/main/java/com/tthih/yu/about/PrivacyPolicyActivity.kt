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
                    body { font-family: sans-serif; padding: 16px; line-height: 1.6; color: #333; }
                    h1 { font-size: 20px; color: #000; }
                    h2 { font-size: 18px; color: #000; margin-top: 24px; }
                    p { margin-bottom: 16px; }
                    ul { padding-left: 20px; }
                    li { margin-bottom: 8px; }
                    .date { color: #666; font-style: italic; }
                </style>
            </head>
            <body>
                <h1>YU校园助手隐私政策</h1>
                <p class="date">最后更新日期：2023年7月1日</p>
                
                <p>我们非常重视您的隐私。本隐私政策说明了YU校园助手（以下简称"我们"或"本应用"）收集、使用和保护您的个人信息的方式。请您在使用我们的服务前仔细阅读本隐私政策。</p>
                
                <h2>1. 我们收集的信息</h2>
                <p>为了提供和改进我们的服务，我们可能会收集以下类型的信息：</p>
                <ul>
                    <li><strong>个人信息</strong>：包括但不限于您的学号、密码（经加密处理）、电子邮件地址等。</li>
                    <li><strong>设备信息</strong>：包括设备型号、操作系统版本、设备设置、应用版本等信息。</li>
                    <li><strong>日志信息</strong>：包括您使用我们服务的详细信息，如访问时间、使用的功能等。</li>
                    <li><strong>位置信息</strong>：在您授权的情况下，我们可能会收集您的地理位置信息，以提供校园地图等相关服务。</li>
                </ul>
                
                <h2>2. 我们如何使用这些信息</h2>
                <p>我们使用收集到的信息主要用于以下目的：</p>
                <ul>
                    <li>提供、维护和改进我们的服务；</li>
                    <li>开发新功能和服务；</li>
                    <li>了解用户如何使用我们的服务，以改进用户体验；</li>
                    <li>发送通知、更新和服务相关信息；</li>
                    <li>防止欺诈和滥用行为。</li>
                </ul>
                
                <h2>3. 信息的存储和保护</h2>
                <p>我们非常重视您的个人信息安全，采取了多种安全措施来保护您的个人信息不被未经授权的访问、使用或泄露：</p>
                <ul>
                    <li>我们使用加密技术保护敏感信息（如密码）的传输和存储；</li>
                    <li>我们限制只有需要处理您个人信息的授权人员才能访问这些信息；</li>
                    <li>我们定期审核信息收集、存储和处理实践，以防止未经授权的访问。</li>
                </ul>
                
                <h2>4. 信息共享</h2>
                <p>我们不会与任何第三方共享您的个人信息，除非：</p>
                <ul>
                    <li>获得您的明确同意；</li>
                    <li>为了遵守法律法规的要求；</li>
                    <li>保护本应用、您或公众的权利、财产或安全；</li>
                    <li>与提供技术基础设施服务、分析服务等的服务提供商合作，这些提供商受到保密义务的约束。</li>
                </ul>
                
                <h2>5. 您的权利</h2>
                <p>您对自己的个人信息拥有以下权利：</p>
                <ul>
                    <li>访问和更正您的个人信息；</li>
                    <li>删除您的个人信息；</li>
                    <li>限制或反对我们对您个人信息的处理；</li>
                    <li>数据可携带性：您有权获取我们持有的关于您的个人信息的副本。</li>
                </ul>
                
                <h2>6. 儿童隐私</h2>
                <p>本应用不适用于16岁以下的儿童。我们不会故意收集16岁以下儿童的个人信息。如果您是父母或监护人，发现您的孩子向我们提供了个人信息，请联系我们，我们将采取措施删除这些信息。</p>
                
                <h2>7. 隐私政策的变更</h2>
                <p>我们可能会不时更新本隐私政策。当我们更新隐私政策时，我们会在本页面上发布新的隐私政策，并更新"最后更新日期"。在重大变更的情况下，我们还可能会通过应用内通知或其他方式通知您。</p>
                
                <h2>8. 联系我们</h2>
                <p>如果您对本隐私政策有任何疑问或建议，请通过应用内的"联系我们"功能与我们联系。</p>
                
                <p>感谢您对YU校园助手的信任和支持！</p>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, privacyContent, "text/html", "UTF-8", null)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 