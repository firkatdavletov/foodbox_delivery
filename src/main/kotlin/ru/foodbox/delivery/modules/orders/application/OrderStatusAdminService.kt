package ru.foodbox.delivery.modules.orders.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.ConflictException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusDefinitionRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusTransitionRepository
import java.util.Locale
import java.util.UUID

@Service
class OrderStatusAdminService(
    private val orderStatusDefinitionRepository: OrderStatusDefinitionRepository,
    private val orderStatusTransitionRepository: OrderStatusTransitionRepository,
) {

    fun getStatuses(includeInactive: Boolean = true): List<OrderStatusDefinition> {
        return orderStatusDefinitionRepository.findAll(includeInactive)
            .sortedWith(compareBy<OrderStatusDefinition> { it.sortOrder }.thenBy { it.name })
    }

    fun getStatus(statusId: UUID): OrderStatusDefinition {
        return orderStatusDefinitionRepository.findById(statusId)
            ?: throw NotFoundException("Order status not found: $statusId")
    }

    @Transactional
    fun saveStatus(status: OrderStatusDefinition): OrderStatusDefinition {
        val existing = status.id.let(orderStatusDefinitionRepository::findById)
        val normalizedCode = status.code.trim().uppercase(Locale.ROOT)
            .takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Order status code is required")
        val normalizedName = status.name.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Order status name is required")
        require(status.sortOrder >= 0) { "sortOrder must be non-negative" }
        require(!(status.isInitial && status.isFinal)) { "Initial status cannot be final" }
        require(!(status.isInitial && !status.isActive)) { "Inactive status cannot be initial" }

        orderStatusDefinitionRepository.findByCode(normalizedCode)?.let { duplicate ->
            if (duplicate.id != status.id) {
                throw ConflictException("Order status code '$normalizedCode' is already used")
            }
        }

        if (status.isInitial) {
            orderStatusDefinitionRepository.findAll(includeInactive = false)
                .firstOrNull { it.isInitial && it.id != status.id }
                ?.let {
                    throw ConflictException("Only one active initial order status is allowed")
                }
        } else if (existing?.isInitial == true && !status.isInitial) {
            val hasAnotherInitial = orderStatusDefinitionRepository.findAll(includeInactive = false)
                .any { it.isInitial && it.id != status.id }
            if (!hasAnotherInitial) {
                throw ConflictException("At least one active initial order status must remain configured")
            }
        }

        if (status.isFinal) {
            val activeOutgoing = orderStatusTransitionRepository.findAllByFromStatusId(
                fromStatusId = status.id,
                includeInactive = false,
            )
            if (activeOutgoing.isNotEmpty()) {
                throw ConflictException("Final order status cannot have outgoing transitions")
            }
        }

        return orderStatusDefinitionRepository.save(
            status.copy(
                code = normalizedCode,
                name = normalizedName,
                description = status.description?.trim()?.takeIf { it.isNotBlank() },
                color = status.color?.trim()?.takeIf { it.isNotBlank() },
                icon = status.icon?.trim()?.takeIf { it.isNotBlank() },
            )
        )
    }

    @Transactional
    fun deactivateStatus(statusId: UUID): OrderStatusDefinition {
        val status = getStatus(statusId)
        if (!status.isActive) {
            return status
        }

        if (status.isInitial) {
            val hasAnotherInitial = orderStatusDefinitionRepository.findAll(includeInactive = false)
                .any { it.isInitial && it.id != statusId }
            if (!hasAnotherInitial) {
                throw ConflictException("At least one active initial order status must remain configured")
            }
        }

        return orderStatusDefinitionRepository.save(
            status.copy(
                isActive = false,
                isInitial = false,
            )
        )
    }

    fun getTransitions(statusId: UUID?): List<OrderStatusTransition> {
        val transitions = when (statusId) {
            null -> orderStatusTransitionRepository.findAll(includeInactive = true)
            else -> orderStatusTransitionRepository.findAllByStatusId(statusId, includeInactive = true)
        }
        return transitions.sortedWith(
            compareBy<OrderStatusTransition> { it.fromStatus.sortOrder }
                .thenBy { it.toStatus.sortOrder }
                .thenBy { it.fromStatus.name }
                .thenBy { it.toStatus.name }
        )
    }

    @Transactional
    fun createTransition(transition: OrderStatusTransition): OrderStatusTransition {
        require(transition.fromStatus.id != transition.toStatus.id) {
            "Transition source and target must be different"
        }
        require(transition.fromStatus.isActive) { "Transition source status must be active" }
        require(transition.toStatus.isActive) { "Transition target status must be active" }
        if (transition.fromStatus.isFinal) {
            throw ConflictException("Final order status cannot have outgoing transitions")
        }

        orderStatusTransitionRepository.findTransition(
            fromStatusId = transition.fromStatus.id,
            toStatusId = transition.toStatus.id,
        )?.let {
            throw ConflictException("Order status transition already exists")
        }

        return orderStatusTransitionRepository.save(transition)
    }

    @Transactional
    fun deleteTransition(transitionId: UUID) {
        orderStatusTransitionRepository.findById(transitionId)
            ?: throw NotFoundException("Order status transition not found: $transitionId")
        orderStatusTransitionRepository.deleteById(transitionId)
    }

    @Transactional
    fun bootstrapDefaultsIfNeeded() {
        if (orderStatusDefinitionRepository.findAll(includeInactive = true).isNotEmpty()) {
            return
        }

        OrderStatusWorkflowDefaults.statuses.forEach(orderStatusDefinitionRepository::save)
        OrderStatusWorkflowDefaults.transitions.forEach(orderStatusTransitionRepository::save)
    }
}
