package com.altamas.voiceshield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.altamas.voiceshield.R
import com.altamas.voiceshield.data.DetectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CallDetectionOverlayService : Service() {

    companion object {

        private const val TAG = "VoiceShield"

        private const val CHANNEL_ID =
            "voiceshield_call_overlay"

        private const val NOTIFICATION_ID =
            2001

        /*
         * Public because CallStateReceiver also needs
         * to send this action when the phone call ends.
         */
        const val ACTION_STOP_OVERLAY =
            "com.altamas.voiceshield.STOP_OVERLAY"

        // Full popup size
        private const val FULL_WIDTH_DP = 330

        // Minimized bubble size
        private const val BUBBLE_SIZE_DP = 68
    }

    // =============================================================
    // OVERLAY
    // =============================================================

    private var overlayView: View? = null

    private var bubbleView: View? = null

    private var windowManager: WindowManager? = null

    private var windowParams: WindowManager.LayoutParams? = null

    private var isMinimized = false

    // =============================================================
    // UI
    // =============================================================

    private var resultText: TextView? = null

    private var confidenceText: TextView? = null

    private var startButton: Button? = null

    private var stopButton: Button? = null

    // =============================================================
    // DRAGGING
    // =============================================================

    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var initialWindowX = 0
    private var initialWindowY = 0

    private var isDragging = false

    private var downTime = 0L

    // =============================================================
    // COROUTINE
    // =============================================================

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main.immediate
        )

    // =============================================================
    // SERVICE CREATED
    // =============================================================

    override fun onCreate() {

        super.onCreate()

        android.util.Log.d(
            TAG,
            "🛡️ CallDetectionOverlayService created"
        )

        createNotificationChannel()

        startOverlayForegroundService()

        /*
         * IMPORTANT:
         *
         * Do NOT call showOverlay() here.
         *
         * onCreate() can also happen when Android creates
         * the service for ACTION_STOP_OVERLAY.
         *
         * The popup is therefore created only inside
         * onStartCommand() for a normal start.
         */
        observeDetectionState()
    }

    // =============================================================
    // FOREGROUND SERVICE
    // =============================================================

    private fun startOverlayForegroundService() {

        val notification =
            createNotification()

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )

            } else {

                @Suppress("DEPRECATION")

                startForeground(
                    NOTIFICATION_ID,
                    notification
                )
            }

            android.util.Log.d(
                TAG,
                "✅ Popup foreground service started"
            )

        } catch (e: Exception) {

            android.util.Log.e(
                TAG,
                "❌ Failed to start popup foreground service",
                e
            )

            stopSelf()
        }
    }

    // =============================================================
    // START COMMAND
    // =============================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        android.util.Log.d(
            TAG,
            "🛡️ Overlay service onStartCommand: ${intent?.action}"
        )

        /*
         * =========================================================
         * STOP OVERLAY
         * =========================================================
         */

        if (intent?.action == ACTION_STOP_OVERLAY) {

            android.util.Log.d(
                TAG,
                "🛑 Stop overlay action received"
            )

            /*
             * Remove both the full popup and minimized bubble.
             */
            removeOverlay()

            /*
             * Remove foreground notification.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }

            stopSelf()

            return START_NOT_STICKY
        }

        /*
         * =========================================================
         * NORMAL START
         * =========================================================
         *
         * This is the important part.
         *
         * CallStateReceiver starts this service when an
         * incoming call is detected.
         *
         * The popup must be created here.
         */

        android.util.Log.d(
            TAG,
            "📞 Normal overlay start - showing popup"
        )

        showOverlay()

        return START_NOT_STICKY
    }

    // =============================================================
    // SHOW OVERLAY
    // =============================================================

    private fun showOverlay() {

        if (!Settings.canDrawOverlays(this)) {

            android.util.Log.e(
                TAG,
                "❌ SYSTEM_ALERT_WINDOW permission not granted"
            )

            stopSelf()

            return
        }

        /*
         * Prevent duplicate popup windows.
         */
        if (overlayView != null || bubbleView != null) {

            android.util.Log.d(
                TAG,
                "ℹ️ Popup already exists"
            )

            return
        }

        android.util.Log.d(
            TAG,
            "🛡️ Creating VoiceShield floating popup"
        )

        windowManager =
            getSystemService(
                WINDOW_SERVICE
            ) as WindowManager

        // =========================================================
        // ROOT
        // =========================================================

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(20),
                    dp(16),
                    dp(20),
                    dp(20)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.rgb(
                                8,
                                18,
                                45
                            )
                        )

                        cornerRadius =
                            dp(24).toFloat()

                        setStroke(
                            dp(1),
                            Color.rgb(
                                45,
                                111,
                                190
                            )
                        )
                    }
            }

        // =========================================================
        // HEADER
        // =========================================================

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        // ---------------------------------------------------------
        // Header title
        // ---------------------------------------------------------

        val headerTitle =
            TextView(this).apply {

                text =
                    "VoiceShield"

                textSize =
                    20f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        header.addView(
            headerTitle,
            LinearLayout.LayoutParams(
                0,
                dp(42)
            ).apply {

                weight = 1f
            }
        )

        // ---------------------------------------------------------
        // MINIMIZE BUTTON
        // ---------------------------------------------------------

        val minimizeButton =
            TextView(this).apply {

                text =
                    "−"

                textSize =
                    28f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.rgb(
                                25,
                                50,
                                90
                            )
                        )

                        cornerRadius =
                            dp(10).toFloat()
                    }

                setOnClickListener {

                    android.util.Log.d(
                        TAG,
                        "➖ Minimizing VoiceShield popup"
                    )

                    minimizeOverlay()
                }
            }

        header.addView(
            minimizeButton,
            LinearLayout.LayoutParams(
                dp(42),
                dp(42)
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )

        // =========================================================
        // LOGO
        // =========================================================

        val logo =
            ImageView(this).apply {

                setImageResource(
                    R.drawable.voiceshield_logo_transparent
                )

                scaleType =
                    ImageView.ScaleType.FIT_CENTER
            }

        root.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(115),
                dp(115)
            ).apply {

                gravity =
                    Gravity.CENTER_HORIZONTAL

                topMargin =
                    dp(4)
            }
        )

        // =========================================================
        // INCOMING CALL
        // =========================================================

        val incoming =
            TextView(this).apply {

                text =
                    "📞 Incoming Call"

                textSize =
                    15f

                setTextColor(
                    Color.rgb(
                        180,
                        205,
                        235
                    )
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }

        root.addView(
            incoming,
            LinearLayout.LayoutParams(
                -1,
                dp(36)
            )
        )

        // =========================================================
        // RESULT CONTAINER
        // =========================================================

        val resultContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(12),
                    dp(8),
                    dp(12),
                    dp(8)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.rgb(
                                17,
                                34,
                                67
                            )
                        )

                        cornerRadius =
                            dp(14).toFloat()
                    }
            }

        // =========================================================
        // RESULT TEXT
        // =========================================================

        resultText =
            TextView(this).apply {

                text =
                    "READY"

                textSize =
                    17f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        // =========================================================
        // CONFIDENCE
        // =========================================================

        confidenceText =
            TextView(this).apply {

                text =
                    "Waiting for detection"

                textSize =
                    13f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }

        resultContainer.addView(
            resultText,
            LinearLayout.LayoutParams(
                -1,
                dp(30)
            )
        )

        resultContainer.addView(
            confidenceText,
            LinearLayout.LayoutParams(
                -1,
                dp(28)
            )
        )

        root.addView(
            resultContainer,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        // =========================================================
        // START BUTTON
        // =========================================================

        startButton =
            createButton(
                "START DETECTION"
            )

        root.addView(
            startButton,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {

                topMargin =
                    dp(14)
            }
        )

        // =========================================================
        // STOP BUTTON
        // =========================================================

        stopButton =
            createButton(
                "STOP DETECTION"
            )

        root.addView(
            stopButton,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {

                topMargin =
                    dp(9)
            }
        )

        // =========================================================
        // START CLICK
        // =========================================================

        startButton?.setOnClickListener {

            android.util.Log.d(
                TAG,
                "▶️ START DETECTION pressed"
            )

            startDetection()

            startButton?.isEnabled =
                false

            stopButton?.isEnabled =
                true

            resultText?.text =
                "ANALYZING VOICE"

            resultText?.setTextColor(
                Color.WHITE
            )

            confidenceText?.text =
                "Detection started..."
        }

        // =========================================================
        // STOP CLICK
        // =========================================================

        stopButton?.setOnClickListener {

            android.util.Log.d(
                TAG,
                "⏹️ STOP DETECTION pressed"
            )

            stopDetection()

            startButton?.isEnabled =
                true

            stopButton?.isEnabled =
                false

            resultText?.text =
                "DETECTION STOPPED"

            resultText?.setTextColor(
                Color.WHITE
            )

            confidenceText?.text =
                "Press START to analyze again"
        }

        // =========================================================
        // INITIAL BUTTON STATE
        // =========================================================

        stopButton?.isEnabled =
            false

        // =========================================================
        // SAVE VIEW
        // =========================================================

        overlayView =
            root

        // =========================================================
        // WINDOW TYPE
        // =========================================================

        val windowType =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            } else {

                @Suppress("DEPRECATION")

                WindowManager.LayoutParams.TYPE_PHONE
            }

        // =========================================================
        // WINDOW PARAMETERS
        // =========================================================

        val params =
            WindowManager.LayoutParams(
                dp(FULL_WIDTH_DP),
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,

                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,

                PixelFormat.TRANSLUCENT
            )

        /*
         * Start near the center of the screen.
         *
         * Because we use Gravity.TOP | Gravity.START,
         * params.x and params.y become the actual position.
         */

        params.gravity =
            Gravity.TOP or Gravity.START

        params.x =
            (
                    resources.displayMetrics.widthPixels -
                            dp(FULL_WIDTH_DP)
                    ) / 2

        params.y =
            resources.displayMetrics.heightPixels / 4

        windowParams =
            params

        // =========================================================
        // DRAG SUPPORT
        // =========================================================

        setupDrag(root)

        // =========================================================
        // ADD WINDOW
        // =========================================================

        try {

            windowManager?.addView(
                root,
                params
            )

            android.util.Log.d(
                TAG,
                "✅ VoiceShield floating popup displayed"
            )

        } catch (e: Exception) {

            android.util.Log.e(
                TAG,
                "❌ Failed to add popup window",
                e
            )

            overlayView =
                null

            windowParams =
                null

            stopSelf()
        }
    }

    // =============================================================
    // DRAG SUPPORT
    // =============================================================

    private fun setupDrag(
        view: View
    ) {

        view.setOnTouchListener { _, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    val params =
                        windowParams
                            ?: return@setOnTouchListener false

                    initialWindowX =
                        params.x

                    initialWindowY =
                        params.y

                    initialTouchX =
                        event.rawX

                    initialTouchY =
                        event.rawY

                    downTime =
                        System.currentTimeMillis()

                    isDragging =
                        false

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val params =
                        windowParams
                            ?: return@setOnTouchListener false

                    val deltaX =
                        (
                                event.rawX -
                                        initialTouchX
                                ).toInt()

                    val deltaY =
                        (
                                event.rawY -
                                        initialTouchY
                                ).toInt()

                    if (
                        kotlin.math.abs(deltaX) > dp(5) ||
                        kotlin.math.abs(deltaY) > dp(5)
                    ) {

                        isDragging =
                            true
                    }

                    params.x =
                        initialWindowX +
                                deltaX

                    params.y =
                        initialWindowY +
                                deltaY

                    // Keep popup inside screen.

                    val screenWidth =
                        resources.displayMetrics.widthPixels

                    val screenHeight =
                        resources.displayMetrics.heightPixels

                    val popupWidth =
                        view.width

                    val popupHeight =
                        view.height

                    params.x =
                        params.x.coerceIn(
                            0,
                            (
                                    screenWidth -
                                            popupWidth
                                    ).coerceAtLeast(0)
                        )

                    params.y =
                        params.y.coerceIn(
                            0,
                            (
                                    screenHeight -
                                            popupHeight
                                    ).coerceAtLeast(0)
                        )

                    try {

                        windowManager?.updateViewLayout(
                            view,
                            params
                        )

                    } catch (e: Exception) {

                        android.util.Log.e(
                            TAG,
                            "❌ Failed to move popup",
                            e
                        )
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    /*
                     * If user simply tapped instead of dragging,
                     * nothing needs to happen.
                     */

                    isDragging =
                        false

                    true
                }

                else -> false
            }
        }
    }

    // =============================================================
    // MINIMIZE
    // =============================================================

    private fun minimizeOverlay() {

        if (isMinimized) {
            return
        }

        val oldView =
            overlayView
                ?: return

        val params =
            windowParams
                ?: return

        try {

            /*
             * Remove large window first.
             */

            windowManager?.removeView(
                oldView
            )

            // =====================================================
            // CREATE SMALL BUBBLE
            // =====================================================

            val bubble =
                ImageView(this).apply {

                    setImageResource(
                        R.drawable.voiceshield_logo_transparent
                    )

                    scaleType =
                        ImageView.ScaleType.CENTER_INSIDE

                    setPadding(
                        dp(8),
                        dp(8),
                        dp(8),
                        dp(8)
                    )

                    background =
                        GradientDrawable().apply {

                            shape =
                                GradientDrawable.OVAL

                            setColor(
                                Color.rgb(
                                    8,
                                    18,
                                    45
                                )
                            )

                            setStroke(
                                dp(2),
                                Color.rgb(
                                    45,
                                    111,
                                    190
                                )
                            )
                        }
                }

            bubbleView =
                bubble

            /*
             * Change window size to bubble.
             */

            params.width =
                dp(BUBBLE_SIZE_DP)

            params.height =
                dp(BUBBLE_SIZE_DP)

            /*
             * Keep bubble inside screen bounds.
             */

            val screenWidth =
                resources.displayMetrics.widthPixels

            val screenHeight =
                resources.displayMetrics.heightPixels

            params.x =
                params.x.coerceIn(
                    0,
                    (
                            screenWidth -
                                    dp(BUBBLE_SIZE_DP)
                            ).coerceAtLeast(0)
                )

            params.y =
                params.y.coerceIn(
                    0,
                    (
                            screenHeight -
                                    dp(BUBBLE_SIZE_DP)
                            ).coerceAtLeast(0)
                )

            windowManager?.addView(
                bubble,
                params
            )

            /*
             * Bubble can be dragged.
             */

            setupDrag(
                bubble
            )

            /*
             * Tap restores full popup.
             */

            bubble.setOnClickListener {

                if (!isDragging) {

                    android.util.Log.d(
                        TAG,
                        "🔼 Restoring VoiceShield popup"
                    )

                    restoreOverlay()
                }
            }

            /*
             * setupDrag consumes touch events, so use
             * dedicated bubble touch handling.
             */

            setupBubbleTouch(
                bubble
            )

            isMinimized =
                true

            /*
             * The full view no longer exists.
             */
            overlayView =
                null

            android.util.Log.d(
                TAG,
                "➖ VoiceShield popup minimized"
            )

        } catch (e: Exception) {

            android.util.Log.e(
                TAG,
                "❌ Failed to minimize popup",
                e
            )
        }
    }

    // =============================================================
    // BUBBLE TOUCH
    // =============================================================

    private fun setupBubbleTouch(
        bubble: View
    ) {

        bubble.setOnTouchListener { _, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    val params =
                        windowParams
                            ?: return@setOnTouchListener false

                    initialWindowX =
                        params.x

                    initialWindowY =
                        params.y

                    initialTouchX =
                        event.rawX

                    initialTouchY =
                        event.rawY

                    downTime =
                        System.currentTimeMillis()

                    isDragging =
                        false

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val params =
                        windowParams
                            ?: return@setOnTouchListener false

                    val deltaX =
                        (
                                event.rawX -
                                        initialTouchX
                                ).toInt()

                    val deltaY =
                        (
                                event.rawY -
                                        initialTouchY
                                ).toInt()

                    if (
                        kotlin.math.abs(deltaX) > dp(5) ||
                        kotlin.math.abs(deltaY) > dp(5)
                    ) {

                        isDragging =
                            true
                    }

                    params.x =
                        initialWindowX +
                                deltaX

                    params.y =
                        initialWindowY +
                                deltaY

                    val screenWidth =
                        resources.displayMetrics.widthPixels

                    val screenHeight =
                        resources.displayMetrics.heightPixels

                    params.x =
                        params.x.coerceIn(
                            0,
                            (
                                    screenWidth -
                                            dp(BUBBLE_SIZE_DP)
                                    ).coerceAtLeast(0)
                        )

                    params.y =
                        params.y.coerceIn(
                            0,
                            (
                                    screenHeight -
                                            dp(BUBBLE_SIZE_DP)
                                    ).coerceAtLeast(0)
                        )

                    try {

                        windowManager?.updateViewLayout(
                            bubble,
                            params
                        )

                    } catch (e: Exception) {

                        android.util.Log.e(
                            TAG,
                            "❌ Failed to move bubble",
                            e
                        )
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    val wasDragging =
                        isDragging

                    isDragging =
                        false

                    if (!wasDragging) {

                        restoreOverlay()
                    }

                    true
                }

                else -> false
            }
        }
    }

    // =============================================================
    // RESTORE
    // =============================================================

    private fun restoreOverlay() {

        if (!isMinimized) {
            return
        }

        val bubble =
            bubbleView
                ?: return

        val params =
            windowParams
                ?: return

        try {

            /*
             * Remove bubble.
             */

            windowManager?.removeView(
                bubble
            )

            bubbleView =
                null

            // =====================================================
            // RESTORE FULL WINDOW
            // =====================================================

            val root =
                createFullOverlayView()

            overlayView =
                root

            params.width =
                dp(FULL_WIDTH_DP)

            params.height =
                WindowManager.LayoutParams.WRAP_CONTENT

            /*
             * Keep bubble's current position.
             */

            val screenWidth =
                resources.displayMetrics.widthPixels

            params.x =
                params.x.coerceIn(
                    0,
                    (
                            screenWidth -
                                    dp(FULL_WIDTH_DP)
                            ).coerceAtLeast(0)
                )

            windowManager?.addView(
                root,
                params
            )

            setupDrag(
                root
            )

            isMinimized =
                false

            android.util.Log.d(
                TAG,
                "🔼 VoiceShield popup restored"
            )

        } catch (e: Exception) {

            android.util.Log.e(
                TAG,
                "❌ Failed to restore popup",
                e
            )
        }
    }

    // =============================================================
    // CREATE FULL OVERLAY VIEW
    // =============================================================

    private fun createFullOverlayView(): View {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(20),
                    dp(16),
                    dp(20),
                    dp(20)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.rgb(
                                8,
                                18,
                                45
                            )
                        )

                        cornerRadius =
                            dp(24).toFloat()

                        setStroke(
                            dp(1),
                            Color.rgb(
                                45,
                                111,
                                190
                            )
                        )
                    }
            }

        // =========================================================
        // HEADER
        // =========================================================

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val headerTitle =
            TextView(this).apply {

                text =
                    "VoiceShield"

                textSize =
                    20f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        header.addView(
            headerTitle,
            LinearLayout.LayoutParams(
                0,
                dp(42)
            ).apply {

                weight = 1f
            }
        )

        val minimizeButton =
            TextView(this).apply {

                text =
                    "−"

                textSize =
                    28f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.rgb(
                                25,
                                50,
                                90
                            )
                        )

                        cornerRadius =
                            dp(10).toFloat()
                    }

                setOnClickListener {

                    minimizeOverlay()
                }
            }

        header.addView(
            minimizeButton,
            LinearLayout.LayoutParams(
                dp(42),
                dp(42)
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )

        // =========================================================
        // LOGO
        // =========================================================

        val logo =
            ImageView(this).apply {

                setImageResource(
                    R.drawable.voiceshield_logo_transparent
                )

                scaleType =
                    ImageView.ScaleType.FIT_CENTER
            }

        root.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(115),
                dp(115)
            ).apply {

                gravity =
                    Gravity.CENTER_HORIZONTAL

                topMargin =
                    dp(4)
            }
        )

        // =========================================================
        // INCOMING CALL
        // =========================================================

        val incoming =
            TextView(this).apply {

                text =
                    "📞 Incoming Call"

                textSize =
                    15f

                setTextColor(
                    Color.rgb(
                        180,
                        205,
                        235
                    )
                )

                gravity =
                    Gravity.CENTER
            }

        root.addView(
            incoming,
            LinearLayout.LayoutParams(
                -1,
                dp(36)
            )
        )

        // =========================================================
        // RESULT CONTAINER
        // =========================================================

        val resultContainer =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(12),
                    dp(8),
                    dp(12),
                    dp(8)
                )

                background =
                    GradientDrawable().apply {

                        setColor(
                            Color.rgb(
                                17,
                                34,
                                67
                            )
                        )

                        cornerRadius =
                            dp(14).toFloat()
                    }
            }

        resultText =
            TextView(this).apply {

                text =
                    "READY"

                textSize =
                    17f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        confidenceText =
            TextView(this).apply {

                text =
                    "Waiting for detection"

                textSize =
                    13f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER
            }

        resultContainer.addView(
            resultText,
            LinearLayout.LayoutParams(
                -1,
                dp(30)
            )
        )

        resultContainer.addView(
            confidenceText,
            LinearLayout.LayoutParams(
                -1,
                dp(28)
            )
        )

        root.addView(
            resultContainer,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        // =========================================================
        // START BUTTON
        // =========================================================

        startButton =
            createButton(
                "START DETECTION"
            )

        root.addView(
            startButton,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {

                topMargin =
                    dp(14)
            }
        )

        // =========================================================
        // STOP BUTTON
        // =========================================================

        stopButton =
            createButton(
                "STOP DETECTION"
            )

        root.addView(
            stopButton,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {

                topMargin =
                    dp(9)
            }
        )

        // =========================================================
        // BUTTON STATE
        // =========================================================

        stopButton?.isEnabled =
            false

        // =========================================================
        // BUTTON LISTENERS
        // =========================================================

        startButton?.setOnClickListener {

            android.util.Log.d(
                TAG,
                "▶️ START DETECTION pressed"
            )

            startDetection()

            startButton?.isEnabled =
                false

            stopButton?.isEnabled =
                true

            resultText?.text =
                "ANALYZING VOICE"

            resultText?.setTextColor(
                Color.WHITE
            )

            confidenceText?.text =
                "Detection started..."
        }

        stopButton?.setOnClickListener {

            android.util.Log.d(
                TAG,
                "⏹️ STOP DETECTION pressed"
            )

            stopDetection()

            startButton?.isEnabled =
                true

            stopButton?.isEnabled =
                false

            resultText?.text =
                "DETECTION STOPPED"

            resultText?.setTextColor(
                Color.WHITE
            )

            confidenceText?.text =
                "Press START to analyze again"
        }

        /*
         * Restore current detection state if popup was
         * minimized while detection was running.
         */

        val currentState =
            DetectionStateManager.state.value

        when (
            currentState.status.uppercase()
        ) {

            "SUSPICIOUS" -> {

                resultText?.text =
                    "⚠ POSSIBLE VOICE CLONE"

                resultText?.setTextColor(
                    Color.rgb(
                        255,
                        80,
                        80
                    )
                )

                confidenceText?.text =
                    "Confidence: ${
                        String.format(
                            "%.1f",
                            currentState.confidence
                        )
                    }%"

                startButton?.isEnabled =
                    false

                stopButton?.isEnabled =
                    true
            }

            "GENUINE" -> {

                resultText?.text =
                    "✓ VOICE APPEARS GENUINE"

                resultText?.setTextColor(
                    Color.rgb(
                        80,
                        230,
                        150
                    )
                )

                confidenceText?.text =
                    "Confidence: ${
                        String.format(
                            "%.1f",
                            currentState.confidence
                        )
                    }%"

                startButton?.isEnabled =
                    false

                stopButton?.isEnabled =
                    true
            }
        }

        return root
    }

    // =============================================================
    // BUTTON CREATOR
    // =============================================================

    private fun createButton(
        textValue: String
    ): Button {

        return Button(this).apply {

            text =
                textValue

            textSize =
                13f

            setTextColor(
                Color.WHITE
            )

            isAllCaps =
                false

            background =
                GradientDrawable().apply {

                    setColor(
                        Color.rgb(
                            25,
                            93,
                            190
                        )
                    )

                    cornerRadius =
                        dp(12).toFloat()
                }

            setPadding(
                dp(10),
                0,
                dp(10),
                0
            )
        }
    }

    // =============================================================
    // START DETECTION
    // =============================================================

    private fun startDetection() {

        try {

            val intent =
                Intent(
                    this,
                    CallDetectionService::class.java
                )

            ContextCompat.startForegroundService(
                this,
                intent
            )

            android.util.Log.d(
                TAG,
                "🎙️ CallDetectionService started"
            )

        } catch (e: Exception) {

            android.util.Log.e(
                TAG,
                "❌ Failed to start detection service",
                e
            )

            startButton?.isEnabled =
                true

            stopButton?.isEnabled =
                false
        }
    }

    // =============================================================
    // STOP DETECTION
    // =============================================================

    private fun stopDetection() {

        try {

            /*
             * IMPORTANT:
             *
             * Do NOT directly call stopService().
             *
             * CallDetectionService needs to receive its
             * ACTION_STOP_DETECTION so it can perform its
             * own graceful shutdown and history handling.
             */

            val intent =
                Intent(
                    this,
                    CallDetectionService::class.java
                ).apply {
                    action =
                        CallDetectionService.ACTION_STOP_DETECTION
                }

            ContextCompat.startForegroundService(
                this,
                intent
            )

            android.util.Log.d(
                TAG,
                "🛑 Stop detection action sent"
            )

        } catch (e: Exception) {

            android.util.Log.e(
                TAG,
                "❌ Failed to stop detection service",
                e
            )
        }
    }

    // =============================================================
    // OBSERVE DETECTION STATE
    // =============================================================

    private fun observeDetectionState() {

        serviceScope.launch {

            DetectionStateManager.state.collectLatest { state ->

                when (
                    state.status.uppercase()
                ) {

                    // =================================================
                    // SUSPICIOUS
                    // =================================================

                    "SUSPICIOUS" -> {

                        resultText?.text =
                            "⚠ POSSIBLE VOICE CLONE"

                        resultText?.setTextColor(
                            Color.rgb(
                                255,
                                80,
                                80
                            )
                        )

                        confidenceText?.text =
                            "Confidence: ${
                                String.format(
                                    "%.1f",
                                    state.confidence
                                )
                            }%"

                        startButton?.isEnabled =
                            false

                        stopButton?.isEnabled =
                            true

                        android.util.Log.d(
                            TAG,
                            "🚨 Popup result: SUSPICIOUS ${state.confidence}%"
                        )
                    }

                    // =================================================
                    // GENUINE
                    // =================================================

                    "GENUINE" -> {

                        resultText?.text =
                            "✓ VOICE APPEARS GENUINE"

                        resultText?.setTextColor(
                            Color.rgb(
                                80,
                                230,
                                150
                            )
                        )

                        confidenceText?.text =
                            "Confidence: ${
                                String.format(
                                    "%.1f",
                                    state.confidence
                                )
                            }%"

                        startButton?.isEnabled =
                            false

                        stopButton?.isEnabled =
                            true

                        android.util.Log.d(
                            TAG,
                            "✅ Popup result: GENUINE ${state.confidence}%"
                        )
                    }

                    else -> {

                        if (
                            startButton?.isEnabled == false
                        ) {

                            resultText?.text =
                                "ANALYZING VOICE"

                            resultText?.setTextColor(
                                Color.WHITE
                            )

                            confidenceText?.text =
                                "Waiting for detection..."
                        }
                    }
                }
            }
        }
    }

    // =============================================================
    // REMOVE OVERLAY
    // =============================================================

    private fun removeOverlay() {

        try {

            /*
             * Remove full popup if present.
             */

            overlayView?.let {

                try {
                    windowManager?.removeView(it)
                } catch (_: Exception) {
                    // View may already have been removed.
                }
            }

            /*
             * Remove minimized bubble if present.
             */

            bubbleView?.let {

                try {
                    windowManager?.removeView(it)
                } catch (_: Exception) {
                    // View may already have been removed.
                }
            }

            android.util.Log.d(
                TAG,
                "🛑 VoiceShield popup removed"
            )

        } catch (e: Exception) {

            android.util.Log.e(
                TAG,
                "⚠️ Error removing popup",
                e
            )
        }

        /*
         * Clear all references so a future incoming call
         * can create a completely new popup.
         */

        overlayView =
            null

        bubbleView =
            null

        windowParams =
            null

        isMinimized =
            false

        resultText =
            null

        confidenceText =
            null

        startButton =
            null

        stopButton =
            null
    }

    // =============================================================
    // NOTIFICATION CHANNEL
    // =============================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "VoiceShield Call Protection",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "VoiceShield incoming-call protection"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    // =============================================================
    // NOTIFICATION
    // =============================================================

    private fun createNotification(): Notification {

        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setSmallIcon(
                R.mipmap.ic_launcher
            )
            .setContentTitle(
                "VoiceShield"
            )
            .setContentText(
                "Incoming-call protection is active"
            )
            .setOngoing(
                true
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .build()
    }

    // =============================================================
    // DP
    // =============================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }

    // =============================================================
    // DESTROY
    // =============================================================

    override fun onDestroy() {

        android.util.Log.d(
            TAG,
            "🛑 CallDetectionOverlayService destroyed"
        )

        /*
         * Cancel only the overlay service's coroutine.
         *
         * IMPORTANT:
         * Do NOT stop CallDetectionService here.
         *
         * The overlay service and detection service have
         * separate lifecycles.
         */
        serviceScope.cancel()

        removeOverlay()

        super.onDestroy()
    }

    // =============================================================
    // BIND
    // =============================================================

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}