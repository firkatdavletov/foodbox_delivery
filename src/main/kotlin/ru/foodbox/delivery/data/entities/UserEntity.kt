package ru.foodbox.delivery.data.entities

import jakarta.persistence.*

@Entity
@Table(name = "users")
class UserEntity(
    @Column(unique = true)
    val phone: String,

    var email: String = "",

    var name: String = "",

    @OneToMany(
        mappedBy = "user",
        cascade = [CascadeType.REMOVE], // или ALL, если нужно каскадировать всё
        orphanRemoval = true
    )
    val orders: MutableList<OrderEntity> = mutableListOf()
) : BaseAuditEntity<Long>()