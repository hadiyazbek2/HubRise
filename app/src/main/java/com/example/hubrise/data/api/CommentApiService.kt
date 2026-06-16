package com.example.hubrise.data.api

import com.example.hubrise.data.model.Comment
import com.example.hubrise.data.model.PaginatedResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CommentApiService {

    @GET("api/posts/{id}/comments/")
    suspend fun getComments(
        @Path("id") postId: Int
    ): Response<PaginatedResponse<Comment>>

    @POST("api/posts/{id}/comments/")
    suspend fun createComment(
        @Path("id") postId: Int,
        @Body body: RequestBody
    ): Response<Comment>

    @DELETE("api/comments/{id}/")
    suspend fun deleteComment(
        @Path("id") commentId: Int
    ): Response<Unit>
}
