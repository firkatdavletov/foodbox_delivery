package ru.foodbox.delivery.controllers.admin.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.CategoryDto
import ru.foodbox.delivery.services.dto.ProductDto

class SaveProductResponseBody(
    val product: ProductDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(productDto: ProductDto) : this(productDto, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}