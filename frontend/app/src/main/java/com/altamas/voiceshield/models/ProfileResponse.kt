package com.altamas.voiceshield.models

data class ProfileResponse(
    val user_id: Int,
    val email: String,
    val name: String?,
    val age: Int?,
    val gender: String?,
    val profile_picture: String?,
    val profile_completed: Boolean
)