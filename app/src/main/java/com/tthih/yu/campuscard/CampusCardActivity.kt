package com.tthih.yu.campuscard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.* // Import necessary layout components
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh // Add dependency
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState // Add dependency
import com.tthih.yu.ui.theme.YUTheme
import java.util.* // For Calendar

class CampusCardActivity : ComponentActivity() {
    private lateinit var viewModel: CampusCardViewModel
    
    companion object {
        const val REQUEST_LOGIN = 1001
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
                    CampusCardScreen(viewModel)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            // Check if we have card data returned from the login activity
            val cardDataJson = data?.getStringExtra("card_data")
            
            if (cardDataJson != null) {
                try {
                    // Parse the card data JSON
                    val jsonObject = org.json.JSONObject(cardDataJson)
                    val cardNumber = jsonObject.optString("cardNumber", "")
                    val balance = jsonObject.optString("balance", "0.0").toDoubleOrNull() ?: 0.0
                    
                    // Update the card info in the ViewModel
                    viewModel.updateCardInfoDirectly(cardNumber, balance)
                }
                catch (e: Exception) {
                    Log.e("CampusCardActivity", "Error parsing card data: ${e.message}")
                }
            }
            
            // Refresh all data
            viewModel.refreshAllData(forceLogin = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusCardScreen(viewModel: CampusCardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    // State for Login Dialog
    var showLoginDialog by remember { mutableStateOf(false) }
    
    // Trigger login dialog if required
    LaunchedEffect(uiState.requiresLogin) {
        if (uiState.requiresLogin) {
            showLoginDialog = true
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
                if (lastVisibleItemIndex != null && 
                    lastVisibleItemIndex >= uiState.transactions.size - 5 && // Trigger near the end
                    !uiState.isLoading && 
                    !uiState.isFetchingMore) { 
                    viewModel.fetchMoreTransactions()
                }
            }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("校园卡") },
                actions = {
                        IconButton(onClick = {
                        // Launch WebView login activity instead of showing dialog
                        val intent = Intent(context, CampusCardLoginActivity::class.java)
                        (context as? ComponentActivity)?.startActivityForResult(intent, CampusCardActivity.REQUEST_LOGIN)
                    }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "登录/设置账号")
                    }
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
            modifier = Modifier
                    .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Display Card Info
                uiState.cardInfo?.let { info ->
                    CardInfoDisplay(cardInfo = info) // Assume this exists in Components.kt
                } ?: Box(modifier = Modifier.height(100.dp)) // Placeholder height
                
                // Tabs for different views
                var selectedTabIndex by remember { mutableStateOf(0) }
                val tabTitles = listOf("本月账单", "账单明细", "消费趋势")
                
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
            
                // Content based on selected tab
            Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> {
                            // 本月账单
                            if (uiState.monthBill != null) {
                                MonthBillSummary(monthBill = uiState.monthBill!!)
                            } else if (!uiState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("无本月账单信息")
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
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.transactions, key = { it.id }) { transaction ->
                                        TransactionItem(transaction = transaction) // Assume this exists
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
                        2 -> {
                            // 消费趋势
                            if (uiState.consumeTrend != null) {
                                ConsumeTrendDisplay(trend = uiState.consumeTrend)
                            } else if (!uiState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("无消费趋势信息")
                                }
                            }
                        }
                    }
                    // Overall Loading Indicator (optional, SwipeRefresh handles main loading)
                    if (uiState.isLoading && uiState.transactions.isEmpty() && selectedTabIndex == 1) { // Show only on transaction tab when initially loading
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

// Simple Login Dialog Composable (Replace with a more robust implementation if needed)
@Composable
fun LoginDialog(
    currentUsername: String?,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf(currentUsername ?: "") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登录校园卡") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("学号") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码 (统一身份认证)") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Text(
                    text = "密码将明文存储，请注意安全风险！后续版本将改进。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onLogin(username, password)
                    }                    
                },
                enabled = username.isNotBlank() && password.isNotBlank()
            ) {
                Text("登录并保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
} 