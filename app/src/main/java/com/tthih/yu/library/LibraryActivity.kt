package com.tthih.yu.library

import android.content.Context
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tthih.yu.R

class LibraryActivity : AppCompatActivity() {
    
    private val viewModel: LibraryViewModel by viewModels()
    
    // 图书馆网站URL
    private val libraryUrl = "http://211.86.225.3:8090/opac/"
    
    // 定义抹茶绿主题的颜色常量
    private val MatchaGreen = Color(0xFF8BC34A) // 主色
    private val MatchaLightGreen = Color(0xFFCFE5B4) // 浅绿色
    private val MatchaDarkGreen = Color(0xFF689F38) // 深绿色
    private val MatchaBgColor = Color(0xFFF4F8F0) // 背景色
    private val MatchaTextPrimary = Color(0xFF2E4F2C) // 主文本色
    private val MatchaTextSecondary = Color(0xFF5E7859) // 次要文本色
    private val MatchaTextHint = Color(0xFF8FA889) // 提示文本色
    private val MatchaCardBg = Color(0xFFFFFFFF) // 卡片背景色
    private val MatchaDivider = Color(0xFFEAF2E3) // 分割线颜色
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        
        composeView.setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = MatchaGreen,
                    secondary = MatchaDarkGreen,
                    background = MatchaBgColor,
                    surface = MatchaCardBg,
                    onPrimary = Color.White,
                    onBackground = MatchaTextPrimary,
                    onSurface = MatchaTextPrimary
                )
            ) {
                LibraryScreen()
            }
        }
    }
    
    @Composable
    fun LibraryScreen() {
        var isLoading by remember { mutableStateOf(true) }
        var loadError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var webView by remember { mutableStateOf<WebView?>(null) }
        var canGoBack by remember { mutableStateOf(false) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MatchaBgColor)
                .padding(top = 16.dp)
        ) {
            // 顶部标题栏
            TopBar(
                onBackClick = { finish() },
                canGoBack = canGoBack,
                onWebBack = {
                    webView?.let {
                        if (it.canGoBack()) {
                            it.goBack()
                        }
                    }
                }
            )
            
            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isConnectedToCampusNetwork()) {
                    // 在校园网内，加载WebView
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, url: String) {
                                        isLoading = false
                                        loadError = false
                                        canGoBack = view.canGoBack()
                                    }
                                    
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        isLoading = false
                                        loadError = true
                                        errorMessage = "加载失败，请检查网络连接"
                                    }
                                    
                                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                        canGoBack = view?.canGoBack() ?: false
                                    }
                                }
                                
                                loadUrl(libraryUrl)
                                webView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    if (isLoading) {
                        LoadingIndicator()
                    }
                    
                    if (loadError) {
                        ErrorMessage(message = errorMessage) {
                            isLoading = true
                            loadError = false
                            webView?.reload()
                        }
                    }
                } else {
                    // 不在校园网环境
                    NotInCampusNetworkMessage()
                }
            }
        }
    }
    
    @Composable
    fun TopBar(onBackClick: () -> Unit, canGoBack: Boolean, onWebBack: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MatchaGreen
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 标题
                Text(
                    text = "图书馆",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f),
                    textAlign = TextAlign.Center
                )
                
                // WebView返回按钮
                IconButton(
                    onClick = onWebBack,
                    enabled = canGoBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_web_back),
                        contentDescription = "网页返回",
                        tint = if (canGoBack) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    
    @Composable
    fun LoadingIndicator() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MatchaCardBg.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MatchaGreen,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "加载中...",
                        color = MatchaTextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
    
    @Composable
    fun ErrorMessage(message: String, onRetry: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MatchaCardBg
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "出错了",
                        color = MatchaTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        color = MatchaTextSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatchaGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "重试",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun NotInCampusNetworkMessage() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MatchaCardBg
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "未连接校园网",
                        color = MatchaTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "图书馆系统需要校园网环境才能访问，请连接校园网后重试。",
                        color = MatchaTextSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "可以通过连接校园WiFi或使用校园VPN连接校园网。",
                        color = MatchaTextHint,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { recreate() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatchaGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "刷新",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
    
    // 检查是否连接到校园网
    private fun isConnectedToCampusNetwork(): Boolean {
        // 这里简化处理，实际项目中可以通过检查IP地址段或使用其他方式判断是否在校园网环境
        return isConnectedToNetwork()
    }
    
    // 检查是否有网络连接
    private fun isConnectedToNetwork(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }
} 