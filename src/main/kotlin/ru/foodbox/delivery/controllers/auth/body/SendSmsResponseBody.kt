package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.controllers.base.ResponseModel

data class SendSmsResponseBody(
    val status: Int,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?,
) : ResponseModel {
    constructor(status: Int) : this(status, true, null, null)
    constructor(error: String?, code: Int?) : this(100, false, error, code)
}


