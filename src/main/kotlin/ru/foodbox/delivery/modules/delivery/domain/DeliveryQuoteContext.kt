package ru.foodbox.delivery.modules.delivery.domain

import java.util.UUID

data class DeliveryQuoteContext(
    val cartId: UUID? = null,
    val subtotalMinor: Long,
    val itemCount: Int,
    val deliveryMethod: DeliveryMethodType,
    val deliveryAddress: DeliveryAddress? = null,
    val pickupPointId: UUID? = null,
    val pickupPointExternalId: String? = null,
    val totalWeightGrams: Long? = null,
    val totalVolumeCm3: Long? = null,
)
