package com.example.hubrise.ui.hubs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.LeaderboardEntry
import com.example.hubrise.data.repository.HubRepository
import kotlinx.coroutines.launch

class ChallengeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository()

    private val _challenge = MutableLiveData<Challenge?>(null)
    val challenge: LiveData<Challenge?> = _challenge

    private val _leaderboard = MutableLiveData<List<LeaderboardEntry>>(emptyList())
    val leaderboard: LiveData<List<LeaderboardEntry>> = _leaderboard

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _deleted = MutableLiveData(false)
    val deleted: LiveData<Boolean> = _deleted

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun load(challengeId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = repository.getChallenge(challengeId)) {
                is HubRepository.Result.Success -> _challenge.value = r.data
                is HubRepository.Result.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
        loadLeaderboard(challengeId)
    }

    fun loadLeaderboard(challengeId: Int) {
        viewModelScope.launch {
            when (val r = repository.getLeaderboard(challengeId)) {
                is HubRepository.Result.Success -> _leaderboard.value = r.data
                is HubRepository.Result.Error -> {}
            }
        }
    }

    fun deleteChallenge(challengeId: Int) {
        viewModelScope.launch {
            when (val r = repository.deleteChallenge(challengeId)) {
                is HubRepository.Result.Success -> _deleted.value = true
                is HubRepository.Result.Error -> _error.value = r.message
            }
        }
    }
}
