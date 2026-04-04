package ru.foodbox.delivery.modules.orders.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusDefinitionRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderStatusDefinitionEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderStatusDefinitionJpaRepository
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class OrderStatusDefinitionRepositoryImpl(
    private val jpaRepository: OrderStatusDefinitionJpaRepository,
) : OrderStatusDefinitionRepository {

    override fun findAll(includeInactive: Boolean): List<OrderStatusDefinition> {
        val entities = if (includeInactive) {
            jpaRepository.findAllByOrderBySortOrderAscNameAsc()
        } else {
            jpaRepository.findAllByIsActiveTrueOrderBySortOrderAscNameAsc()
        }
        return entities.map(OrderStatusDefinitionEntity::toDomain)
    }

    override fun findById(id: UUID): OrderStatusDefinition? {
        return jpaRepository.findById(id).getOrNull()?.toDomain()
    }

    override fun findByCode(code: String): OrderStatusDefinition? {
        return jpaRepository.findByCode(code.trim().uppercase())?.toDomain()
    }

    override fun findByStateType(stateType: OrderStateType): List<OrderStatusDefinition> {
        return jpaRepository.findAllByStateTypeOrderBySortOrderAscNameAsc(stateType).map(OrderStatusDefinitionEntity::toDomain)
    }

    override fun findInitial(): OrderStatusDefinition? {
        return jpaRepository.findFirstByIsInitialTrueAndIsActiveTrue()?.toDomain()
    }

    override fun save(status: OrderStatusDefinition): OrderStatusDefinition {
        val existing = jpaRepository.findById(status.id).getOrNull()
        val now = Instant.now()
        val entity = existing ?: OrderStatusDefinitionEntity(
            id = status.id,
            code = status.code,
            name = status.name,
            description = status.description,
            stateType = status.stateType,
            color = status.color,
            icon = status.icon,
            isInitial = status.isInitial,
            isFinal = status.isFinal,
            isCancellable = status.isCancellable,
            isActive = status.isActive,
            visibleToCustomer = status.visibleToCustomer,
            notifyCustomer = status.notifyCustomer,
            notifyStaff = status.notifyStaff,
            sortOrder = status.sortOrder,
            createdAt = now,
            updatedAt = now,
        )

        entity.code = status.code
        entity.name = status.name
        entity.description = status.description
        entity.stateType = status.stateType
        entity.color = status.color
        entity.icon = status.icon
        entity.isInitial = status.isInitial
        entity.isFinal = status.isFinal
        entity.isCancellable = status.isCancellable
        entity.isActive = status.isActive
        entity.visibleToCustomer = status.visibleToCustomer
        entity.notifyCustomer = status.notifyCustomer
        entity.notifyStaff = status.notifyStaff
        entity.sortOrder = status.sortOrder
        entity.updatedAt = now

        return jpaRepository.save(entity).toDomain()
    }
}

internal fun OrderStatusDefinitionEntity.toDomain(): OrderStatusDefinition {
    return OrderStatusDefinition(
        id = id,
        code = code,
        name = name,
        description = description,
        stateType = stateType,
        color = color,
        icon = icon,
        isInitial = isInitial,
        isFinal = isFinal,
        isCancellable = isCancellable,
        isActive = isActive,
        visibleToCustomer = visibleToCustomer,
        notifyCustomer = notifyCustomer,
        notifyStaff = notifyStaff,
        sortOrder = sortOrder,
    )
}
