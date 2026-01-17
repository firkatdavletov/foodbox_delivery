package ru.foodbox.delivery.controllers.cart.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.CartDto

class GetCartResponseBody(
    val cart: CartDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?,
): ResponseModel {
    constructor(cart: CartDto) : this(cart, true, null, null)
    constructor(message: String, errorCode: Int) : this(null, false, message, errorCode)
}
