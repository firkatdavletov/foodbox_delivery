package ru.foodbox.delivery.modules.delivery.application

import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext

interface DeliveryCostCalculator {
    fun supports(method: DeliveryMethodType): Boolean
    fun calculate(context: DeliveryQuoteContext): DeliveryQuote
}
