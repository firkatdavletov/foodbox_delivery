package ru.foodbox.delivery.modules.admin.auth.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole

data class CreateAdminUserRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val login: String,

    @field:NotBlank
    @field:Size(min = 8, max = 255)
    val password: String,

    val role: AdminRole,

    val active: Boolean = true,
)
