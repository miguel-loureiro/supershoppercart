package com.migge.supershoppercart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.migge.supershoppercart.remote.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState

    fun autoSignIn(deviceId: String) {
        _signInState.value = SignInState.Loading
        viewModelScope.launch {
            val success = authRepository.attemptAutomaticGoogleSignIn(deviceId)
            _signInState.value = if (success) SignInState.Success else SignInState.Failure
        }
    }
}

sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    object Success : SignInState()
    object Failure : SignInState()
}