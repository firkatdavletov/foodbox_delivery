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
class RefreshTokenEntity(
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
    @Column(name = "hashed_token", nullable = false)
    var hashedToken: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity<Long>()