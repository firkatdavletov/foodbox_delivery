package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "addresses")
data class AddressEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val lat: Double,

    @Column(nullable = false)
    val long: Double,

    @Column(nullable = false)
    val city: String,

    @Column(nullable = false)
    val street: String,

    @Column(nullable = false)
    val house: String,

    val flat: String?,

    val intercome: String?,

    val comment: String?,

    @ManyToOne
    val user: UserEntity? = null
)