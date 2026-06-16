package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

object CompletionStatus {
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
}

data class CompletionRequest(
    val id: Int,
    val user: Int,
    val username: String,
    @SerializedName("user_avatar_url") val userAvatarUrl: String? = null,
    val challenge: Int,
    @SerializedName("challenge_title") val challengeTitle: String? = null,
    @SerializedName("hub_id") val hubId: Int,
    val status: String = CompletionStatus.PENDING,
    @SerializedName("member_note") val memberNote: String = "",
    @SerializedName("submitted_at") val submittedAt: String = "",
    @SerializedName("reviewed_at") val reviewedAt: String? = null,
    @SerializedName("reviewed_by") val reviewedBy: Int? = null,
    @SerializedName("reviewed_by_username") val reviewedByUsername: String? = null,
    @SerializedName("admin_note") val adminNote: String = "",
    @SerializedName("announcement_post") val announcementPost: Int? = null,
)
