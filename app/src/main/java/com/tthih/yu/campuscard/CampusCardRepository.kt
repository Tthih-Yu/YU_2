package com.tthih.yu.campuscard

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// Constants - Adjust these based on actual URLs and form field names
private const val TAG = "CampusCardRepository"
private const val CAS_LOGIN_URL = "https://webvpn.ahpu.edu.cn/http/webvpn40a1cc242791dfe16b3115ea5846a65e/authserver/login?service=https%3A%2F%2Fwebvpn.ahpu.edu.cn%2Fenlink%2Fapi%2Fclient%2Fcallback%2Fcas"
private const val CARD_SERVICE_BASE_URL_EHALL = "http://ehall.ahpu.edu.cn/publicapp/sys/myyktzd/mobile/"
private const val WEBVPN_PREFIX_PLACEHOLDER = "/http/webvpn<SESSION_SPECIFIC_PART>/"
private const val PREFS_NAME = "CampusCardPrefs"
private const val PREF_USERNAME = "username"
private const val PREF_PASSWORD = "password" // Consider EncryptedSharedPreferences for real app
private const val PREF_WEBVPN_PREFIX = "webvpn_prefix"

class CampusCardRepository {
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

    fun initialize(context: Context) {
        if (database == null) {
            getDatabase(context)
        }
        if (sharedPreferences == null) {
            getSharedPreferences(context)
        }
    }

    // --- Credential Management ---
    fun saveCredentials(username: String, password: String?) {
        sharedPreferences?.edit()?.apply {
            putString(PREF_USERNAME, username)
            if (password != null) { // Only save if provided
                putString(PREF_PASSWORD, password) // !! Insecure !! Use EncryptedPrefs
            }
            apply()
        }
    }

    fun getUsername(): String? = sharedPreferences?.getString(PREF_USERNAME, null)
    fun getPassword(): String? = sharedPreferences?.getString(PREF_PASSWORD, null) // !! Insecure !!

    // --- WebVPN Prefix Management ---
    private fun saveWebvpnPrefix(prefix: String) {
        sharedPreferences?.edit()?.putString(PREF_WEBVPN_PREFIX, prefix)?.apply()
    }

    // Make internal to be accessible from ViewModel in the same module
    internal fun getWebvpnPrefix(): String? {
        return sharedPreferences?.getString(PREF_WEBVPN_PREFIX, null)
    }

    // --- API Call Logic ---

