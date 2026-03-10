package ru.foodbox.delivery.modules.admin.auth.infrastructure.mapper

import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminUserEntity

object AdminUserMapper {

    fun toDomain(entity: AdminUserEntity): AdminUser =
        AdminUser(
            id = entity.id,
            login = entity.login,
            normalizedLogin = entity.normalizedLogin,
            passwordHash = entity.passwordHash,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}
