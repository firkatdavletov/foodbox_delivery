package ru.foodbox.delivery.modules.user.infrastructure.persistance.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    val phone: String? = null,

    var login: String? = null,

    var email: String? = null,

    var name: String? = null,

    var company: String? = null,

    @Column(name = "hash_password")
    var hashPassword: String? = null,

    @Column(nullable = false, length = 32)
    var status: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant
)