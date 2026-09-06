package com.altamas.voiceshield.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altamas.voiceshield.data.ProfileRepository
import com.altamas.voiceshield.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val profileRepository = ProfileRepository()

    private val _authState =
        MutableStateFlow<AuthState>(AuthState.Loading)

    val authState: StateFlow<AuthState> = _authState


    // =========================================================
    // CHECK AUTHENTICATION
    // =========================================================

    fun checkAuthentication() {

        val token = tokenManager.getToken()

        println(
            "VoiceShield Auth: token exists = ${token != null}"
        )

        // -----------------------------------------------------
        // No JWT
        // -----------------------------------------------------

        if (token.isNullOrBlank()) {

            _authState.value =
                AuthState.LoggedOut

            return
        }


        // -----------------------------------------------------
        // JWT exists → check profile
        // -----------------------------------------------------

        viewModelScope.launch {

            try {

                println(
                    "VoiceShield Auth: requesting /profile"
                )

                val profile =
                    profileRepository.getProfile(token)

                println(
                    "VoiceShield Auth: profile response = $profile"
                )


                // -------------------------------------------------
                // Profile request failed
                // -------------------------------------------------

                if (profile == null) {

                    _authState.value =
                        AuthState.Error(
                            "Unable to load profile"
                        )

                    return@launch
                }


                // -------------------------------------------------
                // Check profile completion
                // -------------------------------------------------

                if (profile.profile_completed) {

                    println(
                        "VoiceShield Auth: profile complete"
                    )

                    _authState.value =
                        AuthState.ProfileComplete

                } else {

                    println(
                        "VoiceShield Auth: profile incomplete"
                    )

                    _authState.value =
                        AuthState.ProfileIncomplete
                }

            } catch (e: Exception) {

                println(
                    "VoiceShield Auth ERROR: ${e.message}"
                )

                _authState.value =
                    AuthState.Error(
                        e.message
                            ?: "Something went wrong"
                    )
            }
        }
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    fun logout() {

        println(
            "VoiceShield Auth: logging out"
        )

        tokenManager.clearToken()

        _authState.value =
            AuthState.LoggedOut
    }
}