package ru.foodbox.delivery.modules.delivery.domain

import java.time.Instant
import java.util.UUID

data class OrderDeliveryOffer(
    val id: UUID,
    val orderId: UUID,
    val offerId: UUID,
    val externalRequestId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val confirmedAt: Instant?,
) {
    fun confirm(externalRequestId: String, now: Instant): OrderDeliveryOffer {
        return copy(
            externalRequestId = externalRequestId,
            updatedAt = now,
            confirmedAt = now,
        )
    }
}
