package com.tthih.yu.campuscard

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.tthih.yu.R
import org.jsoup.Jsoup

class CampusCardLoginActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var messageTextView: TextView
    private lateinit var webViewContainer: ViewGroup
    
    // WebVPN Login URL - 指定目标服务为校园卡入口
    private val TARGET_SERVICE_URL = "http://ehall.ahpu.edu.cn/publicapp/sys/myyktzd/mobile/index.do"
    private val LOGIN_URL = try {
        "https://webvpn.ahpu.edu.cn/http/webvpn40a1cc242791dfe16b3115ea5846a65e/authserver/login?service=${java.net.URLEncoder.encode(TARGET_SERVICE_URL, "UTF-8")}"
    } catch (e: Exception) {
        // Fallback or handle encoding error
        Log.e("CampusCardLoginActivity", "Failed to encode service URL", e)
        "https://webvpn.ahpu.edu.cn/http/webvpn40a1cc242791dfe16b3115ea5846a65e/authserver/login" // Or some default
    }
    
    // Base path for campus card system (will be constructed with actual prefix)
    private val CAMPUS_CARD_BASE_PATH = "/publicapp/sys/myyktzd/mobile/oneCard/index.html#!/"
    // Known entry point within WebVPN to extract the correct prefix after login
    private val CAMPUS_CARD_ENTRY_PATH = "/publicapp/sys/myyktzd/mobile/index.do"
    
    // Tag for logging
    private val TAG = "CampusCardLoginActivity"
    
    // Add flags to track states
    private var isLoggedIn = false
    private var extractedWebVpnPrefix: String? = null
    // private var isNavigatingToCampusCard = false // This flag might not be needed anymore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_login)
        
        // Initialize views
        progressBar = findViewById(R.id.progress_bar)
        messageTextView = findViewById(R.id.tv_message)
        webView = findViewById(R.id.webview)
        webViewContainer = findViewById(R.id.webview_container)
        
        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "校园卡登录"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        // Setup WebView
        setupWebView()
        
        // Start with the login page
        messageTextView.text = "请登录您的账号..."
        Log.d(TAG, "Loading initial login URL: $LOGIN_URL")
        webView.loadUrl(LOGIN_URL)
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        // Add JavaScript interface to receive data from WebView
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        
        // Configure WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")
                progressBar.isVisible = false
                if (url == null) return

                // Always try to extract and store the latest valid prefix from *any* loaded page
                extractWebVpnPrefix(url)?.let { latestPrefix ->
                    // Only store if it's different or first time
                    if (latestPrefix != extractedWebVpnPrefix) {
                        extractedWebVpnPrefix = latestPrefix
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putString(PREF_WEBVPN_PREFIX, latestPrefix)
                            .apply()
                        Log.d(TAG,"Stored latest valid prefix: $latestPrefix")
                    }
                }

                when {
                    // Landed on the target entry page after login redirect
                    // Check for the base path AND a ticket parameter to confirm it's the post-login redirect
                    url.contains(CAMPUS_CARD_ENTRY_PATH) && url.contains("ticket=") -> {
                        Log.d(TAG, "Reached campus card entry point via ticket redirect.")
                        messageTextView.text = "认证成功，正在加载校园卡主页..."
                        isLoggedIn = true // We are authenticated

                        // extractedWebVpnPrefix should now hold the CORRECT prefix from this URL
                        if (extractedWebVpnPrefix != null) {
                            val finalCampusCardUrl = "https://webvpn.ahpu.edu.cn" + extractedWebVpnPrefix + CAMPUS_CARD_BASE_PATH
                            Log.d(TAG, "Loading final campus card page: $finalCampusCardUrl")
                            webView.loadUrl(finalCampusCardUrl)
                        } else {
                            Log.e(TAG, "Failed to load final campus card page: Prefix is null after reaching entry point.")
                            messageTextView.text = "无法加载校园卡主页，未能获取有效路径。"
                        }
                    }

                    // Landed on the final campus card page
                    url.contains(CAMPUS_CARD_BASE_PATH) -> {
                        Log.d(TAG, "Successfully loaded final campus card page.")
                        messageTextView.text = "校园卡页面加载成功，正在提取数据..."
                        isLoggedIn = true // Ensure flag is set
                        Handler(Looper.getMainLooper()).postDelayed({
                            injectCardDataExtractor()
                        }, 3000) // Adjust delay if needed
                    }

                    // Still on the login page
                    url.contains("authserver/login") -> {
                         messageTextView.text = "请输入统一身份认证账号密码"
                         isLoggedIn = false
                    }

                     // Landed on WebVPN portal (shouldn't happen now, but handle defensively)
                    url.contains("/enlink/#/client/app") || url.startsWith("https://webvpn.ahpu.edu.cn/enlink/") -> {
                         Log.w(TAG, "Landed on WebVPN portal unexpectedly. Login service parameter might be wrong.")
                         messageTextView.text = "登录后跳转页面异常，请联系开发者。"
                         isLoggedIn = true // Authenticated, but stuck
                    }
                }
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "WebView error: ${error?.description} for URL: ${request?.url}")
                 // Avoid showing error for intermediate redirects if possible
                 if (request?.isForMainFrame == true) {
                    messageTextView.text = "加载出错: ${error?.description}"
                 }
                progressBar.isVisible = false
            }
            
            // Allow redirects within WebVPN
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d(TAG, "shouldOverrideUrlLoading: $url")
                return false // Let WebView handle all loads
            }
        }
        
        // Configure WebChromeClient for progress tracking
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    progressBar.isVisible = true
                    progressBar.progress = newProgress
                } else {
                    progressBar.isVisible = false
                }
            }
        }
    }
    
    private fun extractWebVpnPrefix(url: String): String? {
        try {
            // Regex to capture the WebVPN prefix: /http/webvpn<hex_string>
            val regex = Regex("(/http/webvpn[a-fA-F0-9]+)")
            val match = regex.find(url)
            val prefix = match?.groups?.get(1)?.value
            
            if (prefix != null) {
                // Only log if it's different from the currently stored one
                if (prefix != extractedWebVpnPrefix) {
                     Log.d(TAG, "Extracted WebVPN prefix: $prefix from URL: $url")
                }
                return prefix
            } else {
                // Log if URL was expected to have a prefix but didn't
                if (url.contains("webvpn.ahpu.edu.cn/http/")) {
                    Log.w(TAG, "URL contains /http/ but failed to extract prefix: $url")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting WebVPN prefix from $url", e)
        }
        return null
    }
    
    private fun injectCardDataExtractor() {
        messageTextView.text = "正在获取校园卡数据..."
        
        val jsCode = """
            try {
                // Function to extract card information
                function extractCardData() {
                    // Look for balance
                    let balanceElement = document.querySelector('.balance');
                    let balance = balanceElement ? balanceElement.innerText.trim() : 'N/A';
                    
                    // Look for card number
                    let cardNumElement = document.querySelector('.account');
                    let cardNumber = cardNumElement ? cardNumElement.innerText.trim() : 'N/A';
                    
                    // Return the data as JSON
                    return JSON.stringify({
                        balance: balance,
                        cardNumber: cardNumber
                    });
                }
                
                // Execute the extraction
                let cardData = extractCardData();
                Android.onCardDataExtracted(cardData);
                
            } catch(e) {
                Android.onProgress("提取校园卡数据出错: " + e.message);
                console.error("提取校园卡数据出错:", e);
            }
        """.trimIndent()
        
        webView.evaluateJavascript(jsCode, null)
    }
    
    // Override back button to return success if logged in
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else if (isLoggedIn) {
            finishWithSuccess()
        } else {
            super.onBackPressed()
        }
    }
    
    private fun finishWithSuccess() {
        // Send back result to calling activity
        setResult(RESULT_OK)
        finish()
    }
    
    inner class WebAppInterface {
        @JavascriptInterface
        fun onProgress(message: String) {
            runOnUiThread {
                messageTextView.text = message
            }
        }
        
        @JavascriptInterface
        fun onCardDataExtracted(cardDataJson: String) {
            Log.d(TAG, "Card data extracted: $cardDataJson")
            runOnUiThread {
                messageTextView.text = "校园卡数据获取成功，点击返回按钮回到主界面"
            }
            
            // Save the extracted data to pass back to the activity
            val intent = Intent()
            intent.putExtra("card_data", cardDataJson)
            setResult(RESULT_OK, intent)
            
            // Don't finish yet, let the user manually go back
            // This gives them a chance to interact with the campus card page
        }
    }
    
    companion object {
        // Constants from CampusCardRepository
        private const val PREFS_NAME = "CampusCardPrefs"
        private const val PREF_WEBVPN_PREFIX = "webvpn_prefix"
    }
} 