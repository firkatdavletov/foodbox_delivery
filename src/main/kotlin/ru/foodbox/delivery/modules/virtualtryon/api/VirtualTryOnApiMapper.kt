package ru.foodbox.delivery.modules.virtualtryon.api

import ru.foodbox.delivery.modules.virtualtryon.api.dto.CreateVirtualTryOnSessionRequest
import ru.foodbox.delivery.modules.virtualtryon.api.dto.FashnWebhookRequest
import ru.foodbox.delivery.modules.virtualtryon.api.dto.VirtualTryOnSessionErrorResponse
import ru.foodbox.delivery.modules.virtualtryon.api.dto.VirtualTryOnSessionResponse
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnProviderStatusResponse
import ru.foodbox.delivery.modules.virtualtryon.application.command.CreateVirtualTryOnSessionCommand
import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSession
import ru.foodbox.delivery.modules.virtualtryon.infrastructure.websocket.VirtualTryOnSocketDestinationFactory

internal fun CreateVirtualTryOnSessionRequest.toCommand(): CreateVirtualTryOnSessionCommand {
    return CreateVirtualTryOnSessionCommand(
        productId = productId,
        variantId = variantId,
        modelImage = modelImage,
        category = category,
        garmentPhotoType = garmentPhotoType,
        mode = mode,
        moderationLevel = moderationLevel,
        segmentationFree = segmentationFree,
        seed = seed,
        numSamples = numSamples,
        outputFormat = outputFormat,
    )
}

internal fun VirtualTryOnSession.toResponse(
    socketDestinationFactory: VirtualTryOnSocketDestinationFactory,
): VirtualTryOnSessionResponse {
    return VirtualTryOnSessionResponse(
        id = id,
        productId = productId,
        variantId = variantId,
        garmentImageUrl = garmentImageUrl,
        status = status,
        providerStatus = providerStatus,
        outputImages = outputImages,
        error = if (errorName.isNullOrBlank() && errorMessage.isNullOrBlank()) {
            null
        } else {
            VirtualTryOnSessionErrorResponse(
                name = errorName,
                message = errorMessage,
            )
        },
        websocketEndpoint = socketDestinationFactory.endpoint(),
        websocketDestination = socketDestinationFactory.destination(this),
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
    )
}

internal fun FashnWebhookRequest.toProviderStatusResponse(): VirtualTryOnProviderStatusResponse {
    val predictionId = id?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("id is required")
    val providerStatus = status?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("status is required")

    return VirtualTryOnProviderStatusResponse(
        predictionId = predictionId,
        providerStatus = providerStatus,
        outputImages = output.orEmpty().filter { it.isNotBlank() },
        errorName = error?.name?.trim()?.takeIf { it.isNotBlank() },
        errorMessage = error?.message?.trim()?.takeIf { it.isNotBlank() },
    )
}
