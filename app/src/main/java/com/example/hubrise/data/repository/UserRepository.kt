package com.example.hubrise.data.repository

import com.example.hubrise.data.api.RetrofitClient
import com.example.hubrise.data.api.UserApiService
import com.example.hubrise.data.model.FollowResponse
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.model.ProfilePictureResponse
import com.example.hubrise.data.model.UpdateProfileRequest
import com.example.hubrise.data.model.UpdateProfileResponse
import com.example.hubrise.data.model.UserPublicProfile
import okhttp3.MultipartBody

class UserRepository {

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    private val api: UserApiService by lazy {
        RetrofitClient.getRetrofit().create(UserApiService::class.java)
    }

    suspend fun getProfile(userId: Int): Result<UserPublicProfile> = try {
        val r = api.getUserProfile(userId)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun updateProfile(userId: Int, fullName: String, bio: String): Result<UpdateProfileResponse> = try {
        val r = api.updateProfile(userId, UpdateProfileRequest(fullName = fullName, bio = bio))
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun uploadProfilePicture(userId: Int, imagePart: MultipartBody.Part): Result<ProfilePictureResponse> = try {
        val r = api.uploadProfilePicture(userId, imagePart)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun getPosts(userId: Int): Result<List<Post>> = try {
        val r = api.getUserPosts(userId)
        if (r.isSuccessful) Result.Success(r.body()?.results ?: emptyList())
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }

    suspend fun followToggle(userId: Int): Result<FollowResponse> = try {
        val r = api.followToggle(userId)
        if (r.isSuccessful && r.body() != null) Result.Success(r.body()!!)
        else Result.Error("${r.code()}: ${r.errorBody()?.string()}")
    } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
}
