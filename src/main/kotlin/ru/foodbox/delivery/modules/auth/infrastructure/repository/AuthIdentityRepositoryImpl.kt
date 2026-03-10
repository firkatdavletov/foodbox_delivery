package ru.foodbox.delivery.modules.auth.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.auth.domain.AuthIdentity
import ru.foodbox.delivery.modules.auth.domain.IdentityType
import ru.foodbox.delivery.modules.auth.domain.repository.AuthIdentityRepository
import ru.foodbox.delivery.modules.auth.infrastructure.mapper.AuthIdentityMapper
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity.AuthIdentityEntity
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.jpa.AuthIdentityJpaRepository
import kotlin.jvm.optionals.getOrNull

@Repository
class AuthIdentityRepositoryImpl(
    private val jpaRepository: AuthIdentityJpaRepository
) : AuthIdentityRepository {
    override fun findByTypeAndExternalId(
        type: IdentityType,
        externalId: String
    ): AuthIdentity? {
        val entity = jpaRepository.findByTypeAndExternalId(type, externalId) ?: return null
        return AuthIdentityMapper.toDto(entity)
    }

    override fun findByTypeAndNormalizedLogin(
        type: IdentityType,
        normalizedLogin: String
    ): AuthIdentity? {
        val entity = jpaRepository.findByTypeAndNormalizedLogin(type, normalizedLogin) ?: return null
        return AuthIdentityMapper.toDto(entity)
    }

    override fun save(identity: AuthIdentity): AuthIdentity {
        val existing = jpaRepository.findById(identity.id).getOrNull()
        val entity = existing ?: AuthIdentityEntity(
            id = identity.id,
            externalId = identity.externalId,
            type = identity.type,
            userId = identity.userId,
            normalizedLogin = identity.normalizedLogin,
            isVerified = identity.isVerified,
            createdAt = identity.createdAt,
            lastUsedAt = identity.lastUsedAt,
        )
        val saved = jpaRepository.save(entity)
        return AuthIdentityMapper.toDto(saved)
    }
}