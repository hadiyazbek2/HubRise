package com.example.hubrise.ui.hubs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.repository.HubRepository
import kotlinx.coroutines.launch

class HubsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository()

    private val _myHubs = MutableLiveData<List<Hub>>(emptyList())
    val myHubs: LiveData<List<Hub>> = _myHubs

    private val _recommended = MutableLiveData<List<Hub>>(emptyList())
    val recommended: LiveData<List<Hub>> = _recommended

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init { loadHubs() }

    fun loadHubs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val allHubs = when (val result = repository.getHubs()) {
                is HubRepository.Result.Success -> result.data
                is HubRepository.Result.Error -> {
                    _error.value = result.message
                    emptyList()
                }
            }

            _myHubs.value = allHubs.filter { it.isMember }

            val nonJoined = allHubs.filter { !it.isMember }
            val recFromEndpoint = when (val result = repository.getRecommended()) {
                is HubRepository.Result.Success -> result.data.filter { !it.isMember }
                is HubRepository.Result.Error -> emptyList()
            }
            // Merge: recommended-endpoint results first, then any remaining non-joined hubs
            val recIds = recFromEndpoint.map { it.id }.toSet()
            val rec = recFromEndpoint + nonJoined.filter { it.id !in recIds }
            _recommended.value = rec

            _isEmpty.value = allHubs.isEmpty()
            _isLoading.value = false
        }
    }

    fun toggleJoin(hub: Hub) {
        viewModelScope.launch {
            val result = if (hub.isMember) repository.leaveHub(hub.id) else repository.joinHub(hub.id)
            when (result) {
                is HubRepository.Result.Success -> {
                    val newMemberCount = result.data.membersCount
                    val updated = hub.copy(isMember = !hub.isMember, membersCount = newMemberCount)
                    updateHubInLists(updated)
                }
                is HubRepository.Result.Error -> _error.value = result.message
            }
        }
    }

    private fun updateHubInLists(updated: Hub) {
        _myHubs.value = _myHubs.value.orEmpty()
            .map { if (it.id == updated.id) updated else it }
            .filter { it.isMember }

        val newRec = _recommended.value.orEmpty()
            .map { if (it.id == updated.id) updated else it }
            .filter { !it.isMember }

        val allKnownIds = _myHubs.value.orEmpty().map { it.id }.toSet()
        _recommended.value = if (!updated.isMember && updated.id !in allKnownIds)
            listOf(updated) + newRec.filter { it.id != updated.id }
        else newRec
    }

    fun refresh() = loadHubs()
}
