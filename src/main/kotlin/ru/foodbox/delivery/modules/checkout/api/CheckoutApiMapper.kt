package ru.foodbox.delivery.modules.checkout.api

import ru.foodbox.delivery.modules.checkout.api.dto.CheckoutDeliveryOptionResponse
import ru.foodbox.delivery.modules.checkout.api.dto.CheckoutOptionsResponse
import ru.foodbox.delivery.modules.checkout.api.dto.CheckoutPaymentMethodResponse
import ru.foodbox.delivery.modules.checkout.domain.CheckoutDeliveryOption

internal fun List<CheckoutDeliveryOption>.toResponse(): CheckoutOptionsResponse {
    return CheckoutOptionsResponse(
        options = map { it.toResponse() },
    )
}

internal fun CheckoutDeliveryOption.toResponse(): CheckoutDeliveryOptionResponse {
    return CheckoutDeliveryOptionResponse(
        code = deliveryMethod.method,
        name = deliveryMethod.title,
        description = deliveryMethod.description,
        requiresAddress = deliveryMethod.method.requiresAddress,
        requiresPickupPoint = deliveryMethod.method.requiresPickupPoint,
        paymentMethods = paymentMethods.map {
            CheckoutPaymentMethodResponse(
                code = it.code,
                name = it.name,
                description = it.description,
                isOnline = it.isOnline,
                isActive = it.isActive,
            )
        },
    )
}
