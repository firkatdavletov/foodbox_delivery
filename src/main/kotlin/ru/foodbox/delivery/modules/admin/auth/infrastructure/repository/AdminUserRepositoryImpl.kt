package ru.foodbox.delivery.modules.admin.auth.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import ru.foodbox.delivery.modules.admin.auth.domain.AdminUser
import ru.foodbox.delivery.modules.admin.auth.domain.repository.AdminUserRepository
import ru.foodbox.delivery.modules.admin.auth.infrastructure.mapper.AdminUserMapper
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity.AdminUserEntity
import ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.jpa.AdminUserJpaRepository
import java.util.UUID
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
            role = user.role,
            active = user.active,
            deletedAt = user.deletedAt,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
        entity.login = user.login
        entity.normalizedLogin = user.normalizedLogin
        entity.passwordHash = user.passwordHash
        entity.role = user.role
        entity.active = user.active
        entity.deletedAt = user.deletedAt
        entity.updatedAt = user.updatedAt
        val saved = jpaRepository.save(entity)
        return AdminUserMapper.toDomain(saved)
    }

    override fun findById(id: UUID): AdminUser? {
        val entity = jpaRepository.findById(id).getOrNull() ?: return null
        return AdminUserMapper.toDomain(entity)
    }

    override fun findByNormalizedLogin(normalizedLogin: String): AdminUser? {
        val entity = jpaRepository.findByNormalizedLogin(normalizedLogin) ?: return null
        return AdminUserMapper.toDomain(entity)
    }

    override fun findAllNotDeleted(): List<AdminUser> =
        jpaRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc()
            .map(AdminUserMapper::toDomain)

    override fun existsByNormalizedLogin(normalizedLogin: String): Boolean =
        jpaRepository.existsByNormalizedLogin(normalizedLogin)

    override fun existsByNormalizedLoginExceptId(normalizedLogin: String, excludedId: UUID): Boolean =
        jpaRepository.existsByNormalizedLoginAndIdNot(normalizedLogin, excludedId)

    override fun countActiveByRole(role: AdminRole): Long =
        jpaRepository.countByRoleAndActiveIsTrueAndDeletedAtIsNull(role)

    override fun count(): Long = jpaRepository.count()
}
