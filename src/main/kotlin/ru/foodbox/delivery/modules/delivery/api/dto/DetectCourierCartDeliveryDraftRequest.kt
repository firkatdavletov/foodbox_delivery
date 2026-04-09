package ru.foodbox.delivery.modules.delivery.api.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType

data class DetectCourierCartDeliveryDraftRequest(
    @field:NotNull
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double,

    @field:NotNull
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double,

    @field:NotNull
    val deliveryMethod: DeliveryMethodType,
)
