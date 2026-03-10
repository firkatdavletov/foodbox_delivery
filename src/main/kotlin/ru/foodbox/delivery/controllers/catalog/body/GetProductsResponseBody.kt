package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.common.utils.ResponseModel
import ru.foodbox.delivery.services.dto.ProductDto

class GetProductsResponseBody(
    val products: List<ProductDto>,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(products: List<ProductDto>) : this(products, true, null, null)
}