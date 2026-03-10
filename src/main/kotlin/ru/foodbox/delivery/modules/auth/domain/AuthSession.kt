package ru.foodbox.delivery.modules.auth.domain

import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class AuthSession(
    val id: UUID,
    val userId: UUID,
    val deviceId: String?,
    val userAgent: String?,
    val ip: String?,
    val refreshTokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val replacedBySessionId: Long?,
    val createdAt: Instant,
    val lastUsedAt: Instant
) {
    fun isActive(now: Instant): Boolean =
        revokedAt == null && expiresAt.isAfter(now)
}