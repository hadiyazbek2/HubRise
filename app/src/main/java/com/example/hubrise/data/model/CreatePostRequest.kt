package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class CreatePostRequest(
    val hub: Int? = null,
    @SerializedName("post_type") val postType: String = "regular",
    val content: String,
    @SerializedName("is_public") val isPublic: Boolean = true,
    // When set, this post performs the challenge's progress action (stage
    // complete / count log / streak check-in) instead of being a regular post.
    val challenge: Int? = null,
    val stage: Int? = null,
    val amount: Double? = null,
)
