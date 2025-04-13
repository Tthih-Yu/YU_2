package com.tthih.yu.electricity

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit

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
        
        // 初始化WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
        
        // 设置定时任务
        scheduleElectricityDailyWork()
        scheduleWidgetUpdateWork()
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
    
    /**
     * 配置电费每日定时查询任务（23:50和00:00:01）
     */
    private fun scheduleElectricityDailyWork() {
        // 创建约束，要求设备已连接电源且有网络连接
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // 创建15分钟定时执行的WorkRequest
        // 注意：WorkManager的最小时间间隔为15分钟，但我们在Worker中会检查具体时间
        val dailyWorkRequest = PeriodicWorkRequestBuilder<ElectricityScheduledWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        // 注册定时任务
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ELECTRICITY_DAILY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }
    
    /**
     * 配置小部件更新任务（每3小时）
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
            ExistingPeriodicWorkPolicy.UPDATE,
            widgetWorkRequest
        )
    }
} 