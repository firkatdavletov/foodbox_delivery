package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import java.util.UUID

interface PickupPointRepository {
    fun findAll(): List<PickupPoint>
    fun findAllByIsActive(isActive: Boolean): List<PickupPoint>
    fun findById(id: UUID): PickupPoint?
    fun findByCode(code: String): PickupPoint?
    fun save(point: PickupPoint): PickupPoint
    fun findActiveById(id: UUID): PickupPoint?
    fun findAllActive(): List<PickupPoint>
}
