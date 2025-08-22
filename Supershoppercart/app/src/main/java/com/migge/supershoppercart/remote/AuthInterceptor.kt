package com.migge.supershoppercart.remote

import android.content.Context
import com.migge.supershoppercart.storage.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = TokenStorage.getAccessToken(context)
        val originalRequest = chain.request()
        return if (accessToken != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}