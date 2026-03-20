package ru.foodbox.delivery.modules.checkout.api.dto

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode

data class CheckoutOptionsResponse(
    val options: List<CheckoutDeliveryOptionResponse>,
)

data class CheckoutDeliveryOptionResponse(
    val code: DeliveryMethodType,
    val name: String,
    val requiresAddress: Boolean,
    val requiresPickupPoint: Boolean,
    val paymentMethods: List<CheckoutPaymentMethodResponse>,
)

data class CheckoutPaymentMethodResponse(
    val code: PaymentMethodCode,
    val name: String,
    val description: String?,
    val isOnline: Boolean,
    val isActive: Boolean,
)
