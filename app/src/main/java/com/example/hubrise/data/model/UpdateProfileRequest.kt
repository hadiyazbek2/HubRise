package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("full_name") val fullName: String?,
    val bio: String?,
    @SerializedName("wishlist_url") val wishlistUrl: String? = null,
)
