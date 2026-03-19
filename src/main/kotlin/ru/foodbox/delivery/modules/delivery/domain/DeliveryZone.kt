package ru.foodbox.delivery.modules.delivery.domain

import java.util.UUID

data class DeliveryZone(
    val id: UUID,
    val code: String,
    val name: String,
    val city: String?,
    val postalCode: String?,
    val active: Boolean,
)
