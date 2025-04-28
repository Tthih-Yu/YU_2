package com.tthih.yu.campuscard

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Helper to format milliseconds to date string
fun Long.toFormattedDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(this))

// Helper to show Date Picker Dialog
fun showDatePickerDialog(
    context: Context,
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    minDateMillis: Long? = null, // Optional minimum date
    maxDateMillis: Long? = null  // Optional maximum date
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = initialDateMillis
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(context, { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
        val selectedCalendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        onDateSelected(selectedCalendar.timeInMillis)
    }, year, month, day).apply {
        // Apply date constraints if provided
        minDateMillis?.let { datePicker.minDate = it }
        maxDateMillis?.let { datePicker.maxDate = it }
        show()
    }
}

// Display for Consumption Trend - Modified to accept transactions and handle date range
@Composable
fun ConsumeTrendDisplay(transactions: List<CampusCardTransaction>) {
    val context = LocalContext.current
    
    // State for selected date range
    val initialCalendar = remember { Calendar.getInstance() }
    val defaultEndDateMillis = remember { initialCalendar.timeInMillis }
    initialCalendar.add(Calendar.DAY_OF_YEAR, -30) // Default to last 30 days
    val defaultStartDateMillis = remember { initialCalendar.timeInMillis }

    var startDateMillis by remember { mutableStateOf(defaultStartDateMillis) }
    var endDateMillis by remember { mutableStateOf(defaultEndDateMillis) }

    // --- Date Formatters (moved inside where needed or use extension) ---
    val dateFormatWithSeconds = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val dateFormatWithoutSeconds = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Filter transactions based on the selected date range
    val filteredTransactions = remember(transactions, startDateMillis, endDateMillis) {
        transactions.filter { tx ->
            try {
                val date = try { dateFormatWithSeconds.parse(tx.time) } catch (e: Exception) { try { dateFormatWithoutSeconds.parse(tx.time) } catch (e: Exception) { null } }
                date != null && date.time >= startDateMillis && date.time <= (endDateMillis + TimeUnit.DAYS.toMillis(1) - 1) // Include the whole end day
            } catch (e: Exception) {
                false
            }
        }
    }

    // Calculate trend data based on filtered transactions
    val trendData = remember(filteredTransactions) {
        if (filteredTransactions.isEmpty()) {
             ConsumeTrend() // Return empty trend if no data in range
        } else {
            val dailyTotals = filteredTransactions
                .mapNotNull { tx ->
                    var date: Date? = null
                    try {
                        date = try { dateFormatWithSeconds.parse(tx.time) } catch (e: Exception) { try { dateFormatWithoutSeconds.parse(tx.time) } catch (e: Exception) { null } }
                        if (date != null && tx.amount < 0) { // Only consider expenses for trend
                            dayFormat.format(date) to -tx.amount
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                .groupBy { it.first } // Group by yyyy-MM-dd string
                .mapValues { entry -> entry.value.sumOf { it.second } } // Sum amounts for each day
                .toList() // Convert Map to List<Pair<String, Double>>
                .sortedBy { it.first } // Sort by date string
                
            val dates = dailyTotals.map { it.first.takeLast(5) } // Keep MM-dd format
            val amounts = dailyTotals.map { it.second }
            
            if (dates.isEmpty() || amounts.isEmpty()) {
                ConsumeTrend()
            } else {
                val totalAmount = amounts.sum()
                val maxAmount = amounts.maxOrNull() ?: 0.0
                val averageAmount = if (amounts.isNotEmpty()) totalAmount / amounts.size else 0.0
                ConsumeTrend(
                    dates = dates,
                    amounts = amounts,
                    maxAmount = maxAmount,
                    totalAmount = totalAmount,
                    averageAmount = averageAmount
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Date Range Selection --- 
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "选择日期范围", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = { 
                        showDatePickerDialog(context, startDateMillis, {
                            startDateMillis = it
                            // Ensure start date is not after end date
                            if (startDateMillis > endDateMillis) endDateMillis = startDateMillis
                        }, maxDateMillis = System.currentTimeMillis()) // Cannot select future date
                    }) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(startDateMillis.toFormattedDate())
                    }
                    Text("至")
                    Button(onClick = { 
                        showDatePickerDialog(context, endDateMillis, {
                            endDateMillis = it
                            // Ensure end date is not before start date
                            if (endDateMillis < startDateMillis) startDateMillis = endDateMillis
                        }, minDateMillis = startDateMillis, maxDateMillis = System.currentTimeMillis())
                     }) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(endDateMillis.toFormattedDate())
                    }
                }
            }
        }

        // --- Statistics Card --- (Uses calculated trendData)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                 Text(
                    "范围统计", // Changed title
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TrendStatItem(
                        label = "总消费", 
                        value = "￥${trendData.totalAmount.format(2)}", // Use trendData
                        icon = "💰"
                    )
                    TrendStatItem(
                        label = "最高单日", 
                        value = "￥${trendData.maxAmount.format(2)}", // Use trendData
                        icon = "📈"
                    )
                    TrendStatItem(
                        label = "日均消费", 
                        value = "￥${trendData.averageAmount.format(2)}", // Use trendData
                        icon = "📊"
                    )
                }
                
                val nonZeroDays = trendData.amounts.count { it > 0 }
                val totalDaysInRange = TimeUnit.MILLISECONDS.toDays(endDateMillis - startDateMillis).toInt() + 1 // +1 to include both days
                val percentageActive = if (totalDaysInRange > 0) {
                    (nonZeroDays.toFloat() / totalDaysInRange) * 100
                } else 0f
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TrendStatItem(
                        label = "消费天数", 
                        value = "$nonZeroDays/$totalDaysInRange",
                        icon = "📅"
                    )
                    TrendStatItem(
                        label = "消费频率", 
                        value = "${percentageActive.toInt()}%",
                        icon = "🔄"
                    )
                }
            }
        }

        // --- Chart Card --- (Uses calculated trendData)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "消费走势", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "${trendData.dates.size}天数据", // Updated label
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Pass calculated dates and amounts from trendData
                    SimpleBarChart(dates = trendData.dates, amounts = trendData.amounts)
                }
                
                // Trend analysis (can be kept simple or adapted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastWeekAvg = if (trendData.amounts.size >= 7) {
                        trendData.amounts.takeLast(7).average()
                    } else trendData.averageAmount
                    
                    val prevWeekAvg = if (trendData.amounts.size >= 14) {
                        trendData.amounts.dropLast(7).takeLast(7).average()
                    } else trendData.averageAmount
                    
                    val weekTrendValue = lastWeekAvg - prevWeekAvg
                    val trendText = when {
                        weekTrendValue > 5 -> "近期消费有上升趋势"
                        weekTrendValue < -5 -> "近期消费有下降趋势"
                        else -> "近期消费保持稳定"
                    }
                    
                    val trendIcon = when {
                        weekTrendValue > 5 -> "⬆️"
                        weekTrendValue < -5 -> "⬇️"
                        else -> "➡️"
                    }
                    
                    Text(
                        if(trendData.amounts.size >= 14) "$trendIcon $trendText (周对比)" else "", // Only show if enough data
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendStatItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            label, 
            style = MaterialTheme.typography.bodySmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value, 
            style = MaterialTheme.typography.titleMedium, 
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Simplified Bar Chart Composable
@Composable
fun SimpleBarChart(dates: List<String>, amounts: List<Double>) {
    if (dates.isEmpty() || amounts.isEmpty() || dates.size != amounts.size) {
        Text("无效的图表数据", color = MaterialTheme.colorScheme.error)
        return
    }

    val maxAmount = amounts.maxOrNull() ?: 1.0 // Avoid division by zero
    val displayData = if (dates.size > 15) { // Show max 15 bars for clarity
        val start = dates.size - 15
        dates.subList(start, dates.size) to amounts.subList(start, amounts.size)
    } else {
        dates to amounts
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) { // Use BoxWithConstraints for dynamic height
        val chartHeight = this.maxHeight - 30.dp // Leave space for labels
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Y轴最大值标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "最高: ¥${maxAmount.format(0)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 图表区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 22.dp), // 调整下边距为标签留出空间
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
                displayData.first.zip(displayData.second).forEachIndexed { index, (date, amount) ->
                val barHeight = (amount / maxAmount * chartHeight.value).coerceAtLeast(1.0).dp
                val shortDate = date.takeLast(5) // Format date as needed (e.g., MM-DD)

                    // 使用渐变色和圆角效果提升视觉效果
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                                .width(12.dp) // 稍微窄一点的柱子
                            .height(barHeight)
                            .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            MaterialTheme.colorScheme.primary
                                        )
                                    ),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                        
                    Spacer(modifier = Modifier.height(4.dp))
                        
                        // 日期标签
                    Text(
                        text = shortDate,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                            overflow = TextOverflow.Visible, // Allow slight overflow if needed
                            color = if (index % 3 == 0) MaterialTheme.colorScheme.onSurface 
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 主要信息卡片 - 本月消费摘要
    Card(
        modifier = Modifier
            .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
    ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "本月账单概览", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 显示余额变化
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val netChange = monthBill.inAmount - monthBill.totalAmount
                            val netChangeText = if (netChange >= 0) "+¥${netChange.format(2)}" else "-¥${(-netChange).format(2)}"
                            val netChangeColor = if (netChange >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            
                            Text(
                                "本月余额变化",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                netChangeText,
                                style = MaterialTheme.typography.headlineMedium,
                                color = netChangeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 支出和收入详情
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "总支出",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "¥${monthBill.totalAmount.format(2)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "总收入",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "¥${monthBill.inAmount.format(2)}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // 分析卡片 - 每日平均消费
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "消费分析", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 获取当月天数和当前日期
                    val cal = Calendar.getInstance() // 获取当前日期时间
                    val currentDayOfMonth = cal.get(Calendar.DAY_OF_MONTH) // 当前是几号
                    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH) // 本月总天数
                    
                    // 修正计算逻辑
                    val daysPassed = currentDayOfMonth // 已过天数就是当前日期号
                    val dailyAvg = if (daysPassed > 0) monthBill.totalAmount / daysPassed else 0.0
                    val projectedTotal = dailyAvg * daysInMonth
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoColumn(
                            label = "日均消费",
                            value = "¥${dailyAvg.format(2)}",
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                        
                        InfoColumn(
                            label = "预计月底",
                            value = "¥${projectedTotal.format(2)}",
                            valueColor = MaterialTheme.colorScheme.tertiary
                        )
                        
                        InfoColumn(
                            label = "已经过天数",
                            value = "$daysPassed/$daysInMonth",
                            valueColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
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
    val categoryIconAndColor = getCategoryIconAndColor(transaction.type, transaction.location)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类别图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = categoryIconAndColor.second.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryIconAndColor.first,
                    fontSize = 20.sp,
                    color = categoryIconAndColor.second
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 交易信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 交易描述
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // 交易金额
            Text(
                text = if (transaction.amount >= 0) "+${transaction.amount.format(2)}" else transaction.amount.format(2),
                style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                color = if (transaction.amount >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 底部信息栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 交易时间
                    Text(
                        text = transaction.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 交易后余额
                    Text(
                        text = "余额: ${transaction.balance.format(2)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 可选：添加交易类别标签
                if (transaction.type.isNotBlank() && transaction.type != "其他支出") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = categoryIconAndColor.second.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryIconAndColor.second
                        )
                    }
                }
            }
        }
    }
}

// 根据交易类型和地点获取相应的图标和颜色
@Composable
fun getCategoryIconAndColor(type: String, location: String): Pair<String, Color> {
    return when {
        type.contains("充值") || type.contains("收入") -> 
            Pair("💰", Color(0xFF4CAF50)) // 绿色，充值/收入
        type.contains("餐饮") || location.contains("食堂") || location.contains("餐厅") ->
            Pair("🍚", Color(0xFFFF9800)) // 橙色，餐饮
        type.contains("购物") || location.contains("超市") || location.contains("商店") ->
            Pair("🛒", Color(0xFF2196F3)) // 蓝色，购物
        location.contains("水") || type.contains("水") ->
            Pair("💧", Color(0xFF03A9F4)) // 浅蓝色，水费
        location.contains("电") || type.contains("电") ->
            Pair("⚡", Color(0xFFFFEB3B)) // 黄色，电费
        location.contains("浴") || type.contains("浴") ->
            Pair("🚿", Color(0xFF00BCD4)) // 青色，洗浴
        type.contains("生活") || type.contains("缴费") ->
            Pair("🏠", Color(0xFF9C27B0)) // 紫色，生活缴费
        else ->
            Pair("📝", Color(0xFF607D8B)) // 灰蓝色，其他支出
    }
}

// Helper extension for Double formatting (moved to DataModels file, kept here for reference if needed)
// fun Double?.format(digits: Int): String = String.format("%.${digits}f", this ?: 0.0) 

// --- 新增图表相关组件 ---

// 图表选择芯片组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

// 饼图组件（用于预定义分类支出、交易类型等）
@Composable
fun PieChartDisplay(
    data: List<Pair<String, Float>>,
    title: String
) {
    // 暂时使用简单的文本列表代替实际的饼图，后续可以引入第三方库
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "饼图数据 - $title",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val total = data.sumOf { it.second.toDouble() }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(data.size) { index ->
                val (category, value) = data[index]
                val percentage = (value / total * 100).toFloat()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                getColorForIndex(index),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        category,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${String.format("%.2f", value)}元 (${String.format("%.1f", percentage)}%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// 月度收支柱状图
@Composable
fun MonthlyIncomeExpenseChart(data: List<Triple<String, Float, Float>>) {
    // 暂时使用文本列表替代，后续可引入MPAndroidChart或其他库实现柱状图
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "月度收支数据",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(data.size) { index ->
                val (month, income, expense) = data[index]
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        month,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 收入进度条
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                        ) {
                            // 安全处理进度条值，避免NaN
                            val safeMaxValue = income.coerceAtLeast(expense).coerceAtLeast(0.01f) // 确保分母不为0
                            val safeProgress = (income / (safeMaxValue * 1.2f)).coerceIn(0f, 1f) // 限制在0-1范围内
                            if (safeProgress.isFinite()) { // 额外检查确保不是NaN或Infinity
                                LinearProgressIndicator(
                                    progress = safeProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp),
                                    color = Color(0xFF4CAF50) // 绿色表示收入
                                )
                            } else {
                                // 如果计算出的值无效，显示一个小进度条或空白
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.1f) // 10%的宽度
                                        .height(20.dp)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${String.format("%.2f", income)}元",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 支出进度条
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                        ) {
                            // 安全处理进度条值，避免NaN
                            val safeMaxValue = income.coerceAtLeast(expense).coerceAtLeast(0.01f) // 确保分母不为0
                            val safeProgress = (expense / (safeMaxValue * 1.2f)).coerceIn(0f, 1f) // 限制在0-1范围内
                            if (safeProgress.isFinite()) { // 额外检查确保不是NaN或Infinity
                                LinearProgressIndicator(
                                    progress = safeProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp),
                                    color = Color(0xFFE57373) // 红色表示支出
                                )
                            } else {
                                // 如果计算出的值无效，显示一个小进度条或空白
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.1f) // 10%的宽度
                                        .height(20.dp)
                                        .background(Color(0xFFE57373))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${String.format("%.2f", expense)}元",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (index < data.size - 1) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// 商户排行柱状图
@Composable
fun TopMerchantsChart(data: List<Pair<String, Float>>) {
    val sortedData = data.sortedByDescending { it.second }
    val maxValue = sortedData.maxOfOrNull { it.second }?.coerceAtLeast(1f) ?: 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "商户排行",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                itemsIndexed(sortedData) { index, item ->
                    val merchant = item.first
                    val amount = item.second
                    val safeProgress = if (maxValue > 0 && amount.isFinite()) amount / maxValue else 0f
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${index + 1}. $merchant",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "¥${amount.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.End
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        LinearProgressIndicator(
                            progress = { safeProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        }
    }
}

// 星期消费柱状图
@Composable
fun DayOfWeekBarChart(data: List<Pair<String, Float>>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "按星期消费分布",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val maxAmount = data.maxOfOrNull { it.second } ?: 1f
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, (day, amount) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val barHeight = (amount / maxAmount * 150).coerceAtLeast(1f).dp
                    
                    Text(
                        "${String.format("%.0f", amount)}元",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(barHeight)
                            .background(
                                getColorForIndex(index),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    
                    Text(
                        day,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// 小时消费柱状图
@Composable
fun HourOfDayBarChart(data: List<Pair<String, Float>>) {
    // 简化版，只显示部分小时数据以适应屏幕
    val visibleHours = data.chunked(4).map { chunk ->
        chunk.maxByOrNull { it.second } ?: (chunk.firstOrNull() ?: ("00:00" to 0f))
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "按小时消费分布",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val maxAmount = data.maxOfOrNull { it.second } ?: 1f
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            visibleHours.forEachIndexed { index, (hour, amount) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val barHeight = (amount / maxAmount * 150).coerceAtLeast(1f).dp
                    
                    Text(
                        "${String.format("%.0f", amount)}元",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(barHeight)
                            .background(
                                getColorForIndex(index),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    
                    Text(
                        hour,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        Text(
            "注：为显示清晰，合并了相邻小时数据",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

// 消费区间柱状图
@Composable
fun SpendingRangeBarChart(data: List<Pair<String, Float>>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "不同金额区间交易次数",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val maxCount = data.maxOfOrNull { it.second } ?: 1f
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, (range, count) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val barHeight = (count / maxCount * 150).coerceAtLeast(1f).dp
                    
                    Text(
                        "${count.toInt()}次",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(barHeight)
                            .background(
                                getColorForIndex(index),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    
                    Text(
                        "¥$range",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// 获取颜色的辅助函数
@Composable
fun getColorForIndex(index: Int): Color {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF4CAF50), // 绿色
        Color(0xFFFF9800), // 橙色
        Color(0xFF2196F3), // 蓝色
        Color(0xFFE91E63), // 粉色
        Color(0xFF673AB7), // 紫色
        Color(0xFF795548), // 棕色
        Color(0xFF009688)  // 青绿色
    )
    return colors[index % colors.size]
}

// 图表统计辅助函数
@Composable
fun ChartSummaryRow(
    leftItem: Pair<String, String>,
    middleItem: Pair<String, String>,
    rightItem: Pair<String, String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatInfoItem(leftItem.first, leftItem.second)
        StatInfoItem(middleItem.first, middleItem.second)
        StatInfoItem(rightItem.first, rightItem.second)
    }
}

@Composable
fun StatInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
} 