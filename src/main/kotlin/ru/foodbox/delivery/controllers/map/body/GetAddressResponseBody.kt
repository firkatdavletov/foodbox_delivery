package ru.foodbox.delivery.controllers.map.body

import ru.foodbox.delivery.services.dto.GeoAddressDto

data class GetAddressResponseBody(
    val address: GeoAddressDto
)
