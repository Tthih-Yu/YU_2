package com.tthih.yu.campuscard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tthih.yu.R
// import org.jsoup.Jsoup // No longer needed
import okhttp3.Cookie as OkHttpCookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CampusCardLoginActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var messageTextView: TextView
    private lateinit var webViewContainer: ViewGroup
    private lateinit var confirmLoginFab: FloatingActionButton
    
    // Direct login URL for the campus card system
    private val LOGIN_URL = "http://220.178.164.65:8053/"
    // Domain for cookie extraction
    private val TARGET_DOMAIN = "220.178.164.65"
    // A path segment assumed to be present after successful login
    private val SUCCESS_INDICATOR_PATH = "/Phone/Index" // Updated based on logs
    
    // Tag for logging
    private val TAG = "CampusCardLoginActivity"
    
    // Add flags to track states
    private var isLoggedInLikely = false
    private var isExtractingAccount = false // Flag to prevent multiple extraction attempts
    private var hasReachedSuccessPage = false // <-- Added flag
    // private var extractedWebVpnPrefix: String? = null // Removed WebVPN prefix logic
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_login)
        
        // Initialize views
        progressBar = findViewById(R.id.progress_bar)
        messageTextView = findViewById(R.id.tv_message)
        webView = findViewById(R.id.webview)
        webViewContainer = findViewById(R.id.webview_container)
        confirmLoginFab = findViewById(R.id.fab_confirm_login)
        
        // Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "校园卡登录"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        // Set FAB click listener
        confirmLoginFab.setOnClickListener {
            if (isExtractingAccount) return@setOnClickListener // Prevent double taps

            Log.d(TAG, "Confirm login FAB clicked.")
            // 1. Save Cookies first
            saveCookiesToNetworkModule(LOGIN_URL, TARGET_DOMAIN)

            // 2. Attempt to extract Account Number via JavaScript
            extractAccountAndFinish()
        }
        
        // Clear cookies for the target domain before starting login
        // to ensure a fresh session
        clearCookiesForDomain(TARGET_DOMAIN)
        
        // Setup WebView
        setupWebView()
        
        // Start with the login page
        messageTextView.text = "请登录校园卡系统..."
        confirmLoginFab.isVisible = false // Ensure FAB starts hidden
        Log.d(TAG, "Loading initial login URL: $LOGIN_URL")
        webView.loadUrl(LOGIN_URL)
    }
    
    private fun clearCookiesForDomain(domain: String) {
        // Clear WebView cookies (keep this part)
        val cookieManager = android.webkit.CookieManager.getInstance()
        val cookies = cookieManager.getCookie(LOGIN_URL) // Get cookies for the base URL
        if (cookies != null) {
            val cookieArray = cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (cookie in cookieArray) {
                val cookieParts = cookie.split("=".toRegex(), 2).toTypedArray()
                if (cookieParts.size == 2) {
                    val cookieName = cookieParts[0].trim { it <= ' ' }
                    cookieManager.setCookie(LOGIN_URL, "$cookieName=; Domain=$domain; Path=/; Max-Age=0")
                     Log.d(TAG, "Attempting to clear WebView cookie: $cookieName for domain $domain")
                }
            }
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                 cookieManager.flush()
             } else {
                 // Deprecated method for older APIs
                 // CookieSyncManager.getInstance().sync()
             }
            Log.d(TAG, "Cleared WebView cookies for domain: $domain")
        } else {
             Log.d(TAG, "No WebView cookies found for domain: $domain to clear.")
        }
        
        // Clear OkHttp persistent cookies using NetworkModule
        NetworkModule.clearCookies()
        Log.i(TAG, "Cleared OkHttp persistent cookies via NetworkModule.")
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true // Keep JS enabled for login forms
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_NO_CACHE // Avoid caching login pages
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // Might be needed depending on the site
        }
        
        // Add JavaScript interface ONLY for progress updates, if needed
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        
        // Configure WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")
                progressBar.isVisible = false
                if (url == null) return

                // Check if the loaded URL indicates potential successful login
                if (url.contains(SUCCESS_INDICATOR_PATH)) {
                    if (!hasReachedSuccessPage) { // Check if this is the first time
                        Log.i(TAG, "Detected likely successful login page for the first time: $url")
                        messageTextView.text = "请点击[我的账单]登陆，登陆后请点击右下角按钮确认。"
                        hasReachedSuccessPage = true // Set the flag
                    } else {
                        // Already seen success page, maybe navigated back or around
                        messageTextView.text = "已登录，可继续操作或点击右下角按钮完成。"
                    }
                    confirmLoginFab.isVisible = true // Always show FAB once success page is reached

                } else if (url == LOGIN_URL || url.contains("/Phone/Login")) {
                    // If back on login page AND we haven't confirmed yet, reset flag and hide FAB
                    if (!isFinishing) { // Check if activity is still running
                        hasReachedSuccessPage = false // Reset if back on login page
                        messageTextView.text = "请输入校园卡账号密码"
                        confirmLoginFab.isVisible = false
                    }
                } else {
                    // For any other page (within domain or off-domain)
                    // Keep FAB visible if success page was reached, otherwise hide
                    confirmLoginFab.isVisible = hasReachedSuccessPage
                    if (hasReachedSuccessPage) {
                        messageTextView.text = "已登录，可继续操作或点击右下角按钮完成。"
                    } else {
                        // Handle cases like loading intermediate pages or errors before login success
                        if (url.startsWith(LOGIN_URL)) {
                            messageTextView.text = "正在加载..."
                        } else {
                            Log.w(TAG, "Navigated away from target domain: $url")
                            messageTextView.text = "已离开校园卡系统页面"
                        }
                    }
                }
            }
            
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.isVisible = true // Show progress bar when page starts loading
                // Hide FAB during page loads unless we are already likely logged in
                // --- Removed the old logic ---
                // if (!isLoggedInLikely) { 
                //     confirmLoginFab.isVisible = false
                // }
                // --- End Removed Logic ---

                // Keep FAB visible during page loads if success page has been reached
                confirmLoginFab.isVisible = hasReachedSuccessPage
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                val errorUrl = request?.url.toString()
                val errorCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) error?.errorCode else 0
                val errorDesc = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) error?.description else "Unknown Error"

                Log.e(TAG, "WebView error: Code=$errorCode, Desc=$errorDesc for URL: $errorUrl")
                 if (request?.isForMainFrame == true) {
                    messageTextView.text = "加载出错: $errorDesc"
                 }
                progressBar.isVisible = false
            }
            
            // Let WebView handle all loads within the same base URL
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d(TAG, "shouldOverrideUrlLoading: $url")
                // Allow loading within the target host or initial login URL
                // return !(url.startsWith(LOGIN_URL))
                 return false // Let the WebView handle everything for now, simplify debugging
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
    
    // Save cookies from WebView to OkHttp CookieJar (now uses NetworkModule helper)
    private fun saveCookiesToNetworkModule(url: String, domain: String) {
        val cookieManager = android.webkit.CookieManager.getInstance()
        val cookiesString = cookieManager.getCookie(url)
        
        if (cookiesString != null && cookiesString.isNotEmpty()) {
            val okHttpUrl = url.toHttpUrl()
            val parsedCookies = mutableListOf<OkHttpCookie>()
            val cookieHeaders = cookiesString.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            
            cookieHeaders.forEach { cookieHeader ->
                OkHttpCookie.parse(okHttpUrl, cookieHeader)?.let { parsedCookie ->
                   // Ensure the cookie domain matches our target
                    if (parsedCookie.domain.contains(domain, ignoreCase = true)) {
                        parsedCookies.add(parsedCookie)
                        Log.d(TAG, "Parsed cookie to save: ${parsedCookie.name}=${parsedCookie.value}")
                    } else {
                         Log.w(TAG, "Skipping cookie with mismatched domain: ${parsedCookie.name}, domain=${parsedCookie.domain}")
                    }
                }
            }

            if (parsedCookies.isNotEmpty()) {
                // USE the new NetworkModule function
                NetworkModule.saveCookiesFromResponse(url, parsedCookies)
                Log.i(TAG, "Successfully saved ${parsedCookies.size} cookies via NetworkModule for domain $domain")
            } else {
                 Log.w(TAG, "No relevant cookies parsed to save for domain $domain from string: $cookiesString")
            }

        } else {
            Log.w(TAG, "Could not get cookies from WebView for URL: $url")
                }
            }
                
    // Method to finish activity with success result
    private fun finishWithSuccess() {
        if (!isFinishing) { // Prevent multiple calls
            Log.i(TAG, "Login successful (Cookies & Account Saved), setting RESULT_OK and finishing activity.")
            val resultIntent = Intent()
            // Optionally pass back data if needed
            // resultIntent.putExtra("some_key", "some_value")
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
    
    // Override back button to return success if logged in
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            // If cannot go back, finish with CANCELED result
            Log.i(TAG, "User pressed back, finishing with RESULT_CANCELED.")
            setResult(RESULT_CANCELED)
            super.onBackPressed()
        }
    }
    
    // Add JavaScript interface if not already present or adjust if needed
    inner class WebAppInterface {
        // You might not need anything here if just relying on URL checks
    }

    // --- New function to handle account extraction and finishing ---
    private fun extractAccountAndFinish() {
        isExtractingAccount = true // Set flag
        messageTextView.text = "正在提取学号..."
        confirmLoginFab.isEnabled = false // Disable FAB during extraction

        // *** IMPORTANT: YOU MUST VERIFY THIS JAVASCRIPT ***
        // Inspect the HTML source of the successful login page (e.g., the one with /Phone/Index)
        // Find the element containing the user's account number (学号).
        // Replace 'user-account-id' with the correct element ID, or use other selectors
        // like getElementsByClassName, querySelector, etc. if ID is not available.
        val jsCode = "(function() { var elem = document.getElementById('user-account-id'); return elem ? elem.innerText : null; })();"
        // Alternative example using querySelector (if ID is not present):
        // val jsCode = "(function() { var elem = document.querySelector('.some-class-containing-account'); return elem ? elem.innerText : null; })();"

        webView.evaluateJavascript(jsCode) { result ->
            val userAccount = result?.takeIf { it != "null" && it.isNotEmpty() }?.trim('"', '\'')

            if (userAccount != null) {
                Log.i(TAG, "Successfully extracted account via JS: $userAccount")
                saveAccountAndFinish(userAccount)
            } else {
                Log.e(TAG, "Failed to extract account via JS or account not found in HTML using selector in jsCode.")
                // Handle failure: Ask user to input manually
                promptForManualAccountInput()
                // Re-enable FAB in case manual input is cancelled
                // isExtractingAccount = false // Reset flag handled in prompt result
                // confirmLoginFab.isEnabled = true
            }
        }
            }
            
    // --- Function to ask user for account manually ---
    private fun promptForManualAccountInput() {
        // Apply the custom rounded style here
        val inputDialog = android.app.AlertDialog.Builder(this, R.style.RoundedAlertDialog) 
        inputDialog.setTitle("输入卡号")
        inputDialog.setMessage("查询方法：进入主副卡转账查看主卡账号，手动输入：")
        val inputEditText = android.widget.EditText(this)
        inputEditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER // Or text if needed
        inputDialog.setView(inputEditText)

        inputDialog.setPositiveButton("确定") { dialog, _ ->
            val manualAccount = inputEditText.text.toString().trim()
            if (manualAccount.isNotEmpty()) {
                Log.i(TAG, "User manually entered account: $manualAccount")
                saveAccountAndFinish(manualAccount)
            } else {
                showError("未输入卡号，无法完成登录。")
                resetLoginStateAfterFailure()
            }
            dialog.dismiss()
        }
        inputDialog.setNegativeButton("取消") { dialog, _ ->
            showError("用户取消输入，无法完成登录。")
            resetLoginStateAfterFailure()
            dialog.dismiss()
        }
        inputDialog.setCancelable(false) // Prevent dismissing by tapping outside
        inputDialog.show()
    }
    
    // --- Helper to reset state if account extraction/input fails ---
    private fun resetLoginStateAfterFailure() {
         messageTextView.text = "获取学号失败，请重试。"
         confirmLoginFab.isEnabled = true // Re-enable button
         isExtractingAccount = false // Reset flag
    }


    // --- Updated function to save account AND THEN finish ---
    private fun saveAccountAndFinish(account: String) {
        // --- Access CampusCardRepository Instance via Singleton ---
         val repository = CampusCardRepository.getInstance(applicationContext)
        
        // Save the account using the repository instance
        repository.saveAccount(account)
        Log.i(TAG, "User account '$account' saved to SharedPreferences via Repository Singleton.")

        // Now finish the activity with success
        finishWithSuccess()
    }

    // --- Helper to show error messages ---
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        // Optionally log the error as well
        Log.e(TAG, "Login Error: $message")
    }
} 