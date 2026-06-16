package com.example.hubrise.data.repository

import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.ChallengeTemplate
import com.example.hubrise.data.model.CompletionRequest
import com.example.hubrise.data.model.CreateChallengeRequest
import com.example.hubrise.data.model.CreateHubRequest
import com.example.hubrise.data.model.CreatePostRequest
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.model.HubMember
import com.example.hubrise.data.model.JoinLeaveResponse
import com.example.hubrise.data.model.LeaderboardEntry
import com.example.hubrise.data.model.Post
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class HubRepository {

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    private val api = RetrofitClient.getHubApiService()

    suspend fun getHubs(): Result<List<Hub>> = try {
        val r = api.getHubs()
        if (r.isSuccessful) Result.Success(r.body()?.results ?: emptyList())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getRecommended(): Result<List<Hub>> = try {
        val r = api.getRecommended()
        if (r.isSuccessful) Result.Success(r.body()?.results ?: emptyList())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getHub(id: Int): Result<Hub> = try {
        val r = api.getHub(id)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun createHub(request: CreateHubRequest): Result<Hub> = try {
        val r = api.createHub(request)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun joinHub(id: Int): Result<JoinLeaveResponse> = try {
        val r = api.joinHub(id)
        if (r.isSuccessful) Result.Success(r.body() ?: JoinLeaveResponse())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun leaveHub(id: Int): Result<JoinLeaveResponse> = try {
        val r = api.leaveHub(id)
        if (r.isSuccessful) Result.Success(r.body() ?: JoinLeaveResponse())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getHubPosts(id: Int): Result<List<Post>> = try {
        val r = api.getHubPosts(id)
        if (r.isSuccessful) Result.Success(r.body()?.results ?: emptyList())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getHubChallenges(id: Int): Result<List<Challenge>> = try {
        val r = api.getHubChallenges(id)
        if (r.isSuccessful) Result.Success(r.body()?.results ?: emptyList())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun createChallenge(hubId: Int, request: CreateChallengeRequest): Result<Challenge> = try {
        val r = api.createChallenge(hubId, request)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getTemplates(
        category: String? = null,
        progressModel: String? = null,
        search: String? = null,
    ): Result<List<ChallengeTemplate>> = try {
        val r = api.getTemplates(category, progressModel, search)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getChallenge(id: Int): Result<Challenge> = try {
        val r = api.getChallenge(id)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun deleteChallenge(id: Int): Result<Unit> = try {
        val r = api.deleteChallenge(id)
        if (r.isSuccessful) Result.Success(Unit)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getLeaderboard(challengeId: Int): Result<List<LeaderboardEntry>> = try {
        val r = api.getLeaderboard(challengeId)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun submitCompletionRequest(challengeId: Int, memberNote: String): Result<CompletionRequest> = try {
        val body = if (memberNote.isNotBlank()) mapOf("member_note" to memberNote) else emptyMap()
        val r = api.submitCompletionRequest(challengeId, body)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getMyCompletionRequest(challengeId: Int): Result<CompletionRequest?> = try {
        val r = api.getMyCompletionRequest(challengeId)
        if (r.isSuccessful) Result.Success(r.body())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getHubCompletionRequests(hubId: Int, status: String = "pending"): Result<List<CompletionRequest>> = try {
        val r = api.getHubCompletionRequests(hubId, status)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun approveCompletionRequest(requestId: Int, adminNote: String): Result<CompletionRequest> = try {
        val body = mutableMapOf("action" to "approve")
        if (adminNote.isNotBlank()) body["admin_note"] = adminNote
        val r = api.reviewCompletionRequest(requestId, body)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun rejectCompletionRequest(requestId: Int, adminNote: String): Result<CompletionRequest> = try {
        val r = api.reviewCompletionRequest(requestId, mapOf("action" to "reject", "admin_note" to adminNote))
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getHubMembers(id: Int): Result<List<HubMember>> = try {
        val r = api.getHubMembers(id)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun updateHub(
        id: Int,
        name: String?,
        description: String?,
        isPublic: Boolean?,
        coverFile: File?,
    ): Result<Hub> = try {
        val fields = mutableMapOf<String, okhttp3.RequestBody>()
        name?.let { fields["name"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
        description?.let { fields["description"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
        isPublic?.let { fields["is_public"] = it.toString().toRequestBody("text/plain".toMediaTypeOrNull()) }
        val coverPart = coverFile?.let {
            MultipartBody.Part.createFormData("cover_image", it.name, it.asRequestBody("image/*".toMediaTypeOrNull()))
        }
        val r = api.updateHub(id, fields, coverPart)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun deleteHub(id: Int): Result<Unit> = try {
        val r = api.deleteHub(id)
        if (r.isSuccessful) Result.Success(Unit)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun removeMember(hubId: Int, userId: Int): Result<Unit> = try {
        val r = api.removeMember(hubId, userId)
        if (r.isSuccessful) Result.Success(Unit)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun createPost(request: CreatePostRequest): Result<Post> = try {
        val r = api.createPost(request)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun uploadPostMedia(postId: Int, file: File): Result<Post> = try {
        val mimeType = when (file.extension.lowercase()) {
            "mp4", "mov", "avi", "mkv" -> "video/*"
            else -> "image/*"
        }
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("media_file", file.name, requestFile)
        val r = api.uploadPostMedia(postId, part)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun uploadHubCover(hubId: Int, file: File): Result<Hub> = try {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("cover_image", file.name, requestFile)
        val r = api.uploadHubCover(hubId, part)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
}
