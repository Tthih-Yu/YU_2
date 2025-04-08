package com.tthih.yu.electricity

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * 电费小部件后台更新工作类
 */
class ElectricityWidgetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        private const val TAG = "ElectricityWidgetWorker"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "执行电费小部件后台更新工作")
        
        return try {
            // 获取电费数据仓库
            val repository = ElectricityRepository(applicationContext)
            
            // 检查是否已经设置了JSESSIONID
            if (!repository.isJsessionIdSet()) {
                Log.w(TAG, "JSESSIONID未设置，无法更新数据")
                return Result.success()
            }
            
            // 获取最新电费数据
            val electricityData = withContext(Dispatchers.IO) {
                repository.getElectricityData()
            }
            
            if (electricityData != null) {
                Log.d(TAG, "成功获取电费数据: 余额=${electricityData.balance}元, 预计天数=${electricityData.estimatedDays}")
                
                // 保存电费数据到数据库
                withContext(Dispatchers.IO) {
                    repository.saveElectricityData(electricityData)
                }
                
                // 创建历史记录
                val lastData = withContext(Dispatchers.IO) {
                    repository.getLastElectricityData()
                }
                
                if (lastData != null && lastData.id != electricityData.id) {
                    // 前后余额差值
                    val balanceChange = electricityData.balance - lastData.balance
                    
                    // 创建历史记录数据
                    val historyData = ElectricityHistoryData(
                        date = java.util.Date(),
                        balance = electricityData.balance.toDouble(),
                        building = electricityData.building,
                        roomId = electricityData.roomId,
                        usage = if (balanceChange < 0) abs(balanceChange.toDouble()) else 0.0,
                        recharge = if (balanceChange > 0) balanceChange.toDouble() else 0.0
                    )
                    
                    // 保存历史记录
                    withContext(Dispatchers.IO) {
                        repository.saveElectricityHistoryData(historyData)
                    }
                }
                
                // 更新所有小部件
                ElectricityWidgetProvider.updateAllWidgets(applicationContext)
                
                Result.success()
            } else {
                Log.w(TAG, "获取电费数据失败")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行小部件更新工作异常: ${e.message}", e)
            Result.failure()
        }
    }
} 