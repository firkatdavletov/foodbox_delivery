package ru.foodbox.delivery.modules.delivery.infrastructure.yandex

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.foodbox.delivery.modules.delivery.application.YandexOfferCreateRequest
import ru.foodbox.delivery.modules.delivery.application.YandexOfferItem
import ru.foodbox.delivery.modules.delivery.application.YandexOfferPaymentMethod

class YandexDeliveryApiClientTest {

    @Test
    fun `rounds self pickup price up to nearest 100 rubles`() {
        withClient(
            pricingResponseBody = """
                {
                  "pricing_total": "365.56 RUB",
                  "delivery_days": 3
                }
            """.trimIndent()
        ) { client, requestMethod, authorizationHeader, _, _ ->
            val quote = client.calculateSelfPickupPrice(
                pickupPointId = "pickup-point-1",
                subtotalMinor = 199_900,
                totalWeightGrams = 1_500,
            )

            assertEquals(40_000, quote.priceMinor)
            assertEquals("RUB", quote.currency)
            assertEquals(3, quote.deliveryDays)
            assertEquals("POST", requestMethod.get())
            assertEquals("Bearer test-token", authorizationHeader.get())
        }
    }

    @Test
    fun `keeps self pickup price unchanged when already multiple of 100 rubles`() {
        withClient(
            pricingResponseBody = """
                {
                  "pricing_total": "400.00 RUB",
                  "delivery_days": 5
                }
            """.trimIndent()
        ) { client, requestMethod, authorizationHeader, _, _ ->
            val quote = client.calculateSelfPickupPrice(
                pickupPointId = "pickup-point-2",
                subtotalMinor = 199_900,
                totalWeightGrams = 1_500,
            )

            assertEquals(40_000, quote.priceMinor)
            assertEquals("RUB", quote.currency)
            assertEquals(5, quote.deliveryDays)
            assertEquals("POST", requestMethod.get())
            assertEquals("Bearer test-token", authorizationHeader.get())
        }
    }

    @Test
    fun `creates offers with payment method and parses response`() {
        withClient(
            offersResponseBody = """
                {
                  "offers": [
                    {
                      "offer_id": "offer-1",
                      "expires_at": "2026-03-21T10:00:00Z",
                      "offer_details": {
                        "delivery_interval": {
                          "min": "2026-03-22T10:00:00Z",
                          "max": "2026-03-22T12:00:00Z",
                          "policy": "self_pickup"
                        },
                        "pickup_interval": {
                          "min": "2026-03-21T11:00:00Z",
                          "max": "2026-03-21T13:00:00Z"
                        },
                        "pricing": "390.00 RUB",
                        "pricing_commission_on_delivery_payment": "2.2%",
                        "pricing_commission_on_delivery_payment_amount": "10.00 RUB",
                        "pricing_total": "400.00 RUB"
                      }
                    }
                  ]
                }
            """.trimIndent()
        ) { client, requestMethod, authorizationHeader, requestUri, requestBody ->
            val offers = client.createOffers(
                YandexOfferCreateRequest(
                    operatorRequestId = "order-1",
                    comment = "Comment",
                    destinationPickupPointId = "pickup-point-1",
                    recipientName = "Test User",
                    recipientPhone = "+79990000000",
                    recipientEmail = "user@example.com",
                    items = listOf(
                        YandexOfferItem(
                            id = "item-1",
                            name = "T-Shirt",
                            article = "sku-1",
                            count = 2,
                            unitPriceMinor = 199_900,
                        )
                    ),
                    paymentMethod = YandexOfferPaymentMethod.CARD_ON_RECEIPT,
                    deliveryCostMinor = 40_000,
                    pickupIntervalFrom = java.time.Instant.parse("2026-03-21T09:00:00Z"),
                    pickupIntervalTo = java.time.Instant.parse("2026-03-21T18:00:00Z"),
                )
            )

            assertEquals(1, offers.size)
            assertEquals("offer-1", offers.first().externalOfferId)
            assertEquals(39_000L, offers.first().pricingMinor)
            assertEquals(40_000L, offers.first().pricingTotalMinor)
            assertEquals(1_000L, offers.first().commissionOnDeliveryAmountMinor)
            assertEquals("RUB", offers.first().currency)
            assertEquals("POST", requestMethod.get())
            assertEquals("Bearer test-token", authorizationHeader.get())
            assertTrue(requestUri.get()?.contains("/api/b2b/platform/offers/create?send_unix=false") == true)
            assertTrue(requestBody.get()?.contains("\"payment_method\":\"card_on_receipt\"") == true)
            assertTrue(requestBody.get()?.contains("\"platform_id\":\"pickup-point-1\"") == true)
        }
    }

