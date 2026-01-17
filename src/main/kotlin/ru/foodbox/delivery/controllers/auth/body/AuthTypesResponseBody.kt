package ru.foodbox.delivery.controllers.auth.body

import ru.foodbox.delivery.controllers.base.ResponseModel

data class AuthTypesResponseBody(
    val types: List<String>,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(types: List<String>) : this(types, true, null, null)
    constructor(error: String?, code: Int?) : this(emptyList(), false, error, code)
}
