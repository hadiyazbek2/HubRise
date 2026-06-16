package com.example.hubrise.data.repository

import com.example.hubrise.data.api.CommentApiService
import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.Comment
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class CommentRepository {

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    private val api: CommentApiService by lazy {
        RetrofitClient.getRetrofit().create(CommentApiService::class.java)
    }

    suspend fun getComments(postId: Int): Result<List<Comment>> = try {
        val r = api.getComments(postId)
        if (r.isSuccessful) Result.Success(r.body()?.results ?: emptyList())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun createComment(postId: Int, content: String): Result<Comment> = try {
        val body = JSONObject().put("content", content).toString()
            .toRequestBody("application/json".toMediaTypeOrNull())
        val r = api.createComment(postId, body)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun deleteComment(commentId: Int): Result<Unit> = try {
        val r = api.deleteComment(commentId)
        if (r.isSuccessful) Result.Success(Unit)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
}
