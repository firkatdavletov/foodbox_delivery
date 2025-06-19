package ru.foodbox.delivery.controllers.cart.body

import ru.foodbox.delivery.services.dto.CartDto

data class UpdateQuantityResponseBody(
    val cart: CartDto
)
