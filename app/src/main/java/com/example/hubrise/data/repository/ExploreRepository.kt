package com.example.hubrise.data.repository

import com.example.hubrise.data.api.ExploreApiService
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.Post

class ExploreRepository {

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    private val api: ExploreApiService by lazy {
        RetrofitClient.getRetrofit().create(ExploreApiService::class.java)
    }

    suspend fun getFeed(page: Int = 1): Result<List<Post>> = try {
        val r = api.getExploreFeed(page = page, pageSize = 20)
        if (r.isSuccessful) Result.Success(r.body()?.results ?: emptyList())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }
}
