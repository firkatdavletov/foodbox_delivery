package ru.foodbox.delivery.data.entities

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val phone: String,

    val name: String = "",

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
    val cart: CartEntity? = null,
)