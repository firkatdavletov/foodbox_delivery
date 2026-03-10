package ru.foodbox.delivery.modules.admin.auth.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminUserRepository
import ru.foodbox.delivery.modules.admin.auth.infrastructure.mapper.AdminUserMapper
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminUserEntity
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.jpa.AdminUserJpaRepository
import kotlin.jvm.optionals.getOrNull

@Repository
class AdminUserRepositoryImpl(
    private val jpaRepository: AdminUserJpaRepository,
) : AdminUserRepository {

    override fun save(user: AdminUser): AdminUser {
        val existing = jpaRepository.findById(user.id).getOrNull()
        val entity = existing ?: AdminUserEntity(
            id = user.id,
            login = user.login,
            normalizedLogin = user.normalizedLogin,
            passwordHash = user.passwordHash,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
        entity.login = user.login
        entity.normalizedLogin = user.normalizedLogin
        entity.passwordHash = user.passwordHash
        entity.updatedAt = user.updatedAt
        val saved = jpaRepository.save(entity)
        return AdminUserMapper.toDomain(saved)
    }

    override fun findByNormalizedLogin(normalizedLogin: String): AdminUser? {
        val entity = jpaRepository.findByNormalizedLogin(normalizedLogin) ?: return null
        return AdminUserMapper.toDomain(entity)
    }

    override fun count(): Long = jpaRepository.count()
}
