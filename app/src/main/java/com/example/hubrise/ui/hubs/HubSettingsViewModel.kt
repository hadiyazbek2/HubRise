package com.example.hubrise.ui.hubs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.model.HubCategory
import com.example.hubrise.data.model.HubMember
import com.example.hubrise.data.repository.HubRepository
import kotlinx.coroutines.launch
import java.io.File

class HubSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository()

    private val _hub = MutableLiveData<Hub?>()
    val hub: LiveData<Hub?> = _hub

    private val _members = MutableLiveData<List<HubMember>>(emptyList())
    val members: LiveData<List<HubMember>> = _members

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saved = MutableLiveData(false)
    val saved: LiveData<Boolean> = _saved

    private val _deleted = MutableLiveData(false)
    val deleted: LiveData<Boolean> = _deleted

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _pendingCompletionRequestsCount = MutableLiveData(0)
    val pendingCompletionRequestsCount: LiveData<Int> = _pendingCompletionRequestsCount

    private val _categories = MutableLiveData<List<HubCategory>>(emptyList())
    val categories: LiveData<List<HubCategory>> = _categories

    private val _selectedCategory = MutableLiveData<HubCategory?>(null)
    val selectedCategory: LiveData<HubCategory?> = _selectedCategory

    init {
        viewModelScope.launch {
            when (val r = repository.getCategories()) {
                is HubRepository.Result.Success -> _categories.value = r.data
                is HubRepository.Result.Error -> {}
            }
        }
    }

    fun selectCategory(category: HubCategory) {
        _selectedCategory.value = category
    }

    fun setInitialCategory(categoryName: String?) {
        viewModelScope.launch {
            val cats = _categories.value
            if (!cats.isNullOrEmpty() && categoryName != null) {
                _selectedCategory.value = cats.firstOrNull { it.name == categoryName }
            }
        }
    }

    fun loadPendingCompletionRequestsCount(hubId: Int) {
        viewModelScope.launch {
            when (val r = repository.getHubCompletionRequests(hubId)) {
                is HubRepository.Result.Success -> _pendingCompletionRequestsCount.value = r.data.size
                is HubRepository.Result.Error -> {}
            }
        }
    }

    fun loadMembers(hubId: Int) {
        viewModelScope.launch {
            when (val r = repository.getHubMembers(hubId)) {
                is HubRepository.Result.Success -> _members.value = r.data
                is HubRepository.Result.Error -> _error.value = r.message
            }
        }
    }

    fun saveSettings(hubId: Int, name: String, description: String, isPublic: Boolean, coverFile: File?) {
        viewModelScope.launch {
            _isSaving.value = true
            val categoryId = _selectedCategory.value?.id
            when (val r = repository.updateHub(hubId, name.trim(), description.trim(), isPublic, coverFile, categoryId)) {
                is HubRepository.Result.Success -> {
                    _hub.value = r.data
                    _saved.value = true
                }
                is HubRepository.Result.Error -> _error.value = r.message
            }
            _isSaving.value = false
        }
    }

    fun deleteHub(hubId: Int) {
        viewModelScope.launch {
            _isSaving.value = true
            when (val r = repository.deleteHub(hubId)) {
                is HubRepository.Result.Success -> _deleted.value = true
                is HubRepository.Result.Error -> _error.value = r.message
            }
            _isSaving.value = false
        }
    }

    fun removeMember(hubId: Int, userId: Int) {
        viewModelScope.launch {
            when (val r = repository.removeMember(hubId, userId)) {
                is HubRepository.Result.Success -> {
                    _members.value = _members.value?.filter { it.userId != userId }
                }
                is HubRepository.Result.Error -> _error.value = r.message
            }
        }
    }
}
