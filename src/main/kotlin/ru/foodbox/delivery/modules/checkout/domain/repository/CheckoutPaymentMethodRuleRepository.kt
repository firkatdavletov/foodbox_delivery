package ru.foodbox.delivery.modules.checkout.domain.repository

import ru.foodbox.delivery.modules.checkout.domain.CheckoutPaymentMethodRule

interface CheckoutPaymentMethodRuleRepository {
    fun findAll(): List<CheckoutPaymentMethodRule>
}
