package ru.foodbox.delivery.modules.orders.application.command

import ru.foodbox.delivery.modules.orders.domain.OrderDeliveryType

data class CheckoutCommand(
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?,
    val deliveryType: OrderDeliveryType,
    val deliveryAddress: String?,
    val comment: String?,
)
