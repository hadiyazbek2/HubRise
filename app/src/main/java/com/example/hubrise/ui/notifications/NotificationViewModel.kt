package com.example.hubrise.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.NotificationItem
import com.example.hubrise.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    private val _notifications = MutableLiveData<List<NotificationItem>>(emptyList())
    val notifications: LiveData<List<NotificationItem>> = _notifications

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val repository = NotificationRepository()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = repository.getNotifications()) {
                is NotificationRepository.Result.Success -> _notifications.value = result.data
                is NotificationRepository.Result.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            when (val result = repository.getUnreadCount()) {
                is NotificationRepository.Result.Success -> _unreadCount.value = result.data
                is NotificationRepository.Result.Error -> {}
            }
        }
    }
}
