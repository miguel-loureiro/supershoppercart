package com.migge.supershopper.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("auth/google-login")
    suspend fun googleLogin(
        @Header("Authorization") bearerToken: String,
        @Header("deviceId") deviceId: String
    ): Response<DevLoginResponse>

    @POST("auth/dev-login")
    suspend fun devLogin(
        @Body request: DevLoginRequest
    ): Response<DevLoginResponse>

    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Header("Authorization") bearerToken: String
    ): Response<DevLoginResponse>
}
