package ru.foodbox.delivery.modules.admin.auth.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.admin.auth.domain.AdminAuthSession
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminAuthSessionRepository
import ru.foodbox.delivery.modules.admin.auth.infrastructure.mapper.AdminAuthSessionMapper
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminAuthSessionEntity
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.jpa.AdminAuthSessionJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class AdminAuthSessionRepositoryImpl(
    private val jpaRepository: AdminAuthSessionJpaRepository,
) : AdminAuthSessionRepository {

    override fun save(session: AdminAuthSession): AdminAuthSession {
        val existing = jpaRepository.findById(session.id).getOrNull()
        val entity = existing ?: AdminAuthSessionEntity(
            id = session.id,
            adminId = session.adminId,
            deviceId = session.deviceId,
            userAgent = session.userAgent,
            ip = session.ip,
            refreshTokenHash = session.refreshTokenHash,
            expiresAt = session.expiresAt,
            revokedAt = session.revokedAt,
            createdAt = session.createdAt,
            lastUsedAt = session.lastUsedAt,
        )
        entity.adminId = session.adminId
        entity.deviceId = session.deviceId
        entity.userAgent = session.userAgent
        entity.ip = session.ip
        entity.refreshTokenHash = session.refreshTokenHash
        entity.expiresAt = session.expiresAt
        entity.revokedAt = session.revokedAt
        entity.lastUsedAt = session.lastUsedAt
        val saved = jpaRepository.save(entity)
        return AdminAuthSessionMapper.toDomain(saved)
    }

    override fun findByRefreshTokenHash(hash: String): AdminAuthSession? {
        val entity = jpaRepository.findByRefreshTokenHash(hash) ?: return null
        return AdminAuthSessionMapper.toDomain(entity)
    }

    override fun revokeById(sessionId: UUID, revokedAt: Instant) {
        val existing = jpaRepository.findById(sessionId).getOrNull() ?: return
        existing.revokedAt = revokedAt
        jpaRepository.save(existing)
    }
}
