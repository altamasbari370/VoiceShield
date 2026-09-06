
package com.altamas.voiceshield.data

data class AnalysisResult(
    val status: String,
    val average_spoof_probability: Double,
    val confidence: Double,
    val suspicious_chunks: Int,
    val total_chunks: Int
)

