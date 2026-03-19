package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import java.util.UUID

interface PickupPointRepository {
    fun findById(id: UUID): PickupPoint?
    fun findActiveById(id: UUID): PickupPoint?
    fun findAllActive(): List<PickupPoint>
}
