package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class CreateHubRequest(
    val name: String,
    val description: String,
    val category: Int? = null,
    @SerializedName("is_public") val isPublic: Boolean = true
)
