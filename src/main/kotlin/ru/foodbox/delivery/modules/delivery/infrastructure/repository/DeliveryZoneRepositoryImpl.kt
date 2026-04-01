package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.locationtech.jts.geom.MultiPolygon
import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZone
import ru.foodbox.delivery.modules.delivery.domain.DeliveryZoneType
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryZoneRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.DeliveryZoneEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.DeliveryZoneJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class DeliveryZoneRepositoryImpl(
    private val jpaRepository: DeliveryZoneJpaRepository,
) : DeliveryZoneRepository {

    override fun findAll(): List<DeliveryZone> {
        return jpaRepository.findAll()
            .sortedBy(DeliveryZoneEntity::name)
            .map { it.toDomain() }
    }

    override fun findAllByIsActive(isActive: Boolean): List<DeliveryZone> {
        return jpaRepository.findAllByIsActiveOrderByNameAsc(isActive).map { it.toDomain() }
    }

    override fun findById(id: UUID): DeliveryZone? {
        return jpaRepository.findById(id).getOrNull()?.toDomain()
    }

    override fun findByCode(code: String): DeliveryZone? {
        return jpaRepository.findByCode(code)?.toDomain()
    }

    override fun save(zone: DeliveryZone): DeliveryZone {
        val existing = jpaRepository.findById(zone.id).getOrNull()
        val now = Instant.now()
        val normalizedCity = zone.city?.normalizeForLookup()
        val normalizedPostalCode = zone.postalCode?.trim()?.takeIf { it.isNotBlank() }
        val entity = existing ?: DeliveryZoneEntity(
            id = zone.id,
            code = zone.code,
            name = zone.name,
            type = zone.type,
            city = zone.city,
            normalizedCity = normalizedCity,
            postalCode = normalizedPostalCode,
            geometry = zone.geometry.copyGeometry(),
            priority = zone.priority,
            isActive = zone.active,
            createdAt = now,
            updatedAt = now,
        )

        entity.code = zone.code
        entity.name = zone.name
        entity.type = zone.type
        entity.city = zone.city?.trim()?.takeIf { it.isNotBlank() }
        entity.normalizedCity = normalizedCity
        entity.postalCode = normalizedPostalCode
        entity.geometry = zone.geometry.copyGeometry()
        entity.priority = zone.priority
        entity.isActive = zone.active
        entity.updatedAt = now

        return jpaRepository.save(entity).toDomain()
    }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }

    override fun findActiveByPoint(latitude: Double, longitude: Double): DeliveryZone? {
        return jpaRepository.findActivePolygonContainingPoint(latitude = latitude, longitude = longitude)?.toDomain()
    }

    override fun findActiveByCity(city: String): DeliveryZone? {
        return jpaRepository.findByNormalizedCityAndTypeAndIsActiveTrue(
            normalizedCity = city.normalizeForLookup(),
            type = DeliveryZoneType.CITY,
        )?.toDomain()
    }

    override fun findActiveByPostalCode(postalCode: String): DeliveryZone? {
        return jpaRepository.findByPostalCodeAndTypeAndIsActiveTrue(
            postalCode = postalCode.trim(),
            type = DeliveryZoneType.POSTAL_CODE,
        )?.toDomain()
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

private fun String.normalizeForLookup(): String {
    return trim().replace(Regex("\\s+"), " ").lowercase()
}
