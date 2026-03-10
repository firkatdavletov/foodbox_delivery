package ru.foodbox.delivery.modules.auth.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.auth.domain.AuthSession
import ru.foodbox.delivery.modules.auth.domain.repository.AuthSessionRepository
import ru.foodbox.delivery.modules.auth.infrastructure.mapper.AuthSessionMapper
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity.AuthSessionEntity
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.jpa.AuthSessionJpaRepository
import java.time.Instant
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Repository
class AuthSessionRepositoryImpl(
    private val jpaRepository: AuthSessionJpaRepository,
) : AuthSessionRepository {
    override fun save(session: AuthSession): AuthSession {
        val existing = jpaRepository.findById(session.id).getOrNull()
        val entity = existing ?: AuthSessionEntity(
            id = session.id,
            userId = session.userId,
            deviceId = session.deviceId,
            userAgent = session.userAgent,
            ip = session.ip,
            refreshTokenHash = session.refreshTokenHash,
            expiresAt = session.expiresAt,
            revokedAt = session.revokedAt,
            createdAt = session.createdAt,
            lastUsedAt = session.lastUsedAt,
        )
        val saved = jpaRepository.save(entity)
        return AuthSessionMapper.toDto(saved)
    }

    override fun findByRefreshTokenHash(hash: String): AuthSession? {
        val entity = jpaRepository.findByRefreshTokenHash(hash) ?: return null
        return AuthSessionMapper.toDto(entity)
    }

    override fun revokeById(sessionId: UUID, revokedAt: Instant) {
        val existing = jpaRepository.findById(sessionId).getOrNull() ?: return
        existing.revokedAt = revokedAt
        jpaRepository.save(existing)
    }

    override fun revokeAllByUserId(userId: UUID, revokedAt: Instant) {
        val allExisting = jpaRepository.findAllByUserIdAndRevokedAtIsNull(userId)
        allExisting.forEach {
            it.revokedAt = revokedAt
        }
        jpaRepository.saveAll(allExisting)
    }
}