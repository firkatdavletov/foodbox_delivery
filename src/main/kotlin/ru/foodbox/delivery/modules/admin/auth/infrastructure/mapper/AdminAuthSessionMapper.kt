package ru.foodbox.delivery.modules.admin.auth.infrastructure.mapper

import ru.foodbox.delivery.modules.admin.auth.domain.AdminAuthSession
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminAuthSessionEntity

object AdminAuthSessionMapper {

    fun toDomain(entity: AdminAuthSessionEntity): AdminAuthSession =
        AdminAuthSession(
            id = entity.id,
            adminId = entity.adminId,
            deviceId = entity.deviceId,
            userAgent = entity.userAgent,
            ip = entity.ip,
            refreshTokenHash = entity.refreshTokenHash,
            expiresAt = entity.expiresAt,
            revokedAt = entity.revokedAt,
            createdAt = entity.createdAt,
            lastUsedAt = entity.lastUsedAt,
        )
}
