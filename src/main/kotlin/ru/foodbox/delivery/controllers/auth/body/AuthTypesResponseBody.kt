package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.AuthTypeDto

data class AuthTypesResponseBody(
    val types: List<AuthTypeDto>?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(types: List<AuthTypeDto>) : this(types, true, null, null)
    constructor(error: String?, code: Int?) : this(null, false, error, code)
}
