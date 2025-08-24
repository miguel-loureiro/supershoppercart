package com.migge.supershoppercart.ui.screen.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migge.supershoppercart.data.remote.ApiClient
import com.migge.supershoppercart.domain.DTO.TokenResponseDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {
    private val _state = MutableStateFlow<SignInState>(SignInState.Idle)
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun loginWithGoogle(idToken: String, deviceId: String?) {
        viewModelScope.launch {
            try {
                _state.value = SignInState.Loading
                val response = ApiClient.authApi.loginWithGoogle(
                    authHeader = "Bearer $idToken",
                    deviceId = deviceId
                )
                if (response.isSuccessful) {
                    val body: TokenResponseDTO? = response.body()
                    if (body?.accessToken != null) {
                        _state.value = SignInState.Success(body.accessToken, body.refreshToken)
                    } else {
                        _state.value = SignInState.Error(body?.error ?: "Unknown error")
                    }
                } else {
                    _state.value = SignInState.Error("Login failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = SignInState.Error("Exception: ${e.message}")
            }
        }
    }
}