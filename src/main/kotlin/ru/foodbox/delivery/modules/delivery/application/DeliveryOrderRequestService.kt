package ru.foodbox.delivery.modules.delivery.application

import ru.foodbox.delivery.modules.orders.domain.Order

interface DeliveryOrderRequestService {
    fun createAndConfirm(order: Order): DeliveryOrderRequestConfirmation?
}

data class DeliveryOrderRequestConfirmation(
    val externalOfferId: String,
    val externalRequestId: String,
    val deliveryFeeMinor: Long,
    val currency: String,
)
