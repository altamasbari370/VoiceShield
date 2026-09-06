package com.altamas.voiceshield.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

import com.altamas.voiceshield.data.DetectionStateManager
import com.altamas.voiceshield.service.CallDetectionService


// =============================================================
// VOICESHIELD COLORS
// =============================================================

private val DarkCard = Color(0xFF0C1424)

private val Blue = Color(0xFF2979FF)
private val Cyan = Color(0xFF00E5FF)
private val Green = Color(0xFF00E676)
private val Purple = Color(0xFF9C27B0)
private val Red = Color(0xFFFF1744)

private val SecondaryText = Color(0xFF8A94A6)
private val White = Color.White


// =============================================================
// PROFILE IMAGE DECODER
// =============================================================

private fun decodeProfilePicture(
    profilePicture: String?
): Bitmap? {

    if (profilePicture.isNullOrBlank()) {
        return null
    }

    return try {

        // Handles both:
        // data:image/jpeg;base64,....
        //
        // and:
        // raw-base64-string

        val base64Data =
            if (profilePicture.contains(",")) {
                profilePicture.substringAfter(",")
            } else {
                profilePicture
            }

        val imageBytes =
            Base64.decode(
                base64Data,
                Base64.DEFAULT
            )

        BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size
        )

    } catch (e: Exception) {

        println(
            "VoiceShield Dashboard Photo Error: ${e.message}"
        )

        null
    }
}


// =============================================================
// DASHBOARD
// =============================================================

