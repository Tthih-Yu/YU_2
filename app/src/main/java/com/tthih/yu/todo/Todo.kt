package com.tthih.yu.todo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val createdDate: Date,
    val dueDate: Date? = null,
    val priority: Priority = Priority.MEDIUM,
    val tag: String = "",
    val isCompleted: Boolean = false
) {
    val formattedCreatedDate: String
        get() {
            val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            return dateFormat.format(createdDate)
        }
        
    val formattedDueDate: String?
        get() {
            if (dueDate == null) return null
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(dueDate)
        }
        
    // 检查是否已经过期
    val isOverdue: Boolean
        get() = dueDate != null && dueDate.before(Date()) && !isCompleted
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH
} 