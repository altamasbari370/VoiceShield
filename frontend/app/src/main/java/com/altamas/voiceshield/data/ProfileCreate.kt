package com.altamas.voiceshield.data

data class ProfileCreate(
    val name: String,
    val age: Int,
    val gender: String,
    val profile_picture: String? = null
)