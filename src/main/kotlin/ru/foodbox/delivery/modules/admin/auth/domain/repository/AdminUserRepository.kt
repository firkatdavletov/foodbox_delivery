package ru.foodbox.delivery.modules.admin.auth.domain.repository

import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser

interface AdminUserRepository {
    fun save(user: AdminUser): AdminUser
    fun findByNormalizedLogin(normalizedLogin: String): AdminUser?
    fun count(): Long
}
