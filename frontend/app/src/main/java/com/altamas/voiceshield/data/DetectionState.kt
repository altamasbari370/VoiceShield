package com.altamas.voiceshield.data

data class DetectionState(
    val status: String = "NO_RESULT",
    val confidence: Double = 0.0,
    val suspiciousChunks: Int = 0,
    val totalChunks: Int = 0
)