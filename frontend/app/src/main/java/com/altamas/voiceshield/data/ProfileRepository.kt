package com.altamas.voiceshield.data

import com.altamas.voiceshield.models.ProfileCreateRequest
import com.altamas.voiceshield.models.ProfileResponse

class ProfileRepository {

    private val api = RetrofitClient.api

    // =========================================================
    // GET PROFILE
    // =========================================================

    suspend fun getProfile(
        token: String
    ): ProfileResponse? {

        val response = api.getProfile(
            "Bearer $token"
        )

        println(
            "VoiceShield Profile: HTTP ${response.code()}"
        )

        if (!response.isSuccessful) {

            println(
                "VoiceShield Profile ERROR: " +
                        "HTTP ${response.code()} " +
                        "${response.errorBody()?.string()}"
            )

            return null
        }

        val body = response.body()

        println(
            "VoiceShield Profile: body = $body"
        )

        return body
    }


    // =========================================================
    // CREATE PROFILE
    // =========================================================

    suspend fun createProfile(
        token: String,
        request: ProfileCreateRequest
    ): ProfileResponse? {

        val response = api.createProfile(
            "Bearer $token",
            request
        )

        println(
            "VoiceShield Profile Create: HTTP ${response.code()}"
        )

        if (!response.isSuccessful) {

            println(
                "VoiceShield Profile Create ERROR: " +
                        "HTTP ${response.code()} " +
                        "${response.errorBody()?.string()}"
            )

            return null
        }

        return response.body()
    }
}