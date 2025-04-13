package com.tthih.yu.todo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 待办事项数据仓库
 * 使用Room数据库进行持久化存储
 */
class TodoRepository(private val context: Context) {
    
    private val todoDao = TodoDatabase.getDatabase(context).todoDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun getAllTodos(): Flow<List<Todo>> = todoDao.getAllTodos()
    
    fun getTodosByTag(tag: String): Flow<List<Todo>> = todoDao.getTodosByTag(tag)
    
    fun getOverdueTodos(): Flow<List<Todo>> = todoDao.getOverdueTodos(System.currentTimeMillis())
    
    suspend fun getTodoById(id: String): Todo? = todoDao.getTodoById(id)
    
    suspend fun insertTodo(todo: Todo) {
        todoDao.insert(todo)
        Log.d("TodoRepository", "Added new todo: ${todo.title}")
    }
    
    suspend fun updateTodo(todo: Todo) {
        todoDao.update(todo)
        Log.d("TodoRepository", "Updated todo: ${todo.title}, completed: ${todo.isCompleted}")
    }
    
    suspend fun deleteTodo(todoId: String) {
        todoDao.deleteById(todoId)
        Log.d("TodoRepository", "Removed todo with ID: $todoId")
    }
} 