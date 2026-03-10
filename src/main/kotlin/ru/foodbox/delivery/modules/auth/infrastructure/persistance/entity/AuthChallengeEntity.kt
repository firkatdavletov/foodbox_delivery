package ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity

import jakarta.persistence.*
import ru.foodbox.delivery.modules.auth.domain.AuthChallengeStatus
import ru.foodbox.delivery.modules.auth.domain.AuthMethod
import java.time.Instant
import java.util.*

@Entity
@Table(name = "auth_challenge")
class AuthChallengeEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var method: AuthMethod,

    @Column(nullable = false, length = 255)
    var target: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: AuthChallengeStatus,

    @Column(name = "code_hash", length = 255)
    var codeHash: String?,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "attempts_left", nullable = false)
    var attemptsLeft: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "completed_at")
    var completedAt: Instant? = null
)