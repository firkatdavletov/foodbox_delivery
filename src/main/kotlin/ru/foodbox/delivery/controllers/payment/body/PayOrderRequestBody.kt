package ru.foodbox.delivery.controllers.payment.body

data class PayOrderRequestBody(
    val paymentType: String,
    val token: String?,
    val cryptogram: String?,
)
