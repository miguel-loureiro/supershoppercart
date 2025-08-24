package com.migge.supershoppercart.data.remote


import com.migge.supershoppercart.domain.DTO.TokenResponseDTO
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/google")
    suspend fun loginWithGoogle(
        @Header("Authorization") authHeader: String,
        @Header("X-Device-Id") deviceId: String? = null
    ): Response<TokenResponseDTO>
}

object ApiClient {
    private const val BASE_URL = "https://supershoppercart-dev.onrender.com" 

    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}
