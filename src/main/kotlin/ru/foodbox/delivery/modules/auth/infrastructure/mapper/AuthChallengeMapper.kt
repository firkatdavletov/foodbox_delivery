package ru.foodbox.delivery.modules.auth.infrastructure.mapper

import ru.foodbox.delivery.modules.auth.domain.AuthChallenge
import ru.foodbox.delivery.modules.auth.infrastructure.persistence.entity.AuthChallengeEntity

object AuthChallengeMapper {
    fun toDto(entity: AuthChallengeEntity): AuthChallenge {
        return AuthChallenge(
            id = entity.id,
            method = entity.method,
            target = entity.target,
            status = entity.status,
            codeHash = entity.codeHash,
            externalState = null,
            createdAt = entity.createdAt,
            expiresAt = entity.expiresAt,
            attemptsLeft = entity.attemptsLeft,
            completedAt = entity.completedAt,
        )
    }
}