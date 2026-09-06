package com.altamas.voiceshield.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altamas.voiceshield.data.AuthRepository
import com.altamas.voiceshield.data.TokenManager
import com.altamas.voiceshield.ui.theme.ShieldBlack
import com.altamas.voiceshield.ui.theme.ShieldBlue
import com.altamas.voiceshield.ui.theme.ShieldCyan
import com.altamas.voiceshield.ui.theme.ShieldGreen
import com.altamas.voiceshield.ui.theme.ShieldPurple
import com.altamas.voiceshield.ui.theme.ShieldTextSecondary
import com.altamas.voiceshield.ui.theme.ShieldWhite
import kotlinx.coroutines.launch


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onGuestClick: () -> Unit = {}
) {

    // =========================================================
    // CONTEXT
    // =========================================================

    val context = LocalContext.current

    // =========================================================
    // TOKEN MANAGER
    // =========================================================

    val tokenManager = remember {
        TokenManager(context)
    }

    // =========================================================
    // LOGIN STATE
    // =========================================================

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var loginError by remember {
        mutableStateOf<String?>(null)
    }

    // =========================================================
    // AUTH REPOSITORY
    // =========================================================

    val authRepository = remember {
        AuthRepository(tokenManager)
    }

    val coroutineScope = rememberCoroutineScope()


    // =========================================================
    // MAIN SCREEN
    // =========================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier = Modifier.height(20.dp)
            )


            // =================================================
            // 3D SHIELD
            // =================================================

            Shield3D()


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // =================================================
            // LOGO
            // =================================================

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Voice",
                    color = ShieldBlack,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Shield",
                    color = ShieldCyan,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }


            Spacer(
                modifier = Modifier.height(4.dp)
            )


            Text(
                text = "AI-POWERED VOICE PROTECTION",
                color = ShieldCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // =================================================
            // DECORATIVE LINE
            // =================================================

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(1.dp)
                        .background(
                            ShieldBlue.copy(alpha = 0.7f)
                        )
                )

                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ShieldCyan,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(18.dp)
                )

                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(1.dp)
                        .background(
                            ShieldBlue.copy(alpha = 0.7f)
                        )
                )
            }


            Spacer(
                modifier = Modifier.height(22.dp)
            )


            // =================================================
            // LOGIN CARD
            // =================================================

            LoginCard(
                email = email,
                password = password,
                passwordVisible = passwordVisible,
                isLoading = isLoading,
                loginError = loginError,

                onEmailChange = {
                    email = it
                    loginError = null
                },

                onPasswordChange = {
                    password = it
                    loginError = null
                },

                onPasswordVisibilityChange = {
                    passwordVisible = !passwordVisible
                },

                onLoginClick = {

                    if (email.isBlank()) {
                        loginError = "Please enter your email"
                        return@LoginCard
                    }

                    if (password.isBlank()) {
                        loginError = "Please enter your password"
                        return@LoginCard
                    }

                    if (password.length < 8) {
                        loginError = "Password must be at least 8 characters"
                        return@LoginCard
                    }

                    isLoading = true
                    loginError = null

                    coroutineScope.launch {

                        val result = authRepository.login(
                            email = email.trim(),
                            password = password
                        )

                        isLoading = false

                        result.onSuccess { response ->

                            println(
                                "VoiceShield Login: ${response.message}"
                            )

                            println(
                                "JWT token saved successfully"
                            )

                            // Login successful
                            onLoginSuccess()
                        }

                        result.onFailure { error ->

                            println(
                                "VoiceShield Login Error: ${error.message}"
                            )

                            loginError = "Invalid email or password"
                        }
                    }
                },
                onRegisterClick = onRegisterClick,
                onGuestClick = onGuestClick
            )


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // SECURITY FOOTER
            // =================================================

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    bottom = 18.dp
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ShieldTextSecondary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text = "End-to-end encrypted",
                    color = ShieldTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}


// =============================================================
// 3D SHIELD
// =============================================================

