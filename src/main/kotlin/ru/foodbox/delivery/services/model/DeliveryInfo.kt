package ru.foodbox.delivery.services.model

import java.math.BigDecimal

data class DeliveryInfo(
    val deliveryPrice: BigDecimal,
    val freeDeliveryPrice: BigDecimal?,
)
