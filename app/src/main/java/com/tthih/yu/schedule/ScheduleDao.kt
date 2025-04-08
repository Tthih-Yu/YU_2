package com.tthih.yu.schedule

import androidx.room.*

/**
 * 课程表数据访问对象
 */
@Dao
interface ScheduleDao {
    
    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<ScheduleData>
    
    @Query("SELECT * FROM schedules WHERE weekDay = :weekDay")
    suspend fun getSchedulesByWeekDay(weekDay: Int): List<ScheduleData>
    
    @Query("SELECT * FROM schedules WHERE (:week BETWEEN startWeek AND endWeek) AND weekDay = :weekDay")
    suspend fun getSchedulesByWeekAndDay(week: Int, weekDay: Int): List<ScheduleData>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleData): Long
    
    @Update
    suspend fun updateSchedule(schedule: ScheduleData)
    
    @Delete
    suspend fun deleteSchedule(schedule: ScheduleData)
    
    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Int)
    
    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()
} 