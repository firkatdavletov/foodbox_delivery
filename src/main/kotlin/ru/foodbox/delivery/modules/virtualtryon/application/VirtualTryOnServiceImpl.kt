package ru.foodbox.delivery.modules.virtualtryon.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.ForbiddenException
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.common.web.CurrentActor
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantRepository
import ru.foodbox.delivery.modules.virtualtryon.application.command.CreateVirtualTryOnSessionCommand
import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSession
import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSessionStatus
import ru.foodbox.delivery.modules.virtualtryon.domain.repository.VirtualTryOnSessionRepository
import java.time.Instant
import java.util.UUID

@Service
class VirtualTryOnServiceImpl(
    private val sessionRepository: VirtualTryOnSessionRepository,
    private val productRepository: CatalogProductRepository,
    private val productVariantRepository: CatalogProductVariantRepository,
    private val providerGateway: VirtualTryOnProviderGateway,
    private val webhookTokenVerifier: VirtualTryOnWebhookTokenVerifier,
    private val updatePublisher: VirtualTryOnUpdatePublisher,
) : VirtualTryOnService {

    override fun createSession(
        actor: CurrentActor,
        command: CreateVirtualTryOnSessionCommand,
    ): VirtualTryOnSession {
        val modelImage = command.modelImage.trim()
        if (modelImage.isBlank()) {
            throw IllegalArgumentException("modelImage must not be blank")
        }

        val product = productRepository.findById(command.productId)
            ?.takeIf { it.isActive }
            ?: throw NotFoundException("Product not found")

        val variant = command.variantId?.let { variantId ->
            productVariantRepository.findById(variantId)
                ?.takeIf { it.productId == product.id && it.isActive }
                ?: throw NotFoundException("Product variant not found")
        }

        val garmentImageUrl = listOfNotNull(
            variant?.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            product.imageUrl?.trim()?.takeIf { it.isNotBlank() },
        ).firstOrNull()
            ?: throw IllegalArgumentException("Product image is required for virtual try-on")

        val startResponse = providerGateway.startTryOn(
            StartVirtualTryOnProviderRequest(
                modelImage = modelImage,
                garmentImage = garmentImageUrl,
                category = command.category,
                garmentPhotoType = command.garmentPhotoType,
                mode = command.mode,
                moderationLevel = command.moderationLevel,
                segmentationFree = command.segmentationFree,
                seed = command.seed,
                numSamples = command.numSamples,
                outputFormat = command.outputFormat,
            )
        )

        val now = Instant.now()
        return sessionRepository.save(
            VirtualTryOnSession(
                id = UUID.randomUUID(),
                ownerType = actor.ownerType(),
                ownerValue = actor.ownerValue(),
                productId = product.id,
                variantId = variant?.id,
                garmentImageUrl = garmentImageUrl,
                providerPredictionId = startResponse.predictionId,
                providerStatus = PROVIDER_STATUS_STARTING,
                status = VirtualTryOnSessionStatus.PENDING,
                outputImages = emptyList(),
                errorName = null,
                errorMessage = null,
                subscriptionToken = UUID.randomUUID().toString(),
                createdAt = now,
                updatedAt = now,
                completedAt = null,
            )
        )
    }

    override fun getSession(
        actor: CurrentActor,
        sessionId: UUID,
    ): VirtualTryOnSession {
        val session = sessionRepository.findById(sessionId)
            ?: throw NotFoundException("Virtual try-on session not found")
        ensureOwnership(session, actor)
        return session
    }

    @Transactional
    override fun syncSession(
        actor: CurrentActor,
        sessionId: UUID,
    ): VirtualTryOnSession {
        val session = getSession(actor, sessionId)
        if (session.status != VirtualTryOnSessionStatus.PENDING) {
            return session
        }

        val providerStatus = providerGateway.getPredictionStatus(session.providerPredictionId)
        return applyProviderUpdate(session, providerStatus)
    }

    @Transactional
    override fun handleWebhook(
        token: String?,
        payload: VirtualTryOnProviderStatusResponse,
    ) {
        if (!webhookTokenVerifier.isValid(token)) {
            throw ForbiddenException("Invalid virtual try-on webhook token")
        }

        val session = sessionRepository.findByProviderPredictionId(payload.predictionId) ?: return
        applyProviderUpdate(session, payload)
    }

    private fun ensureOwnership(
        session: VirtualTryOnSession,
        actor: CurrentActor,
    ) {
        val belongsToActor = session.ownerType == actor.ownerType() && session.ownerValue == actor.ownerValue()
        if (!belongsToActor) {
            throw NotFoundException("Virtual try-on session not found")
        }
    }

    private fun applyProviderUpdate(
        session: VirtualTryOnSession,
        payload: VirtualTryOnProviderStatusResponse,
    ): VirtualTryOnSession {
        val normalizedProviderStatus = payload.providerStatus.lowercase()
        val nextStatus = when (normalizedProviderStatus) {
            PROVIDER_STATUS_COMPLETED -> VirtualTryOnSessionStatus.COMPLETED
            PROVIDER_STATUS_FAILED -> VirtualTryOnSessionStatus.FAILED
            else -> VirtualTryOnSessionStatus.PENDING
        }
        val nextOutputImages = if (nextStatus == VirtualTryOnSessionStatus.COMPLETED) {
            payload.outputImages.distinct()
        } else {
            emptyList()
        }
        val nextErrorName = if (nextStatus == VirtualTryOnSessionStatus.FAILED) {
            payload.errorName?.trim()?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        val nextErrorMessage = if (nextStatus == VirtualTryOnSessionStatus.FAILED) {
            payload.errorMessage?.trim()?.takeIf { it.isNotBlank() }
        } else {
            null
        }
        val nextCompletedAt = when (nextStatus) {
            VirtualTryOnSessionStatus.PENDING -> null
            else -> session.completedAt ?: Instant.now()
        }

        val changed = session.providerStatus != normalizedProviderStatus ||
            session.status != nextStatus ||
            session.outputImages != nextOutputImages ||
            session.errorName != nextErrorName ||
            session.errorMessage != nextErrorMessage ||
            session.completedAt != nextCompletedAt

        if (!changed) {
            return session
        }

        val now = Instant.now()
        val updated = session.copy(
            providerStatus = normalizedProviderStatus,
            status = nextStatus,
            outputImages = nextOutputImages,
            errorName = nextErrorName,
            errorMessage = nextErrorMessage,
            updatedAt = now,
            completedAt = nextCompletedAt,
        )

        val saved = sessionRepository.save(updated)
        updatePublisher.publish(saved)
        return saved
    }

    companion object {
        private const val PROVIDER_STATUS_STARTING = "starting"
        private const val PROVIDER_STATUS_COMPLETED = "completed"
        private const val PROVIDER_STATUS_FAILED = "failed"
    }
}
