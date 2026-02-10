package ru.foodbox.delivery.services.dto

import ru.foodbox.delivery.services.model.UnitOfMeasure

data class CartItemDto(
    val productId: Long,
    val title: String,
    val quantity: Int,
    val price: Long,
    val countStep: Int,
    val unit: UnitOfMeasure
)