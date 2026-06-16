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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SignupStep2ViewModel : ViewModel() {

    private val apiService = RetrofitClient.getAuthApiService()

    // UI State
    private val _fullName = MutableLiveData("")
    val fullName: LiveData<String> = _fullName

    private val _username = MutableLiveData("")
    val username: LiveData<String> = _username

    private val _dateOfBirth = MutableLiveData("")
    val dateOfBirth: LiveData<String> = _dateOfBirth

    private val _phoneNumber = MutableLiveData("")
    val phoneNumber: LiveData<String> = _phoneNumber

    private val _selectedInterests = MutableLiveData(emptyList<Int>())
    val selectedInterests: LiveData<List<Int>> = _selectedInterests

    // Validation states
    private val _fullNameError = SingleLiveEvent<String>()
    val fullNameError: LiveData<String> = _fullNameError

    private val _usernameError = SingleLiveEvent<String>()
    val usernameError: LiveData<String> = _usernameError

    private val _dateOfBirthError = SingleLiveEvent<String>()
    val dateOfBirthError: LiveData<String> = _dateOfBirthError

    private val _phoneNumberError = SingleLiveEvent<String>()
    val phoneNumberError: LiveData<String> = _phoneNumberError

    private val _isCheckingUsername = MutableLiveData(false)
    val isCheckingUsername: LiveData<Boolean> = _isCheckingUsername

    private val _usernameAvailable = MutableLiveData(true)
    val usernameAvailable: LiveData<Boolean> = _usernameAvailable

    private val _proceedToStep3 = SingleLiveEvent<Unit>()
    val proceedToStep3: LiveData<Unit> = _proceedToStep3

    private var usernameCheckJob: Job? = null

    // Update full name
    fun setFullName(newName: String) {
        _fullName.value = newName

        if (_fullNameError.value != null) {
            _fullNameError.value = null
        }
    }

    // Update username with debounce check
    fun setUsername(newUsername: String) {
        _username.value = newUsername

        if (_usernameError.value != null) {
            _usernameError.value = null
        }

        usernameCheckJob?.cancel()

        if (newUsername.isEmpty()) {
            _usernameAvailable.value = true
            return
        }

        // Validate format first
        if (ValidationHelper.isValidUsername(newUsername) != null) {
            _usernameAvailable.value = true
            return
        }

        // Check availability with debounce
        usernameCheckJob = viewModelScope.launch {
            delay(500)
            _isCheckingUsername.value = true
            // TODO: Call repository to check username availability
            delay(500) // Simulate API call
            _isCheckingUsername.value = false
        }
    }

    // Update date of birth
    fun setDateOfBirth(date: String) {
        _dateOfBirth.value = date

        if (_dateOfBirthError.value != null) {
            _dateOfBirthError.value = null
        }
    }

    // Update phone number
    fun setPhoneNumber(phone: String) {
        _phoneNumber.value = phone

        if (_phoneNumberError.value != null) {
            _phoneNumberError.value = null
        }
    }

    // Toggle interest selection
    fun toggleInterest(interestId: Int) {
        val current = _selectedInterests.value?.toMutableList() ?: mutableListOf()
        if (current.contains(interestId)) {
            current.remove(interestId)
        } else {
            current.add(interestId)
        }
        _selectedInterests.value = current
    }

    // Validate form
    private fun validateForm(): Boolean {
        var isValid = true

        val fullName = _fullName.value ?: ""
        val fullNameError = ValidationHelper.isValidFullName(fullName)
        if (fullNameError != null) {
            _fullNameError.value = fullNameError
            isValid = false
        }

        val username = _username.value ?: ""
        val usernameError = ValidationHelper.isValidUsername(username)
        if (usernameError != null) {
            _usernameError.value = usernameError
            isValid = false
        } else if (!_usernameAvailable.value!!) {
            _usernameError.value = "Username is already taken"
            isValid = false
        }

        val dateOfBirth = _dateOfBirth.value ?: ""
        val dobError = ValidationHelper.isValidDateOfBirth(dateOfBirth)
        if (dobError != null) {
            _dateOfBirthError.value = dobError
            isValid = false
        }

        val phoneNumber = _phoneNumber.value ?: ""
        if (phoneNumber.isNotEmpty()) {
            val phoneError = ValidationHelper.isValidPhoneNumber(phoneNumber)
            if (phoneError != null) {
                _phoneNumberError.value = phoneError
                isValid = false
            }
        }

        return isValid
    }

    // Proceed to step 3
    fun proceedToStep3() {
        if (validateForm()) {
            _proceedToStep3.value = Unit
        }
    }

    // Get form data for Step 2
    data class Step2Data(
        val fullName: String,
        val username: String,
        val dateOfBirth: String,
        val phoneNumber: String,
        val interests: List<Int>
    )

    fun getFormData(): Step2Data {
        return Step2Data(
            fullName = _fullName.value ?: "",
            username = _username.value ?: "",
            dateOfBirth = _dateOfBirth.value ?: "",
            phoneNumber = _phoneNumber.value ?: "",
            interests = _selectedInterests.value ?: emptyList()
        )
    }
}
