package ru.foodbox.delivery.modules.cart.api

import ru.foodbox.delivery.modules.cart.api.dto.CartDeliveryDraftResponse
import ru.foodbox.delivery.modules.cart.api.dto.CartItemResponse
import ru.foodbox.delivery.modules.cart.api.dto.CartResponse
import ru.foodbox.delivery.modules.cart.domain.Cart
import ru.foodbox.delivery.modules.cart.domain.CartDeliveryDraft
import ru.foodbox.delivery.modules.delivery.api.toResponse
import ru.foodbox.delivery.modules.delivery.api.dto.toResponse
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import java.time.Instant

internal fun Cart.toResponse(): CartResponse {
    return CartResponse(
        id = id,
        status = status,
        items = items.map {
            CartItemResponse(
                productId = it.productId,
                variantId = it.variantId,
                title = it.title,
                unit = it.unit,
                countStep = it.countStep,
                quantity = it.quantity,
                priceMinor = it.priceMinor,
                lineTotalMinor = it.lineTotalMinor(),
            )
        },
        totalPriceMinor = totalPriceMinor,
        delivery = deliveryDraft.toResponse(),
    )
}

internal fun CartDeliveryDraft?.toResponse(now: Instant = Instant.now()): CartDeliveryDraftResponse? {
    if (this == null) {
        return null
    }

    return CartDeliveryDraftResponse(
        deliveryMethod = deliveryMethod,
        address = deliveryAddress?.toResponse(),
        pickupPointId = pickupPointId,
        pickupPointExternalId = pickupPointExternalId,
        pickupPointName = pickupPointName,
        pickupPointAddress = pickupPointAddress,
        quote = quote?.let {
            DeliveryQuote(
                deliveryMethod = deliveryMethod,
                available = it.available,
                priceMinor = it.priceMinor,
                currency = it.currency,
                zoneCode = it.zoneCode,
                zoneName = it.zoneName,
                estimatedDays = it.estimatedDays,
                message = it.message,
                pickupPointId = pickupPointId,
                pickupPointExternalId = pickupPointExternalId,
                pickupPointName = pickupPointName,
                pickupPointAddress = pickupPointAddress,
            ).toResponse()
        },
        quoteExpired = quote?.isExpired(now) ?: false,
        updatedAt = updatedAt,
    )
}
