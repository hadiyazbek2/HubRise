package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class CreatePostRequest(
    val hub: Int? = null,
    @SerializedName("post_type") val postType: String = "regular",
    val content: String,
    @SerializedName("is_public") val isPublic: Boolean = true,
)
