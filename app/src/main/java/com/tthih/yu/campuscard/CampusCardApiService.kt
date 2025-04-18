package com.tthih.yu.campuscard

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.* // Import necessary annotations

interface CampusCardApiService {
    // 1. Initial CAS Login Page Request (to get execution token, etc.)
    @GET
    suspend fun getCasLoginPage(@Url loginUrl: String): Response<ResponseBody>

    // 2. Post CAS Login Credentials
    // The response is often an HTML page with a redirect or a script, hence ResponseBody
    @FormUrlEncoded
    @POST
    suspend fun postCasLogin(
        @Url loginUrl: String,
        @FieldMap loginData: Map<String, String> // Fields like username, password, lt, execution, etc.
    ): Response<ResponseBody>

    // 3. Access the target service via WebVPN after CAS login
    // The exact WebVPN path prefix needs careful construction.
    // Example path: /http/webvpnxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx/publicapp/sys/myyktzd/mobile/index.do
    // This might need adjustments based on how WebVPN constructs the URL.
    // Using @Url allows dynamic URLs
    @GET
    suspend fun getWebvpnService(@Url webVpnUrl: String): Response<ResponseBody>
    
    // --- API Calls via WebVPN --- 
    // These URLs will be prefixed with the WebVPN path.
    // Again, using @Url for flexibility.

    // Example: Get Card Basic Info
    @GET
    suspend fun getCardInfo(@Url url: String): Response<CardInfoResponse> // Defined in CampusCardDataModels.kt

    // Example: Get Month Bill Summary
    @GET
    suspend fun getMonthBill(@Url url: String): Response<MonthBillResponse> // Defined in CampusCardDataModels.kt

    // Example: Get Consumption Trend
    @GET
    suspend fun getConsumeTrend(@Url url: String): Response<ConsumeTrendResponse> // Defined in CampusCardDataModels.kt

    // Example: Get Transaction Details by Month
    @GET
    suspend fun getConsumeDetailByMonth(
        @Url url: String,
        @QueryMap params: Map<String, String> // year, month, pageNumber, pageSize
    ): Response<TransactionDetailResponse> // Defined in CampusCardDataModels.kt
    
    // Add other necessary API calls here...
}

// Removed duplicate data class definitions below // 