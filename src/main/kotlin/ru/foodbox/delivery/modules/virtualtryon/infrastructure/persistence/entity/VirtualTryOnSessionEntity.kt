package ru.foodbox.delivery.modules.virtualtryon.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSessionStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "virtual_try_on_sessions",
    indexes = [
        Index(name = "idx_virtual_try_on_owner", columnList = "owner_type,owner_value"),
        Index(name = "idx_virtual_try_on_status", columnList = "status"),
        Index(name = "idx_virtual_try_on_prediction", columnList = "provider_prediction_id"),
    ],
)
class VirtualTryOnSessionEntity(
    @Id
    @Column(nullable = false)
    var id: UUID,

    @Column(name = "owner_type", nullable = false, length = 32)
    var ownerType: String,

    @Column(name = "owner_value", nullable = false, length = 128)
    var ownerValue: String,

    @Column(name = "product_id", nullable = false)
    var productId: UUID,

    @Column(name = "variant_id")
    var variantId: UUID? = null,

    @Column(name = "garment_image_url", nullable = false, columnDefinition = "text")
    var garmentImageUrl: String,

    @Column(name = "provider_prediction_id", nullable = false, unique = true, length = 128)
    var providerPredictionId: String,

    @Column(name = "provider_status", length = 32)
    var providerStatus: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: VirtualTryOnSessionStatus,

    @Column(name = "output_images_json", columnDefinition = "text")
    var outputImagesJson: String? = null,

    @Column(name = "error_name", length = 128)
    var errorName: String? = null,

    @Column(name = "error_message", columnDefinition = "text")
    var errorMessage: String? = null,

    @Column(name = "subscription_token", nullable = false, length = 64)
    var subscriptionToken: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,
)
