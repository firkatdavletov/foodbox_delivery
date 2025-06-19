package ru.foodbox.delivery.services.dto

import java.math.BigDecimal

data class CartItemDto(
    val productId: Long,
    val title: String,
    val quantity: Int,
    val price: Double,
)