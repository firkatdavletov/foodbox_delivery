package ru.foodbox.delivery.modules.orders.application.event

import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderStatus

data class OrderStatusChangedEvent(
    val order: Order,
    val previousStatus: OrderStatus,
)
