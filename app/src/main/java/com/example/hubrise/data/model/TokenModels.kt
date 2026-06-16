package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class RefreshTokenRequest(
    @SerializedName("refresh")
    val refresh: String
)

data class RefreshTokenResponse(
    @SerializedName("access")
    val access: String
)

data class ProfilePictureResponse(
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String
)
