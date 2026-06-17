package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class HubCategory(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
)
