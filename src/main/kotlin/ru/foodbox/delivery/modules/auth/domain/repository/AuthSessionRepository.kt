package ru.foodbox.delivery.modules.auth.domain.repository

import ru.foodbox.delivery.modules.auth.domain.AuthSession
import java.time.Instant
import java.util.UUID

interface AuthSessionRepository {
    fun save(session: AuthSession): AuthSession
    fun findByRefreshTokenHash(hash: String): AuthSession?
    fun revokeById(sessionId: UUID, revokedAt: Instant)
    fun revokeAllByUserId(userId: UUID, revokedAt: Instant)
}