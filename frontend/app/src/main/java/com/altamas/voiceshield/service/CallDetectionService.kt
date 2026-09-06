package com.altamas.voiceshield.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.altamas.voiceshield.audio.WavEncoder
import com.altamas.voiceshield.data.DetectionStateManager
import com.altamas.voiceshield.data.RetrofitClient
import com.altamas.voiceshield.data.TokenManager
import com.altamas.voiceshield.models.HistoryCreateRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class CallDetectionService : Service() {

    companion object {

        private const val TAG = "VoiceShield"

        const val ACTION_STOP_DETECTION =
            "com.altamas.voiceshield.STOP_DETECTION"

        // Optional caller information.
        const val EXTRA_CALLER_NAME =
            "caller_name"

        const val EXTRA_CALLER_NUMBER =
            "caller_number"

        private const val CHANNEL_ID =
            "voiceshield_call_detection"

        private const val NOTIFICATION_ID = 1001

        private const val WARNING_NOTIFICATION_ID = 1002

        private const val WARNING_CHANNEL_ID =
            "voiceshield_suspicious_voice"

        private val WARNING_VIBRATION_PATTERN = longArrayOf(
            0L,
            1_500L,
            700L,
            1_500L
        )

        // =====================================================
        // AUDIO CONFIGURATION
        // =====================================================

        private const val SAMPLE_RATE = 16_000

        private const val CHANNEL_CONFIG =
            AudioFormat.CHANNEL_IN_MONO

        private const val AUDIO_FORMAT =
            AudioFormat.ENCODING_PCM_16BIT

        // =====================================================
        // BATCH CONFIGURATION
        // =====================================================

        private const val BATCH_DURATION_SECONDS = 5

        private const val BYTES_PER_SAMPLE = 2

        private const val BATCH_SIZE_BYTES =
            SAMPLE_RATE *
                    BATCH_DURATION_SECONDS *
                    BYTES_PER_SAMPLE

        // =====================================================
        // FINAL HISTORY THRESHOLD
        // =====================================================

        /*
         * Average spoof probability >= 0.50
         * means the complete call is considered suspicious.
         */
        private const val SUSPICIOUS_THRESHOLD = 0.50
    }

    private var audioRecord: AudioRecord? = null

    private var recordingThread: Thread? = null

    private var batchNumber = 0

    // =========================================================
    // ALERT STATE
    // =========================================================

    private var previousDetectionStatus: String? = null

    @Volatile
    private var isRecording = false

    // =========================================================
    // CALL INFORMATION
    // =========================================================

    private var callerName: String? = null

    private var callerNumber: String? = null

    // =========================================================
    // CALL DURATION
    // =========================================================

    private var detectionStartTimeMillis: Long = 0L

    // =========================================================
    // HISTORY ACCUMULATION
    // =========================================================

    /*
     * These are kept inside the service itself.
     *
     * This is important because the Dashboard may reset its
     * DetectionStateManager when STOP is pressed.
     *
     * History must still have the complete call statistics.
     */
    private var historyTotalConfidence = 0.0

    private var historyTotalSpoofProbability = 0.0

    private var historyProcessedBatches = 0

    private var historySuspiciousBatches = 0

    // =========================================================
    // UPLOAD THREAD TRACKING
    // =========================================================

    /*
     * Every upload runs on its own thread.
     *
     * Before creating history, we wait for all outstanding
     * upload threads to finish.
     */
    private val uploadThreads =
        mutableListOf<Thread>()

    private val uploadThreadsLock =
        Any()

    // =========================================================
    // SERVICE CREATED
    // =========================================================

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()
        createSuspiciousNotificationChannel()

        val notification = createNotification()

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        Log.d(
            TAG,
            "CallDetectionService created"
        )
    }

    // =========================================================
    // SERVICE START
    // =========================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP_DETECTION) {

            Log.d(
                TAG,
                "🛑 Stop detection command received"
            )

            stopRecording()

            stopForeground(
                STOP_FOREGROUND_REMOVE
            )

            stopSelf()

            return START_NOT_STICKY
        }

        Log.d(
            TAG,
            "Starting microphone capture"
        )

        // =====================================================
        // NEW DETECTION SESSION
        // =====================================================

        resetSessionData()

        callerName =
            intent?.getStringExtra(
                EXTRA_CALLER_NAME
            )

        callerNumber =
            intent?.getStringExtra(
                EXTRA_CALLER_NUMBER
            )

        Log.d(
            TAG,
            "Caller name: $callerName"
        )

        Log.d(
            TAG,
            "Caller number: $callerNumber"
        )

        cancelSuspiciousNotification()

        startRecording()

        return START_NOT_STICKY
    }

    // =========================================================
    // RESET SESSION DATA
    // =========================================================

    private fun resetSessionData() {

        batchNumber = 0

        previousDetectionStatus = null

        historyTotalConfidence = 0.0

        historyTotalSpoofProbability = 0.0

        historyProcessedBatches = 0

        historySuspiciousBatches = 0

        detectionStartTimeMillis = 0L

        /*
         * Reset the dashboard state when a completely new
         * detection session begins.
         */
        DetectionStateManager.reset()

        Log.d(
            TAG,
            "🔄 New detection session initialized"
        )
    }

    // =========================================================
    // START RECORDING
    // =========================================================

    private fun startRecording() {

        if (isRecording) {

            Log.d(
                TAG,
                "Recording already running"
            )

            return
        }

        // -----------------------------------------------------
        // MICROPHONE PERMISSION
        // -----------------------------------------------------

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Log.e(
                TAG,
                "❌ RECORD_AUDIO permission not granted"
            )

            stopSelf()

            return
        }

        // -----------------------------------------------------
        // BUFFER SIZE
        // -----------------------------------------------------

        val minBufferSize =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

        if (
            minBufferSize == AudioRecord.ERROR ||
            minBufferSize == AudioRecord.ERROR_BAD_VALUE
        ) {

            Log.e(
                TAG,
                "❌ Invalid audio configuration"
            )

            return
        }

        val bufferSize =
            maxOf(
                minBufferSize * 2,
                4096
            )

        // -----------------------------------------------------
        // CREATE AUDIO RECORD
        // -----------------------------------------------------

        try {

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (
                audioRecord?.state !=
                AudioRecord.STATE_INITIALIZED
            ) {

                Log.e(
                    TAG,
                    "❌ AudioRecord initialization failed"
                )

                audioRecord?.release()

                audioRecord = null

                return
            }

            // -------------------------------------------------
            // START MICROPHONE
            // -------------------------------------------------

            audioRecord?.startRecording()

            isRecording = true

            detectionStartTimeMillis =
                System.currentTimeMillis()

            Log.d(
                TAG,
                "🎙️ Microphone recording started"
            )

            // -------------------------------------------------
            // RECORDING THREAD
            // -------------------------------------------------

            recordingThread = Thread {

                val readBuffer =
                    ByteArray(bufferSize)

                val batchBuffer =
                    ByteArrayOutputStream(
                        BATCH_SIZE_BYTES
                    )

                while (isRecording) {

                    val bytesRead =
                        audioRecord?.read(
                            readBuffer,
                            0,
                            readBuffer.size
                        ) ?: 0

                    if (bytesRead > 0) {

                        batchBuffer.write(
                            readBuffer,
                            0,
                            bytesRead
                        )

                        // =====================================
                        // BATCH READY
                        // =====================================

                        if (
                            batchBuffer.size()
                            >= BATCH_SIZE_BYTES
                        ) {

                            val pcmData =
                                batchBuffer.toByteArray()

                            Log.d(
                                TAG,
                                "📦 5-second batch ready: " +
                                        "${pcmData.size} bytes"
                            )

                            // =================================
                            // PCM → WAV
                            // =================================

                            val wavData =
                                WavEncoder.pcmToWav(
                                    pcmData,
                                    SAMPLE_RATE,
                                    1.toShort(),
                                    16.toShort()
                                )

                            Log.d(
                                TAG,
                                "🎵 WAV created: " +
                                        "${wavData.size} bytes"
                            )

                            // =================================
                            // BATCH NUMBER
                            // =================================

                            batchNumber++

                            // =================================
                            // UPLOAD
                            // =================================

                            uploadWav(
                                wavData,
                                batchNumber
                            )

                            // =================================
                            // RESET BATCH BUFFER
                            // =================================

                            batchBuffer.reset()
                        }
                    }
                }

                Log.d(
                    TAG,
                    "🎙️ Recording thread stopped"
                )

            }.apply {
                start()
            }

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "❌ Microphone permission denied",
                e
            )

            stopSelf()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "❌ Failed to start recording",
                e
            )

            stopSelf()
        }
    }

    // =========================================================
    // UPLOAD WAV
    // =========================================================

    private fun uploadWav(
        wavData: ByteArray,
        batchNumber: Int
    ) {

        val uploadThread =
            Thread {

                try {

                    Log.d(
                        TAG,
                        "🚀 Uploading batch #$batchNumber"
                    )

                    val requestBody =
                        wavData.toRequestBody(
                            "audio/wav".toMediaType()
                        )

                    val multipartBody =
                        MultipartBody.Part.createFormData(
                            "file",
                            "voiceshield_batch_${batchNumber}.wav",
                            requestBody
                        )

                    val response =
                        runBlocking {

                            RetrofitClient.api.uploadAudio(
                                multipartBody
                            )
                        }

                    Log.d(
                        TAG,
                        "✅ Backend response received for batch #$batchNumber"
                    )

                    // =================================================
                    // READ BACKEND ANALYSIS
                    // =================================================

                    val analysis =
                        response.analysis

                    Log.d(
                        TAG,
                        "Batch #$batchNumber status: ${analysis.status}"
                    )

                    Log.d(
                        TAG,
                        "Batch #$batchNumber confidence: ${analysis.confidence}"
                    )

                    Log.d(
                        TAG,
                        "Batch #$batchNumber spoof probability: " +
                                "${analysis.spoof_probability}"
                    )

                    Log.d(
                        TAG,
                        "Batch #$batchNumber message: ${analysis.message}"
                    )

                    // =================================================
                    // DERIVE CURRENT BATCH COUNTS
                    // =================================================

                    val isSuspicious =
                        analysis.status.equals(
                            "SUSPICIOUS",
                            ignoreCase = true
                        )

                    val suspiciousChunks =
                        if (isSuspicious) {
                            1
                        } else {
                            0
                        }

                    val totalChunks = 1

                    // =================================================
                    // UPDATE DASHBOARD
                    // =================================================

                    DetectionStateManager.updateFromBackend(

                        status = analysis.status,

                        confidence = analysis.confidence,

                        suspiciousChunks =
                            suspiciousChunks,

                        totalChunks =
                            totalChunks
                    )

                    // =================================================
                    // ACCUMULATE HISTORY DATA
                    // =================================================

                    synchronized(this@CallDetectionService) {

                        historyTotalConfidence +=
                            analysis.confidence

                        historyTotalSpoofProbability +=
                            analysis.spoof_probability

                        historyProcessedBatches++

                        if (isSuspicious) {

                            historySuspiciousBatches++
                        }
                    }

                    // =================================================
                    // ALSO UPDATE DETECTION STATE ACCUMULATOR
                    // =================================================

                    DetectionStateManager.addChunkResult(
                        confidence =
                            analysis.confidence,
                        spoofProbability =
                            analysis.spoof_probability
                    )

                    // =================================================
                    // FAKE / SUSPICIOUS VOICE ALERT
                    // =================================================

                    handleDetectionAlert(
                        status = analysis.status
                    )

                    Log.d(
                        TAG,
                        "📊 Batch #$batchNumber processed"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "❌ Audio upload failed for batch #$batchNumber",
                        e
                    )

                } finally {

                    synchronized(
                        uploadThreadsLock
                    ) {

                        uploadThreads.remove(
                            Thread.currentThread()
                        )
                    }
                }
            }

        synchronized(uploadThreadsLock) {

            uploadThreads.add(
                uploadThread
            )
        }

        uploadThread.start()
    }

    // =========================================================
    // WAIT FOR UPLOADS
    // =========================================================

    private fun waitForPendingUploads() {

        Log.d(
            TAG,
            "⏳ Waiting for pending audio uploads..."
        )

        while (true) {

            val threadsToWait: List<Thread>

            synchronized(uploadThreadsLock) {

                if (uploadThreads.isEmpty()) {
                    break
                }

                threadsToWait =
                    uploadThreads.toList()
            }

            for (thread in threadsToWait) {

                try {

                    thread.join()

                } catch (e: InterruptedException) {

                    Thread.currentThread().interrupt()

                    Log.e(
                        TAG,
                        "❌ Interrupted while waiting for uploads",
                        e
                    )

                    return
                }
            }
        }

        Log.d(
            TAG,
            "✅ All pending uploads completed"
        )
    }

    // =========================================================
    // CREATE FINAL HISTORY
    // =========================================================

    private fun createFinalHistory() {

        /*
         * No successful backend batches means there is nothing
         * meaningful to store.
         */
        if (historyProcessedBatches == 0) {

            Log.d(
                TAG,
                "⚠️ No completed detection batches. History not created."
            )

            return
        }

        val averageConfidence =
            historyTotalConfidence /
                    historyProcessedBatches

        val averageSpoofProbability =
            historyTotalSpoofProbability /
                    historyProcessedBatches

        // =====================================================
        // FINAL CALL STATUS
        // =====================================================

        val finalStatus =
            if (
                averageSpoofProbability >=
                SUSPICIOUS_THRESHOLD
            ) {
                "SUSPICIOUS"
            } else {
                "GENUINE"
            }

        val durationSeconds =
            if (detectionStartTimeMillis > 0L) {

                (
                        System.currentTimeMillis() -
                                detectionStartTimeMillis
                        ).toDouble() / 1000.0

            } else {
                historyProcessedBatches *
                        BATCH_DURATION_SECONDS.toDouble()
            }

        val message =
            if (finalStatus == "SUSPICIOUS") {

                "Suspicious voice detected"

            } else {

                "Genuine voice detected"
            }

        Log.d(
            TAG,
            "================================================="
        )

        Log.d(
            TAG,
            "📊 FINAL CALL ANALYSIS"
        )

        Log.d(
            TAG,
            "Processed batches: $historyProcessedBatches"
        )

        Log.d(
            TAG,
            "Suspicious batches: $historySuspiciousBatches"
        )

        Log.d(
            TAG,
            "Average confidence: $averageConfidence"
        )

        Log.d(
            TAG,
            "Average spoof probability: $averageSpoofProbability"
        )

        Log.d(
            TAG,
            "Final status: $finalStatus"
        )

        Log.d(
            TAG,
            "Duration: $durationSeconds seconds"
        )

        Log.d(
            TAG,
            "Caller name: $callerName"
        )

        Log.d(
            TAG,
            "Caller number: $callerNumber"
        )

        Log.d(
            TAG,
            "================================================="
        )

        // =====================================================
        // GET JWT
        // =====================================================

        val tokenManager =
            TokenManager(applicationContext)

        val token =
            tokenManager.getToken()

        if (token.isNullOrBlank()) {

            Log.e(
                TAG,
                "❌ No authentication token. Cannot save history."
            )

            return
        }

        // =====================================================
        // CREATE REQUEST
        // =====================================================

        val historyRequest =
            HistoryCreateRequest(

                caller_name =
                    callerName,

                caller_number =
                    callerNumber,

                status =
                    finalStatus,

                confidence =
                    averageConfidence,

                spoof_probability =
                    averageSpoofProbability,

                duration_seconds =
                    durationSeconds,

                message =
                    message
            )

        // =====================================================
        // POST ONE HISTORY RECORD
        // =====================================================

        try {

            Log.d(
                TAG,
                "🚀 Saving final call history..."
            )

            val response =
                runBlocking {

                    RetrofitClient.api.createHistory(
                        token =
                            "Bearer $token",
                        request =
                            historyRequest
                    )
                }

            if (response.isSuccessful) {

                val savedHistory =
                    response.body()

                Log.d(
                    TAG,
                    "✅ Call history saved successfully"
                )

                Log.d(
                    TAG,
                    "History ID: ${savedHistory?.id}"
                )

            } else {

                Log.e(
                    TAG,
                    "❌ Failed to save history"
                )

                Log.e(
                    TAG,
                    "HTTP code: ${response.code()}"

                )

                Log.e(
                    TAG,
                    "Error body: ${response.errorBody()?.string()}"
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "❌ Exception while saving call history",
                e
            )
        }
    }

    // =========================================================
    // STOP RECORDING
    // =========================================================

    private fun stopRecording() {

        /*
         * Prevent the recording thread from creating any more
         * batches.
         */
        isRecording = false

        // =====================================================
        // STOP MICROPHONE
        // =====================================================

        try {

            audioRecord?.stop()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error stopping AudioRecord",
                e
            )
        }

        // =====================================================
        // WAIT FOR RECORDING THREAD
        // =====================================================

        val currentRecordingThread =
            recordingThread

        if (
            currentRecordingThread != null &&
            currentRecordingThread !=
            Thread.currentThread()
        ) {

            try {

                currentRecordingThread.join()

            } catch (e: InterruptedException) {

                Thread.currentThread().interrupt()

                Log.e(
                    TAG,
                    "❌ Interrupted while stopping recording thread",
                    e
                )
            }
        }

        recordingThread = null

        // =====================================================
        // RELEASE AUDIO RECORD
        // =====================================================

        try {

            audioRecord?.release()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error releasing AudioRecord",
                e
            )
        }

        audioRecord = null

        Log.d(
            TAG,
            "🎙️ AudioRecord released"
        )

        // =====================================================
        // WAIT FOR ALL UPLOADS
        // =====================================================

        waitForPendingUploads()

        // =====================================================
        // CREATE ONE FINAL HISTORY RECORD
        // =====================================================

        createFinalHistory()

        // =====================================================
        // RESET ALERT STATE
        // =====================================================

        previousDetectionStatus = null

        cancelSuspiciousNotification()

        // =====================================================
        // RESET DASHBOARD STATE
        // =====================================================

        DetectionStateManager.reset()

        Log.d(
            TAG,
            "🛑 Detection session completely finished"
        )
    }

    // =========================================================
    // SERVICE DESTROYED
    // =========================================================

    override fun onDestroy() {

        stopRecording()

        Log.d(
            TAG,
            "🛑 CallDetectionService destroyed"
        )

        super.onDestroy()
    }

    // =========================================================
    // BIND
    // =========================================================

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "VoiceShield Call Detection",
                NotificationManager.IMPORTANCE_LOW
            )

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    // =========================================================
    // SUSPICIOUS VOICE NOTIFICATION CHANNEL
    // =========================================================

    private fun createSuspiciousNotificationChannel() {

        val channel =
            NotificationChannel(
                WARNING_CHANNEL_ID,
                "Suspicious Voice Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    "Alerts when VoiceShield detects a suspicious or fake voice."

                enableVibration(false)
            }

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    // =========================================================
    // SUSPICIOUS VOICE ALERT LOGIC
    // =========================================================

    private fun handleDetectionAlert(
        status: String
    ) {

        val isSuspicious =
            status.equals(
                "SUSPICIOUS",
                ignoreCase = true
            )

        val isGenuine =
            status.equals(
                "GENUINE",
                ignoreCase = true
            ) ||
                    status.equals(
                        "REAL",
                        ignoreCase = true
                    )

        val wasSuspicious =
            previousDetectionStatus.equals(
                "SUSPICIOUS",
                ignoreCase = true
            )

        when {

            isSuspicious && !wasSuspicious -> {

                showSuspiciousNotification()

                vibrateSuspiciousAlert()
            }

            isSuspicious -> {

                showSuspiciousNotification()
            }

            isGenuine -> {

                cancelSuspiciousNotification()
            }
        }

        previousDetectionStatus =
            when {

                isSuspicious ->
                    "SUSPICIOUS"

                isGenuine ->
                    "GENUINE"

                else ->
                    status
            }
    }

    // =========================================================
    // SHOW SUSPICIOUS NOTIFICATION
    // =========================================================

    private fun showSuspiciousNotification() {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Log.w(
                TAG,
                "⚠️ POST_NOTIFICATIONS permission not granted"
            )

            return
        }

        val notification =
            Notification.Builder(
                this,
                WARNING_CHANNEL_ID
            )
                .setContentTitle(
                    "⚠️ Suspicious Voice Detected"
                )
                .setContentText(
                    "This voice may be AI-generated or cloned."
                )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_alert
                )
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .build()

        manager.notify(
            WARNING_NOTIFICATION_ID,
            notification
        )
    }

    // =========================================================
    // CANCEL SUSPICIOUS NOTIFICATION
    // =========================================================

    private fun cancelSuspiciousNotification() {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.cancel(
            WARNING_NOTIFICATION_ID
        )
    }

    // =========================================================
    // VIBRATION
    // =========================================================

    private fun vibrateSuspiciousAlert() {

        try {

            val amplitudes =
                intArrayOf(
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                    0,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                val vibratorManager =
                    getSystemService(
                        VibratorManager::class.java
                    )

                vibratorManager
                    .defaultVibrator
                    .vibrate(
                        VibrationEffect.createWaveform(
                            WARNING_VIBRATION_PATTERN,
                            amplitudes,
                            -1
                        )
                    )

            } else {

                @Suppress("DEPRECATION")
                val vibrator =
                    getSystemService(
                        VIBRATOR_SERVICE
                    ) as Vibrator

                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        WARNING_VIBRATION_PATTERN,
                        amplitudes,
                        -1
                    )
                )
            }

            Log.d(
                TAG,
                "📳 Suspicious voice alert vibration started"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "❌ Failed to vibrate suspicious voice alert",
                e
            )
        }
    }

    // =========================================================
    // MAIN NOTIFICATION
    // =========================================================

    private fun createNotification(): Notification {

        return Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle(
                "VoiceShield"
            )
            .setContentText(
                "Call protection is active"
            )
            .setSmallIcon(
                android.R.drawable.ic_lock_lock
            )
            .setOngoing(true)
            .build()
    }
}