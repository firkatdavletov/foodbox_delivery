package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.controllers.base.ResponseModel

class CartTokenResponseBody(
    val token: String?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(token: String): this(token, true, null, null)
    constructor(error: String, code: Int): this(null, false, error, code)
}