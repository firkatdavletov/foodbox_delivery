package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.UserEntity

interface UserRepository: JpaRepository<UserEntity, Long> {
    fun findByPhone(phone: String): UserEntity?
}