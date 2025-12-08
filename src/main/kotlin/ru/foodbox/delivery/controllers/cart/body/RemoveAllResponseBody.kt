package ru.foodbox.delivery.controllers.cart.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.CartDto

data class RemoveAllResponseBody(
    val cart: CartDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(cart: CartDto) : this(cart, true, null, null)
    constructor(error: String, code: Int) : this(null, true, error, code)
}
