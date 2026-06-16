package com.example.hubrise.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.data.repository.UserRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class EditProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saved = MutableLiveData(false)
    val saved: LiveData<Boolean> = _saved

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val prefs = UserPreferences(app)
    private val repository = UserRepository()

    fun saveProfile(userId: Int, fullName: String, bio: String, imagePart: MultipartBody.Part? = null) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            if (imagePart != null) {
                when (val r = repository.uploadProfilePicture(userId, imagePart)) {
                    is UserRepository.Result.Success -> {
                        r.data.profilePictureUrl?.let { prefs.saveProfilePictureUrl(it) }
                    }
                    is UserRepository.Result.Error -> {
                        _error.value = "Photo upload failed: ${r.message}"
                        _isSaving.value = false
                        return@launch
                    }
                }
            }

            when (val r = repository.updateProfile(userId, fullName, bio)) {
                is UserRepository.Result.Success -> {
                    prefs.saveFullName(r.data.fullName)
                    _saved.value = true
                }
                is UserRepository.Result.Error -> {
                    _error.value = r.message
                }
            }
            _isSaving.value = false
        }
    }
}
