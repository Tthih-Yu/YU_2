package com.tthih.yu.electricity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 电费数据模型
 */
@Entity(tableName = "electricity_data")
data class ElectricityData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 电费余额
    val balance: Float = 0f,
    
    // 时间戳
    val timestamp: String = "",
    
    // 房间号
    val roomId: String = "",
    
    // 宿舍楼
    val building: String = "",
    
    // 下次刷新时间
    val nextRefresh: String = "",
    
    // 刷新间隔（分钟）
    val refreshInterval: Int = 30,
    
    // 日均用电（元/天）
    val dailyUsage: Float = 0f,
    
    // 预计可用天数
    val estimatedDays: Int = 0
) 