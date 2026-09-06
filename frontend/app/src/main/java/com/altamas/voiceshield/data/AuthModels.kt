package com.altamas.voiceshield.data

data class LoginRequest(
    val email: String,
    val password: String
)
data class RegisterRequest(
    val email: String,
    val password: String
)

data class RegisterResponse(
    val message: String,
    val email: String
)