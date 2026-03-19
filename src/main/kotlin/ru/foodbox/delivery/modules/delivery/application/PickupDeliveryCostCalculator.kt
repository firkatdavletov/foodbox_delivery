package ru.foodbox.delivery.modules.delivery.application

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.DeliveryValidationException
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryTariffRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository

@Component
class PickupDeliveryCostCalculator(
    private val pickupPointRepository: PickupPointRepository,
    private val deliveryTariffRepository: DeliveryTariffRepository,
) : DeliveryCostCalculator {

    override fun supports(method: DeliveryMethodType): Boolean = method == DeliveryMethodType.PICKUP

    override fun calculate(context: DeliveryQuoteContext): DeliveryQuote {
        val pickupPointId = context.pickupPointId
            ?: throw DeliveryValidationException("pickupPointId is required for pickup delivery")

        val pickupPoint = pickupPointRepository.findById(pickupPointId)
            ?: throw DeliveryValidationException("Pickup point not found")

        if (!pickupPoint.active) {
            return DeliveryQuote(
                deliveryMethod = DeliveryMethodType.PICKUP,
                available = false,
                priceMinor = null,
                currency = DEFAULT_CURRENCY,
                message = "Pickup point is inactive",
                pickupPointId = pickupPoint.id,
                pickupPointName = pickupPoint.name,
                pickupPointAddress = pickupPoint.address.toSingleLine(),
            )
        }

        val tariff = deliveryTariffRepository.findDefaultByMethod(DeliveryMethodType.PICKUP)
        val available = tariff?.available ?: true
        val priceMinor = tariff?.fixedPriceMinor ?: 0L
        val estimatedDays = tariff?.estimatedDays ?: 0
        val currency = tariff?.currency ?: DEFAULT_CURRENCY

        return DeliveryQuote(
            deliveryMethod = DeliveryMethodType.PICKUP,
            available = available,
            priceMinor = if (available) priceMinor else null,
            currency = currency,
            estimatedDays = if (available) estimatedDays else null,
            message = if (available) null else "Pickup is temporarily unavailable",
            pickupPointId = pickupPoint.id,
            pickupPointName = pickupPoint.name,
            pickupPointAddress = pickupPoint.address.toSingleLine(),
        )
    }

    private companion object {
        private const val DEFAULT_CURRENCY = "RUB"
    }
}