    @Test
    fun `confirms offer and returns request id`() {
        withClient(
            confirmResponseBody = """
                {
                  "request_id": "request-1"
                }
            """.trimIndent()
        ) { client, requestMethod, authorizationHeader, requestUri, requestBody ->
            val confirmation = client.confirmOffer("offer-1")

            assertEquals("request-1", confirmation.externalRequestId)
            assertEquals("POST", requestMethod.get())
            assertEquals("Bearer test-token", authorizationHeader.get())
            assertEquals("/api/b2b/platform/offers/confirm", requestUri.get())
            assertTrue(requestBody.get()?.contains("\"offer_id\":\"offer-1\"") == true)
        }
    }

    private fun withClient(
        pricingResponseBody: String = """
            {
              "pricing_total": "365.56 RUB",
              "delivery_days": 3
            }
        """.trimIndent(),
        offersResponseBody: String = """
            {
              "offers": []
            }
        """.trimIndent(),
        confirmResponseBody: String = """
            {
              "request_id": "request-1"
            }
        """.trimIndent(),
        block: (
            YandexDeliveryApiClient,
            AtomicReference<String?>,
            AtomicReference<String?>,
            AtomicReference<String?>,
            AtomicReference<String?>,
        ) -> Unit,
    ) {
        val requestMethod = AtomicReference<String?>()
        val authorizationHeader = AtomicReference<String?>()
        val requestUri = AtomicReference<String?>()
        val requestBody = AtomicReference<String?>()
        val server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/api/b2b/platform/pricing-calculator") { exchange ->
                requestMethod.set(exchange.requestMethod)
                authorizationHeader.set(exchange.requestHeaders.getFirst("Authorization"))
                requestUri.set(exchange.requestURI.toString())
                requestBody.set(String(exchange.requestBody.readAllBytes()))
                val responseBytes = pricingResponseBody.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                exchange.responseBody.use { it.write(responseBytes) }
            }
            createContext("/api/b2b/platform/offers/create") { exchange ->
                requestMethod.set(exchange.requestMethod)
                authorizationHeader.set(exchange.requestHeaders.getFirst("Authorization"))
                requestUri.set(exchange.requestURI.toString())
                requestBody.set(String(exchange.requestBody.readAllBytes()))
                val responseBytes = offersResponseBody.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                exchange.responseBody.use { it.write(responseBytes) }
            }
            createContext("/api/b2b/platform/offers/confirm") { exchange ->
                requestMethod.set(exchange.requestMethod)
                authorizationHeader.set(exchange.requestHeaders.getFirst("Authorization"))
                requestUri.set(exchange.requestURI.toString())
                requestBody.set(String(exchange.requestBody.readAllBytes()))
                val responseBytes = confirmResponseBody.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                exchange.responseBody.use { it.write(responseBytes) }
            }
            start()
        }

        try {
            val properties = YandexDeliveryProperties().apply {
                enabled = true
                token = "test-token"
                baseUrl = "http://127.0.0.1:${server.address.port}"
                sourceStationId = "source-station-id"
            }
            val client = YandexDeliveryApiClient(properties, RestClient.builder())

            block(client, requestMethod, authorizationHeader, requestUri, requestBody)
        } finally {
            server.stop(0)
        }
    }
}
