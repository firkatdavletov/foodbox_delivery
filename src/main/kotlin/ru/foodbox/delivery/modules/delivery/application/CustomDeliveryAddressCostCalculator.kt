package ru.foodbox.delivery.modules.delivery.application

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext

@Component
class CustomDeliveryAddressCostCalculator : DeliveryCostCalculator {

    override fun supports(method: DeliveryMethodType): Boolean {
        return method == DeliveryMethodType.CUSTOM_DELIVERY_ADDRESS
    }

    override fun calculate(context: DeliveryQuoteContext): DeliveryQuote {
        return DeliveryQuote(
            deliveryMethod = DeliveryMethodType.CUSTOM_DELIVERY_ADDRESS,
            available = true,
            priceMinor = 0,
            currency = DEFAULT_CURRENCY,
        )
    }

    private companion object {
        private const val DEFAULT_CURRENCY = "RUB"
    }
}
