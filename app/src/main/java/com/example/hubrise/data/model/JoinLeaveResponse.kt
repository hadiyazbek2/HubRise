package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class JoinLeaveResponse(
    val detail: String = "",
    @SerializedName("members_count") val membersCount: Int = 0
)
