package com.raymond.cms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    protected val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        viewModelScope.launch {
            _uiEvent.emit("Error: ${throwable.localizedMessage ?: "Unknown error"}")
        }
    }

    fun sendEvent(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(message)
        }
    }
}
