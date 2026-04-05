package ru.foodbox.delivery.modules.orders.infrastructure.repository

import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderPaymentSnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderStateType
import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistoryEntry
import ru.foodbox.delivery.modules.orders.modifier.domain.OrderItemModifier
import ru.foodbox.delivery.modules.orders.domain.repository.OrderRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderDeliverySnapshotEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderItemEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.entity.OrderItemModifierEntity
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderJpaRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderStatusDefinitionJpaRepository
import ru.foodbox.delivery.modules.orders.infrastructure.persistence.jpa.OrderStatusHistoryJpaRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.persistence.embedded.DeliveryAddressEmbeddable
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class OrderRepositoryImpl(
    private val jpaRepository: OrderJpaRepository,
    private val orderStatusDefinitionJpaRepository: OrderStatusDefinitionJpaRepository,
    private val orderStatusHistoryJpaRepository: OrderStatusHistoryJpaRepository,
) : OrderRepository {

    @Transactional
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
            currentStatus = orderStatusDefinitionJpaRepository.getReferenceById(order.currentStatus.id),
            comment = order.comment,
            paymentMethodCode = order.payment?.methodCode,
            paymentMethodName = order.payment?.methodName,
            subtotalMinor = order.subtotalMinor,
            deliveryFeeMinor = order.deliveryFeeMinor,
            totalMinor = order.totalMinor,
            statusChangedAt = order.statusChangedAt,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
        )

        entity.customerType = order.customerType
        entity.userId = order.userId
        entity.guestInstallId = order.guestInstallId
        entity.customerName = order.customerName
        entity.customerPhone = order.customerPhone
        entity.customerEmail = order.customerEmail
        entity.currentStatus = orderStatusDefinitionJpaRepository.getReferenceById(order.currentStatus.id)
        entity.comment = order.comment
        entity.paymentMethodCode = order.payment?.methodCode
        entity.paymentMethodName = order.payment?.methodName
        entity.subtotalMinor = order.subtotalMinor
        entity.deliveryFeeMinor = order.deliveryFeeMinor
        entity.totalMinor = order.totalMinor
        entity.statusChangedAt = order.statusChangedAt
        entity.updatedAt = order.updatedAt

        entity.items.clear()
        entity.items.addAll(
            order.items.map { item ->
                OrderItemEntity(
                    id = item.id,
                    order = entity,
                    productId = item.productId,
                    variantId = item.variantId,
                    sku = item.sku,
                    title = item.title,
                    unit = item.unit,
                    quantity = item.quantity,
                    priceMinor = item.priceMinor,
                    totalMinor = item.totalMinor,
                ).apply {
                    modifiers.addAll(
                        item.modifiers.map { modifier ->
                            OrderItemModifierEntity(
                                id = UUID.randomUUID(),
                                orderItem = this,
                                modifierGroupId = modifier.modifierGroupId,
                                modifierOptionId = modifier.modifierOptionId,
                                groupCodeSnapshot = modifier.groupCodeSnapshot,
                                groupNameSnapshot = modifier.groupNameSnapshot,
                                optionCodeSnapshot = modifier.optionCodeSnapshot,
                                optionNameSnapshot = modifier.optionNameSnapshot,
                                applicationScopeSnapshot = modifier.applicationScopeSnapshot,
                                priceSnapshot = modifier.priceSnapshot,
                                quantity = modifier.quantity,
                            )
                        }
                    )
                }
            }
        )

        entity.delivery = entity.delivery?.apply {
            method = order.delivery.method
            methodName = order.delivery.methodName
            priceMinor = order.delivery.priceMinor
            currency = order.delivery.currency
            zoneCode = order.delivery.zoneCode
            zoneName = order.delivery.zoneName
            estimatedDays = order.delivery.estimatedDays
            estimatesMinutes = order.delivery.estimatesMinutes
            pickupPointId = order.delivery.pickupPointId
            pickupPointExternalId = order.delivery.pickupPointExternalId
            pickupPointName = order.delivery.pickupPointName
            pickupPointAddress = order.delivery.pickupPointAddress
            address = DeliveryAddressEmbeddable.fromDomain(order.delivery.address)
        } ?: OrderDeliverySnapshotEntity(
            id = UUID.randomUUID(),
            order = entity,
            method = order.delivery.method,
            methodName = order.delivery.methodName,
            priceMinor = order.delivery.priceMinor,
            currency = order.delivery.currency,
            zoneCode = order.delivery.zoneCode,
            zoneName = order.delivery.zoneName,
            estimatedDays = order.delivery.estimatedDays,
            estimatesMinutes = order.delivery.estimatesMinutes,
            pickupPointId = order.delivery.pickupPointId,
            pickupPointExternalId = order.delivery.pickupPointExternalId,
            pickupPointName = order.delivery.pickupPointName,
            pickupPointAddress = order.delivery.pickupPointAddress,
            address = DeliveryAddressEmbeddable.fromDomain(order.delivery.address),
        )

        return toDomain(
            entity = jpaRepository.save(entity),
            statusHistory = order.statusHistory,
        )
    }

    @Transactional(readOnly = true)
    override fun findById(orderId: UUID): Order? {
        val entity = jpaRepository.findById(orderId).getOrNull() ?: return null
        return toDomain(entity, loadStatusHistory(setOf(entity.id))[entity.id].orEmpty())
    }

    @Transactional(readOnly = true)
    override fun findAllByCurrentStatusStateTypes(stateTypes: Set<OrderStateType>): List<Order> {
        return toDomainList(jpaRepository.findAllByCurrentStatusStateTypeInOrderByCreatedAtDesc(stateTypes))
    }

    @Transactional(readOnly = true)
    override fun findByOrderNumber(orderNumber: String): Order? {
        return jpaRepository.findByOrderNumber(orderNumber)
            ?.let { entity -> toDomain(entity, loadStatusHistory(setOf(entity.id))[entity.id].orEmpty()) }
    }

    @Transactional(readOnly = true)
    override fun findByUserId(userId: UUID): List<Order> {
        return toDomainList(jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
    }

    @Transactional(readOnly = true)
    override fun findByGuestInstallId(installId: String): List<Order> {
        return toDomainList(jpaRepository.findAllByGuestInstallIdOrderByCreatedAtDesc(installId))
    }

    @Transactional(readOnly = true)
    override fun existsByCurrentStatusId(statusId: UUID): Boolean {
        return jpaRepository.existsByCurrentStatusId(statusId)
    }

    private fun toDomainList(entities: List<OrderEntity>): List<Order> {
        if (entities.isEmpty()) {
            return emptyList()
        }

        val historyByOrderId = loadStatusHistory(entities.map(OrderEntity::id).toSet())
        return entities.map { entity ->
            toDomain(entity, historyByOrderId[entity.id].orEmpty())
        }
    }

    private fun loadStatusHistory(orderIds: Collection<UUID>): Map<UUID, List<OrderStatusHistoryEntry>> {
        if (orderIds.isEmpty()) {
            return emptyMap()
        }

        return orderStatusHistoryJpaRepository.findAllByOrderIdInWithCurrentStatus(orderIds)
            .groupBy { it.order.id }
            .mapValues { (_, history) ->
                history.map { entry ->
                    OrderStatusHistoryEntry(
                        code = entry.currentStatus.code,
                        name = entry.currentStatus.name,
                        timestamp = entry.changedAt,
                    )
                }
            }
    }

    private fun toDomain(
        entity: OrderEntity,
        statusHistory: List<OrderStatusHistoryEntry> = emptyList(),
    ): Order {
        return Order(
            id = entity.id,
            orderNumber = entity.orderNumber,
            customerType = entity.customerType,
            userId = entity.userId,
            guestInstallId = entity.guestInstallId,
            customerName = entity.customerName,
            customerPhone = entity.customerPhone,
            customerEmail = entity.customerEmail,
            currentStatus = entity.currentStatus.toDomain(),
            delivery = entity.delivery?.toDomain()
                ?: error("Order delivery snapshot is missing for order ${entity.id}"),
            comment = entity.comment,
            items = entity.items.map { item ->
                OrderItem(
                    id = item.id,
                    productId = item.productId,
                    variantId = item.variantId,
                    sku = item.sku,
                    title = item.title,
                    unit = item.unit,
                    quantity = item.quantity,
                    priceMinor = item.priceMinor,
                    totalMinor = item.totalMinor,
                    modifiers = item.modifiers.map { modifier ->
                        OrderItemModifier(
                            modifierGroupId = modifier.modifierGroupId,
                            modifierOptionId = modifier.modifierOptionId,
                            groupCodeSnapshot = modifier.groupCodeSnapshot,
                            groupNameSnapshot = modifier.groupNameSnapshot,
                            optionCodeSnapshot = modifier.optionCodeSnapshot,
                            optionNameSnapshot = modifier.optionNameSnapshot,
                            applicationScopeSnapshot = modifier.applicationScopeSnapshot,
                            priceSnapshot = modifier.priceSnapshot,
                            quantity = modifier.quantity,
                        )
                    },
                )
            },
            subtotalMinor = entity.subtotalMinor,
            deliveryFeeMinor = entity.deliveryFeeMinor,
            totalMinor = entity.totalMinor,
            statusChangedAt = entity.statusChangedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            payment = entity.paymentMethodCode?.let { methodCode ->
                OrderPaymentSnapshot(
                    methodCode = methodCode,
                    methodName = entity.paymentMethodName ?: methodCode.displayName,
                )
            },
            statusHistory = statusHistory,
        )
    }

    private fun OrderDeliverySnapshotEntity.toDomain(): OrderDeliverySnapshot {
        return OrderDeliverySnapshot(
            method = method,
            methodName = methodName,
            priceMinor = priceMinor,
            currency = currency,
            zoneCode = zoneCode,
            zoneName = zoneName,
            estimatedDays = estimatedDays,
            estimatesMinutes = estimatesMinutes,
            pickupPointId = pickupPointId,
            pickupPointExternalId = pickupPointExternalId,
            pickupPointName = pickupPointName,
            pickupPointAddress = pickupPointAddress,
            address = address?.toDomain(),
        )
    }
}
