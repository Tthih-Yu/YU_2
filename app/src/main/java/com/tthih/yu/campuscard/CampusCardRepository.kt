package com.tthih.yu.campuscard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import com.tthih.yu.campuscard.ApiTransaction
import com.tthih.yu.campuscard.TransactionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// Constants - Updated for direct card system interaction
private const val TAG = "CampusCardRepository"
private const val API_BASE_URL = "http://220.178.164.65:8053" // Direct API URL
private const val PREFS_NAME = "CampusCardPrefs"
private const val PREF_ACCOUNT = "campus_card_account" // Store account ID (学号)

class CampusCardRepository private constructor(context: Context) {
    private var database: CampusCardDatabase? = null
    private val apiService = NetworkModule.apiService
    private var sharedPreferences: SharedPreferences? = null
    
    // --- Database Initialization ---
    private fun getDatabase(context: Context): CampusCardDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                CampusCardDatabase::class.java,
                "campus_card_database"
            ).fallbackToDestructiveMigration() // Handle migrations appropriately
             .build()
            database = instance
            instance
        }
    }
    
    // --- SharedPreferences Initialization ---
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return sharedPreferences ?: synchronized(this) {
            val instance = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = instance
            instance
        }
    }

    // <<< ADD Companion Object for Singleton >>>
    companion object {
        @Volatile
        private var INSTANCE: CampusCardRepository? = null

        fun getInstance(context: Context): CampusCardRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CampusCardRepository(context.applicationContext) // Pass application context
                INSTANCE = instance
                instance
            }
        }
    }
    // <<< End Companion Object >>>
    
    init { // <<< Use init block instead of separate initialize function >>>
        // Initialize DB and SharedPreferences immediately
        database = Room.databaseBuilder(
            context.applicationContext,
            CampusCardDatabase::class.java,
            "campus_card_database"
        ).fallbackToDestructiveMigration().build()

        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "CampusCardRepository Initialized (Singleton)")
    }

    // --- Account Management ---
    fun saveAccount(account: String?) {
        sharedPreferences?.edit()?.apply {
             if (account != null) {
                 putString(PREF_ACCOUNT, account)
             } else {
                 remove(PREF_ACCOUNT)
            }
            apply()
        }
         Log.d(TAG, "Saved account: $account")
    }

    fun getAccount(): String? {
        val account = sharedPreferences?.getString(PREF_ACCOUNT, null)
         Log.d(TAG, "Retrieved account: $account")
        return account
    }

    // --- API Call Logic ---

    // Result wrapper for API calls
    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    }

    // Function to fetch card info
    suspend fun fetchCardInfo(): Result<CardInfo> = withContext(Dispatchers.IO) {
        val prefix = "" // Removed prefix usage
        val account = getAccount()
        if (account == null) {
            Log.e(TAG, "Cannot fetch card info: Account number is null.")
            return@withContext Result.Error("未找到校园卡账号，请先登录")
        }

        Log.w(TAG, "fetchCardInfo - Using placeholder logic. Needs rewrite for direct API.")
        // Placeholder - return error or empty
        Result.Error("获取卡片信息功能待更新")
    }
    
    // Function to fetch monthly bill summary
    suspend fun fetchMonthBillSummary(year: Int, month: Int): Result<MonthBill> = withContext(Dispatchers.IO) {
        val prefix = "" // Removed prefix usage
        val account = getAccount()
        if (account == null) {
            Log.e(TAG, "Cannot fetch month bill summary: Account number is null.")
            return@withContext Result.Error("未找到校园卡账号，请先登录")
        }

        Log.w(TAG, "fetchMonthBillSummary - Using placeholder logic. Needs rewrite for direct API.")
        // Placeholder - return error or empty
        Result.Error("获取月账单功能待更新")
    }
    
    // Function to fetch consumption trend
    suspend fun fetchConsumeTrend(year: Int, month: Int): Result<ConsumeTrend> = withContext(Dispatchers.IO) {
        val prefix = "" // Removed prefix usage
        val account = getAccount()
        if (account == null) {
            Log.e(TAG, "Cannot fetch consume trend: Account number is null.")
            return@withContext Result.Error("未找到校园卡账号，请先登录")
        }

        Log.w(TAG, "fetchConsumeTrend - Using placeholder logic. Needs rewrite for direct API.")
        // Placeholder - return error or empty
        Result.Error("获取消费趋势功能待更新")
    }

    // Function to fetch transactions for a specific month
    suspend fun fetchTransactions(page: Int = 1): Result<Pair<List<CampusCardTransaction>, Int>> = withContext(Dispatchers.IO) {
        val account = getAccount()
        if (account == null) {
            Log.e(TAG, "Cannot fetch transactions: Account number is null.")
            return@withContext Result.Error("未找到校园卡账号，请先登录")
        }

        // Attempt to get hallticket/sourcetype from cookies if needed by the API
        // val hallticket = NetworkModule.getCookiesForUrl(API_BASE_URL).find { it.name == "hallticket" }?.value
        // if (hallticket == null) { Log.w(TAG, "Hallticket cookie not found, API call might fail if sourcetype is required.") }

        try {
            Log.d(TAG, "Fetching transactions for account: $account, page: $page")
            // Make the actual API call using Retrofit service
            val response = apiService.getTransactions(
                account = account,
                page = page
                // sourceType = hallticket // Pass hallticket as sourcetype if API requires it
            )

            if (response.isSuccessful && response.body() != null) {
                val responseData = response.body()!!
                // Map API response to UI model
                val apiRows = responseData.rows // Get the nullable list first
                
                Log.d(TAG, "API response: success=${responseData.issucceed}, total=${responseData.total}, rowCount=${apiRows?.size ?: 0}")
                
                val transactions = if (apiRows != null) {
                    apiRows.mapNotNull { apiTx -> mapApiTransactionToUi(apiTx) } // Use explicit lambda parameter name
                } else {
                    emptyList()
                }
                
                // The 'total' field from the API is the total number of records
                val totalRecords = responseData.total ?: 0
                // Use the non-null 'transactions' list size
                val pageSize = if (transactions.isNotEmpty()) transactions.size else 15 // Default page size if list is empty
                
                val totalPages = if (totalRecords > 0 && pageSize > 0) {
                    (totalRecords + pageSize - 1) / pageSize // Ceiling division for total pages
                } else if (totalRecords == 0 && transactions.isNotEmpty()) {
                    1 // We have data but total is 0, assume 1 page
                } else if (totalRecords == 0 && transactions.isEmpty()) {
                    0 // No data and total is 0, assume 0 pages
                } else {
                    1 // Default fallback: assume at least one page if totalRecords > 0 but pageSize is 0 (shouldn't happen)
                }

                Log.i(TAG, "Successfully fetched ${transactions.size} transactions for page $page. Total calculated pages: $totalPages (from total records: $totalRecords)")
                
                // --- Save fetched transactions to the database ---
                if (transactions.isNotEmpty()) {
                    try {
                         database?.campusCardDao()?.insertAll(transactions)
                         Log.d(TAG, "Saved ${transactions.size} transactions to the database.")
                    } catch (dbError: Exception) {
                        Log.e(TAG, "Error saving transactions to database: ${dbError.message}", dbError)
                        // Optionally propagate this error or handle it (e.g., return a different error state)
                        // For now, we'll log it and proceed with the fetched data
                    }
                }
                // --- End of saving ---
                
                if (transactions.isEmpty() && apiRows?.isNotEmpty() == true) {
                    // We got API data but couldn't parse any transactions - log the first item to help debug
                    val firstApiRow = apiRows.firstOrNull()
                    Log.w(TAG, "Got ${apiRows.size} rows from API but parsed 0 transactions. First row: $firstApiRow")
                }
                
                Result.Success(Pair(transactions, totalPages))
            } else {
                // Handle API error response
                val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                Log.e(TAG, "API error fetching transactions: Code=${response.code()}, Message=${response.message()}, Body=$errorBody")
                Result.Error("获取交易记录失败: ${response.message()} (${response.code()})")
            }
        } catch (e: IOException) {
            // Handle network errors (e.g., no connection)
            Log.e(TAG, "Network error fetching transactions: ${e.message}", e)
            Result.Error("网络错误，请检查连接: ${e.message}")
        } catch (e: Exception) {
            // Handle other errors (e.g., JSON parsing issues)
            Log.e(TAG, "Generic error fetching transactions: ${e.message}", e)
            Result.Error("获取交易记录时出错: ${e.message}")
        }
    }

    // --- Database Operations (Caching) ---
    suspend fun getCachedTransactions(): List<CampusCardTransaction> {
        return database?.let {
            withContext<List<CampusCardTransaction>>(Dispatchers.IO) {
                it.campusCardDao().getAllTransactions()
            }
        } ?: emptyList()
    }
    
    // --- New function to clear all transactions --- 
    suspend fun clearAllTransactions() {
        database?.let {
            withContext<Unit>(Dispatchers.IO) {
                Log.i(TAG, "Executing DAO clearAllTransactions...")
                it.campusCardDao().clearAll() // Call the DAO function
            }
        }
        Log.i(TAG, "Finished clearing all transactions in repository.")
    }
    
    suspend fun clearCachedTransactions() {
        database?.let {
            withContext<Unit>(Dispatchers.IO) {
                it.campusCardDao().deleteAllTransactions()
            }
        }
    }

    // --- Data Mapping Functions (API to UI Model) ---
    private fun mapApiCardInfoToUi(apiData: CardInfoData?): CardInfo? {
        if (apiData == null) return null
        return CardInfo(
            cardNumber = apiData.cardNumber ?: "N/A",
            balance = apiData.balance?.toDoubleOrNull() ?: 0.0,
            expiryDate = apiData.expiryDate ?: "N/A",
            status = apiData.status ?: "未知"
        )
    }
    
    private fun mapApiMonthBillToUi(apiData: MonthBillData?): MonthBill? {
        if (apiData == null) return null
        return MonthBill(
            totalAmount = apiData.totalAmount?.toDoubleOrNull() ?: 0.0,
            inAmount = apiData.inAmount?.toDoubleOrNull() ?: 0.0
        )
    }
    
    private fun mapApiConsumeTrendToUi(apiData: ConsumeTrendData?): ConsumeTrend? {
         if (apiData == null) return null
        
        var dates: List<String> = emptyList()
        var amounts: List<Double> = emptyList()

        // Try parsing xData/yData format first
        if (apiData.xData != null && apiData.yData != null && apiData.xData.size == apiData.yData.size) {
            dates = apiData.xData
            amounts = apiData.yData
        } 
        // Try parsing serisData format
        else if (apiData.serisData != null) {
            val parsedDates = mutableListOf<String>()
            val parsedAmounts = mutableListOf<Double>()
            apiData.serisData.forEach { item ->
                if (item.size >= 2 && item[0] is String && item[1] is Number) {
                    parsedDates.add(item[0] as String)
                    parsedAmounts.add((item[1] as Number).toDouble())
                }
            }
            dates = parsedDates
            amounts = parsedAmounts
        }

        if (dates.isEmpty() || amounts.isEmpty()) return ConsumeTrend() // Return empty if parsing failed

        val totalAmount = amounts.sum()
        val maxAmount = amounts.maxOrNull() ?: 0.0
        val averageAmount = if (amounts.isNotEmpty()) totalAmount / amounts.size else 0.0

        return ConsumeTrend(
            dates = dates,
            amounts = amounts,
            maxAmount = maxAmount,
            totalAmount = totalAmount,
            averageAmount = averageAmount
        )
    }

    private fun mapApiTransactionToUi(apiTx: ApiTransaction?): CampusCardTransaction? {
        // Check for required fields
        if (apiTx?.id == null || apiTx.time == null || apiTx.amount == null || apiTx.balance == null) {
            Log.w(TAG, "Skipping transaction with missing required fields: id=${apiTx?.id}, time=${apiTx?.time}, amount=${apiTx?.amount}, balance=${apiTx?.balance}")
            return null // Skip invalid transactions
        }
        
        // Format time (e.g., "2025-04-13 17:30")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedTime = try {
            inputFormat.parse(apiTx.time)?.let { outputFormat.format(it) } ?: apiTx.time
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse time format: ${apiTx.time}, using original", e)
            apiTx.time // Fallback to original time if parsing fails
        }

        val location = apiTx.location?.trim() ?: "未知地点"
        
        // Use TRANNAME as description or combine with location
        val tranName = apiTx.tranName?.trim() ?: ""
        val description = if (tranName.isNotEmpty()) {
            "$location ($tranName)"
        } else {
            location
        }
        
        // Determine transaction type (simple example)
        val type = when {
            apiTx.amount > 0 -> "充值/收入"
            location.contains("餐厅") || location.contains("食堂") -> "餐饮"
            location.contains("超市") -> "购物"
            location.contains("水") || location.contains("浴") || location.contains("电") -> "生活缴费"
            tranName.contains("支付") -> "支付"
            // Add more rules based on location or tranName or tranCode
            else -> "其他支出"
        }

        // Convert the id to String since CampusCardTransaction expects String id
        val idString = apiTx.id.toString()

        return CampusCardTransaction(
            id = idString,
            time = formattedTime,
            amount = apiTx.amount,
            balance = apiTx.balance,
            type = type,
            location = location,
            description = description
        )
    }
} 