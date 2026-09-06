package com.altamas.voiceshield.ui.state

sealed class AuthState {

    data object Loading : AuthState()

    data object LoggedOut : AuthState()

    data object ProfileComplete : AuthState()

    data object ProfileIncomplete : AuthState()

    data class Error(
        val message: String
    ) : AuthState()
}