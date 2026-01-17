package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.model.CallPhoneModel

class AuthByCallResponseModel(
    val callPhone: CallPhoneModel?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(callPhone: CallPhoneModel) : this(callPhone, true, null, null)
    constructor(error: String?, code: Int) : this(null, false, error, code)
}