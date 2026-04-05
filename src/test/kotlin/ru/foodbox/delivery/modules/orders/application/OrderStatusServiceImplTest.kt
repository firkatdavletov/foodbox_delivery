package ru.foodbox.delivery.modules.orders.application

import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import ru.foodbox.delivery.common.error.ConflictException
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.orders.application.command.ChangeOrderStatusCommand
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistory
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusDefinitionRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusHistoryRepository
import ru.foodbox.delivery.modules.orders.domain.repository.OrderStatusTransitionRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderStatusServiceImplTest {

    @Test
    fun `change status stores history and updates order`() {
        val orderRepository = InMemoryOrderRepository()
        val definitionRepository = InMemoryOrderStatusDefinitionRepository()
        val transitionRepository = InMemoryOrderStatusTransitionRepository()
        val historyRepository = InMemoryOrderStatusHistoryRepository()
        val eventPublisher = RecordingApplicationEventPublisher()

        OrderStatusWorkflowDefaults.statuses.forEach(definitionRepository::save)
        OrderStatusWorkflowDefaults.transitions.forEach(transitionRepository::save)

        val service = OrderStatusServiceImpl(
            orderRepository = orderRepository,
            orderStatusDefinitionRepository = definitionRepository,
            orderStatusTransitionRepository = transitionRepository,
            orderStatusHistoryRepository = historyRepository,
            applicationEventPublisher = eventPublisher,
        )

        val pending = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }
        val order = orderRepository.save(orderWithStatus(pending))
        service.recordInitialStatus(order, OrderStatusChangeActor.system())

        val updated = service.changeStatus(
            orderId = order.id,
            command = ChangeOrderStatusCommand(targetStatusCode = "CONFIRMED", comment = "Approved"),
            actor = OrderStatusChangeActor(
                sourceType = ru.foodbox.delivery.modules.orders.domain.OrderStatusChangeSourceType.ADMIN,
            ),
        )

        assertEquals("CONFIRMED", updated.currentStatus.code)
        assertEquals(listOf("PENDING", "CONFIRMED"), updated.statusHistory.map { it.code })
        assertEquals(2, historyRepository.findAllByOrderId(order.id).size)
        assertEquals(1, eventPublisher.events.size)
    }

    @Test
    fun `system actor cannot use manual transition`() {
        val orderRepository = InMemoryOrderRepository()
        val definitionRepository = InMemoryOrderStatusDefinitionRepository()
        val transitionRepository = InMemoryOrderStatusTransitionRepository()
        val historyRepository = InMemoryOrderStatusHistoryRepository()

        OrderStatusWorkflowDefaults.statuses.forEach(definitionRepository::save)
        OrderStatusWorkflowDefaults.transitions.forEach(transitionRepository::save)

        val service = OrderStatusServiceImpl(
            orderRepository = orderRepository,
            orderStatusDefinitionRepository = definitionRepository,
            orderStatusTransitionRepository = transitionRepository,
            orderStatusHistoryRepository = historyRepository,
            applicationEventPublisher = RecordingApplicationEventPublisher(),
        )

        val pending = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" }
        val order = orderRepository.save(orderWithStatus(pending))
        service.recordInitialStatus(order, OrderStatusChangeActor.system())

        assertFailsWith<ConflictException> {
            service.changeStatus(
                orderId = order.id,
                command = ChangeOrderStatusCommand(targetStateType = OrderStateType.CANCELED),
                actor = OrderStatusChangeActor.system(),
            )
        }
    }

    private fun orderWithStatus(status: OrderStatusDefinition): Order {
        val now = Instant.now()
        return Order(
            id = UUID.randomUUID(),
            orderNumber = "ORD-${System.nanoTime()}",
            customerType = OrderCustomerType.GUEST,
            userId = null,
            guestInstallId = "install-1",
            customerName = "Guest",
            customerPhone = "+79990000000",
            customerEmail = null,
            currentStatus = status,
            delivery = OrderDeliverySnapshot(
                method = DeliveryMethodType.COURIER,
                methodName = DeliveryMethodType.COURIER.displayName,
                priceMinor = 500,
                currency = "RUB",
                zoneCode = "city",
                zoneName = "City",
                estimatedDays = 1,
                pickupPointId = null,
                pickupPointExternalId = null,
                pickupPointName = null,
                pickupPointAddress = null,
                address = null,
            ),
            comment = null,
            items = listOf(
                OrderItem(
                    id = UUID.randomUUID(),
                    productId = UUID.randomUUID(),
                    variantId = null,
                    sku = "TEST-1",
                    title = "Test product",
                    unit = ru.foodbox.delivery.modules.catalog.domain.ProductUnit.PIECE,
                    quantity = 1,
                    priceMinor = 1_000,
                    totalMinor = 1_000,
                )
            ),
            subtotalMinor = 1_000,
            deliveryFeeMinor = 500,
            totalMinor = 1_500,
            statusChangedAt = now,
            createdAt = now,
            updatedAt = now,
        )
    }

    private class InMemoryOrderRepository : OrderRepository {
        private val orders = linkedMapOf<UUID, Order>()

        override fun save(order: Order): Order {
            orders[order.id] = order.copy()
            return order
        }

        override fun findById(orderId: UUID): Order? = orders[orderId]?.copy()

        override fun findAllByCurrentStatusStateTypes(stateTypes: Set<OrderStateType>): List<Order> {
            return orders.values.filter { it.currentStatus.stateType in stateTypes }.map(Order::copy)
        }

        override fun findByOrderNumber(orderNumber: String): Order? {
            return orders.values.firstOrNull { it.orderNumber == orderNumber }?.copy()
        }

        override fun findByUserId(userId: UUID): List<Order> {
            return orders.values.filter { it.userId == userId }.map(Order::copy)
        }

        override fun findByGuestInstallId(installId: String): List<Order> {
            return orders.values.filter { it.guestInstallId == installId }.map(Order::copy)
        }

        override fun existsByCurrentStatusId(statusId: UUID): Boolean {
            return orders.values.any { it.currentStatus.id == statusId }
        }
    }

    private class InMemoryOrderStatusDefinitionRepository : OrderStatusDefinitionRepository {
        private val definitions = linkedMapOf<UUID, OrderStatusDefinition>()

        override fun findAll(includeInactive: Boolean): List<OrderStatusDefinition> {
            return definitions.values.filter { includeInactive || it.isActive }
        }

        override fun findById(id: UUID): OrderStatusDefinition? = definitions[id]

        override fun findByCode(code: String): OrderStatusDefinition? {
            return definitions.values.firstOrNull { it.code == code.trim().uppercase() }
        }

        override fun findByStateType(stateType: OrderStateType): List<OrderStatusDefinition> {
            return definitions.values.filter { it.stateType == stateType }
        }

        override fun findInitial(): OrderStatusDefinition? {
            return definitions.values.firstOrNull { it.isInitial && it.isActive }
        }

        override fun save(status: OrderStatusDefinition): OrderStatusDefinition {
            definitions[status.id] = status
            return status
        }
    }

    private class InMemoryOrderStatusTransitionRepository : OrderStatusTransitionRepository {
        private val transitions = linkedMapOf<UUID, OrderStatusTransition>()

        override fun findAll(includeInactive: Boolean): List<OrderStatusTransition> {
            return transitions.values.filter { includeInactive || it.isActive }
        }

        override fun findById(id: UUID): OrderStatusTransition? = transitions[id]

        override fun findAllByFromStatusId(fromStatusId: UUID, includeInactive: Boolean): List<OrderStatusTransition> {
            return transitions.values.filter { it.fromStatus.id == fromStatusId && (includeInactive || it.isActive) }
        }

        override fun findAllByStatusId(statusId: UUID, includeInactive: Boolean): List<OrderStatusTransition> {
            return transitions.values.filter {
                (it.fromStatus.id == statusId || it.toStatus.id == statusId) &&
                    (includeInactive || it.isActive)
            }
        }

        override fun findTransition(fromStatusId: UUID, toStatusId: UUID): OrderStatusTransition? {
            return transitions.values.firstOrNull { it.fromStatus.id == fromStatusId && it.toStatus.id == toStatusId }
        }

        override fun save(transition: OrderStatusTransition): OrderStatusTransition {
            transitions[transition.id] = transition
            return transition
        }

        override fun deleteById(id: UUID) {
            transitions.remove(id)
        }
    }

    private class InMemoryOrderStatusHistoryRepository : OrderStatusHistoryRepository {
        private val histories = mutableListOf<OrderStatusHistory>()

        override fun save(history: OrderStatusHistory): OrderStatusHistory {
            histories += history
            return history
        }

        override fun findAllByOrderId(orderId: UUID): List<OrderStatusHistory> {
            return histories.filter { it.orderId == orderId }
        }

        override fun existsByOrderId(orderId: UUID): Boolean {
            return histories.any { it.orderId == orderId }
        }
    }

    private class RecordingApplicationEventPublisher : ApplicationEventPublisher {
        val events = mutableListOf<Any>()

        override fun publishEvent(event: Any) {
            events += event
        }
    }
}
