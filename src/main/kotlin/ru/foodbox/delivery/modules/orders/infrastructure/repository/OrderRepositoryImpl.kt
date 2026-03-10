package ru.foodbox.delivery.modules.orders.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderStatus
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderItemEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class OrderRepositoryImpl(
    private val jpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: Order): Order {
        val existing = jpaRepository.findById(order.id).getOrNull()
        val entity = existing ?: OrderEntity(
            id = order.id,
            orderNumber = order.orderNumber,
            customerType = order.customerType,
            userId = order.userId,
            guestInstallId = order.guestInstallId,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            customerEmail = order.customerEmail,
            status = order.status,
            deliveryType = order.deliveryType,
            deliveryAddress = order.deliveryAddress,
            comment = order.comment,
            subtotalMinor = order.subtotalMinor,
            deliveryFeeMinor = order.deliveryFeeMinor,
            totalMinor = order.totalMinor,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
        )

        entity.customerType = order.customerType
        entity.userId = order.userId
        entity.guestInstallId = order.guestInstallId
        entity.customerName = order.customerName
        entity.customerPhone = order.customerPhone
        entity.customerEmail = order.customerEmail
        entity.status = order.status
        entity.deliveryType = order.deliveryType
        entity.deliveryAddress = order.deliveryAddress
        entity.comment = order.comment
        entity.subtotalMinor = order.subtotalMinor
        entity.deliveryFeeMinor = order.deliveryFeeMinor
        entity.totalMinor = order.totalMinor
        entity.updatedAt = order.updatedAt

        entity.items.clear()
        entity.items.addAll(
            order.items.map { item ->
                OrderItemEntity(
                    id = item.id,
                    order = entity,
                    productId = item.productId,
                    title = item.title,
                    unit = item.unit,
                    quantity = item.quantity,
                    priceMinor = item.priceMinor,
                    totalMinor = item.totalMinor,
                )
            }
        )

        return toDomain(jpaRepository.save(entity))
    }

    override fun findById(orderId: UUID): Order? {
        val entity = jpaRepository.findById(orderId).getOrNull() ?: return null
        return toDomain(entity)
    }

    override fun findAllByStatuses(statuses: Set<OrderStatus>): List<Order> {
        return jpaRepository.findAllByStatusInOrderByCreatedAtDesc(statuses).map(::toDomain)
    }

    override fun findByOrderNumber(orderNumber: String): Order? {
        return jpaRepository.findByOrderNumber(orderNumber)?.let(::toDomain)
    }

    override fun findByUserId(userId: UUID): List<Order> {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map(::toDomain)
    }

    override fun findByGuestInstallId(installId: String): List<Order> {
        return jpaRepository.findAllByGuestInstallIdOrderByCreatedAtDesc(installId).map(::toDomain)
    }

    private fun toDomain(entity: OrderEntity): Order {
        return Order(
            id = entity.id,
            orderNumber = entity.orderNumber,
            customerType = entity.customerType,
            userId = entity.userId,
            guestInstallId = entity.guestInstallId,
            customerName = entity.customerName,
            customerPhone = entity.customerPhone,
            customerEmail = entity.customerEmail,
            status = entity.status,
            deliveryType = entity.deliveryType,
            deliveryAddress = entity.deliveryAddress,
            comment = entity.comment,
            items = entity.items.map { item ->
                OrderItem(
                    id = item.id,
                    productId = item.productId,
                    title = item.title,
                    unit = item.unit,
                    quantity = item.quantity,
                    priceMinor = item.priceMinor,
                    totalMinor = item.totalMinor,
                )
            },
            subtotalMinor = entity.subtotalMinor,
            deliveryFeeMinor = entity.deliveryFeeMinor,
            totalMinor = entity.totalMinor,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
