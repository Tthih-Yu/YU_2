package com.tthih.yu.campuscard

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CampusCardRepository {
    private var database: CampusCardDatabase? = null
    
    private fun getDatabase(context: Context): CampusCardDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                CampusCardDatabase::class.java,
                "campus_card_database"
            ).build()
            database = instance
            instance
        }
    }
    
    suspend fun saveTransactions(transactions: List<CampusCardTransaction>) {
        database?.let { db ->
            withContext(Dispatchers.IO) {
                db.campusCardDao().insertAll(transactions)
            }
        }
    }
    
    suspend fun getTransactions(): List<CampusCardTransaction> {
        return database?.let { db ->
            withContext(Dispatchers.IO) {
                db.campusCardDao().getAllTransactions()
            }
        } ?: emptyList()
    }
    
    fun initialize(context: Context) {
        if (database == null) {
            getDatabase(context)
        }
    }
} 