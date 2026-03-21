package ru.foodbox.delivery.modules.delivery.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YandexPickupPointDeliveryCostCalculatorTest {

    @Test
    fun `returns quote for configured yandex pickup point`() {
        val calculator = YandexPickupPointDeliveryCostCalculator(
            yandexDeliveryGateway = StubYandexDeliveryGateway(
                configured = true,
                pickupPoint = YandexPickupPointOption(
                    id = "0198602de4a6749aba12e151bdf4caaa",
                    name = "Пункт выдачи заказов Яндекс Маркета",
                    address = "Москва, Пролетарский проспект, 19",
                    fullAddress = "Москва, Пролетарский проспект, 19",
                ),
                pricingQuote = YandexDeliveryPricingQuote(
                    priceMinor = 22_570,
                    currency = "RUB",
                    deliveryDays = 7,
                ),
            ),
        )

        val quote = calculator.calculate(
            DeliveryQuoteContext(
                subtotalMinor = 199_900,
                itemCount = 1,
                deliveryMethod = DeliveryMethodType.YANDEX_PICKUP_POINT,
                pickupPointExternalId = "0198602de4a6749aba12e151bdf4caaa",
            )
        )

        assertTrue(quote.available)
        assertEquals(22_570, quote.priceMinor)
        assertEquals("RUB", quote.currency)
        assertEquals(7, quote.estimatedDays)
        assertEquals("0198602de4a6749aba12e151bdf4caaa", quote.pickupPointExternalId)
        assertEquals("Пункт выдачи заказов Яндекс Маркета", quote.pickupPointName)
    }

    @Test
    fun `returns unavailable quote when pickup point was not found`() {
        val calculator = YandexPickupPointDeliveryCostCalculator(
            yandexDeliveryGateway = StubYandexDeliveryGateway(
                configured = true,
                pickupPoint = null,
                pricingQuote = YandexDeliveryPricingQuote(
                    priceMinor = 22_570,
                    currency = "RUB",
                    deliveryDays = 7,
                ),
            ),
        )

        val quote = calculator.calculate(
            DeliveryQuoteContext(
                subtotalMinor = 199_900,
                itemCount = 1,
                deliveryMethod = DeliveryMethodType.YANDEX_PICKUP_POINT,
                pickupPointExternalId = "missing-point",
            )
        )

        assertFalse(quote.available)
        assertEquals(null, quote.priceMinor)
        assertEquals("missing-point", quote.pickupPointExternalId)
    }

    private class StubYandexDeliveryGateway(
        private val configured: Boolean,
        private val pickupPoint: YandexPickupPointOption?,
        private val pricingQuote: YandexDeliveryPricingQuote,
    ) : YandexDeliveryGateway {

        override fun isConfigured(): Boolean = configured

        override fun detectLocations(query: String): List<YandexDeliveryLocationVariant> = emptyList()

        override fun listPickupPoints(geoId: Long, paymentMethod: String?): List<YandexPickupPointOption> =
            listOfNotNull(pickupPoint)

        override fun getPickupPoint(pickupPointId: String): YandexPickupPointOption? = pickupPoint

        override fun calculateSelfPickupPrice(
            pickupPointId: String,
            subtotalMinor: Long,
            totalWeightGrams: Long?,
        ): YandexDeliveryPricingQuote = pricingQuote

        override fun createOffers(request: YandexOfferCreateRequest): List<YandexDeliveryOffer> {
            throw UnsupportedOperationException("Not used in pickup point calculator tests")
        }

        override fun confirmOffer(offerId: String): YandexConfirmedDeliveryRequest {
            throw UnsupportedOperationException("Not used in pickup point calculator tests")
        }
    }
}
