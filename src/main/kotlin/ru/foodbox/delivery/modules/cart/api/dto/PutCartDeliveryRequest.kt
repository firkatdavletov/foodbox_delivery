package ru.foodbox.delivery.modules.cart.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryAddressRequest
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.util.UUID

data class PutCartDeliveryRequest(
    @field:NotNull
    val deliveryMethod: DeliveryMethodType,

    @field:Valid
    val address: DeliveryAddressRequest? = null,

    val pickupPointId: UUID? = null,
    val pickupPointExternalId: String? = null,
)
