package com.example.educationsystem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip

/**
 * 这个Fragment实现了"电脑模式"功能，通过修改WebView的用户代理字符串
 * 让教务网站误认为是来自桌面浏览器的请求，从而显示适合电脑屏幕的页面版本。
 * 该功能对于一些只能在电脑版显示完整功能的教务系统网站非常有用。
 */
class DesktopModeWebViewFragment : Fragment() {

    private var webView: WebView? = null
    private var chipDesktopMode: Chip? = null
    
    // 保存移动设备的原始用户代理字符串
    private var mobileUserAgent: String = ""
    
    companion object {
        // 桌面浏览器的用户代理字符串
        private const val DESKTOP_USER_AGENT = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36"
    }

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 返回主布局视图
        return inflater.inflate(R.layout.fragment_webview_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化WebView
        webView = view.findViewById<WebView>(R.id.wv_course).apply {
            setupWebView(this)
        }
        
        // 初始化电脑模式切换按钮
        chipDesktopMode = view.findViewById<Chip>(R.id.chip_mode).apply {
            setupDesktopModeToggle(this)
        }
        
        // 加载教务系统网址
        webView?.loadUrl("https://example-education-system.edu.cn/login")
    }
    
    /**
     * 设置WebView的基本配置
     */
    private fun setupWebView(webView: WebView) {
        with(webView) {
            settings.apply {
                // 启用JavaScript
                javaScriptEnabled = true
                
                // 保存当前的移动用户代理字符串
                mobileUserAgent = userAgentString
                
                // 允许缩放
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            
            // 设置WebViewClient以拦截URL加载
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }
            }
        }
    }
    
    /**
     * 设置电脑模式切换功能
     */
    private fun setupDesktopModeToggle(chip: Chip) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            setDesktopMode(isChecked)
        }
    }
    
    /**
     * 切换桌面模式
     * @param enabled 是否启用桌面模式
     */
    private fun setDesktopMode(enabled: Boolean) {
        webView?.settings?.apply {
            if (enabled) {
                // 设置桌面浏览器的用户代理
                userAgentString = DESKTOP_USER_AGENT
                
                // 启用宽视图端口
                useWideViewPort = true
                loadWithOverviewMode = true
                
                // 禁用文本自动调整大小，保持原始布局
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            } else {
                // 恢复移动设备的用户代理
                userAgentString = mobileUserAgent
                
                // 禁用宽视图端口，使用移动视图
                useWideViewPort = false
                loadWithOverviewMode = false
                
                // 启用文本自动调整大小，适应移动设备
                layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            }
        }
        
        // 重新加载当前页面以应用新设置
        webView?.reload()
    }
    
    /**
     * 清理资源
     */
    override fun onDestroyView() {
        super.onDestroyView()
        webView?.destroy()
        webView = null
        chipDesktopMode = null
    }
} 