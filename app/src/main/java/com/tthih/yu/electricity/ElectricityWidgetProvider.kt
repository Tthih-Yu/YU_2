package com.tthih.yu.electricity

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.util.Log
import android.widget.RemoteViews
import com.tthih.yu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.view.View
import android.graphics.Color
import kotlinx.coroutines.withContext

/**
 * 电费桌面小部件提供者类
 * 负责小部件的创建、更新和数据提供
 */
class ElectricityWidgetProvider : AppWidgetProvider() {
    
    private val TAG = "ElectricityWidget"
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate 被调用，更新${appWidgetIds.size}个小部件")
        // 更新所有小部件实例
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        // 当第一次放置小部件时调用
        Log.d(TAG, "onEnabled 被调用，第一个小部件被添加")
        super.onEnabled(context)
        // 启动定期更新服务
        startWidgetUpdateWork(context)
    }
    
    override fun onDisabled(context: Context) {
        // 当最后一个小部件实例被删除时调用
        Log.d(TAG, "onDisabled 被调用，最后一个小部件被移除")
        super.onDisabled(context)
        // 取消定期更新服务
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive 被调用，action=${intent.action}")
        super.onReceive(context, intent)
        
        // 处理刷新按钮点击
        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.d(TAG, "刷新小部件，ID=$appWidgetId")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            } else {
                Log.d(TAG, "刷新所有小部件")
                updateAllWidgets(context)
            }
        }
    }
    
    companion object {
        const val ACTION_REFRESH = "com.tthih.yu.electricity.ACTION_REFRESH_WIDGET"
        private const val WIDGET_UPDATE_WORK = "electricity_widget_update_work"
        
        // 添加updateAllWidgets方法
        fun updateAllWidgets(context: Context) {
            Log.d("ElectricityWidget", "updateAllWidgets 被调用")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ElectricityWidgetProvider::class.java)
            )
            
            // 更新所有小部件
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
        
        // 启动定期更新任务
        fun startWidgetUpdateWork(context: Context) {
            Log.d("ElectricityWidget", "startWidgetUpdateWork 被调用")
            val updateRequest = PeriodicWorkRequestBuilder<ElectricityWidgetWorker>(
                3, TimeUnit.HOURS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                updateRequest
            )
            
            // 立即更新一次小部件
            updateAllWidgets(context)
        }
        
        // 更新小部件显示
        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            Log.d("ElectricityWidget", "更新小部件 ID: $appWidgetId")
            
            // 创建小部件视图
            val views = RemoteViews(context.packageName, R.layout.electricity_widget)
            
            // 设置点击打开主应用的意图
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ElectricityActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            // 添加刷新按钮的点击事件
            val refreshIntent = Intent(context, ElectricityWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId, // 使用widgetId作为请求码，确保每个widget的PendingIntent都不同
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
            
            // 先显示加载中
            views.setTextViewText(R.id.electricity_amount, "加载中")
            views.setTextViewText(R.id.remaining_days, "...")
            views.setTextViewText(R.id.remaining_days_large, "...")
            views.setViewVisibility(R.id.daily_usage_container, View.GONE)
            views.setTextViewText(R.id.last_update_time, "正在更新...")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // 异步加载数据并更新小部件
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 从仓库获取电量数据
                    val repository = ElectricityRepository(context)
                    val electricityData = repository.getLastElectricityData()
                    // 获取低电量阈值
                    val lowBalanceThreshold = repository.getLowBalanceThreshold()
                    
                    // 在主线程更新UI
                    withContext(Dispatchers.Main) {
                        if (electricityData != null) {
                            // 更新电费余额 - 确保有效值
                            if (electricityData.balance > 0) {
                                views.setTextViewText(
                                    R.id.electricity_amount,
                                    String.format(Locale.CHINA, "%.1f", electricityData.balance)
                                )
                                
                                // 根据余额与阈值的比较设置颜色
                                if (electricityData.balance < lowBalanceThreshold) {
                                    // 低于阈值，设置为红色
                                    views.setTextColor(R.id.electricity_amount, Color.parseColor("#FF5722"))
                                } else {
                                    // 正常余额，设置为明显的颜色（浅绿色）
                                    views.setTextColor(R.id.electricity_amount, Color.parseColor("#4CAF50"))
                                }
                            } else {
                                views.setTextViewText(R.id.electricity_amount, "未知")
                                views.setTextColor(R.id.electricity_amount, Color.parseColor("#FFC107")) // 黄色警告
                            }
                            
                            // 更新日均用电量 - 确保有有效值或提供估计值
                            if (electricityData.dailyUsage > 0) {
                                views.setTextViewText(
                                    R.id.daily_usage,
                                    String.format(Locale.CHINA, "%.1f", electricityData.dailyUsage)
                                )
                                views.setViewVisibility(R.id.daily_usage_container, View.VISIBLE)
                            } else if (electricityData.balance > 0) {
                                // 如果只有余额数据，显示估计值
                                views.setTextViewText(R.id.daily_usage, "约2.5")
                                views.setViewVisibility(R.id.daily_usage_container, View.VISIBLE)
                            } else {
                                views.setViewVisibility(R.id.daily_usage_container, View.GONE)
                            }
                            
                            // 更新预计剩余天数 - 显示更精确的数值或基于余额的估计
                            if (electricityData.estimatedDays > 0) {
                                updateRemainingDays(views, electricityData.estimatedDays)
                            } else if (electricityData.balance > 0) {
                                // 如果有余额但没有估计天数，计算一个估计值
                                val estimatedDays = (electricityData.balance / 2.5f).toInt()
                                updateRemainingDays(views, estimatedDays)
                            } else {
                                views.setTextViewText(R.id.remaining_days, "--")
                                views.setTextViewText(R.id.remaining_days_large, "--")
                            }
                            
                            // 更新更新时间
                            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                            val updateTime = dateFormat.format(Date())
                            views.setTextViewText(R.id.last_update_time, "更新: $updateTime")
                        } else {
                            // 无数据
                            views.setTextViewText(R.id.electricity_amount, "--")
                            views.setTextViewText(R.id.remaining_days, "--")
                            views.setTextViewText(R.id.remaining_days_large, "--")
                            views.setViewVisibility(R.id.daily_usage_container, View.GONE)
                            views.setTextViewText(R.id.last_update_time, "暂无数据")
                        }
                        
                        // 更新小部件
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    // 出现异常，显示错误信息
                    Log.e("ElectricityWidget", "更新小部件出错: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.electricity_amount, "--")
                        views.setTextViewText(R.id.remaining_days, "--")
                        views.setTextViewText(R.id.remaining_days_large, "--")
                        views.setViewVisibility(R.id.daily_usage_container, View.GONE)
                        views.setTextViewText(R.id.last_update_time, "更新失败")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
        
        // 辅助方法：更新剩余天数显示
        private fun updateRemainingDays(views: RemoteViews, days: Int) {
            // 更新小号显示
            views.setTextViewText(R.id.remaining_days, days.toString())
            
            // 同时更新大号显示
            views.setTextViewText(R.id.remaining_days_large, days.toString())
            
            // 根据剩余天数设置不同的颜色
            val textColor = when {
                days <= 3 -> "#FF5722" // 红色警告
                days <= 7 -> "#FFC107" // 黄色警告
                else -> "#FFFFFF" // 白色正常
            }
            views.setTextColor(R.id.remaining_days, Color.parseColor(textColor))
            views.setTextColor(R.id.remaining_days_large, Color.parseColor(textColor))
        }
    }
} 