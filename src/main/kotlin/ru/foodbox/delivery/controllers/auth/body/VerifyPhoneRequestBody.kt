package ru.foodbox.delivery.controllers.auth.body

data class VerifyPhoneRequestBody(
    val phone: String,
    val code: String,
)