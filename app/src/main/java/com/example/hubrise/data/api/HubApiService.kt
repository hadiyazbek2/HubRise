package com.example.hubrise.data.api

import com.example.hubrise.data.model.Challenge
import com.example.hubrise.data.model.ChallengeTemplate
import com.example.hubrise.data.model.CompletionRequest
import com.example.hubrise.data.model.CreateChallengeRequest
import com.example.hubrise.data.model.CreateHubRequest
import com.example.hubrise.data.model.CreatePostRequest
import com.example.hubrise.data.model.Hub
import com.example.hubrise.data.model.HubCategory
import com.example.hubrise.data.model.HubMember
import com.example.hubrise.data.model.JoinLeaveResponse
import com.example.hubrise.data.model.LeaderboardEntry
import com.example.hubrise.data.model.PaginatedResponse
import com.example.hubrise.data.model.Post
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface HubApiService {

    @GET("api/hubs/")
    suspend fun getHubs(): Response<PaginatedResponse<Hub>>

    @GET("api/hubs/recommended/")
    suspend fun getRecommended(): Response<PaginatedResponse<Hub>>

    @GET("api/hubs/{id}/")
    suspend fun getHub(@Path("id") id: Int): Response<Hub>

    @POST("api/hubs/")
    suspend fun createHub(@Body request: CreateHubRequest): Response<Hub>

    @POST("api/hubs/{id}/join/")
    suspend fun joinHub(@Path("id") id: Int): Response<JoinLeaveResponse>

    @POST("api/hubs/{id}/leave/")
    suspend fun leaveHub(@Path("id") id: Int): Response<JoinLeaveResponse>

    @GET("api/hubs/{id}/posts/")
    suspend fun getHubPosts(@Path("id") id: Int): Response<PaginatedResponse<Post>>

    @GET("api/hubs/{id}/challenges/")
    suspend fun getHubChallenges(@Path("id") id: Int): Response<PaginatedResponse<Challenge>>

    @POST("api/hubs/{id}/challenges/")
    suspend fun createChallenge(@Path("id") hubId: Int, @Body request: CreateChallengeRequest): Response<Challenge>

    @GET("api/templates/")
    suspend fun getTemplates(
        @Query("category") category: String? = null,
        @Query("progress_model") progressModel: String? = null,
        @Query("search") search: String? = null,
    ): Response<List<ChallengeTemplate>>

    @GET("api/challenges/{id}/")
    suspend fun getChallenge(@Path("id") id: Int): Response<Challenge>

    @DELETE("api/challenges/{id}/")
    suspend fun deleteChallenge(@Path("id") id: Int): Response<Unit>

    @GET("api/challenges/{id}/leaderboard/")
    suspend fun getLeaderboard(@Path("id") id: Int): Response<List<LeaderboardEntry>>

    @POST("api/challenges/{id}/completion-request/")
    suspend fun submitCompletionRequest(
        @Path("id") id: Int,
        @Body body: Map<String, String> = emptyMap(),
    ): Response<CompletionRequest>

    @GET("api/challenges/{id}/completion-request/mine/")
    suspend fun getMyCompletionRequest(@Path("id") id: Int): Response<CompletionRequest?>

    @GET("api/hubs/{id}/completion-requests/")
    suspend fun getHubCompletionRequests(
        @Path("id") id: Int,
        @Query("status") status: String = "pending",
    ): Response<List<CompletionRequest>>

    @PATCH("api/completion-requests/{id}/")
    suspend fun reviewCompletionRequest(
        @Path("id") id: Int,
        @Body body: Map<String, String>,
    ): Response<CompletionRequest>

    @GET("api/hubs/{id}/members/")
    suspend fun getHubMembers(@Path("id") id: Int): Response<List<HubMember>>

    @Multipart
    @PATCH("api/hubs/{id}/settings/")
    suspend fun updateHub(
        @Path("id") id: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards okhttp3.RequestBody>,
        @Part cover: MultipartBody.Part? = null,
    ): Response<Hub>

    @DELETE("api/hubs/{id}/delete/")
    suspend fun deleteHub(@Path("id") id: Int): Response<Unit>

    @DELETE("api/hubs/{id}/members/{userId}/")
    suspend fun removeMember(@Path("id") hubId: Int, @Path("userId") userId: Int): Response<Unit>

    @POST("api/posts/")
    suspend fun createPost(@Body request: CreatePostRequest): Response<Post>

    @Multipart
    @POST("api/posts/{id}/media/")
    suspend fun uploadPostMedia(
        @Path("id") postId: Int,
        @Part media: MultipartBody.Part,
    ): Response<Post>

    @Multipart
    @POST("api/hubs/{id}/cover/")
    suspend fun uploadHubCover(
        @Path("id") hubId: Int,
        @Part cover: MultipartBody.Part,
    ): Response<Hub>

    @GET("api/auth/interests/")
    suspend fun getCategories(): Response<List<HubCategory>>
}
