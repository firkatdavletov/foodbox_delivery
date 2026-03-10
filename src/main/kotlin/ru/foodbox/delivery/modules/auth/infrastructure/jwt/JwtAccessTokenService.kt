package ru.foodbox.delivery.modules.auth.infrastructure.jwt

import ru.foodbox.delivery.common.security.UserPrincipal
import ru.foodbox.delivery.common.security.UserRole
import java.time.Instant
import java.util.UUID

interface JwtAccessTokenService {
    fun generateAccessToken(userId: UUID, sessionId: UUID, roles: List<UserRole>, expiresAt: Instant): String
    fun parseAndValidate(token: String): UserPrincipal?
}