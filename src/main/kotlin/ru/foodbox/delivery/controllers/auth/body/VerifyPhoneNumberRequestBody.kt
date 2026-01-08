package ru.foodbox.delivery.controllers.auth.body

data class VerifyPhoneNumberRequestBody(
    val phone: String,
    val type: String
)