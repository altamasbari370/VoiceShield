package com.altamas.voiceshield.models

data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)