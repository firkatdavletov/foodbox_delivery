package ru.foodbox.delivery.modules.media.infrastructure.repository

import org.springframework.stereotype.Repository
import ru.foodbox.delivery.modules.media.domain.MediaImage
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import ru.foodbox.delivery.modules.media.domain.repository.MediaImageRepository
import ru.foodbox.delivery.modules.media.infrastructure.persistence.entity.MediaImageEntity
import ru.foodbox.delivery.modules.media.infrastructure.persistence.jpa.MediaImageJpaRepository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class MediaImageRepositoryImpl(
    private val jpaRepository: MediaImageJpaRepository,
) : MediaImageRepository {

    override fun findById(id: UUID): MediaImage? {
        val entity = jpaRepository.findById(id).getOrNull() ?: return null
        return toDomain(entity)
    }

    override fun findAllByIds(ids: Collection<UUID>): List<MediaImage> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByIdIn(ids).map(::toDomain)
    }

    override fun findAllByTargetTypeAndTargetIdIn(targetType: MediaTargetType, targetIds: Collection<UUID>): List<MediaImage> {
        if (targetIds.isEmpty()) {
            return emptyList()
        }

        return jpaRepository.findAllByTargetTypeAndTargetIdIn(targetType, targetIds).map(::toDomain)
    }

    override fun save(mediaImage: MediaImage): MediaImage {
        val existing = jpaRepository.findById(mediaImage.id).getOrNull()
        val entity = existing ?: MediaImageEntity(
            id = mediaImage.id,
            targetType = mediaImage.targetType,
            targetId = mediaImage.targetId,
            bucket = mediaImage.bucket,
            objectKey = mediaImage.objectKey,
            originalFilename = mediaImage.originalFilename,
            contentType = mediaImage.contentType,
            fileSize = mediaImage.fileSize,
            status = mediaImage.status,
            publicUrl = mediaImage.publicUrl,
            thumbKey = mediaImage.thumbKey,
            cardKey = mediaImage.cardKey,
            processingError = mediaImage.processingError,
            createdAt = mediaImage.createdAt,
            updatedAt = mediaImage.updatedAt,
        )

        entity.targetType = mediaImage.targetType
        entity.targetId = mediaImage.targetId
        entity.bucket = mediaImage.bucket
        entity.objectKey = mediaImage.objectKey
        entity.originalFilename = mediaImage.originalFilename
        entity.contentType = mediaImage.contentType
        entity.fileSize = mediaImage.fileSize
        entity.status = mediaImage.status
        entity.publicUrl = mediaImage.publicUrl
        entity.thumbKey = mediaImage.thumbKey
        entity.cardKey = mediaImage.cardKey
        entity.processingError = mediaImage.processingError
        entity.updatedAt = mediaImage.updatedAt

        return toDomain(jpaRepository.save(entity))
    }

    override fun saveAll(mediaImages: List<MediaImage>): List<MediaImage> {
        if (mediaImages.isEmpty()) {
            return emptyList()
        }

        val existingById = jpaRepository.findAllByIdIn(mediaImages.map { it.id }).associateBy { it.id }
        val entities = mediaImages.map { mediaImage ->
            val entity = existingById[mediaImage.id] ?: MediaImageEntity(
                id = mediaImage.id,
                targetType = mediaImage.targetType,
                targetId = mediaImage.targetId,
                bucket = mediaImage.bucket,
                objectKey = mediaImage.objectKey,
                originalFilename = mediaImage.originalFilename,
                contentType = mediaImage.contentType,
                fileSize = mediaImage.fileSize,
                status = mediaImage.status,
                publicUrl = mediaImage.publicUrl,
                thumbKey = mediaImage.thumbKey,
                cardKey = mediaImage.cardKey,
                processingError = mediaImage.processingError,
                createdAt = mediaImage.createdAt,
                updatedAt = mediaImage.updatedAt,
            )

            entity.targetType = mediaImage.targetType
            entity.targetId = mediaImage.targetId
            entity.bucket = mediaImage.bucket
            entity.objectKey = mediaImage.objectKey
            entity.originalFilename = mediaImage.originalFilename
            entity.contentType = mediaImage.contentType
            entity.fileSize = mediaImage.fileSize
            entity.status = mediaImage.status
            entity.publicUrl = mediaImage.publicUrl
            entity.thumbKey = mediaImage.thumbKey
            entity.cardKey = mediaImage.cardKey
            entity.processingError = mediaImage.processingError
            entity.updatedAt = mediaImage.updatedAt
            entity
        }

        return jpaRepository.saveAll(entities).map(::toDomain)
    }

    private fun toDomain(entity: MediaImageEntity): MediaImage {
        return MediaImage(
            id = entity.id,
            targetType = entity.targetType,
            targetId = entity.targetId,
            bucket = entity.bucket,
            objectKey = entity.objectKey,
            originalFilename = entity.originalFilename,
            contentType = entity.contentType,
            fileSize = entity.fileSize,
            status = entity.status,
            publicUrl = entity.publicUrl,
            thumbKey = entity.thumbKey,
            cardKey = entity.cardKey,
            processingError = entity.processingError,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