@Composable
fun DashboardScreen(
    userName: String = "User",

    // NEW:
    // Profile picture received from backend.
    profilePicture: String? = null,

    onStartDetection: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current


    // =========================================================
    // PROFILE PHOTO
    // =========================================================

    val dashboardProfileBitmap =
        remember(profilePicture) {
            decodeProfilePicture(profilePicture)
        }


    // =========================================================
    // DETECTION STATE
    // =========================================================

    val detectionState by
    DetectionStateManager.state.collectAsState()


    // =========================================================
    // LOCAL DETECTION RUNNING STATE
    // =========================================================

    var isDetectionRunning by remember {
        mutableStateOf(false)
    }


    // =========================================================
    // PERMISSION FLOW
    // =========================================================

    var permissionFlowStarted by remember {
        mutableStateOf(false)
    }


    // =========================================================
    // NOTIFICATION PERMISSION
    // =========================================================

    val notificationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) {

            openOverlayPermission(context)
        }


    // =========================================================
    // MICROPHONE PERMISSION
    // =========================================================

    val microphoneLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                val notificationGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED


                if (!notificationGranted) {

                    notificationLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )

                } else {

                    openOverlayPermission(context)
                }

            } else {

                openOverlayPermission(context)
            }
        }


    // =========================================================
    // PERMISSION FLOW START
    // =========================================================

    LaunchedEffect(Unit) {

        if (!permissionFlowStarted) {

            permissionFlowStarted = true


            val microphoneGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED


            if (!microphoneGranted) {

                microphoneLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )

            } else {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU
                ) {

                    val notificationGranted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED


                    if (!notificationGranted) {

                        notificationLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )

                    } else {

                        openOverlayPermission(context)
                    }

                } else {

                    openOverlayPermission(context)
                }
            }
        }
    }


    // =========================================================
    // MAIN UI
    // =========================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {

            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // TOP BAR
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Welcome back,",
                        color = SecondaryText,
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = userName,
                        color = Color(0xFF111827),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }


                // =================================================
                // PROFILE BUTTON
                // =================================================

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Blue.copy(alpha = 0.15f)
                        )
                        .border(
                            width = 1.dp,
                            color = Blue.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .pointerInput(Unit) {

                            detectTapGestures {

                                onProfileClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {

                    // =================================================
                    // SHOW SAVED PROFILE PHOTO
                    // =================================================

                    if (dashboardProfileBitmap != null) {

                        Image(
                            bitmap =
                                dashboardProfileBitmap
                                    .asImageBitmap(),

                            contentDescription = "Profile",

                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                    } else {

                        // =================================================
                        // FALLBACK PERSON ICON
                        // =================================================

                        Icon(
                            imageVector =
                                Icons.Default.Person,

                            contentDescription =
                                "Profile",

                            tint = Blue,

                            modifier =
                                Modifier.size(25.dp)
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // LIVE DETECTION RESULT
            // =================================================

            val resultStatus =
                detectionState.status.uppercase()


            val resultColor =
                when (resultStatus) {

                    "SUSPICIOUS" -> Red

                    "GENUINE" -> Green

                    else -> SecondaryText
                }


            val resultTitle =
                when (resultStatus) {

                    "SUSPICIOUS" ->
                        "⚠️ Possible Voice Clone"

                    "GENUINE" ->
                        "✓ Voice Appears Genuine"

                    else ->
                        "Waiting for Analysis"
                }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(20.dp)
                    )
                    .background(
                        resultColor.copy(alpha = 0.08f)
                    )
                    .border(
                        width = 1.dp,
                        color = resultColor.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(
                            resultColor.copy(alpha = 0.15f)
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Security,

                        contentDescription = null,

                        tint = resultColor,

                        modifier =
                            Modifier.size(24.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.size(13.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = resultTitle,
                        color = Color(0xFF111827),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )


                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )


                    Text(
                        text =
                            if (
                                resultStatus ==
                                "NO_RESULT"
                            ) {

                                "No voice has been analyzed yet"

                            } else {

                                "Confidence: ${
                                    String.format(
                                        "%.1f",
                                        detectionState.confidence
                                    )
                                }%"
                            },

                        color = resultColor,

                        fontSize = 12.sp
                    )


                    if (
                        detectionState.totalChunks > 0
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )


                        Text(
                            text =
                                "${detectionState.suspiciousChunks} suspicious / " +
                                        "${detectionState.totalChunks} chunks",

                            color =
                                SecondaryText,

                            fontSize = 11.sp
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // SECURITY STATUS
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(20.dp)
                    )
                    .background(
                        Green.copy(alpha = 0.08f)
                    )
                    .border(
                        width = 1.dp,
                        color = Green.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(
                            Green.copy(alpha = 0.15f)
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Shield,

                        contentDescription = null,

                        tint = Green,

                        modifier =
                            Modifier.size(25.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.size(13.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "VoiceShield is active",
                        color = Color(0xFF111827),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )


                    Text(
                        text =
                            if (isDetectionRunning) {

                                "Live voice protection is running"

                            } else {

                                "Your voice protection is ready"
                            },

                        color = Green,

                        fontSize = 12.sp
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(24.dp)
            )


            // =================================================
            // DETECTION CARD
            // =================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(30.dp)
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFEAF3FF),
                                Color(0xFFF7FBFF)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Blue.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(24.dp)
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    // =========================================
                    // DETECTION ICON
                    // =========================================

                    Box(
                        modifier = Modifier
                            .size(85.dp)
                            .clip(CircleShape)
                            .background(
                                Cyan.copy(alpha = 0.10f)
                            )
                            .border(
                                width = 2.dp,
                                color = Cyan.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                if (isDetectionRunning) {

                                    Icons.Default.Mic

                                } else {

                                    Icons.Default.Security
                                },

                            contentDescription = null,

                            tint = Blue,

                            modifier =
                                Modifier.size(42.dp)
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )


                    // =========================================
                    // TITLE
                    // =========================================

                    Text(
                        text = "Voice Clone Detection",

                        color =
                            Color(0xFF111827),

                        fontSize = 22.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )


                    // =========================================
                    // DESCRIPTION
                    // =========================================

                    Text(
                        text =
                            if (isDetectionRunning) {

                                "Voice monitoring is currently active"

                            } else {

                                "Analyze a voice recording for AI-generated speech"
                            },

                        color = SecondaryText,

                        fontSize = 13.sp
                    )


                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )


                    // =================================================
                    // START / STOP DETECTION BUTTON
                    // =================================================

                    Button(
                        onClick = {

                            // =================================================
                            // STOP DETECTION
                            // =================================================

                            if (isDetectionRunning) {

                                /*
                                 * IMPORTANT:
                                 *
                                 * Do NOT use stopService() here.
                                 *
                                 * CallDetectionService needs to receive
                                 * ACTION_STOP_DETECTION so that it can:
                                 *
                                 * 1. Stop recording
                                 * 2. Wait for pending uploads
                                 * 3. Calculate the final result
                                 * 4. Save ONE history record
                                 * 5. Reset DetectionStateManager
                                 * 6. Stop itself
                                 */

                                val stopIntent =
                                    Intent(
                                        context,
                                        CallDetectionService::class.java
                                    ).apply {
                                        action =
                                            CallDetectionService.ACTION_STOP_DETECTION
                                    }

                                context.startService(
                                    stopIntent
                                )

                                /*
                                 * Do NOT reset DetectionStateManager here.
                                 *
                                 * The service needs the accumulated
                                 * detection data to create the final
                                 * history record.
                                 */

                                isDetectionRunning = false

                            }

                            // =================================================
                            // START DETECTION
                            // =================================================

                            else {

                                val intent =
                                    Intent(
                                        context,
                                        CallDetectionService::class.java
                                    )


                                ContextCompat.startForegroundService(
                                    context,
                                    intent
                                )


                                // Immediately update dashboard UI
                                isDetectionRunning = true


                                // Keep existing callback
                                onStartDetection()
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),

                        shape =
                            RoundedCornerShape(17.dp),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (isDetectionRunning) {

                                        Red

                                    } else {

                                        Blue
                                    }
                            )
                    ) {

                        // =================================================
                        // BUTTON ICON
                        // =================================================

                        Icon(
                            imageVector =
                                if (isDetectionRunning) {

                                    Icons.Default.Stop

                                } else {

                                    Icons.Default.Security
                                },

                            contentDescription = null
                        )


                        Spacer(
                            modifier = Modifier.size(9.dp)
                        )


                        // =================================================
                        // BUTTON TEXT
                        // =================================================

                        Text(
                            text =
                                if (isDetectionRunning) {

                                    "STOP DETECTION"

                                } else {

                                    "START DETECTION"
                                },

                            fontSize = 14.sp,

                            fontWeight =
                                FontWeight.Bold,

                            letterSpacing = 1.sp
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(22.dp)
            )


            // =================================================
            // QUICK ACTIONS
            // =================================================

            Text(
                text = "Quick Actions",

                color =
                    Color(0xFF111827),

                fontSize = 17.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                // =================================================
                // HISTORY
                // =================================================

                DashboardAction(
                    icon =
                        Icons.Default.History,

                    title =
                        "History",

                    modifier =
                        Modifier.weight(1f),

                    onClick =
                        onHistoryClick
                )


                // =================================================
                // PROFILE
                // =================================================

                DashboardAction(
                    icon =
                        Icons.Default.Person,

                    title =
                        "Profile",

                    modifier =
                        Modifier.weight(1f),

                    onClick =
                        onProfileClick
                )


                // =================================================
                // LOGOUT
                // =================================================

                DashboardAction(
                    icon =
                        Icons.Default.Logout,

                    title =
                        "Logout",

                    modifier =
                        Modifier.weight(1f),

                    onClick =
                        onLogoutClick
                )
            }


            Spacer(
                modifier = Modifier.weight(1f)
            )


            // =================================================
            // FOOTER
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.GraphicEq,

                    contentDescription = null,

                    tint = Purple,

                    modifier =
                        Modifier.size(17.dp)
                )


                Spacer(
                    modifier = Modifier.size(7.dp)
                )


                Text(
                    text =
                        "AI-powered voice security",

                    color =
                        SecondaryText,

                    fontSize = 11.sp
                )
            }
        }
    }
}


// =============================================================
// OVERLAY PERMISSION
// =============================================================

private fun openOverlayPermission(
    context: Context
) {

    if (!Settings.canDrawOverlays(context)) {

        val intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse(
                    "package:${context.packageName}"
                )
            )

        context.startActivity(intent)
    }
}


// =============================================================
// DASHBOARD ACTION
// =============================================================

@Composable
private fun DashboardAction(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Column(
        modifier = modifier
            .height(90.dp)
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(
                Color(0xFFF4F7FB)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFDCE3ED),
                shape = RoundedCornerShape(18.dp)
            )
            .pointerInput(Unit) {

                detectTapGestures {

                    onClick()
                }
            }
            .padding(10.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Icon(
            imageVector = icon,

            contentDescription =
                title,

            tint = Blue,

            modifier =
                Modifier.size(25.dp)
        )


        Spacer(
            modifier = Modifier.height(7.dp)
        )


        Text(
            text = title,

            color =
                Color(0xFF111827),

            fontSize = 12.sp,

            fontWeight =
                FontWeight.Medium
        )
    }
}