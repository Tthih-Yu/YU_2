package com.tthih.yu.campuscard

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// --- UI Data Models ---

// Represents basic card info for UI display
data class CardInfo(
    val cardNumber: String = "",
    val balance: Double = 0.0,
    val expiryDate: String = "",
    val status: String = ""
)

// Represents monthly bill summary for UI display
data class MonthBill(
    val totalAmount: Double = 0.0, // Total spending this month
    val inAmount: Double = 0.0 // Total income/recharge this month
)

// Represents consumption trend data for UI display
data class ConsumeTrend(
    val dates: List<String> = emptyList(), // List of dates (e.g., "04-13")
    val amounts: List<Double> = emptyList(), // List of corresponding daily consumption amounts
    val maxAmount: Double = 0.0, // Max daily consumption in the period
    val totalAmount: Double = 0.0, // Total consumption in the period
    val averageAmount: Double = 0.0 // Average daily consumption
)

// Represents a single transaction for UI display and database storage
@Entity(tableName = "campus_card_transactions")
data class CampusCardTransaction(
    @PrimaryKey
    val id: String, // Transaction ID (JYLSH)
    val time: String, // Formatted time string
    val amount: Double, // Transaction amount (+ for income, - for expense)
    val balance: Double, // Balance after transaction
    val type: String, // Transaction type (e.g., "餐饮", "充值", "其他")
    val location: String, // Location (SHMC)
    val description: String // Description (e.g., SHMC + ZDMC)
)

// --- API Response Data Models ---

// Generic response wrapper (if applicable)
data class ApiResponse<T>(
    val code: Int, // e.g., 200 for success
    val msg: String?,
    val datas: T? // Actual data payload
)

// Response for Card Info API
data class CardInfoResponse(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("msg") val message: String? = null,
    @SerializedName("datas") val data: CardInfoData? = null
)

data class CardInfoData(
    @SerializedName("accountId") val cardNumber: String? = null,
    @SerializedName("balance") val balance: String? = null, // API might return String
    @SerializedName("expiryDate") val expiryDate: String? = null,
    @SerializedName("accountStatus") val status: String? = null // Or accountState, etc.
    // Add other fields as needed based on actual API response
)

// Response for Month Bill Summary API
data class MonthBillResponse(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("msg") val message: String? = null,
    @SerializedName("datas") val data: MonthBillData? = null
)

data class MonthBillData(
    @SerializedName("totalAmount") val totalAmount: String? = null, // Or JYZE
    @SerializedName("inAmount") val inAmount: String? = null // Or SRJE
    // Add other fields as needed
)

// Response for Consumption Trend API - Multiple possible formats
data class ConsumeTrendResponse(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("msg") val message: String? = null,
    @SerializedName("datas") val data: ConsumeTrendData? = null
)

data class ConsumeTrendData(
    // Format 1: Separate lists
    val xData: List<String>? = null,
    val yData: List<Double>? = null,
    // Format 2: List of pairs
    val serisData: List<List<Any>>? = null // e.g., [["04-13", 15.48], ...]
    // Add other potential formats
)

// Response for Transaction Details API
data class TransactionDetailResponse(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("msg") val message: String? = null,
    @SerializedName("datas") val data: TransactionListData? = null // Often nested
)

data class TransactionListData(
    @SerializedName("rows") val transactions: List<ApiTransaction>? = null,
    @SerializedName("total") val totalRecords: Int? = null // Total number of records available
    // Other pagination info might be present
)

// Raw transaction data from API (e.g., inside 'rows')
data class ApiTransaction(
    @SerializedName("JYLSH") val id: String?, // Transaction ID
    @SerializedName("JYSJ") val time: String?, // Transaction time (e.g., "2025-04-13 17:30:00")
    @SerializedName("JYJE") val amount: Double?, // Transaction amount
    @SerializedName("YE") val balance: Double?, // Balance after transaction
    @SerializedName("SHMC") val location: String?, // Merchant name
    @SerializedName("ZDMC") val terminal: String?, // Terminal name
    @SerializedName("JYKM") val typeCode: String? // Transaction type code/name
    // Add other fields if available from the API
)

// Helper extension for formatting
fun Double?.format(digits: Int): String = String.format("%.${digits}f", this ?: 0.0) 