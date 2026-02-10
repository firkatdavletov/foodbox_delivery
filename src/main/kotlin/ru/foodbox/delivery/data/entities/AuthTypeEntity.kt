package ru.foodbox.delivery.data.entities

import jakarta.persistence.*

@Entity
@Table(name = "auth_types")
class AuthTypeEntity(
    @Column(nullable = false)
    val key: String,

    val title: String,

) : BaseEntity<Long>()