package ru.foodbox.delivery.modules.user.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.user.infrastructure.persistence.entity.UserEntity
import java.util.UUID

interface UserJpaRepository : JpaRepository<UserEntity, UUID>
{
    fun findByPhone(phone: String): UserEntity?
    fun findByEmailIgnoreCase(email: String): UserEntity?
}
