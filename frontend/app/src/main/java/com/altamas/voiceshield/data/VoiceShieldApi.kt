package com.altamas.voiceshield.data

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

import com.altamas.voiceshield.models.HistoryCreateRequest
import com.altamas.voiceshield.models.HistoryResponse
import retrofit2.http.DELETE


import com.altamas.voiceshield.models.ChangePasswordRequest
import com.altamas.voiceshield.models.ChangePasswordResponse
import com.altamas.voiceshield.models.ProfileCreateRequest
import com.altamas.voiceshield.models.ProfileResponse


interface VoiceShieldApi {

    @Multipart
    @POST("upload-audio")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part
    ): AudioUploadResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @GET("profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<ProfileResponse>

    @POST("profile")
    suspend fun createProfile(
        @Header("Authorization") token: String,
        @Body request: ProfileCreateRequest
    ): Response<ProfileResponse>
    @POST("history")
    suspend fun createHistory(
        @Header("Authorization") token: String,
        @Body request: HistoryCreateRequest
    ): Response<HistoryResponse>

    @GET("history")
    suspend fun getHistory(
        @Header("Authorization") token: String
    ): Response<List<HistoryResponse>>

    @DELETE("history/{history_id}")
    suspend fun deleteHistory(
        @Header("Authorization") token: String,
        @retrofit2.http.Path("history_id") historyId: Int
    ): Response<Map<String, String>>

    @POST("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>
}