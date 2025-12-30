package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.controllers.base.ResponseModel
class DeleteProductResponseBody(
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor() : this(true, null, null)
    constructor(error: String, code: Int?) : this(false, error, code)
}