package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.common.utils.ResponseModel
import ru.foodbox.delivery.services.dto.CategoryDto

data class GetCategoriesResponseBody(
    val catalog: List<CategoryDto>?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(categories: List<CategoryDto>) : this(categories, true, null, null)
    constructor(message: String, errorCode: Int) : this(null, false, message, errorCode)
}
