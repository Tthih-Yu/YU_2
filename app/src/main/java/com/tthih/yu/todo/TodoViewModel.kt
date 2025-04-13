package com.tthih.yu.todo

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TodoRepository(application)
    
    private val _allTodos = MutableStateFlow<List<Todo>>(emptyList())
    val allTodos: StateFlow<List<Todo>> = _allTodos.asStateFlow()
    
    private val _filteredTodos = MutableStateFlow<List<Todo>>(emptyList())
    val filteredTodos: StateFlow<List<Todo>> = _filteredTodos.asStateFlow()
    
    private val _currentTag = MutableStateFlow<String?>(null)
    val currentTag: StateFlow<String?> = _currentTag.asStateFlow()
    
    private val _availableTags = MutableStateFlow<Set<String>>(emptySet())
    val availableTags: StateFlow<Set<String>> = _availableTags.asStateFlow()
    
    private val _isAddDialogVisible = MutableStateFlow(false)
    val isAddDialogVisible: StateFlow<Boolean> = _isAddDialogVisible.asStateFlow()
    
    private val _todoBeingEdited = MutableStateFlow<Todo?>(null)
    val todoBeingEdited: StateFlow<Todo?> = _todoBeingEdited.asStateFlow()
    
    private val _isTagInputVisible = MutableStateFlow(false)
    val isTagInputVisible: StateFlow<Boolean> = _isTagInputVisible.asStateFlow()
    
    private val _newTagName = MutableStateFlow("")
    val newTagName: StateFlow<String> = _newTagName.asStateFlow()
    
    private val _customTags = MutableStateFlow<Set<String>>(emptySet())
    val customTags: StateFlow<Set<String>> = _customTags.asStateFlow()
    
    init {
        loadTodos()
        clearCustomTags()
        loadCustomTags()
    }
    
    private fun loadTodos() {
        viewModelScope.launch {
            repository.getAllTodos().collect { todos ->
                _allTodos.value = todos
                updateFilteredTodos(todos)
                updateAvailableTags(todos)
            }
        }
    }
    
    private fun loadCustomTags() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
            val tagsJson = prefs.getString("custom_tags", null)
            if (tagsJson != null) {
                try {
                    val type = object : TypeToken<Set<String>>() {}.type
                    val tags = Gson().fromJson<Set<String>>(tagsJson, type)
                    _customTags.value = tags
                } catch (e: Exception) {
                    Log.e("TodoViewModel", "Error loading custom tags: ${e.message}")
                }
            }
        }
    }
    
    private fun saveCustomTags() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
            val tagsJson = Gson().toJson(_customTags.value)
            prefs.edit().putString("custom_tags", tagsJson).apply()
        }
    }
    
    private fun clearCustomTags() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove("custom_tags").apply()
            _customTags.value = emptySet()
            updateAvailableTags(_allTodos.value)
        }
    }
    
    fun setFilter(tag: String?) {
        _currentTag.value = tag
        updateFilteredTodos(_allTodos.value)
    }
    
    private fun updateFilteredTodos(todos: List<Todo>) {
        val tag = _currentTag.value
        _filteredTodos.value = if (tag == null) {
            todos
        } else {
            todos.filter { it.tag == tag }
        }
    }
    
    private fun updateAvailableTags(todos: List<Todo>) {
        val todoTags = todos.map { it.tag }.filter { it.isNotEmpty() }.toSet()
        _availableTags.value = (todoTags + _customTags.value).filter { it.isNotEmpty() }.toSet()
    }
    
    fun showTagInput() {
        _isTagInputVisible.value = true
    }
    
    fun hideTagInput() {
        _isTagInputVisible.value = false
        _newTagName.value = ""
    }
    
    fun setNewTagName(name: String) {
        _newTagName.value = name
    }
    
    fun addCustomTag() {
        val tagName = _newTagName.value.trim()
        if (tagName.isNotEmpty() && !_availableTags.value.contains(tagName)) {
            val updatedTags = _customTags.value + tagName
            _customTags.value = updatedTags
            updateAvailableTags(_allTodos.value)
            saveCustomTags()
            _newTagName.value = ""
        }
    }
    
    fun removeTag(tag: String) {
        _customTags.value = _customTags.value.filter { it != tag }.toSet()
        
        if (_currentTag.value == tag) {
            _currentTag.value = null
        }
        
        updateAvailableTags(_allTodos.value)
        saveCustomTags()
        
        viewModelScope.launch {
            _allTodos.value.filter { it.tag == tag }.forEach { todo ->
                val updatedTodo = todo.copy(tag = "")
                repository.updateTodo(updatedTodo)
            }
        }
    }
    
    fun showAddDialog() {
        _isAddDialogVisible.value = true
        _todoBeingEdited.value = null
    }
    
    fun showEditDialog(todo: Todo) {
        _todoBeingEdited.value = todo
        _isAddDialogVisible.value = true
    }
    
    fun hideDialog() {
        _isAddDialogVisible.value = false
        _todoBeingEdited.value = null
    }
    
    fun saveTodo(
        title: String,
        description: String,
        dueDateString: String?,
        priority: Priority,
        tag: String
    ) {
        if (title.isBlank()) return
        
        val dueDate = if (!dueDateString.isNullOrEmpty()) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dueDateString)
            } catch (e: Exception) {
                null
            }
        } else null
        
        val todoToEdit = _todoBeingEdited.value
        
        if (todoToEdit != null) {
            val updatedTodo = todoToEdit.copy(
                title = title.trim(),
                description = description.trim(),
                dueDate = dueDate,
                priority = priority,
                tag = tag.trim()
            )
            
            viewModelScope.launch {
                repository.updateTodo(updatedTodo)
            }
        } else {
            val newTodo = Todo(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                createdDate = Date(),
                dueDate = dueDate,
                priority = priority,
                tag = tag.trim(),
                isCompleted = false
            )
            
            viewModelScope.launch {
                repository.insertTodo(newTodo)
            }
        }
        
        hideDialog()
    }
    
    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            repository.deleteTodo(todoId)
        }
    }
    
    fun toggleTodoCompleted(todoId: String) {
        viewModelScope.launch {
            val currentTodo = repository.getTodoById(todoId) ?: return@launch
            val updatedTodo = currentTodo.copy(isCompleted = !currentTodo.isCompleted)
            repository.updateTodo(updatedTodo)
        }
    }
} 