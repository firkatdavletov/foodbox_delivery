package ru.foodbox.delivery.services.dto

data class OrderItemDto(
    val productId: Long,
    val name: String,
    val quantity: Int,
    val price: Float,
)