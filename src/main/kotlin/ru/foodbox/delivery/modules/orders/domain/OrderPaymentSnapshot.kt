package ru.foodbox.delivery.modules.orders.domain

import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode

data class OrderPaymentSnapshot(
    val methodCode: PaymentMethodCode,
    val methodName: String,
)
