package ru.foodbox.delivery.modules.auth.infrastructure.mapper

import ru.foodbox.delivery.modules.auth.domain.AuthIdentity
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity.AuthIdentityEntity

object AuthIdentityMapper {
    fun toDto(entity: AuthIdentityEntity): AuthIdentity {
        return AuthIdentity(
            id = entity.id,
            userId = entity.userId,
            type = entity.type,
            externalId = entity.externalId,
            normalizedLogin = entity.normalizedLogin,
            isVerified = entity.isVerified,
            createdAt = entity.createdAt,
            lastUsedAt = entity.lastUsedAt
        )
    }
}