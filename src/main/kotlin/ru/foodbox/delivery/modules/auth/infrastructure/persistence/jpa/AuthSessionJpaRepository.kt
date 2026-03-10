package ru.foodbox.delivery.modules.auth.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.auth.infrastructure.persistence.entity.AuthSessionEntity
import java.util.UUID

interface AuthSessionJpaRepository : JpaRepository<AuthSessionEntity, UUID> {
    fun findByRefreshTokenHash(refreshTokenHash: String): AuthSessionEntity?
    fun findAllByUserIdAndRevokedAtIsNull(userId: UUID): List<AuthSessionEntity>
}