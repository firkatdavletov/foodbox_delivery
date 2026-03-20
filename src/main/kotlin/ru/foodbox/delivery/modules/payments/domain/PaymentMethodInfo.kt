package ru.foodbox.delivery.modules.payments.domain

data class PaymentMethodInfo(
    val code: PaymentMethodCode,
    val name: String,
    val description: String?,
    val isOnline: Boolean,
    val isActive: Boolean,
)
