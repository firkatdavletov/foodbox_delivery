package ru.foodbox.delivery.modules.delivery.api.dto

import jakarta.validation.constraints.Size
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress

data class DeliveryAddressRequest(
    @field:Size(max = 255)
    val country: String? = null,

    @field:Size(max = 255)
    val region: String? = null,

    @field:Size(max = 255)
    val city: String? = null,

    @field:Size(max = 255)
    val street: String? = null,

    @field:Size(max = 255)
    val house: String? = null,

    @field:Size(max = 255)
    val apartment: String? = null,

    @field:Size(max = 64)
    val postalCode: String? = null,

    @field:Size(max = 255)
    val entrance: String? = null,

    @field:Size(max = 255)
    val floor: String? = null,

    @field:Size(max = 255)
    val intercom: String? = null,

    @field:Size(max = 500)
    val comment: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class DeliveryAddressResponse(
    val country: String?,
    val region: String?,
    val city: String?,
    val street: String?,
    val house: String?,
    val apartment: String?,
    val postalCode: String?,
    val entrance: String?,
    val floor: String?,
    val intercom: String?,
    val comment: String?,
    val latitude: Double?,
    val longitude: Double?,
)

fun DeliveryAddressRequest.toDomain(): DeliveryAddress {
    return DeliveryAddress(
        country = country,
        region = region,
        city = city,
        street = street,
        house = house,
        apartment = apartment,
        postalCode = postalCode,
        entrance = entrance,
        floor = floor,
        intercom = intercom,
        comment = comment,
        latitude = latitude,
        longitude = longitude,
    ).normalized()
}

fun DeliveryAddress.toResponse(): DeliveryAddressResponse {
    return DeliveryAddressResponse(
        country = country,
        region = region,
        city = city,
        street = street,
        house = house,
        apartment = apartment,
        postalCode = postalCode,
        entrance = entrance,
        floor = floor,
        intercom = intercom,
        comment = comment,
        latitude = latitude,
        longitude = longitude,
    )
}
