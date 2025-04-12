package com.tthih.yu.schedule

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.chip.Chip
import com.tthih.yu.R
import org.json.JSONArray

class ScheduleImportActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var messageTextView: TextView
    private lateinit var importBtn: Button
    private lateinit var toolbar: Toolbar
    private lateinit var chipDesktopMode: Chip
    private var isWebViewLoaded = false
    
    // 添加 Handler 和导入成功标志
    private val handler = Handler(Looper.getMainLooper())
    private var importSuccess = false
    
    // 登录URL
    private val LOGIN_URL = "https://webvpn.ahpu.edu.cn/http/webvpn40a1cc242791dfe16b3115ea5846a65e/authserver/login?service=https://webvpn.ahpu.edu.cn/enlink/api/client/callback/cas"

    // WebView容器，用于需要重建WebView时
    private lateinit var webViewContainer: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_import)

        // 初始化视图
        progressBar = findViewById(R.id.progress_bar)
        messageTextView = findViewById(R.id.tv_message)
        importBtn = findViewById(R.id.btn_import)
        toolbar = findViewById(R.id.toolbar)
        chipDesktopMode = findViewById(R.id.chip_desktop_mode)
        webView = findViewById(R.id.webview)
        webViewContainer = webView.parent as ViewGroup
        
        // 设置Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "导入教务系统课表"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // 配置WebView
        setupWebView()

        // 设置电脑模式切换不再需要改变WebView的显示模式，而是仅用作标志
        chipDesktopMode.setOnCheckedChangeListener { _, isChecked ->
            messageTextView.text = if (isChecked) 
                "已启用电脑模式，将直接解析课表数据" 
            else 
                "已切换到移动模式"
        }

        // 设置导入按钮点击监听
        importBtn.setOnClickListener {
            if (!isWebViewLoaded) {
                Toast.makeText(this, "页面尚未加载完成，请稍候", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            importSchedule()
        }
    }

    /**
     * 导入课表
     */
    private fun importSchedule() {
        progressBar.visibility = android.view.View.VISIBLE
        importBtn.isEnabled = false
        messageTextView.text = "正在解析课表数据..."
        
        // 改进的导入方法
        // 1. 先尝试注入直接解析脚本
        injectDirectParserScript()
        
        // 2. 添加额外的直接解析JS调用，专门处理校园网WebVPN环境
        webView.evaluateJavascript("""
            try {
                // 添加一个统一的处理函数
                function extractCourseData() {
                    const courses = [];
                    
                    // 处理课表格子
                    const cells = document.querySelectorAll('.kbcontent');
                    if (cells && cells.length > 0) {
                        Android.onProgress("找到" + cells.length + "个课表格子，正在提取...");
                        
                        cells.forEach((cell, index) => {
                            const text = cell.textContent.trim();
                            if (text && text !== '' && text !== '&nbsp;') {
                                // 解析这个格子的课程信息
                                const courseMatch = text.match(/(.+?)\\n(.+?)\\n(.+)/);
                                if (courseMatch) {
                                    const courseName = courseMatch[1].trim();
                                    const teacherLocation = courseMatch[2].trim();
                                    const weekInfo = courseMatch[3].trim();
                                    
                                    // 确定周几
                                    const colIndex = index % 7;
                                    const day = colIndex + 1;
                                    
                                    // 确定节次
                                    const rowIndex = Math.floor(index / 7);
                                    const startNode = rowIndex * 2 + 1;
                                    const endNode = startNode + 1;
                                    
                                    // 确定周次 - 简单处理默认为1-16周
                                    const weeks = Array.from({length: 16}, (_, i) => i + 1);
                                    
                                    // 解析教师和地点
                                    let teacher = '';
                                    let location = '';
                                    if (teacherLocation.includes('/')) {
                                        [teacher, location] = teacherLocation.split('/');
                                    } else {
                                        location = teacherLocation;
                                    }
                                    
                                    // 创建课程对象
                                    courses.push({
                                        name: courseName,
                                        teacher: teacher,
                                        position: location,
                                        day: day,
                                        sections: [startNode, endNode],
                                        weeks: weeks
                                    });
                                }
                            }
                        });
                    }
                    
                    // 如果通过界面获取不到，尝试从全局变量获取
                    if (courses.length === 0 && typeof coursesData !== 'undefined' && Array.isArray(coursesData)) {
                        return coursesData;
                    }
                    
                    return courses;
                }
                
                // 执行提取
                const extractedCourses = extractCourseData();
                if (extractedCourses && extractedCourses.length > 0) {
                    Android.onProgress("成功提取到" + extractedCourses.length + "门课程");
                    Android.onScheduleDataReceived(JSON.stringify(extractedCourses));
                } else {
                    Android.onProgress("未能从界面提取到课程，请确保已加载课表页面");
                }
            } catch(e) {
                Android.onProgress("提取课程数据出错: " + e.message);
                console.error("提取课程数据出错:", e);
            }
        """, null)
    }

    /**
     * 注入直接解析课表的脚本
     */
    private fun injectDirectParserScript() {
        // 先注入xiaoai中的三个JS文件内容
        val sharedFunctions = """
            // 工具函数模拟
            function loadTool(toolName) {
                return Promise.resolve();
            }
            
            // 兼容处理，避免bg和jQuery未定义错误
            if (typeof bg === 'undefined') {
                var bg = {
                    form: {
                        addInput: function() { return null; }
                    }
                };
            }
            
            if (typeof jQuery === 'undefined') {
                var jQuery = function() { return { on: function() {} }; };
            }
            
            // AIScheduleTools提供的方法模拟
            const AIScheduleTools = function() {
                return {
                    AIScheduleAlert: function(content) {
                        if (typeof content === 'object') {
                            Android.onProgress(content.contentText || JSON.stringify(content));
                        } else {
                            Android.onProgress(content);
                        }
                        return Promise.resolve();
                    },
                    AISchedulePrompt: function(options) {
                        // 调用Android方法提示用户输入
                        Android.onProgress(options.titleText || "请输入" + options.defaultText);
                        return new Promise((resolve) => {
                            // 创建一个全局回调
                            window.resolvePrompt = function(value) {
                                resolve(value);
                                delete window.resolvePrompt;
                            };
                            // 调用原生方法以显示输入对话框
                            Android.showPromptDialog(
                                options.titleText || "",
                                options.tipText || "",
                                options.defaultText || ""
                            );
                        });
                    },
                    AIScheduleSelect: function(options) {
                        Android.onProgress("准备选择: " + (options.contentText || "请选择"));
                        return new Promise((resolve) => {
                            window.resolveSelect = function(value) {
                                console.log("选择结果: " + value);
                                resolve(value);
                                delete window.resolveSelect;
                            };
                            
                            // 确保selectList是数组
                            let selectList = options.selectList;
                            if (!selectList) {
                                console.log("selectList为空，创建默认选项");
                                selectList = ["0:默认当前学期"];
                            }
                            
                            if (typeof selectList === 'string') {
                                console.log("selectList是字符串，尝试解析", selectList);
                                try {
                                    selectList = JSON.parse(selectList);
                                } catch (e) {
                                    console.error("JSON解析失败，尝试按逗号分割", e);
                                    selectList = selectList.split(',').map(s => s.trim());
                                }
                            }
                            
                            // 如果还不是数组，强制转换为数组
                            if (!Array.isArray(selectList)) {
                                console.log("selectList不是数组，转为数组", selectList);
                                selectList = [String(selectList)];
                            }
                            
                            // 确保非空且格式正确
                            if (selectList.length === 0) {
                                console.log("selectList为空数组，添加默认选项");
                                selectList = ["0:默认当前学期", "1:上一学期"];
                            }
                            
                            // 记录选项列表用于调试
                            console.log("最终选项列表: " + JSON.stringify(selectList));
                            Android.onProgress("显示 " + selectList.length + " 个选项");
                            
                            // 将选项列表转为简单的字符串数组，避免复杂对象序列化问题
                            const finalOptions = selectList.map(option => {
                                if (typeof option === 'object') {
                                    return JSON.stringify(option);
                                }
                                return String(option);
                            });
                            
                            // 转换为JSON字符串并确保是数组格式
                            const selectListJson = JSON.stringify(finalOptions);
                            console.log("发送给Android的选项: " + selectListJson);
                            
                            Android.showSelectDialog(
                                options.titleText || "选择",
                                options.contentText || "请做出选择",
                                selectListJson
                            );
                        });
                    }
                };
            };
            
            // 直接暴露AIScheduleAlert, AISchedulePrompt, AIScheduleSelect函数到全局
            // 这样provider.js可以直接调用这些函数而不需要先调用loadTool
            var AIScheduleAlert = AIScheduleTools().AIScheduleAlert;
            var AISchedulePrompt = AIScheduleTools().AISchedulePrompt;
            var AIScheduleSelect = AIScheduleTools().AIScheduleSelect;
            
            // 模拟request函数
            async function request(method, data, url) {
                try {
                    return new Promise((resolve, reject) => {
                        let xhr = new XMLHttpRequest();
                        xhr.open(method, url, true);
                        xhr.onreadystatechange = function() {
                            if (xhr.readyState === 4) {
                                if (xhr.status >= 200 && xhr.status < 300) {
                                    resolve(xhr.responseText);
                                } else {
                                    reject(new Error('请求失败: ' + xhr.status));
                                }
                            }
                        };
                        xhr.onerror = function() {
                            reject(new Error('网络错误'));
                        };
                        xhr.send(data);
                    });
                } catch (error) {
                    Android.onError("请求错误: " + error.message);
                    throw error;
                }
            }
        """.trimIndent()
        
        // 注入SHA1函数、解析提供者和解析器
        val providerJs = readRawJsFile(R.raw.provider)
        val parserJs = readRawJsFile(R.raw.parser)
        
        // 组合脚本，添加启动执行逻辑
        val startScript = """
            async function startScheduleImport() {
                try {
                    Android.onProgress("正在初始化...");
                    
                    // 执行课表导入流程
                    Android.onProgress("调用scheduleHtmlProvider...");
                    let html = await scheduleHtmlProvider();
                    if (!html || html === "do not continue") {
                        Android.onError("无法获取课表数据，请检查网络连接或重新登录");
                        return;
                    }
                    
                    // 记录HTML长度，帮助调试
                    console.log("获取到HTML数据，长度: " + (html ? html.length : 0));
                    
                    Android.onProgress("正在解析课表数据...");
                    let result = scheduleHtmlParser(html);
                    
                    // 改进这里，确保记录实际的结果对象
                    console.log("解析后的结果: " + JSON.stringify(result));
                    
                    // 检查结果是否为数组
                    if (!Array.isArray(result)) {
                        console.error("解析结果不是数组: " + typeof result);
                        Android.onProgress("解析结果格式不正确: " + typeof result);
                        result = []; // 重置为空数组
                    }
                    
                    // 检查结果长度
                    console.log("课程数组长度: " + result.length);
                    
                    // 如果数组为空，尝试使用备用方法解析
                    if (result.length === 0) {
                        Android.onProgress("主解析方法未找到课程，尝试备用方法...");
                        // 备用方法
                        result = extractCoursesFromPage();
                        console.log("备用方法结果: " + JSON.stringify(result));
                    }
                    
                    // 检查数据结构
                    if (result.length > 0) {
                        console.log("第一个课程详情: " + JSON.stringify(result[0]));
                    }
                    
                    // 将结果发送回Android，确保是字符串
                    console.log("发送给Android的数据: " + JSON.stringify(result));
                    Android.onScheduleDataReceived(JSON.stringify(result));
                } catch (error) {
                    console.error("导入失败:", error);
                    Android.onError("导入失败: " + (error.message || "未知错误"));
                }
            }
            
            // 备用方法：尝试从页面直接提取课表
            function extractCoursesFromPage() {
                try {
                    Android.onProgress("正在尝试从页面元素提取课表...");
                    const courses = [];
                    
                    // 查找课表表格
                    const tableElement = document.querySelector('#kblist_table');
                    if (!tableElement) {
                        console.log("未找到课表表格");
                        return courses;
                    }
                    
                    console.log("找到课表表格元素");
                    
                    // 获取所有课程单元格
                    const cells = document.querySelectorAll('.kbcontent');
                    console.log("课程单元格数量: " + cells.length);
                    
                    if (cells && cells.length > 0) {
                        Android.onProgress("找到" + cells.length + "个课表格子，正在提取...");
                        
                        // 7列，5行是标准课表结构
                        cells.forEach((cell, index) => {
                            const cellHtml = cell.innerHTML;
                            const cellText = cell.textContent.trim();
                            
                            console.log("单元格 [" + index + "] 内容: " + JSON.stringify(cellText));
                            
                            if (cellText && cellText !== '' && cellText !== '&nbsp;') {
                                // 尝试解析这个单元格
                                let courseName = "";
                                let location = "";
                                let teacher = "";
                                let weeks = [];
                                
                                // 尝试不同的解析策略
                                // 1. 按换行符分割
                                const lines = cellText.split('\\n').map(l => l.trim()).filter(l => l);
                                console.log("单元格分行: " + JSON.stringify(lines));
                                
                                if (lines.length >= 1) {
                                    courseName = lines[0];
                                }
                                
                                if (lines.length >= 2) {
                                    // 第二行可能包含老师和地点
                                    const teacherLocationLine = lines[1];
                                    if (teacherLocationLine.includes('/')) {
                                        const parts = teacherLocationLine.split('/');
                                        teacher = parts[0].trim();
                                        location = parts[1].trim();
                                    } else {
                                        location = teacherLocationLine;
                                    }
                                }
                                
                                // 提取周次信息（通常在第三行或包含在HTML中）
                                if (lines.length >= 3) {
                                    const weekInfo = lines[2];
                                    // 简单处理，假设是1-16周这种格式
                                    const weekMatch = weekInfo.match(/(\\d+)-(\\d+)/);
                                    if (weekMatch) {
                                        const start = parseInt(weekMatch[1]);
                                        const end = parseInt(weekMatch[2]);
                                        for (let i = start; i <= end; i++) {
                                            weeks.push(i);
                                        }
                                    } else {
                                        // 默认1-16周
                                        for (let i = 1; i <= 16; i++) {
                                            weeks.push(i);
                                        }
                                    }
                                } else {
                                    // 默认1-16周
                                    for (let i = 1; i <= 16; i++) {
                                        weeks.push(i);
                                    }
                                }
                                
                                // 计算星期几（0-6表示周一到周日）
                                const day = (index % 7) + 1;
                                
                                // 计算节次（每行通常是连续的两节课）
                                const rowIndex = Math.floor(index / 7);
                                const startNode = rowIndex * 2 + 1;
                                const endNode = startNode + 1;
                                
                                // 创建课程对象
                                const course = {
                                    name: courseName,
                                    teacher: teacher,
                                    position: location,
                                    day: day,
                                    sections: [startNode, endNode],
                                    weeks: weeks
                                };
                                
                                console.log("解析出课程: " + JSON.stringify(course));
                                courses.push(course);
                            }
                        });
                    }
                    
                    console.log("通过表格提取到 " + courses.length + " 门课程");
                    return courses;
                } catch (e) {
                    console.error("备用提取方法错误:", e);
                    Android.onProgress("备用提取方法失败: " + e.message);
                    return [];
                }
            }
            
            // 捕获全局错误
            window.onerror = function(message, source, lineno, colno, error) {
                console.error("全局错误:", message, "at", source, ":", lineno);
                Android.onProgress("脚本错误: " + message + " 位置: " + source + ":" + lineno);
                return true;
            };
            
            // 在错误出现的地方添加try-catch
            window.addEventListener('unhandledrejection', function(event) {
                console.error("未处理的Promise拒绝:", event.reason);
                Android.onProgress("Promise错误: " + event.reason);
                event.preventDefault();
            });
            
            // 开始执行导入
            startScheduleImport();
        """.trimIndent()
        
        // 组合所有脚本
        val combinedScript = """
            $sharedFunctions
            
            $providerJs
            
            $parserJs
            
            $startScript
        """.trimIndent()
        
        // 执行脚本
        webView.evaluateJavascript(combinedScript, null)

        // 添加在脚本最后，确保解析结果发送回安卓
        webView.evaluateJavascript("""
            // 监听课表解析完成事件，当课表数据准备好时调用
            try {
                // 尝试不同方式获取课程数据
                function checkAndSendData() {
                    try {
                        console.log("正在检查课程数据...");
                        
                        // 检查全局变量中是否有课程数据
                        if (typeof coursesData !== 'undefined' && coursesData) {
                            console.log("从coursesData获取到课程数据: " + JSON.stringify(coursesData));
                            if (Array.isArray(coursesData) && coursesData.length > 0) {
                                console.log("从coursesData获取到" + coursesData.length + "门课程");
                                Android.onScheduleDataReceived(JSON.stringify(coursesData));
                                return true;
                            } else {
                                console.log("coursesData存在但为空或不是数组");
                            }
                        }
                        
                        // 尝试通过window对象获取解析对象
                        if (typeof window.kbData !== 'undefined' && window.kbData) {
                            console.log("从window.kbData获取到课程数据: " + JSON.stringify(window.kbData));
                            if (Array.isArray(window.kbData) && window.kbData.length > 0) {
                                console.log("从window.kbData获取到" + window.kbData.length + "门课程");
                                Android.onScheduleDataReceived(JSON.stringify(window.kbData));
                                return true;
                            } else {
                                console.log("window.kbData存在但为空或不是数组");
                            }
                        }
                        
                        // 查找全局变量中是否有解析结果
                        for (var key in window) {
                            if (Array.isArray(window[key]) && window[key].length > 0 && 
                                window[key][0] && typeof window[key][0] === 'object' && 
                                window[key][0].hasOwnProperty('name') && window[key][0].hasOwnProperty('day') && 
                                window[key][0].hasOwnProperty('sections')) {
                                console.log("从window." + key + "找到课程数据: " + JSON.stringify(window[key]));
                                console.log("从window." + key + "找到" + window[key].length + "门课程");
                                Android.onScheduleDataReceived(JSON.stringify(window[key]));
                                return true;
                            }
                        }
                        
                        // 尝试从DOM中提取课表数据
                        const scheduleTable = document.querySelector('#kblist_table');
                        if (scheduleTable) {
                            console.log("找到课表表格，尝试直接解析");
                            const courses = extractCoursesFromPage();
                            if (courses && courses.length > 0) {
                                console.log("直接从DOM提取到" + courses.length + "门课程");
                                Android.onScheduleDataReceived(JSON.stringify(courses));
                                return true;
                            }
                        }
                        
                        console.log("未找到有效的课程数据");
                        return false;
                    } catch (e) {
                        console.error("检查课程数据失败:", e);
                        return false;
                    }
                }
                
                // 添加一个直接捕获解析结果的方法
                window.saveAndSendCourseData = function(data) {
                    try {
                        console.log("尝试保存课程数据: " + JSON.stringify(data));
                        if (data && Array.isArray(data) && data.length > 0) {
                            console.log("保存课程数据: " + data.length + "门");
                            Android.onScheduleDataReceived(JSON.stringify(data));
                            return true;
                        } else {
                            console.log("传入的数据为空或不是数组");
                        }
                        return false;
                    } catch(e) {
                        console.error("保存课程数据失败:", e);
                        return false;
                    }
                };
                
                // 在原始解析代码后注入一个直接调用
                setTimeout(function() {
                    try {
                        console.log("开始检查课程数据...");
                        if (!checkAndSendData()) {
                            // 尝试直接通过eval获取结果
                            console.log("尝试通过eval获取课程数据");
                            const evalResult = new Function("try { return coursesData || window.coursesData || []; } catch(e) { return []; }")();
                            console.log("eval结果: " + JSON.stringify(evalResult));
                            
                            if (evalResult && Array.isArray(evalResult) && evalResult.length > 0) {
                                console.log("通过eval获取到" + evalResult.length + "门课程");
                                window.saveAndSendCourseData(evalResult);
                            } else {
                                Android.onProgress("无法找到课程数据，尝试手动点击课表链接");
                                
                                // 尝试查找并点击课表链接
                                const links = Array.from(document.querySelectorAll('a'));
                                const scheduleLinks = links.filter(link => 
                                    link.textContent.includes('课表') || 
                                    link.href.includes('course') || 
                                    link.href.includes('kbcx') ||
                                    link.href.includes('kcb')
                                );
                                
                                if (scheduleLinks.length > 0) {
                                    console.log("找到疑似课表链接: " + scheduleLinks.length + "个");
                                    Android.onProgress("找到课表链接，尝试点击...");
                                    // 点击第一个课表链接
                                    scheduleLinks[0].click();
                                    // 设置定时检查
                                    setTimeout(checkAndSendData, 2000);
                                } else {
                                    Android.onProgress("未找到课表链接，请手动导航到课表页面");
                                }
                            }
                        }
                    } catch(e) {
                        console.error("直接获取数据失败:", e);
                        Android.onProgress("查找课程数据出错: " + e.message);
                    }
                }, 1000);
                
                // 立即检查一次
                if (!checkAndSendData()) {
                    // 如果没有找到数据，设置定时检查
                    Android.onProgress("未立即找到课表数据，设置定时检查...");
                    var checkCount = 0;
                    var dataCheckInterval = setInterval(function() {
                        checkCount++;
                        console.log("第" + checkCount + "次检查课程数据");
                        if (checkAndSendData() || checkCount > 15) {
                            clearInterval(dataCheckInterval);
                            if (checkCount > 15) {
                                Android.onProgress("自动检查超时，请直接点击导入按钮，或手动导航到课表页面再导入");
                            }
                        }
                    }, 1000);
                }
            } catch(e) {
                console.error("发送课表数据失败:", e);
                Android.onProgress("发送课表数据失败: " + e.message);
            }
        """, null)
    }

    /**
     * 设置WebView
     */
    private fun setupWebView() {
        // 限制WebView内存使用
        val currentProcessDalvikHeapSize = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        if (currentProcessDalvikHeapSize > 512) {
            WebView.setDataDirectorySuffix("small_webview")
        }
        
        webView.settings.apply {
            // 基本设置
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadsImagesAutomatically = true
            
            // 禁用缓存
            cacheMode = WebSettings.LOAD_NO_CACHE
            
            // 视口设置
            useWideViewPort = true
            
            // 允许混合内容
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // 性能优化
            // 注意：部分WebView设置方法已在新版本Android中弃用，已移除：
            // - setRenderPriority
            // - setAppCacheEnabled
            // - setGeolocationEnabled
            // - setSaveFormData
            setJavaScriptCanOpenWindowsAutomatically(false)
            blockNetworkImage = false
            blockNetworkLoads = false
            
            // 内存优化
            setEnableSmoothTransition(false)
        }
        
        // 直接添加Activity作为JavaScript接口，提供所有 @JavascriptInterface 方法
        webView.addJavascriptInterface(this, "Android")
        
        // 设置WebView调试
        WebView.setWebContentsDebuggingEnabled(true)
        
        // 配置WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                messageTextView.text = "请先登录教务系统，进入用户照片页面即可，然后点击「导入」按钮开始导入课表，等待自动解析完成。"
                importBtn.isEnabled = true
                isWebViewLoaded = true
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                messageTextView.text = "页面加载出错: ${error?.description}"
                isWebViewLoaded = false
            }
        }
        
        // 加载初始URL
        webView.loadUrl(LOGIN_URL)
    }

    // 处理返回按钮
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // 读取原始JS文件
    private fun readRawJsFile(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }

    // 直接在 Activity 中实现 showSelectDialog
    @JavascriptInterface
    fun showSelectDialog(title: String, content: String, selectListJson: String) {
        runOnUiThread {
            try {
                // 记录原始JSON以便调试
                Log.d("SelectDialog", "原始JSON: $selectListJson")
                
                // 预处理JSON字符串，确保格式正确
                val fixedJson = if (!selectListJson.startsWith("[")) {
                    "[$selectListJson]" 
                } else {
                    selectListJson
                }
                
                // 尝试先用JSONArray解析
                var items = arrayOf<String>()
                try {
                    val selectList = JSONArray(fixedJson)
                    items = Array(selectList.length()) { i -> selectList.getString(i) }
                } catch (e: Exception) {
                    Log.e("SelectDialog", "JSONArray解析失败: ${e.message}", e)
                    
                    // 如果JSONArray解析失败，尝试其他方式解析
                    try {
                        // 尝试去除首尾的引号和方括号，然后按逗号分割
                        val cleanJson = selectListJson
                            .replace("[", "")
                            .replace("]", "")
                            .replace("\"", "")
                            .trim()
                        
                        // 按逗号分割，忽略空字符串
                        items = cleanJson.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toTypedArray()
                        
                        Log.d("SelectDialog", "手动解析结果: ${items.joinToString()}")
                    } catch (e2: Exception) {
                        Log.e("SelectDialog", "手动解析也失败: ${e2.message}", e2)
                    }
                }
                
                // 再次确保有选项
                if (items.isEmpty()) {
                    Log.e("SelectDialog", "没有学期选项，强制添加默认选项")
                    items = arrayOf(
                        "0:默认当前学期", 
                        "1:2023-2024学年第二学期", 
                        "2:2023-2024学年第一学期"
                    )
                }
                
                Log.d("SelectDialog", "最终解析的选项 (${items.size}): ${items.joinToString()}")
                
                // 确保UI更新在主线程
                android.app.AlertDialog.Builder(this@ScheduleImportActivity)
                    .setTitle(title)
                    .setMessage(content)
                    .setItems(items) { _, which ->
                        val selectedValue = items[which]
                        Log.d("SelectDialog", "用户选择了: $selectedValue")
                        // 注意转义，避免JS注入问题
                        val escapedValue = selectedValue.replace("'", "\\'")
                        webView.evaluateJavascript("window.resolveSelect('$escapedValue');", null)
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        dialog.cancel()
                        webView.evaluateJavascript("window.resolveSelect('0:默认当前学期');", null)
                    }
                    .show()
            } catch (e: Exception) {
                Log.e("SelectDialog", "对话框错误", e)
                // 直接调用 Activity 的 onError
                onError("选择对话框错误: ${e.message}")
                
                // 即使出错也要提供一个默认选项
                val defaultItems = arrayOf(
                    "0:默认当前学期", 
                    "1:2023-2024学年第二学期", 
                    "2:2023-2024学年第一学期"
                )
                android.app.AlertDialog.Builder(this@ScheduleImportActivity)
                    .setTitle("$title (出错后备选项)")
                    .setMessage("原始数据解析失败，使用默认选项")
                    .setItems(defaultItems) { _, which ->
                        Log.d("SelectDialog", "用户选择了备用选项: ${defaultItems[which]}")
                        val escapedValue = defaultItems[which].replace("'", "\\'")
                        webView.evaluateJavascript("window.resolveSelect('$escapedValue');", null)
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        dialog.cancel()
                        webView.evaluateJavascript("window.resolveSelect('0:默认当前学期');", null)
                    }
                    .show()
            }
        }
    }
    
    // 解析课表数据
    private fun parseScheduleData(jsonData: String): List<ScheduleData> {
        val schedules = mutableListOf<ScheduleData>()
        
        try {
            val jsonArray = JSONArray(jsonData)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                // 解析课程数据
                val name = jsonObject.getString("name")
                val position = jsonObject.optString("position", "")
                val teacher = jsonObject.optString("teacher", "")
                val day = jsonObject.getInt("day")
                
                // 解析上课节次
                val sectionsArray = jsonObject.getJSONArray("sections")
                val sections = mutableListOf<Int>()
                for (j in 0 until sectionsArray.length()) {
                    sections.add(sectionsArray.getInt(j))
                }
                
                val startNode = sections.minOrNull() ?: 1
                val endNode = sections.maxOrNull() ?: startNode
                
                // 解析周次
                val weeksArray = jsonObject.getJSONArray("weeks")
                val weeks = mutableListOf<Int>()
                for (j in 0 until weeksArray.length()) {
                    weeks.add(weeksArray.getInt(j))
                }
                
                val startWeek = weeks.minOrNull() ?: 1
                val endWeek = weeks.maxOrNull() ?: startWeek
                
                // 创建课程数据对象
                val schedule = ScheduleData(
                    name = name,
                    classroom = position,
                    teacher = teacher,
                    weekDay = day,
                    startNode = startNode,
                    endNode = endNode,
                    startWeek = startWeek,
                    endWeek = endWeek
                )
                
                schedules.add(schedule)
            }
        } catch (e: Exception) {
            throw Exception("解析课表数据失败: ${e.message}")
        }
        
        return schedules
    }
    
    // 添加新方法，用于接收并处理已解析的课表数据
    @JavascriptInterface
    fun onScheduleDataReceived(jsonString: String) {
        Log.d("ImportActivity", "收到课表数据: $jsonString")
        try {
            val schedules = parseScheduleData(jsonString)
            Log.d("ImportActivity", "解析到${schedules.size}门课程")
            
            // 将处理移到主线程
            runOnUiThread {
                try {
                    // 保存课程数据到数据库
                    val viewModel = ScheduleViewModel(application)
                    
                    // 清除旧的课程数据
                    viewModel.clearAllSchedules {
                        // 添加新课程数据
                        for (schedule in schedules) {
                            viewModel.addSchedule(schedule)
                        }
                        
                        importSuccess = true // 标记导入成功
                        messageTextView.text = "导入成功! 共导入了${schedules.size}门课程"
                        progressBar.visibility = android.view.View.GONE
                        
                        // 3秒后自动返回
                        handler.postDelayed({
                            // 仅在成功时返回 OK 并关闭
                            if (importSuccess) {
                                val intent = Intent()
                                intent.putExtra("IMPORTED_COURSES_COUNT", schedules.size)
                                setResult(RESULT_OK, intent)
                                finish()
                            }
                        }, 3000)
                    }
                } catch (e: Exception) {
                    Log.e("ImportActivity", "保存课表数据失败", e)
                    importSuccess = false // 标记导入失败
                    messageTextView.text = "导入失败: ${e.message}"
                    progressBar.visibility = android.view.View.GONE
                    importBtn.isEnabled = true
                    // 可选：设置 RESULT_CANCELED 并传递错误信息
                    // val errorIntent = Intent()
                    // errorIntent.putExtra("ERROR_MESSAGE", "保存课表数据失败: ${e.message}")
                    // setResult(RESULT_CANCELED, errorIntent)
                    // finish() // 或者直接结束，让用户看到错误信息
                }
            }
        } catch (e: Exception) {
            Log.e("ImportActivity", "解析课表数据失败", e)
            importSuccess = false // 标记导入失败
            // 在主线程更新UI
            runOnUiThread {
                messageTextView.text = "解析数据失败: ${e.message}"
                progressBar.visibility = android.view.View.GONE
                importBtn.isEnabled = true
                // 可选：设置 RESULT_CANCELED 并传递错误信息
                // val errorIntent = Intent()
                // errorIntent.putExtra("ERROR_MESSAGE", "解析数据失败: ${e.message}")
                // setResult(RESULT_CANCELED, errorIntent)
                // finish() // 或者直接结束，让用户看到错误信息
            }
        }
    }
    
    override fun onDestroy() {
        // 移除所有待处理的消息和回调，防止内存泄漏
        handler.removeCallbacksAndMessages(null)
        
        super.onDestroy()
        // 清理WebView资源，防止内存泄漏
        // 尝试捕获可能的 IllegalStateException
        try {
            if (webViewContainer != null && webView != null) {
                webViewContainer.removeView(webView)
            }
            webView?.stopLoading()
            webView?.settings?.javaScriptEnabled = false
            webView?.clearHistory()
            webView?.removeAllViews()
            webView?.destroy()
        } catch (e: Exception) {
            Log.e("ImportActivity", "销毁 WebView 时出错", e)
        } finally {
            // webView = null // 如果 webView 是可空类型，可以设为 null
        }
        
        // 主动调用GC回收内存
        System.gc()
    }

    // 直接在 Activity 中实现 onProgress
    @JavascriptInterface
    fun onProgress(message: String) {
        runOnUiThread {
            messageTextView.text = message
        }
    }

    // 确保 onError 方法存在并正确标记
    @JavascriptInterface
    fun onError(error: String) {
        runOnUiThread {
            Log.e("ImportActivity", "JavaScript Error: $error") // 添加日志
            progressBar.visibility = android.view.View.GONE
            messageTextView.text = "导入失败：$error"
            importBtn.isEnabled = true
            importSuccess = false // 确保标记为失败
        }
    }
} 