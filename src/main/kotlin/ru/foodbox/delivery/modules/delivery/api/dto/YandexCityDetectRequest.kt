package ru.foodbox.delivery.modules.delivery.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

data class YandexCityDetectRequest(
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double,

    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double,
)

data class YandexCityDetectResponse(
    val city: String?,
)
