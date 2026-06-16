package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class UserPublicProfile(
    val id: Int,
    val username: String,
    @SerializedName("full_name") val fullName: String = "",
    val bio: String = "",
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("wishlist_url") val wishlistUrl: String = "",
    @SerializedName("post_count") val postCount: Int = 0,
    @SerializedName("hubs_count") val hubsCount: Int = 0,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("is_following") val isFollowing: Boolean = false,
)
