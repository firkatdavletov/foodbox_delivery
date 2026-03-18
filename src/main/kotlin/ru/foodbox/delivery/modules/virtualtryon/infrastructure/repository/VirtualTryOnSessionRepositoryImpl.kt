package ru.foodbox.delivery.modules.virtualtryon.infrastructure.repository

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.virtualtryon.domain.VirtualTryOnSession
import ru.foodbox.delivery.modules.virtualtryon.domain.repository.VirtualTryOnSessionRepository
import ru.foodbox.delivery.modules.virtualtryon.infrastructure.persistence.entity.VirtualTryOnSessionEntity
import ru.foodbox.delivery.modules.virtualtryon.infrastructure.persistence.jpa.VirtualTryOnSessionJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class VirtualTryOnSessionRepositoryImpl(
    private val jpaRepository: VirtualTryOnSessionJpaRepository,
    private val objectMapper: ObjectMapper,
) : VirtualTryOnSessionRepository {

    override fun findById(id: UUID): VirtualTryOnSession? {
        val entity = jpaRepository.findById(id).getOrNull() ?: return null
        return toDomain(entity)
    }

    override fun findByProviderPredictionId(providerPredictionId: String): VirtualTryOnSession? {
        return jpaRepository.findByProviderPredictionId(providerPredictionId)?.let(::toDomain)
    }

    override fun save(session: VirtualTryOnSession): VirtualTryOnSession {
        val existing = jpaRepository.findById(session.id).getOrNull()
        val entity = existing ?: VirtualTryOnSessionEntity(
            id = session.id,
            ownerType = session.ownerType,
            ownerValue = session.ownerValue,
            productId = session.productId,
            variantId = session.variantId,
            garmentImageUrl = session.garmentImageUrl,
            providerPredictionId = session.providerPredictionId,
            providerStatus = session.providerStatus,
            status = session.status,
            outputImagesJson = serializeOutputImages(session.outputImages),
            errorName = session.errorName,
            errorMessage = session.errorMessage,
            subscriptionToken = session.subscriptionToken,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
            completedAt = session.completedAt,
        )

        entity.ownerType = session.ownerType
        entity.ownerValue = session.ownerValue
        entity.productId = session.productId
        entity.variantId = session.variantId
        entity.garmentImageUrl = session.garmentImageUrl
        entity.providerPredictionId = session.providerPredictionId
        entity.providerStatus = session.providerStatus
        entity.status = session.status
        entity.outputImagesJson = serializeOutputImages(session.outputImages)
        entity.errorName = session.errorName
        entity.errorMessage = session.errorMessage
        entity.subscriptionToken = session.subscriptionToken
        entity.updatedAt = session.updatedAt
        entity.completedAt = session.completedAt

        return toDomain(jpaRepository.save(entity))
    }

    private fun toDomain(entity: VirtualTryOnSessionEntity): VirtualTryOnSession {
        return VirtualTryOnSession(
            id = entity.id,
            ownerType = entity.ownerType,
            ownerValue = entity.ownerValue,
            productId = entity.productId,
            variantId = entity.variantId,
            garmentImageUrl = entity.garmentImageUrl,
            providerPredictionId = entity.providerPredictionId,
            providerStatus = entity.providerStatus,
            status = entity.status,
            outputImages = deserializeOutputImages(entity.outputImagesJson),
            errorName = entity.errorName,
            errorMessage = entity.errorMessage,
            subscriptionToken = entity.subscriptionToken,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            completedAt = entity.completedAt,
        )
    }

    private fun serializeOutputImages(outputImages: List<String>): String? {
        if (outputImages.isEmpty()) {
            return null
        }
        return objectMapper.writeValueAsString(outputImages)
    }

    private fun deserializeOutputImages(outputImagesJson: String?): List<String> {
        if (outputImagesJson.isNullOrBlank()) {
            return emptyList()
        }
        return objectMapper.readValue(outputImagesJson)
    }
}
