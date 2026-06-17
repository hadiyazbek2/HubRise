package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

object PostType {
    const val REGULAR = "regular"
    const val PROGRESS = "progress_update"
    const val ACHIEVEMENT = "achievement_broadcast"
    const val ANNOUNCEMENT = "admin_announcement"
    const val STAGE_PROOF = "stage_proof"
    const val COUNT_ENTRY = "count_entry"
    const val STREAK_CHECKIN = "streak_checkin"

    val CHALLENGE_TYPES = setOf(STAGE_PROOF, COUNT_ENTRY, STREAK_CHECKIN)
}

data class Post(
    val id: Int,
    val author: Int = 0,
    @SerializedName("author_username") val authorUsername: String,
    @SerializedName("author_avatar_url") val authorAvatarUrl: String?,
    @SerializedName("author_wishlist_url") val authorWishlistUrl: String? = null,
    @SerializedName("media_type") val mediaType: String = "",
    val hub: Int? = null,
    @SerializedName("hub_name") val hubName: String?,
    @SerializedName("post_type") val postType: String = "regular",
    val content: String,
    @SerializedName("media_url") val mediaUrl: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("likes_count") val likesCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("liked_by_me") val isLiked: Boolean = false,
    val challenge: Int? = null,
    @SerializedName("challenge_title") val challengeTitle: String? = null,
    @SerializedName("is_trusted") val isTrusted: Boolean = false,
    @SerializedName("validations_count") val validationsCount: Int = 0,
    @SerializedName("validated_by_me") val validatedByMe: Boolean = false,
)

data class ValidateResponse(
    @SerializedName("post_id") val postId: Int,
    @SerializedName("new_score") val newScore: Double,
    @SerializedName("is_trusted") val isTrusted: Boolean,
    @SerializedName("your_weight") val yourWeight: Double? = null,
)
