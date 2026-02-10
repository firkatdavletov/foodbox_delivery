package ru.foodbox.delivery.controllers.cart.body

import java.math.BigDecimal

data class UpdateQuantityRequestBody(
    val productId: Long,
    val quantity: Int
)