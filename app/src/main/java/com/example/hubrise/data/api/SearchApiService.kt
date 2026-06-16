package com.example.hubrise.data.api

import com.example.hubrise.data.model.SearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApiService {

    @GET("api/search/")
    suspend fun search(@Query("q") query: String): Response<SearchResponse>
}
