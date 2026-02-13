package ru.foodbox.delivery.controllers.auth.body

data class LoginByEmailRequestBody(
    val email: String,
    val password: String,
)
