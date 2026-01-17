package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.services.model.DeliveryInfo

data class GeoAddressDto(
    val city: CityDto,
    val street: String,
    val house: String,
    val entrance: Int?,
    val deliveryInfo: DeliveryInfo?,
    val deliveryTime: Int,
    val latitude: Double,
    val longitude: Double,
    val uri: String?,
)