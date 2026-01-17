package ru.foodbox.delivery.services.dto

data class BankDto(
    val bankName: String,
    val logoUrl: String,
    val schema: String,
    val packageName: String?,
)
