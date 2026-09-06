package com.altamas.voiceshield.models

data class HistoryResponse(
    val id: Int,
    val caller_name: String?,
    val caller_number: String?,
    val status: String,
    val confidence: Double,
    val spoof_probability: Double,
    val duration_seconds: Double,
    val message: String?,
    val detected_at: String
)