package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.services.model.UnitOfMeasure

data class OrderItemDto(
    val productId: Long,
    val imageUrl: String?,
    val unit: UnitOfMeasure,
    val name: String,
    val quantity: Int,
    val price: Long,
    val totalPrice: Long,
)