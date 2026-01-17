package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.controllers.base.ResponseModel

data class VerifyPhoneNumberResponseBody(
    val status: Int?,
    val checkId: String?,
    val callPhone: String?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?,
) : ResponseModel {
    constructor(status: Int, checkId: String?, callPhone: String?) : this(status, checkId, callPhone, true, null, null,)
    constructor(error: String?, code: Int?) : this(code, null, null, false, error, code)
}


