package com.migge.supershoppercart.ui.screen.signin

/**
 * Representa os estados possíveis da tela de Sign-In.
 */
sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Success(val accessToken: String?, val refreshToken: String?) : SignInState()
    data class Error(val message: String) : SignInState()
}