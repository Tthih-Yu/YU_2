package com.tthih.yu.electricity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Date

@Dao
interface ElectricityHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(historyData: ElectricityHistoryData)
    
    @Query("SELECT * FROM electricity_history WHERE building = :building AND roomId = :roomId ORDER BY date DESC")
    suspend fun getAllHistory(building: String, roomId: String): List<ElectricityHistoryData>
    
    @Query("SELECT * FROM electricity_history WHERE building = :building AND roomId = :roomId AND date >= :startTime AND date <= :endTime ORDER BY date ASC")
    suspend fun getHistoryByDateRange(building: String, roomId: String, startTime: Date, endTime: Date): List<ElectricityHistoryData>
    
    @Query("SELECT * FROM electricity_history WHERE building = :building AND roomId = :roomId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestHistory(building: String, roomId: String): ElectricityHistoryData?
    
    @Query("DELETE FROM electricity_history WHERE building = :building AND roomId = :roomId")
    suspend fun clearHistory(building: String, roomId: String)
    
    @Query("DELETE FROM electricity_history")
    suspend fun clearAllHistory()
} 