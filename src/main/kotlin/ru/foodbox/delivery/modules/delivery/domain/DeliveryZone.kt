package ru.foodbox.delivery.modules.delivery.domain

import org.locationtech.jts.geom.MultiPolygon
import java.util.UUID

data class DeliveryZone(
    val id: UUID,
    val code: String,
    val name: String,
    val type: DeliveryZoneType,
    val city: String?,
    val normalizedCity: String?,
    val postalCode: String?,
    val geometry: MultiPolygon?,
    val priority: Int,
    val active: Boolean,
)
