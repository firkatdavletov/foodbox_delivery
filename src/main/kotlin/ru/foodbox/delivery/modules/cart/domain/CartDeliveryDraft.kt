package ru.foodbox.delivery.modules.cart.domain

import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.time.Instant
import java.util.UUID

data class CartDeliveryDraft(
    val deliveryMethod: DeliveryMethodType,
    val deliveryAddress: DeliveryAddress?,
    val pickupPointId: UUID?,
    val pickupPointExternalId: String?,
    val pickupPointName: String?,
    val pickupPointAddress: String?,
    val quote: CartDeliveryQuote?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun invalidateQuote(now: Instant): CartDeliveryDraft {
        return copy(
            quote = null,
            updatedAt = now,
        )
    }
}

data class CartDeliveryQuote(
    val available: Boolean,
    val priceMinor: Long?,
    val currency: String,
    val zoneCode: String?,
    val zoneName: String?,
    val estimatedDays: Int?,
    val message: String?,
    val calculatedAt: Instant,
    val expiresAt: Instant,
) {
    fun isExpired(now: Instant): Boolean = expiresAt.isBefore(now)
}
