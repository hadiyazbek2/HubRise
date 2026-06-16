package com.example.hubrise.ui.hubs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.CompletionRequest
import com.example.hubrise.data.repository.HubRepository
import kotlinx.coroutines.launch

class CompletionRequestsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository()

    private val _requests = MutableLiveData<List<CompletionRequest>>(emptyList())
    val requests: LiveData<List<CompletionRequest>> = _requests

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun load(hubId: Int, status: String = "pending") {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = repository.getHubCompletionRequests(hubId, status)) {
                is HubRepository.Result.Success -> _requests.value = r.data
                is HubRepository.Result.Error -> _error.value = r.message
            }
            _isLoading.value = false
        }
    }

    fun approve(request: CompletionRequest) {
        viewModelScope.launch {
            when (val r = repository.approveCompletionRequest(request.id, "")) {
                is HubRepository.Result.Success -> _requests.value = _requests.value.orEmpty().filter { it.id != request.id }
                is HubRepository.Result.Error -> _error.value = r.message
            }
        }
    }

    fun reject(request: CompletionRequest, adminNote: String) {
        viewModelScope.launch {
            when (val r = repository.rejectCompletionRequest(request.id, adminNote)) {
                is HubRepository.Result.Success -> _requests.value = _requests.value.orEmpty().filter { it.id != request.id }
                is HubRepository.Result.Error -> _error.value = r.message
            }
        }
    }
}
