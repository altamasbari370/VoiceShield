package com.altamas.voiceshield.data

class AuthRepository(
    private val tokenManager: TokenManager
) {

    private val api = RetrofitClient.api


    // =========================================================
    // LOGIN
    // =========================================================

    suspend fun login(
        email: String,
        password: String
    ): Result<LoginResponse> {

        return try {

            val response = api.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )

            if (response.isSuccessful) {

                val body = response.body()

                if (body != null) {

                    // Save JWT securely after successful login
                    tokenManager.saveToken(
                        body.access_token
                    )

                    Result.success(body)

                } else {

                    Result.failure(
                        Exception("Empty response from server")
                    )
                }

            } else {

                Result.failure(
                    Exception(
                        response.errorBody()?.string()
                            ?: "Login failed"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }


    // =========================================================
    // REGISTER
    // =========================================================

    suspend fun register(
        email: String,
        password: String
    ): Result<RegisterResponse> {

        return try {

            val response = api.register(
                RegisterRequest(
                    email = email,
                    password = password
                )
            )

            if (response.isSuccessful) {

                val body = response.body()

                if (body != null) {

                    // Registration does NOT save JWT.
                    // User must login after registration.

                    Result.success(body)

                } else {

                    Result.failure(
                        Exception("Empty response from server")
                    )
                }

            } else {

                Result.failure(
                    Exception(
                        response.errorBody()?.string()
                            ?: "Registration failed"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}