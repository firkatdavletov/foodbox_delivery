package ru.foodbox.delivery.modules.orders.api.dto

import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.orders.domain.OrderDeliveryType

data class CheckoutRequest(
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerEmail: String? = null,

    @field:NotNull
    val deliveryType: OrderDeliveryType,

    val deliveryAddress: String? = null,
    val comment: String? = null,
)
