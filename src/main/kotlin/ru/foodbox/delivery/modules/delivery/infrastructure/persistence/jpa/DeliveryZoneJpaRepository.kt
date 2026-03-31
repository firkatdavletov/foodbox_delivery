package ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa

import org.springframework.data.jpa.repository.JpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryZoneEntity
import java.util.UUID

interface DeliveryZoneJpaRepository : JpaRepository<DeliveryZoneEntity, UUID> {
    fun findAllByIsActiveOrderByNameAsc(isActive: Boolean): List<DeliveryZoneEntity>
    fun findByCode(code: String): DeliveryZoneEntity?
    fun findByNormalizedCityAndIsActiveTrue(normalizedCity: String): DeliveryZoneEntity?
    fun findByPostalCodeAndIsActiveTrue(postalCode: String): DeliveryZoneEntity?
}
