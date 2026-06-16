package com.example.hubrise.ui.auth.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hubrise.utils.PasswordStrength
import com.example.hubrise.utils.SingleLiveEvent
import com.example.hubrise.utils.ValidationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SignupStep1ViewModel : ViewModel() {

    // UI State
    private val _email = MutableLiveData("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData("")
    val password: LiveData<String> = _password

    private val _confirmPassword = MutableLiveData("")
    val confirmPassword: LiveData<String> = _confirmPassword

    private val _agreeToTerms = MutableLiveData(false)
    val agreeToTerms: LiveData<Boolean> = _agreeToTerms

    // Password strength
    private val _passwordStrength = MutableLiveData(PasswordStrength.WEAK)
    val passwordStrength: LiveData<PasswordStrength> = _passwordStrength

    // Validation states
    private val _emailError = SingleLiveEvent<String>()
    val emailError: LiveData<String> = _emailError

    private val _passwordError = SingleLiveEvent<String>()
    val passwordError: LiveData<String> = _passwordError

    private val _confirmPasswordError = SingleLiveEvent<String>()
    val confirmPasswordError: LiveData<String> = _confirmPasswordError

    private val _termsError = SingleLiveEvent<String>()
    val termsError: LiveData<String> = _termsError

    private val _isCheckingEmail = MutableLiveData(false)
    val isCheckingEmail: LiveData<Boolean> = _isCheckingEmail

    private val _emailAvailable = MutableLiveData(true)
    val emailAvailable: LiveData<Boolean> = _emailAvailable

    private val _proceedToStep2 = SingleLiveEvent<Unit>()
    val proceedToStep2: LiveData<Unit> = _proceedToStep2

    private var emailCheckJob: Job? = null

    // Update email with debounce check
    fun setEmail(newEmail: String) {
        _email.value = newEmail

        // Clear error when user starts typing
        if (_emailError.value != null) {
            _emailError.value = null
        }

        // Cancel previous job
        emailCheckJob?.cancel()

        // Validate format first
        if (newEmail.isEmpty()) {
            _emailAvailable.value = true
            return
        }

        if (!ValidationHelper.isValidEmail(newEmail)) {
            _emailAvailable.value = true
            return
        }

        // Check availability with debounce
        emailCheckJob = viewModelScope.launch {
            delay(500) // Debounce
            _isCheckingEmail.value = true
            // TODO: Call repository to check email availability
            delay(500) // Simulate API call
            _isCheckingEmail.value = false
        }
    }

    // Update password and check strength
    fun setPassword(newPassword: String) {
        _password.value = newPassword

        // Clear error when user starts typing
        if (_passwordError.value != null) {
            _passwordError.value = null
        }

        // Update strength
        _passwordStrength.value = ValidationHelper.getPasswordStrength(newPassword)
    }

    // Update confirm password
    fun setConfirmPassword(newConfirmPassword: String) {
        _confirmPassword.value = newConfirmPassword

        // Clear error when user starts typing
        if (_confirmPasswordError.value != null) {
            _confirmPasswordError.value = null
        }
    }

    // Toggle terms agreement
    fun toggleTermsAgreement(agreed: Boolean) {
        _agreeToTerms.value = agreed

        // Clear error when user changes checkbox
        if (_termsError.value != null) {
            _termsError.value = null
        }
    }

    // Validate form
    private fun validateForm(): Boolean {
        var isValid = true

        val email = _email.value ?: ""
        if (email.isEmpty()) {
            _emailError.value = "Email is required"
            isValid = false
        } else if (!ValidationHelper.isValidEmail(email)) {
            _emailError.value = "Please enter a valid email"
            isValid = false
        }

        val password = _password.value ?: ""
        val passwordError = ValidationHelper.isValidPassword(password)
        if (passwordError != null) {
            _passwordError.value = passwordError
            isValid = false
        }

        val confirmPassword = _confirmPassword.value ?: ""
        if (!ValidationHelper.passwordsMatch(password, confirmPassword)) {
            _confirmPasswordError.value = "Passwords do not match"
            isValid = false
        }

        if (_agreeToTerms.value != true) {
            _termsError.value = "You must agree to the terms and conditions"
            isValid = false
        }

        return isValid
    }

    // Proceed to next step
    fun proceedToStep2() {
        if (validateForm()) {
            _proceedToStep2.value = Unit
        }
    }

    // Get form data for Step 1
    data class Step1Data(
        val email: String,
        val password: String
    )

    fun getFormData(): Step1Data {
        return Step1Data(
            email = _email.value ?: "",
            password = _password.value ?: ""
        )
    }
}
