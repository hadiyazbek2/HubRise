package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

object TemplateCategory {
    const val FITNESS = "fitness"
    const val FINANCE = "finance"
    const val LEARNING = "learning"
    const val READING = "reading"
    const val MINDFULNESS = "mindfulness"
    const val CREATIVE = "creative"
    const val CAREER = "career"
    const val OTHER = "other"

    val ALL = listOf(FITNESS, FINANCE, LEARNING, READING, MINDFULNESS, CREATIVE, CAREER, OTHER)

    fun label(category: String): String = when (category) {
        FITNESS -> "Fitness"
        FINANCE -> "Finance"
        LEARNING -> "Learning"
        READING -> "Reading"
        MINDFULNESS -> "Mindfulness"
        CREATIVE -> "Creative"
        CAREER -> "Career"
        else -> "Other"
    }
}

data class TemplateStage(
    @SerializedName("order_index") val orderIndex: Int,
    val title: String,
    val description: String = "",
    @SerializedName("proof_type") val proofType: String = "any",
)

data class ChallengeTemplate(
    val id: Int,
    val name: String,
    val category: String,
    @SerializedName("progress_model") val progressModel: String,
    val description: String = "",
    @SerializedName("is_official") val isOfficial: Boolean = true,
    @SerializedName("use_count") val useCount: Int = 0,
    val stages: List<TemplateStage> = emptyList(),
    @SerializedName("count_target") val countTarget: Double? = null,
    @SerializedName("count_unit_label") val countUnitLabel: String = "",
    @SerializedName("count_entry_increment") val countEntryIncrement: Double = 1.0,
    @SerializedName("streak_target_days") val streakTargetDays: Int? = null,
    @SerializedName("streak_frequency") val streakFrequency: String = "daily",
    @SerializedName("streak_grace_days") val streakGraceDays: Int = 0,
)
