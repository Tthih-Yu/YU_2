package com.tthih.yu.todo

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END, priority DESC, createdDate DESC")
    fun getAllTodos(): Flow<List<Todo>>
    
    @Query("SELECT * FROM todos WHERE id = :todoId")
    suspend fun getTodoById(todoId: String): Todo?
    
    @Query("SELECT * FROM todos WHERE tag = :tag")
    fun getTodosByTag(tag: String): Flow<List<Todo>>
    
    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate < :currentDate")
    fun getOverdueTodos(currentDate: Long): Flow<List<Todo>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: Todo)
    
    @Update
    suspend fun update(todo: Todo)
    
    @Query("DELETE FROM todos WHERE id = :todoId")
    suspend fun deleteById(todoId: String)
    
    @Query("DELETE FROM todos")
    suspend fun deleteAll()
} 