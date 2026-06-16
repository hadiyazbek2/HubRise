package com.example.hubrise.data.repository

import com.example.hubrise.data.api.NotificationApiService
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.NotificationItem

class NotificationRepository {

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    private val api: NotificationApiService by lazy {
        RetrofitClient.getRetrofit().create(NotificationApiService::class.java)
    }

    suspend fun getNotifications(): Result<List<NotificationItem>> = try {
        val r = api.getNotifications()
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getUnreadCount(): Result<Int> = try {
        val r = api.getUnreadCount()
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!.count)
        else Result.Error("${r.code()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
}
