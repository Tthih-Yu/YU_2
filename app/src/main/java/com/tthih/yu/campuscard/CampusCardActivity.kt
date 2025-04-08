package com.tthih.yu.campuscard

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.tthih.yu.ui.theme.YUTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.tthih.yu.campuscard.CardInfoDisplay
import com.tthih.yu.campuscard.MonthBillSummary
import com.tthih.yu.campuscard.TransactionList
import com.tthih.yu.campuscard.TransactionItem
import com.tthih.yu.campuscard.ConsumeTrendDisplay
import android.webkit.WebSettings
import androidx.compose.material3.IconButton

class CampusCardActivity : ComponentActivity() {
    private lateinit var viewModel: CampusCardViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[CampusCardViewModel::class.java]
        
        // 初始化仓库
        viewModel.initializeRepository(this)
        
        // 尝试从本地数据库加载之前的数据
        viewModel.loadCachedTransactions()
        
        setContent {
            YUTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CampusCardScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusCardScreen(viewModel: CampusCardViewModel) {
    // 使用remember mutableStateOf来管理状态
    var transactions by remember { mutableStateOf(emptyList<CampusCardTransaction>()) }
    var isLoading by remember { mutableStateOf(true) }
    var loginStatus by remember { mutableStateOf("") }
    var cardInfo by remember { mutableStateOf<CardInfo?>(null) }
    var monthBill by remember { mutableStateOf<MonthBill?>(null) }
    
    // 添加标签页状态
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("本月账单", "账单明细", "消费趋势")
    val coroutineScope = rememberCoroutineScope()
    
    // 添加变量控制是否允许自动关闭WebView
    var allowAutoClose by remember { mutableStateOf(true) }
    
    // 使用LaunchedEffect订阅LiveData
    DisposableEffect(Unit) {
        val transactionObserver = Observer<List<CampusCardTransaction>> { data ->
            transactions = data
        }
        val loadingObserver = Observer<Boolean> { loading ->
            isLoading = loading
        }
        val statusObserver = Observer<String> { status ->
            loginStatus = status
        }
        val cardInfoObserver = Observer<CardInfo?> { info ->
            cardInfo = info
        }
        val monthBillObserver = Observer<MonthBill?> { bill ->
            monthBill = bill
        }
        
        viewModel.transactionData.observeForever(transactionObserver)
        viewModel.isLoading.observeForever(loadingObserver)
        viewModel.loginStatus.observeForever(statusObserver)
        viewModel.cardInfo.observeForever(cardInfoObserver)
        viewModel.monthBill.observeForever(monthBillObserver)
        
        onDispose {
            viewModel.transactionData.removeObserver(transactionObserver)
            viewModel.isLoading.removeObserver(loadingObserver)
            viewModel.loginStatus.removeObserver(statusObserver)
            viewModel.cardInfo.removeObserver(cardInfoObserver)
            viewModel.monthBill.removeObserver(monthBillObserver)
        }
    }
    
    var webViewVisible by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("校园卡") },
                actions = {
                    // 当WebView可见且不允许自动关闭时，显示返回按钮
                    if (webViewVisible && !allowAutoClose) {
                        IconButton(onClick = {
                            // 手动关闭WebView，返回主界面
                            webViewVisible = false
                            allowAutoClose = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 触发导航到登录界面的操作
                    viewModel.prepareForRelogin()
                    webViewVisible = true
                    allowAutoClose = false
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loginStatus.isNotEmpty()) {
                Text(
                    text = loginStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 校园卡信息卡片
            cardInfo?.let { info ->
                CardInfoDisplay(cardInfo = info)
            }
            
            // 账单导航标签
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { 
                            selectedTabIndex = index 
                            // 如果选择了账单明细，确保已加载数据
                            if (index == 1 && webViewVisible && transactions.isEmpty()) {
                                coroutineScope.launch {
                                    // 强制加载数据
                                    webViewVisible = false
                                }
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }
            
            if (isLoading && webViewVisible) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // 根据选中的标签页显示不同内容
            Box(modifier = Modifier.weight(1f)) {
                if (webViewVisible) {
                    CampusCardWebView(
                        viewModel = viewModel,
                        onDataLoaded = { webViewVisible = false },
                        allowAutoClose = allowAutoClose
                    )
                } else {
                    when (selectedTabIndex) {
                        0 -> {
                            // 本月账单
                            monthBill?.let { bill ->
                                Column {
                                    MonthBillSummary(bill = bill)
                                    
                                    if (transactions.isNotEmpty()) {
                                        // 仅显示最近5条记录
                                        val recentTransactions: List<CampusCardTransaction> = transactions.take(5)
                                        TransactionList(transactions = recentTransactions)
                                    } else {
                                        Text(
                                            text = "暂无交易数据",
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } ?: run {
                                if (!isLoading) {
                                    Text(
                                        text = "暂无账单数据",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                        1 -> {
                            // 账单明细 - 显示完整交易记录
                            if (transactions.isNotEmpty()) {
                                val allTransactions: List<CampusCardTransaction> = transactions.toList()
                                TransactionList(transactions = allTransactions)
                            } else {
                                Text(
                                    text = "暂无交易数据",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        2 -> {
                            // 消费趋势
                            ConsumeTrendDisplay(viewModel.trends.value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CampusCardWebView(
    viewModel: CampusCardViewModel,
    onDataLoaded: () -> Unit,
    allowAutoClose: Boolean
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    
    // 当组件重新显示时，重新加载登录页面
    LaunchedEffect(Unit) {
        webView?.let { view ->
            view.loadUrl("http://ehall.ahpu.edu.cn/new/index.html?browser=no")
        }
    }
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // WebView配置
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    
                    // 必要的额外设置
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    
                    // 允许混合内容
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                
                // 清除所有Cookie和缓存，确保重新登录
                CookieManager.getInstance().removeAllCookies(null)
                clearCache(true)
                
                // 设置WebViewClient
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        view.loadUrl(request.url.toString())
                        return true
                    }
                    
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        
                        when {
                            url.contains("ehall.ahpu.edu.cn/new/index.html") -> {
                                viewModel.updateLoginStatus("登录成功，正在进入校园卡页面...")
                                view.loadUrl("http://ehall.ahpu.edu.cn/publicapp/sys/myyktzd/index.do#/statement")
                            }
                            url.contains("myyktzd/index.do") -> {
                                viewModel.updateLoginStatus("正在获取校园卡数据...")
                                // 延迟注入脚本，等待页面完全加载
                                view.postDelayed({
                                    @Suppress("UNUSED_EXPRESSION", "JSUnresolvedVariable", "JSUnresolvedFunction")
                                    // 定义JavaScript代码字符串
                                    val jsCode = """
                                        (function() {
                                            // 获取校园卡主要信息
                                            function getCardMainInfo() {
                                                try {
                                                    // 直接从页面获取卡片信息
                                                    const cardNumber = document.querySelector('.bh-form-label + span')?.textContent.trim() || '';
                                                    const balance = parseFloat(document.querySelector('.bh-tag-primary')?.textContent.trim() || '0');
                                                    const expiryDate = document.querySelectorAll('.bh-form-label + span')[1]?.textContent.trim() || '';
                                                    const cardStatus = document.querySelectorAll('.bh-form-label + span')[2]?.textContent.trim() || '';
                                                    
                                                    const cardInfo = {
                                                        status: 200,
                                                        cardNumber: cardNumber,
                                                        balance: balance,
                                                        expiryDate: expiryDate,
                                                        cardStatus: cardStatus
                                                    };
                                                    
                                                    window.CampusCardInterface.onCardInfoReceived(JSON.stringify(cardInfo));
                                                } catch(e) {
                                                    window.CampusCardInterface.onError("从页面提取卡片信息失败: " + e.message);
                                                }
                                                
                                                // 通过API获取更多卡片信息
                                                fetchData('/publicapp/sys/myyktzd/mySmartCard/loadSmartCardBillMain.do', {}, 
                                                    function(response) {
                                                        window.CampusCardInterface.onCardInfoReceived(JSON.stringify(response));
                                                    }, 
                                                    function(error) {
                                                        window.CampusCardInterface.onError("获取卡片信息失败: " + error);
                                                    }
                                                );
                                            }
                                            
                                            // 获取账单详情
                                            function getBillDetails() {
                                                try {
                                                    // 获取当前选择的年月
                                                    const yearMonthSelect = document.getElementById('statementYMSelect');
                                                    let yearMonth;
                                                    
                                                    if (yearMonthSelect && yearMonthSelect.value) {
                                                        yearMonth = yearMonthSelect.value;
                                                    } else {
                                                        // 默认使用当前月份
                                                        const today = new Date();
                                                        const year = today.getFullYear();
                                                        const month = today.getMonth() + 1;
                                                        const formattedMonth = month < 10 ? '0' + month : month;
                                                        yearMonth = year + '-' + formattedMonth;
                                                    }
                                                    
                                                    // 1. 先获取当月账单汇总
                                                    fetchData('/publicapp/sys/myyktzd/mySmartCard/getCurrentMonthBill.do', 
                                                        { yearMonth: yearMonth },
                                                        function(response) {
                                                            window.CampusCardInterface.onMonthBillReceived(JSON.stringify(response));
                                                            
                                                            // 2. 准备获取账单明细
                                                            prepareDetailTable();
                                                        },
                                                        function(error) {
                                                            window.CampusCardInterface.onError("获取当月账单失败: " + error);
                                                            // 仍然尝试获取明细
                                                            prepareDetailTable();
                                                        }
                                                    );
                                                } catch(e) {
                                                    window.CampusCardInterface.onError("获取账单明细失败: " + e.message);
                                                    prepareDetailTable();
                                                }
                                            }
                                            
                                            // 准备账单明细表格并获取数据
                                            function prepareDetailTable() {
                                                // 先点击明细菜单项，确保相关JS初始化
                                                const detailTab = document.querySelector('.bh-menu-link-item[menuid="detail"]');
                                                if (detailTab) {
                                                    detailTab.click();
                                                    
                                                    // 等待表格初始化
                                                    setTimeout(function() {
                                                        // 1. 尝试通过触发搜索按钮获取数据
                                                        const searchButton = document.querySelector('#advancedQueryPlaceholder .bh-btn-primary');
                                                        if (searchButton) {
                                                            searchButton.click();
                                                            
                                                            // 等待搜索结果
                                                            setTimeout(extractTableData, 1500);
                                                        } else {
                                                            // 2. 如果没有搜索按钮，直接尝试获取数据
                                                            extractTableData();
                                                        }
                                                    }, 1000);
                                                } else {
                                                    // 无法找到明细标签，直接尝试API请求
                                                    extractTableData();
                                                }
                                            }
                                            
                                            // 从表格提取数据
                                            function extractTableData() {
                                                try {
                                                    // 先看能否从表格直接提取
                                                    const tableRows = document.querySelectorAll('#scholarshipManageTable .jqx-grid-cell');
                                                    if (tableRows && tableRows.length > 0) {
                                                        extractTableViewData();
                                                    } else {
                                                        // 否则直接调用API获取
                                                        getBillDetailFromAPI();
                                                    }
                                                } catch(e) {
                                                    window.CampusCardInterface.onError("表格数据提取失败: " + e.message);
                                                    getBillDetailFromAPI();
                                                }
                                            }
                                            
                                            // 直接从表格视图提取数据
                                            function extractTableViewData() {
                                                try {
                                                    // 分析表格结构
                                                    const rows = [];
                                                    const grid = $('#scholarshipManageTable').data().jqxGrid;
                                                    
                                                    if (grid) {
                                                        const data = grid.source.records || [];
                                                        
                                                        for (let i = 0; i < data.length; i++) {
                                                            const row = data[i];
                                                            
                                                            // 提取需要的字段
                                                            const transaction = {
                                                                id: row['JYLSH'] || row['jylsh'] || '',
                                                                time: row['JYSJ'] || row['jysj'] || '',
                                                                amount: parseFloat(row['JYJE'] || row['jyje'] || 0),
                                                                balance: parseFloat(row['ZHYE'] || row['zhye'] || 0),
                                                                type: row['JYLX'] || row['jylx'] || '',
                                                                location: row['ZDMC'] || row['zdmc'] || '',
                                                                description: row['JYMC'] || row['jymc'] || ''
                                                            };
                                                            
                                                            rows.push(transaction);
                                                        }
                                                        
                                                        if (rows.length > 0) {
                                                            const result = {
                                                                source: "page-extraction",
                                                                rows: rows
                                                            };
                                                            
                                                            window.CampusCardInterface.onTransactionDataReceived(JSON.stringify(result));
                                                            
                                                            // 继续获取消费趋势
                                                            getTrendsData();
                                                            return;
                                                        }
                                                    }
                                                    
                                                    // 如果无法从jqxGrid获取数据，尝试直接从DOM解析
                                                    const tableRows = document.querySelectorAll('#scholarshipManageTable .jqx-grid-content table tr');
                                                    if (tableRows && tableRows.length > 0) {
                                                        const rows = [];
                                                        
                                                        // 找出表头，确定各列的含义
                                                        const headerCells = document.querySelectorAll('#scholarshipManageTable .jqx-grid-columnheader-container div');
                                                        const headers = [];
                                                        for (let i = 0; i < headerCells.length; i++) {
                                                            headers.push(headerCells[i].textContent.trim());
                                                        }
                                                        
                                                        // 从表格行提取数据
                                                        for (let i = 0; i < tableRows.length; i++) {
                                                            const cells = tableRows[i].querySelectorAll('td');
                                                            if (cells.length === 0) continue;
                                                            
                                                            const row = {};
                                                            for (let j = 0; j < Math.min(cells.length, headers.length); j++) {
                                                                row[headers[j]] = cells[j].textContent.trim();
                                                            }
                                                            
                                                            const transaction = {
                                                                id: row['交易流水号'] || row['JYLSH'] || '',
                                                                time: row['交易时间'] || row['JYSJ'] || '',
                                                                amount: parseFloat(row['交易金额'] || row['JYJE'] || 0),
                                                                balance: parseFloat(row['账户余额'] || row['ZHYE'] || 0),
                                                                type: row['交易类型'] || row['JYLX'] || '',
                                                                location: row['终端名称'] || row['ZDMC'] || '',
                                                                description: row['交易名称'] || row['JYMC'] || ''
                                                            };
                                                            
                                                            rows.push(transaction);
                                                        }
                                                        
                                                        if (rows.length > 0) {
                                                            const result = {
                                                                source: "page-extraction",
                                                                rows: rows
                                                            };
                                                            
                                                            window.CampusCardInterface.onTransactionDataReceived(JSON.stringify(result));
                                                            
                                                            // 继续获取消费趋势
                                                            getTrendsData();
                                                            return;
                                                        }
                                                    }
                                                    
                                                    // 如果都失败了，尝试API
                                                    getBillDetailFromAPI();
                                                } catch(e) {
                                                    window.CampusCardInterface.onError("表格数据提取异常: " + e.message);
                                                    getBillDetailFromAPI();
                                                }
                                            }
                                            
                                            // 通过API获取账单明细
                                            function getBillDetailFromAPI() {
                                                // 构造请求参数 - 包含当前年月和更长的时间范围
                                                const today = new Date();
                                                const year = today.getFullYear();
                                                const month = today.getMonth() + 1;
                                                const day = today.getDate();
                                                const endDate = formatDate(today);
                                                
                                                // 获取30天前的日期
                                                const startDate = new Date();
                                                startDate.setDate(startDate.getDate() - 30);
                                                
                                                const params = {
                                                    querySetting: JSON.stringify([
                                                        { name: "date", value: "30" },  // 近30天
                                                        { name: "consumeType", value: "all" }  // 所有类型
                                                    ])
                                                };
                                                
                                                fetchData('/publicapp/sys/myyktzd/mySmartCard/getBillDetail.do', 
                                                    params,
                                                    function(response) {
                                                        if (response && (response.rows || response.datas)) {
                                                            window.CampusCardInterface.onTransactionDataReceived(JSON.stringify(response));
                                                        } else {
                                                            window.CampusCardInterface.onError("账单明细API返回格式异常");
                                                        }
                                                        
                                                        // 继续获取消费趋势
                                                        getTrendsData();
                                                    },
                                                    function(error) {
                                                        window.CampusCardInterface.onError("获取账单明细API失败: " + error);
                                                        getTrendsData();
                                                    }
                                                );
                                            }
                                            
                                            // 获取消费趋势数据
                                            function getTrendsData() {
                                                // 先点击回本月账单菜单项
                                                const statementTab = document.querySelector('.bh-menu-link-item[menuid="statement"]');
                                                if (statementTab) {
                                                    statementTab.click();
                                                }
                                                
                                                // 调用API获取消费趋势
                                                fetchData('/publicapp/sys/myyktzd/mySmartCard/getConsumerTrendsData.do', 
                                                    {},
                                                    function(response) {
                                                        window.CampusCardInterface.onTrendsReceived(JSON.stringify(response));
                                                        // 所有数据获取完成
                                                        finishDataCollection();
                                                    },
                                                    function(error) {
                                                        window.CampusCardInterface.onError("获取消费趋势失败: " + error);
                                                        finishDataCollection();
                                                    }
                                                );
                                            }
                                            
                                            // 通用的数据请求函数
                                            function fetchData(url, params, onSuccess, onError) {
                                                try {
                                                    var xhr = new XMLHttpRequest();
                                                    xhr.open('POST', url, true);
                                                    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                                                    xhr.timeout = 10000; // 10秒超时
                                                    
                                                    xhr.onreadystatechange = function() {
                                                        if (xhr.readyState === 4) {
                                                            if (xhr.status === 200) {
                                                                try {
                                                                    var response = JSON.parse(xhr.responseText);
                                                                    onSuccess(response);
                                                                } catch(e) {
                                                                    onError("响应解析失败: " + e.message);
                                                                }
                                                            } else {
                                                                onError("HTTP错误: " + xhr.status);
                                                            }
                                                        }
                                                    };
                                                    
                                                    xhr.ontimeout = function() {
                                                        onError("请求超时");
                                                    };
                                                    
                                                    xhr.onerror = function() {
                                                        onError("网络错误");
                                                    };
                                                    
                                                    // 转换参数为URL编码格式
                                                    var query = '';
                                                    for (var key in params) {
                                                        if (params.hasOwnProperty(key)) {
                                                            if (query.length) query += '&';
                                                            query += encodeURIComponent(key) + '=' + encodeURIComponent(params[key]);
                                                        }
                                                    }
                                                    
                                                    xhr.send(query);
                                                } catch(e) {
                                                    onError("请求发送失败: " + e.message);
                                                }
                                            }
                                            
                                            // 日期格式化辅助函数
                                            function formatDate(date) {
                                                const year = date.getFullYear();
                                                const month = (date.getMonth() + 1).toString().padStart(2, '0');
                                                const day = date.getDate().toString().padStart(2, '0');
                                                return year + '-' + month + '-' + day;
                                            }
                                            
                                            // 完成数据收集
                                            function finishDataCollection() {
                                                // 通知应用数据已加载完成
                                                window.CampusCardInterface.onAllDataLoaded();
                                            }
                                            
                                            // 主执行流程
                                            // 1. 先获取卡片基本信息
                                            getCardMainInfo();
                                            
                                            // 2. 获取账单信息
                                            setTimeout(getBillDetails, 1000);
                                        })();
                                    """
                                    
                                    // 执行JavaScript
                                    view.evaluateJavascript(jsCode, null)
                                }, 1000)
                            }
                        }
                    }
                }
                
                // 添加JavaScript接口
                addJavascriptInterface(object : Any() {
                    @JavascriptInterface
                    fun onCardInfoReceived(jsonData: String) {
                        viewModel.processCardInfo(jsonData)
                    }
                    
                    @JavascriptInterface
                    fun onTransactionDataReceived(jsonData: String) {
                        viewModel.processTransactionData(jsonData)
                    }
                    
                    @JavascriptInterface
                    fun onMonthBillReceived(jsonData: String) {
                        viewModel.processMonthBill(jsonData)
                    }
                    
                    @JavascriptInterface
                    fun onTrendsReceived(jsonData: String) {
                        viewModel.processTrends(jsonData)
                    }
                    
                    @JavascriptInterface
                    fun onError(message: String) {
                        viewModel.updateLoginStatus(message)
                    }
                    
                    @JavascriptInterface
                    fun onAllDataLoaded() {
                        viewModel.setDataLoaded(true)
                        // 只有当允许自动关闭时才关闭WebView
                        if (allowAutoClose) {
                            onDataLoaded()
                        }
                    }
                }, "CampusCardInterface")
                
                // 开始登录流程
                loadUrl("http://ehall.ahpu.edu.cn/new/index.html?browser=no")
                
                webView = this
            }
        }
    )
} 