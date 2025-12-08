package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "addresses")
class AddressEntity(
    @ManyToOne
    var cityEntity: CityEntity,

    @Column(nullable = false)
    var street: String,

    @Column(nullable = false)
    var house: String,

    var entrance: Int?,

    var flat: Int?,

    var intercome: String?,

    var comment: String?,

    @ManyToOne
    val user: UserEntity? = null,

    val latitude: Double,

    val longitude: Double
) : BaseEntity<Long>()