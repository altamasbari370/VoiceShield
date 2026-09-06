package com.altamas.voiceshield.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://voiceshield-backend-0gxt.onrender.com/"

    private lateinit var tokenManager: TokenManager

    fun initialize(context: Context) {
        tokenManager = TokenManager(context.applicationContext)
    }

    private val authInterceptor = Interceptor { chain ->

        val token = if (::tokenManager.isInitialized) {
            tokenManager.getToken()
        } else {
            null
        }

        val requestBuilder = chain.request().newBuilder()

        if (token != null) {
            requestBuilder.addHeader(
                "Authorization",
                "Bearer $token"
            )
        }

        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val api: VoiceShieldApi by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(VoiceShieldApi::class.java)
    }
}