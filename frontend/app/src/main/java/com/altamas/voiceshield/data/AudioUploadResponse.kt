package com.altamas.voiceshield.data

data class AudioUploadResponse(
    val filename: String,
    val content_type: String,
    val size_bytes: Int,
    val duration_seconds: Double,
    val prediction: Prediction,
    val analysis: Analysis,
    val message: String
)

data class Prediction(
    val prediction_id: String,
    val global: GlobalPrediction,
    val segments: List<Segment>,
    val model: String,
    val processing_time: Double,
    val audio_duration: Double,
    val warnings: List<String>
)

data class GlobalPrediction(
    val confidence: Double,
    val result: String,
    val reason: String?,
    val score: Double
)

data class Segment(
    val start: Double,
    val end: Double,
    val confidence: Double,
    val result: String,
    val scores: List<Double>
)

data class Analysis(
    val status: String,
    val confidence: Double,
    val spoof_probability: Double,
    val message: String
)