package ru.foodbox.delivery.modules.cart.application.command

import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.util.UUID

data class UpdateCartDeliveryCommand(
    val deliveryMethod: DeliveryMethodType,
    val deliveryAddress: DeliveryAddress?,
    val pickupPointId: UUID?,
    val pickupPointExternalId: String?,
)
