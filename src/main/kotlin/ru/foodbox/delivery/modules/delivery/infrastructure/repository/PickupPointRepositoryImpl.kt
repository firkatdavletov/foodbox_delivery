package ru.foodbox.delivery.modules.delivery.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.entity.PickupPointEntity
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.jpa.PickupPointJpaRepository
import kotlin.jvm.optionals.getOrNull

@Repository
class PickupPointRepositoryImpl(
    private val jpaRepository: PickupPointJpaRepository,
) : PickupPointRepository {

    override fun findById(id: java.util.UUID): PickupPoint? {
        return jpaRepository.findById(id).getOrNull()?.toDomain()
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
