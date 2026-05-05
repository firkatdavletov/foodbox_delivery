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
    var promoCode: String? = null,
    var promoDiscountMinor: Long = 0,
    var giftCertificateId: UUID? = null,
    var giftCertificateCodeLast4: String? = null,
    var giftCertificateAmountMinor: Long = 0,
    var totalMinor: Long,
    var statusChangedAt: Instant,
    val createdAt: Instant,
    var updatedAt: Instant,
    var payment: OrderPaymentSnapshot? = null,
    var statusHistory: List<OrderStatusHistoryEntry> = emptyList(),
) {
    fun updateStatus(
        newStatus: OrderStatusDefinition,
        changedAt: Instant = Instant.now(),
    ) {
        ensureStatusHistoryInitialized()
        currentStatus = newStatus
        statusChangedAt = changedAt
        updatedAt = changedAt
        statusHistory = statusHistory + OrderStatusHistoryEntry(
            code = newStatus.code,
            name = newStatus.name,
            timestamp = changedAt,
        )
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
        totalMinor = calculateTotalMinor()
        updatedAt = Instant.now()
    }

    fun applyPricingAdjustments(
        promoCode: String?,
        promoDiscountMinor: Long,
        giftCertificateId: UUID?,
        giftCertificateCodeLast4: String?,
        giftCertificateAmountMinor: Long,
    ) {
        require(promoDiscountMinor >= 0) { "promoDiscountMinor must be non-negative" }
        require(giftCertificateAmountMinor >= 0) { "giftCertificateAmountMinor must be non-negative" }

        this.promoCode = promoCode?.trim()?.takeIf { it.isNotBlank() }
        this.promoDiscountMinor = promoDiscountMinor
        this.giftCertificateId = giftCertificateId
        this.giftCertificateCodeLast4 = giftCertificateCodeLast4?.trim()?.takeIf { it.isNotBlank() }
        this.giftCertificateAmountMinor = giftCertificateAmountMinor
        this.totalMinor = calculateTotalMinor()
        updatedAt = Instant.now()
    }

    fun ensureStatusHistoryInitialized() {
        if (statusHistory.isNotEmpty()) {
            return
        }

        statusHistory = listOf(
            OrderStatusHistoryEntry(
                code = currentStatus.code,
                name = currentStatus.name,
                timestamp = statusChangedAt,
            )
        )
    }

    fun grossTotalMinor(): Long {
        return subtotalMinor + deliveryFeeMinor
    }

    private fun calculateTotalMinor(): Long {
        val total = grossTotalMinor() - promoDiscountMinor - giftCertificateAmountMinor
        require(total >= 0) { "Order total must be non-negative" }
        return total
    }
}
