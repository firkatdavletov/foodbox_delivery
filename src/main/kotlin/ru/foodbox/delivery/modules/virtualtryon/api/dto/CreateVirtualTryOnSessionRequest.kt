package ru.foodbox.delivery.modules.virtualtryon.api.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnCategory
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnGarmentPhotoType
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnMode
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnModerationLevel
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnOutputFormat
import java.util.UUID

data class CreateVirtualTryOnSessionRequest(
    @field:NotNull
    val productId: UUID,

    val variantId: UUID? = null,

    @field:NotBlank
    val modelImage: String,

    val category: VirtualTryOnCategory = VirtualTryOnCategory.AUTO,

    val garmentPhotoType: VirtualTryOnGarmentPhotoType = VirtualTryOnGarmentPhotoType.AUTO,

    val mode: VirtualTryOnMode = VirtualTryOnMode.BALANCED,

    val moderationLevel: VirtualTryOnModerationLevel = VirtualTryOnModerationLevel.PERMISSIVE,

    val segmentationFree: Boolean = true,

    @field:Min(0)
    @field:Max(4294967295)
    val seed: Long? = null,

    @field:Min(1)
    @field:Max(4)
    val numSamples: Int? = null,

    val outputFormat: VirtualTryOnOutputFormat = VirtualTryOnOutputFormat.PNG,
)
