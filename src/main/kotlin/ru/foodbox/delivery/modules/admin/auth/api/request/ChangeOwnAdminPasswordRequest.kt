package ru.foodbox.delivery.modules.admin.auth.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangeOwnAdminPasswordRequest(
    @field:NotBlank
    val currentPassword: String,

    @field:NotBlank
    @field:Size(min = 8, max = 255)
    val newPassword: String,
)
