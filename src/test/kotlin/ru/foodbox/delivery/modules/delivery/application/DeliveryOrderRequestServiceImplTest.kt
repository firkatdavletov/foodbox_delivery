package ru.foodbox.delivery.modules.delivery.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.catalog.domain.ProductUnit
import ru.foodbox.delivery.modules.delivery.domain.DeliveryValidationException
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryOffer
import ru.foodbox.delivery.modules.delivery.domain.OrderDeliveryOffer
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import ru.foodbox.delivery.modules.delivery.domain.repository.DeliveryOfferRepository
import ru.foodbox.delivery.modules.delivery.domain.repository.OrderDeliveryOfferRepository
import ru.foodbox.delivery.modules.delivery.infrastructure.yandex.YandexDeliveryProperties
import ru.foodbox.delivery.modules.orders.application.OrderStatusWorkflowDefaults
import ru.foodbox.delivery.modules.orders.domain.Order
import ru.foodbox.delivery.modules.orders.domain.OrderCustomerType
import ru.foodbox.delivery.modules.orders.domain.OrderDeliverySnapshot
import ru.foodbox.delivery.modules.orders.domain.OrderItem
import ru.foodbox.delivery.modules.orders.domain.OrderPaymentSnapshot
import ru.foodbox.delivery.modules.payments.domain.PaymentMethodCode
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class DeliveryOrderRequestServiceImplTest {

    @Test
    fun `creates confirms and stores yandex offer for order`() {
        val offerRepository = InMemoryDeliveryOfferRepository()
        val orderOfferRepository = InMemoryOrderDeliveryOfferRepository()
        val gateway = RecordingYandexDeliveryGateway(
            offers = listOf(
                YandexDeliveryOffer(
                    externalOfferId = "offer-expensive",
                    expiresAt = Instant.parse("2026-03-21T12:00:00Z"),
                    pricingMinor = 45_000,
                    pricingTotalMinor = 46_000,
                    currency = "RUB",
                    commissionOnDeliveryPercent = "2.2%",
                    commissionOnDeliveryAmountMinor = 1_000,
                    deliveryPolicy = "self_pickup",
                    deliveryIntervalFrom = null,
                    deliveryIntervalTo = null,
                    pickupIntervalFrom = null,
                    pickupIntervalTo = null,
                ),
                YandexDeliveryOffer(
                    externalOfferId = "offer-best",
                    expiresAt = Instant.parse("2026-03-21T11:00:00Z"),
                    pricingMinor = 39_000,
                    pricingTotalMinor = 40_000,
                    currency = "RUB",
                    commissionOnDeliveryPercent = "2.2%",
                    commissionOnDeliveryAmountMinor = 1_000,
                    deliveryPolicy = "self_pickup",
                    deliveryIntervalFrom = null,
                    deliveryIntervalTo = null,
                    pickupIntervalFrom = null,
                    pickupIntervalTo = null,
                ),
            ),
            confirmation = YandexConfirmedDeliveryRequest(
                externalRequestId = "request-1",
            ),
        )
        val service = DeliveryOrderRequestServiceImpl(
            yandexDeliveryGateway = gateway,
            yandexDeliveryProperties = YandexDeliveryProperties().apply {
                enabled = true
                token = "test-token"
                baseUrl = "https://example.test"
                sourceStationId = "source-station-1"
                sourcePickupIntervalHours = 12
            },
            deliveryOfferRepository = offerRepository,
            orderDeliveryOfferRepository = orderOfferRepository,
        )

        val order = yandexPickupOrder(paymentMethodCode = PaymentMethodCode.CARD_ON_DELIVERY)

        val confirmation = service.createAndConfirm(order)

        assertNotNull(confirmation)
        assertEquals("offer-best", confirmation.externalOfferId)
        assertEquals("request-1", confirmation.externalRequestId)
        assertEquals(40_000L, confirmation.deliveryFeeMinor)
        assertEquals(YandexOfferPaymentMethod.CARD_ON_RECEIPT, gateway.lastCreateRequest?.paymentMethod)
        assertEquals(40_000L, gateway.lastCreateRequest?.deliveryCostMinor)
        assertEquals("sku-1", gateway.lastCreateRequest?.items?.single()?.article)

        val savedOffer = offerRepository.saved.single()
        assertEquals("offer-best", savedOffer.externalOfferId)
        val savedLink = orderOfferRepository.findByOrderId(order.id)
        assertNotNull(savedLink)
        assertEquals(savedOffer.id, savedLink.offerId)
        assertEquals("request-1", savedLink.externalRequestId)
    }

    @Test
    fun `uses already paid offer mode for online payment methods`() {
        val gateway = RecordingYandexDeliveryGateway(
            offers = listOf(
                YandexDeliveryOffer(
                    externalOfferId = "offer-online",
                    expiresAt = Instant.parse("2026-03-21T11:00:00Z"),
                    pricingMinor = 39_000,
                    pricingTotalMinor = 39_000,
                    currency = "RUB",
                    commissionOnDeliveryPercent = null,
                    commissionOnDeliveryAmountMinor = null,
                    deliveryPolicy = "self_pickup",
                    deliveryIntervalFrom = null,
                    deliveryIntervalTo = null,
                    pickupIntervalFrom = null,
                    pickupIntervalTo = null,
                )
            ),
            confirmation = YandexConfirmedDeliveryRequest(
                externalRequestId = "request-online",
            ),
        )
        val service = DeliveryOrderRequestServiceImpl(
            yandexDeliveryGateway = gateway,
            yandexDeliveryProperties = YandexDeliveryProperties().apply {
                enabled = true
                token = "test-token"
                baseUrl = "https://example.test"
                sourceStationId = "source-station-1"
            },
            deliveryOfferRepository = InMemoryDeliveryOfferRepository(),
            orderDeliveryOfferRepository = InMemoryOrderDeliveryOfferRepository(),
        )

        val confirmation = service.createAndConfirm(
            yandexPickupOrder(paymentMethodCode = PaymentMethodCode.SBP)
        )

        assertNotNull(confirmation)
        assertEquals(YandexOfferPaymentMethod.ALREADY_PAID, gateway.lastCreateRequest?.paymentMethod)
        assertEquals(0L, gateway.lastCreateRequest?.deliveryCostMinor)
    }

    @Test
    fun `rejects cash payment for yandex pickup order`() {
        val gateway = RecordingYandexDeliveryGateway(
            offers = emptyList(),
            confirmation = YandexConfirmedDeliveryRequest(
                externalRequestId = "request-1",
            ),
        )
        val service = DeliveryOrderRequestServiceImpl(
            yandexDeliveryGateway = gateway,
            yandexDeliveryProperties = YandexDeliveryProperties().apply {
                enabled = true
                token = "test-token"
                baseUrl = "https://example.test"
                sourceStationId = "source-station-1"
            },
            deliveryOfferRepository = InMemoryDeliveryOfferRepository(),
            orderDeliveryOfferRepository = InMemoryOrderDeliveryOfferRepository(),
        )

        assertFailsWith<DeliveryValidationException> {
            service.createAndConfirm(
                yandexPickupOrder(paymentMethodCode = PaymentMethodCode.CASH)
            )
        }
        assertEquals(null, gateway.lastCreateRequest)
    }

    private fun yandexPickupOrder(paymentMethodCode: PaymentMethodCode): Order {
        val now = Instant.now()
        return Order(
            id = UUID.randomUUID(),
            orderNumber = "ORD-123",
            customerType = OrderCustomerType.GUEST,
            userId = null,
            guestInstallId = "install-1",
            customerName = "Test User",
            customerPhone = "+79990000000",
            customerEmail = "test@example.com",
            currentStatus = OrderStatusWorkflowDefaults.statuses.first { it.code == "PENDING" },
            delivery = OrderDeliverySnapshot(
                method = DeliveryMethodType.YANDEX_PICKUP_POINT,
                methodName = DeliveryMethodType.YANDEX_PICKUP_POINT.displayName,
                priceMinor = 40_000,
                currency = "RUB",
                zoneCode = null,
                zoneName = null,
                estimatedDays = 2,
                pickupPointId = null,
                pickupPointExternalId = "pickup-point-1",
                pickupPointName = "Yandex Point 1",
                pickupPointAddress = "Lenina, 1",
                address = null,
            ),
            comment = "Comment",
            items = listOf(
                OrderItem(
                    id = UUID.randomUUID(),
                    productId = UUID.randomUUID(),
                    variantId = null,
                    sku = "sku-1",
                    title = "T-Shirt",
                    unit = ProductUnit.PIECE,
                    quantity = 2,
                    priceMinor = 199_900,
                    totalMinor = 399_800,
                )
            ),
            subtotalMinor = 399_800,
            deliveryFeeMinor = 40_000,
            totalMinor = 439_800,
            statusChangedAt = now,
            createdAt = now,
            updatedAt = now,
            payment = OrderPaymentSnapshot(
                methodCode = paymentMethodCode,
                methodName = paymentMethodCode.displayName,
            ),
        )
    }

    private class RecordingYandexDeliveryGateway(
        private val offers: List<YandexDeliveryOffer>,
        private val confirmation: YandexConfirmedDeliveryRequest,
    ) : YandexDeliveryGateway {
        var lastCreateRequest: YandexOfferCreateRequest? = null
        var lastConfirmedOfferId: String? = null

        override fun isConfigured(): Boolean = true

        override fun detectLocations(query: String): List<YandexDeliveryLocationVariant> = emptyList()

        override fun listPickupPoints(geoId: Long, paymentMethod: String?): List<YandexPickupPointOption> = emptyList()

        override fun getPickupPoint(pickupPointId: String): YandexPickupPointOption? = null

        override fun calculateSelfPickupPrice(
            pickupPointId: String,
            subtotalMinor: Long,
            totalWeightGrams: Long?,
        ): YandexDeliveryPricingQuote {
            throw UnsupportedOperationException("Not used in delivery order request tests")
        }

        override fun createOffers(request: YandexOfferCreateRequest): List<YandexDeliveryOffer> {
            lastCreateRequest = request
            return offers
        }

        override fun confirmOffer(offerId: String): YandexConfirmedDeliveryRequest {
            lastConfirmedOfferId = offerId
            return confirmation
        }
    }

    private class InMemoryDeliveryOfferRepository : DeliveryOfferRepository {
        val saved = mutableListOf<DeliveryOffer>()

        override fun save(offer: DeliveryOffer): DeliveryOffer {
            saved.removeIf { it.id == offer.id }
            saved += offer
            return offer
        }

        override fun findById(offerId: UUID): DeliveryOffer? = saved.firstOrNull { it.id == offerId }
    }

    private class InMemoryOrderDeliveryOfferRepository : OrderDeliveryOfferRepository {
        private val saved = mutableListOf<OrderDeliveryOffer>()

        override fun save(link: OrderDeliveryOffer): OrderDeliveryOffer {
            saved.removeIf { it.id == link.id }
            saved += link
            return link
        }

        override fun findByOrderId(orderId: UUID): OrderDeliveryOffer? {
            return saved.firstOrNull { it.orderId == orderId }
        }
    }
}
