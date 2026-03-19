package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryTariffRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryTariffEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryZoneEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryTariffJpaRepository
import java.util.UUID

@Repository
class DeliveryTariffRepositoryImpl(
    private val jpaRepository: DeliveryTariffJpaRepository,
) : DeliveryTariffRepository {

    override fun findByMethodAndZone(method: DeliveryMethodType, zoneId: UUID?): DeliveryTariff? {
        if (zoneId == null) {
            return null
        }
        return jpaRepository.findByMethodAndZoneId(method, zoneId)?.toDomain()
    }

    override fun findDefaultByMethod(method: DeliveryMethodType): DeliveryTariff? {
        return jpaRepository.findByMethodAndZoneIsNull(method)?.toDomain()
    }

    private fun DeliveryTariffEntity.toDomain(): DeliveryTariff {
        return DeliveryTariff(
            id = id,
            method = method,
            zone = zone?.toDomain(),
            available = isAvailable,
            fixedPriceMinor = fixedPriceMinor,
            freeFromAmountMinor = freeFromAmountMinor,
            currency = currency,
            estimatedDays = estimatedDays,
        )
    }

    private fun DeliveryZoneEntity.toDomain(): DeliveryZone {
        return DeliveryZone(
            id = id,
            code = code,
            name = name,
            city = city,
            postalCode = postalCode,
            active = isActive,
        )
    }
}
