package com.altamas.voiceshield.data

data class PredictionResult(

    val filename: String,

    val spoof_probability: Double,

    val prediction: String,

    val confidence: Double
)