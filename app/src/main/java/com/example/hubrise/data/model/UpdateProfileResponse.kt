package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class UpdateProfileResponse(
    @SerializedName("full_name") val fullName: String,
    val bio: String,
)
