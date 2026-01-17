package ru.foodbox.delivery.controllers.cart.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.CartDto

class UpdateQuantityResponseBody(
    val cart: CartDto,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel
