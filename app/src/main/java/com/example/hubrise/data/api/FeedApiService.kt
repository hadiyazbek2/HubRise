package com.example.hubrise.data.api

import com.example.hubrise.data.model.PaginatedResponse
import com.example.hubrise.data.model.Post
import com.example.hubrise.data.model.ValidateResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FeedApiService {

    @GET("api/posts/feed/")
    suspend fun getFeed(@Query("page") page: Int = 1): Response<PaginatedResponse<Post>>

    @POST("api/posts/{id}/like/")
    suspend fun toggleLike(
        @Path("id") id: Int
    ): Response<Unit>

    @POST("api/posts/{id}/validate/")
    suspend fun validatePost(@Path("id") id: Int): Response<ValidateResponse>

    @DELETE("api/posts/{id}/validate/")
    suspend fun unvalidatePost(@Path("id") id: Int): Response<ValidateResponse>
}
