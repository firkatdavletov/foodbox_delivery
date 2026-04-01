package ru.foodbox.delivery.modules.delivery.infrastructure.yandex.geocoder

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YandexGeocoderApiClientTest {

    @Test
    fun `reverse geocodes coordinates into structured delivery address`() {
        withClient(
            responseBody = """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": [
                        {
                          "GeoObject": {
                            "metaDataProperty": {
                              "GeocoderMetaData": {
                                "Address": {
                                  "postal_code": "620014",
                                  "Components": [
                                    { "kind": "country", "name": "Россия" },
                                    { "kind": "province", "name": "Свердловская область" },
                                    { "kind": "locality", "name": "Екатеринбург" },
                                    { "kind": "street", "name": "улица 8 Марта" },
                                    { "kind": "house", "name": "10" }
                                  ]
                                }
                              }
                            },
                            "Point": {
                              "pos": "60.606000 56.839000"
                            }
                          }
                        }
                      ]
                    }
                  }
                }
            """.trimIndent()
        ) { client, requestMethod, requestQuery ->
            val address = client.reverseGeocode(
                latitude = 56.8389,
                longitude = 60.6057,
            )

            assertEquals("GET", requestMethod.get())
            assertEquals("test-api-key", requestQuery.getValue("apikey"))
            assertEquals("60.6057,56.8389", requestQuery.getValue("geocode"))
            assertEquals("ru_RU", requestQuery.getValue("lang"))
            assertEquals("house", requestQuery.getValue("kind"))
            assertEquals("1", requestQuery.getValue("results"))
            assertEquals("json", requestQuery.getValue("format"))
            assertEquals("longlat", requestQuery.getValue("sco"))

            requireNotNull(address)
            assertEquals("Россия", address.country)
            assertEquals("Свердловская область", address.region)
            assertEquals("Екатеринбург", address.city)
            assertEquals("улица 8 Марта", address.street)
            assertEquals("10", address.house)
            assertEquals("620014", address.postalCode)
            assertEquals(56.839, address.latitude)
            assertEquals(60.606, address.longitude)
        }
    }

    @Test
    fun `returns null when Yandex Geocoder returns no address`() {
        withClient(
            responseBody = """
                {
                  "response": {
                    "GeoObjectCollection": {
                      "featureMember": []
                    }
                  }
                }
            """.trimIndent()
        ) { client, _, _ ->
            val address = client.reverseGeocode(
                latitude = 56.8389,
                longitude = 60.6057,
            )

            assertNull(address)
        }
    }

    private fun withClient(
        responseBody: String,
        block: (
            YandexGeocoderApiClient,
            AtomicReference<String?>,
            AtomicReference<String?>,
        ) -> Unit,
    ) {
        val requestMethod = AtomicReference<String?>()
        val requestQuery = AtomicReference<String?>()
        val server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/v1") { exchange ->
                requestMethod.set(exchange.requestMethod)
                requestQuery.set(exchange.requestURI.rawQuery)

                val responseBytes = responseBody.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                exchange.responseBody.use { it.write(responseBytes) }
            }
            start()
        }

        try {
            val properties = YandexGeocoderProperties().apply {
                enabled = true
                apiKey = "test-api-key"
                baseUrl = "http://127.0.0.1:${server.address.port}/v1"
            }
            val client = YandexGeocoderApiClient(properties, RestClient.builder())

            block(client, requestMethod, requestQuery)
        } finally {
            server.stop(0)
        }
    }

    private fun AtomicReference<String?>.getValue(name: String): String? {
        return get()
            ?.split("&")
            .orEmpty()
            .mapNotNull { segment ->
                val parts = segment.split("=", limit = 2)
                if (parts.size != 2) {
                    null
                } else {
                    parts[0] to URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                }
            }
            .firstOrNull { it.first == name }
            ?.second
    }
}
