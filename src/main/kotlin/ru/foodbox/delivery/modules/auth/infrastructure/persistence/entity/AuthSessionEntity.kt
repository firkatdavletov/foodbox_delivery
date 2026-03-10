package ru.foodbox.delivery.modules.auth.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auth_session")
class AuthSessionEntity(

    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 255)
    var refreshTokenHash: String,

    @Column(name = "device_id", length = 255)
    var deviceId: String?,

    @Column(name = "user_agent", length = 1000)
    var userAgent: String?,

    @Column(name = "ip", length = 64)
    var ip: String?,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "last_used_at", nullable = false)
    var lastUsedAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
)