package ru.foodbox.delivery.controllers.admin.body

import ru.foodbox.delivery.controllers.base.ResponseModel

class ImportFileResponseBody(
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
}