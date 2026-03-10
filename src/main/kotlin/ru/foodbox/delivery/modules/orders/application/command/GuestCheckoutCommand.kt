package ru.foodbox.delivery.modules.orders.application.command

import ru.foodbox.delivery.modules.orders.domain.OrderDeliveryType

data class GuestCheckoutCommand(
    val items: List<GuestCheckoutItemCommand>,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val deliveryType: OrderDeliveryType,
    val deliveryAddress: String?,
    val comment: String?,
)

data class GuestCheckoutItemCommand(
    val productId: java.util.UUID,
    val quantity: Int,
)
