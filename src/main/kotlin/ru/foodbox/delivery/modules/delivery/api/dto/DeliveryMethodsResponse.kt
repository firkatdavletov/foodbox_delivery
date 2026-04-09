package ru.foodbox.delivery.modules.delivery.api.dto

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.util.UUID

data class DeliveryMethodsResponse(
    val methods: List<DeliveryMethodResponse>,
    val pickupPoints: List<PickupPointResponse>,
)

data class PickupPointsResponse(
    val pickupPoints: List<PickupPointResponse>,
)

data class DeliveryMethodResponse(
    val code: DeliveryMethodType,
    val name: String,
    val description: String?,
    val requiresAddress: Boolean,
    val requiresPickupPoint: Boolean,
)

data class PickupPointResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val address: DeliveryAddressResponse,
    val isActive: Boolean,
)
