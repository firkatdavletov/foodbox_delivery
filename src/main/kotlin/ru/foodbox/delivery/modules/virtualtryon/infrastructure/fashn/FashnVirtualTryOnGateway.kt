package ru.foodbox.delivery.modules.virtualtryon.infrastructure.fashn

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import ru.foodbox.delivery.modules.virtualtryon.application.StartVirtualTryOnProviderRequest
import ru.foodbox.delivery.modules.virtualtryon.application.StartVirtualTryOnProviderResponse
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnProviderGateway
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnProviderStatusResponse

@Component
class FashnVirtualTryOnGateway(
    private val properties: FashnVirtualTryOnProperties,
    restClientBuilder: RestClient.Builder,
) : VirtualTryOnProviderGateway {

    private val logger = LoggerFactory.getLogger(FashnVirtualTryOnGateway::class.java)
    private val restClient: RestClient = run {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(properties.connectTimeoutMs)
            setReadTimeout(properties.readTimeoutMs)
        }

        restClientBuilder
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    override fun startTryOn(request: StartVirtualTryOnProviderRequest): StartVirtualTryOnProviderResponse {
        ensureConfigured()

        val inputs = linkedMapOf<String, Any>(
            "model_image" to request.modelImage,
            "garment_image" to request.garmentImage,
            "category" to request.category.value,
            "garment_photo_type" to request.garmentPhotoType.value,
            "mode" to request.mode.value,
            "moderation_level" to request.moderationLevel.value,
            "segmentation_free" to request.segmentationFree,
            "output_format" to request.outputFormat.value,
        )
        request.seed?.let { inputs["seed"] = it }
        request.numSamples?.let { inputs["num_samples"] = it }

        return try {
            val response = restClient.post()
                .uri { builder ->
                    builder.path("/v1/run")
                        .queryParam("webhook_url", buildWebhookUrl())
                        .build()
                }
                .headers { headers ->
                    headers.setBearerAuth(properties.apiKey.trim())
                }
                .body(
                    mapOf(
                        "model_name" to MODEL_NAME,
                        "inputs" to inputs,
                    )
                )
                .retrieve()
                .body(FashnRunResponse::class.java)

            val predictionId = response?.id?.trim().orEmpty()
            if (predictionId.isBlank()) {
                val responseError = response?.error?.message?.takeIf { it.isNotBlank() }
                    ?: "FASHN API returned empty prediction id"
                throw IllegalStateException(responseError)
            }

            StartVirtualTryOnProviderResponse(predictionId = predictionId)
        } catch (ex: RestClientResponseException) {
            logger.warn(
                "FASHN start try-on HTTP error status={} body={}",
                ex.statusCode.value(),
                ex.responseBodyAsString.take(MAX_ERROR_BODY_LOG_LENGTH),
            )
            throw IllegalStateException("FASHN API HTTP error ${ex.statusCode.value()}")
        } catch (ex: Exception) {
            logger.warn(
                "FASHN start try-on error type={}",
                ex.javaClass.simpleName,
            )
            throw IllegalStateException(ex.message ?: "FASHN API unavailable")
        }
    }

    override fun getPredictionStatus(predictionId: String): VirtualTryOnProviderStatusResponse {
        ensureConfigured()

        return try {
            val response = restClient.get()
                .uri("/v1/status/{id}", predictionId)
                .headers { headers ->
                    headers.setBearerAuth(properties.apiKey.trim())
                }
                .retrieve()
                .body(FashnStatusResponse::class.java)
                ?: throw IllegalStateException("FASHN API returned empty status response")

            VirtualTryOnProviderStatusResponse(
                predictionId = response.id?.trim().takeUnless { it.isNullOrBlank() } ?: predictionId,
                providerStatus = response.status?.trim()?.lowercase()
                    ?: throw IllegalStateException("FASHN API returned empty status"),
                outputImages = response.output.orEmpty().filter { it.isNotBlank() },
                errorName = response.error?.name?.trim()?.takeIf { it.isNotBlank() },
                errorMessage = response.error?.message?.trim()?.takeIf { it.isNotBlank() },
            )
        } catch (ex: RestClientResponseException) {
            logger.warn(
                "FASHN status HTTP error predictionId={} status={} body={}",
                predictionId,
                ex.statusCode.value(),
                ex.responseBodyAsString.take(MAX_ERROR_BODY_LOG_LENGTH),
            )
            throw IllegalStateException("FASHN API HTTP error ${ex.statusCode.value()}")
        } catch (ex: Exception) {
            logger.warn(
                "FASHN status error predictionId={} type={}",
                predictionId,
                ex.javaClass.simpleName,
            )
            throw IllegalStateException(ex.message ?: "FASHN API unavailable")
        }
    }

    private fun buildWebhookUrl(): String {
        return UriComponentsBuilder.fromUriString(properties.webhookBaseUrl.trim())
            .path(WEBHOOK_PATH)
            .queryParam("token", properties.webhookSecret.trim())
            .build(true)
            .toUriString()
    }

    private fun ensureConfigured() {
        if (!properties.enabled) {
            throw IllegalStateException("FASHN virtual try-on integration is disabled")
        }
        if (properties.apiKey.isBlank()) {
            throw IllegalStateException("FASHN API key is not configured")
        }
        if (properties.webhookBaseUrl.isBlank()) {
            throw IllegalStateException("FASHN webhook base URL is not configured")
        }
        if (properties.webhookSecret.isBlank()) {
            throw IllegalStateException("FASHN webhook secret is not configured")
        }
    }

    data class FashnStatusResponse(
        val id: String? = null,
        val status: String? = null,
        val output: List<String>? = null,
        val error: FashnErrorResponse? = null,
    )

    private data class FashnRunResponse(
        val id: String? = null,
        val error: FashnErrorResponse? = null,
    )

    data class FashnErrorResponse(
        val name: String? = null,
        val message: String? = null,
        @JsonProperty("type")
        val type: String? = null,
    )

    companion object {
        private const val MODEL_NAME = "tryon-v1.6"
        private const val WEBHOOK_PATH = "/api/v1/virtual-try-on/webhooks/fashn"
        private const val MAX_ERROR_BODY_LOG_LENGTH = 300
    }
}
