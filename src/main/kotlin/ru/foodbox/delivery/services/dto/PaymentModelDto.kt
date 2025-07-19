package ru.foodbox.delivery.services.dto

data class PaymentModelDto(
    val qrUrl: String? = null,
    val orderId: Long?,
    val banks: List<BankInfoDto>? = null
)
