package com.example.hubrise.data.repository

import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.model.ValidateResponse

class PostRepository {

    private val api = RetrofitClient.getFeedApiService()

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    suspend fun getFeed(): Result<List<Post>> = try {
        val r = api.getFeed()
        if (r.isSuccessful) Result.Success(r.body()?.results ?: emptyList())
        else Result.Error("Feed error ${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    suspend fun toggleLike(postId: Int): Result<Unit> = try {
        val r = api.toggleLike(postId)
        if (r.isSuccessful) Result.Success(Unit)
        else Result.Error("Like error ${r.code()}")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    suspend fun validatePost(postId: Int): Result<ValidateResponse> = try {
        val r = api.validatePost(postId)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }

    suspend fun unvalidatePost(postId: Int): Result<ValidateResponse> = try {
        val r = api.unvalidatePost(postId)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network error")
    }
}
