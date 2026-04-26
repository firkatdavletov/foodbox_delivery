package ru.foodbox.delivery.modules.admin.auth.application.command

data class ChangeOwnAdminPasswordCommand(
    val currentPassword: String,
    val newPassword: String,
)
