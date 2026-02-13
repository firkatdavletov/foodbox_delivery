package ru.foodbox.delivery.controllers.auth.body

data class RegisterRequestBody(
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
)
