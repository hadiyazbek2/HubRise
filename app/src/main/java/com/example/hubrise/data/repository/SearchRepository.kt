package com.example.hubrise.data.repository

import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.api.SearchApiService
import com.example.hubrise.data.model.SearchResponse

class SearchRepository {

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    private val api: SearchApiService by lazy {
        RetrofitClient.getRetrofit().create(SearchApiService::class.java)
    }

    suspend fun search(query: String): Result<SearchResponse> = try {
        val r = api.search(query)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
}
