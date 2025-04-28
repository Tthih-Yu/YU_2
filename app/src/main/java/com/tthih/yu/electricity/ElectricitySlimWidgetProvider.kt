package com.tthih.yu.electricity

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tthih.yu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.SharedPreferences

/**
 * 电费迷你桌面小部件提供者类 (2x1)
 * 左侧显示大字体余额，右侧为刷新按钮，背景根据低余额阈值变色
 */
class ElectricitySlimWidgetProvider : AppWidgetProvider() {
    
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
        // 立即更新一次小部件
        updateAllWidgets(context)
    }
    
    override fun onDisabled(context: Context) {
        // 当最后一个小部件实例被删除时调用
        Log.d(TAG, "onDisabled 被调用，最后一个小部件被移除")
        super.onDisabled(context)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive 被调用，action=${intent.action}")
        super.onReceive(context, intent)
        
        // 恢复处理刷新按钮点击
        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.d(TAG, "刷新小部件，ID=$appWidgetId")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                // 显示加载状态并触发刷新
                showLoadingAndUpdate(context, appWidgetManager, appWidgetId)
            } else {
                Log.d(TAG, "刷新所有小部件")
                updateAllWidgets(context) // 刷新所有实例
            }
            
            // 触发后台数据更新
            triggerImmediateRefresh(context)
        }
    }
    
    companion object {
        private const val TAG = "ElectricitySlimWidget"
        // 恢复 ACTION_REFRESH
        const val ACTION_REFRESH = "com.tthih.yu.electricity.ACTION_REFRESH_SLIM_WIDGET"
        
        // 更新所有小部件
        fun updateAllWidgets(context: Context) {
            Log.d(TAG, "updateAllWidgets 被调用")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ElectricitySlimWidgetProvider::class.java)
            )
            
            // 更新所有小部件
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
        
        // 手动触发立即刷新 (后台 Worker)
        fun triggerImmediateRefresh(context: Context) {
            Log.d(TAG, "手动触发后台刷新任务")
            val refreshWork = OneTimeWorkRequestBuilder<ElectricityWidgetWorker>().build()
            WorkManager.getInstance(context).enqueue(refreshWork)
        }

        // 显示加载状态并触发刷新
        private fun showLoadingAndUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
             Log.d(TAG, "显示加载状态，小部件 ID: $appWidgetId")
             val views = RemoteViews(context.packageName, R.layout.electricity_slim_widget_layout)
             views.setTextViewText(R.id.tv_widget_slim_balance, "...")
             // 可以考虑设置一个中性的加载背景色
             // views.setInt(R.id.widget_slim_background, "setBackgroundResource", R.color.matcha_text_hint)
             try {
                 appWidgetManager.updateAppWidget(appWidgetId, views)
                 Log.d(TAG, "显示加载状态成功，小部件 ID: $appWidgetId")
                 // 触发实际的数据更新
                 updateAppWidget(context, appWidgetManager, appWidgetId)
             } catch (e: Exception) {
                 Log.e(TAG, "显示加载状态时出错，小部件 ID: $appWidgetId", e)
             }
        }
        
        // 更新小部件显示
        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            Log.i(TAG, "Attempting to update widget ID: $appWidgetId")
            
            val views = RemoteViews(context.packageName, R.layout.electricity_slim_widget_layout)
            
            // 设置点击余额 TextView 打开主应用的意图
            try {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 1, // Use different request code for uniqueness
                    Intent(context, ElectricityActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.tv_widget_slim_balance, pendingIntent) // 点击余额打开 APP
                Log.d(TAG, "Set onClick for balance text view, widget ID: $appWidgetId")
            } catch (e: Exception) {
                 Log.e(TAG, "Error setting balance click PendingIntent for widget ID: $appWidgetId", e)
            }

            // 恢复刷新按钮的点击事件设置
            try {
                val refreshIntent = Intent(context, ElectricitySlimWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("intent://widget/id/$appWidgetId/refresh_slim") 
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 2, // Use different request code for uniqueness
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
                Log.d(TAG, "Set onClick for refresh button, widget ID: $appWidgetId")
            } catch (e: Exception) {
                 Log.e(TAG, "Error setting refresh click PendingIntent for widget ID: $appWidgetId", e)
            }
            
            // (初始加载状态由 showLoadingAndUpdate 处理，这里直接获取数据)
            
            // 异步加载数据并更新小部件
            Log.d(TAG, "Starting background data fetch for widget ID: $appWidgetId")
            CoroutineScope(Dispatchers.IO).launch {
                var electricityData: ElectricityData? = null
                var errorOccurred = false
                var errorMessage = ""
                var lowBalanceThreshold = 20f // 默认低余额阈值

                try {
                    // 从仓库获取电量数据
                    Log.d(TAG, "Accessing repository for widget ID: $appWidgetId")
                    val repository = ElectricityRepository(context)
                    electricityData = repository.getLastElectricityData() // 获取最新数据

                    // 从Repository获取低余额阈值，不再自己从SharedPreferences读取
                    lowBalanceThreshold = repository.getLowBalanceThreshold()

                    Log.i(TAG, "Data fetched for widget ID $appWidgetId: Balance=${electricityData?.balance}, LowBalanceThreshold=$lowBalanceThreshold")
                    
                    // 更新UI
                    if (electricityData != null) {
                        // 更新电费余额
                            val balanceText = String.format(Locale.CHINA, "%.1f", electricityData.balance)
                        views.setTextViewText(R.id.tv_widget_slim_balance, balanceText) // 只显示数字，不带单位
                        
                        // 根据余额设置背景颜色 (红或绿)
                        val backgroundColorResId = if (electricityData.balance < lowBalanceThreshold) {
                            R.color.danger_red
                        } else {
                            R.color.safe_green
                        }
                        views.setInt(R.id.widget_slim_background, "setBackgroundResource", backgroundColorResId)
                        
                        // 字体颜色始终为白色
                        views.setTextColor(R.id.tv_widget_slim_balance, Color.WHITE)
                        // 刷新按钮图标颜色也设为白色 (布局里已经是白色了，这里可以不设置)
                        // views.setInt(R.id.btn_refresh, "setColorFilter", Color.WHITE)

                        Log.d(TAG, "Widget ID $appWidgetId: Balance=$balanceText, LowBalanceThreshold=$lowBalanceThreshold, ColorResID=$backgroundColorResId")

                    } else {
                        // 无数据
                        views.setTextViewText(R.id.tv_widget_slim_balance, "N/A")
                        views.setTextColor(R.id.tv_widget_slim_balance, Color.WHITE)
                        views.setInt(R.id.widget_slim_background, "setBackgroundResource", R.color.matcha_text_hint) // 灰色背景
                        Log.w(TAG, "No electricity data found for widget ID: $appWidgetId")
                    }
                } catch (e: Exception) {
                    errorOccurred = true
                    errorMessage = e.message ?: "Unknown error"
                    // 出现异常
                    Log.e(TAG, "Error updating widget ID $appWidgetId during background fetch: $errorMessage", e)
                    views.setTextViewText(R.id.tv_widget_slim_balance, "ERR") // 显示错误
                    views.setTextColor(R.id.tv_widget_slim_balance, Color.WHITE)
                    views.setInt(R.id.widget_slim_background, "setBackgroundResource", R.color.matcha_text_hint) // 灰色背景
                } finally {
                     // 无论成功或失败，都尝试最后更新小部件
                    try {
                        Log.d(TAG, "Performing final updateAppWidget call for widget ID: $appWidgetId, Error: $errorOccurred")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                         Log.i(TAG, "Final update call finished for widget ID: $appWidgetId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during final updateAppWidget call for widget ID: $appWidgetId", e)
                    }
                }
            }
        }
    }
} 