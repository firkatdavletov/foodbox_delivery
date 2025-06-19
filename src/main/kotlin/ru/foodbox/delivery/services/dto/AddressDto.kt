package ru.foodbox.delivery.services.dto

data class AddressDto(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val street: String,
    val house: String,
    val flat: String?,
    val intercome: String?,
    val comment: String?
)