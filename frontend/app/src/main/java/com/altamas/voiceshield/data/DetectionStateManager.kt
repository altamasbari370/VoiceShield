package com.altamas.voiceshield.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DetectionStateManager {

    private val _state = MutableStateFlow(
        DetectionState()
    )

    val state: StateFlow<DetectionState> =
        _state.asStateFlow()

    // =========================================================
    // CALL HISTORY ACCUMULATION
    // =========================================================

    private var totalConfidence = 0.0
    private var totalSpoofProbability = 0.0
    private var processedChunks = 0

    // =========================================================
    // UPDATE UI WITH LATEST BACKEND RESULT
    // =========================================================

    fun updateFromBackend(
        status: String,
        confidence: Double,
        suspiciousChunks: Int,
        totalChunks: Int
    ) {

        _state.value = DetectionState(
            status = status,
            confidence = confidence,
            suspiciousChunks = suspiciousChunks,
            totalChunks = totalChunks
        )
    }

    // =========================================================
    // ADD ONE CHUNK RESULT
    // =========================================================

    fun addChunkResult(
        confidence: Double,
        spoofProbability: Double
    ) {

        totalConfidence += confidence
        totalSpoofProbability += spoofProbability
        processedChunks++
    }

    // =========================================================
    // GET OVERALL AVERAGE CONFIDENCE
    // =========================================================

    fun getAverageConfidence(): Double {

        if (processedChunks == 0) {
            return 0.0
        }

        return totalConfidence / processedChunks
    }

    // =========================================================
    // GET OVERALL AVERAGE SPOOF PROBABILITY
    // =========================================================

    fun getAverageSpoofProbability(): Double {

        if (processedChunks == 0) {
            return 0.0
        }

        return totalSpoofProbability / processedChunks
    }

    // =========================================================
    // GET NUMBER OF PROCESSED CHUNKS
    // =========================================================

    fun getProcessedChunks(): Int {
        return processedChunks
    }

    // =========================================================
    // RESET EVERYTHING
    // =========================================================

    fun reset() {

        totalConfidence = 0.0
        totalSpoofProbability = 0.0
        processedChunks = 0

        _state.value = DetectionState()
    }
}