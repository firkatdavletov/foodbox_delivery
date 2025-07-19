package ru.foodbox.delivery.services.dto

data class PaymentDto(
    val success: Boolean,
    val message:  String? = null,
    val model: PaymentModelDto? = null,
    val paymentType: String,
)
