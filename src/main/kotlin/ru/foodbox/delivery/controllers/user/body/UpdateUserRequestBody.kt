package ru.foodbox.delivery.controllers.user.body

import ru.foodbox.delivery.modules.user.domain.User

data class UpdateUserRequestBody(
    val user: User
)
