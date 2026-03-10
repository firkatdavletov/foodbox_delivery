package ru.foodbox.delivery.controllers.admin.body

import ru.foodbox.delivery.common.utils.ResponseModel
import ru.foodbox.delivery.services.dto.CategoryDto

class SaveCategoryResponseBody(
    val category: CategoryDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(categoryDto: CategoryDto) : this(categoryDto, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}