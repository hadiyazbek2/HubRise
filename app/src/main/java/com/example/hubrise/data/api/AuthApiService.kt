package com.example.hubrise.data.api

import com.example.hubrise.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface AuthApiService {

    @POST("api/auth/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("api/auth/social-login/")
    suspend fun socialLogin(
        @Body request: SocialLoginRequest
    ): AuthResponse

    @GET("api/auth/check-email/")
    suspend fun checkEmailAvailability(
        @Query("email") email: String
    ): AvailabilityResponse

    @GET("api/auth/check-username/")
    suspend fun checkUsernameAvailability(
        @Query("username") username: String
    ): AvailabilityResponse

    @POST("api/auth/signup/")
    suspend fun signup(
        @Body request: SignupRequest
    ): AuthResponse

    @POST("api/auth/token/refresh/")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): RefreshTokenResponse

    @Multipart
    @POST("api/users/{user_id}/profile-picture/")
    suspend fun uploadProfilePicture(
        @Path("user_id") userId: Int,
        @Part("image_file") imagePart: MultipartBody.Part
    ): ProfilePictureResponse
}
