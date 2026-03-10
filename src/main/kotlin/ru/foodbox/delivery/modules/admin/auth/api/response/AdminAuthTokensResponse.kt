package ru.foodbox.delivery.modules.admin.auth.api.response

import java.time.Instant
import java.util.UUID

data class AdminAuthTokensResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val adminId: UUID,
)
