package ru.foodbox.delivery.controllers.cart.body

import ru.foodbox.delivery.services.dto.CartDto

data class GetCartResponseBody(
    val cart: CartDto,
)
