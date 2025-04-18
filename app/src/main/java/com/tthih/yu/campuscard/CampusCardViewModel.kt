package com.tthih.yu.campuscard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.* // For Calendar

class CampusCardViewModel : ViewModel() {
    private val TAG = "CampusCardViewModel"
    private lateinit var repository: CampusCardRepository

    // --- StateFlows for UI State ---
    private val _uiState = MutableStateFlow(CampusCardUiState())
    val uiState: StateFlow<CampusCardUiState> = _uiState.asStateFlow()

    // Keep track of current pagination for transactions
    private var currentPage = 1
    private var isFetchingTransactions = false
    private var allTransactionsLoaded = false

    fun initialize(context: Context) {
        repository = CampusCardRepository().apply { initialize(context) }
        // Load cached data initially
        loadCachedData()
        // Trigger initial data refresh
        refreshAllData(forceLogin = false) // Don't force login on initial load
    }

    // --- Credential Management ---
    fun saveCredentials(username: String, password: String?) {
        repository.saveCredentials(username, password)
        // After saving new credentials, trigger a refresh with forced login
        refreshAllData(forceLogin = true)
    }

    fun getUsername(): String? = repository.getUsername()

    // --- Data Fetching Logic ---
    private fun loadCachedData() {
        viewModelScope.launch {
            val cachedTransactions = repository.getCachedTransactions()
            _uiState.update {
                it.copy(transactions = cachedTransactions)
            }
        }
    }

    // --- Data Refreshing Logic ---
    fun refreshAllData(forceLogin: Boolean = false) {
        currentPage = 1 // Reset pagination
        allTransactionsLoaded = false
        isFetchingTransactions = false
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // If force login is requested, or if no WebVPN prefix is found, perform login first
            if (forceLogin || repository.getWebvpnPrefix() == null) {
                when (val result = repository.login()) {
                    is CampusCardRepository.Result.Success<Boolean> -> {
                        // Login successful, continue
                        Log.d(TAG, "Login successful")
                    }
                    is CampusCardRepository.Result.Error -> {
                        // Login failed, show error and return
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                errorMessage = result.message,
                                requiresLogin = true
                            ) 
                        }
                        return@launch
                    }
                }
            }
            
            // Now fetch all data
            fetchCardInfoInternal()
            fetchCurrentMonthBillInternal()
            fetchCurrentMonthTrendInternal()
            fetchTransactionsInternal(page = 1, replaceExisting = true)
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    // Method to update card info directly from login activity data
    fun updateCardInfoDirectly(cardNumber: String, balance: Double) {
        if (cardNumber.isNotBlank()) {
            val cardInfo = CardInfo(
                cardNumber = cardNumber,
                balance = balance,
                expiryDate = "",  // We don't have this yet
                status = "正常"    // Assume normal status
            )
            
            _uiState.update { it.copy(cardInfo = cardInfo) }
        }
    }

    private suspend fun fetchCardInfoInternal() {
        when (val result = repository.fetchCardInfo()) {
            is CampusCardRepository.Result.Success<CardInfo> -> _uiState.update { it.copy(cardInfo = result.data) }
            is CampusCardRepository.Result.Error -> _uiState.update { 
                // Only update the error message, don't clear cardInfo if it exists
                it.copy(errorMessage = "获取卡片信息失败: ${result.message}") 
            }
        }
    }

    private suspend fun fetchCurrentMonthBillInternal() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        
        when (val result = repository.fetchMonthBillSummary(year, month)) {
            is CampusCardRepository.Result.Success<MonthBill> -> _uiState.update { it.copy(monthBill = result.data) }
            is CampusCardRepository.Result.Error -> _uiState.update { 
                // Only update the error message, don't clear monthBill if it exists
                it.copy(errorMessage = "获取月账单失败: ${result.message}")
            }
        }
    }

    private suspend fun fetchCurrentMonthTrendInternal() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        
        when (val result = repository.fetchConsumeTrend(year, month)) {
            is CampusCardRepository.Result.Success<ConsumeTrend> -> _uiState.update { it.copy(consumeTrend = result.data) }
            is CampusCardRepository.Result.Error -> _uiState.update { 
                // Only update the error message, don't clear trend data if it exists
                it.copy(errorMessage = "获取消费趋势失败: ${result.message}")
            }
        }
    }

    // --- Transaction Pagination Logic ---
    fun fetchMoreTransactions() {
        if (isFetchingTransactions || allTransactionsLoaded) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingMore = true) }
            fetchTransactionsInternal(currentPage + 1, replaceExisting = false)
            _uiState.update { it.copy(isFetchingMore = false) }
        }
    }
    
    private suspend fun fetchTransactionsInternal(page: Int, replaceExisting: Boolean) {
        isFetchingTransactions = true
        
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        
        when (val result = repository.fetchTransactions(year, month, page)) {
            is CampusCardRepository.Result.Success<List<CampusCardTransaction>> -> {
                currentPage = page
                val transactions = result.data
                
                if (transactions.isEmpty()) {
                    allTransactionsLoaded = true
                                                            } else {
                    _uiState.update {
                        if (replaceExisting) {
                            it.copy(transactions = transactions)
                                            } else {
                            // Append new transactions, avoiding duplicates
                            val existingIds = it.transactions.map { tx -> tx.id }.toSet()
                            val newTransactions = transactions.filterNot { tx -> existingIds.contains(tx.id) }
                            it.copy(transactions = it.transactions + newTransactions)
                        }
                    }
                }
            }
            is CampusCardRepository.Result.Error -> _uiState.update { 
                it.copy(errorMessage = "获取交易记录失败: ${result.message}")
            }
        }
        
        isFetchingTransactions = false
    }

    // Helper function to cancel login attempt
    fun cancelLogin() {
        _uiState.update { it.copy(isLoading = false, requiresLogin = false, errorMessage = "登录已取消") }
    }
}

// --- UI State Data Class ---
data class CampusCardUiState(
    val isLoading: Boolean = true,
    val isFetchingMore: Boolean = false, // For pagination indicator
    val errorMessage: String? = null,
    val cardInfo: CardInfo? = null,
    val monthBill: MonthBill? = null,
    val consumeTrend: ConsumeTrend? = null,
    val transactions: List<CampusCardTransaction> = emptyList(),
    val requiresLogin: Boolean = false // Flag to potentially show a login screen
)

// Removed duplicate data class definitions below // 