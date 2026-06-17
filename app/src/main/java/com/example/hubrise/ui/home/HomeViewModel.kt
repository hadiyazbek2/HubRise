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

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var currentPage = 1
    private var hasMorePages = false
    private var isLoadingMoreFlag = false

    init {
        loadFeed()
    }

    fun loadFeed() {
        currentPage = 1
        hasMorePages = false
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = repository.getFeed(1)) {
                is PostRepository.Result.Success -> {
                    _posts.value = result.data.results
                    _isEmpty.value = result.data.results.isEmpty()
                    hasMorePages = result.data.next != null
                }
                is PostRepository.Result.Error -> {
                    _error.value = result.message
                    _isEmpty.value = _posts.value.isNullOrEmpty()
                }
            }

            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (isLoadingMoreFlag || !hasMorePages || _isLoading.value == true) return
        isLoadingMoreFlag = true
        _isLoadingMore.value = true
        val nextPage = currentPage + 1
        viewModelScope.launch {
            when (val result = repository.getFeed(nextPage)) {
                is PostRepository.Result.Success -> {
                    _posts.value = _posts.value.orEmpty() + result.data.results
                    hasMorePages = result.data.next != null
                    currentPage = nextPage
                }
                is PostRepository.Result.Error -> {
                    _error.value = result.message
                }
            }
            isLoadingMoreFlag = false
            _isLoadingMore.value = false
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
