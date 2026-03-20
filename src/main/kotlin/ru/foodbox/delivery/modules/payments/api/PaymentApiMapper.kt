package ru.foodbox.delivery.modules.payments.api

import ru.foodbox.delivery.modules.payments.api.dto.PaymentMethodResponse
import ru.foodbox.delivery.modules.payments.api.dto.PaymentResponse
import ru.foodbox.delivery.modules.payments.domain.Payment
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo

internal fun PaymentMethodInfo.toResponse(): PaymentMethodResponse {
    return PaymentMethodResponse(
        code = code,
        name = name,
        description = description,
        isOnline = isOnline,
        isActive = isActive,
    )
}

internal fun Payment.toResponse(): PaymentResponse {
    return PaymentResponse(
        id = id,
        orderId = orderId,
        paymentMethodCode = paymentMethodCode,
        paymentMethodName = paymentMethodName,
        status = status,
        amountMinor = amountMinor,
        currency = currency,
        isOnline = paymentMethodCode.isOnline,
        providerCode = providerCode,
        externalPaymentId = externalPaymentId,
        confirmationUrl = confirmationUrl,
        details = details,
        createdAt = createdAt,
        updatedAt = updatedAt,
        paidAt = paidAt,
    )
}
