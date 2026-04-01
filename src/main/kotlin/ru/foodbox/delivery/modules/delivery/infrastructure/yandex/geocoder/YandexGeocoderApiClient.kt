package ru.foodbox.delivery.modules.delivery.infrastructure.yandex.geocoder

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import ru.foodbox.delivery.modules.delivery.application.DeliveryAddressGeocoder
import ru.foodbox.delivery.modules.delivery.domain.DeliveryAddress

@Component
class YandexGeocoderApiClient(
    private val properties: YandexGeocoderProperties,
    restClientBuilder: RestClient.Builder,
) : DeliveryAddressGeocoder {

    private val logger = LoggerFactory.getLogger(YandexGeocoderApiClient::class.java)
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

    override fun reverseGeocode(latitude: Double, longitude: Double): DeliveryAddress? {
        ensureConfigured()
        validateCoordinates(latitude = latitude, longitude = longitude)

        val response = execute("reverse geocode") {
            restClient.get()
                .uri { builder ->
                    val uriBuilder = builder
                        .queryParam("apikey", properties.apiKey.trim())
                        .queryParam("geocode", "${longitude},${latitude}")
                        .queryParam("lang", properties.lang.trim())
                        .queryParam("results", properties.results.coerceAtLeast(1))
                        .queryParam("format", "json")
                        .queryParam("sco", "longlat")

                    val kind = properties.kind.trim()
                    if (kind.isNotBlank()) {
                        uriBuilder.queryParam("kind", kind)
                    }

                    uriBuilder.build()
                }
                .retrieve()
                .body(GeocoderApiResponse::class.java)
        }

        val geoObject = response?.response
            ?.geoObjectCollection
            ?.featureMember
            .orEmpty()
            .firstOrNull()
            ?.geoObject
            ?: return null

        val address = geoObject.metaDataProperty?.geocoderMetaData?.address ?: return null
        val point = parsePoint(geoObject.point?.pos)

        return DeliveryAddress(
            country = address.findComponent("country"),
            region = address.findComponent("province") ?: address.findComponent("area"),
            city = address.findComponent("locality"),
            street = address.findComponent("street"),
            house = address.findComponent("house"),
            postalCode = address.postalCode?.trim()?.takeIf { it.isNotBlank() },
            latitude = point?.latitude ?: latitude,
            longitude = point?.longitude ?: longitude,
        ).normalized()
    }

    private fun parsePoint(pos: String?): Coordinates? {
        val normalized = pos?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val parts = normalized.split(Regex("\\s+"))
        if (parts.size < 2) {
            return null
        }

        val longitude = parts[0].toDoubleOrNull() ?: return null
        val latitude = parts[1].toDoubleOrNull() ?: return null
        return Coordinates(
            latitude = latitude,
            longitude = longitude,
        )
    }

    private fun AddressData.findComponent(kind: String): String? {
        return components.orEmpty().firstNotNullOfOrNull { component ->
            val componentKind = component.kind?.trim()?.lowercase()
            if (componentKind == kind) {
                component.name?.trim()?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        }
    }

    private fun validateCoordinates(latitude: Double, longitude: Double) {
        require(latitude.isFinite()) { "latitude must be finite" }
        require(longitude.isFinite()) { "longitude must be finite" }
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180" }
    }

    private fun ensureConfigured() {
        if (!properties.isConfigured()) {
            throw IllegalStateException("Yandex Geocoder is not configured")
        }
    }

    private fun <T> execute(operation: String, call: () -> T): T {
        return try {
            call()
        } catch (ex: RestClientResponseException) {
            logger.warn(
                "Yandex Geocoder {} HTTP error status={} body={}",
                operation,
                ex.statusCode.value(),
                ex.responseBodyAsString.take(MAX_ERROR_BODY_LOG_LENGTH),
            )
            throw IllegalStateException("Yandex Geocoder HTTP error ${ex.statusCode.value()}")
        } catch (ex: Exception) {
            logger.warn(
                "Yandex Geocoder {} error type={}",
                operation,
                ex.javaClass.simpleName,
            )
            throw IllegalStateException(ex.message ?: "Yandex Geocoder unavailable")
        }
    }

    private data class GeocoderApiResponse(
        val response: GeocoderResponseBody? = null,
    )

    private data class GeocoderResponseBody(
        @JsonProperty("GeoObjectCollection")
        val geoObjectCollection: GeoObjectCollection? = null,
    )

    private data class GeoObjectCollection(
        val featureMember: List<FeatureMember>? = null,
    )

    private data class FeatureMember(
        @JsonProperty("GeoObject")
        val geoObject: GeoObjectData? = null,
    )

    private data class GeoObjectData(
        val metaDataProperty: GeoObjectMetaDataProperty? = null,
        @JsonProperty("Point")
        val point: PointData? = null,
    )

    private data class GeoObjectMetaDataProperty(
        @JsonProperty("GeocoderMetaData")
        val geocoderMetaData: GeocoderMetaData? = null,
    )

    private data class GeocoderMetaData(
        @JsonProperty("Address")
        val address: AddressData? = null,
    )

    private data class AddressData(
        @JsonProperty("postal_code")
        val postalCode: String? = null,
        @JsonProperty("Components")
        val components: List<AddressComponent>? = null,
    )

    private data class AddressComponent(
        val kind: String? = null,
        val name: String? = null,
    )

    private data class PointData(
        val pos: String? = null,
    )

    private data class Coordinates(
        val latitude: Double,
        val longitude: Double,
    )

    private companion object {
        private const val MAX_ERROR_BODY_LOG_LENGTH = 300
    }
}
