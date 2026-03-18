package ru.foodbox.delivery.modules.virtualtryon.api.dto

import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSessionStatus
import java.time.Instant
import java.util.UUID

data class VirtualTryOnSessionResponse(
    val id: UUID,
    val productId: UUID,
    val variantId: UUID?,
    val garmentImageUrl: String,
    val status: VirtualTryOnSessionStatus,
    val providerStatus: String?,
    val outputImages: List<String>,
    val error: VirtualTryOnSessionErrorResponse?,
    val websocketEndpoint: String,
    val websocketDestination: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant?,
)

data class VirtualTryOnSessionErrorResponse(
    val name: String?,
    val message: String?,
)
