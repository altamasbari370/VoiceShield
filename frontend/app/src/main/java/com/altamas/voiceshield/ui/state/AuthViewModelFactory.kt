package com.altamas.voiceshield.ui.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.altamas.voiceshield.data.TokenManager

class AuthViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {

            return AuthViewModel(
                TokenManager(context.applicationContext)
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}