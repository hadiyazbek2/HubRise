package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

object NotificationType {
    const val FOLLOW = "follow"
    const val LIKE = "like"
    const val COMMENT = "comment"
    const val COMPLETION_SUBMITTED = "completion_submitted"
    const val COMPLETION_APPROVED = "completion_approved"
    const val COMPLETION_REJECTED = "completion_rejected"
}

data class NotificationItem(
    val id: Int,
    val type: String,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("sender_username") val senderUsername: String,
    @SerializedName("sender_avatar") val senderAvatar: String? = null,
    @SerializedName("post_id") val postId: Int? = null,
    @SerializedName("challenge_id") val challengeId: Int? = null,
    @SerializedName("challenge_title") val challengeTitle: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String,
    val message: String,
)

data class UnreadCountResponse(
    val count: Int,
)
