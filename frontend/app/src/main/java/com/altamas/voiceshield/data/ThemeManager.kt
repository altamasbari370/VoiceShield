package com.altamas.voiceshield.data

import android.content.Context

class ThemeManager(context: Context) {

    private val preferences =
        context.getSharedPreferences(
            "voiceshield_settings",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val DARK_MODE_KEY = "dark_mode"
    }

    fun isDarkMode(): Boolean {
        return preferences.getBoolean(
            DARK_MODE_KEY,
            false
        )
    }

    fun setDarkMode(enabled: Boolean) {
        preferences.edit()
            .putBoolean(DARK_MODE_KEY, enabled)
            .apply()
    }
}