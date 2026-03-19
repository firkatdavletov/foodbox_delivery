package ru.foodbox.delivery.modules.delivery.domain

import java.util.UUID

data class PickupPoint(
    val id: UUID,
    val code: String,
    val name: String,
    val address: DeliveryAddress,
    val active: Boolean,
)
