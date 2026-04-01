package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.embedded.DeliveryAddressEmbeddable
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.PickupPointEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.PickupPointJpaRepository
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Repository
class PickupPointRepositoryImpl(
    private val jpaRepository: PickupPointJpaRepository,
) : PickupPointRepository {

    override fun findAll(): List<PickupPoint> {
        return jpaRepository.findAllByOrderByNameAsc().map { it.toDomain() }
    }

    override fun findAllByIsActive(isActive: Boolean): List<PickupPoint> {
        return jpaRepository.findAllByIsActiveOrderByNameAsc(isActive).map { it.toDomain() }
    }

    override fun findById(id: java.util.UUID): PickupPoint? {
        return jpaRepository.findById(id).getOrNull()?.toDomain()
    }

    override fun findByCode(code: String): PickupPoint? {
        return jpaRepository.findByCode(code)?.toDomain()
    }

    override fun save(point: PickupPoint): PickupPoint {
        val existing = jpaRepository.findById(point.id).getOrNull()
        val now = Instant.now()
        val address = DeliveryAddressEmbeddable.fromDomain(point.address)
            ?: throw IllegalArgumentException("Pickup point address must not be empty")
        val entity = existing ?: PickupPointEntity(
            id = point.id,
            code = point.code,
            name = point.name,
            address = address,
            isActive = point.active,
            createdAt = now,
            updatedAt = now,
        )

        entity.code = point.code
        entity.name = point.name
        entity.address = address
        entity.isActive = point.active
        entity.updatedAt = now

        return jpaRepository.save(entity).toDomain()
    }

    override fun deleteById(id: java.util.UUID) {
        jpaRepository.deleteById(id)
    }

    override fun findActiveById(id: java.util.UUID): PickupPoint? {
        return findById(id)?.takeIf { it.active }
    }

    override fun findAllActive(): List<PickupPoint> {
        return jpaRepository.findAllByIsActiveTrueOrderByNameAsc().map { it.toDomain() }
    }

    private fun PickupPointEntity.toDomain(): PickupPoint {
        return PickupPoint(
            id = id,
            code = code,
            name = name,
            address = address.toDomain(),
            active = isActive,
        )
    }
}
