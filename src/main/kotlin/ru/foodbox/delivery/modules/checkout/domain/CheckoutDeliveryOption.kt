package ru.foodbox.delivery.modules.checkout.domain

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodSetting
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodInfo

data class CheckoutDeliveryOption(
    val deliveryMethod: DeliveryMethodSetting,
    val paymentMethods: List<PaymentMethodInfo>,
)
