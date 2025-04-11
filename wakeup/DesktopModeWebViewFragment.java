package com.example.educationsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;

/**
 * 这个Fragment实现了"电脑模式"功能，通过修改WebView的用户代理字符串
 * 让教务网站误认为是来自桌面浏览器的请求，从而显示适合电脑屏幕的页面版本。
 * 该功能对于一些只能在电脑版显示完整功能的教务系统网站非常有用。
 */
public class DesktopModeWebViewFragment extends Fragment {

    private WebView webView;
    private Chip chipDesktopMode;
    
    // 桌面浏览器的用户代理字符串
    private static final String DESKTOP_USER_AGENT = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/94.0.4606.81 Safari/537.36";
    
    // 移动端的默认用户代理字符串（将在运行时获取）
    private String mobileUserAgent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        // 返回主布局视图
        return inflater.inflate(R.layout.fragment_webview_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化WebView
        webView = view.findViewById(R.id.wv_course);
        setupWebView();
        
        // 初始化电脑模式切换按钮
        chipDesktopMode = view.findViewById(R.id.chip_mode);
        setupDesktopModeToggle();
        
        // 加载教务系统网址
        webView.loadUrl("https://example-education-system.edu.cn/login");
    }
    
    /**
     * 设置WebView的基本配置
     */
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        
        // 启用JavaScript
        webSettings.setJavaScriptEnabled(true);
        
        // 保存当前的移动用户代理字符串
        mobileUserAgent = webSettings.getUserAgentString();
        
        // 允许缩放
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // 设置WebViewClient以拦截URL加载
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }
    
    /**
     * 设置电脑模式切换功能
     */
    private void setupDesktopModeToggle() {
        chipDesktopMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setDesktopMode(isChecked);
            }
        });
    }
    
    /**
     * 切换桌面模式
     * @param enabled 是否启用桌面模式
     */
    private void setDesktopMode(boolean enabled) {
        WebSettings webSettings = webView.getSettings();
        
        if (enabled) {
            // 设置桌面浏览器的用户代理
            webSettings.setUserAgentString(DESKTOP_USER_AGENT);
            
            // 启用宽视图端口
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
            
            // 禁用文本自动调整大小，保持原始布局
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        } else {
            // 恢复移动设备的用户代理
            webSettings.setUserAgentString(mobileUserAgent);
            
            // 禁用宽视图端口，使用移动视图
            webSettings.setUseWideViewPort(false);
            webSettings.setLoadWithOverviewMode(false);
            
            // 启用文本自动调整大小，适应移动设备
            webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        }
        
        // 重新加载当前页面以应用新设置
        webView.reload();
    }
    
    /**
     * 清理资源
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }
} 