package ru.foodbox.delivery.modules.delivery.domain

import java.util.UUID

data class DeliveryQuote(
    val deliveryMethod: DeliveryMethodType,
    val available: Boolean,
    val priceMinor: Long?,
    val currency: String,
    val zoneCode: String? = null,
    val zoneName: String? = null,
    val estimatedDays: Int? = null,
    val message: String? = null,
    val pickupPointId: UUID? = null,
    val pickupPointExternalId: String? = null,
    val pickupPointName: String? = null,
    val pickupPointAddress: String? = null,
)
