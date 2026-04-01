package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryTariffEntity
import java.util.UUID

interface DeliveryTariffJpaRepository : JpaRepository<DeliveryTariffEntity, UUID> {
    fun findByMethodAndZoneId(method: DeliveryMethodType, zoneId: UUID): DeliveryTariffEntity?
    fun findByMethodAndZoneIsNull(method: DeliveryMethodType): DeliveryTariffEntity?
    fun existsByZoneId(zoneId: UUID): Boolean
}
