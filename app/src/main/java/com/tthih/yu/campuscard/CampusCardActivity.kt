package com.tthih.yu.campuscard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.* // Import necessary layout components
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh // Add dependency
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState // Add dependency
import com.tthih.yu.ui.theme.YUTheme
import java.util.* // For Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entriesOf
import com.patrykandpatrick.vico.core.entry.ChartEntry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.ScrollState // Import needed for verticalScroll
import androidx.compose.foundation.rememberScrollState // Import needed for verticalScroll
import androidx.compose.foundation.verticalScroll // Import needed for verticalScroll
import androidx.compose.foundation.layout.heightIn // Import needed for height constraint
import androidx.compose.ui.text.style.TextAlign // <-- Added Import for potential text alignment
import androidx.compose.foundation.horizontalScroll // <-- Add this import

class CampusCardActivity : ComponentActivity() {
    private lateinit var viewModel: CampusCardViewModel
    
    companion object {
        const val REQUEST_LOGIN = 1001
    }
    
    // New Activity Result Launcher
    private val loginActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.i("CampusCardActivity", "Login successful via CampusCardLoginActivity. Refreshing data.")
            // Login activity handled cookie storage.
            // ViewModel state should update, potentially clearing requiresLogin.
            // Refresh might be needed if ViewModel doesn't auto-refresh on successful auth state change.
            viewModel.refreshAllData(forceLogin = false)
        } else {
            Log.w("CampusCardActivity", "Login attempt finished with result code: ${result.resultCode}")
            // Optionally show a message if login was cancelled or failed, maybe using the snackbar
             // If login is still required, the effect in CampusCardScreen will trigger again.
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[CampusCardViewModel::class.java]
        viewModel.initialize(this) // Pass context
        
        setContent {
            YUTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass the launcher to the composable
                    CampusCardScreen(viewModel, onLoginRequired = {
                         val intent = Intent(this, CampusCardLoginActivity::class.java)
                         loginActivityResultLauncher.launch(intent)
                    })
                }
            }
        }
    }
}

