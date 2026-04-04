package ru.foodbox.delivery.modules.orders.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusTransitionRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderStatusTransitionEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderStatusDefinitionJpaRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderStatusTransitionJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class OrderStatusTransitionRepositoryImpl(
    private val jpaRepository: OrderStatusTransitionJpaRepository,
    private val orderStatusDefinitionJpaRepository: OrderStatusDefinitionJpaRepository,
) : OrderStatusTransitionRepository {

    override fun findAll(includeInactive: Boolean): List<OrderStatusTransition> {
        val entities = if (includeInactive) {
            jpaRepository.findAllByOrderByIdAsc()
        } else {
            jpaRepository.findAllByIsActiveTrueOrderByIdAsc()
        }
        return entities.map(OrderStatusTransitionEntity::toDomain)
    }

    override fun findById(id: UUID): OrderStatusTransition? {
        return jpaRepository.findById(id).getOrNull()?.toDomain()
    }

    override fun findAllByFromStatusId(fromStatusId: UUID, includeInactive: Boolean): List<OrderStatusTransition> {
        val entities = if (includeInactive) {
            jpaRepository.findAllByFromStatusIdOrderByIdAsc(fromStatusId)
        } else {
            jpaRepository.findAllByFromStatusIdAndIsActiveTrueOrderByIdAsc(fromStatusId)
        }
        return entities.map(OrderStatusTransitionEntity::toDomain)
    }

    override fun findAllByStatusId(statusId: UUID, includeInactive: Boolean): List<OrderStatusTransition> {
        return jpaRepository.findAllByFromStatusIdOrToStatusIdOrderByIdAsc(statusId, statusId)
            .filter { includeInactive || it.isActive }
            .map(OrderStatusTransitionEntity::toDomain)
    }

    override fun findTransition(fromStatusId: UUID, toStatusId: UUID): OrderStatusTransition? {
        return jpaRepository.findByFromStatusIdAndToStatusId(fromStatusId, toStatusId)?.toDomain()
    }

    override fun save(transition: OrderStatusTransition): OrderStatusTransition {
        val existing = jpaRepository.findById(transition.id).getOrNull()
        val now = Instant.now()
        val entity = existing ?: OrderStatusTransitionEntity(
            id = transition.id,
            fromStatus = orderStatusDefinitionJpaRepository.getReferenceById(transition.fromStatus.id),
            toStatus = orderStatusDefinitionJpaRepository.getReferenceById(transition.toStatus.id),
            requiredRole = transition.requiredRole,
            isAutomatic = transition.isAutomatic,
            guardCode = transition.guardCode,
            isActive = transition.isActive,
            createdAt = now,
            updatedAt = now,
        )

        entity.fromStatus = orderStatusDefinitionJpaRepository.getReferenceById(transition.fromStatus.id)
        entity.toStatus = orderStatusDefinitionJpaRepository.getReferenceById(transition.toStatus.id)
        entity.requiredRole = transition.requiredRole
        entity.isAutomatic = transition.isAutomatic
        entity.guardCode = transition.guardCode
        entity.isActive = transition.isActive
        entity.updatedAt = now

        return jpaRepository.save(entity).toDomain()
    }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}

internal fun OrderStatusTransitionEntity.toDomain(): OrderStatusTransition {
    return OrderStatusTransition(
        id = id,
        fromStatus = fromStatus.toDomain(),
        toStatus = toStatus.toDomain(),
        requiredRole = requiredRole,
        isAutomatic = isAutomatic,
        guardCode = guardCode,
        isActive = isActive,
    )
}
