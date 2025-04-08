package com.tthih.yu.campuscard

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campus_card_transactions")
data class CampusCardTransaction(
    @PrimaryKey
    val id: String, // 交易流水号
    val time: String, // 交易时间
    val amount: Double, // 交易金额
    val balance: Double, // 账户余额
    val type: String, // 交易类型
    val location: String, // 交易地点
    val description: String // 交易描述
) 