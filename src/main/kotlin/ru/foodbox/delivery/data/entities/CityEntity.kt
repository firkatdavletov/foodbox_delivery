package ru.foodbox.delivery.data.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_city_id")
    var parentCity: CityEntity? = null,

    @OneToMany(mappedBy = "parentCity")
    var subCities: List<CityEntity> = emptyList()
) : BaseEntity<Long>()