    // Result wrapper for API calls
    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Error(val message: String, val exception: Exception? = null) : Result<Nothing>()
    }

    // Function to perform login
    suspend fun login(): Result<Boolean> = withContext(Dispatchers.IO) {
        val username = getUsername()
        val password = getPassword()
        if (username == null || password == null) {
            return@withContext Result.Error("用户名或密码未设置")
        }

        NetworkModule.clearCookies() // Clear old cookies before login

        try {
            // 1. Get CAS Login Page to extract tokens (lt, execution, etc.)
            val loginPageResponse = apiService.getCasLoginPage(CAS_LOGIN_URL)
            if (!loginPageResponse.isSuccessful || loginPageResponse.body() == null) {
                return@withContext Result.Error("获取登录页面失败: ${loginPageResponse.code()}")
            }

            val loginPageHtml = loginPageResponse.body()!!.string()
            val loginFormData = parseCasLoginForm(loginPageHtml)
            if (loginFormData["lt"] == null || loginFormData["execution"] == null) {
                return@withContext Result.Error("无法解析登录表单")
            }

            // Add username and password
            loginFormData["username"] = username
            loginFormData["password"] = password
            // Add other potential required fields like 'rememberMe'
            // loginFormData["rememberMe"] = "true"

            // 2. Post CAS Login Credentials
            val casResponse = apiService.postCasLogin(CAS_LOGIN_URL, loginFormData)
            if (!casResponse.isSuccessful) {
                return@withContext Result.Error("CAS登录失败: ${casResponse.code()}")
            }

            // 3. Check CAS Response - Looking for a redirect (302) with a 'ticket'
            // The actual target URL might be in the 'Location' header
            val locationHeader = casResponse.headers()["Location"]
            if (casResponse.code() != 302 || locationHeader == null || !locationHeader.contains("ticket=")) {
                Log.w(TAG, "CAS login did not result in expected redirect. Code: ${casResponse.code()}, Location: $locationHeader")
                // Might need to parse the response body if it's not a 302
                // return@withContext Result.Error("CAS认证失败，请检查用户名密码")
                // For now, assume success and try to proceed, relying on cookies being set
            }
            
            // 4. Extract WebVPN prefix from the redirect URL (if needed)
            // This is tricky and depends on the exact flow. The prefix might be stable or session-based.
            // Let's assume we get it from a redirect like the one in 1744560484928_raw
            // location: https://webvpn.ahpu.edu.cn/http/webvpn<session_part>/publicapp/...
            val webVpnRedirectUrl = locationHeader // Use the actual redirect URL from login or subsequent request
            val prefix = extractWebvpnPrefix(webVpnRedirectUrl ?: "") // Implement this helper
            if (prefix == null) {
                Log.w(TAG, "Could not extract WebVPN prefix from: $webVpnRedirectUrl")
                 // Try accessing a known endpoint and get prefix from its URL after redirect
                 // Use a known static resource or a simple API endpoint if available
                val knownUrlToAccess = CARD_SERVICE_BASE_URL_EHALL + "index.do" // Example
                try {
                     val knownEndpointResponse = apiService.getWebvpnService(knownUrlToAccess)
                     // Use property access for request and url
                     val finalUrl = knownEndpointResponse.raw().request.url.toString()
                     val extractedPrefix = extractWebvpnPrefix(finalUrl)
                     if (extractedPrefix != null) {
                         saveWebvpnPrefix(extractedPrefix)
                         Log.i(TAG, "Extracted WebVPN prefix from known endpoint: $extractedPrefix")
                     } else {
                         Log.e(TAG, "Failed to determine WebVPN prefix after accessing known URL: $finalUrl")
                         return@withContext Result.Error("无法确定WebVPN路径")
                     }
                } catch (knownUrlError: Exception) {
                     Log.e(TAG, "Error accessing known URL to determine WebVPN prefix", knownUrlError)
                     return@withContext Result.Error("访问WebVPN服务失败: ${knownUrlError.message}")
                }
            } else {
                 saveWebvpnPrefix(prefix)
                 Log.i(TAG, "Extracted WebVPN prefix from initial redirect: $prefix")
            }

            Result.Success(true) // Login sequence successful (or presumed successful)

        } catch (e: IOException) {
            Log.e(TAG, "Login network error", e)
            Result.Error("网络错误: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Login unexpected error", e)
            Result.Error("登录时发生未知错误: ${e.message}", e)
        }
    }

    // Function to fetch card info
    suspend fun fetchCardInfo(): Result<CardInfo> = withContext(Dispatchers.IO) {
        val prefix = getWebvpnPrefix() ?: return@withContext loginAndRetry { fetchCardInfo() }
        // !! Adjust the actual API path based on network analysis !!
        val url = buildWebvpnUrl(prefix, "/publicapp/sys/myyktzd/api/account/info.do") 
        
        try {
            val response = apiService.getCardInfo(url)
            val responseBody = response.body()
            if (response.isSuccessful && responseBody?.code == 200) {
                mapApiCardInfoToUi(responseBody.data)?.let {
                    Result.Success(it)
                } ?: Result.Error("卡片信息解析失败 (空数据)")
            } else {
                Result.Error("获取卡片信息失败: ${response.code()} - ${responseBody?.message ?: response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchCardInfo Exception", e)
            Result.Error("获取卡片信息异常: ${e.message}", e)
        }
    }
    
    // Function to fetch monthly bill summary
    suspend fun fetchMonthBillSummary(year: Int, month: Int): Result<MonthBill> = withContext(Dispatchers.IO) {
        val prefix = getWebvpnPrefix() ?: return@withContext loginAndRetry { fetchMonthBillSummary(year, month) }
        // !! Adjust API path and parameters as needed !!
        val url = buildWebvpnUrl(prefix, "/publicapp/sys/myyktzd/api/bill/summary.do?year=$year&month=${String.format("%02d", month)}") 
        
        try {
            val response = apiService.getMonthBill(url)
            val responseBody = response.body()
            if (response.isSuccessful && responseBody?.code == 200) {
                mapApiMonthBillToUi(responseBody.data)?.let {
                    Result.Success(it)
                } ?: Result.Error("月账单解析失败 (空数据)")
            } else {
                Result.Error("获取月账单失败: ${response.code()} - ${responseBody?.message ?: response.message()}")
            }
        } catch (e: Exception) {
             Log.e(TAG, "fetchMonthBillSummary Exception", e)
            Result.Error("获取月账单异常: ${e.message}", e)
        }
    }
    
    // Function to fetch consumption trend
    suspend fun fetchConsumeTrend(year: Int, month: Int): Result<ConsumeTrend> = withContext(Dispatchers.IO) {
        val prefix = getWebvpnPrefix() ?: return@withContext loginAndRetry { fetchConsumeTrend(year, month) }
        // !! Adjust API path and parameters !!
        val url = buildWebvpnUrl(prefix, "/publicapp/sys/myyktzd/api/consume/trend.do?year=$year&month=${String.format("%02d", month)}") 
        
        try {
            val response = apiService.getConsumeTrend(url)
            val responseBody = response.body()
            if (response.isSuccessful && responseBody?.code == 200) {
                mapApiConsumeTrendToUi(responseBody.data)?.let {
                    Result.Success(it)
                } ?: Result.Error("消费趋势解析失败 (空数据)")
            } else {
                Result.Error("获取消费趋势失败: ${response.code()} - ${responseBody?.message ?: response.message()}")
            }
        } catch (e: Exception) {
             Log.e(TAG, "fetchConsumeTrend Exception", e)
            Result.Error("获取消费趋势异常: ${e.message}", e)
        }
    }

    // Function to fetch transactions for a specific month
    suspend fun fetchTransactions(year: Int, month: Int, page: Int = 1, pageSize: Int = 20): Result<List<CampusCardTransaction>> = withContext(Dispatchers.IO) {
        val prefix = getWebvpnPrefix() ?: return@withContext loginAndRetry { fetchTransactions(year, month, page, pageSize) }
        
        val apiUrl = buildWebvpnUrl(prefix, "/publicapp/sys/myyktzd/api/getConsumeDetailByMonth.do") // API from README
        val params = mapOf(
            "year" to year.toString(),
            "month" to String.format("%02d", month),
            "pageNumber" to page.toString(),
            "pageSize" to pageSize.toString()
        )

        try {
            val response = apiService.getConsumeDetailByMonth(apiUrl, params)
            val responseBody = response.body()
            if (response.isSuccessful && responseBody?.code == 200) {
                val apiTransactions = responseBody.data?.transactions ?: emptyList()
                // Use explicit type for mapNotNull lambda parameter
                val uiTransactions = apiTransactions.mapNotNull { apiTx: ApiTransaction? -> mapApiTransactionToUi(apiTx) }
                // Save fetched transactions to cache
                if (uiTransactions.isNotEmpty()) {
                    database?.campusCardDao()?.insertAll(uiTransactions)
                }
                Result.Success(uiTransactions)
            } else {
                 Log.e(TAG, "Fetch transactions failed: ${response.code()} - ${response.message()} - ${response.errorBody()?.string()}")
                 Result.Error("获取交易记录失败: ${response.code()} - ${responseBody?.message ?: response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch transactions exception", e)
            Result.Error("获取交易记录异常: ${e.message}", e)
        }
    }
    
    // Helper function to handle re-login attempt
    private suspend fun <T> loginAndRetry(action: suspend () -> Result<T>): Result<T> {
        Log.i(TAG, "WebVPN prefix missing or invalid, attempting re-login...")
        val loginResult = login()
        return if (loginResult is Result.Success && loginResult.data) {
            Log.i(TAG, "Re-login successful, retrying action...")
            action()
        } else {
            Log.e(TAG, "Re-login failed.")
            val errorMsg = if (loginResult is Result.Error) loginResult.message else "登录失败"
            Result.Error("需要重新登录，但登录失败: $errorMsg")
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
    
    suspend fun clearCachedTransactions() {
        database?.let {
            withContext<Unit>(Dispatchers.IO) {
                it.campusCardDao().deleteAllTransactions()
            }
        }
    }

    // --- Helper & Parsing Functions ---

    private fun parseCasLoginForm(html: String): MutableMap<String, String> {
        val data = mutableMapOf<String, String>()
        try {
            val doc = Jsoup.parse(html)
            // Find form elements (adjust selectors based on actual HTML)
            data["lt"] = doc.selectFirst("input[name=lt]")?.attr("value") ?: ""
            data["dllt"] = doc.selectFirst("input[name=dllt]")?.attr("value") ?: "submit"
            data["execution"] = doc.selectFirst("input[name=execution]")?.attr("value") ?: ""
            data["_eventId"] = doc.selectFirst("input[name=_eventId]")?.attr("value") ?: "submit"
            data["rmShown"] = doc.selectFirst("input[name=rmShown]")?.attr("value") ?: "1"
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CAS login form", e)
        }
        // Log parsed form data for debugging
        Log.d(TAG, "Parsed CAS form data: $data")
        return data
    }

    // Extracts the dynamic part, e.g., "/http/webvpn<session_part>/"
     private fun extractWebvpnPrefix(url: String): String? {
        // Regex to capture the base URL and the webvpn prefix part
        val regex = Regex("(https?://[^/]+)(/http/webvpn[a-fA-F0-9]+)/.*")
        val match = regex.find(url)
        // Group 2 should contain the desired prefix, e.g., /http/webvpn...
        return match?.groups?.get(2)?.value?.let { 
             if (it.endsWith("/")) it else "$it/" // Ensure it ends with a slash
        }
    }

    // Builds the full WebVPN URL
    private fun buildWebvpnUrl(prefix: String, apiPath: String): String {
         // Base URL is already part of the prefix extracted
        val baseUrl = "https://webvpn.ahpu.edu.cn"
        // Ensure prefix starts correctly and apiPath starts with /
        val cleanPrefix = if (prefix.startsWith("/")) prefix else "/$prefix"
        val cleanApiPath = if (apiPath.startsWith("/")) apiPath else "/$apiPath"
        
        // Combine, ensuring no double slashes between prefix and apiPath
        val combinedPath = (cleanPrefix.removeSuffix("/") + cleanApiPath).replace("//", "/")
        
        return baseUrl + combinedPath
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
        // Use correct field names from ApiTransaction
        if (apiTx?.id == null || apiTx.time == null || apiTx.amount == null || apiTx.balance == null) {
            return null // Skip invalid transactions
        }
        
        // Format time (e.g., "2025-04-13 17:30")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedTime = try {
            inputFormat.parse(apiTx.time)?.let { outputFormat.format(it) } ?: apiTx.time
        } catch (e: Exception) {
            apiTx.time // Fallback to original time if parsing fails
        }

        val location = apiTx.location ?: "未知地点"
        val terminal = apiTx.terminal ?: ""
        val description = if (terminal.isNotEmpty()) "$location ($terminal)" else location
        
        // Determine transaction type (simple example)
        val type = when {
            apiTx.amount > 0 -> "充值/收入"
            location.contains("餐厅") || location.contains("食堂") -> "餐饮"
            location.contains("超市") -> "购物"
            location.contains("水") || location.contains("浴") || location.contains("电") -> "生活缴费"
            // Add more rules based on location or typeCode (apiTx.typeCode)
            else -> "其他支出"
        }

        return CampusCardTransaction(
            id = apiTx.id,
            time = formattedTime,
            amount = apiTx.amount,
            balance = apiTx.balance,
            type = type,
            location = location,
            description = description
        )
    }
} 