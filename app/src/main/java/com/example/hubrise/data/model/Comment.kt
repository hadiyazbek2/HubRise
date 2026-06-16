package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class Comment(
    val id: Int,
    val post: Int,
    val author: Int,
    @SerializedName("author_username") val authorUsername: String,
    @SerializedName("author_avatar_url") val authorAvatarUrl: String?,
    val content: String,
    @SerializedName("created_at") val createdAt: String,
)
