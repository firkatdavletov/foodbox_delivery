package ru.foodbox.delivery.modules.delivery.api

import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryMethodResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryMethodsResponse
import ru.foodbox.delivery.modules.delivery.api.dto.PickupPointsResponse
import ru.foodbox.delivery.modules.delivery.api.dto.DeliveryQuoteResponse
import ru.foodbox.delivery.modules.delivery.api.dto.PickupPointResponse
import ru.foodbox.delivery.modules.delivery.api.dto.YandexLocationDetectResponse
import ru.foodbox.delivery.modules.delivery.api.dto.YandexLocationVariantResponse
import ru.foodbox.delivery.modules.delivery.api.dto.YandexPickupPointResponse
import ru.foodbox.delivery.modules.delivery.api.dto.YandexPickupPointsResponse
import ru.foodbox.delivery.modules.delivery.api.dto.toResponse
import ru.foodbox.delivery.modules.delivery.domain.DeliveryMethodType
import ru.foodbox.delivery.modules.delivery.domain.DeliveryQuote
import ru.foodbox.delivery.modules.delivery.domain.PickupPoint
import ru.foodbox.delivery.modules.delivery.domain.YandexDeliveryLocationVariant
import ru.foodbox.delivery.modules.delivery.domain.YandexPickupPointOption

internal fun toMethodsResponse(
    methods: List<DeliveryMethodType>,
    pickupPoints: List<PickupPoint>,
): DeliveryMethodsResponse {
    return DeliveryMethodsResponse(
        methods = methods.map {
            DeliveryMethodResponse(
                code = it,
                name = it.displayName,
                requiresAddress = it.requiresAddress,
                requiresPickupPoint = it.requiresPickupPoint,
            )
        },
        pickupPoints = pickupPoints.map(PickupPoint::toResponse),
    )
}

internal fun toPickupPointsResponse(
    pickupPoints: List<PickupPoint>,
): PickupPointsResponse {
    return PickupPointsResponse(
        pickupPoints = pickupPoints.map(PickupPoint::toResponse),
    )
}

fun DeliveryQuote.toResponse(): DeliveryQuoteResponse {
    return DeliveryQuoteResponse(
        deliveryMethod = deliveryMethod,
        available = available,
        priceMinor = priceMinor,
        currency = currency,
        zoneCode = zoneCode,
        zoneName = zoneName,
        estimatedDays = estimatedDays,
        message = message,
        pickupPointId = pickupPointId,
        pickupPointExternalId = pickupPointExternalId,
        pickupPointName = pickupPointName,
        pickupPointAddress = pickupPointAddress,
    )
}

internal fun toYandexLocationDetectResponse(
    variants: List<YandexDeliveryLocationVariant>,
): YandexLocationDetectResponse {
    return YandexLocationDetectResponse(
        variants = variants.map {
            YandexLocationVariantResponse(
                geoId = it.geoId,
                address = it.address,
            )
        },
    )
}

internal fun toYandexPickupPointsResponse(
    points: List<YandexPickupPointOption>,
): YandexPickupPointsResponse {
    return YandexPickupPointsResponse(
        points = points.map {
            YandexPickupPointResponse(
                id = it.id,
                name = it.name,
                address = it.address,
                fullAddress = it.fullAddress,
                instruction = it.instruction,
                latitude = it.latitude,
                longitude = it.longitude,
                paymentMethods = it.paymentMethods,
                isYandexBranded = it.isYandexBranded,
            )
        },
    )
}

private fun PickupPoint.toResponse(): PickupPointResponse {
    return PickupPointResponse(
        id = id,
        code = code,
        name = name,
        address = address.toResponse(),
        isActive = active,
    )
}
