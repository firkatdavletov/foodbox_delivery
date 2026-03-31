package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import java.util.UUID

interface DeliveryZoneRepository {
    fun findAll(): List<DeliveryZone>
    fun findAllByIsActive(isActive: Boolean): List<DeliveryZone>
    fun findById(id: UUID): DeliveryZone?
    fun findByCode(code: String): DeliveryZone?
    fun save(zone: DeliveryZone): DeliveryZone
    fun findActiveByCity(city: String): DeliveryZone?
    fun findActiveByPostalCode(postalCode: String): DeliveryZone?
}
