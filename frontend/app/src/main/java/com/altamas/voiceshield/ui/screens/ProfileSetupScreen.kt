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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altamas.voiceshield.data.ProfileRepository
import com.altamas.voiceshield.data.TokenManager
import com.altamas.voiceshield.models.ProfileCreateRequest
import com.altamas.voiceshield.ui.theme.ShieldBlack
import com.altamas.voiceshield.ui.theme.ShieldBlue
import com.altamas.voiceshield.ui.theme.ShieldCyan
import com.altamas.voiceshield.ui.theme.ShieldGreen
import com.altamas.voiceshield.ui.theme.ShieldPurple
import com.altamas.voiceshield.ui.theme.ShieldTextSecondary
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onProfileComplete: () -> Unit
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
    // REPOSITORY
    // =========================================================

    val profileRepository = remember {
        ProfileRepository()
    }

    val coroutineScope = rememberCoroutineScope()

    // =========================================================
    // FORM STATE
    // =========================================================

    var name by remember {
        mutableStateOf("")
    }

    var age by remember {
        mutableStateOf("")
    }

    var gender by remember {
        mutableStateOf("")
    }

    var genderExpanded by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var profileError by remember {
        mutableStateOf<String?>(null)
    }

    // =========================================================
    // GENDER OPTIONS
    // =========================================================

    val genderOptions = listOf(
        "Male",
        "Female",
        "Other"
    )

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
                modifier = Modifier.height(30.dp)
            )

            // =================================================
            // PROFILE ICON
            // =================================================

            Box(
                modifier = Modifier
                    .size(96.dp)
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
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Color.White
                        )
                        .border(
                            width = 1.dp,
                            color = ShieldCyan.copy(alpha = 0.30f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = ShieldBlue,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // =================================================
            // TITLE
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
                text = "COMPLETE YOUR PROFILE",
                color = ShieldBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            // =================================================
            // PROFILE CARD
            // =================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(24.dp)
                    )
                    .background(
                        Color.White
                    )
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
                    text = "Personal Information",
                    color = ShieldBlack,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Tell us a little about yourself.",
                    color = ShieldTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                // =================================================
                // NAME
                // =================================================

                Text(
                    text = "Full Name *",
                    color = ShieldBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        profileError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Enter your full name",
                            color = ShieldTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
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
                // AGE
                // =================================================

                Text(
                    text = "Age *",
                    color = ShieldBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = { newValue ->

                        if (newValue.all { it.isDigit() }) {

                            age = newValue
                            profileError = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    placeholder = {
                        Text(
                            text = "Enter your age",
                            color = ShieldTextSecondary
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
// GENDER
// =================================================

                Text(
                    text = "Gender *",
                    color = ShieldBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = {
                        if (!isLoading) {
                            genderExpanded = !genderExpanded
                        }
                    }
                ) {

                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = {
                            Text(
                                text = "Select your gender",
                                color = ShieldTextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ShieldBlue
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = genderExpanded
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

                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = {
                            genderExpanded = false
                        },
                        modifier = Modifier
                            .background(Color.White)
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE0E7F0),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {

                        genderOptions.forEach { option ->

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = ShieldBlue,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {

                                    gender = option
                                    profileError = null
                                    genderExpanded = false
                                },
                                modifier = Modifier.background(Color.White)
                            )
                        }
                    }
                }

                // =================================================
                // ERROR
                // =================================================

                if (profileError != null) {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = profileError!!,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                // =================================================
                // SAVE BUTTON
                // =================================================

                Button(
                    onClick = {

                        when {

                            name.isBlank() -> {

                                profileError =
                                    "Please enter your name"
                            }

                            age.isBlank() -> {

                                profileError =
                                    "Please enter your age"
                            }

                            age.toIntOrNull() == null -> {

                                profileError =
                                    "Please enter a valid age"
                            }

                            age.toInt() < 1 ||
                                    age.toInt() > 120 -> {

                                profileError =
                                    "Please enter a valid age between 1 and 120"
                            }

                            gender.isBlank() -> {

                                profileError =
                                    "Please select your gender"
                            }

                            else -> {

                                isLoading = true
                                profileError = null

                                coroutineScope.launch {

                                    try {

                                        val token =
                                            tokenManager.getToken()

                                        if (token.isNullOrBlank()) {

                                            profileError =
                                                "Authentication session expired"

                                            isLoading = false

                                            return@launch
                                        }

                                        val request =
                                            ProfileCreateRequest(
                                                name = name.trim(),
                                                age = age.toInt(),
                                                gender = gender,
                                                profile_picture = null
                                            )

                                        val response =
                                            profileRepository.createProfile(
                                                token = token,
                                                request = request
                                            )

                                        isLoading = false

                                        if (response != null) {

                                            println(
                                                "VoiceShield Profile: profile created successfully"
                                            )

                                            onProfileComplete()

                                        } else {

                                            profileError =
                                                "Unable to save profile. Please try again."
                                        }

                                    } catch (e: Exception) {

                                        isLoading = false

                                        println(
                                            "VoiceShield Profile Error: ${e.message}"
                                        )

                                        profileError =
                                            "Unable to save profile. Please try again."
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
                            text = "SAVING PROFILE...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                    } else {

                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Text(
                            text = "SAVE & CONTINUE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                // =================================================
                // REQUIRED INFO
                // =================================================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(14.dp)
                        )
                        .background(
                            Color(0xFFF4FAF6)
                        )
                        .border(
                            1.dp,
                            ShieldGreen.copy(alpha = 0.22f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                ShieldGreen.copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ShieldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = "Name, age and gender are required.",
                        color = ShieldTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // =================================================
            // SECURITY FOOTER
            // =================================================

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
                modifier = Modifier.height(15.dp)
            )
        }
    }
}