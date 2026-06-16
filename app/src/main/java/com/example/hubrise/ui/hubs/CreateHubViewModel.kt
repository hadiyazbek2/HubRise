package com.example.hubrise.ui.hubs

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.CreateHubRequest
import com.example.hubrise.data.repository.HubRepository
import kotlinx.coroutines.launch
import java.io.File

class CreateHubViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _createdHubId = MutableLiveData<Int?>(null)
    val createdHubId: LiveData<Int?> = _createdHubId

    private val _coverUri = MutableLiveData<Uri?>(null)
    val coverUri: LiveData<Uri?> = _coverUri
    private var coverFile: File? = null

    fun setCover(uri: Uri, file: File) {
        _coverUri.value = uri
        coverFile = file
    }

    fun createHub(name: String, description: String, isPublic: Boolean) {
        if (name.isBlank()) {
            _error.value = "Hub name is required"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val request = CreateHubRequest(name.trim(), description.trim(), isPublic = isPublic)
            when (val result = repository.createHub(request)) {
                is HubRepository.Result.Success -> {
                    val hub = result.data
                    // Upload cover image if one was selected
                    val cover = coverFile
                    if (cover != null && cover.exists()) {
                        repository.uploadHubCover(hub.id, cover)
                        // Ignore upload errors — hub is still created
                    }
                    _createdHubId.value = hub.id
                }
                is HubRepository.Result.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }
}
