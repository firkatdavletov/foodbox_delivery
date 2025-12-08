package ru.foodbox.delivery.controllers.user.body

import ru.foodbox.delivery.controllers.base.ResponseModel
import ru.foodbox.delivery.services.dto.UserDto

data class GetUserResponseBody(
    val user: UserDto?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?,
) : ResponseModel {
    constructor(user: UserDto) : this(user, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}
