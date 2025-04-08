package com.tthih.yu.electricity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "electricity_history")
data class ElectricityHistoryData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date,
    val balance: Double,
    val building: String = "",  // 宿舍楼
    val roomId: String = "",    // 房间号
    val usage: Double = 0.0,
    val recharge: Double = 0.0
) {
    /**
     * 计算与前一天数据的变化
     * 返回余额的变化量：正值表示充值（增加），负值表示用电（减少）
     */
    fun calculateChange(previousData: ElectricityHistoryData?): Double {
        if (previousData == null) return 0.0
        // 余额的变化：正值表示充值，负值表示用电
        return balance - previousData.balance
    }
    
    /**
     * 检查是否是充值日
     */
    fun isRechargeDay(): Boolean {
        return recharge > 0.0
    }
    
    // 获取格式化的日期（yyyy-MM-dd）
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }
    
    // 获取当前日期的月份（yyyy年MM月）
    fun getYearMonth(): String {
        val sdf = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        return sdf.format(date)
    }
    
    // 获取当前日期的日
    fun getDayOfMonth(): Int {
        val sdf = SimpleDateFormat("d", Locale.getDefault())
        return sdf.format(date).toInt()
    }

    companion object {
        /**
         * 从服务器响应数据创建历史数据对象
         */
        fun fromResponse(responseData: Map<String, Any>): ElectricityHistoryData {
            val date = responseData["date"] as? Date ?: Date()
            val balance = (responseData["balance"] as? Number)?.toDouble() ?: 0.0
            val building = responseData["building"] as? String ?: ""
            val roomId = responseData["roomId"] as? String ?: ""
            val usage = (responseData["usage"] as? Number)?.toDouble() ?: 0.0
            val recharge = (responseData["recharge"] as? Number)?.toDouble() ?: 0.0
            
            return ElectricityHistoryData(0, date, balance, building, roomId, usage, recharge)
        }
    }
} 