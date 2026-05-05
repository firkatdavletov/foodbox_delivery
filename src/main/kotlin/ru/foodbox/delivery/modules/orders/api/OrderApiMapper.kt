package ru.foodbox.delivery.modules.orders.api

import ru.foodbox.delivery.modules.delivery.api.dto.toResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderDeliveryResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusHistoryResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusHistoryEntryResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusSummaryResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderStatusTransitionResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderItemModifierResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderItemResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderPaymentResponse
import ru.foodbox.delivery.modules.orders.api.dto.OrderResponse
import ru.foodbox.delivery.modules.cart.pricing.domain.calculateCartItemPrice
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderStatusDefinition
import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistoryEntry
import ru.foodbox.delivery.modules.orders.domain.OrderStatusHistory
import ru.foodbox.delivery.modules.orders.domain.OrderStatusTransition

internal fun Order.toResponse(
    productThumbUrls: Map<java.util.UUID, String> = emptyMap(),
): OrderResponse {
    return OrderResponse(
        id = id,
        orderNumber = orderNumber,
        customerType = customerType,
        userId = userId,
        guestInstallId = guestInstallId,
        customerName = customerName,
        customerPhone = customerPhone,
        customerEmail = customerEmail,
        status = currentStatus.code,
        statusName = currentStatus.name,
        stateType = currentStatus.stateType,
        currentStatus = currentStatus.toSummaryResponse(),
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
                imageUrl = productThumbUrls[it.productId],
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
        statusHistory = statusHistory.map(OrderStatusHistoryEntry::toResponse),
        subtotalMinor = subtotalMinor,
        deliveryFeeMinor = deliveryFeeMinor,
        promoCode = promoCode,
        promoDiscountMinor = promoDiscountMinor,
        giftCertificateCodeLast4 = giftCertificateCodeLast4,
        giftCertificateAmountMinor = giftCertificateAmountMinor,
        totalMinor = totalMinor,
        statusChangedAt = statusChangedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun OrderStatusDefinition.toSummaryResponse(): OrderStatusSummaryResponse {
    return OrderStatusSummaryResponse(
        id = id,
        code = code,
        name = name,
        stateType = stateType,
        color = color,
        icon = icon,
        isFinal = isFinal,
        isCancellable = isCancellable,
        visibleToCustomer = visibleToCustomer,
    )
}

internal fun OrderStatusDefinition.toResponse(): OrderStatusResponse {
    return OrderStatusResponse(
        id = id,
        code = code,
        name = name,
        description = description,
        stateType = stateType,
        color = color,
        icon = icon,
        isInitial = isInitial,
        isFinal = isFinal,
        isCancellable = isCancellable,
        isActive = isActive,
        visibleToCustomer = visibleToCustomer,
        notifyCustomer = notifyCustomer,
        notifyStaff = notifyStaff,
        sortOrder = sortOrder,
    )
}

internal fun OrderStatusTransition.toResponse(): OrderStatusTransitionResponse {
    return OrderStatusTransitionResponse(
        id = id,
        fromStatus = fromStatus.toSummaryResponse(),
        toStatus = toStatus.toSummaryResponse(),
        requiredRole = requiredRole,
        isAutomatic = isAutomatic,
        guardCode = guardCode,
        isActive = isActive,
    )
}

internal fun OrderStatusHistory.toResponse(): OrderStatusHistoryResponse {
    return OrderStatusHistoryResponse(
        id = id,
        previousStatus = previousStatus?.toSummaryResponse(),
        currentStatus = currentStatus.toSummaryResponse(),
        changeSourceType = changeSourceType,
        changedByUserId = changedByUserId,
        comment = comment,
        changedAt = changedAt,
    )
}

internal fun OrderStatusHistoryEntry.toResponse(): OrderStatusHistoryEntryResponse {
    return OrderStatusHistoryEntryResponse(
        code = code,
        name = name,
        timestamp = timestamp,
    )
}
