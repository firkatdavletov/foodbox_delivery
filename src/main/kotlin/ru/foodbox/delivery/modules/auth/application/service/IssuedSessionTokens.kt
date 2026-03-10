package ru.foodbox.delivery.modules.auth.application.service

import ru.foodbox.delivery.modules.auth.domain.AuthSession
import java.time.Instant

data class IssuedSessionTokens(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
    val session: AuthSession
)