@Composable
fun CampusCardVisual(
    cardInfo: CardInfo,
    currentMonthExpense: Double = 0.0,
    totalExpense: Double = 0.0
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp) // 增加高度以容纳更多信息
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Use transparent container for gradient background
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Example Gradient - Adjust colors as needed for realism
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween // Space out content
            ) {
                // Top Row: Logo/Icon and Card Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "校园一卡通", // Card Type
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.CreditCard, // Using a standard icon from Filled package
                        contentDescription = "校园卡标识",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Middle: Card Number (Masked)
                Text(
                    text = cardInfo.cardNumber.maskCardNumber(), // Use extension function
                    style = TextStyle( // Custom style for card number
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        letterSpacing = 2.sp, // Add spacing between digits
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(vertical = 4.dp) // Reduce vertical padding
                )

                // 消费信息区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "当月消费",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "¥ ${currentMonthExpense.format(2)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column {
                        Text(
                            text = "总消费金额",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "¥ ${totalExpense.format(2)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bottom Row: Status and Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, // Changed to SpaceBetween
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "卡片状态",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = cardInfo.status,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "当前余额",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "¥ ${cardInfo.balance.format(2)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper extension function to mask card number (Place it at the bottom of the file or in a utils file)
fun String?.maskCardNumber(visibleDigits: Int = 4): String {
    if (this == null) return "**** **** **** ****"
    val cleaned = this.replace(Regex("[^0-9]"), "") // Remove non-digits
    if (cleaned.length <= visibleDigits) return cleaned

    val maskedPart = "*".repeat(cleaned.length - visibleDigits)
    val visiblePart = cleaned.takeLast(visibleDigits)

    // Insert spaces for better readability (e.g., **** **** **** 1234)
    val combined = maskedPart + visiblePart
    return combined.chunked(4).joinToString(" ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusCardScreen(
    viewModel: CampusCardViewModel,
    onLoginRequired: () -> Unit // Callback to launch login
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showTutorialDialog by remember { mutableStateOf(false) } // <-- New state for tutorial dialog
    var showClearConfirmDialog by remember { mutableStateOf(false) } // <-- New state for clear confirmation dialog
    
    // State for Login Dialog - REMOVE
    // var showLoginDialog by remember { mutableStateOf(false) }
    
    // Trigger login activity launch when required
    LaunchedEffect(uiState.requiresLogin) {
        if (uiState.requiresLogin) {
            // showLoginDialog = true // REMOVE
            onLoginRequired() // Call the lambda to launch the activity
        }
    }
    
    // Show error messages in Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            // Optionally clear the error message in ViewModel after showing
        }
    }

    // Trigger loading more transactions when reaching the end of the list
    LaunchedEffect(listState, uiState.transactions) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItemIndex = visibleItems.lastOrNull()?.index
                if (uiState.transactions.isNotEmpty() &&
                    lastVisibleItemIndex != null && 
                    lastVisibleItemIndex >= uiState.transactions.size - 5 && // Trigger near the end
                    !uiState.isLoading && 
                    !uiState.isFetchingMore) { 
                    Log.d("CampusCardScreen", "Triggering fetchMoreTransactions - lastVisible: $lastVisibleItemIndex, size: ${uiState.transactions.size}")
                    viewModel.fetchMoreTransactions()
                }
            }
    }
    
    // Vico Chart Entry Model Producer for daily spending
    val dailySpendingEntryProducer = remember { ChartEntryModelProducer() }

    // Update the producer when data changes in uiState
    LaunchedEffect(uiState.dailySpendingData) {
        uiState.dailySpendingData?.let {
            dailySpendingEntryProducer.setEntries(it)
        } ?: dailySpendingEntryProducer.setEntries(emptyList<ChartEntry>()) // Specify type explicitly
    }
    
    // 日期格式化器
    val dateFormatWithSeconds = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val dateFormatWithoutSeconds = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // 计算当月消费金额 (已存在)
    val currentMonthExpense = remember(uiState.transactions) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val currentMonthStr = "${year}-${String.format("%02d", month)}"
        uiState.transactions
            .filter { transaction -> 
                if (transaction.amount >= 0) return@filter false
                try {
                    val date = try { dateFormatWithSeconds.parse(transaction.time) } catch (e: Exception) { try { dateFormatWithoutSeconds.parse(transaction.time) } catch (e: Exception) { null } }
                    date != null && monthFormat.format(date) == currentMonthStr
                } catch (e: Exception) { false }
            }
            .sumOf { -it.amount }
    }

    // 新增：计算本月账单数据 (MonthBill)
    val currentMonthBill = remember(uiState.transactions) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val currentMonthStr = "${year}-${String.format("%02d", month)}"
        var income = 0.0
        var expense = 0.0
        uiState.transactions.forEach { transaction ->
            try {
                val date = try { dateFormatWithSeconds.parse(transaction.time) } catch (e: Exception) { try { dateFormatWithoutSeconds.parse(transaction.time) } catch (e: Exception) { null } }
                if (date != null && monthFormat.format(date) == currentMonthStr) {
                    if (transaction.amount > 0) {
                        income += transaction.amount
                    } else {
                        expense += -transaction.amount // Use positive value for expense
                    }
                }
            } catch (e: Exception) { /* Ignore parsing errors */ }
        }
        MonthBill(totalAmount = expense, inAmount = income)
    }

    // 新增：计算消费趋势数据 (ConsumeTrend for last 30 days)
    /*
    val recentConsumeTrend = remember(uiState.transactions) {
        // ... (previous calculation logic)
    }
    */
    
    // --- Tutorial Dialog ---
    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = { Text("使用教程") },
            text = {
                // Make tutorial content scrollable
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        """
                        欢迎使用此功能！强烈建议先阅读此教程

                        基本操作：
                        - 余额和状态：卡片上方会显示您的校园卡余额（不准）和当前状态。
                        - 账单/趋势：下方标签页可切换查看本月账单汇总、详细交易记录、消费趋势和图表分析。
                        - 刷新：在屏幕任意位置向下滑动或者点击右上方刷新按钮。
                        - 加载更多：在"账单明细"标签页，滚动到底部会自动加载更多历史交易。

                        特别说明：
                        - 目前只需要登陆后就可多次刷新，数据延迟属于正常情况
                        - 但是若出现未知问题，建议先清理数据后再重新登陆一次
                        - 累计登陆多人账号会出现数据重叠，建议点击清理数据

                        账号-密码-卡号说明：
                        - 登陆过程中的账号就是你的学号
                        - 登陆过程中的密码是你的《小灵龙》APP的登陆密码，也是查询密码，不是教务系统的密码
                        - 卡号不是学号，是'主副卡转账'中的主卡账号

                        图表分析：
                        - 点击顶部的图表名称（如"每日消费"）可以切换不同的统计图表。
                        - 图表会根据您已加载的交易数据生成。

                        常见问题：
                        - 数据不更新？请尝试下拉刷新或点击刷新按钮。如果长时间无响应，请检查网络连接或尝试重新登录。
                        - 本功能有一个更完善的开源版本，基于Python本地运行，又能力者可以尝试
                        - 开源链接：https://github.com/Tthih-Yu/AHPU-
                        - 或者 cmd 输入：git clone git@github.com:Tthih-Yu/AHPU-.git  回车
                        -
                        - 

                        祝您使用愉快！若有问题加QQ群 747802286
                        """.trimIndent(),
                        fontSize = 14.sp // Adjust font size if needed
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorialDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }
    // --- End Tutorial Dialog ---
    
    // --- Clear Cache Confirmation Dialog ---
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = "清除图标") },
            title = { Text("确认清除数据？") },
            text = { Text("这将永久删除本地存储的所有校园卡交易记录和相关缓存数据。此操作不可恢复，确认继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache() // <-- Call ViewModel to clear cache
                        showClearConfirmDialog = false
                        // Optionally show a snackbar message using snackbarHostState and LaunchedEffect/rememberCoroutineScope
                    }
                ) {
                    Text("确认清除", color = MaterialTheme.colorScheme.error) // Make confirm button red
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    // --- End Clear Cache Confirmation Dialog ---
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("校园卡") },
                actions = {
                     // Added Clear button
                    IconButton(onClick = { showClearConfirmDialog = true }) { // <-- Opens the confirmation dialog
                        Icon(Icons.Filled.DeleteForever, contentDescription = "清理缓存")
                    }
                    // Tutorial button
                    IconButton(onClick = { showTutorialDialog = true }) { 
                        Icon(Icons.Filled.HelpOutline, contentDescription = "使用教程") 
                    }
                     // Account button
                        IconButton(onClick = {
                        onLoginRequired() 
                    }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "登录/设置账号")
                    }
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshAllData(forceLogin = false) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isLoading),
            onRefresh = { viewModel.refreshAllData(forceLogin = false) },
            modifier = Modifier.padding(paddingValues)
        ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
                // Display Simulated Campus Card
                uiState.cardInfo?.let { info ->
                    CampusCardVisual(
                        cardInfo = info,
                        currentMonthExpense = currentMonthExpense,
                        totalExpense = uiState.transactions.filter { it.amount < 0 }.sumOf { -it.amount }
                    ) 
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Placeholder height matching the card
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                    ) { Text("校园卡信息加载中...") } // Placeholder content
                
                // Tabs for different views
                var selectedTabIndex by remember { mutableStateOf(0) }
                val tabTitles = listOf("本月账单", "账单明细", "消费趋势", "图表分析")
                
            TabRow(
                selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
             // --- Display Progress Bar and Text during multi-page fetch ---
             val currentProgress = uiState.loadingProgress // Capture the potentially null value
             val progressText = uiState.loadingProgressText // Capture the text
             if (uiState.isLoading && currentProgress != null) { // Check the captured value
                 Column( // Use a Column to stack the bar and text
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(horizontal = 16.dp, vertical = 4.dp),
                     horizontalAlignment = Alignment.CenterHorizontally
                 ) {
                     LinearProgressIndicator(
                         progress = { currentProgress / 100f }, // Use the captured non-null value
                         modifier = Modifier
                             .fillMaxWidth()
                             .height(6.dp) // Adjust height as needed
                     )
                     Spacer(modifier = Modifier.height(4.dp))
                     Text(
                         // Display text like "正在获取 5 / 50 页... (10%)"
                         text = "${progressText ?: ""} (${currentProgress}%)", 
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
                // Content based on selected tab
            Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> {
                            // 本月账单
                            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("无交易记录")
                                }
                            } else {
                                // 添加滚动修饰符
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    MonthBillSummary(monthBill = currentMonthBill)
                                }
                            }
                        }
                        1 -> {
                            // 账单明细
                            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("无交易记录")
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 状态管理 - 提升到更高级别的作用域
                                    var expanded by remember { mutableStateOf(false) }
                                    var selectedMonth by remember { mutableStateOf("全部") }
                                    var showMonthDropdown by remember { mutableStateOf(false) }
                                    var selectedType by remember { mutableStateOf("全部") }
                                    var showTypeDropdown by remember { mutableStateOf(false) }
                                    
                                    // 获取可用月份列表（从交易数据中提取）
                                    val availableMonths = remember(uiState.transactions) {
                                        val months = mutableSetOf("全部")
                                        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                        
                                        uiState.transactions.forEach { transaction ->
                                            try {
                                                val date = formatter.parse(transaction.time)
                                                date?.let {
                                                    months.add(dateFormat.format(it))
                                                }
                                            } catch (e: Exception) { }
                                        }
                                        months.toList().sorted()
                                    }
                                    
                                    // 获取可用的交易类型
                                    val availableTypes = remember(uiState.transactions) {
                                        val types = mutableSetOf("全部")
                                        uiState.transactions.forEach { transaction ->
                                            if (transaction.type.isNotBlank()) {
                                                types.add(transaction.type)
                                            }
                                        }
                                        types.toList().sorted()
                                    }
                                    
                                    // 月度统计卡片（可选）
                                    uiState.monthBill?.let { bill ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp), // Reduced vertical padding
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "本月支出",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "¥${bill.totalAmount.format(2)}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "本月收入",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "¥${bill.inAmount.format(2)}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = Color(0xFF4CAF50)
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "当前余额",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "¥${uiState.cardInfo?.balance?.format(2) ?: "0.00"}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // 筛选选项卡
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 2.dp), // Reduced vertical padding
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp) // Reduced vertical padding
                                        ) {
                                            // 显示/隐藏筛选选项 - 修改这部分，使整个区域可点击
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null // 改为使用容器自带的效果
                                                    ) { expanded = !expanded }
                                                    .padding(vertical = 4.dp), // Reduced vertical padding
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "筛选选项",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    modifier = Modifier.padding(start = 8.dp)
                                                )
                                                // 改为普通Icon而不是IconButton，避免点击冲突
                                                Icon(
                                                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                    contentDescription = "展开筛选选项",
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                            }
                                            
                                            // 筛选选项内容
                                            AnimatedVisibility(visible = expanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 4.dp) // Reduced padding
                                                        .heightIn(max = 250.dp) // Constrain max height
                                                        .verticalScroll(rememberScrollState()) // Make content scrollable
                                                ) {
                                                    // 月份选择
                                                    Text(
                                                        text = "按月份筛选",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        OutlinedTextField(
                                                            value = selectedMonth,
                                                            onValueChange = {},
                                                            readOnly = true,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable(
                                                                    interactionSource = remember { MutableInteractionSource() },
                                                                    indication = null
                                                                ) { showMonthDropdown = true },
                                                            trailingIcon = {
                                                                Icon(
                                                                    imageVector = Icons.Filled.ArrowDropDown,
                                                                    contentDescription = "选择月份",
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                            },
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                                                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                                            )
                                                        )
                                                        
                                                        // 在整个TextField上覆盖一个透明的可点击层
                                                        if (!showMonthDropdown) {
                                                            Spacer(
                                                                modifier = Modifier
                                                                    .matchParentSize()
                                                                    .clickable { showMonthDropdown = true }
                                                                    .background(Color.Transparent)
                                                            )
                                                        }
                                                        
                                                        // 美化的下拉菜单
                                                        DropdownMenu(
                                                            expanded = showMonthDropdown,
                                                            onDismissRequest = { showMonthDropdown = false },
                                                            modifier = Modifier
                                                                .fillMaxWidth(0.5f) // Reduced width significantly
                                                                .background(
                                                                    MaterialTheme.colorScheme.surfaceVariant, // Use a slightly different background
                                                                    shape = RoundedCornerShape(16.dp) // More rounded corners
                                                                )
                                                                .padding(vertical = 4.dp), // Add some vertical padding for the container
                                                            offset = DpOffset(0.dp, 8.dp) // Adjust offset slightly
                                                        ) {
                                                            // Wrap items in a Box with constrained height and scrolling
                                                            Box(modifier = Modifier
                                                                .heightIn(max = 200.dp) // Limit max height
                                                                .verticalScroll(rememberScrollState()) // Make it scrollable
                                                            ) {
                                                                Column { // Use Column to layout items vertically within the scrollable Box
                                                                    availableMonths.forEach { month ->
                                                                        val isSelected = selectedMonth == month
                                                                        DropdownMenuItem(
                                                                            text = {
                                                                                Text(
                                                                                    month,
                                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal // Use SemiBold
                                                                                ) 
                                                                            },
                                                                            onClick = { 
                                                                                selectedMonth = month 
                                                                                showMonthDropdown = false
                                                                            },
                                                                            leadingIcon = { // Use leading icon slot for checkmark
                                                                                if (isSelected) {
                                                                                    Icon(
                                                                                        imageVector = Icons.Filled.Check,
                                                                                        contentDescription = "已选择",
                                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                                        modifier = Modifier.size(20.dp) // Adjust size
                                                                                    )
                                                                                } else {
                                                                                    // Add a spacer to align text when no icon is present
                                                                                    Spacer(modifier = Modifier.width(20.dp))
                                                                                }
                                                                            },
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(horizontal = 12.dp, vertical = 4.dp) // Adjust padding
                                                                                .background(
                                                                                    // Use a more subtle background for selected item
                                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                                                                    shape = RoundedCornerShape(12.dp) // Rounded corners for items too
                                                                                ),
                                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp) // Adjust content padding
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp)) // Reduced spacer height
                                                    
                                                    // 交易类型选择
                                                    Text(
                                                        text = "按消费方式筛选",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        OutlinedTextField(
                                                            value = selectedType,
                                                            onValueChange = {},
                                                            readOnly = true,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable(
                                                                    interactionSource = remember { MutableInteractionSource() },
                                                                    indication = null
                                                                ) { showTypeDropdown = true },
                                                            trailingIcon = {
                                                                Icon(
                                                                    imageVector = Icons.Filled.ArrowDropDown,
                                                                    contentDescription = "选择交易类型",
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                            },
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = OutlinedTextFieldDefaults.colors(
                                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                                                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                                            )
                                                        )
                                                        
                                                        // 在整个TextField上覆盖一个透明的可点击层
                                                        if (!showTypeDropdown) {
                                                            Spacer(
                                                                modifier = Modifier
                                                                    .matchParentSize()
                                                                    .clickable { showTypeDropdown = true }
                                                                    .background(Color.Transparent)
                                                            )
                                                        }
                                                        
                                                        // 美化的下拉菜单
                                                        DropdownMenu(
                                                            expanded = showTypeDropdown,
                                                            onDismissRequest = { showTypeDropdown = false },
                                                            modifier = Modifier
                                                                .fillMaxWidth(0.5f) // Reduced width significantly
                                                                .background(
                                                                    MaterialTheme.colorScheme.surfaceVariant, // Use a slightly different background
                                                                    shape = RoundedCornerShape(16.dp) // More rounded corners
                                                                )
                                                                .padding(vertical = 4.dp), // Add some vertical padding for the container
                                                            offset = DpOffset(0.dp, 8.dp) // Adjust offset slightly
                                                        ) {
                                                            // Wrap items in a Box with constrained height and scrolling
                                                            Box(modifier = Modifier
                                                                .heightIn(max = 200.dp) // Limit max height
                                                                .verticalScroll(rememberScrollState()) // Make it scrollable
                                                            ) {
                                                                Column { // Use Column to layout items vertically within the scrollable Box
                                                                    availableTypes.forEach { type ->
                                                                        val isSelected = selectedType == type
                                                                        DropdownMenuItem(
                                                                            text = {
                                                                                Text(
                                                                                    type,
                                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal // Use SemiBold
                                                                                ) 
                                                                            },
                                                                            onClick = { 
                                                                                selectedType = type 
                                                                                showTypeDropdown = false
                                                                            },
                                                                            leadingIcon = { // Use leading icon slot for checkmark
                                                                                if (isSelected) {
                                                                                    Icon(
                                                                                        imageVector = Icons.Filled.Check,
                                                                                        contentDescription = "已选择",
                                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                                        modifier = Modifier.size(20.dp) // Adjust size
                                                                                    )
                                                                                } else {
                                                                                    // Add a spacer to align text when no icon is present
                                                                                    Spacer(modifier = Modifier.width(20.dp))
                                                                                }
                                                                            },
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(horizontal = 12.dp, vertical = 4.dp) // Adjust padding
                                                                                .background(
                                                                                    // Use a more subtle background for selected item
                                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                                                                    shape = RoundedCornerShape(12.dp) // Rounded corners for items too
                                                                                ),
                                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp) // Adjust content padding
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // 交易列表（添加筛选逻辑）
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        // 筛选交易列表
                                        val filteredTransactions = uiState.transactions.filter { transaction ->
                                            // 月份筛选
                                            val monthMatch = if (selectedMonth == "全部") {
                                                true
                                            } else {
                                                try {
                                                    val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                                                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                                    val date = formatter.parse(transaction.time)
                                                    date != null && dateFormat.format(date) == selectedMonth
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            }
                                            
                                            // 类型筛选
                                            val typeMatch = if (selectedType == "全部") {
                                                true
                                            } else {
                                                transaction.type == selectedType
                                            }
                                            
                                            // 两者都匹配才显示
                                            monthMatch && typeMatch
                                        }
                                        
                                        items(filteredTransactions, key = { it.id }) { transaction ->
                                            TransactionItem(transaction = transaction)
                                        }
                                        
                                        // 显示筛选结果统计
                                        if (filteredTransactions.isEmpty() && uiState.transactions.isNotEmpty()) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("没有符合筛选条件的交易记录")
                                                }
                                            }
                                        }
                                        
                                    // Loading indicator at the bottom
                                    if (uiState.isFetchingMore) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // 消费趋势
                            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("无交易记录")
                                }
                            } else {
                                // Pass the full transaction list instead of pre-calculated trend
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                     ConsumeTrendDisplay(transactions = uiState.transactions)
                                }
                            }
                        }
                        3 -> {
                            // New Tab: 图表分析
                            ChartContent(uiState) // 使用新的多图表组件，传入整个uiState
                        }
                    }
                    // Overall Loading Indicator (optional, SwipeRefresh handles main loading)
                    // Only show the centered spinner if NOT showing the linear progress bar
                    if (uiState.isLoading && uiState.loadingProgress == null && uiState.transactions.isEmpty() && selectedTabIndex == 1) { // Show only on transaction tab when initially loading
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
fun ChartContent(uiState: CampusCardUiState) {
    val availableCharts = listOf(
        "每日消费" to "最近30天每日消费趋势",
        "月度收支" to "月度收入与支出统计",
        "消费分类" to "按预定义分类的消费金额",
        "商户排行" to "消费金额Top10商户",
        "星期分布" to "按周一至周日的消费分布",
        "时段分布" to "按小时的消费分布",
        "交易类型" to "收入/支出交易次数占比",
        "消费区间" to "不同金额区间的交易次数",
        "余额趋势" to "校园卡余额变化趋势"
    )
    
    var selectedChartIndex by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 图表选择区域
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(availableCharts) { index, chart ->
                        val (chartName, _) = chart
                        ChartChip(
                            text = chartName,
                            isSelected = selectedChartIndex == index,
                            onClick = { selectedChartIndex = index }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 图表标题
        Text(
            availableCharts[selectedChartIndex].second,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
        
        // 图表内容区域
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            // Apply scroll modifiers conditionally based on the selected chart
            val chartContainerModifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .then(
                    // 针对特定图表，添加垂直和水平滚动
                    if (selectedChartIndex in listOf(4, 5, 7)) { // 星期分布, 时段分布, 消费区间
                        Modifier
                            .verticalScroll(rememberScrollState()) // 添加垂直滚动
                            .horizontalScroll(rememberScrollState())
                    } else {
                        Modifier // 其他图表不添加水平滚动
                    }
                )

            Box(
                modifier = chartContainerModifier, // Use the conditional modifier
                contentAlignment = Alignment.Center
            ) {
                when (selectedChartIndex) {
                    0 -> {
                        // 每日消费
                        if (uiState.dailySpendingData != null && uiState.dailySpendingData.isNotEmpty()) {
                            val dailyEntryProducer = remember(uiState.dailySpendingData) { 
                                ChartEntryModelProducer().apply { setEntries(uiState.dailySpendingData) }
                            }
                            Chart(
                                chart = columnChart(),
                                chartModelProducer = dailyEntryProducer,
                                startAxis = rememberStartAxis(title = "消费金额 (元)"),
                                bottomAxis = rememberBottomAxis(
                                    title = "日期", 
                                    valueFormatter = { value, _ ->
                                        // 将相对天数（index 0-29）转换为实际日期
                                        val daysAgo = 29 - value.toInt() // Assuming x=0 is 30 days ago, x=29 is today
                                        val calendar = Calendar.getInstance()
                                        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
                                        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                                        sdf.format(calendar.time)
                                    },
                                    itemPlacer = com.patrykandpatrick.vico.core.axis.AxisItemPlacer.Horizontal.default(
                                        spacing = 6, // Match spacing with Balance Trend chart
                                        offset = 0,
                                        shiftExtremeTicks = true
                                    ),
                                    labelRotationDegrees = -45f // Rotate labels
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("暂无每日消费数据")
                        }
                    }
                    1 -> {
                        // 月度收支
                        if (uiState.monthlyIncomeExpenseData != null && uiState.monthlyIncomeExpenseData.isNotEmpty()) {
                            MonthlyIncomeExpenseChart(uiState.monthlyIncomeExpenseData)
                        } else {
                            Text("暂无月度收支数据")
                        }
                    }
                    2 -> {
                        // 消费分类
                        if (uiState.predefinedCategoryData != null && uiState.predefinedCategoryData.isNotEmpty()) {
                            PieChartDisplay(
                                data = uiState.predefinedCategoryData,
                                title = "预定义分类消费金额"
                            )
                        } else {
                            Text("暂无消费分类数据")
                        }
                    }
                    3 -> {
                        // 商户排行
                        if (uiState.merchantTopSpendingData != null && uiState.merchantTopSpendingData.isNotEmpty()) {
                            TopMerchantsChart(uiState.merchantTopSpendingData)
                        } else {
                            Text("暂无商户排行数据")
                        }
                    }
                    4 -> {
                        // 星期分布
                        if (uiState.dayOfWeekSpendingData != null && uiState.dayOfWeekSpendingData.isNotEmpty()) {
                             // Wrap in a Box to allow the chart to define its own intrinsic width
                            Box(modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Max)) {
                                DayOfWeekBarChart(uiState.dayOfWeekSpendingData)
                            }
                        } else {
                            Text("暂无星期分布数据")
                        }
                    }
                    5 -> {
                        // 时段分布
                        if (uiState.hourOfDaySpendingData != null && uiState.hourOfDaySpendingData.isNotEmpty()) {
                             // Wrap in a Box to allow the chart to define its own intrinsic width
                            Box(modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Max)) {
                                HourOfDayBarChart(uiState.hourOfDaySpendingData)
                            }
                        } else {
                            Text("暂无时段分布数据")
                        }
                    }
                    6 -> {
                        // 交易类型 (Pie chart, likely doesn't need scrolling)
                        if (uiState.transactionTypeData != null && uiState.transactionTypeData.isNotEmpty()) {
                            PieChartDisplay(
                                data = uiState.transactionTypeData,
                                title = "交易类型次数占比"
                            )
                        } else {
                            Text("暂无交易类型数据")
                        }
                    }
                    7 -> {
                        // 消费区间
                        if (uiState.spendingRangeData != null && uiState.spendingRangeData.isNotEmpty()) {
                            // Wrap in a Box to allow the chart to define its own intrinsic width
                             Box(modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Max)) {
                                SpendingRangeBarChart(uiState.spendingRangeData)
                            }
                        } else {
                            Text("暂无消费区间数据")
                        }
                    }
                    8 -> {
                        // 余额趋势 - Reverse X-axis logic
                        if (uiState.balanceTrendData != null && uiState.balanceTrendData.isNotEmpty()) {
                            // IMPORTANT: Need to adjust data processing in ViewModel first.
                            // Assuming ViewModel now provides balanceTrendData with X as 'daysAgo'.
                            val balanceEntryProducer = remember(uiState.balanceTrendData) {
                                ChartEntryModelProducer().apply { setEntries(uiState.balanceTrendData) }
                            }

                            Chart(
                                chart = lineChart(),
                                chartModelProducer = balanceEntryProducer,
                                startAxis = rememberStartAxis(
                                    title = "余额 (元)",
                                    valueFormatter = { value, _ -> "¥${value.toInt()}" }
                                ),
                                bottomAxis = rememberBottomAxis(
                                    title = "日期",
                                    valueFormatter = { value, _ ->
                                        // value still represents 'daysAgo'
                                        val daysAgoInt = value.toInt()
                                        // Format the date if it's one of the chosen labels
                                        val calendar = Calendar.getInstance()
                                        calendar.add(Calendar.DAY_OF_YEAR, -daysAgoInt)
                                        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                                        sdf.format(calendar.time)
                                    },
                                    // Attempt to force fewer labels by specifying guideline count or item placer
                                    // Option 1: Guideline count (simpler, might work)
                                    // guidelineCount = 5, 
                                    // Option 2: Item Placer (more control)
                                    itemPlacer = com.patrykandpatrick.vico.core.axis.AxisItemPlacer.Horizontal.default(
                                        spacing = 6, // Spacing in terms of entries (adjust as needed)
                                        offset = 0, 
                                        shiftExtremeTicks = true // Try shifting ticks
                                    ),
                                    labelRotationDegrees = -45f // Keep rotation
                                ),
                                modifier = Modifier.fillMaxSize() // Chart itself fills the Box
                            )
                        } else {
                            Text("暂无余额趋势数据")
                        }
                    }
                }
            }
        }
        
        // 可选：底部图表描述或统计数字
        if (selectedChartIndex == 0 && uiState.monthBill != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 使用CampusCardComponents.kt中定义的公共版本
                    ChartSummaryRow(
                        leftItem = Pair("本月总支出", "￥${uiState.monthBill.totalAmount.format(2)}"),
                        middleItem = Pair("本月总收入", "￥${uiState.monthBill.inAmount.format(2)}"),
                        rightItem = Pair("当前余额", uiState.cardInfo?.let { "￥${it.balance.format(2)}" } ?: "-")
                    )
                }
            }
        }
    }
} 