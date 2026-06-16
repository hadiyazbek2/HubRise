package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: Int,
    @SerializedName("email")
    val email: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String? = null,
    @SerializedName("interests")
    val interests: List<Int> = emptyList()
)
