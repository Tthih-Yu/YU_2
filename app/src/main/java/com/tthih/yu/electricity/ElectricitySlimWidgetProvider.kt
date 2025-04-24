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

/**
 * 电费迷你桌面小部件提供者类
 * 提供2x1大小的小部件，左侧显示大数字电量，右侧是刷新按钮
 */
class ElectricitySlimWidgetProvider : AppWidgetProvider() {
    
    private val TAG = "ElectricitySlimWidget"
    
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
            
            // 触发立即刷新
            triggerImmediateRefresh(context)
        }
    }
    
    companion object {
        const val ACTION_REFRESH = "com.tthih.yu.electricity.ACTION_REFRESH_SLIM_WIDGET"
        
        // 更新所有小部件
        fun updateAllWidgets(context: Context) {
            Log.d("ElectricitySlimWidget", "updateAllWidgets 被调用")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ElectricitySlimWidgetProvider::class.java)
            )
            
            // 更新所有小部件
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
        
        // 手动触发立即刷新
        fun triggerImmediateRefresh(context: Context) {
            Log.d("ElectricitySlimWidget", "手动触发立即刷新")
            
            // 创建一次性工作请求来刷新电费数据
            val refreshWork = OneTimeWorkRequestBuilder<ElectricityScheduledWorker>()
                .build()
                
            // 立即执行
            WorkManager.getInstance(context).enqueue(refreshWork)
        }
        
        // 更新小部件显示
        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            Log.i("ElectricitySlimWidget", "Attempting to update widget ID: $appWidgetId")
            
            // 创建小部件视图
            val views: RemoteViews?
            try {
                views = RemoteViews(context.packageName, R.layout.electricity_slim_widget_layout)
                Log.d("ElectricitySlimWidget", "RemoteViews created for widget ID: $appWidgetId")
            } catch (e: Exception) {
                Log.e("ElectricitySlimWidget", "Error creating RemoteViews for widget ID: $appWidgetId", e)
                return // Cannot proceed without RemoteViews
            }
            
            // 设置点击打开主应用的意图
            try {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0, 
                    Intent(context, ElectricityActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.tv_electricity_balance, pendingIntent)
                Log.d("ElectricitySlimWidget", "Set onClick for balance TextView, widget ID: $appWidgetId")
            } catch (e: Exception) {
                 Log.e("ElectricitySlimWidget", "Error setting balance click PendingIntent for widget ID: $appWidgetId", e)
            }

            // 添加刷新按钮的点击事件
            try {
                val refreshIntent = Intent(context, ElectricitySlimWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    // Ensure intent is unique using appWidgetId
                    data = Uri.parse("intent://widget/id/$appWidgetId/refresh_slim") 
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 1000, // 使用不同的请求码避免与其他小部件冲突
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
                Log.d("ElectricitySlimWidget", "Set onClick for refresh button, widget ID: $appWidgetId")
            } catch (e: Exception) {
                 Log.e("ElectricitySlimWidget", "Error setting refresh click PendingIntent for widget ID: $appWidgetId", e)
            }
            
            // 先显示加载中
            Log.d("ElectricitySlimWidget", "Setting initial loading state for widget ID: $appWidgetId")
            views.setTextViewText(R.id.tv_electricity_balance, "...") // Use ellipsis for loading
            try {
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("ElectricitySlimWidget", "Initial updateAppWidget call succeeded for widget ID: $appWidgetId")
            } catch (e: Exception) {
                 Log.e("ElectricitySlimWidget", "Error during initial updateAppWidget call for widget ID: $appWidgetId", e)
                 // If this fails, the widget might not appear at all or show the error
                 return 
            }
            
            // 异步加载数据并更新小部件
            Log.d("ElectricitySlimWidget", "Starting background data fetch for widget ID: $appWidgetId")
            CoroutineScope(Dispatchers.IO).launch {
                var electricityData: ElectricityData? = null
                var lowBalanceThreshold = 20f // Default threshold
                var errorOccurred = false
                var errorMessage = ""

                try {
                    // 从仓库获取电量数据
                    Log.d("ElectricitySlimWidget", "Accessing repository for widget ID: $appWidgetId")
                    val repository = ElectricityRepository(context)
                    electricityData = repository.getLastElectricityData()
                    lowBalanceThreshold = repository.getLowBalanceThreshold()
                    Log.i("ElectricitySlimWidget", "Data fetched for widget ID $appWidgetId: Balance=${electricityData?.balance}, Threshold=$lowBalanceThreshold")
                    
                    // 更新UI (RemoteViews可以在任何线程中更新)
                    if (electricityData != null) {
                        // 更新电费余额 - 确保有效值
                        if (electricityData.balance > 0) {
                            val balanceText = String.format(Locale.CHINA, "%.1f", electricityData.balance)
                            views.setTextViewText(R.id.tv_electricity_balance, "$balanceText ¥") // 添加人民币符号
                            Log.d("ElectricitySlimWidget", "Set balance text to $balanceText ¥ for widget ID: $appWidgetId")
                            
                            // 根据余额与阈值的比较设置颜色
                            if (electricityData.balance < lowBalanceThreshold) {
                                // 低于阈值，设置为红色警告(背景已经是绿色，文字改为白色)
                                views.setTextColor(R.id.tv_electricity_balance, Color.parseColor("#FFFFFF")) 
                                Log.d("ElectricitySlimWidget", "Set balance text color to WHITE (Low Balance Warning) for widget ID: $appWidgetId")
                            } else {
                                // 正常余额，白色字体
                                views.setTextColor(R.id.tv_electricity_balance, Color.parseColor("#FFFFFF")) 
                                Log.d("ElectricitySlimWidget", "Set balance text color to WHITE for widget ID: $appWidgetId")
                            }
                        } else {
                            views.setTextViewText(R.id.tv_electricity_balance, "0.0 ¥") // 添加人民币符号
                            views.setTextColor(R.id.tv_electricity_balance, Color.parseColor("#FFFFFF")) // 白色字体
                             Log.d("ElectricitySlimWidget", "Set balance text to 0.0 ¥ (White) for widget ID: $appWidgetId")
                        }
                    } else {
                        // 无数据
                        views.setTextViewText(R.id.tv_electricity_balance, "N/A") // Indicate No Data
                        views.setTextColor(R.id.tv_electricity_balance, Color.parseColor("#FFFFFF"))
                        Log.w("ElectricitySlimWidget", "No electricity data found for widget ID: $appWidgetId")
                    }
                } catch (e: Exception) {
                    errorOccurred = true
                    errorMessage = e.message ?: "Unknown error"
                    // 出现异常，记录日志
                    Log.e("ElectricitySlimWidget", "Error updating widget ID $appWidgetId during background fetch: $errorMessage", e)
                    views.setTextViewText(R.id.tv_electricity_balance, "ERR") // Indicate Error
                    views.setTextColor(R.id.tv_electricity_balance, Color.parseColor("#FFFFFF"))
                } finally {
                     // 无论成功或失败，都尝试最后更新小部件
                    try {
                        Log.d("ElectricitySlimWidget", "Performing final updateAppWidget call for widget ID: $appWidgetId, Error: $errorOccurred")
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                         Log.i("ElectricitySlimWidget", "Final update call finished for widget ID: $appWidgetId")
                    } catch (e: Exception) {
                        Log.e("ElectricitySlimWidget", "Error during final updateAppWidget call for widget ID: $appWidgetId", e)
                    }
                }
            }
        }
    }
} 