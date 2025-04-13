package com.tthih.yu.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {
    
    private val repository = LibraryRepository()
    
    // 网络连接状态
    private val _isConnectedToCampusNetwork = MutableLiveData<Boolean>()
    val isConnectedToCampusNetwork: LiveData<Boolean> = _isConnectedToCampusNetwork
    
    // 记录是否正在加载
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // 检查校园网连接
    fun checkCampusNetworkConnectivity(isConnected: Boolean) {
        _isConnectedToCampusNetwork.value = isConnected
    }
    
    // 设置加载状态
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    // 设置错误信息
    fun setError(message: String?) {
        _errorMessage.value = message
    }
    
    // 重置错误消息
    fun resetError() {
        _errorMessage.value = null
    }
} 