@Composable
private fun Shield3D() {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "shield_animation"
        )


    // =========================================================
    // FLOATING MOVEMENT
    // =========================================================

    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )


    // =========================================================
    // TILT
    // =========================================================

    val tilt by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )


    Box(
        modifier = Modifier
            .size(220.dp)
            .offset(y = floatY.dp),
        contentAlignment = Alignment.Center
    ) {

        // =====================================================
        // SHIELD GLOW
        // =====================================================

        Box(
            modifier = Modifier
                .size(170.dp)
                .blur(45.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ShieldCyan.copy(alpha = 0.65f),
                            ShieldBlue.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )


        Canvas(
            modifier = Modifier.size(200.dp)
        ) {

            val w = size.width
            val h = size.height

            val centerX = w / 2f
            val centerY = h * 0.43f


            // =================================================
            // 3D EXTRUSION
            // =================================================

            val depth = 10.dp.toPx()


            drawShield(
                w = w,
                h = h,
                offsetX = depth,
                offsetY = depth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF080F20),
                        Color(0xFF02040A)
                    )
                )
            )


            for (i in 7 downTo 1) {

                val d = i * 1.7.dp.toPx()

                drawShield(
                    w = w,
                    h = h,
                    offsetX = d,
                    offsetY = d,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF132B55),
                            Color(0xFF071225),
                            Color(0xFF020711)
                        )
                    )
                )
            }


            // =================================================
            // MAIN CHROME SHIELD
            // =================================================

            drawShield(
                w = w,
                h = h,
                offsetX = 0f,
                offsetY = 0f,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFF8EA7C9),
                        Color(0xFF172C4B),
                        Color(0xFFBFD9FF),
                        Color(0xFF3D5A82)
                    )
                ),
                strokeWidth = 5.dp.toPx()
            )


            // =================================================
            // INNER DARK PANEL
            // =================================================

            drawShield(
                w = w,
                h = h,
                offsetX = 0f,
                offsetY = 0f,
                inset = 11.dp.toPx(),
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF173A70),
                        Color(0xFF07172F),
                        Color(0xFF020712),
                        Color(0xFF0C2245)
                    )
                )
            )


            // =================================================
            // NEON RIM
            // =================================================

            drawShield(
                w = w,
                h = h,
                offsetX = 0f,
                offsetY = 0f,
                inset = 17.dp.toPx(),
                brush = Brush.linearGradient(
                    colors = listOf(
                        ShieldCyan,
                        ShieldBlue,
                        ShieldPurple,
                        ShieldCyan
                    )
                ),
                strokeWidth = 3.dp.toPx()
            )


            // =================================================
            // MICROPHONE GLOW
            // =================================================

            drawCircle(
                color = ShieldCyan.copy(alpha = 0.12f),
                radius = 48.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    centerX,
                    centerY
                )
            )


            // =================================================
            // MICROPHONE BODY
            // =================================================

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        ShieldCyan,
                        ShieldBlue,
                        Color(0xFF183D79)
                    )
                ),
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - 17.dp.toPx(),
                    centerY - 38.dp.toPx()
                ),
                size = androidx.compose.ui.geometry.Size(
                    34.dp.toPx(),
                    70.dp.toPx()
                ),
                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        18.dp.toPx()
                    )
            )


            // =================================================
            // MICROPHONE DARK FACE
            // =================================================

            drawRoundRect(
                color = Color(0xFF071225),
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - 11.dp.toPx(),
                    centerY - 31.dp.toPx()
                ),
                size = androidx.compose.ui.geometry.Size(
                    22.dp.toPx(),
                    55.dp.toPx()
                ),
                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        12.dp.toPx()
                    )
            )


            // =================================================
            // MICROPHONE HIGHLIGHT
            // =================================================

            drawLine(
                color = Color.White.copy(alpha = 0.75f),
                start = androidx.compose.ui.geometry.Offset(
                    centerX - 6.dp.toPx(),
                    centerY - 25.dp.toPx()
                ),
                end = androidx.compose.ui.geometry.Offset(
                    centerX - 6.dp.toPx(),
                    centerY - 5.dp.toPx()
                ),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )


            // =================================================
            // U-SHAPED HOLDER
            // =================================================

            drawArc(
                color = ShieldCyan,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - 35.dp.toPx(),
                    centerY - 5.dp.toPx()
                ),
                size = androidx.compose.ui.geometry.Size(
                    70.dp.toPx(),
                    70.dp.toPx()
                ),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )


            // =================================================
            // MICROPHONE STAND
            // =================================================

            drawLine(
                color = ShieldCyan,
                start = androidx.compose.ui.geometry.Offset(
                    centerX,
                    centerY + 29.dp.toPx()
                ),
                end = androidx.compose.ui.geometry.Offset(
                    centerX,
                    centerY + 55.dp.toPx()
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )


            drawLine(
                color = ShieldCyan,
                start = androidx.compose.ui.geometry.Offset(
                    centerX - 18.dp.toPx(),
                    centerY + 55.dp.toPx()
                ),
                end = androidx.compose.ui.geometry.Offset(
                    centerX + 18.dp.toPx(),
                    centerY + 55.dp.toPx()
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )


            // =================================================
            // AUDIO WAVEFORM
            // =================================================

            val waveform = listOf(
                13f,
                25f,
                38f,
                20f,
                13f,
                20f,
                38f,
                25f,
                13f
            )


            waveform.forEachIndexed { index, heightValue ->

                val x =
                    centerX -
                            63.dp.toPx() +
                            index * 15.dp.toPx()

                val y1 =
                    centerY -
                            heightValue.dp.toPx() / 2

                val y2 =
                    centerY +
                            heightValue.dp.toPx() / 2


                drawLine(
                    color = ShieldCyan,
                    start = androidx.compose.ui.geometry.Offset(
                        x,
                        y1
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        x,
                        y2
                    ),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }


            // =================================================
            // SPECULAR HIGHLIGHT
            // =================================================

            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = androidx.compose.ui.geometry.Offset(
                    w * 0.27f,
                    h * 0.17f
                ),
                end = androidx.compose.ui.geometry.Offset(
                    w * 0.46f,
                    h * 0.08f
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }


    }
}


// =============================================================
// LOGIN CARD
// =============================================================

@Composable
private fun LoginCard(
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    loginError: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityChange: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onGuestClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ShieldBlue.copy(alpha = 0.9f),
                        ShieldPurple.copy(alpha = 0.4f),
                        ShieldCyan.copy(alpha = 0.9f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(22.dp)
    ) {

        // =====================================================
        // EMAIL
        // =====================================================

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = ShieldBlue,
                modifier = Modifier.size(22.dp)
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = "Email",
                color = ShieldBlack,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }


        Spacer(
            modifier = Modifier.height(9.dp)
        )


        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            placeholder = {
                Text(
                    text = "Enter your email",
                    color = ShieldTextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = ShieldTextSecondary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ShieldCyan,
                unfocusedBorderColor = Color(0xFFB8C2D1),
                focusedTextColor = ShieldBlack,
                unfocusedTextColor = ShieldBlack,
                cursorColor = ShieldCyan
            ),
            shape = RoundedCornerShape(18.dp)
        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // PASSWORD
        // =====================================================

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = ShieldBlue,
                modifier = Modifier.size(22.dp)
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = "Password",
                color = ShieldBlack,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }


        Spacer(
            modifier = Modifier.height(9.dp)
        )


        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            placeholder = {
                Text(
                    text = "Enter your password",
                    color = ShieldTextSecondary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ShieldTextSecondary
                )
            },
            trailingIcon = {

                IconButton(
                    onClick = onPasswordVisibilityChange,
                    enabled = !isLoading
                ) {

                    Icon(
                        imageVector =
                            if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                        contentDescription =
                            "Toggle password visibility",
                        tint = ShieldTextSecondary
                    )
                }
            },
            visualTransformation =
                if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ShieldCyan,
                unfocusedBorderColor = Color(0xFFB8C2D1),
                focusedTextColor = ShieldBlack,
                unfocusedTextColor = ShieldBlack,
                cursorColor = ShieldCyan
            ),
            shape = RoundedCornerShape(18.dp)
        )


        Spacer(
            modifier = Modifier.height(8.dp)
        )


        // =====================================================
        // FORGOT PASSWORD
        // =====================================================

        Text(
            text = "Forgot Password?",
            modifier = Modifier
                .align(Alignment.End)
                .pointerInput(Unit) {
                    detectTapGestures {
                        // Forgot password will be implemented later
                    }
                },
            color = ShieldBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )


        // =====================================================
        // ERROR MESSAGE
        // =====================================================

        if (loginError != null) {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = loginError,
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // LOGIN BUTTON
        // =====================================================

        Button(
            onClick = onLoginClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ShieldBlue,
                disabledContainerColor =
                    ShieldBlue.copy(alpha = 0.5f)
            )
        ) {

            if (isLoading) {

                Text(
                    text = "LOGGING IN...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                )

            } else {

                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp)
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Text(
                    text = "LOGIN",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // OR DIVIDER
        // =====================================================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Color(0xFF30394A)
                    )
            )

            Text(
                text = "OR",
                color = ShieldTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(
                    horizontal = 14.dp
                )
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Color(0xFF30394A)
                    )
            )
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // GUEST BUTTON
        // =====================================================

        Button(
            onClick = onGuestClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = ShieldBlack
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                ShieldPurple
            )
        ) {

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(23.dp)
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Text(
                text = "Continue as Guest",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(27.dp)
            )
        }


        Spacer(
            modifier = Modifier.height(20.dp)
        )


        // =====================================================
        // SECURITY STATUS
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    ShieldGreen.copy(alpha = 0.07f)
                )
                .border(
                    1.dp,
                    ShieldGreen.copy(alpha = 0.25f),
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        ShieldGreen.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ShieldGreen,
                    modifier = Modifier.size(24.dp)
                )
            }


            Spacer(
                modifier = Modifier.width(12.dp)
            )


            Column {

                Text(
                    text = "Your voice. Your privacy. Our priority.",
                    color = ShieldBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = "Protected by Advanced AI Security",
                    color = ShieldGreen,
                    fontSize = 12.sp
                )
            }
        }
    }
}


