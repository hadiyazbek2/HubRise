package com.example.hubrise.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.repository.NotificationRepository
import com.example.hubrise.data.repository.PostRepository
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository()
    private val notificationRepository = NotificationRepository()

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = repository.getFeed()) {
                is PostRepository.Result.Success -> {
                    _posts.value = result.data
                    _isEmpty.value = result.data.isEmpty()
                }
                is PostRepository.Result.Error -> {
                    _error.value = result.message
                    _isEmpty.value = _posts.value.isNullOrEmpty()
                }
            }

            _isLoading.value = false
        }
    }

    fun refresh() = loadFeed()

    fun fetchUnreadCount() {
        viewModelScope.launch {
            when (val result = notificationRepository.getUnreadCount()) {
                is NotificationRepository.Result.Success -> _unreadCount.value = result.data
                is NotificationRepository.Result.Error -> {}
            }
        }
    }

    fun toggleLike(post: Post) {
        // Optimistic update
        _posts.value = _posts.value.orEmpty().map {
            if (it.id == post.id) it.copy(
                isLiked = !it.isLiked,
                likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1
            ) else it
        }
        viewModelScope.launch {
            repository.toggleLike(post.id)
            // No rollback on failure for simplicity — next refresh corrects it
        }
    }

    fun toggleValidate(post: Post) {
        viewModelScope.launch {
            val result = if (post.validatedByMe) {
                repository.unvalidatePost(post.id)
            } else {
                repository.validatePost(post.id)
            }
            when (result) {
                is PostRepository.Result.Success -> {
                    _posts.value = _posts.value.orEmpty().map {
                        if (it.id == post.id) it.copy(
                            validatedByMe = !post.validatedByMe,
                            validationsCount = if (post.validatedByMe) (it.validationsCount - 1).coerceAtLeast(0) else it.validationsCount + 1,
                            isTrusted = result.data.isTrusted,
                        ) else it
                    }
                }
                is PostRepository.Result.Error -> _error.value = result.message
            }
        }
    }
}
