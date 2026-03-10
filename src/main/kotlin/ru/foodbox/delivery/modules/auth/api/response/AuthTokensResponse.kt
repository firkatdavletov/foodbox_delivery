package ru.foodbox.delivery.modules.auth.api.response

import java.time.Instant
import java.util.UUID

data class AuthTokensResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val isNewUser: Boolean,
    val userId: UUID
)