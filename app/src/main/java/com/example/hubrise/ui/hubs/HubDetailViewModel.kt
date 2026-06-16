package com.example.hubrise.ui.hubs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.model.ProgressModel
import com.example.hubrise.data.repository.HubRepository
import com.example.hubrise.data.repository.PostRepository
import kotlinx.coroutines.launch

class HubDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository()
    private val postRepository = PostRepository()

    private val _hub = MutableLiveData<Hub?>(null)
    val hub: LiveData<Hub?> = _hub

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _challenges = MutableLiveData<List<Challenge>>(emptyList())
    val challenges: LiveData<List<Challenge>> = _challenges

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _postsLoading = MutableLiveData(false)
    val postsLoading: LiveData<Boolean> = _postsLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _joinLeaveSuccess = MutableLiveData<Boolean?>(null)
    val joinLeaveSuccess: LiveData<Boolean?> = _joinLeaveSuccess

    private val _deleted = MutableLiveData(false)
    val deleted: LiveData<Boolean> = _deleted

    fun load(hubId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getHub(hubId)) {
                is HubRepository.Result.Success -> _hub.value = result.data
                is HubRepository.Result.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
        loadPosts(hubId)
    }

    fun loadPosts(hubId: Int) {
        viewModelScope.launch {
            _postsLoading.value = true
            when (val result = repository.getHubPosts(hubId)) {
                is HubRepository.Result.Success -> _posts.value = result.data
                is HubRepository.Result.Error -> {}
            }
            _postsLoading.value = false
        }
    }

    fun loadChallenges(hubId: Int) {
        viewModelScope.launch {
            when (val result = repository.getHubChallenges(hubId)) {
                is HubRepository.Result.Success -> _challenges.value = result.data
                is HubRepository.Result.Error -> _error.value = result.message
            }
        }
    }

    /** Only applies to count-based main challenges — stage/streak need their full detail screen. */
    fun logMainChallengeProgress() {
        val mainChallenge = _hub.value?.mainChallenge ?: return
        if (mainChallenge.progressModel != ProgressModel.COUNT) return
        viewModelScope.launch {
            when (val r = repository.logCountEntry(mainChallenge.id)) {
                is HubRepository.Result.Success -> {
                    val percent = if (r.data.targetCount > 0) {
                        ((r.data.currentCount / r.data.targetCount) * 100).toInt()
                    } else 0
                    val updated = mainChallenge.copy(
                        percentComplete = percent,
                        summary = "${formatNumber(r.data.currentCount)}/${formatNumber(r.data.targetCount)}",
                    )
                    _hub.value = _hub.value?.copy(mainChallenge = updated)
                }
                is HubRepository.Result.Error -> _error.value = r.message
            }
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    fun toggleValidate(post: Post) {
        viewModelScope.launch {
            val result = if (post.validatedByMe) {
                postRepository.unvalidatePost(post.id)
            } else {
                postRepository.validatePost(post.id)
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

    fun toggleJoin(hub: Hub) {
        viewModelScope.launch {
            val result = if (hub.isMember) repository.leaveHub(hub.id) else repository.joinHub(hub.id)
            when (result) {
                is HubRepository.Result.Success -> {
                    val updated = hub.copy(
                        isMember = !hub.isMember,
                        membersCount = result.data.membersCount
                    )
                    _hub.value = updated
                    _joinLeaveSuccess.value = updated.isMember
                }
                is HubRepository.Result.Error -> _error.value = result.message
            }
        }
    }
}
