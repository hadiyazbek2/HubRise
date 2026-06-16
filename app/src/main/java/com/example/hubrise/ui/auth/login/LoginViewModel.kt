package com.example.hubrise.ui.auth.login

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

class LoginViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    private val apiService = RetrofitClient.getAuthApiService()
    private val repository = AuthRepository(apiService, userPreferences)

    // UI State
    private val _email = MutableLiveData("")
    val email: LiveData<String> = _email

    private val _password = MutableLiveData("")
    val password: LiveData<String> = _password

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginError = SingleLiveEvent<String>()
    val loginError: LiveData<String> = _loginError

    private val _loginSuccess = SingleLiveEvent<Unit>()
    val loginSuccess: LiveData<Unit> = _loginSuccess

    private val _emailError = SingleLiveEvent<String>()
    val emailError: LiveData<String> = _emailError

    private val _passwordError = SingleLiveEvent<String>()
    val passwordError: LiveData<String> = _passwordError

    // Update email
    fun setEmail(newEmail: String) {
        _email.value = newEmail
        // Clear error when user starts typing
        if (_emailError.value != null) {
            _emailError.value = null
        }
    }

    // Update password
    fun setPassword(newPassword: String) {
        _password.value = newPassword
        // Clear error when user starts typing
        if (_passwordError.value != null) {
            _passwordError.value = null
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
        if (password.isEmpty()) {
            _passwordError.value = "Password is required"
            isValid = false
        }

        return isValid
    }

    // Login
    fun login() {
        if (!validateForm()) {
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            val email = _email.value ?: ""
            val password = _password.value ?: ""

            val result = repository.login(email, password)

            result.onSuccess {
                _loginSuccess.value = Unit
            }

            result.onFailure { exception ->
                _isLoading.value = false
                _loginError.value = when {
                    exception.message?.contains("401") == true -> "Invalid email or password"
                    exception.message?.contains("Connection refused") == true -> "Cannot connect to server"
                    exception.message.isNullOrEmpty() -> "An error occurred. Please try again"
                    else -> exception.message ?: "Login failed"
                }
            }
        }
    }

    // Social login
    fun socialLogin(provider: String, idToken: String) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.socialLogin(provider, idToken)

            result.onSuccess {
                _loginSuccess.value = Unit
            }

            result.onFailure { exception ->
                _isLoading.value = false
                _loginError.value = exception.message ?: "Social login failed"
            }
        }
    }
}
