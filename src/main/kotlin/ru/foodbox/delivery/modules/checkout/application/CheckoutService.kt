package ru.foodbox.delivery.modules.checkout.application

import ru.foodbox.delivery.modules.checkout.domain.CheckoutDeliveryOption

interface CheckoutService {
    fun getAvailableOptions(query: CheckoutOptionsQuery = CheckoutOptionsQuery()): List<CheckoutDeliveryOption>
}
