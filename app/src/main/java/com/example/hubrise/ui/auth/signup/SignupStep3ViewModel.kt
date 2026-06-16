package com.example.hubrise.ui.auth.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.data.repository.AuthRepository
import com.example.hubrise.utils.SingleLiveEvent
import com.example.hubrise.utils.ValidationHelper
import kotlinx.coroutines.launch

class SignupStep3ViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val apiService = RetrofitClient.getAuthApiService()
    private val repository = AuthRepository(apiService, userPreferences)

    // UI State
    private val _bio = MutableLiveData("")
    val bio: LiveData<String> = _bio

    private val _profileImagePath = MutableLiveData<String?>(null)
    val profileImagePath: LiveData<String?> = _profileImagePath

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _bioError = SingleLiveEvent<String>()
    val bioError: LiveData<String> = _bioError

    private val _signupError = SingleLiveEvent<String>()
    val signupError: LiveData<String> = _signupError

    private val _signupSuccess = SingleLiveEvent<Unit>()
    val signupSuccess: LiveData<Unit> = _signupSuccess

    // Update bio
    fun setBio(newBio: String) {
        _bio.value = newBio

        if (_bioError.value != null) {
            _bioError.value = null
        }
    }

    // Set profile image path
    fun setProfileImagePath(path: String) {
        _profileImagePath.value = path
    }

    // Validate form
    private fun validateForm(): Boolean {
        var isValid = true

        val bio = _bio.value ?: ""
        if (bio.isNotEmpty()) {
            val bioError = ValidationHelper.isValidBio(bio)
            if (bioError != null) {
                _bioError.value = bioError
                isValid = false
            }
        }

        return isValid
    }

    // Complete signup
    fun completeSignup(
        email: String,
        password: String,
        fullName: String,
        username: String,
        dateOfBirth: String,
        phoneNumber: String,
        interests: List<Int>
    ) {
        if (!validateForm()) {
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            // Call signup endpoint
            val result = repository.signup(
                email = email,
                password = password,
                fullName = fullName,
                username = username,
                dateOfBirth = dateOfBirth,
                phoneNumber = phoneNumber,
                interests = interests
            )

            result.onSuccess {
                // TODO: Upload profile picture if selected

                _signupSuccess.value = Unit
            }

            result.onFailure { exception ->
                _isLoading.value = false
                _signupError.value = when {
                    exception.message?.contains("409") == true -> "Email or username already taken"
                    exception.message?.contains("Connection refused") == true -> "Cannot connect to server"
                    else -> exception.message ?: "Signup failed"
                }
            }
        }
    }

    // Upload profile picture
    fun uploadProfilePicture(userId: Int, imagePart: okhttp3.MultipartBody.Part) {
        viewModelScope.launch {
            val result = repository.uploadProfilePicture(userId, imagePart)

            result.onSuccess {
                // Picture uploaded successfully
            }

            result.onFailure { exception ->
                // Picture upload failed, but signup was successful
                // Log or handle image upload failure
            }
        }
    }
}
