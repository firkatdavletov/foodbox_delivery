package ru.foodbox.delivery.modules.delivery.domain.repository

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import java.util.UUID

interface DeliveryTariffRepository {
    fun findByMethodAndZone(method: DeliveryMethodType, zoneId: UUID?): DeliveryTariff?
    fun findDefaultByMethod(method: DeliveryMethodType): DeliveryTariff?
}
