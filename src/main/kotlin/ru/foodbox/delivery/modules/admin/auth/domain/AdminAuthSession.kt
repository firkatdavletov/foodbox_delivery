package ru.foodbox.delivery.modules.admin.auth.domain

import java.time.Instant
import java.util.UUID

data class AdminAuthSession(
    val id: UUID,
    val adminId: UUID,
    val deviceId: String?,
    val userAgent: String?,
    val ip: String?,
    val refreshTokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant,
    val lastUsedAt: Instant,
) {
    fun isActive(now: Instant): Boolean =
        revokedAt == null && expiresAt.isAfter(now)
}
