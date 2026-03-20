package ru.foodbox.delivery.modules.checkout.infrastructure.config

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentMethodRule
import ru.foodbox.delivery.modules.checkout.domain.repository.CheckoutPaymentMethodRuleRepository
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode

@Component
class InMemoryCheckoutPaymentMethodRuleRepository : CheckoutPaymentMethodRuleRepository {

    private val rules = listOf(
        CheckoutPaymentMethodRule(
            deliveryMethod = DeliveryMethodType.PICKUP,
            paymentMethods = listOf(
                PaymentMethodCode.CASH,
                PaymentMethodCode.CARD_ON_DELIVERY,
                PaymentMethodCode.CARD_ONLINE,
                PaymentMethodCode.SBP,
            ),
        ),
        CheckoutPaymentMethodRule(
            deliveryMethod = DeliveryMethodType.COURIER,
            paymentMethods = listOf(
                PaymentMethodCode.CASH,
                PaymentMethodCode.CARD_ON_DELIVERY,
                PaymentMethodCode.CARD_ONLINE,
                PaymentMethodCode.SBP,
            ),
        ),
    )

    init {
        validateRules(rules)
    }

    override fun findAll(): List<CheckoutPaymentMethodRule> = rules

    private fun validateRules(rules: List<CheckoutPaymentMethodRule>) {
        val duplicateDeliveryMethods = rules
            .groupBy { it.deliveryMethod }
            .filterValues { it.size > 1 }
            .keys
        require(duplicateDeliveryMethods.isEmpty()) {
            "Duplicate checkout payment rules found for delivery methods: ${duplicateDeliveryMethods.joinToString()}"
        }

        val missingDeliveryMethods = DeliveryMethodType.entries.filterNot { method ->
            method in DYNAMIC_DELIVERY_METHODS ||
            rules.any { it.deliveryMethod == method }
        }
        require(missingDeliveryMethods.isEmpty()) {
            "Missing checkout payment rules for delivery methods: ${missingDeliveryMethods.joinToString()}"
        }

        val duplicatedPaymentMethods = rules
            .mapNotNull { rule ->
                val duplicates = rule.paymentMethods
                    .groupingBy { it }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
                if (duplicates.isEmpty()) {
                    null
                } else {
                    "${rule.deliveryMethod}: ${duplicates.joinToString()}"
                }
            }
        require(duplicatedPaymentMethods.isEmpty()) {
            "Duplicate payment methods found in checkout rules: ${duplicatedPaymentMethods.joinToString()}"
        }
    }

    private companion object {
        private val DYNAMIC_DELIVERY_METHODS = setOf(DeliveryMethodType.YANDEX_PICKUP_POINT)
    }
}
