package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.ProductDto

class GetProductResponseBody(
    val product: ProductDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(product: ProductDto) : this(product, true, null, null)
    constructor(error: String?, code: Int?) : this(null, false, error, code)
}