// =============================================================
// SHIELD DRAWING FUNCTION
// =============================================================

private fun DrawScope.drawShield(
    w: Float,
    h: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    inset: Float = 0f,
    brush: Brush,
    strokeWidth: Float = 0f
) {

    val path = Path()

    val left = inset + offsetX
    val top = inset + offsetY
    val right = w - inset + offsetX
    val bottom = h - inset + offsetY

    val centerX = w / 2f + offsetX


    path.moveTo(
        centerX,
        top
    )


    path.cubicTo(
        right * 0.95f,
        top + h * 0.10f,
        right * 0.92f,
        top + h * 0.30f,
        right * 0.88f,
        top + h * 0.48f
    )


    path.cubicTo(
        right * 0.82f,
        top + h * 0.72f,
        right * 0.67f,
        top + h * 0.86f,
        centerX,
        bottom
    )


    path.cubicTo(
        left + w * 0.33f,
        top + h * 0.86f,
        left + w * 0.18f,
        top + h * 0.72f,
        left + w * 0.12f,
        top + h * 0.48f
    )


    path.cubicTo(
        left + w * 0.08f,
        top + h * 0.30f,
        left + w * 0.05f,
        top + h * 0.10f,
        centerX,
        top
    )


    path.close()


    if (strokeWidth > 0f) {

        drawPath(
            path = path,
            brush = brush,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )

    } else {

        drawPath(
            path = path,
            brush = brush
        )
    }
}
