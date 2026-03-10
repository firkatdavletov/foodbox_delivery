package ru.foodbox.delivery.modules.orders.application.event

import ru.foodbox.delivery.modules.orders.domain.Order

data class OrderCreatedEvent(
    val order: Order,
)
