package ru.foodbox.delivery.modules.delivery.api.dto

import jakarta.validation.constraints.Min

data class YandexPickupPointsRequest(
    @field:Min(0)
    val geoId: Long,
)

data class YandexPickupPointsResponse(
    val points: List<YandexPickupPointResponse>,
)

data class YandexPickupPointResponse(
    val id: String,
    val name: String,
    val address: String,
    val fullAddress: String?,
    val instruction: String?,
    val latitude: Double?,
    val longitude: Double?,
    val paymentMethods: List<String>,
    val isYandexBranded: Boolean,
)
