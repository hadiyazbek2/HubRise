package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class UserSearchResult(
    val id: Int,
    val username: String,
    @SerializedName("full_name") val fullName: String = "",
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
)

data class PostSearchResult(
    val id: Int,
    val content: String = "",
    @SerializedName("author_id") val authorId: Int,
    @SerializedName("author_username") val authorUsername: String,
    @SerializedName("author_avatar_url") val authorAvatarUrl: String? = null,
    @SerializedName("hub_name") val hubName: String? = null,
    @SerializedName("hub_id") val hubId: Int? = null,
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("likes_count") val likesCount: Int = 0,
)

data class ChallengeSearchResult(
    val id: Int,
    val title: String,
    val description: String = "",
    @SerializedName("hub_name") val hubName: String? = null,
    @SerializedName("hub_id") val hubId: Int? = null,
)

data class SearchResponse(
    val users: List<UserSearchResult> = emptyList(),
    val hubs: List<Hub> = emptyList(),
    val posts: List<PostSearchResult> = emptyList(),
    val challenges: List<ChallengeSearchResult> = emptyList(),
)
