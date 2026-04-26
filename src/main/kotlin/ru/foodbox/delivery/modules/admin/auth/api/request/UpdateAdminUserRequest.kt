package ru.foodbox.delivery.modules.admin.auth.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.foodbox.delivery.modules.admin.auth.domain.AdminRole

data class UpdateAdminUserRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val login: String,

    val role: AdminRole,

    val active: Boolean,
)
