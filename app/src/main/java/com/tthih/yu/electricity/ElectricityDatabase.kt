package com.tthih.yu.electricity

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ElectricityHistoryData::class, ElectricityData::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class ElectricityDatabase : RoomDatabase() {
    
    abstract fun electricityHistoryDao(): ElectricityHistoryDao
    abstract fun electricityDao(): ElectricityDao
    
    companion object {
        @Volatile
        private var INSTANCE: ElectricityDatabase? = null
        
        fun getDatabase(context: Context): ElectricityDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    Log.d("ElectricityDatabase", "开始创建或获取数据库实例")
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        ElectricityDatabase::class.java,
                        "electricity_database"
                    )
                    .fallbackToDestructiveMigration() // 如果版本不匹配，重新创建表
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d("ElectricityDatabase", "数据库首次创建")
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            Log.d("ElectricityDatabase", "数据库已打开")
                            // 验证表是否正确创建
                            try {
                                db.query("SELECT * FROM electricity_history LIMIT 1").close()
                                Log.d("ElectricityDatabase", "electricity_history表验证成功")
                            } catch (e: Exception) {
                                Log.e("ElectricityDatabase", "electricity_history表异常: ${e.message}", e)
                            }
                            
                            try {
                                db.query("SELECT * FROM electricity_data LIMIT 1").close()
                                Log.d("ElectricityDatabase", "electricity_data表验证成功")
                            } catch (e: Exception) {
                                Log.e("ElectricityDatabase", "electricity_data表异常: ${e.message}", e)
                            }
                        }
                    })
                    .build()
                    
                    Log.d("ElectricityDatabase", "数据库实例创建成功")
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("ElectricityDatabase", "创建数据库失败: ${e.message}", e)
                    // 尝试删除数据库文件重新创建
                    context.deleteDatabase("electricity_database")
                    Log.d("ElectricityDatabase", "尝试删除旧数据库并重新创建")
                    
                    Room.databaseBuilder(
                        context.applicationContext,
                        ElectricityDatabase::class.java,
                        "electricity_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                }
            }
        }
    }
} 