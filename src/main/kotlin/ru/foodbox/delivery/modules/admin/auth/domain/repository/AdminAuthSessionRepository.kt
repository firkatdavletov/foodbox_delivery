package ru.foodbox.delivery.modules.admin.auth.domain.repository

import ru.foodbox.delivery.modules.admin.auth.domain.AdminAuthSession
import java.time.Instant
import java.util.UUID

interface AdminAuthSessionRepository {
    fun save(session: AdminAuthSession): AdminAuthSession
    fun findByRefreshTokenHash(hash: String): AdminAuthSession?
    fun revokeById(sessionId: UUID, revokedAt: Instant)
}
