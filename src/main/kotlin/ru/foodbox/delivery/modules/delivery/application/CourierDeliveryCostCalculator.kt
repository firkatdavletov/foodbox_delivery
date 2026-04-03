package ru.foodbox.delivery.modules.delivery.application

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.DeliveryValidationException
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryTariffRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryZoneRepository

@Component
class CourierDeliveryCostCalculator(
    private val deliveryZoneRepository: DeliveryZoneRepository,
    private val deliveryTariffRepository: DeliveryTariffRepository,
) : DeliveryCostCalculator {

    override fun supports(method: DeliveryMethodType): Boolean = method == DeliveryMethodType.COURIER

    override fun calculate(context: DeliveryQuoteContext): DeliveryQuote {
        val address = context.deliveryAddress?.normalized()
            ?: throw DeliveryValidationException("deliveryAddress is required for courier delivery")

        validateCourierAddress(address)

        val zone = resolveZone(address)
            ?: return DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = false,
                priceMinor = null,
                currency = DEFAULT_CURRENCY,
                message = "Не можем доставить по этому адресу",
            )

        val tariff = deliveryTariffRepository.findByMethodAndZone(
            method = DeliveryMethodType.COURIER,
            zoneId = zone.id,
        ) ?: return DeliveryQuote(
            deliveryMethod = DeliveryMethodType.COURIER,
            available = false,
            priceMinor = null,
            currency = DEFAULT_CURRENCY,
            zoneCode = zone.code,
            zoneName = zone.name,
            message = "Courier tariff is not configured for the selected zone",
        )

        if (!tariff.available) {
            return DeliveryQuote(
                deliveryMethod = DeliveryMethodType.COURIER,
                available = false,
                priceMinor = null,
                currency = tariff.currency,
                zoneCode = zone.code,
                zoneName = zone.name,
                message = "Courier delivery is temporarily unavailable for the selected zone",
            )
        }

        val priceMinor = if (tariff.freeFromAmountMinor != null && context.subtotalMinor >= tariff.freeFromAmountMinor) {
            0L
        } else {
            tariff.fixedPriceMinor
        }

        return DeliveryQuote(
            deliveryMethod = DeliveryMethodType.COURIER,
            available = true,
            priceMinor = priceMinor,
            currency = tariff.currency,
            zoneCode = zone.code,
            zoneName = zone.name,
            estimatedDays = tariff.estimatedDays,
            estimatesMinutes = tariff.deliveryMinutes,
        )
    }

    private fun validateCourierAddress(address: DeliveryAddress) {
        if (address.latitude != null && address.longitude != null) {
            return
        }
        requireNotNull(address.city) { "deliveryAddress.city is required for courier delivery" }
        requireNotNull(address.street) { "deliveryAddress.street is required for courier delivery" }
        requireNotNull(address.house) { "deliveryAddress.house is required for courier delivery" }
    }

    private fun resolveZone(address: DeliveryAddress) =
        address.latitude
            ?.takeIf { address.longitude != null }
            ?.let { latitude ->
                deliveryZoneRepository.findActiveByPoint(latitude, address.longitude!!)
            }
            ?: address.city?.let(deliveryZoneRepository::findActiveByCity)
            ?: address.postalCode?.let(deliveryZoneRepository::findActiveByPostalCode)

    private companion object {
        private const val DEFAULT_CURRENCY = "RUB"
    }
}
