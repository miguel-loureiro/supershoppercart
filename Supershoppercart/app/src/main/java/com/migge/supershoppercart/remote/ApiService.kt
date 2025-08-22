package com.migge.supershoppercart.remote

import retrofit2.Call
import retrofit2.http.*
import retrofit2.Response

// Request model, aligned with backend DTO
data class DevLoginRequest(
    val email: String,
    val deviceId: String? = null
)

// Response model, aligned with backend DTO
data class DevLoginResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val error: String? = null
)

// Sync version for Authenticator (Retrofit synchronous Call)
interface ApiService {
    @POST("auth/dev-login")
    suspend fun devLogin(
        @Body request: DevLoginRequest
    ): Response<DevLoginResponse>

    @POST("auth/google")
    suspend fun googleLogin(
        @Header("Authorization") authorization: String,
        @Header("X-Device-Id") deviceId: String,
        @Body body: Any = Any()
    ): Response<DevLoginResponse>

    @POST("auth/refresh")
    fun refreshTokenSync(
        @Header("Authorization") refreshToken: String,
        @Header("X-Device-Id") deviceId: String
    ): Call<DevLoginResponse>
}