package com.example.supershoppercartapp.data.models

data class AuthModels(
    val accessToken: String,
    val refreshToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceId: String
)

data class Shopper(
    val id: String,
    val email: String,
    val name: String,
    val shopCartIds: List<String> = emptyList()
)
