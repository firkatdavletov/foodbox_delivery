package ru.foodbox.delivery.services.dto

data class BankInfoDto(
    val bankName: String,
    val logoUrl: String,
    val schema: String,
    val packageName: String?,
    val webClientUrl: String?,
    val isWebClientActive: String?
)
