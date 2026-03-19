package ru.foodbox.delivery.modules.delivery.infrastructure.yandex

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import ru.foodbox.delivery.modules.delivery.application.YandexDeliveryGateway
import ru.foodbox.delivery.modules.delivery.application.YandexDeliveryPricingQuote
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class YandexDeliveryApiClient(
    private val properties: YandexDeliveryProperties,
    restClientBuilder: RestClient.Builder,
) : YandexDeliveryGateway {

    private val logger = LoggerFactory.getLogger(YandexDeliveryApiClient::class.java)
    private val restClient: RestClient = run {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeoutMs)
            setReadTimeout(properties.readTimeoutMs)
        }

        restClientBuilder
            .baseUrl(properties.baseUrl.trimEnd('/'))
            .requestFactory(requestFactory)
            .build()
    }

    override fun isConfigured(): Boolean = properties.isConfigured()

    override fun detectLocations(query: String): List<YandexDeliveryLocationVariant> {
        ensureConfigured()

        val response = execute("location detect") {
            restClient.post()
                .uri(LOCATION_DETECT_PATH)
                .headers { headers -> headers.setBearerAuth(properties.token.trim()) }
                .body(LocationDetectRequest(location = query.trim()))
                .retrieve()
                .body(LocationDetectResponse::class.java)
        }

        return response?.variants.orEmpty().mapNotNull { variant ->
            val geoId = variant.geoId ?: return@mapNotNull null
            val address = variant.address?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            YandexDeliveryLocationVariant(
                geoId = geoId,
                address = address,
            )
        }
    }

    override fun listPickupPoints(geoId: Long): List<YandexPickupPointOption> {
        ensureConfigured()
        require(geoId >= 0) { "geoId must be greater than or equal to zero" }

        return listPickupPointsInternal(
            PickupPointsListRequest(
                geoId = geoId,
                type = PICKUP_POINT_TYPE,
                paymentMethod = DEFAULT_PAYMENT_METHOD,
            )
        )
    }

    override fun getPickupPoint(pickupPointId: String): YandexPickupPointOption? {
        ensureConfigured()

        val normalizedPickupPointId = pickupPointId.trim().takeIf { it.isNotBlank() } ?: return null
        return listPickupPointsInternal(
            PickupPointsListRequest(
                pickupPointIds = listOf(normalizedPickupPointId),
                type = PICKUP_POINT_TYPE,
                paymentMethod = DEFAULT_PAYMENT_METHOD,
            )
        ).firstOrNull()
    }

    override fun calculateSelfPickupPrice(
        pickupPointId: String,
        subtotalMinor: Long,
        totalWeightGrams: Long?,
    ): YandexDeliveryPricingQuote {
        ensureConfigured()

        val response = execute("pricing calculator") {
            restClient.post()
                .uri(PRICING_CALCULATOR_PATH)
                .headers { headers -> headers.setBearerAuth(properties.token.trim()) }
                .body(
                    PricingCalculatorRequest(
                        source = PricingNode(platformStationId = properties.sourceStationId.trim()),
                        destination = PricingNode(platformStationId = pickupPointId.trim()),
                        totalWeight = totalWeightGrams ?: 0L,
                        totalAssessedPrice = subtotalMinor,
                    )
                )
                .retrieve()
                .body(PricingCalculatorResponse::class.java)
        } ?: throw IllegalStateException("Yandex Delivery API returned empty pricing response")

        val pricingTotal = response.pricingTotal?.trim().takeIf { !it.isNullOrBlank() }
            ?: throw IllegalStateException("Yandex Delivery API returned empty pricing_total")
        val parsed = parsePricingTotal(pricingTotal)

        return YandexDeliveryPricingQuote(
            priceMinor = parsed.first,
            currency = parsed.second,
            deliveryDays = response.deliveryDays,
        )
    }

    private fun listPickupPointsInternal(request: PickupPointsListRequest): List<YandexPickupPointOption> {
        val response = execute("pickup points list") {
            restClient.post()
                .uri(PICKUP_POINTS_LIST_PATH)
                .headers { headers -> headers.setBearerAuth(properties.token.trim()) }
                .body(request)
                .retrieve()
                .body(PickupPointsListResponse::class.java)
        }

        return response?.points.orEmpty().mapNotNull { point ->
            val id = point.id?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = point.name?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val fullAddress = point.address?.fullAddress?.trim()?.takeIf { it.isNotBlank() }
            val address = fullAddress
                ?: buildFallbackAddress(point.address)
                ?: return@mapNotNull null

            YandexPickupPointOption(
                id = id,
                name = name,
                address = address,
                fullAddress = fullAddress,
                instruction = point.instruction?.trim()?.takeIf { it.isNotBlank() },
                latitude = point.position?.latitude,
                longitude = point.position?.longitude,
                paymentMethods = point.paymentMethods.orEmpty().filterNotNull(),
                isYandexBranded = point.isYandexBranded ?: false,
            )
        }
    }

    private fun buildFallbackAddress(address: PickupPointAddressResponse?): String? {
        if (address == null) {
            return null
        }

        val streetLine = listOfNotNull(
            address.street?.trim()?.takeIf { it.isNotBlank() },
            address.house?.trim()?.takeIf { it.isNotBlank() },
        ).joinToString(", ").takeIf { it.isNotBlank() }

        return listOfNotNull(
            address.locality?.trim()?.takeIf { it.isNotBlank() },
            streetLine,
        ).joinToString(", ").takeIf { it.isNotBlank() }
    }

    private fun parsePricingTotal(value: String): Pair<Long, String> {
        val parts = value.split(Regex("\\s+"), limit = 2)
        require(parts.size == 2) { "Unexpected pricing_total format: $value" }

        val amountMinor = BigDecimal(parts[0])
            .multiply(MINOR_UNITS_MULTIPLIER)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        return amountMinor to parts[1]
    }

    private fun ensureConfigured() {
        if (!properties.isConfigured()) {
            throw IllegalStateException("Yandex delivery integration is not configured")
        }
    }

    private fun <T> execute(requestName: String, block: () -> T): T {
        return try {
            block()
        } catch (ex: RestClientResponseException) {
            logger.warn(
                "Yandex Delivery API {} HTTP error status={} body={}",
                requestName,
                ex.statusCode.value(),
                ex.responseBodyAsString.take(MAX_ERROR_BODY_LOG_LENGTH),
            )
            throw IllegalStateException("Yandex Delivery API HTTP error ${ex.statusCode.value()}")
        } catch (ex: Exception) {
            logger.warn(
                "Yandex Delivery API {} error type={}",
                requestName,
                ex.javaClass.simpleName,
            )
            throw IllegalStateException(ex.message ?: "Yandex Delivery API unavailable")
        }
    }

    private data class LocationDetectRequest(
        val location: String,
    )

    private data class LocationDetectResponse(
        val variants: List<LocationVariantResponse>? = null,
    )

    private data class LocationVariantResponse(
        @JsonProperty("geo_id")
        val geoId: Long? = null,
        val address: String? = null,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class PickupPointsListRequest(
        @JsonProperty("geo_id")
        val geoId: Long? = null,
        @JsonProperty("pickup_point_ids")
        val pickupPointIds: List<String>? = null,
        val type: String = PICKUP_POINT_TYPE,
        @JsonProperty("payment_method")
        val paymentMethod: String = DEFAULT_PAYMENT_METHOD,
    )

    private data class PickupPointsListResponse(
        val points: List<PickupPointResponse>? = null,
    )

    private data class PickupPointResponse(
        val id: String? = null,
        val name: String? = null,
        val address: PickupPointAddressResponse? = null,
        val instruction: String? = null,
        val position: PickupPointPositionResponse? = null,
        @JsonProperty("payment_methods")
        val paymentMethods: List<String?>? = null,
        @JsonProperty("is_yandex_branded")
        val isYandexBranded: Boolean? = null,
    )

    private data class PickupPointAddressResponse(
        @JsonProperty("full_address")
        val fullAddress: String? = null,
        val locality: String? = null,
        val street: String? = null,
        val house: String? = null,
    )

    private data class PickupPointPositionResponse(
        val latitude: Double? = null,
        val longitude: Double? = null,
    )

    private data class PricingCalculatorRequest(
        val source: PricingNode,
        val destination: PricingNode,
        val tariff: String = SELF_PICKUP_TARIFF,
        @JsonProperty("total_weight")
        val totalWeight: Long,
        @JsonProperty("total_assessed_price")
        val totalAssessedPrice: Long,
        @JsonProperty("client_price")
        val clientPrice: Long = 0L,
        @JsonProperty("payment_method")
        val paymentMethod: String = DEFAULT_PAYMENT_METHOD,
        val places: List<Map<String, Any>> = emptyList(),
    )

    private data class PricingNode(
        @JsonProperty("platform_station_id")
        val platformStationId: String,
    )

    private data class PricingCalculatorResponse(
        @JsonProperty("pricing_total")
        val pricingTotal: String? = null,
        @JsonProperty("delivery_days")
        val deliveryDays: Int? = null,
    )

    private companion object {
        private const val LOCATION_DETECT_PATH = "/api/b2b/platform/location/detect"
        private const val PICKUP_POINTS_LIST_PATH = "/api/b2b/platform/pickup-points/list"
        private const val PRICING_CALCULATOR_PATH = "/api/b2b/platform/pricing-calculator"
        private const val PICKUP_POINT_TYPE = "pickup_point"
        private const val SELF_PICKUP_TARIFF = "self_pickup"
        private const val DEFAULT_PAYMENT_METHOD = "already_paid"
        private const val MAX_ERROR_BODY_LOG_LENGTH = 300
        private val MINOR_UNITS_MULTIPLIER = BigDecimal(100)
    }
}
