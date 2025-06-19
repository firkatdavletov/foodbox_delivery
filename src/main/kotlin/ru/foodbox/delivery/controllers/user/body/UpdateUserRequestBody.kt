package ru.foodbox.delivery.controllers.user.body

import ru.foodbox.delivery.services.dto.UserDto

data class UpdateUserRequestBody(
    val user: UserDto
)
