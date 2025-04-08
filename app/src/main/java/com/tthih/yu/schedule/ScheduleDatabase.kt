package com.tthih.yu.schedule

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tthih.yu.electricity.DateConverter

/**
 * 课程表数据库
 */
@Database(entities = [ScheduleData::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class ScheduleDatabase : RoomDatabase() {
    
    abstract fun scheduleDao(): ScheduleDao
    
    companion object {
        @Volatile
        private var INSTANCE: ScheduleDatabase? = null
        
        fun getDatabase(context: Context): ScheduleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScheduleDatabase::class.java,
                    "schedule_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 