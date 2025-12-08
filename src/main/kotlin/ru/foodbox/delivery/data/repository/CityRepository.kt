package ru.foodbox.delivery.data.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.data.entities.CityEntity

interface CityRepository : JpaRepository<CityEntity, Long> {
    fun findByName(name: String): CityEntity?
}