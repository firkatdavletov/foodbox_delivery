package ru.foodbox.delivery.modules.auth.infrastructure.persistance.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity.AuthSessionEntity
import java.util.UUID

interface AuthSessionJpaRepository : JpaRepository<AuthSessionEntity, UUID> {
    fun findByRefreshTokenHash(refreshTokenHash: String): AuthSessionEntity?
    fun findAllByUserIdAndRevokedAtIsNull(userId: UUID): List<AuthSessionEntity>
}