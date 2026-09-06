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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {

    val black = Color(0xFF02050C)
    val blue = Color(0xFF2979FF)
    val cyan = Color(0xFF00E5FF)
    val purple = Color(0xFF9C27B0)
    val white = Color.White
    val secondary = Color(0xFF8A94A6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF02050C),
                        Color(0xFF050A16),
                        black
                    )
                )
            )
    ) {

        // =====================================================
        // BACKGROUND GLOW
        // =====================================================

        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            blue.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                )
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // =================================================
            // SHIELD LOGO
            // =================================================

            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                blue.copy(alpha = 0.25f),
                                cyan.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = cyan.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "VoiceShield",
                    tint = cyan,
                    modifier = Modifier.size(75.dp)
                )
            }


            Spacer(
                modifier = Modifier.height(28.dp)
            )


            // =================================================
            // LOGO TEXT
            // =================================================

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Voice",
                    color = white,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "Shield",
                    color = cyan,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(
                text = "AI-POWERED VOICE PROTECTION",
                color = cyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp
            )


            Spacer(
                modifier = Modifier.height(22.dp)
            )


            Text(
                text = "Protect your voice identity",
                color = white,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(
                text = "Detect AI-generated and cloned voices\nbefore they become a threat.",
                color = secondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )


            Spacer(
                modifier = Modifier.height(38.dp)
            )


            // =================================================
            // LOGIN BUTTON
            // =================================================

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = blue
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(
                    modifier = Modifier.size(10.dp)
                )

                Text(
                    text = "LOGIN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            // =================================================
            // REGISTER BUTTON
            // =================================================

            Button(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = white
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = purple.copy(alpha = 0.8f)
                )
            ) {

                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(
                    modifier = Modifier.size(10.dp)
                )

                Text(
                    text = "CREATE ACCOUNT",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }


            Spacer(
                modifier = Modifier.height(30.dp)
            )


            // =================================================
            // SECURITY INFO
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Color.White.copy(alpha = 0.035f)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            cyan.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = cyan,
                        modifier = Modifier.size(22.dp)
                    )
                }


                Spacer(
                    modifier = Modifier.size(12.dp)
                )


                Column {

                    Text(
                        text = "Advanced Voice Security",
                        color = white,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Your voice. Your privacy.",
                        color = secondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}