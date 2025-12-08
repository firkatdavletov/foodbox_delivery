package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "cities")
class CityEntity(
    var name: String,

    @Column(name = "can_deliver")
    var canDeliver: Boolean = true,

    var latitude: Double,

    var longitude: Double,

    @OneToMany
    @Column(name = "sub_cities")
    var subCities: List<CityEntity> = emptyList()
) : BaseEntity<Long>()