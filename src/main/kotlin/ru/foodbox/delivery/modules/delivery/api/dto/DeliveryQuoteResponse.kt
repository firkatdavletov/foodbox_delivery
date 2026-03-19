package ru.foodbox.delivery.modules.delivery.api.dto

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.util.UUID

data class DeliveryQuoteResponse(
    val deliveryMethod: DeliveryMethodType,
    val available: Boolean,
    val priceMinor: Long?,
    val currency: String,
    val zoneCode: String?,
    val zoneName: String?,
    val estimatedDays: Int?,
    val message: String?,
    val pickupPointId: UUID?,
    val pickupPointExternalId: String?,
    val pickupPointName: String?,
    val pickupPointAddress: String?,
)
