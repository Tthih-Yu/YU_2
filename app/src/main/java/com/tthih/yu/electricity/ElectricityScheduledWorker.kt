package com.tthih.yu.electricity

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs

/**
 * 电费定时刷新工作类
 * 在指定时间点（晚上23:50和早上0:0:1）自动查询和保存电费数据
 */
class ElectricityScheduledWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        private const val TAG = "ElectricityScheduledWorker"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "执行电费定时刷新工作")
        
        // 检查当前时间是否是我们需要执行的时间点
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        
        // 只在晚上23:50或早上0:0:1左右执行
        val isScheduledTime = (hour == 23 && minute >= 50) || 
                             (hour == 0 && minute == 0 && (second == 0 || second == 1))
        
        if (!isScheduledTime) {
            Log.d(TAG, "当前时间 $hour:$minute:$second 不是指定的刷新时间点，跳过执行")
            return Result.success()
        }
        
        Log.d(TAG, "当前时间 $hour:$minute:$second 符合定时刷新条件，开始刷新电费数据")
        
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
                
                // 检测电量变化并记录到历史
                val changed = withContext(Dispatchers.IO) {
                    repository.checkAndRecordElectricityChange(electricityData)
                }
                
                if (changed) {
                    Log.d(TAG, "电费数据有变化，已更新历史记录")
                } else {
                    Log.d(TAG, "电费数据无变化")
                }
                
                // 更新所有小部件
                ElectricityWidgetProvider.updateAllWidgets(applicationContext)
                
                Log.d(TAG, "定时刷新电费数据完成")
                Result.success()
            } else {
                Log.w(TAG, "获取电费数据失败")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "定时刷新电费数据异常: ${e.message}", e)
            Result.failure()
        }
    }
} 