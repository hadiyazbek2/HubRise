package com.example.hubrise.data.api

import com.example.hubrise.data.model.PaginatedResponse
import com.example.hubrise.data.model.Post
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExploreApiService {

    @GET("api/explore/feed/")
    suspend fun getExploreFeed(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): Response<PaginatedResponse<Post>>
}
