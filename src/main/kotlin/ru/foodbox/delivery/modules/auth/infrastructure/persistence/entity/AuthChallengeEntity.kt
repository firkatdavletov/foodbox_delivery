package ru.foodbox.delivery.modules.auth.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.auth.domain.AuthChallengeStatus
import ru.foodbox.delivery.modules.auth.domain.AuthMethod
import java.time.Instant
import java.util.UUID

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
