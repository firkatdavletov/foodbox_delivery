package ru.foodbox.delivery.data.entities

import jakarta.persistence.*
import ru.foodbox.delivery.services.model.UserRole

@Entity
@Table(name = "users")
class UserEntity(
    @Column(unique = true)
    val phone: String,

    @Column(unique = true)
    var email: String = "",

    var name: String = "",

    var company: String = "",

    @Column(name = "hash_password")
    var hashPassword: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole,

    @OneToMany(
        mappedBy = "user",
        cascade = [CascadeType.REMOVE], // или ALL, если нужно каскадировать всё
        orphanRemoval = true
    )
    val orders: MutableList<OrderEntity> = mutableListOf()
) : BaseAuditEntity<Long>()
