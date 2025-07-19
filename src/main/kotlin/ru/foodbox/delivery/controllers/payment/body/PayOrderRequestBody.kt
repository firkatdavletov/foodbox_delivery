package ru.foodbox.delivery.controllers.payment.body

data class PayOrderRequestBody(
    val paymentType: String,
    val amount: Double,
    val token: String?,
    val cryptogram: String?,
)
