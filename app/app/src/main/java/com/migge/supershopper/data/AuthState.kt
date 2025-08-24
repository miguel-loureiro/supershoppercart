package com.migge.supershopper.data

data class AuthState(
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)