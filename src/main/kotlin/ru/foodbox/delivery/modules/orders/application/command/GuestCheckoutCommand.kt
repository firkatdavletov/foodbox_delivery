package ru.foodbox.delivery.modules.orders.application.command

import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType

data class GuestCheckoutCommand(
    val items: List<GuestCheckoutItemCommand>,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val deliveryMethod: DeliveryMethodType,
    val deliveryAddress: DeliveryAddress?,
    val pickupPointId: java.util.UUID?,
    val pickupPointExternalId: String?,
    val comment: String?,
)

data class GuestCheckoutItemCommand(
    val productId: java.util.UUID,
    val variantId: java.util.UUID?,
    val quantity: Int,
)
