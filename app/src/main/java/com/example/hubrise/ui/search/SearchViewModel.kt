package com.example.hubrise.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.ChallengeSearchResult
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.model.PostSearchResult
import com.example.hubrise.data.model.UserSearchResult
import com.example.hubrise.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _users = MutableLiveData<List<UserSearchResult>>(emptyList())
    val users: LiveData<List<UserSearchResult>> = _users

    private val _hubs = MutableLiveData<List<Hub>>(emptyList())
    val hubs: LiveData<List<Hub>> = _hubs

    private val _posts = MutableLiveData<List<PostSearchResult>>(emptyList())
    val posts: LiveData<List<PostSearchResult>> = _posts

    private val _challenges = MutableLiveData<List<ChallengeSearchResult>>(emptyList())
    val challenges: LiveData<List<ChallengeSearchResult>> = _challenges

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val repository = SearchRepository()
    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _users.value = emptyList()
            _hubs.value = emptyList()
            _posts.value = emptyList()
            _challenges.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            _error.value = null
            when (val result = repository.search(query)) {
                is SearchRepository.Result.Success -> {
                    _users.value = result.data.users
                    _hubs.value = result.data.hubs
                    _posts.value = result.data.posts
                    _challenges.value = result.data.challenges
                }
                is SearchRepository.Result.Error -> {
                    _error.value = result.message
                }
            }
            _isLoading.value = false
        }
    }
}
