package ru.foodbox.delivery.modules.delivery.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.DeliveryValidationException

@Component
class YandexPickupPointDeliveryCostCalculator(
    private val yandexDeliveryGateway: YandexDeliveryGateway,
) : DeliveryCostCalculator {

    private val logger = LoggerFactory.getLogger(YandexPickupPointDeliveryCostCalculator::class.java)

    override fun supports(method: DeliveryMethodType): Boolean = method == DeliveryMethodType.YANDEX_PICKUP_POINT

    override fun calculate(context: DeliveryQuoteContext): DeliveryQuote {
        val pickupPointExternalId = context.pickupPointExternalId?.trim()?.takeIf { it.isNotBlank() }
            ?: throw DeliveryValidationException("pickupPointExternalId is required for Yandex pickup point delivery")

        if (!yandexDeliveryGateway.isConfigured()) {
            return unavailableQuote(
                pickupPointExternalId = pickupPointExternalId,
                message = "Yandex pickup point delivery is not configured",
            )
        }

        return try {
            val pickupPoint = yandexDeliveryGateway.getPickupPoint(pickupPointExternalId)
                ?: return unavailableQuote(
                    pickupPointExternalId = pickupPointExternalId,
                    message = "Yandex pickup point not found",
                )

            val pricing = yandexDeliveryGateway.calculateSelfPickupPrice(
                pickupPointId = pickupPointExternalId,
                subtotalMinor = context.subtotalMinor,
                totalWeightGrams = context.totalWeightGrams,
            )

            DeliveryQuote(
                deliveryMethod = DeliveryMethodType.YANDEX_PICKUP_POINT,
                available = true,
                priceMinor = pricing.priceMinor,
                currency = pricing.currency,
                estimatedDays = pricing.deliveryDays,
                pickupPointExternalId = pickupPoint.id,
                pickupPointName = pickupPoint.name,
                pickupPointAddress = pickupPoint.fullAddress ?: pickupPoint.address,
            )
        } catch (ex: Exception) {
            logger.warn(
                "Yandex pickup point quote failed pickupPointExternalId={} errorType={}",
                pickupPointExternalId,
                ex.javaClass.simpleName,
            )
            unavailableQuote(
                pickupPointExternalId = pickupPointExternalId,
                message = "Yandex pickup point delivery is temporarily unavailable",
            )
        }
    }

    private fun unavailableQuote(
        pickupPointExternalId: String,
        message: String,
    ): DeliveryQuote {
        return DeliveryQuote(
            deliveryMethod = DeliveryMethodType.YANDEX_PICKUP_POINT,
            available = false,
            priceMinor = null,
            currency = DEFAULT_CURRENCY,
            message = message,
            pickupPointExternalId = pickupPointExternalId,
        )
    }

    private companion object {
        private const val DEFAULT_CURRENCY = "RUB"
    }
}
