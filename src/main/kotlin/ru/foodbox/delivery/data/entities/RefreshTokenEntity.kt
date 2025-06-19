package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
data class RefreshTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
    @Column(name = "hashed_token", nullable = false)
    val hashedToken: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)