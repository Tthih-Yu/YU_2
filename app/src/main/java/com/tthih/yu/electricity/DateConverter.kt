package com.tthih.yu.electricity

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room数据库Date类型转换器
 * 用于将Date类型转换为Long类型存储，以及从Long类型恢复为Date类型
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
} 