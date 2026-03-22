package ru.foodbox.delivery.modules.delivery.infrastructure.yandex

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import ru.foodbox.delivery.modules.delivery.application.YandexConfirmedDeliveryRequest
import ru.foodbox.delivery.modules.delivery.application.YandexDeliveryGateway
import ru.foodbox.delivery.modules.delivery.application.YandexDeliveryOffer
import ru.foodbox.delivery.modules.delivery.application.YandexDeliveryPricingQuote
import ru.foodbox.delivery.modules.delivery.application.YandexOfferCreateRequest
import ru.foodbox.delivery.modules.delivery.application.YandexOfferItem
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

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

    override fun listPickupPoints(geoId: Long, paymentMethod: String?): List<YandexPickupPointOption> {
        ensureConfigured()
        require(geoId >= 0) { "geoId must be greater than or equal to zero" }

        return listPickupPointsInternal(
            PickupPointsListRequest(
                geoId = geoId,
                paymentMethod = paymentMethod?.trim()?.takeIf { it.isNotBlank() },
                type = PICKUP_POINT_TYPE,
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
        val parsed = parseMoneyAmount(pricingTotal)
        val roundedPriceMinor = roundUpToNearestHundredRubles(parsed.first)

        return YandexDeliveryPricingQuote(
            priceMinor = roundedPriceMinor,
            currency = parsed.second,
            deliveryDays = response.deliveryDays,
        )
    }

    override fun createOffers(request: YandexOfferCreateRequest): List<YandexDeliveryOffer> {
        ensureConfigured()
        require(request.items.isNotEmpty()) { "items must not be empty" }

        val response = execute("offers create") {
            restClient.post()
                .uri { builder ->
                    builder
                        .path(OFFERS_CREATE_PATH)
                        .queryParam("send_unix", false)
                        .build()
                }
                .headers { headers -> headers.setBearerAuth(properties.token.trim()) }
                .body(
                    OffersCreateApiRequest(
                        info = OffersCreateRequestInfo(
                            operatorRequestId = request.operatorRequestId,
                            comment = request.comment,
                        ),
                        source = OfferSourceRequestNode(
                            platformStation = PlatformStationRequest(
                                platformId = properties.sourceStationId.trim(),
                            ),
                            intervalUtc = TimeIntervalUtcRequest(
                                from = request.pickupIntervalFrom,
                                to = request.pickupIntervalTo,
                            ),
                        ),
                        destination = OfferDestinationRequestNode(
                            type = DESTINATION_TYPE_PLATFORM_STATION,
                            platformStation = PlatformStationRequest(
                                platformId = request.destinationPickupPointId.trim(),
                            ),
                        ),
                        items = request.items.map(::toApiItem),
                        places = request.items.map(::toApiPlace),
                        billingInfo = BillingInfoRequest(
                            paymentMethod = request.paymentMethod.apiCode,
                            deliveryCost = if (request.paymentMethod == ru.foodbox.delivery.modules.delivery.application.YandexOfferPaymentMethod.ALREADY_PAID) {
                                0L
                            } else {
                                request.deliveryCostMinor
                            },
                        ),
                        recipientInfo = ContactRequest(
                            firstName = request.recipientName?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_RECIPIENT_NAME,
                            phone = request.recipientPhone,
                            email = request.recipientEmail?.trim()?.takeIf { it.isNotBlank() },
                        ),
                        lastMilePolicy = LAST_MILE_POLICY_SELF_PICKUP,
                    )
                )
                .retrieve()
                .body(OffersCreateApiResponse::class.java)
        }

        return response?.offers.orEmpty().map(::toDomainOffer)
    }

    override fun confirmOffer(offerId: String): YandexConfirmedDeliveryRequest {
        ensureConfigured()

        val normalizedOfferId = offerId.trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("offerId must not be blank")

        val response = execute("offers confirm") {
            restClient.post()
                .uri(OFFERS_CONFIRM_PATH)
                .headers { headers -> headers.setBearerAuth(properties.token.trim()) }
                .body(OffersConfirmApiRequest(offerId = normalizedOfferId))
                .retrieve()
                .body(OffersConfirmApiResponse::class.java)
        } ?: throw IllegalStateException("Yandex Delivery API returned empty offer confirmation response")

        val requestId = response.requestId?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Yandex Delivery API returned empty request_id")

        return YandexConfirmedDeliveryRequest(
            externalRequestId = requestId,
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

    private fun toApiItem(item: YandexOfferItem): OfferItemRequest {
        return OfferItemRequest(
            count = item.count,
            name = item.name,
            article = item.article,
            placeBarcode = item.id,
            billingDetails = ItemBillingDetailsRequest(
                unitPrice = item.unitPriceMinor,
                assessedUnitPrice = item.unitPriceMinor,
            ),
        )
    }

    private fun toApiPlace(item: YandexOfferItem): OfferPlaceRequest {
        return OfferPlaceRequest(
            barcode = item.id,
            physicalDims = PlacePhysicalDimensionsRequest(
                weightGross = (properties.defaultPlaceWeightGrams * item.count).coerceAtLeast(1L),
                dx = properties.defaultPlaceLengthCm,
                dy = properties.defaultPlaceHeightCm,
                dz = properties.defaultPlaceWidthCm,
            ),
        )
    }

    private fun toDomainOffer(response: OfferResponse): YandexDeliveryOffer {
        val pricing = response.offerDetails?.pricing?.let(::parseMoneyAmount)
        val pricingTotal = response.offerDetails?.pricingTotal?.let(::parseMoneyAmount)
        val commissionAmount = response.offerDetails?.pricingCommissionOnDeliveryPaymentAmount?.let(::parseMoneyAmount)
        val currency = pricingTotal?.second ?: pricing?.second ?: commissionAmount?.second

        return YandexDeliveryOffer(
            externalOfferId = response.offerId?.trim()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Yandex Delivery API returned empty offer_id"),
            expiresAt = response.expiresAt,
            pricingMinor = pricing?.first,
            pricingTotalMinor = pricingTotal?.first,
            currency = currency,
            commissionOnDeliveryPercent = response.offerDetails?.pricingCommissionOnDeliveryPayment
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            commissionOnDeliveryAmountMinor = commissionAmount?.first,
            deliveryPolicy = response.offerDetails?.deliveryInterval?.policy?.trim()?.takeIf { it.isNotBlank() },
            deliveryIntervalFrom = response.offerDetails?.deliveryInterval?.min,
            deliveryIntervalTo = response.offerDetails?.deliveryInterval?.max,
            pickupIntervalFrom = response.offerDetails?.pickupInterval?.min,
            pickupIntervalTo = response.offerDetails?.pickupInterval?.max,
        )
    }

    private fun parseMoneyAmount(value: String): Pair<Long, String> {
        val parts = value.split(Regex("\\s+"), limit = 2)
        require(parts.size == 2) { "Unexpected pricing_total format: $value" }

        val amountMinor = BigDecimal(parts[0])
            .multiply(MINOR_UNITS_MULTIPLIER)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        return amountMinor to parts[1]
    }

    private fun roundUpToNearestHundredRubles(amountMinor: Long): Long {
        if (amountMinor <= 0L) {
            return amountMinor
        }

        return ((amountMinor + HUNDRED_RUBLES_MINOR - 1) / HUNDRED_RUBLES_MINOR) * HUNDRED_RUBLES_MINOR
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
        @JsonProperty("payment_method")
        val paymentMethod: String? = null,
        val type: String = PICKUP_POINT_TYPE,
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class OffersCreateApiRequest(
        val info: OffersCreateRequestInfo,
        val source: OfferSourceRequestNode,
        val destination: OfferDestinationRequestNode,
        val items: List<OfferItemRequest>,
        val places: List<OfferPlaceRequest>,
        @JsonProperty("billing_info")
        val billingInfo: BillingInfoRequest,
        @JsonProperty("recipient_info")
        val recipientInfo: ContactRequest,
        @JsonProperty("last_mile_policy")
        val lastMilePolicy: String,
        @JsonProperty("particular_items_refuse")
        val particularItemsRefuse: Boolean = false,
        @JsonProperty("forbid_unboxing")
        val forbidUnboxing: Boolean = false,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class OffersCreateRequestInfo(
        @JsonProperty("operator_request_id")
        val operatorRequestId: String,
        val comment: String? = null,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class OfferSourceRequestNode(
        @JsonProperty("platform_station")
        val platformStation: PlatformStationRequest,
        @JsonProperty("interval_utc")
        val intervalUtc: TimeIntervalUtcRequest,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class OfferDestinationRequestNode(
        val type: String,
        @JsonProperty("platform_station")
        val platformStation: PlatformStationRequest? = null,
    )

    private data class PlatformStationRequest(
        @JsonProperty("platform_id")
        val platformId: String,
    )

    private data class TimeIntervalUtcRequest(
        val from: Instant,
        val to: Instant,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class OfferItemRequest(
        val count: Int,
        val name: String,
        val article: String? = null,
        @JsonProperty("place_barcode")
        val placeBarcode: String,
        @JsonProperty("billing_details")
        val billingDetails: ItemBillingDetailsRequest,
    )

    private data class ItemBillingDetailsRequest(
        @JsonProperty("unit_price")
        val unitPrice: Long,
        @JsonProperty("assessed_unit_price")
        val assessedUnitPrice: Long,
    )

    private data class OfferPlaceRequest(
        val barcode: String,
        @JsonProperty("physical_dims")
        val physicalDims: PlacePhysicalDimensionsRequest,
    )

    private data class PlacePhysicalDimensionsRequest(
        @JsonProperty("weight_gross")
        val weightGross: Long,
        val dx: Int,
        val dy: Int,
        val dz: Int,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class BillingInfoRequest(
        @JsonProperty("payment_method")
        val paymentMethod: String,
        @JsonProperty("delivery_cost")
        val deliveryCost: Long? = null,
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class ContactRequest(
        @JsonProperty("first_name")
        val firstName: String,
        val phone: String,
        val email: String? = null,
    )

    private data class OffersCreateApiResponse(
        val offers: List<OfferResponse>? = null,
    )

    private data class OfferResponse(
        @JsonProperty("offer_id")
        val offerId: String? = null,
        @JsonProperty("expires_at")
        val expiresAt: Instant? = null,
        @JsonProperty("offer_details")
        val offerDetails: OfferDetailsResponse? = null,
    )

    private data class OfferDetailsResponse(
        @JsonProperty("delivery_interval")
        val deliveryInterval: DeliveryIntervalResponse? = null,
        @JsonProperty("pickup_interval")
        val pickupInterval: PickupIntervalUtcResponse? = null,
        val pricing: String? = null,
        @JsonProperty("pricing_commission_on_delivery_payment")
        val pricingCommissionOnDeliveryPayment: String? = null,
        @JsonProperty("pricing_commission_on_delivery_payment_amount")
        val pricingCommissionOnDeliveryPaymentAmount: String? = null,
        @JsonProperty("pricing_total")
        val pricingTotal: String? = null,
    )

    private data class DeliveryIntervalResponse(
        val min: Instant? = null,
        val max: Instant? = null,
        val policy: String? = null,
    )

    private data class PickupIntervalUtcResponse(
        val min: Instant? = null,
        val max: Instant? = null,
    )

    private data class OffersConfirmApiRequest(
        @JsonProperty("offer_id")
        val offerId: String,
    )

    private data class OffersConfirmApiResponse(
        @JsonProperty("request_id")
        val requestId: String? = null,
    )

    private companion object {
        private const val LOCATION_DETECT_PATH = "/api/b2b/platform/location/detect"
        private const val PICKUP_POINTS_LIST_PATH = "/api/b2b/platform/pickup-points/list"
        private const val PRICING_CALCULATOR_PATH = "/api/b2b/platform/pricing-calculator"
        private const val OFFERS_CREATE_PATH = "/api/b2b/platform/offers/create"
        private const val OFFERS_CONFIRM_PATH = "/api/b2b/platform/offers/confirm"
        private const val PICKUP_POINT_TYPE = "pickup_point"
        private const val SELF_PICKUP_TARIFF = "self_pickup"
        private const val DEFAULT_PAYMENT_METHOD = "already_paid"
        private const val DESTINATION_TYPE_PLATFORM_STATION = "platform_station"
        private const val LAST_MILE_POLICY_SELF_PICKUP = "self_pickup"
        private const val DEFAULT_RECIPIENT_NAME = "Получатель"
        private const val MAX_ERROR_BODY_LOG_LENGTH = 300
        private const val HUNDRED_RUBLES_MINOR = 10_000L
        private val MINOR_UNITS_MULTIPLIER = BigDecimal(100)
    }
}
