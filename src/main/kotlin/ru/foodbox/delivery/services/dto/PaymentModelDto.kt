package ru.foodbox.delivery.services.dto

data class PaymentModelDto(
    val qrUrl: String? = null,
    val banks: List<BankInfoDto>? = null
)
