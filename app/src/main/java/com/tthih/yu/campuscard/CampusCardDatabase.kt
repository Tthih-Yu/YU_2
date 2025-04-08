package com.tthih.yu.campuscard

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CampusCardTransaction::class], version = 1, exportSchema = false)
abstract class CampusCardDatabase : RoomDatabase() {
    abstract fun campusCardDao(): CampusCardDao
} 