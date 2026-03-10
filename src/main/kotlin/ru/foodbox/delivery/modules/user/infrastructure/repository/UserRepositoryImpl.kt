package ru.foodbox.delivery.modules.user.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.user.domain.User
import ru.foodbox.delivery.modules.user.domain.repository.UserRepository
import ru.foodbox.delivery.modules.user.infrastructure.mapper.UserMapper
import ru.foodbox.delivery.modules.user.infrastructure.persistance.entity.UserEntity
import ru.foodbox.delivery.modules.user.infrastructure.persistance.jpa.UserJpaRepository
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    override fun create(user: User): User {
        val existing = jpaRepository.findById(user.id).getOrNull()
        val entity = existing ?: UserEntity(
            id = user.id,
            email = user.email,
            phone = user.phone,
            login = user.login,
            name = user.name,
            company = user.company,
            status = "CREATED",
            createdAt = Instant.now(),
        )
        val saved = jpaRepository.save(entity)
        return UserMapper.map(saved)
    }
}