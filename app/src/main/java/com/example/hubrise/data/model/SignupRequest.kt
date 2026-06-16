package com.example.hubrise.data.model

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("date_of_birth")
    val dateOfBirth: String,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("interests")
    val interests: List<Int> = emptyList()
)
