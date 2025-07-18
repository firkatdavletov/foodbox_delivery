package ru.foodbox.delivery.data.entities

import jakarta.persistence.*

@Entity
@Table(name = "auth_types")
data class AuthTypeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val name: String,
)