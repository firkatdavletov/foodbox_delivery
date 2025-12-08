package ru.foodbox.delivery.controllers.websockets.model

import ru.foodbox.delivery.data.entities.OrderStatus

data class UserOrdersStatusUpdate(
    val update: OrderStatusUpdate
)

data class OrderStatusUpdate(
    val orderId: Long,
    val status: OrderStatus,
)
