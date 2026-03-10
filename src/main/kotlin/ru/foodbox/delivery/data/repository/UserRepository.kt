package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.user.infrastructure.persistance.entity.UserEntity

interface UserJpaRepository: JpaRepository<UserEntity, Long> {
    fun findByPhone(phone: String): UserEntity?
    fun findByEmailIgnoreCase(email: String): UserEntity?
}
