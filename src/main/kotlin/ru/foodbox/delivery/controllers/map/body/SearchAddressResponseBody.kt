package ru.foodbox.delivery.controllers.map.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.GeoAddressDto

data class SearchAddressResponseBody(
    val addresses: List<GeoAddressDto>?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?,
) : ResponseModel
