package ru.foodbox.delivery.modules.admin.auth.application.service

import java.time.Instant

data class IssuedAdminSessionTokens(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
)
