package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryZoneRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryZoneEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryZoneJpaRepository

@Repository
class DeliveryZoneRepositoryImpl(
    private val jpaRepository: DeliveryZoneJpaRepository,
) : DeliveryZoneRepository {

    override fun findActiveByCity(city: String): DeliveryZone? {
        return jpaRepository.findByNormalizedCityAndIsActiveTrue(city.normalizeForLookup())?.toDomain()
    }

    override fun findActiveByPostalCode(postalCode: String): DeliveryZone? {
        return jpaRepository.findByPostalCodeAndIsActiveTrue(postalCode.trim())?.toDomain()
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

private fun String.normalizeForLookup(): String {
    return trim().replace(Regex("\\s+"), " ").lowercase()
}
