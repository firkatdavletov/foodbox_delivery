package ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminUserEntity
import java.util.UUID

interface AdminUserJpaRepository : JpaRepository<AdminUserEntity, UUID> {
    fun findByNormalizedLogin(normalizedLogin: String): AdminUserEntity?
    fun findAllByDeletedAtIsNullOrderByCreatedAtDesc(): List<AdminUserEntity>
    fun existsByNormalizedLogin(normalizedLogin: String): Boolean
    fun existsByNormalizedLoginAndIdNot(normalizedLogin: String, id: UUID): Boolean
    fun countByRoleAndActiveIsTrueAndDeletedAtIsNull(role: AdminRole): Long
}
