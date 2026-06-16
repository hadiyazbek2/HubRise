package com.example.hubrise.ui.hubs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.LeaderboardEntry
import com.example.hubrise.data.model.MyProgress
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

    private val _completedEvent = MutableLiveData<Boolean?>(null)
    val completedEvent: LiveData<Boolean?> = _completedEvent

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

    fun completeStage(challengeId: Int, stageId: Int) {
        viewModelScope.launch {
            when (val r = repository.completeStage(challengeId, stageId)) {
                is HubRepository.Result.Success -> {
                    load(challengeId)
                    if (r.data.isComplete) _completedEvent.value = true
                }
                is HubRepository.Result.Error -> _error.value = r.message
            }
        }
    }

    fun logCountEntry(challengeId: Int, amount: Double?) {
        viewModelScope.launch {
            when (val r = repository.logCountEntry(challengeId, amount)) {
                is HubRepository.Result.Success -> {
                    val current = _challenge.value ?: return@launch
                    val updatedProgress = (current.myProgress ?: MyProgress()).copy(
                        currentCount = r.data.currentCount,
                        isComplete = r.data.isComplete,
                    )
                    _challenge.value = current.copy(myProgress = updatedProgress)
                    loadLeaderboard(challengeId)
                    if (r.data.isComplete) _completedEvent.value = true
                }
                is HubRepository.Result.Error -> _error.value = r.message
            }
        }
    }

    fun checkinStreak(challengeId: Int) {
        viewModelScope.launch {
            when (val r = repository.streakCheckin(challengeId)) {
                is HubRepository.Result.Success -> {
                    val current = _challenge.value ?: return@launch
                    val updatedProgress = MyProgress(
                        currentStreak = r.data.currentStreak,
                        longestStreak = r.data.longestStreak,
                        totalCheckins = r.data.totalCheckins,
                        checkinCalendar = r.data.checkinCalendar,
                        isComplete = r.data.isComplete,
                    )
                    _challenge.value = current.copy(myProgress = updatedProgress)
                    loadLeaderboard(challengeId)
                    if (r.data.isComplete) _completedEvent.value = true
                }
                is HubRepository.Result.Error -> _error.value = r.message
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

    fun clearCompletedEvent() {
        _completedEvent.value = null
    }
}
