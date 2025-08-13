package com.migge.supershoppercartapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migge.supershoppercartapp.data.remote.ApiService
import com.migge.supershoppercartapp.data.storage.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    val tokenManager: TokenManager
) : ViewModel() {

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val userName: String?) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun exchangeGoogleIdToken(idToken: String, deviceId: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val authModels = apiService.loginWithGoogle(
                    authorization = "Bearer $idToken",
                    deviceId = deviceId
                )
                tokenManager.saveTokens(
                    accessToken = authModels.accessToken,
                    refreshToken = authModels.refreshToken,
                    deviceId = deviceId
                )
                _loginState.value = LoginState.Success(userName = null)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }
}