package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.CartEntity

interface CartRepository : JpaRepository<CartEntity, Long> {
    fun findByUserId(userId: Long): CartEntity?
    fun existsByUserId(userId: Long): Boolean
}