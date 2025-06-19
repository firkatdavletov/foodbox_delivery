package ru.foodbox.delivery.services.dto

data class TokenPairDto(
    val access: String,
    val refresh: String,
)