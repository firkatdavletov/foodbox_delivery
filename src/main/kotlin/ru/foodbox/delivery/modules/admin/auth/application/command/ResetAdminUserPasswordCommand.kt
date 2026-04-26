package ru.foodbox.delivery.modules.admin.auth.application.command

data class ResetAdminUserPasswordCommand(
    val password: String,
)
