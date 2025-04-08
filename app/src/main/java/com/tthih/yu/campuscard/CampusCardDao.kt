package com.tthih.yu.campuscard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CampusCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<CampusCardTransaction>)
    
    @Query("SELECT * FROM campus_card_transactions ORDER BY time DESC")
    suspend fun getAllTransactions(): List<CampusCardTransaction>
    
    @Query("DELETE FROM campus_card_transactions")
    suspend fun deleteAllTransactions()
} 