package ru.foodbox.delivery.modules.delivery.application

import org.springframework.stereotype.Service
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.DeliveryValidationException
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryMethodSettingRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.PickupPointRepository
import ru.foodbox.delivery.modules.payments.application.PaymentService

@Service
class DeliveryServiceImpl(
    private val deliveryMethodSettingRepository: DeliveryMethodSettingRepository,
    private val pickupPointRepository: PickupPointRepository,
    private val yandexDeliveryGateway: YandexDeliveryGateway,
    private val paymentService: PaymentService,
    private val calculators: List<DeliveryCostCalculator>,
) : DeliveryService {

    override fun getAvailableMethods(): List<DeliveryMethodType> {
        return deliveryMethodSettingRepository.findAll()
            .filter { it.enabled }
            .map { it.method }
            .filter { method ->
                method != DeliveryMethodType.YANDEX_PICKUP_POINT || yandexDeliveryGateway.isConfigured()
            }
    }

    override fun getActivePickupPoints() = pickupPointRepository.findAllActive()

    override fun detectYandexLocations(query: String) = yandexDeliveryGateway.detectLocations(query)

    override fun getYandexPickupPoints(geoId: Long) = yandexDeliveryGateway.listPickupPoints(
        geoId = geoId,
        paymentMethod = resolveYandexPickupPointsPaymentMethod(),
    )

    override fun getYandexPickupPoint(pickupPointId: String) = yandexDeliveryGateway.getPickupPoint(pickupPointId)

    override fun calculateQuote(context: DeliveryQuoteContext): DeliveryQuote {
        require(context.subtotalMinor >= 0) { "subtotalMinor must be greater than or equal to zero" }
        require(context.itemCount > 0) { "itemCount must be greater than zero" }

        val calculator = calculators.firstOrNull { it.supports(context.deliveryMethod) }
            ?: throw DeliveryValidationException("Unsupported delivery method: ${context.deliveryMethod}")

        return calculator.calculate(context)
    }

    private fun resolveYandexPickupPointsPaymentMethod(): String {
        val hasOnlinePaymentMethods = paymentService.getAvailableMethods().any { paymentMethod ->
            paymentMethod.isActive && paymentMethod.isOnline
        }

        return if (hasOnlinePaymentMethods) {
            YANDEX_PAYMENT_METHOD_ALREADY_PAID
        } else {
            YANDEX_PAYMENT_METHOD_CARD_ON_RECEIPT
        }
    }

    private companion object {
        private const val YANDEX_PAYMENT_METHOD_ALREADY_PAID = "already_paid"
        private const val YANDEX_PAYMENT_METHOD_CARD_ON_RECEIPT = "card_on_receipt"
    }
}
