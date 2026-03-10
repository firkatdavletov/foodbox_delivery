package ru.foodbox.delivery.modules.auth.infrastructure.mapper

import ru.foodbox.delivery.modules.auth.domain.AuthSession
import ru.foodbox.delivery.modules.auth.infrastructure.persistence.entity.AuthSessionEntity

object AuthSessionMapper {
    fun toDto(entity: AuthSessionEntity): AuthSession {
        return AuthSession(
            id = entity.id,
            userId = entity.userId,
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
}
