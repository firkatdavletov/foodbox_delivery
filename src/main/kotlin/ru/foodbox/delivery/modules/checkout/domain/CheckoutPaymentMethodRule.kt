package ru.foodbox.delivery.modules.checkout.domain

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode

data class CheckoutPaymentMethodRule(
    val deliveryMethod: DeliveryMethodType,
    val paymentMethods: List<PaymentMethodCode>,
)
