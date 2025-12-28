package ru.foodbox.delivery.controllers.catalog.body

import ru.foodbox.delivery.controllers.base.BaseResponseModel
import ru.foodbox.delivery.services.dto.CategoryDto

class CreateCategoryResponseBody(
    val category: CategoryDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?,
) : BaseResponseModel<CategoryDto>(category, success, error, code) {
    constructor(category: CategoryDto): this(category, true, null, null)
    constructor(error: String?, code: Int?): this(null, false, error, code)
}
