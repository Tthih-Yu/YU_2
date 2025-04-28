package com.tthih.yu.electricity

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.util.Log
import com.tthih.yu.campuscard.NetworkModule

class ElectricityApplication : Application(), Configuration.Provider {
    
    // 当前电费日均用电量的数据来源
    var currentDataSource: ElectricityViewModel.DataSource = ElectricityViewModel.DataSource.ESTIMATED
    
    companion object {
        // 定时任务名称
        private const val ELECTRICITY_DAILY_WORK = "electricity_daily_work"
        private const val ELECTRICITY_WIDGET_WORK = "electricity_widget_update_work"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize NetworkModule
        NetworkModule.initialize(this)
        Log.i("ElectricityApplication", "NetworkModule initialized for Campus Card.")
        
        // Initialize WorkManager (keep on main thread as recommended for basic setup)
        try {
        WorkManager.initialize(this, workManagerConfiguration)
        } catch (e: IllegalStateException) {
             // WorkManager might already be initialized, which is fine.
             Log.w("ElectricityApplication", "WorkManager already initialized: ${e.message}")
        }
        
        // Schedule work asynchronously
        GlobalScope.launch(Dispatchers.IO) {
            try {
        scheduleElectricityDailyWork()
        scheduleWidgetUpdateWork()
            } catch (e: Exception) {
                Log.e("ElectricityApplication", "Error scheduling background work", e)
            }
        }
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
    
    /**
     * 配置电费每日定时查询任务（23:50和00:00:01）
     * NOW CALLED FROM BACKGROUND THREAD
     */
    private fun scheduleElectricityDailyWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val dailyWorkRequest = PeriodicWorkRequestBuilder<ElectricityScheduledWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        // getInstance() is safe to call from any thread after initialize
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ELECTRICITY_DAILY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE, // Use UPDATE instead of KEEP to allow changes
            dailyWorkRequest
        )
        Log.i("ElectricityApplication", "Scheduled daily electricity work.")
    }
    
    /**
     * 配置小部件更新任务（每3小时）
     * NOW CALLED FROM BACKGROUND THREAD
     */
    private fun scheduleWidgetUpdateWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val widgetWorkRequest = PeriodicWorkRequestBuilder<ElectricityWidgetWorker>(
            3, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ELECTRICITY_WIDGET_WORK,
            ExistingPeriodicWorkPolicy.UPDATE, // Use UPDATE instead of KEEP
            widgetWorkRequest
        )
        Log.i("ElectricityApplication", "Scheduled widget update work.")
    }
} 