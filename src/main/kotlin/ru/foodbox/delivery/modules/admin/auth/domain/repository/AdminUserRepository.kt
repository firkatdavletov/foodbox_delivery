package ru.foodbox.delivery.modules.admin.auth.domain.repository

import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import java.util.UUID

interface AdminUserRepository {
    fun save(user: AdminUser): AdminUser
    fun findById(id: UUID): AdminUser?
    fun findByNormalizedLogin(normalizedLogin: String): AdminUser?
    fun findAllNotDeleted(): List<AdminUser>
    fun existsByNormalizedLogin(normalizedLogin: String): Boolean
    fun existsByNormalizedLoginExceptId(normalizedLogin: String, excludedId: UUID): Boolean
    fun countActiveByRole(role: AdminRole): Long
    fun count(): Long
}
