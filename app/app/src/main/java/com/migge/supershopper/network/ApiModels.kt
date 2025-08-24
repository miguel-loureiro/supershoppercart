package com.migge.supershopper.network

data class DevLoginRequest(
    val email: String,
    val deviceId: String? = null
)

data class DevLoginResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val error: String? = null
)