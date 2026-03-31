package ru.foodbox.delivery.modules.checkout.domain

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode

object CheckoutPaymentRuleDefaults {
    val dynamicDeliveryMethods: Set<DeliveryMethodType> = setOf(DeliveryMethodType.YANDEX_PICKUP_POINT)

    fun defaultRules(): List<CheckoutPaymentMethodRule> {
        val defaultPaymentMethods = listOf(
            PaymentMethodCode.CASH,
            PaymentMethodCode.CARD_ON_DELIVERY,
            PaymentMethodCode.CARD_ONLINE,
            PaymentMethodCode.SBP,
        )

        return listOf(
            CheckoutPaymentMethodRule(
                deliveryMethod = DeliveryMethodType.PICKUP,
                paymentMethods = defaultPaymentMethods,
            ),
            CheckoutPaymentMethodRule(
                deliveryMethod = DeliveryMethodType.COURIER,
                paymentMethods = defaultPaymentMethods,
            ),
        )
    }
}
