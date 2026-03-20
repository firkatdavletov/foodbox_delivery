package ru.foodbox.delivery.modules.checkout.domain

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo

data class CheckoutDeliveryOption(
    val deliveryMethod: DeliveryMethodType,
    val paymentMethods: List<PaymentMethodInfo>,
)
