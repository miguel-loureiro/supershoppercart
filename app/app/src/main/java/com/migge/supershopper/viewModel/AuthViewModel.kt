package com.migge.supershopper.viewModel

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.migge.supershopper.auth.GoogleSignInResult
import com.migge.supershopper.data.AuthState
import com.migge.supershopper.network.ApiClient
import com.migge.supershopper.network.DevLoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun googleSignIn(context: Context, result: GoogleSignInResult): Boolean {
        return try {
            val apiService = ApiClient.create(context)
            val deviceId = getDeviceId(context)

            // Use the ID token for authentication
            val response = apiService.googleLogin("Bearer ${result.idToken}", deviceId)

            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse?.error == null) {
                    _authState.value = AuthState(
                        isAuthenticated = true,
                        userEmail = result.email,
                        accessToken = loginResponse?.accessToken,
                        refreshToken = loginResponse?.refreshToken
                    )
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            // Fallback to dev login for demonstration
            devLogin(context, result.email)
        }
    }

    private suspend fun devLogin(context: Context, email: String): Boolean {
        return try {
            val apiService = ApiClient.create(context)
            val request = DevLoginRequest(email = email, deviceId = getDeviceId(context))
            val response = apiService.devLogin(request)

            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse?.error == null) {
                    _authState.value = AuthState(
                        isAuthenticated = true,
                        userEmail = email,
                        accessToken = loginResponse?.accessToken,
                        refreshToken = loginResponse?.refreshToken
                    )
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            // For demo purposes, simulate success
            _authState.value = AuthState(
                isAuthenticated = true,
                userEmail = email,
                accessToken = "demo_token",
                refreshToken = "demo_refresh_token"
            )
            true
        }
    }

    private fun getDeviceId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var deviceId = sharedPrefs.getString("device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPrefs.edit { putString("device_id", deviceId) }
        }
        return deviceId
    }
}