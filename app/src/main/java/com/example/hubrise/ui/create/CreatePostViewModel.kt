package com.example.hubrise.ui.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.ChallengeStageStatus
import com.example.hubrise.data.model.CreatePostRequest
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.model.ProgressModel
import com.example.hubrise.data.model.StageStatus
import com.example.hubrise.data.repository.HubRepository
import kotlinx.coroutines.launch
import java.io.File

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HubRepository()

    private val _joinedHubs = MutableLiveData<List<Hub>>(emptyList())
    val joinedHubs: LiveData<List<Hub>> = _joinedHubs

    private val _selectedHub = MutableLiveData<Hub?>(null)
    val selectedHub: LiveData<Hub?> = _selectedHub

    private val _availableChallenges = MutableLiveData<List<Challenge>>(emptyList())
    val availableChallenges: LiveData<List<Challenge>> = _availableChallenges

    private val _selectedChallenge = MutableLiveData<Challenge?>(null)
    val selectedChallenge: LiveData<Challenge?> = _selectedChallenge

    // For stage-based challenges: the next stage the user can actually post against.
    // Null while loading, or if every stage is already completed.
    private val _currentStage = MutableLiveData<ChallengeStageStatus?>(null)
    val currentStage: LiveData<ChallengeStageStatus?> = _currentStage

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
                    if (joined.size == 1) selectHub(joined.first())
                }
                is HubRepository.Result.Error -> _error.value = result.message
            }
        }
    }

    fun selectHub(hub: Hub) {
        _selectedHub.value = hub
        clearChallenge()
        loadChallengesForHub(hub.id)
    }

    fun clearHub() {
        _selectedHub.value = null
        clearChallenge()
        _availableChallenges.value = emptyList()
    }

    private fun loadChallengesForHub(hubId: Int) {
        viewModelScope.launch {
            when (val r = repository.getHubChallenges(hubId)) {
                is HubRepository.Result.Success -> _availableChallenges.value = r.data
                is HubRepository.Result.Error -> _availableChallenges.value = emptyList()
            }
        }
    }

    /** Pre-selects a hub + challenge by id, e.g. when launched from a Challenge Detail screen. */
    fun preselect(hubId: Int?, challengeId: Int?) {
        if (hubId == null) return
        viewModelScope.launch {
            when (val r = repository.getHub(hubId)) {
                is HubRepository.Result.Success -> {
                    selectHub(r.data)
                    if (challengeId != null) {
                        when (val cr = repository.getChallenge(challengeId)) {
                            is HubRepository.Result.Success -> selectChallenge(cr.data)
                            is HubRepository.Result.Error -> {}
                        }
                    }
                }
                is HubRepository.Result.Error -> {}
            }
        }
    }

    fun selectChallenge(challenge: Challenge) {
        _selectedChallenge.value = challenge
        _currentStage.value = null
        if (challenge.progressModel == ProgressModel.STAGE) {
            loadCurrentStage(challenge.id)
        }
    }

    fun clearChallenge() {
        _selectedChallenge.value = null
        _currentStage.value = null
    }

    private fun loadCurrentStage(challengeId: Int) {
        viewModelScope.launch {
            when (val r = repository.getChallenge(challengeId)) {
                is HubRepository.Result.Success -> {
                    _currentStage.value = r.data.stages?.firstOrNull { it.status != StageStatus.COMPLETED }
                }
                is HubRepository.Result.Error -> {}
            }
        }
    }

    fun setMedia(uri: Uri, file: File) {
        _selectedMediaUri.value = uri
        selectedMediaFile = file
    }

    fun clearMedia() {
        _selectedMediaUri.value = null
        selectedMediaFile = null
    }

    fun createPost(content: String, amount: Double? = null) {
        val challenge = _selectedChallenge.value

        if (challenge == null && content.isBlank()) {
            _error.value = "Post content cannot be empty"
            return
        }
        if (challenge?.progressModel == ProgressModel.STAGE && _currentStage.value == null) {
            _error.value = "All stages in this challenge are already completed"
            return
        }

        viewModelScope.launch {
            _isPosting.value = true
            _error.value = null

            val hub = _selectedHub.value
            val request = CreatePostRequest(
                hub = hub?.id,
                content = content.trim(),
                isPublic = hub?.isPublic ?: true,
                challenge = challenge?.id,
                stage = if (challenge?.progressModel == ProgressModel.STAGE) _currentStage.value?.id else null,
                amount = if (challenge?.progressModel == ProgressModel.COUNT) amount else null,
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
