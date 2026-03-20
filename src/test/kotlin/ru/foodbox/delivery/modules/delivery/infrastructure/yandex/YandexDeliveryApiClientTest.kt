package ru.foodbox.delivery.modules.delivery.infrastructure.yandex

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

class YandexDeliveryApiClientTest {

    @Test
    fun `rounds self pickup price up to nearest 100 rubles`() {
        withClient(
            responseBody = """
                {
                  "pricing_total": "365.56 RUB",
                  "delivery_days": 3
                }
            """.trimIndent()
        ) { client, requestMethod, authorizationHeader ->
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
            responseBody = """
                {
                  "pricing_total": "400.00 RUB",
                  "delivery_days": 5
                }
            """.trimIndent()
        ) { client, requestMethod, authorizationHeader ->
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

    private fun withClient(
        responseBody: String,
        block: (YandexDeliveryApiClient, AtomicReference<String?>, AtomicReference<String?>) -> Unit,
    ) {
        val requestMethod = AtomicReference<String?>()
        val authorizationHeader = AtomicReference<String?>()
        val responseBytes = responseBody.toByteArray()
        val server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/api/b2b/platform/pricing-calculator") { exchange ->
                requestMethod.set(exchange.requestMethod)
                authorizationHeader.set(exchange.requestHeaders.getFirst("Authorization"))
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

            block(client, requestMethod, authorizationHeader)
        } finally {
            server.stop(0)
        }
    }
}
