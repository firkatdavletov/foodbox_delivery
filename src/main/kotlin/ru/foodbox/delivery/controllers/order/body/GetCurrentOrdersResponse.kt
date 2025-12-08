package ru.foodbox.delivery.controllers.order.body

import ru.foodbox.delivery.services.dto.OrderDto

data class GetCurrentOrdersResponse(
    val orders: List<OrderDto>,
)