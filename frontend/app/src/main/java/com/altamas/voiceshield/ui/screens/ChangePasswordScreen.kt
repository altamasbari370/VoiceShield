package com.altamas.voiceshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altamas.voiceshield.data.RetrofitClient
import com.altamas.voiceshield.data.TokenManager
import com.altamas.voiceshield.models.ChangePasswordRequest
import kotlinx.coroutines.launch

private val ShieldBlue = Color(0xFF2563EB)
private val ShieldPurple = Color(0xFF7C3AED)
private val ShieldGreen = Color(0xFF10B981)
private val ShieldRed = Color(0xFFDC2626)
private val ShieldTextSecondary = Color(0xFF64748B)
private val PageBackground = Color(0xFFF8FAFC)

@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember {
        TokenManager(context.applicationContext)
    }

    val coroutineScope = rememberCoroutineScope()

    var currentPassword by remember {
        mutableStateOf("")
    }

    var newPassword by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var currentPasswordVisible by remember {
        mutableStateOf(false)
    }

    var newPasswordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var successMessage by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {

        // ============================================================
        // TOP BAR
        // ============================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBackClick,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Change Password",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "Secure your VoiceShield account",
                    fontSize = 12.sp,
                    color = ShieldTextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = ShieldPurple,
                modifier = Modifier.size(27.dp)
            )
        }

        // ============================================================
        // CONTENT
        // ============================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Top
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                ShieldPurple.copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ShieldPurple,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "Create a new password",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "Enter your current password and choose a new password for your account. New Password must be different from current password.",
                        fontSize = 13.sp,
                        color = ShieldTextSecondary,
                        lineHeight = 19.sp
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    // =================================================
                    // CURRENT PASSWORD
                    // =================================================

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            errorMessage = null
                            successMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Current password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    currentPasswordVisible =
                                        !currentPasswordVisible
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (currentPasswordVisible)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                    contentDescription =
                                        if (currentPasswordVisible)
                                            "Hide password"
                                        else
                                            "Show password"
                                )
                            }
                        },
                        visualTransformation =
                            if (currentPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    // =================================================
                    // NEW PASSWORD
                    // =================================================

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            errorMessage = null
                            successMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("New password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    newPasswordVisible =
                                        !newPasswordVisible
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (newPasswordVisible)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                    contentDescription =
                                        if (newPasswordVisible)
                                            "Hide password"
                                        else
                                            "Show password"
                                )
                            }
                        },
                        visualTransformation =
                            if (newPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    // =================================================
                    // CONFIRM PASSWORD
                    // =================================================

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                            successMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Confirm new password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    confirmPasswordVisible =
                                        !confirmPasswordVisible
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (confirmPasswordVisible)
                                            Icons.Default.VisibilityOff
                                        else
                                            Icons.Default.Visibility,
                                    contentDescription =
                                        if (confirmPasswordVisible)
                                            "Hide password"
                                        else
                                            "Show password"
                                )
                            }
                        },
                        visualTransformation =
                            if (confirmPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Password must be at least 8 characters.",
                        fontSize = 12.sp,
                        color = ShieldTextSecondary
                    )

                    // =================================================
                    // ERROR
                    // =================================================

                    if (errorMessage != null) {
                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Text(
                            text = errorMessage!!,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ShieldRed
                        )
                    }

                    // =================================================
                    // SUCCESS
                    // =================================================

                    if (successMessage != null) {
                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ShieldGreen,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier = Modifier.size(6.dp)
                            )

                            Text(
                                text = successMessage!!,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = ShieldGreen
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(22.dp)
                    )

                    // =================================================
                    // CHANGE PASSWORD BUTTON
                    // =================================================

                    Button(
                        onClick = {

                            errorMessage = null
                            successMessage = null

                            when {
                                currentPassword.isBlank() -> {
                                    errorMessage =
                                        "Please enter your current password."
                                    return@Button
                                }

                                newPassword.length < 8 -> {
                                    errorMessage =
                                        "New password must be at least 8 characters."
                                    return@Button
                                }

                                confirmPassword.isBlank() -> {
                                    errorMessage =
                                        "Please confirm your new password."
                                    return@Button
                                }

                                newPassword != confirmPassword -> {
                                    errorMessage =
                                        "New passwords do not match."
                                    return@Button
                                }

                                currentPassword == newPassword -> {
                                    errorMessage =
                                        "New password must be different from current password."
                                    return@Button
                                }
                            }

                            val token = tokenManager.getToken()

                            if (token.isNullOrBlank()) {
                                errorMessage =
                                    "Authentication session expired. Please login again."
                                return@Button
                            }

                            isLoading = true

                            coroutineScope.launch {

                                try {
                                    println("CHANGE PASSWORD TOKEN: $token")

                                    val response =
                                        RetrofitClient.api.changePassword(
                                            token = "Bearer $token",
                                            request =
                                                ChangePasswordRequest(
                                                    current_password =
                                                        currentPassword,
                                                    new_password =
                                                        newPassword
                                                )
                                        )

                                    if (response.isSuccessful) {

                                        successMessage =
                                            response.body()?.message
                                                ?: "Password changed successfully."

                                        currentPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""

                                    } else {
                                        val errorBody = response.errorBody()?.string()

                                        println("CHANGE PASSWORD HTTP CODE: ${response.code()}")
                                        println("CHANGE PASSWORD ERROR: $errorBody")

                                        errorMessage = if (!errorBody.isNullOrBlank()) {
                                            "Server error (${response.code()}): $errorBody"
                                        } else {
                                            "Unable to change password. Server returned ${response.code()}."
                                        }
                                    }

                                } catch (e: Exception) {

                                    println(
                                        "VoiceShield Change Password Error: ${e.message}"
                                    )

                                    errorMessage =
                                        "Unable to connect to the server. Please try again."

                                } finally {

                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShieldBlue
                        )
                    ) {

                        if (isLoading) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )

                        } else {

                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(
                                modifier = Modifier.size(8.dp)
                            )

                            Text(
                                text = "CHANGE PASSWORD",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}