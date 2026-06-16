package com.example.hubrise.data.repository

import com.example.hubrise.data.api.AuthApiService
import com.example.hubrise.data.local.UserPreferences
import com.example.hubrise.data.model.*

class AuthRepository(
    private val apiService: AuthApiService,
    private val userPreferences: UserPreferences
) {

    // Login
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)

            // Save tokens and user data
            userPreferences.saveAccessToken(response.accessToken)
            userPreferences.saveRefreshToken(response.refreshToken)
            userPreferences.saveUserId(response.user.id)
            userPreferences.saveEmail(response.user.email)
            userPreferences.saveUsername(response.user.username)
            userPreferences.saveFullName(response.user.fullName)
            userPreferences.saveProfilePictureUrl(response.user.profilePictureUrl ?: "")
            userPreferences.setLoginStatus(true)
            userPreferences.saveLastLoginTimestamp()

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Social Login
    suspend fun socialLogin(provider: String, idToken: String): Result<AuthResponse> {
        return try {
            val request = SocialLoginRequest(provider, idToken)
            val response = apiService.socialLogin(request)

            // Save tokens and user data
            userPreferences.saveAccessToken(response.accessToken)
            userPreferences.saveRefreshToken(response.refreshToken)
            userPreferences.saveUserId(response.user.id)
            userPreferences.saveEmail(response.user.email)
            userPreferences.saveUsername(response.user.username)
            userPreferences.saveFullName(response.user.fullName)
            userPreferences.saveProfilePictureUrl(response.user.profilePictureUrl ?: "")
            userPreferences.setLoginStatus(true)
            userPreferences.saveLastLoginTimestamp()

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check email availability
    suspend fun checkEmailAvailability(email: String): Result<Boolean> {
        return try {
            val response = apiService.checkEmailAvailability(email)
            Result.success(response.available)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check username availability
    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val response = apiService.checkUsernameAvailability(username)
            Result.success(response.available)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Signup
    suspend fun signup(
        email: String,
        password: String,
        fullName: String,
        username: String,
        dateOfBirth: String,
        phoneNumber: String? = null,
        interests: List<Int> = emptyList()
    ): Result<AuthResponse> {
        return try {
            val request = SignupRequest(
                email = email,
                password = password,
                fullName = fullName,
                username = username,
                dateOfBirth = dateOfBirth,
                phoneNumber = phoneNumber,
                interests = interests
            )
            val response = apiService.signup(request)

            // Save tokens and user data
            userPreferences.saveAccessToken(response.accessToken)
            userPreferences.saveRefreshToken(response.refreshToken)
            userPreferences.saveUserId(response.user.id)
            userPreferences.saveEmail(response.user.email)
            userPreferences.saveUsername(response.user.username)
            userPreferences.saveFullName(response.user.fullName)
            userPreferences.saveProfilePictureUrl(response.user.profilePictureUrl ?: "")
            userPreferences.setLoginStatus(true)
            userPreferences.saveLastLoginTimestamp()

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Refresh token
    suspend fun refreshToken(refreshToken: String): Result<String> {
        return try {
            val request = RefreshTokenRequest(refreshToken)
            val response = apiService.refreshToken(request)

            // Update access token
            userPreferences.saveAccessToken(response.access)

            Result.success(response.access)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upload profile picture
    suspend fun uploadProfilePicture(userId: Int, imagePart: okhttp3.MultipartBody.Part): Result<String> {
        return try {
            val response = apiService.uploadProfilePicture(userId, imagePart)

            // Save new URL
            userPreferences.saveProfilePictureUrl(response.profilePictureUrl)

            Result.success(response.profilePictureUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logout
    suspend fun logout() {
        userPreferences.clearAll()
    }
}
