package ru.foodbox.delivery.modules.virtualtryon.domain

import java.time.Instant
import java.util.UUID

data class VirtualTryOnSession(
    val id: UUID,
    val ownerType: String,
    val ownerValue: String,
    val productId: UUID,
    val variantId: UUID?,
    val garmentImageUrl: String,
    val providerPredictionId: String,
    val providerStatus: String?,
    val status: VirtualTryOnSessionStatus,
    val outputImages: List<String>,
    val errorName: String?,
    val errorMessage: String?,
    val subscriptionToken: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant?,
)
