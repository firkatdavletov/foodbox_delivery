package ru.foodbox.delivery.modules.auth.infrastructure.persistance.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.auth.domain.IdentityType
import ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity.AuthIdentityEntity
import java.util.UUID

interface AuthIdentityJpaRepository : JpaRepository<AuthIdentityEntity, UUID> {
    fun findByTypeAndExternalId(type: IdentityType, externalId: String): AuthIdentityEntity?
    fun findByTypeAndNormalizedLogin(type: IdentityType, normalizedLogin: String): AuthIdentityEntity?
}