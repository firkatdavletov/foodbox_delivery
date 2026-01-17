package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.RefreshTokenEntity

interface RefreshTokenRepository: JpaRepository<RefreshTokenEntity, Long> {
    fun findByUserId(userId: Long): RefreshTokenEntity?

    fun findByUserIdAndHashedToken(userId: Long, hashedToken: String): RefreshTokenEntity?

    fun deleteByUserIdAndHashedToken(userId: Long, hashedToken: String)

    fun deleteByUserId(userId: Long)
}