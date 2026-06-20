package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class Hub(
    val id: Int,
    val name: String,
    val description: String = "",
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("members_count") val membersCount: Int = 0,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("is_public") val isPublic: Boolean = true,
    @SerializedName("is_member") val isMember: Boolean = false,
    @SerializedName("is_creator") val isCreator: Boolean = false,
    @SerializedName("created_by") val createdBy: Int = 0,
    @SerializedName("created_by_username") val createdByUsername: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("main_challenge") val mainChallenge: MainChallenge? = null,
    @SerializedName("invite_code") val inviteCode: String? = null,
)

data class MainChallenge(
    val id: Int,
    val title: String,
    @SerializedName("progress_model") val progressModel: String,
    val summary: String,
    @SerializedName("percent_complete") val percentComplete: Int,
)
