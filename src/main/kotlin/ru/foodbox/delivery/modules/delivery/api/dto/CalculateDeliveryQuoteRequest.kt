package ru.foodbox.delivery.modules.delivery.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.util.UUID

data class CalculateDeliveryQuoteRequest(
    @field:Min(0)
    val subtotalMinor: Long,

    @field:Min(1)
    val itemCount: Int,

    @field:NotNull
    val deliveryMethod: DeliveryMethodType,

    @field:Valid
    val address: DeliveryAddressRequest? = null,

    val pickupPointId: UUID? = null,
    val pickupPointExternalId: String? = null,
)
