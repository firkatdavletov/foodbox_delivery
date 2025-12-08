package ru.foodbox.delivery.services.dto

data class CityDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val subCities: List<CityDto>,
)
