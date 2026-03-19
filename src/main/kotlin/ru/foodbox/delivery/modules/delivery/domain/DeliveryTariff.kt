package ru.foodbox.delivery.modules.delivery.domain

import java.util.UUID

data class DeliveryTariff(
    val id: UUID,
    val method: DeliveryMethodType,
    val zone: DeliveryZone?,
    val available: Boolean,
    val fixedPriceMinor: Long,
    val freeFromAmountMinor: Long?,
    val currency: String,
    val estimatedDays: Int?,
)
