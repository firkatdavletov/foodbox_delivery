package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.locationtech.jts.geom.MultiPolygon
import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryTariff
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryTariffRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryTariffEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryZoneEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryZoneJpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryTariffJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class DeliveryTariffRepositoryImpl(
    private val jpaRepository: DeliveryTariffJpaRepository,
    private val deliveryZoneJpaRepository: DeliveryZoneJpaRepository,
) : DeliveryTariffRepository {

    override fun findAll(): List<DeliveryTariff> {
        return jpaRepository.findAll()
            .map { it.toDomain() }
            .sortedWith(compareBy<DeliveryTariff> { it.method.ordinal }.thenBy { it.zone?.name ?: "" })
    }

    override fun findById(id: UUID): DeliveryTariff? {
        return jpaRepository.findById(id).getOrNull()?.toDomain()
    }

    override fun save(tariff: DeliveryTariff): DeliveryTariff {
        val existing = jpaRepository.findById(tariff.id).getOrNull()
        val now = Instant.now()
        val zoneEntity = tariff.zone?.id?.let { zoneId ->
            deliveryZoneJpaRepository.findById(zoneId).getOrNull()
                ?: throw IllegalArgumentException("Delivery zone not found: $zoneId")
        }
        val entity = existing ?: DeliveryTariffEntity(
            id = tariff.id,
            method = tariff.method,
            zone = zoneEntity,
            isAvailable = tariff.available,
            fixedPriceMinor = tariff.fixedPriceMinor,
            freeFromAmountMinor = tariff.freeFromAmountMinor,
            currency = tariff.currency,
            estimatedDays = tariff.estimatedDays,
            createdAt = now,
            updatedAt = now,
        )

        entity.method = tariff.method
        entity.zone = zoneEntity
        entity.isAvailable = tariff.available
        entity.fixedPriceMinor = tariff.fixedPriceMinor
        entity.freeFromAmountMinor = tariff.freeFromAmountMinor
        entity.currency = tariff.currency
        entity.estimatedDays = tariff.estimatedDays
        entity.updatedAt = now

        return jpaRepository.save(entity).toDomain()
    }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }

    override fun findByMethodAndZone(method: DeliveryMethodType, zoneId: UUID?): DeliveryTariff? {
        if (zoneId == null) {
            return null
        }
        return jpaRepository.findByMethodAndZoneId(method, zoneId)?.toDomain()
    }

    override fun findDefaultByMethod(method: DeliveryMethodType): DeliveryTariff? {
        return jpaRepository.findByMethodAndZoneIsNull(method)?.toDomain()
    }

    override fun existsByZoneId(zoneId: UUID): Boolean {
        return jpaRepository.existsByZoneId(zoneId)
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
            type = type,
            city = city,
            normalizedCity = normalizedCity,
            postalCode = postalCode,
            geometry = geometry.copyGeometry(),
            priority = priority,
            active = isActive,
        )
    }
}

private fun MultiPolygon?.copyGeometry(): MultiPolygon? {
    return this?.copy()?.let { geometryCopy ->
        (geometryCopy as MultiPolygon).also { copiedGeometry ->
            copiedGeometry.srid = 4326
        }
    }
}
