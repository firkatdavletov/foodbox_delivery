package ru.foodbox.delivery.modules.delivery.domain

data class YandexPickupPointOption(
    val id: String,
    val name: String,
    val address: String,
    val fullAddress: String? = null,
    val instruction: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val paymentMethods: List<String> = emptyList(),
    val isYandexBranded: Boolean = false,
)
