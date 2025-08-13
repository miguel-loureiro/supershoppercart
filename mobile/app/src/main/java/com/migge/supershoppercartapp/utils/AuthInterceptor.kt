package com.migge.supershoppercartapp.utils

import com.migge.supershoppercartapp.data.storage.TokenManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val accessToken = runBlocking { tokenManager.getAccessToken().firstOrNull() }
        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }
        return chain.proceed(requestBuilder.build())
    }
}