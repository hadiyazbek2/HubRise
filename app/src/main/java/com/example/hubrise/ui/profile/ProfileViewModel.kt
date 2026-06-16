package com.example.hubrise.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.model.UserPublicProfile
import com.example.hubrise.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = UserPreferences(app)
    private val userRepo = UserRepository()

    private val _profile = MutableLiveData<UserPublicProfile?>()
    val profile: LiveData<UserPublicProfile?> = _profile

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loggedOut = MutableLiveData(false)
    val loggedOut: LiveData<Boolean> = _loggedOut

    // Quick username from DataStore for toolbar before API loads
    val username = prefs.username.asLiveData()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = prefs.userId.first() ?: run {
                _isLoading.value = false
                return@launch
            }
            when (val r = userRepo.getProfile(userId)) {
                is UserRepository.Result.Success -> _profile.value = r.data
                is UserRepository.Result.Error -> _error.value = r.message
            }
            when (val r = userRepo.getPosts(userId)) {
                is UserRepository.Result.Success -> _posts.value = r.data
                is UserRepository.Result.Error -> {}
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.clearAll()
            _loggedOut.value = true
        }
    }
}
