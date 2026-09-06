package com.altamas.voiceshield.models

data class HistoryCreateRequest(
    val caller_name: String? = null,
    val caller_number: String? = null,
    val status: String,
    val confidence: Double,
    val spoof_probability: Double,
    val duration_seconds: Double,
    val message: String? = null
)