package com.example.hubrise.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.model.UserPublicProfile
import com.example.hubrise.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UserProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = UserRepository()
    private val prefs = UserPreferences(app)

    private val _profile = MutableLiveData<UserPublicProfile?>()
    val profile: LiveData<UserPublicProfile?> = _profile

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isFollowing = MutableLiveData(false)
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _isOwnProfile = MutableLiveData(false)
    val isOwnProfile: LiveData<Boolean> = _isOwnProfile

    fun load(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val ownId = prefs.userId.first()
            _isOwnProfile.value = (ownId == userId)

            when (val r = repository.getProfile(userId)) {
                is UserRepository.Result.Success -> {
                    _profile.value = r.data
                    _isFollowing.value = r.data.isFollowing
                }
                is UserRepository.Result.Error -> _error.value = r.message
            }
            when (val r = repository.getPosts(userId)) {
                is UserRepository.Result.Success -> _posts.value = r.data
                is UserRepository.Result.Error -> {}
            }
            _isLoading.value = false
        }
    }

    fun toggleFollow(userId: Int) {
        viewModelScope.launch {
            when (val r = repository.followToggle(userId)) {
                is UserRepository.Result.Success -> {
                    _isFollowing.value = r.data.isFollowing
                    _profile.value = _profile.value?.copy(followersCount = r.data.followersCount)
                }
                is UserRepository.Result.Error -> _error.value = r.message
            }
        }
    }
}
