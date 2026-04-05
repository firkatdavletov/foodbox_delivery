package ru.foodbox.delivery.modules.orders.application

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.ConflictException
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.orders.application.command.ChangeOrderStatusCommand
import ru.foodbox.delivery.modules.orders.application.event.OrderStatusChangedEvent
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderStatusChangeSourceType
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistory
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusDefinitionRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusHistoryRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusTransitionRepository
import java.time.Instant
import java.util.UUID

@Service
class OrderStatusServiceImpl(
    private val orderRepository: OrderRepository,
    private val orderStatusDefinitionRepository: OrderStatusDefinitionRepository,
    private val orderStatusTransitionRepository: OrderStatusTransitionRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : OrderStatusService {

    @Transactional(readOnly = true)
    override fun getInitialStatus(): OrderStatusDefinition {
        return orderStatusDefinitionRepository.findInitial()
            ?.takeIf(OrderStatusDefinition::isActive)
            ?: throw IllegalStateException("Initial active order status is not configured")
    }

    @Transactional
    override fun recordInitialStatus(order: Order, actor: OrderStatusChangeActor, comment: String?) {
        order.ensureStatusHistoryInitialized()
        if (orderStatusHistoryRepository.existsByOrderId(order.id)) {
            return
        }

        orderStatusHistoryRepository.save(
            OrderStatusHistory(
                id = UUID.randomUUID(),
                orderId = order.id,
                previousStatus = null,
                currentStatus = order.currentStatus,
                changeSourceType = actor.sourceType,
                changedByUserId = actor.actorId,
                comment = comment?.trim()?.takeIf { it.isNotBlank() },
                changedAt = order.statusChangedAt,
            )
        )
    }

    @Transactional(readOnly = true)
    override fun getAvailableTransitions(orderId: UUID, actor: OrderStatusChangeActor): List<OrderStatusTransition> {
        val order = orderRepository.findById(orderId)
            ?: throw NotFoundException("Order not found")

        return orderStatusTransitionRepository.findAllByFromStatusId(order.currentStatus.id)
            .filter { transition -> isTransitionAllowed(transition, actor) }
    }

    @Transactional
    override fun changeStatus(orderId: UUID, command: ChangeOrderStatusCommand, actor: OrderStatusChangeActor): Order {
        val order = orderRepository.findById(orderId)
            ?: throw NotFoundException("Order not found")

        val targetStatus = resolveTargetStatus(order, command)
        if (order.currentStatus.id == targetStatus.id) {
            return order
        }

        val transition = orderStatusTransitionRepository.findTransition(
            fromStatusId = order.currentStatus.id,
            toStatusId = targetStatus.id,
        )?.takeIf(OrderStatusTransition::isActive)
            ?: throw ConflictException(
                "Transition from '${order.currentStatus.code}' to '${targetStatus.code}' is not allowed"
            )

        validateTransition(transition, actor)

        val previousStatus = order.currentStatus
        val changedAt = Instant.now()
        order.updateStatus(targetStatus, changedAt)
        val saved = orderRepository.save(order)
        val normalizedComment = command.comment?.trim()?.takeIf { it.isNotBlank() }

        orderStatusHistoryRepository.save(
            OrderStatusHistory(
                id = UUID.randomUUID(),
                orderId = saved.id,
                previousStatus = previousStatus,
                currentStatus = targetStatus,
                changeSourceType = actor.sourceType,
                changedByUserId = actor.actorId,
                comment = normalizedComment,
                changedAt = changedAt,
            )
        )

        applicationEventPublisher.publishEvent(
            OrderStatusChangedEvent(
                order = saved,
                previousStatus = previousStatus,
                currentStatus = targetStatus,
            )
        )
        return saved
    }

    @Transactional(readOnly = true)
    override fun getStatusHistory(orderId: UUID): List<OrderStatusHistory> {
        orderRepository.findById(orderId)
            ?: throw NotFoundException("Order not found")
        return orderStatusHistoryRepository.findAllByOrderId(orderId)
    }

    private fun resolveTargetStatus(order: Order, command: ChangeOrderStatusCommand): OrderStatusDefinition {
        val specifiedTargets = listOf(
            command.targetStatusId,
            command.targetStatusCode,
            command.targetStateType,
        ).count { it != null }
        require(specifiedTargets == 1) {
            "Exactly one of targetStatusId, targetStatusCode or targetStateType must be provided"
        }

        command.targetStatusId?.let { statusId ->
            return orderStatusDefinitionRepository.findById(statusId)
                ?.takeIf(OrderStatusDefinition::isActive)
                ?: throw NotFoundException("Order status not found: $statusId")
        }

        command.targetStatusCode?.let { statusCode ->
            return orderStatusDefinitionRepository.findByCode(statusCode.trim())
                ?.takeIf(OrderStatusDefinition::isActive)
                ?: throw NotFoundException("Order status not found: ${statusCode.trim()}")
        }

        val stateType = requireNotNull(command.targetStateType)
        val matchedTransitions = orderStatusTransitionRepository.findAllByFromStatusId(order.currentStatus.id)
            .filter { it.toStatus.isActive && it.toStatus.stateType == stateType }
        if (matchedTransitions.isEmpty()) {
            throw ConflictException("No transition is configured for state type '$stateType'")
        }
        if (matchedTransitions.size > 1) {
            throw ConflictException("Multiple transitions are configured for state type '$stateType'")
        }
        return matchedTransitions.single().toStatus
    }

    private fun validateTransition(transition: OrderStatusTransition, actor: OrderStatusChangeActor) {
        if (!isTransitionAllowed(transition, actor)) {
            if (transition.requiredRole != null && transition.requiredRole !in actor.roles) {
                throw ForbiddenException("Role '${transition.requiredRole.name}' is required for this transition")
            }
            if (actor.sourceType == OrderStatusChangeSourceType.SYSTEM && !transition.isAutomatic) {
                throw ConflictException("Manual transition cannot be applied automatically")
            }
            throw ConflictException("Order status transition is not allowed")
        }
    }

    private fun isTransitionAllowed(
        transition: OrderStatusTransition,
        actor: OrderStatusChangeActor,
    ): Boolean {
        if (!transition.isActive || !transition.toStatus.isActive || !transition.fromStatus.isActive) {
            return false
        }
        if (transition.requiredRole != null && transition.requiredRole !in actor.roles) {
            return false
        }
        if (actor.sourceType == OrderStatusChangeSourceType.SYSTEM && !transition.isAutomatic) {
            return false
        }
        return true
    }
}
