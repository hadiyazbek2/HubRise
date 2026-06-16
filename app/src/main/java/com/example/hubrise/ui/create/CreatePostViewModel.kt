package com.example.hubrise.ui.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.CreatePostRequest
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.repository.HubRepository
import kotlinx.coroutines.launch
import java.io.File

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository()

    private val _joinedHubs = MutableLiveData<List<Hub>>(emptyList())
    val joinedHubs: LiveData<List<Hub>> = _joinedHubs

    private val _selectedHub = MutableLiveData<Hub?>(null)
    val selectedHub: LiveData<Hub?> = _selectedHub

    // Holds the selected media (URI for preview, File for upload)
    private val _selectedMediaUri = MutableLiveData<Uri?>(null)
    val selectedMediaUri: LiveData<Uri?> = _selectedMediaUri
    private var selectedMediaFile: File? = null

    private val _isPosting = MutableLiveData(false)
    val isPosting: LiveData<Boolean> = _isPosting

    private val _postSuccess = MutableLiveData(false)
    val postSuccess: LiveData<Boolean> = _postSuccess

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init { loadJoinedHubs() }

    private fun loadJoinedHubs() {
        viewModelScope.launch {
            when (val result = repository.getHubs()) {
                is HubRepository.Result.Success -> {
                    // Show all joined hubs (Personal hub will appear labeled "Personal")
                    val joined = result.data.filter { it.isMember }
                    _joinedHubs.value = joined
                    // Auto-select if only one non-personal hub, otherwise leave unselected
                    if (joined.size == 1) _selectedHub.value = joined.first()
                }
                is HubRepository.Result.Error -> _error.value = result.message
            }
        }
    }

    fun selectHub(hub: Hub) { _selectedHub.value = hub }
    fun clearHub() { _selectedHub.value = null }

    fun setMedia(uri: Uri, file: File) {
        _selectedMediaUri.value = uri
        selectedMediaFile = file
    }

    fun clearMedia() {
        _selectedMediaUri.value = null
        selectedMediaFile = null
    }

    fun createPost(content: String, postType: String = "regular") {
        if (content.isBlank()) { _error.value = "Post content cannot be empty"; return }

        viewModelScope.launch {
            _isPosting.value = true
            _error.value = null

            val hub = _selectedHub.value
            val request = CreatePostRequest(
                hub = hub?.id,
                postType = postType,
                content = content.trim(),
                isPublic = hub?.isPublic ?: true,
            )
            when (val postResult = repository.createPost(request)) {
                is HubRepository.Result.Error -> {
                    _error.value = postResult.message
                    _isPosting.value = false
                    return@launch
                }
                is HubRepository.Result.Success -> {
                    val post = postResult.data
                    // Step 2: upload media if selected
                    val mediaFile = selectedMediaFile
                    if (mediaFile != null && mediaFile.exists()) {
                        when (val uploadResult = repository.uploadPostMedia(post.id, mediaFile)) {
                            is HubRepository.Result.Error -> {
                                // Post was created but media upload failed — still navigate away
                                _error.value = "Post created but media upload failed: ${uploadResult.message}"
                            }
                            is HubRepository.Result.Success -> { /* full success */ }
                        }
                    }
                    _postSuccess.value = true
                }
            }
            _isPosting.value = false
        }
    }
}
