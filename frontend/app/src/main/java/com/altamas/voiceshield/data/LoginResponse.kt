package com.altamas.voiceshield.data

data class LoginResponse(
    val message: String,
    val access_token: String,
    val token_type: String,
    val user_id: Int,
    val email: String
)