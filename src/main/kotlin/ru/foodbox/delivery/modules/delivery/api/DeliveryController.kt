package ru.foodbox.delivery.modules.delivery.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.common.web.CurrentActorParam
import ru.foodbox.delivery.modules.cart.api.dto.CartDeliveryDraftResponse
import ru.foodbox.delivery.modules.cart.api.toResponse
import ru.foodbox.delivery.modules.cart.application.CartService
import ru.foodbox.delivery.modules.delivery.api.dto.CalculateDeliveryQuoteRequest
import ru.foodbox.delivery.modules.delivery.api.dto.DetectCourierCartDeliveryDraftRequest
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryMethodsResponse
import ru.foodbox.delivery.modules.delivery.api.dto.PickupPointsResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryQuoteResponse
import ru.foodbox.delivery.modules.delivery.api.dto.YandexLocationDetectRequest
import ru.foodbox.delivery.modules.delivery.api.dto.YandexLocationDetectResponse
import ru.foodbox.delivery.modules.delivery.api.dto.YandexPickupPointsRequest
import ru.foodbox.delivery.modules.delivery.api.dto.YandexPickupPointsResponse
import ru.foodbox.delivery.modules.delivery.api.dto.toDomain
import ru.foodbox.delivery.modules.delivery.application.DeliveryService
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuoteContext
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType

@RestController
@RequestMapping("/api/v1/delivery")
class DeliveryController(
    private val deliveryService: DeliveryService,
    private val cartService: CartService,
) {

    @GetMapping("/methods")
    fun getMethods(): DeliveryMethodsResponse {
        val methods = deliveryService.getAvailableMethodSettings()

        return toMethodsResponse(
            methods = methods,
            pickupPoints = if (methods.any { it.method == DeliveryMethodType.PICKUP }) {
                deliveryService.getActivePickupPoints()
            } else {
                emptyList()
            },
        )
    }

    @GetMapping("/pickup-points")
    fun getPickupPoints(): PickupPointsResponse {
        return toPickupPointsResponse(
            deliveryService.getActivePickupPoints(),
        )
    }

    @PostMapping("/yandex/location-detect")
    fun detectYandexLocations(
        @Valid @RequestBody request: YandexLocationDetectRequest,
    ): YandexLocationDetectResponse {
        return toYandexLocationDetectResponse(
            deliveryService.detectYandexLocations(request.query),
        )
    }

    @PostMapping("/yandex/pickup-points")
    fun getYandexPickupPoints(
        @Valid @RequestBody request: YandexPickupPointsRequest,
    ): YandexPickupPointsResponse {
        return toYandexPickupPointsResponse(
            deliveryService.getYandexPickupPoints(request.geoId),
        )
    }

    @PostMapping("/courier/draft-detect")
    fun detectCourierDeliveryDraft(
        @CurrentActorParam actor: CurrentActor,
        @Valid @RequestBody request: DetectCourierCartDeliveryDraftRequest,
    ): CartDeliveryDraftResponse {
        return cartService.detectCourierDeliveryDraft(
            actor = actor,
            latitude = request.latitude,
            longitude = request.longitude,
        ).toResponse()!!
    }

    @PostMapping("/quotes")
    fun calculateQuote(
        @Valid @RequestBody request: CalculateDeliveryQuoteRequest,
    ): DeliveryQuoteResponse {
        return deliveryService.calculateQuote(
            DeliveryQuoteContext(
                subtotalMinor = request.subtotalMinor,
                itemCount = request.itemCount,
                deliveryMethod = request.deliveryMethod,
                deliveryAddress = request.address?.toDomain(),
                pickupPointId = request.pickupPointId,
                pickupPointExternalId = request.pickupPointExternalId,
            )
        ).toResponse()
    }
}
