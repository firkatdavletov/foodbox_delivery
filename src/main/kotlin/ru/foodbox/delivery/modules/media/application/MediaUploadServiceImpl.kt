package ru.foodbox.delivery.modules.media.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.common.error.NotFoundException
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantRepository
import ru.foodbox.delivery.modules.herobanners.domain.repository.HeroBannerRepository
import ru.foodbox.delivery.modules.media.application.command.CreateUploadSessionCommand
import ru.foodbox.delivery.modules.media.domain.MediaImage
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJob
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJobStatus
import ru.foodbox.delivery.modules.media.domain.MediaImageStatus
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import ru.foodbox.delivery.modules.media.domain.MediaUploadSession
import ru.foodbox.delivery.modules.media.domain.repository.ImageProcessingJobRepository
import ru.foodbox.delivery.modules.media.domain.repository.MediaImageRepository
import ru.foodbox.delivery.modules.media.domain.storage.CreateDirectUploadRequest
import ru.foodbox.delivery.modules.media.domain.storage.ObjectStoragePort
import ru.foodbox.delivery.modules.media.domain.storage.StoredObjectMetadata
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class MediaUploadServiceImpl(
    private val mediaImageRepository: MediaImageRepository,
    private val storagePort: ObjectStoragePort,
    private val productRepository: CatalogProductRepository,
    private val categoryRepository: CatalogCategoryRepository,
    private val variantRepository: CatalogProductVariantRepository,
    private val heroBannerRepository: HeroBannerRepository,
    private val mediaUploadProperties: MediaUploadProperties,
    private val objectKeyFactory: MediaObjectKeyFactory,
    private val jobRepository: ImageProcessingJobRepository,
    private val imageProcessingProperties: ImageProcessingProperties,
) : MediaUploadService {

    @Transactional
    override fun createUploadSession(command: CreateUploadSessionCommand): MediaUploadSession {
        val normalizedContentType = normalizeContentType(command.contentType)
        val normalizedFilename = sanitizeOriginalFilename(command.originalFilename)
        validateContentType(normalizedContentType)
        validateFileSize(command.fileSize)
        command.targetId?.let { targetId ->
            ensureTargetExists(command.targetType, targetId)
        }

        val objectKey = objectKeyFactory.newUploadKey(
            targetType = command.targetType,
            targetId = command.targetId,
            originalFilename = normalizedFilename,
            contentType = normalizedContentType,
        )

        val mediaImage = MediaImage(
            id = UUID.randomUUID(),
            targetType = command.targetType,
            targetId = command.targetId,
            bucket = storagePort.bucket(),
            objectKey = objectKey,
            originalFilename = normalizedFilename,
            contentType = normalizedContentType,
            fileSize = command.fileSize,
            status = MediaImageStatus.PENDING,
            publicUrl = null,
            thumbKey = null,
            cardKey = null,
            processingError = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

        val upload = storagePort.createDirectUpload(
            CreateDirectUploadRequest(
                objectKey = objectKey,
                contentType = normalizedContentType,
                fileSize = command.fileSize,
                expiresIn = resolvePresignDuration(),
            )
        )

        val saved = mediaImageRepository.save(mediaImage)
        return MediaUploadSession(
            mediaImage = saved,
            uploadUrl = upload.url,
            uploadMethod = upload.method,
            requiredHeaders = upload.requiredHeaders,
            expiresAt = upload.expiresAt,
        )
    }

    @Transactional
    override fun completeUpload(uploadId: UUID): MediaImage {
        val uploadSession = mediaImageRepository.findById(uploadId)
            ?: throw NotFoundException("Upload session not found")

        if (uploadSession.status == MediaImageStatus.DELETED) {
            throw IllegalArgumentException("Upload session is deleted")
        }

        if (uploadSession.status in setOf(MediaImageStatus.READY, MediaImageStatus.PROCESSING)) {
            return uploadSession
        }

        val objectMetadata = storagePort.getObjectMetadata(uploadSession.objectKey)
            ?: throw IllegalArgumentException("Uploaded object not found in storage")

        validateUploadedObject(uploadSession, objectMetadata)

        val now = Instant.now()
        val processingImage = uploadSession.copy(
            status = MediaImageStatus.PROCESSING,
            publicUrl = storagePort.buildPublicUrl(uploadSession.objectKey),
            updatedAt = now,
        )

        val saved = mediaImageRepository.save(processingImage)

        if (jobRepository.findByImageId(saved.id) == null) {
            jobRepository.save(
                ImageProcessingJob(
                    id = UUID.randomUUID(),
                    imageId = saved.id,
                    status = ImageProcessingJobStatus.PENDING,
                    attempts = 0,
                    maxAttempts = imageProcessingProperties.maxAttempts,
                    nextRetryAt = null,
                    lastError = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        return saved
    }

    private fun validateUploadedObject(
        uploadSession: MediaImage,
        objectMetadata: StoredObjectMetadata,
    ) {
        if (objectMetadata.contentLength != uploadSession.fileSize) {
            throw IllegalArgumentException("Uploaded file size does not match upload session")
        }

        val actualContentType = objectMetadata.contentType?.let(::normalizeContentType)
        val expectedContentType = normalizeContentType(uploadSession.contentType)
        if (!actualContentType.isNullOrBlank() && actualContentType != expectedContentType) {
            throw IllegalArgumentException("Uploaded file content type does not match upload session")
        }
    }

    private fun validateContentType(contentType: String) {
        val allowed = mediaUploadProperties.allowedContentTypes
            .map(::normalizeContentType)
            .toSet()

        if (!allowed.contains(contentType)) {
            throw IllegalArgumentException("Unsupported content type: $contentType")
        }
    }

    private fun validateFileSize(fileSize: Long) {
        if (fileSize <= 0) {
            throw IllegalArgumentException("fileSize must be greater than zero")
        }

        if (fileSize > mediaUploadProperties.maxFileSizeBytes) {
            throw IllegalArgumentException("fileSize exceeds max allowed size")
        }
    }

    private fun ensureTargetExists(targetType: MediaTargetType, targetId: UUID) {
        when (targetType) {
            MediaTargetType.PRODUCT -> {
                if (productRepository.findById(targetId) == null) {
                    throw NotFoundException("Product not found")
                }
            }

            MediaTargetType.CATEGORY -> {
                if (categoryRepository.findById(targetId) == null) {
                    throw NotFoundException("Category not found")
                }
            }

            MediaTargetType.VARIANT -> {
                if (variantRepository.findById(targetId) == null) {
                    throw NotFoundException("Product variant not found")
                }
            }

            MediaTargetType.HERO_BANNER -> {
                val banner = heroBannerRepository.findById(targetId)
                if (banner == null || banner.deletedAt != null) {
                    throw NotFoundException("Hero banner not found")
                }
            }
        }
    }
    private fun sanitizeOriginalFilename(originalFilename: String): String {
        val sanitized = originalFilename
            .trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')

        if (sanitized.isBlank()) {
            throw IllegalArgumentException("originalFilename must not be blank")
        }

        return sanitized.take(255)
    }

    private fun resolvePresignDuration(): Duration {
        val minutes = mediaUploadProperties.presignDurationMinutes
            .coerceAtLeast(MIN_PRESIGN_MINUTES)
            .coerceAtMost(MAX_PRESIGN_MINUTES)

        return Duration.ofMinutes(minutes)
    }

    private fun normalizeContentType(contentType: String): String {
        return contentType.substringBefore(';').trim().lowercase()
    }

    private companion object {
        const val MIN_PRESIGN_MINUTES = 1L
        const val MAX_PRESIGN_MINUTES = 60L
    }
}
