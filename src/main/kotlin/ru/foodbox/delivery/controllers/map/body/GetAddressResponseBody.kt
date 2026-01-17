package ru.foodbox.delivery.controllers.map.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.GeoAddressDto

class GetAddressResponseBody(
    val address: GeoAddressDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(geoAddress: GeoAddressDto) : this(address = geoAddress, true, null, null)
    constructor(message: String, errorCode: Int) : this(null, false, message, errorCode)
}
