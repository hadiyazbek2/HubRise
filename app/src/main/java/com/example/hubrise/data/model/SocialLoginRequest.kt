package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class SocialLoginRequest(
    @SerializedName("provider")
    val provider: String,
    @SerializedName("id_token")
    val idToken: String
)
