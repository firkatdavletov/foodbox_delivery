package ru.foodbox.delivery.modules.delivery.api.dto

import jakarta.validation.constraints.NotBlank

data class YandexLocationDetectRequest(
    @field:NotBlank
    val query: String,
)

data class YandexLocationDetectResponse(
    val variants: List<YandexLocationVariantResponse>,
)

data class YandexLocationVariantResponse(
    val geoId: Long,
    val address: String,
)
