package com.tthih.yu.electricity

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tthih.yu.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * 日历日期适配器
 * 用于在RecyclerView中展示日历格式的电费历史数据
 *
 * @param context 上下文
 * @param daysInMonth 当月天数
 * @param firstDayOfWeek 当月第一天是星期几（0=周日，1=周一，...）
 * @param historyData 当月电费历史数据，以日期为键
 * @param previousMonthData 上月电费历史数据，仅包含上月最后一天
 * @param currentCalendar 当前日历对象
 */
class CalendarDayAdapter(
    private val context: Context,
    private val daysInMonth: Int,
    private val firstDayOfWeek: Int,
    private val historyData: Map<Int, ElectricityHistoryData>,
    private val previousMonthData: Map<Int, ElectricityHistoryData>,
    private val currentCalendar: Calendar
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    // 一共显示6行7列 = 42个日期单元格
    private val totalDays = 42
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun getItemCount(): Int = totalDays

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        // 计算日期
        val day = position - firstDayOfWeek + 1
        
        if (day <= 0 || day > daysInMonth) {
            // 不是当月的日期，隐藏显示
            holder.dayCell.visibility = View.INVISIBLE
            return
        } else {
            holder.dayCell.visibility = View.VISIBLE
        }
        
        // 设置日期文本
        holder.dayText.text = day.toString()
        
        // 获取当天的数据
        val todayData = historyData[day]
        
        if (todayData != null) {
            // 直接使用电费记录中的充值和用电量
            val recharge = todayData.recharge
            val usage = todayData.usage
            
            // 优先显示较大的那个值
            if (recharge > 0) {
                // 有充值记录，显示充值金额
                val formattedRecharge = String.format(Locale.getDefault(), "%.2f", recharge)
                holder.balanceChangeText.text = formattedRecharge
                holder.balanceChangeText.setTextColor(ContextCompat.getColor(context, R.color.matcha_danger))
                // 设置充值日颜色
                holder.dayCell.setBackgroundColor(Color.parseColor("#FFEBEE"))
                
                Log.d("CalendarAdapter", "日期 $day: 显示充值 $formattedRecharge")
            } else if (usage > 0) {
                // 有用电记录，显示用电量，带负号
                val formattedUsage = String.format(Locale.getDefault(), "%.2f", usage)
                holder.balanceChangeText.text = "-" + formattedUsage
                holder.balanceChangeText.setTextColor(ContextCompat.getColor(context, R.color.matcha_success))
                
                // 如果用电量较大，设置高用电日颜色
                if (usage > 3.0) {
                    holder.dayCell.setBackgroundColor(Color.parseColor("#E8F5E9"))
                } else {
                    holder.dayCell.setBackgroundColor(Color.WHITE)
                }
                
                Log.d("CalendarAdapter", "日期 $day: 显示用电 -$formattedUsage")
            } else {
                // 无变化，灰色 0
                holder.balanceChangeText.text = "0"
                holder.balanceChangeText.setTextColor(ContextCompat.getColor(context, R.color.matcha_text_secondary))
                holder.dayCell.setBackgroundColor(Color.WHITE)
                
                Log.d("CalendarAdapter", "日期 $day: 无变化")
            }
        } else {
            // 没有数据，显示0
            holder.balanceChangeText.text = "0"
            holder.balanceChangeText.setTextColor(ContextCompat.getColor(context, R.color.matcha_text_secondary))
            holder.dayCell.setBackgroundColor(Color.WHITE)
            
            Log.d("CalendarAdapter", "日期 $day: 无数据")
        }
        
        // 如果是周末，日期文本设为红色
        val calendar = Calendar.getInstance()
        // 使用当前Activity的calendar对象的年和月，确保正确判断
        calendar.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
        calendar.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH))
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
            holder.dayText.setTextColor(ContextCompat.getColor(context, R.color.matcha_danger))
        } else {
            holder.dayText.setTextColor(ContextCompat.getColor(context, R.color.matcha_text_primary))
        }
    }
    
    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayCell: LinearLayout = itemView.findViewById(R.id.day_cell)
        val dayText: TextView = itemView.findViewById(R.id.tv_day)
        val balanceChangeText: TextView = itemView.findViewById(R.id.tv_balance_change)
    }
} 