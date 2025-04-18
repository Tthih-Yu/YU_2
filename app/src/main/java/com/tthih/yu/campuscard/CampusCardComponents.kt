package com.tthih.yu.campuscard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Display for Consumption Trend
@Composable
fun ConsumeTrendDisplay(trend: ConsumeTrend?) {
    if (trend == null || trend.dates.isEmpty() || trend.amounts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无消费趋势数据")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Statistics Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("消费趋势统计", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    StatItem("总消费", "￥${trend.totalAmount.format(2)}")
                    StatItem("最高单日", "￥${trend.maxAmount.format(2)}")
                    StatItem("日均消费", "￥${trend.averageAmount.format(2)}")
                }
            }
        }

        // Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("近30天消费走势", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SimpleBarChart(dates = trend.dates, amounts = trend.amounts)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

// Simplified Bar Chart Composable
@Composable
fun SimpleBarChart(dates: List<String>, amounts: List<Double>) {
    val maxAmount = amounts.maxOrNull() ?: 1.0 // Avoid division by zero
    val displayData = if (dates.size > 15) { // Show max 15 bars for clarity
        val start = dates.size - 15
        dates.subList(start, dates.size) to amounts.subList(start, amounts.size)
    } else {
        dates to amounts
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) { // Use BoxWithConstraints for dynamic height
        val chartHeight = this.maxHeight - 30.dp // Leave space for labels
        Row(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp, bottom = 22.dp), // Adjusted padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            displayData.first.forEachIndexed { index, date ->
                val amount = displayData.second[index]
                val barHeight = (amount / maxAmount * chartHeight.value).coerceAtLeast(1.0).dp
                val shortDate = date.takeLast(5) // Format date as needed (e.g., MM-DD)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(15.dp) // Bar width
                            .height(barHeight)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = shortDate,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Visible // Allow slight overflow if needed
                    )
                }
            }
        }
    }
}

// Display for Card Info
@Composable
fun CardInfoDisplay(cardInfo: CardInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("我的校园卡", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow("卡号", cardInfo.cardNumber)
            InfoRow("余额", "￥${cardInfo.balance.format(2)}", valueColor = MaterialTheme.colorScheme.tertiary, isBold = true)
            InfoRow("卡状态", cardInfo.status)
            // InfoRow("有效期至", cardInfo.expiryDate)
        }
    }
}

// Display for Monthly Bill Summary
@Composable
fun MonthBillSummary(monthBill: MonthBill) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("本月账单概览", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow("总支出", "￥${monthBill.totalAmount.format(2)}", valueColor = MaterialTheme.colorScheme.error)
            InfoRow("总收入", "+ ￥${monthBill.inAmount.format(2)}", valueColor = Color(0xFF4CAF50)) // Green color for income
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = LocalContentColor.current, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp) // Fixed width for label
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Display for a list of transactions (Used within LazyColumn in Activity)
@Composable
fun TransactionItem(transaction: CampusCardTransaction) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.time, // Already formatted time
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (transaction.amount >= 0) "+${transaction.amount.format(2)}" else transaction.amount.format(2),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (transaction.amount >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

// Helper extension for Double formatting (moved to DataModels file, kept here for reference if needed)
// fun Double?.format(digits: Int): String = String.format("%.${digits}f", this ?: 0.0) 