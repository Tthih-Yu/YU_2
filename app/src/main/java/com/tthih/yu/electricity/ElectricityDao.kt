package com.tthih.yu.electricity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ElectricityDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: ElectricityData)
    
    @Query("SELECT * FROM electricity_data ORDER BY id DESC LIMIT 1")
    suspend fun getLastRecord(): ElectricityData?
    
    @Query("SELECT * FROM electricity_data ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentData(limit: Int): List<ElectricityData>
    
    @Query("DELETE FROM electricity_data")
    suspend fun deleteAll()
} 