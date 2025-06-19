package ru.foodbox.delivery.controllers.auth.body

data class SendSmsResponseBody(
    val status: Int,
    val success: Boolean,
    val message: String
)
