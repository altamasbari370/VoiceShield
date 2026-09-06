package com.altamas.voiceshield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.altamas.voiceshield.service.CallDetectionOverlayService
import com.altamas.voiceshield.service.CallDetectionService

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "VoiceShield"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        // ---------------------------------------------------------
        // Only handle phone-state broadcasts
        // ---------------------------------------------------------

        if (
            intent.action !=
            TelephonyManager.ACTION_PHONE_STATE_CHANGED
        ) {
            return
        }

        val state =
            intent.getStringExtra(
                TelephonyManager.EXTRA_STATE
            )

        Log.d(
            TAG,
            "📞 PHONE STATE RECEIVED: $state"
        )

        when (state) {

            // =====================================================
            // INCOMING CALL
            // =====================================================

            TelephonyManager.EXTRA_STATE_RINGING -> {

                Log.d(
                    TAG,
                    "📞 Incoming call detected"
                )

                try {

                    val overlayIntent =
                        Intent(
                            context,
                            CallDetectionOverlayService::class.java
                        )

                    /*
                     * Incoming call:
                     * START the foreground overlay service.
                     */
                    ContextCompat.startForegroundService(
                        context,
                        overlayIntent
                    )

                    Log.d(
                        TAG,
                        "🛡️ VoiceShield overlay service start requested"
                    )

                } catch (e: SecurityException) {

                    Log.e(
                        TAG,
                        "❌ SecurityException starting overlay service",
                        e
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "❌ Error starting overlay service",
                        e
                    )
                }
            }

            // =====================================================
            // CALL ANSWERED
            // =====================================================

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {

                Log.d(
                    TAG,
                    "📲 Call is active"
                )

                /*
                 * Detection does NOT automatically start.
                 *
                 * User presses START DETECTION
                 * inside the VoiceShield popup.
                 */
            }

            // =====================================================
            // CALL ENDED
            // =====================================================

            TelephonyManager.EXTRA_STATE_IDLE -> {

                Log.d(
                    TAG,
                    "📵 Call ended"
                )

                // =================================================
                // STOP DETECTION
                // =================================================

                try {

                    val detectionIntent =
                        Intent(
                            context,
                            CallDetectionService::class.java
                        ).apply {

                            action =
                                CallDetectionService
                                    .ACTION_STOP_DETECTION
                        }

                    /*
                     * Send the stop command.
                     *
                     * CallDetectionService is responsible for
                     * its own graceful shutdown/history handling.
                     */
                    ContextCompat.startForegroundService(
                        context,
                        detectionIntent
                    )

                    Log.d(
                        TAG,
                        "🛑 Stop detection action sent"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "❌ Error sending stop detection",
                        e
                    )
                }

                // =================================================
                // REMOVE OVERLAY
                // =================================================

                try {

                    val overlayIntent =
                        Intent(
                            context,
                            CallDetectionOverlayService::class.java
                        ).apply {

                            action =
                                CallDetectionOverlayService
                                    .ACTION_STOP_OVERLAY
                        }

                    /*
                     * We only need to deliver the command.
                     *
                     * The overlay service will remove its own
                     * window and call stopSelf().
                     */
                    context.startService(
                        overlayIntent
                    )

                    Log.d(
                        TAG,
                        "🛑 Stop overlay action sent"
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "❌ Error stopping overlay service",
                        e
                    )
                }
            }
        }
    }
}