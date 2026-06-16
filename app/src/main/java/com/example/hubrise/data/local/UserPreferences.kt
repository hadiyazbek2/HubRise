package com.example.hubrise.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ID = intPreferencesKey("user_id")
        private val EMAIL = stringPreferencesKey("email")
        private val USERNAME = stringPreferencesKey("username")
        private val FULL_NAME = stringPreferencesKey("full_name")
        private val PROFILE_PICTURE_URL = stringPreferencesKey("profile_picture_url")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val LAST_LOGIN_TIMESTAMP = longPreferencesKey("last_login_timestamp")
    }

    // Access Token
    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = token
        }
    }

    // Refresh Token
    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN] = token
        }
    }

    // User ID
    val userId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID]
    }

    suspend fun saveUserId(id: Int) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = id
        }
    }

    // Email
    val email: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[EMAIL]
    }

    suspend fun saveEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[EMAIL] = email
        }
    }

    // Username
    val username: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USERNAME]
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME] = username
        }
    }

    // Full Name
    val fullName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FULL_NAME]
    }

    suspend fun saveFullName(fullName: String) {
        context.dataStore.edit { preferences ->
            preferences[FULL_NAME] = fullName
        }
    }

    // Profile Picture URL
    val profilePictureUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_PICTURE_URL]
    }

    suspend fun saveProfilePictureUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_PICTURE_URL] = url
        }
    }

    // Login Status
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    suspend fun setLoginStatus(loggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = loggedIn
        }
    }

    // Last Login Timestamp
    val lastLoginTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_LOGIN_TIMESTAMP] ?: 0L
    }

    suspend fun saveLastLoginTimestamp() {
        context.dataStore.edit { preferences ->
            preferences[LAST_LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    // Returns true if an access token is stored
    suspend fun hasToken(): Boolean = accessToken.first()?.isNotEmpty() == true

    // Clear all data on logout
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
