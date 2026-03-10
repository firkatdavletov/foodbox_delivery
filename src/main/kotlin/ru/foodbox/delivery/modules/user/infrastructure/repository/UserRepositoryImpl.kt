package ru.foodbox.delivery.modules.user.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.user.domain.User
import ru.foodbox.delivery.modules.user.domain.repository.UserRepository
import ru.foodbox.delivery.modules.user.infrastructure.mapper.UserMapper
import ru.foodbox.delivery.modules.user.infrastructure.persistence.entity.UserEntity
import ru.foodbox.delivery.modules.user.infrastructure.persistence.jpa.UserJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    override fun create(user: User): User {
        val existing = jpaRepository.findById(user.id).getOrNull()
        val entity = existing ?: UserEntity(
            id = user.id,
            status = "CREATED",
            createdAt = Instant.now(),
        )
        entity.email = user.email
        entity.phone = user.phone
        entity.login = user.login
        entity.name = user.name
        entity.company = user.company
        val saved = jpaRepository.save(entity)
        return UserMapper.map(saved)
    }

    override fun findById(id: UUID): User? {
        val entity = jpaRepository.findById(id).getOrNull() ?: return null
        return UserMapper.map(entity)
    }
}
