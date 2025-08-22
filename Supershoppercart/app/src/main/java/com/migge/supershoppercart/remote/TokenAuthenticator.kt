package com.migge.supershoppercart.remote

import android.content.Context
import com.migge.supershoppercart.storage.Config
import com.migge.supershoppercart.storage.TokenStorage
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class TokenAuthenticator(
    private val context: Context
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent multiple refresh attempts
        if (responseCount(response) >= 2) return null

        val refreshToken = TokenStorage.getRefreshToken(context) ?: return null
        val deviceId = getDeviceId(context) // implement as needed

        // Create a synchronous Retrofit instance for refreshing token
        val retrofit = Retrofit.Builder()
            .baseUrl(Config.apiBaseUrl(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        // Make the refresh token call synchronously
        val refreshCall = apiService.refreshTokenSync(refreshToken, deviceId)
        val newTokenResponse = try {
            refreshCall.execute().body()
        } catch (e: Exception) {
            null
        }

        if (newTokenResponse?.accessToken != null && newTokenResponse.refreshToken != null && newTokenResponse.error.isNullOrEmpty()) {
            // Save new tokens
            TokenStorage.saveTokens(context, newTokenResponse.accessToken, newTokenResponse.refreshToken)

            // Retry the original request with new access token
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokenResponse.accessToken}")
                .build()
        }
        return null
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private fun getDeviceId(context: Context): String {
        // Implement device ID retrieval, e.g. from SharedPreferences or Settings.Secure
        return "your-device-id"
    }
}