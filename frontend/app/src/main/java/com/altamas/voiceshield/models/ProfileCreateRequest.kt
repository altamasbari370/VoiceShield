package com.altamas.voiceshield.models

data class ProfileCreateRequest(
    val name: String,
    val age: Int,
    val gender: String,
    val profile_picture: String? = null
)