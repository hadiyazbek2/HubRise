package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class NotificationItem(
    val id: Int,
    val type: String,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("sender_username") val senderUsername: String,
    @SerializedName("sender_avatar") val senderAvatar: String? = null,
    @SerializedName("post_id") val postId: Int? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String,
    val message: String,
)

data class UnreadCountResponse(
    val count: Int,
)
