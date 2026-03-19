package ru.foodbox.delivery.modules.cart.api.dto

import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryAddressResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryQuoteResponse
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import java.time.Instant
import java.util.UUID

data class CartDeliveryDraftResponse(
    val deliveryMethod: DeliveryMethodType?,
    val address: DeliveryAddressResponse?,
    val pickupPointId: UUID?,
    val pickupPointExternalId: String?,
    val pickupPointName: String?,
    val pickupPointAddress: String?,
    val quote: DeliveryQuoteResponse?,
    val quoteExpired: Boolean,
    val updatedAt: Instant?,
)
