package ru.foodbox.delivery.services.dto

data class GeoAddressDto(
    val city: String?,
    val street: String?,
    val house: String?,
    val deliveryPrice: Double,
    val deliveryTime: Int,
    val latitude: Double?,
    val longitude: Double?,
)