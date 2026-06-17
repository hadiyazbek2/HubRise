package com.example.hubrise.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.repository.ExploreRepository
import com.example.hubrise.data.repository.PostRepository
import kotlinx.coroutines.launch

class ExploreViewModel(app: Application) : AndroidViewModel(app) {

    private val exploreRepo = ExploreRepository()
    private val postRepo = PostRepository()

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private var currentPage = 1
    private var hasMore = true

    init { loadFeed() }

    fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            currentPage = 1
            hasMore = true
            when (val r = exploreRepo.getFeed(page = 1)) {
                is ExploreRepository.Result.Success -> {
                    _posts.value = r.data
                    _isEmpty.value = r.data.isEmpty()
                    hasMore = r.data.size >= 20
                }
                is ExploreRepository.Result.Error -> _isEmpty.value = true
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (!hasMore || _isLoading.value == true) return
        viewModelScope.launch {
            _isLoading.value = true
            val next = currentPage + 1
            when (val r = exploreRepo.getFeed(page = next)) {
                is ExploreRepository.Result.Success -> {
                    currentPage = next
                    hasMore = r.data.size >= 20
                    _posts.value = _posts.value.orEmpty() + r.data
                }
                is ExploreRepository.Result.Error -> {}
            }
            _isLoading.value = false
        }
    }

    fun toggleLike(post: Post) {
        _posts.value = _posts.value.orEmpty().map {
            if (it.id == post.id) it.copy(
                isLiked = !it.isLiked,
                likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1,
            ) else it
        }
        viewModelScope.launch { postRepo.toggleLike(post.id) }
    }
}
