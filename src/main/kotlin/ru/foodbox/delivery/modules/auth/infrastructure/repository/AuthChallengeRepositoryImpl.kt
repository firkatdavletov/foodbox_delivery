package ru.foodbox.delivery.modules.auth.infrastructure.repository

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.auth.domain.AuthChallenge
import ru.foodbox.delivery.modules.auth.domain.repository.AuthChallengeRepository
import ru.foodbox.delivery.modules.auth.infrastructure.mapper.AuthChallengeMapper
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity.AuthChallengeEntity
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.jpa.AuthChallengeJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class AuthChallengeRepositoryImpl(
    private val jpaRepository: AuthChallengeJpaRepository
) : AuthChallengeRepository {
    override fun save(challenge: AuthChallenge): AuthChallenge {
        val existing = jpaRepository.findById(challenge.id).getOrNull()
        val entity = existing ?: AuthChallengeEntity(
            id = challenge.id,
            method = challenge.method,
            target = challenge.target,
            status = challenge.status,
            codeHash = challenge.codeHash,
            expiresAt = challenge.expiresAt,
            attemptsLeft = challenge.attemptsLeft,
            createdAt = challenge.createdAt,
            completedAt = challenge.completedAt,
        )
        val saved = jpaRepository.save(entity)
        return AuthChallengeMapper.toDto(saved)
    }

    override fun findById(id: UUID): AuthChallenge? {
        val entity = jpaRepository.findByIdOrNull(id) ?: return null
        return AuthChallengeMapper.toDto(entity)
    }
}