package ru.foodbox.delivery.modules.orders.api

import ru.foodbox.delivery.modules.delivery.api.dto.toResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderDeliveryResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderItemModifierResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderItemResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderPaymentResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderResponse
import ru.foodbox.delivery.modules.cart.pricing.domain.calculateCartItemPrice
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
        payment = payment?.let {
            OrderPaymentResponse(
                code = it.methodCode,
                name = it.methodName,
            )
        },
        deliveryMethod = delivery.method,
        delivery = OrderDeliveryResponse(
            method = delivery.method,
            methodName = delivery.methodName,
            priceMinor = delivery.priceMinor,
            currency = delivery.currency,
            zoneCode = delivery.zoneCode,
            zoneName = delivery.zoneName,
            estimatedDays = delivery.estimatedDays,
            estimatesMinutes = delivery.estimatesMinutes,
            pickupPointId = delivery.pickupPointId,
            pickupPointExternalId = delivery.pickupPointExternalId,
            pickupPointName = delivery.pickupPointName,
            pickupPointAddress = delivery.pickupPointAddress,
            address = delivery.address?.toResponse(),
        ),
        comment = comment,
        items = items.map {
            val price = calculateCartItemPrice(
                basePriceMinor = it.priceMinor,
                lineQuantity = it.quantity,
                modifiers = it.modifiers,
            )
            OrderItemResponse(
                id = it.id,
                productId = it.productId,
                variantId = it.variantId,
                sku = it.sku,
                title = it.title,
                unit = it.unit,
                quantity = it.quantity,
                priceMinor = it.priceMinor,
                modifiersTotalMinor = price.lineTotalMinor - it.priceMinor * it.quantity,
                totalMinor = it.totalMinor,
                modifiers = it.modifiers.map { modifier ->
                    OrderItemModifierResponse(
                        modifierGroupId = modifier.modifierGroupId,
                        modifierOptionId = modifier.modifierOptionId,
                        groupCode = modifier.groupCodeSnapshot,
                        groupName = modifier.groupNameSnapshot,
                        optionCode = modifier.optionCodeSnapshot,
                        optionName = modifier.optionNameSnapshot,
                        applicationScope = modifier.applicationScopeSnapshot,
                        priceMinor = modifier.priceSnapshot,
                        quantity = modifier.quantity,
                    )
                },
            )
        },
        subtotalMinor = subtotalMinor,
        deliveryFeeMinor = deliveryFeeMinor,
        totalMinor = totalMinor,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
