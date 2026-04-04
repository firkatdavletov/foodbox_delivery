package ru.foodbox.delivery.modules.orders.domain

import java.time.Instant
import java.util.UUID

data class Order(
    val id: UUID,
    val orderNumber: String,
    val customerType: OrderCustomerType,
    val userId: UUID?,
    val guestInstallId: String?,
    val customerName: String?,
    val customerPhone: String?,
    val customerEmail: String?,
    var currentStatus: OrderStatusDefinition,
    var delivery: OrderDeliverySnapshot,
    val comment: String?,
    val items: List<OrderItem>,
    val subtotalMinor: Long,
    var deliveryFeeMinor: Long,
    var totalMinor: Long,
    var statusChangedAt: Instant,
    val createdAt: Instant,
    var updatedAt: Instant,
    var payment: OrderPaymentSnapshot? = null,
) {
    fun updateStatus(
        newStatus: OrderStatusDefinition,
        changedAt: Instant = Instant.now(),
    ) {
        currentStatus = newStatus
        statusChangedAt = changedAt
        updatedAt = changedAt
    }

    fun updatePaymentSnapshot(snapshot: OrderPaymentSnapshot?) {
        payment = snapshot
        updatedAt = Instant.now()
    }

    fun updateDeliveryPricing(
        priceMinor: Long,
        currency: String,
    ) {
        delivery = delivery.copy(
            priceMinor = priceMinor,
            currency = currency,
        )
        deliveryFeeMinor = priceMinor
        totalMinor = subtotalMinor + priceMinor
        updatedAt = Instant.now()
    }
}
