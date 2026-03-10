package ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminAuthSessionEntity
import java.util.UUID

interface AdminAuthSessionJpaRepository : JpaRepository<AdminAuthSessionEntity, UUID> {
    fun findByRefreshTokenHash(refreshTokenHash: String): AdminAuthSessionEntity?
}
