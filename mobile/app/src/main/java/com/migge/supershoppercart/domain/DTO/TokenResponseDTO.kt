package com.migge.supershoppercart.domain.DTO

data class TokenResponseDTO(
    val accessToken: String?,
    val refreshToken: String?,
    val error: String?
)
