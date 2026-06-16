package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

object ProgressModel {
    const val STAGE = "stage"
    const val COUNT = "count"
    const val STREAK = "streak"
}

object StageStatus {
    const val NOT_STARTED = "not_started"
    const val IN_PROGRESS = "in_progress"
    const val COMPLETED = "completed"
}

data class Challenge(
    val id: Int,
    val hub: Int,
    @SerializedName("hub_name") val hubName: String? = null,
    val title: String,
    val description: String = "",
    @SerializedName("progress_model") val progressModel: String = ProgressModel.COUNT,
    @SerializedName("is_main") val isMain: Boolean = false,
    @SerializedName("created_by") val createdBy: Int = 0,
    @SerializedName("created_by_username") val createdByUsername: String? = null,
    @SerializedName("ends_at") val endsAt: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    val summary: String = "",
    @SerializedName("percent_complete") val percentComplete: Int = 0,
    // Only populated by the GET /challenges/<id>/ detail endpoint:
    @SerializedName("can_manage") val canManage: Boolean = false,
    val stages: List<ChallengeStageStatus>? = null,
    @SerializedName("count_config") val countConfig: CountConfig? = null,
    @SerializedName("streak_config") val streakConfig: StreakConfig? = null,
    @SerializedName("my_progress") val myProgress: MyProgress? = null,
)

data class ChallengeStageStatus(
    val id: Int,
    @SerializedName("order_index") val orderIndex: Int,
    val title: String,
    val description: String = "",
    @SerializedName("proof_type") val proofType: String = "any",
    @SerializedName("proof_prompt") val proofPrompt: String = "",
    @SerializedName("is_milestone") val isMilestone: Boolean = false,
    val status: String = StageStatus.NOT_STARTED,
)

data class CountConfig(
    @SerializedName("target_count") val targetCount: Double = 0.0,
    @SerializedName("unit_label") val unitLabel: String = "",
    @SerializedName("entry_increment") val entryIncrement: Double = 1.0,
    @SerializedName("require_proof_per_entry") val requireProofPerEntry: Boolean = false,
)

data class StreakConfig(
    @SerializedName("target_days") val targetDays: Int = 0,
    val frequency: String = "daily",
    @SerializedName("grace_days") val graceDays: Int = 0,
    @SerializedName("require_proof") val requireProof: Boolean = false,
)

/** Shape differs by progress model; unused fields stay null for a given challenge. */
data class MyProgress(
    @SerializedName("current_count") val currentCount: Double? = null,
    @SerializedName("is_complete") val isComplete: Boolean = false,
    @SerializedName("current_streak") val currentStreak: Int? = null,
    @SerializedName("longest_streak") val longestStreak: Int? = null,
    @SerializedName("total_checkins") val totalCheckins: Int? = null,
    @SerializedName("checkin_calendar") val checkinCalendar: List<String>? = null,
    @SerializedName("last_checkin_date") val lastCheckinDate: String? = null,
)

data class HubMember(
    @SerializedName("user_id") val userId: Int,
    val username: String,
    @SerializedName("full_name") val fullName: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val role: String = "member",
    @SerializedName("is_creator") val isCreator: Boolean = false,
)

data class LeaderboardEntry(
    val rank: Int,
    @SerializedName("user_id") val userId: Int,
    val username: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val score: Double = 0.0,
)

// ---- Create challenge request (model-aware) ----

data class StageInput(
    val title: String,
    val description: String = "",
    @SerializedName("proof_type") val proofType: String = "any",
    @SerializedName("proof_prompt") val proofPrompt: String = "",
    @SerializedName("is_milestone") val isMilestone: Boolean = false,
)

data class CountConfigInput(
    @SerializedName("target_count") val targetCount: Double,
    @SerializedName("unit_label") val unitLabel: String = "",
    @SerializedName("entry_increment") val entryIncrement: Double = 1.0,
    @SerializedName("require_proof_per_entry") val requireProofPerEntry: Boolean = false,
)

data class StreakConfigInput(
    @SerializedName("target_days") val targetDays: Int,
    val frequency: String = "daily",
    @SerializedName("grace_days") val graceDays: Int = 0,
    @SerializedName("require_proof") val requireProof: Boolean = false,
)

data class CreateChallengeRequest(
    val title: String,
    val description: String = "",
    @SerializedName("progress_model") val progressModel: String,
    @SerializedName("is_main") val isMain: Boolean = false,
    @SerializedName("ends_at") val endsAt: String? = null,
    val stages: List<StageInput>? = null,
    @SerializedName("count_config") val countConfig: CountConfigInput? = null,
    @SerializedName("streak_config") val streakConfig: StreakConfigInput? = null,
)

// ---- Progress action responses ----

data class StageCompleteResponse(
    @SerializedName("stage_id") val stageId: Int,
    val status: String,
    @SerializedName("completed_stages") val completedStages: Int,
    @SerializedName("total_stages") val totalStages: Int,
    @SerializedName("is_complete") val isComplete: Boolean,
)

data class CountLogResponse(
    @SerializedName("current_count") val currentCount: Double,
    @SerializedName("target_count") val targetCount: Double,
    @SerializedName("is_complete") val isComplete: Boolean,
)

data class StreakCheckinResponse(
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("total_checkins") val totalCheckins: Int,
    @SerializedName("target_days") val targetDays: Int,
    @SerializedName("is_complete") val isComplete: Boolean,
    @SerializedName("checkin_calendar") val checkinCalendar: List<String>,
)
