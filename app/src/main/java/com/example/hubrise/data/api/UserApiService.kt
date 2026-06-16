package com.example.hubrise.data.api

import com.example.hubrise.data.model.FollowResponse
import com.example.hubrise.data.model.PaginatedResponse
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.model.ProfilePictureResponse
import com.example.hubrise.data.model.UpdateProfileRequest
import com.example.hubrise.data.model.UpdateProfileResponse
import com.example.hubrise.data.model.UserPublicProfile
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface UserApiService {

    @GET("api/users/{id}/profile/")
    suspend fun getUserProfile(
        @Path("id") userId: Int
    ): Response<UserPublicProfile>

    @PATCH("api/users/{id}/profile/")
    suspend fun updateProfile(
        @Path("id") userId: Int,
        @Body request: UpdateProfileRequest
    ): Response<UpdateProfileResponse>

    @Multipart
    @POST("api/users/{id}/profile-picture/")
    suspend fun uploadProfilePicture(
        @Path("id") userId: Int,
        @Part image: MultipartBody.Part
    ): Response<ProfilePictureResponse>

    @GET("api/users/{id}/posts/")
    suspend fun getUserPosts(
        @Path("id") userId: Int
    ): Response<PaginatedResponse<Post>>

    @POST("api/users/{id}/follow/")
    suspend fun followToggle(
        @Path("id") userId: Int
    ): Response<FollowResponse>
}
