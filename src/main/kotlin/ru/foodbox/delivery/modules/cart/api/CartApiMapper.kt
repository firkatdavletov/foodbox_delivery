package ru.foodbox.delivery.modules.cart.api

import ru.foodbox.delivery.modules.cart.api.dto.CartItemResponse
import ru.foodbox.delivery.modules.cart.api.dto.CartResponse
import ru.foodbox.delivery.modules.cart.domain.Cart

internal fun Cart.toResponse(): CartResponse {
    return CartResponse(
        id = id,
        status = status,
        totalPriceMinor = totalPriceMinor,
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
    )
}
