package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class FollowResponse(
    @SerializedName("is_following") val isFollowing: Boolean,
    @SerializedName("followers_count") val followersCount: Int,
)
