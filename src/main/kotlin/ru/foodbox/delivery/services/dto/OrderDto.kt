package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.data.entities.OrderStatus

data class OrderDto(
    val id: Long,
    val status: OrderStatus,
    val items: List<OrderItemDto>,
    val deliveryPrice: Double,
    val totalAmount: Double,
)
