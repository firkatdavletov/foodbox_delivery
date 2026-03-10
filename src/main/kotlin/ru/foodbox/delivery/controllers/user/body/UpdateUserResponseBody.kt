package ru.foodbox.delivery.controllers.user.body

import ru.foodbox.delivery.common.utils.ResponseModel
import ru.foodbox.delivery.modules.user.domain.User

class UpdateUserResponseBody(
    val user: User?,
    override val success: Boolean,
    override val error: String?,
    override val code: Int?
) : ResponseModel {
    constructor(user: User) : this(user, true, null, null)
    constructor(error: String, code: Int) : this(null, false, error, code)
}
