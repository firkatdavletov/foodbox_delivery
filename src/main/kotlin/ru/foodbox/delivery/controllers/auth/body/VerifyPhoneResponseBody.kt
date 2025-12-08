package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.TokenPairDto

data class VerifyPhoneResponseBody(
    val tokens: TokenPairDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(tokens: TokenPairDto) : this(tokens, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}
