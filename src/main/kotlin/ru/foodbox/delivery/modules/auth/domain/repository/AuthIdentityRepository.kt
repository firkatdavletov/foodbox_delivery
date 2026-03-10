package ru.foodbox.delivery.modules.auth.domain.repository

import ru.foodbox.delivery.modules.auth.domain.AuthIdentity
import ru.foodbox.delivery.modules.auth.domain.IdentityType

interface AuthIdentityRepository {
    fun findByTypeAndExternalId(type: IdentityType, externalId: String): AuthIdentity?
    fun findByTypeAndNormalizedLogin(type: IdentityType, normalizedLogin: String): AuthIdentity?
    fun save(identity: AuthIdentity): AuthIdentity
}