package ru.foodbox.delivery.services.dto

data class AddressDto(
    val city: CityDto,
    val street: String,
    val house: String,
    val entrance: Int? = null,
    val flat: Int? = null,
    val intercome: String? = null,
    val comment: String? = null,
    val latitude: Double,
    val longitude: Double,
)