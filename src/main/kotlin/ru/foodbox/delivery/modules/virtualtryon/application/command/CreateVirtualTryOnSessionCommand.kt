package ru.foodbox.delivery.modules.virtualtryon.application.command

import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnCategory
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnGarmentPhotoType
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnMode
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnModerationLevel
import ru.foodbox.delivery.modules.virtualtryon.application.VirtualTryOnOutputFormat
import java.util.UUID

data class CreateVirtualTryOnSessionCommand(
    val productId: UUID,
    val variantId: UUID?,
    val modelImage: String,
    val category: VirtualTryOnCategory = VirtualTryOnCategory.AUTO,
    val garmentPhotoType: VirtualTryOnGarmentPhotoType = VirtualTryOnGarmentPhotoType.AUTO,
    val mode: VirtualTryOnMode = VirtualTryOnMode.BALANCED,
    val moderationLevel: VirtualTryOnModerationLevel = VirtualTryOnModerationLevel.PERMISSIVE,
    val segmentationFree: Boolean = true,
    val seed: Long? = null,
    val numSamples: Int? = null,
    val outputFormat: VirtualTryOnOutputFormat = VirtualTryOnOutputFormat.PNG,
)
