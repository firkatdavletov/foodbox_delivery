package ru.foodbox.delivery.modules.orders.api

import ru.foodbox.delivery.modules.delivery.api.dto.toResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderDeliveryResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderItemResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderResponse
import ru.foodbox.delivery.modules.orders.domain.Order

internal fun Order.toResponse(): OrderResponse {
    return OrderResponse(
        id = id,
        orderNumber = orderNumber,
        customerType = customerType,
        userId = userId,
        guestInstallId = guestInstallId,
        customerName = customerName,
        customerPhone = customerPhone,
        customerEmail = customerEmail,
        status = status,
        deliveryMethod = delivery.method,
        delivery = OrderDeliveryResponse(
            method = delivery.method,
            methodName = delivery.methodName,
            priceMinor = delivery.priceMinor,
            currency = delivery.currency,
            zoneCode = delivery.zoneCode,
            zoneName = delivery.zoneName,
            estimatedDays = delivery.estimatedDays,
            pickupPointId = delivery.pickupPointId,
            pickupPointExternalId = delivery.pickupPointExternalId,
            pickupPointName = delivery.pickupPointName,
            pickupPointAddress = delivery.pickupPointAddress,
            address = delivery.address?.toResponse(),
        ),
        comment = comment,
        items = items.map {
            OrderItemResponse(
                id = it.id,
                productId = it.productId,
                variantId = it.variantId,
                title = it.title,
                unit = it.unit,
                quantity = it.quantity,
                priceMinor = it.priceMinor,
                totalMinor = it.totalMinor,
            )
        },
        subtotalMinor = subtotalMinor,
        deliveryFeeMinor = deliveryFeeMinor,
        totalMinor = totalMinor,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
