package ru.foodbox.delivery.controllers.user.body

import ru.foodbox.delivery.services.dto.UserDto

data class UpdateUserResponseBody(
    val user: UserDto
)
