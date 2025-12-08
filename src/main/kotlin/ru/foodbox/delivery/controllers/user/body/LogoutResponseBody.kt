package ru.foodbox.delivery.controllers.user.body

import ru.foodbox.delivery.controllers.base.ResponseModel

class LogoutResponseBody(
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel