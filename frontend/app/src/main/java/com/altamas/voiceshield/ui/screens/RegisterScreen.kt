package com.altamas.voiceshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.altamas.voiceshield.data.AuthRepository
import com.altamas.voiceshield.data.TokenManager
import com.altamas.voiceshield.ui.theme.ShieldBlack
import com.altamas.voiceshield.ui.theme.ShieldBlue
import com.altamas.voiceshield.ui.theme.ShieldCyan
import com.altamas.voiceshield.ui.theme.ShieldGreen
import com.altamas.voiceshield.ui.theme.ShieldPurple
import com.altamas.voiceshield.ui.theme.ShieldTextSecondary
import kotlinx.coroutines.launch


@Composable
fun RegisterScreen(
    onRegistrationSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {

    // =========================================================
    // CONTEXT
    // =========================================================

    val context = LocalContext.current

    // =========================================================
    // AUTH REPOSITORY
    // =========================================================

    val tokenManager = remember {
        TokenManager(context)
    }

    val authRepository = remember {
        AuthRepository(tokenManager)
    }

    val coroutineScope = rememberCoroutineScope()

    // =========================================================
    // FORM STATE
    // =========================================================

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var registerError by remember {
        mutableStateOf<String?>(null)
    }

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
                modifier = Modifier.height(14.dp)
            )

            // =================================================
            // BACK BUTTON
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBackToLogin,
                    enabled = !isLoading
                ) {

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to login",
                        tint = ShieldBlack
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            // =================================================
            // REGISTER ICON
            // =================================================

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(
                        Color(0xFFF1F7FF)
                    )
                    .border(
                        width = 1.5.dp,
                        color = ShieldBlue.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = ShieldCyan.copy(alpha = 0.30f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Create account",
                        tint = ShieldBlue,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(15.dp)
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
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Shield",
                    color = ShieldCyan,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = "CREATE YOUR ACCOUNT",
                color = ShieldBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // =================================================
            // REGISTER CARD
            // =================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(24.dp)
                    )
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ShieldBlue.copy(alpha = 0.35f),
                                Color(0xFFE0E7F0),
                                ShieldCyan.copy(alpha = 0.35f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp)
            ) {

                // =================================================
                // HEADING
                // =================================================

                Text(
                    text = "Create Account",
                    color = ShieldBlack,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Register to protect your voice identity.",
                    color = ShieldTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                // =================================================
                // EMAIL
                // =================================================

                Text(
                    text = "Email",
                    color = ShieldBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        registerError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
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
                            tint = ShieldBlue
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ShieldCyan,
                        unfocusedBorderColor = Color(0xFFB8C2D1),
                        focusedTextColor = ShieldBlack,
                        unfocusedTextColor = ShieldBlack,
                        cursorColor = ShieldCyan
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                // =================================================
                // PASSWORD
                // =================================================

                Text(
                    text = "Password",
                    color = ShieldBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        registerError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Minimum 8 characters",
                            color = ShieldTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ShieldBlue
                        )
                    },
                    trailingIcon = {

                        IconButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            },
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
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                // =================================================
                // CONFIRM PASSWORD
                // =================================================

                Text(
                    text = "Confirm Password",
                    color = ShieldBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        registerError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Re-enter your password",
                            color = ShieldTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ShieldBlue
                        )
                    },
                    trailingIcon = {

                        IconButton(
                            onClick = {
                                confirmPasswordVisible =
                                    !confirmPasswordVisible
                            },
                            enabled = !isLoading
                        ) {

                            Icon(
                                imageVector =
                                    if (confirmPasswordVisible) {
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
                        if (confirmPasswordVisible) {
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
                    shape = RoundedCornerShape(16.dp)
                )

                // =================================================
                // ERROR
                // =================================================

                if (registerError != null) {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = registerError!!,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                // =================================================
                // REGISTER BUTTON
                // =================================================

                Button(
                    onClick = {

                        when {

                            email.isBlank() -> {

                                registerError =
                                    "Please enter your email"
                            }

                            !android.util.Patterns.EMAIL_ADDRESS
                                .matcher(email.trim())
                                .matches() -> {

                                registerError =
                                    "Please enter a valid email"
                            }

                            password.isBlank() -> {

                                registerError =
                                    "Please enter your password"
                            }

                            password.length < 8 -> {

                                registerError =
                                    "Password must be at least 8 characters"
                            }

                            confirmPassword.isBlank() -> {

                                registerError =
                                    "Please confirm your password"
                            }

                            password != confirmPassword -> {

                                registerError =
                                    "Passwords do not match"
                            }

                            else -> {

                                isLoading = true
                                registerError = null

                                coroutineScope.launch {

                                    val result =
                                        authRepository.register(
                                            email = email.trim(),
                                            password = password
                                        )

                                    isLoading = false

                                    result.onSuccess { response ->

                                        println(
                                            "VoiceShield Register: ${response.message}"
                                        )

                                        onRegistrationSuccess()
                                    }

                                    result.onFailure { error ->

                                        println(
                                            "VoiceShield Register Error: ${error.message}"
                                        )

                                        registerError =
                                            "Registration failed. Email may already exist."
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShieldBlue,
                        disabledContainerColor =
                            ShieldBlue.copy(alpha = 0.5f)
                    )
                ) {

                    if (isLoading) {

                        Text(
                            text = "CREATING ACCOUNT...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                    } else {

                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Text(
                            text = "CREATE ACCOUNT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                // =================================================
                // LOGIN LINK
                // =================================================

                Button(
                    onClick = onBackToLogin,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = ShieldBlue
                    )
                ) {

                    Text(
                        text = "Already have an account? Login",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // =================================================
            // SECURITY STATUS
            // =================================================

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            ShieldGreen.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ShieldGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text = "Your account is protected by VoiceShield security",
                    color = ShieldTextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ShieldPurple,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = "Your voice. Your privacy.",
                    color = ShieldTextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }
    }
}