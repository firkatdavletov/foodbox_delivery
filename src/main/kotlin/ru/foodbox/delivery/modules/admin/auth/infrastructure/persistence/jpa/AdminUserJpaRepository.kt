package ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminUserEntity
import java.util.UUID

interface AdminUserJpaRepository : JpaRepository<AdminUserEntity, UUID> {
    fun findByNormalizedLogin(normalizedLogin: String): AdminUserEntity?
}
