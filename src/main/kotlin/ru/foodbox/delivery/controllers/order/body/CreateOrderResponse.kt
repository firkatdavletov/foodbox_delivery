package ru.foodbox.delivery.controllers.order.body

import ru.foodbox.delivery.services.dto.OrderDto

data class CreateOrderResponse(
    val order: OrderDto,
)