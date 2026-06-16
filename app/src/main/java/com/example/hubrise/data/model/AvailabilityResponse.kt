package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class AvailabilityResponse(
    @SerializedName("available")
    val available: Boolean
)
