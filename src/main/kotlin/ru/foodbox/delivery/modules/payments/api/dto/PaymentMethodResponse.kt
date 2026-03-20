package ru.foodbox.delivery.modules.payments.api.dto

import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode

data class PaymentMethodResponse(
    val code: PaymentMethodCode,
    val name: String,
    val description: String?,
    val isOnline: Boolean,
    val isActive: Boolean,
)
