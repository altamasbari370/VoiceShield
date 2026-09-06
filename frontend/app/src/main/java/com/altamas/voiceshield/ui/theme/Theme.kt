package com.altamas.voiceshield.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoiceShieldDarkColors = darkColorScheme(

    primary = ShieldBlue,
    secondary = ShieldCyan,
    tertiary = ShieldPurple,

    background = ShieldBlack,
    surface = ShieldSurface,

    onPrimary = ShieldWhite,
    onSecondary = ShieldBlack,
    onTertiary = ShieldWhite,

    onBackground = ShieldYellow,
    onSurface = ShieldYellow
)

private val VoiceShieldLightColors = lightColorScheme(

    primary = ShieldBlue,
    secondary = ShieldCyan,
    tertiary = ShieldPurple,

    background = Color(0xFFF8FAFC),
    surface = ShieldWhite,

    onPrimary = ShieldWhite,
    onSecondary = ShieldBlack,
    onTertiary = ShieldWhite,

    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

@Composable
fun VoiceShieldTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) {
            VoiceShieldDarkColors
        } else {
            VoiceShieldLightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}