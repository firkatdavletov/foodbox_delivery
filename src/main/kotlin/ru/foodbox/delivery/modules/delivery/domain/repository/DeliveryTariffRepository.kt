package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import java.util.UUID

interface DeliveryTariffRepository {
    fun findAll(): List<DeliveryTariff>
    fun findById(id: UUID): DeliveryTariff?
    fun save(tariff: DeliveryTariff): DeliveryTariff
    fun deleteById(id: UUID)
    fun findByMethodAndZone(method: DeliveryMethodType, zoneId: UUID?): DeliveryTariff?
    fun findDefaultByMethod(method: DeliveryMethodType): DeliveryTariff?
    fun existsByZoneId(zoneId: UUID): Boolean
}
