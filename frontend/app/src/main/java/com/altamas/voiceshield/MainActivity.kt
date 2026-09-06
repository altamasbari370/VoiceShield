package com.altamas.voiceshield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

import com.altamas.voiceshield.ui.screens.HistoryScreen
import com.altamas.voiceshield.data.ProfileRepository
import com.altamas.voiceshield.data.TokenManager

import com.altamas.voiceshield.ui.screens.ChangePasswordScreen
import com.altamas.voiceshield.ui.screens.DashboardScreen
import com.altamas.voiceshield.ui.screens.LoginScreen
import com.altamas.voiceshield.ui.screens.ProfileScreen
import com.altamas.voiceshield.ui.screens.ProfileSetupScreen
import com.altamas.voiceshield.ui.screens.RegisterScreen
import com.altamas.voiceshield.ui.screens.StartupScreen
import com.altamas.voiceshield.ui.screens.WelcomeScreen

import com.altamas.voiceshield.ui.state.AuthState
import com.altamas.voiceshield.ui.state.AuthViewModel
import com.altamas.voiceshield.ui.state.AuthViewModelFactory

import com.altamas.voiceshield.ui.theme.VoiceShieldTheme


class MainActivity : ComponentActivity() {

    // =========================================================
    // AUTH VIEW MODEL
    // =========================================================

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(applicationContext)
    }

    // =========================================================
    // PERMISSION PREFERENCES
    // =========================================================

    private val permissionPreferences by lazy {
        getSharedPreferences(
            "voiceshield_permissions",
            MODE_PRIVATE
        )
    }

    private companion object {
        const val KEY_PERMISSION_ONBOARDING_COMPLETED =
            "permission_onboarding_completed"
    }

    // =========================================================
    // MICROPHONE PERMISSION
    // =========================================================

    private val microphonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            println(
                "VoiceShield Permission: Microphone result = $granted"
            )

            // Continue to phone permission regardless of result.
            requestPhonePermission()
        }

    // =========================================================
    // PHONE PERMISSION
    // =========================================================

    private val phonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            println(
                "VoiceShield Permission: Phone result = $granted"
            )

            // Continue to overlay permission.
            requestOverlayPermission()
        }

    // =========================================================
    // OVERLAY SETTINGS RESULT
    // =========================================================

    private val overlayPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {

            val granted =
                Settings.canDrawOverlays(this)

            println(
                "VoiceShield Permission: Overlay result = $granted"
            )

            finishPermissionOnboarding()
        }

    // =========================================================
    // START PERMISSION FLOW
    // =========================================================

    private fun startPermissionOnboarding() {

        // -----------------------------------------------------
        // Safety check
        // -----------------------------------------------------

        if (permissionPreferences.getBoolean(
                KEY_PERMISSION_ONBOARDING_COMPLETED,
                false
            )
        ) {

            println(
                "VoiceShield Permission: Onboarding already completed"
            )

            return
        }

        println(
            "VoiceShield Permission: Starting one-time permission flow"
        )

        requestMicrophonePermission()
    }

    // =========================================================
    // MICROPHONE
    // =========================================================

    private fun requestMicrophonePermission() {

        val granted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (granted) {

            println(
                "VoiceShield Permission: Microphone already granted"
            )

            requestPhonePermission()

        } else {

            println(
                "VoiceShield Permission: Requesting microphone"
            )

            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    // =========================================================
    // PHONE
    // =========================================================

    private fun requestPhonePermission() {

        val granted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

        if (granted) {

            println(
                "VoiceShield Permission: Phone permission already granted"
            )

            requestOverlayPermission()

        } else {

            println(
                "VoiceShield Permission: Requesting phone permission"
            )

            phonePermissionLauncher.launch(
                Manifest.permission.READ_PHONE_STATE
            )
        }
    }

    // =========================================================
    // OVERLAY
    // =========================================================

    private fun requestOverlayPermission() {

        if (Settings.canDrawOverlays(this)) {

            println(
                "VoiceShield Permission: Overlay already granted"
            )

            finishPermissionOnboarding()

            return
        }

        println(
            "VoiceShield Permission: Opening overlay settings"
        )

        try {

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse(
                    "package:$packageName"
                )
            )

            overlayPermissionLauncher.launch(intent)

        } catch (e: Exception) {

            println(
                "VoiceShield Permission: Unable to open overlay settings: ${e.message}"
            )

            finishPermissionOnboarding()
        }
    }

    // =========================================================
    // FINISH PERMISSION FLOW
    // =========================================================

    private fun finishPermissionOnboarding() {

        permissionPreferences
            .edit()
            .putBoolean(
                KEY_PERMISSION_ONBOARDING_COMPLETED,
                true
            )
            .apply()

        println(
            "VoiceShield Permission: One-time onboarding completed"
        )
    }

    // =========================================================
    // ACTIVITY
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            VoiceShieldTheme(
                darkTheme = false
            ) {

                val authState by
                authViewModel.authState.collectAsState()

                // =================================================
                // TOKEN MANAGER
                // =================================================

                val tokenManager = remember {
                    TokenManager(applicationContext)
                }

                // =================================================
                // PROFILE REPOSITORY
                // =================================================

                val profileRepository = remember {
                    ProfileRepository()
                }

                // =================================================
                // SCREEN STATE
                // =================================================

                var currentScreen by remember {

                    mutableStateOf(

                        if (tokenManager.isLoggedIn()) {
                            "startup"
                        } else {
                            "welcome"
                        }

                    )
                }

                // =================================================
                // DASHBOARD PROFILE DATA
                // =================================================

                var dashboardUserName by remember {
                    mutableStateOf("User")
                }

                var dashboardProfilePicture by remember {
                    mutableStateOf<String?>(null)
                }

                // =================================================
                // CHECK EXISTING LOGIN
                // =================================================

                LaunchedEffect(Unit) {

                    if (tokenManager.isLoggedIn()) {

                        println(
                            "VoiceShield MainActivity: Existing token found"
                        )

                        authViewModel.checkAuthentication()

                    } else {

                        println(
                            "VoiceShield MainActivity: No saved token"
                        )
                    }
                }

                // =================================================
                // REACT TO AUTH STATE
                // =================================================

                LaunchedEffect(authState) {

                    when (authState) {

                        AuthState.Loading -> {

                            // Keep StartupScreen visible.
                        }

                        AuthState.LoggedOut -> {

                            kotlinx.coroutines.delay(2000)

                            println(
                                "VoiceShield MainActivity: User logged out"
                            )

                            currentScreen = "welcome"
                        }

                        AuthState.ProfileIncomplete -> {

                            kotlinx.coroutines.delay(2000)

                            println(
                                "VoiceShield MainActivity: Profile incomplete"
                            )

                            currentScreen = "profile_setup"
                        }

                        AuthState.ProfileComplete -> {

                            kotlinx.coroutines.delay(2000)

                            println(
                                "VoiceShield MainActivity: Profile complete"
                            )

                            // =====================================================
                            // ONE-TIME PERMISSION ONBOARDING
                            // =====================================================

                            if (
                                !permissionPreferences.getBoolean(
                                    KEY_PERMISSION_ONBOARDING_COMPLETED,
                                    false
                                )
                            ) {

                                println(
                                    "VoiceShield Permission: Starting permission onboarding"
                                )

                                startPermissionOnboarding()
                            }

                            currentScreen = "dashboard"
                        }

                        is AuthState.Error -> {

                            val error =
                                (authState as AuthState.Error).message

                            println(
                                "VoiceShield MainActivity Auth Error: $error"
                            )
                        }
                    }
                }

                // =================================================
                // LOAD PROFILE FOR DASHBOARD
                // =================================================

                LaunchedEffect(currentScreen) {

                    if (currentScreen == "dashboard") {

                        try {

                            val token =
                                tokenManager.getToken()

                            if (!token.isNullOrBlank()) {

                                val profile =
                                    profileRepository.getProfile(token)

                                if (profile != null) {

                                    dashboardUserName =
                                        profile.name ?: "User"

                                    dashboardProfilePicture =
                                        profile.profile_picture

                                } else {

                                    println(
                                        "VoiceShield Dashboard: Profile returned null"
                                    )
                                }

                            } else {

                                println(
                                    "VoiceShield Dashboard: Token is null"
                                )
                            }

                        } catch (e: Exception) {

                            println(
                                "VoiceShield Dashboard Profile Error: ${e.message}"
                            )
                        }
                    }
                }

                // =================================================
                // NAVIGATION
                // =================================================

                when (currentScreen) {

                    // =================================================
                    // WELCOME
                    // =================================================

                    "welcome" -> {

                        WelcomeScreen(

                            onLoginClick = {

                                currentScreen = "login"
                            },

                            onRegisterClick = {

                                currentScreen = "register"
                            }
                        )
                    }

                    // =================================================
                    // LOGIN
                    // =================================================

                    "login" -> {

                        LoginScreen(

                            onLoginSuccess = {

                                println(
                                    "VoiceShield MainActivity: Login successful"
                                )

                                currentScreen = "startup"

                                authViewModel.checkAuthentication()
                            },

                            onGuestClick = {

                                // Guest mode can be implemented later.
                            }
                        )
                    }

                    // =================================================
                    // REGISTER
                    // =================================================

                    "register" -> {

                        RegisterScreen(

                            onRegistrationSuccess = {

                                println(
                                    "VoiceShield MainActivity: Registration successful"
                                )

                                currentScreen = "login"
                            },

                            onBackToLogin = {

                                currentScreen = "login"
                            }
                        )
                    }

                    // =================================================
                    // STARTUP
                    // =================================================

                    "startup" -> {

                        StartupScreen()
                    }

                    // =================================================
                    // PROFILE SETUP
                    // =================================================

                    "profile_setup" -> {

                        ProfileSetupScreen(

                            onProfileComplete = {

                                println(
                                    "VoiceShield MainActivity: Profile setup completed"
                                )

                                // ---------------------------------
                                // Start one-time permissions
                                // ---------------------------------

                                if (
                                    !permissionPreferences.getBoolean(
                                        KEY_PERMISSION_ONBOARDING_COMPLETED,
                                        false
                                    )
                                ) {

                                    startPermissionOnboarding()
                                }

                                // ---------------------------------
                                // Continue existing authentication
                                // ---------------------------------

                                currentScreen = "startup"

                                authViewModel.checkAuthentication()
                            }
                        )
                    }

                    // =================================================
                    // HISTORY
                    // =================================================

                    "history" -> {

                        HistoryScreen(

                            onBackClick = {

                                currentScreen = "dashboard"
                            }
                        )
                    }

                    // =================================================
                    // DASHBOARD
                    // =================================================

                    "dashboard" -> {

                        DashboardScreen(

                            userName =
                                dashboardUserName,

                            profilePicture =
                                dashboardProfilePicture,

                            onStartDetection = {

                                // Detection is handled inside DashboardScreen.
                            },

                            onHistoryClick = {

                                currentScreen = "history"
                            },

                            onProfileClick = {

                                currentScreen = "profile"
                            },

                            onLogoutClick = {

                                println(
                                    "VoiceShield MainActivity: Logging out"
                                )

                                authViewModel.logout()

                                currentScreen = "welcome"
                            }
                        )
                    }

                    // =================================================
                    // PROFILE
                    // =================================================

                    "profile" -> {

                        ProfileScreen(

                            onBackClick = {

                                currentScreen = "dashboard"
                            },

                            onChangePasswordClick = {

                                currentScreen = "change_password"
                            },

                            onChangePhotoClick = {

                                // Photo picker handled inside ProfileScreen.
                            },

                            onSaveClick = { name, age ->

                                // ProfileScreen already updates backend.
                            }
                        )
                    }

                    // =================================================
                    // CHANGE PASSWORD
                    // =================================================

                    "change_password" -> {

                        ChangePasswordScreen(

                            onBackClick = {

                                currentScreen = "profile"
                            }
                        )
                    }
                }
            }
        }
    }
}