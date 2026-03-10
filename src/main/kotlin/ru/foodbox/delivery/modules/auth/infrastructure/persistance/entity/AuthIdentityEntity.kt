package ru.foodbox.delivery.modules.auth.infrastructure.persistance.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import ru.foodbox.delivery.modules.auth.domain.IdentityType
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "auth_identity",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_auth_identity_type_external", columnNames = ["type", "external_id"])
    ]
)
class AuthIdentityEntity(

    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var type: IdentityType,

    @Column(name = "external_id", nullable = false, length = 255)
    var externalId: String,

    @Column(name = "normalized_login", length = 255)
    var normalizedLogin: String?,

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null
)