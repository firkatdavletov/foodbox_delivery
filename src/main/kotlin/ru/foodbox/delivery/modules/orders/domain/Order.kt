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
    var status: OrderStatus,
    val delivery: OrderDeliverySnapshot,
    val comment: String?,
    val items: List<OrderItem>,
    val subtotalMinor: Long,
    val deliveryFeeMinor: Long,
    val totalMinor: Long,
    val createdAt: Instant,
    var updatedAt: Instant,
    var payment: OrderPaymentSnapshot? = null,
) {
    fun updateStatus(newStatus: OrderStatus) {
        status = newStatus
        updatedAt = Instant.now()
    }

    fun updatePaymentSnapshot(snapshot: OrderPaymentSnapshot?) {
        payment = snapshot
        updatedAt = Instant.now()
    }
}
