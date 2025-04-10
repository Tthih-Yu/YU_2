package com.tthih.yu.electricity

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class ElectricityApplication : Application(), Configuration.Provider {
    
    // 当前电费日均用电量的数据来源
    var currentDataSource: ElectricityViewModel.DataSource = ElectricityViewModel.DataSource.ESTIMATED
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
} 