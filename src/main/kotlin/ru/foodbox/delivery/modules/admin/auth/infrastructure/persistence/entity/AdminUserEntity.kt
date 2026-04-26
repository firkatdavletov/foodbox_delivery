package ru.foodbox.delivery.modules.admin.auth.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "admin_user")
class AdminUserEntity(

    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "login", nullable = false, unique = true, length = 255)
    var login: String,

    @Column(name = "normalized_login", nullable = false, unique = true, length = 255)
    var normalizedLogin: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 64)
    var role: AdminRole,

    @Column(name = "is_active", nullable = false)
    var active: Boolean,

    @Column(name = "deleted_at")
    var deletedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
