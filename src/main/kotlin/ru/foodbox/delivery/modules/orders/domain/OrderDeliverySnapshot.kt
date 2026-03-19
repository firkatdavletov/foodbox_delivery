package ru.foodbox.delivery.modules.orders.domain

import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.util.UUID

data class OrderDeliverySnapshot(
    val method: DeliveryMethodType,
    val methodName: String,
    val priceMinor: Long,
    val currency: String,
    val zoneCode: String?,
    val zoneName: String?,
    val estimatedDays: Int?,
    val pickupPointId: UUID?,
    val pickupPointExternalId: String?,
    val pickupPointName: String?,
    val pickupPointAddress: String?,
    val address: DeliveryAddress?,
)
