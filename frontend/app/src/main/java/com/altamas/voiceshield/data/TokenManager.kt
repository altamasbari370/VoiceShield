package com.altamas.voiceshield.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

class TokenManager(context: Context) {

    private val appContext = context.applicationContext

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = createPreferences()

    private fun createPreferences(): android.content.SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            /*
             * The encrypted preferences may become unreadable if the
             * Android Keystore key and encrypted preference data become
             * out of sync.
             *
             * Delete the corrupted preferences and recreate them so the
             * application does not crash during startup.
             */
            try {
                appContext.deleteSharedPreferences(PREFS_NAME)
            } catch (_: Exception) {
                // Ignore cleanup failure and try recreating the preferences.
            }

            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun saveToken(token: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    fun getToken(): String? {
        return try {
            preferences.getString(KEY_ACCESS_TOKEN, null)
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    fun clearToken() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "voiceshield_secure_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}