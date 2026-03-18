package ru.foodbox.delivery.modules.virtualtryon.application

interface VirtualTryOnProviderGateway {
    fun startTryOn(request: StartVirtualTryOnProviderRequest): StartVirtualTryOnProviderResponse
    fun getPredictionStatus(predictionId: String): VirtualTryOnProviderStatusResponse
}

data class StartVirtualTryOnProviderRequest(
    val modelImage: String,
    val garmentImage: String,
    val category: VirtualTryOnCategory,
    val garmentPhotoType: VirtualTryOnGarmentPhotoType,
    val mode: VirtualTryOnMode,
    val moderationLevel: VirtualTryOnModerationLevel,
    val segmentationFree: Boolean,
    val seed: Long?,
    val numSamples: Int?,
    val outputFormat: VirtualTryOnOutputFormat,
)

data class StartVirtualTryOnProviderResponse(
    val predictionId: String,
)

data class VirtualTryOnProviderStatusResponse(
    val predictionId: String,
    val providerStatus: String,
    val outputImages: List<String>,
    val errorName: String?,
    val errorMessage: String?,
)
