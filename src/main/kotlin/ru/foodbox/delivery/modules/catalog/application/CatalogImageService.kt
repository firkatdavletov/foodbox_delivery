package ru.foodbox.delivery.modules.catalog.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.catalog.domain.CatalogCategoryImage
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductImage
import ru.foodbox.delivery.modules.catalog.domain.CatalogProductVariantImage
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogCategoryImageRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductImageRepository
import ru.foodbox.delivery.modules.catalog.domain.repository.CatalogProductVariantImageRepository
import ru.foodbox.delivery.modules.media.domain.MediaImage
import ru.foodbox.delivery.modules.media.domain.MediaImageStatus
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import ru.foodbox.delivery.modules.media.domain.repository.MediaImageRepository
import ru.foodbox.delivery.modules.media.domain.storage.ObjectStoragePort
import ru.foodbox.delivery.modules.media.application.MediaObjectKeyFactory
import java.time.Instant
import java.util.UUID

@Service
class CatalogImageService(
    private val categoryImageRepository: CatalogCategoryImageRepository,
    private val productImageRepository: CatalogProductImageRepository,
    private val productVariantImageRepository: CatalogProductVariantImageRepository,
    private val mediaImageRepository: MediaImageRepository,
    private val storagePort: ObjectStoragePort,
    private val objectKeyFactory: MediaObjectKeyFactory,
) {

    fun getCategoryImageUrls(categoryIds: Collection<UUID>): Map<UUID, List<String>> {
        return mapCategoryImages(categoryImageRepository.findAllByCategoryIds(categoryIds))
    }

    fun getProductImageUrls(productIds: Collection<UUID>): Map<UUID, List<String>> {
        return mapProductImages(productImageRepository.findAllByProductIds(productIds))
    }

    fun getVariantImageUrls(variantIds: Collection<UUID>): Map<UUID, List<String>> {
        return mapVariantImages(productVariantImageRepository.findAllByVariantIds(variantIds))
    }

    @Transactional
    fun syncCategoryImages(categoryId: UUID, imageIds: List<UUID>, now: Instant) {
        val normalizedImageIds = normalizeImageIds(imageIds, "category.imageIds")
        val existingLinks = categoryImageRepository.findAllByCategoryIds(listOf(categoryId))
        validateRequestedImages(
            imageIds = normalizedImageIds,
            targetType = MediaTargetType.CATEGORY,
            allowedUsages = setOf(MediaOwnerUsage(MediaTargetType.CATEGORY, categoryId)),
        )

        categoryImageRepository.deleteAllByCategoryIds(listOf(categoryId))
        markImagesDeletedIfOrphaned(existingLinks.map { it.imageId }.toSet() - normalizedImageIds.toSet(), now)
        categoryImageRepository.saveAll(
            normalizedImageIds.mapIndexed { index, imageId ->
                CatalogCategoryImage(
                    id = UUID.randomUUID(),
                    categoryId = categoryId,
                    imageId = imageId,
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        )
        touchAttachedImages(normalizedImageIds, MediaTargetType.CATEGORY, categoryId, now)
    }

    @Transactional
    fun syncProductImages(productId: UUID, imageIds: List<UUID>, now: Instant) {
        val normalizedImageIds = normalizeImageIds(imageIds, "product.imageIds")
        val existingLinks = productImageRepository.findAllByProductIds(listOf(productId))
        validateRequestedImages(
            imageIds = normalizedImageIds,
            targetType = MediaTargetType.PRODUCT,
            allowedUsages = setOf(MediaOwnerUsage(MediaTargetType.PRODUCT, productId)),
        )

        productImageRepository.deleteAllByProductIds(listOf(productId))
        markImagesDeletedIfOrphaned(existingLinks.map { it.imageId }.toSet() - normalizedImageIds.toSet(), now)
        productImageRepository.saveAll(
            normalizedImageIds.mapIndexed { index, imageId ->
                CatalogProductImage(
                    id = UUID.randomUUID(),
                    productId = productId,
                    imageId = imageId,
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        )
        touchAttachedImages(normalizedImageIds, MediaTargetType.PRODUCT, productId, now)
    }

    fun validateVariantImages(
        existingVariantIds: Collection<UUID>,
        requestedImageIds: List<UUID>,
    ) {
        val normalizedImageIds = normalizeImageIds(requestedImageIds, "variants.imageIds")
        validateRequestedImages(
            imageIds = normalizedImageIds,
            targetType = MediaTargetType.VARIANT,
            allowedUsages = existingVariantIds.map { MediaOwnerUsage(MediaTargetType.VARIANT, it) }.toSet(),
        )
    }

    @Transactional
    fun detachVariantImages(
        existingVariantIds: Collection<UUID>,
        retainedImageIds: Collection<UUID>,
        now: Instant,
    ) {
        val existingLinks = productVariantImageRepository.findAllByVariantIds(existingVariantIds)
        productVariantImageRepository.deleteAllByVariantIds(existingVariantIds)
        markImagesDeletedIfOrphaned(existingLinks.map { it.imageId }.toSet() - retainedImageIds.toSet(), now)
    }

    @Transactional
    fun attachVariantImages(
        imageIdsByVariantId: Map<UUID, List<UUID>>,
        now: Instant,
    ) {
        val normalizedImageIdsByVariantId = imageIdsByVariantId.mapValues { (variantId, imageIds) ->
            normalizeImageIds(imageIds, "variants[$variantId].imageIds")
        }

        productVariantImageRepository.saveAll(
            normalizedImageIdsByVariantId.flatMap { (variantId, imageIds) ->
                imageIds.mapIndexed { index, imageId ->
                    CatalogProductVariantImage(
                        id = UUID.randomUUID(),
                        variantId = variantId,
                        imageId = imageId,
                        sortOrder = index,
                        createdAt = now,
                        updatedAt = now,
                    )
                }
            }
        )

        normalizedImageIdsByVariantId.forEach { (variantId, imageIds) ->
            touchAttachedImages(imageIds, MediaTargetType.VARIANT, variantId, now)
        }
    }

    private fun mapCategoryImages(links: List<CatalogCategoryImage>): Map<UUID, List<String>> {
        if (links.isEmpty()) {
            return emptyMap()
        }

        val imagesById = loadImageMap(links.map { it.imageId })
        return links.groupBy { it.categoryId }
            .mapValues { (_, categoryLinks) ->
                categoryLinks.map { link ->
                    resolveImageUrl(imagesById.getValue(link.imageId))
                }
            }
    }

    private fun mapProductImages(links: List<CatalogProductImage>): Map<UUID, List<String>> {
        if (links.isEmpty()) {
            return emptyMap()
        }

        val imagesById = loadImageMap(links.map { it.imageId })
        return links.groupBy { it.productId }
            .mapValues { (_, productLinks) ->
                productLinks.map { link ->
                    resolveImageUrl(imagesById.getValue(link.imageId))
                }
            }
    }

    private fun mapVariantImages(links: List<CatalogProductVariantImage>): Map<UUID, List<String>> {
        if (links.isEmpty()) {
            return emptyMap()
        }

        val imagesById = loadImageMap(links.map { it.imageId })
        return links.groupBy { it.variantId }
            .mapValues { (_, variantLinks) ->
                variantLinks.map { link ->
                    resolveImageUrl(imagesById.getValue(link.imageId))
                }
            }
    }

    private fun validateRequestedImages(
        imageIds: List<UUID>,
        targetType: MediaTargetType,
        allowedUsages: Set<MediaOwnerUsage>,
    ) {
        if (imageIds.isEmpty()) {
            return
        }

        val imagesById = loadImageMap(imageIds)
        imageIds.forEach { imageId ->
            val image = imagesById[imageId]
                ?: throw IllegalArgumentException("Image '$imageId' not found")
            if (image.status != MediaImageStatus.READY) {
                throw IllegalArgumentException("Image '$imageId' is not ready")
            }
            if (image.targetType != targetType) {
                throw IllegalArgumentException("Image '$imageId' has incompatible target type '${image.targetType}'")
            }
        }

        val usageByImageId = loadUsageByImageIds(imageIds)
        usageByImageId.forEach { (imageId, usages) ->
            val conflict = usages.firstOrNull {
                MediaOwnerUsage(it.targetType, it.ownerId) !in allowedUsages
            }
            if (conflict != null) {
                throw IllegalArgumentException(
                    "Image '$imageId' is already attached to ${conflict.targetType.name.lowercase()} '${conflict.ownerId}'",
                )
            }
        }
    }

    private fun markImagesDeletedIfOrphaned(imageIds: Set<UUID>, now: Instant) {
        if (imageIds.isEmpty()) {
            return
        }

        val usedImageIds = loadUsageByImageIds(imageIds).keys
        val orphanedImageIds = imageIds - usedImageIds
        if (orphanedImageIds.isEmpty()) {
            return
        }

        val orphanedImages = mediaImageRepository.findAllByIds(orphanedImageIds)
            .filter { it.status != MediaImageStatus.DELETED }
            .map { image ->
                image.copy(
                    status = MediaImageStatus.DELETED,
                    updatedAt = now,
                )
            }
        mediaImageRepository.saveAll(orphanedImages)
    }

    private fun touchAttachedImages(
        imageIds: List<UUID>,
        targetType: MediaTargetType,
        targetId: UUID,
        now: Instant,
    ) {
        if (imageIds.isEmpty()) {
            return
        }

        val currentImages = mediaImageRepository.findAllByIds(imageIds)
        val completedRelocations = mutableListOf<CompletedRelocation>()

        try {
            val touchedImages = currentImages.map { image ->
                if (image.targetType == targetType && image.targetId == targetId && image.status == MediaImageStatus.READY) {
                    image
                } else {
                    val relocatedImage = relocateImageIfNeeded(
                        image = image,
                        targetType = targetType,
                        targetId = targetId,
                        now = now,
                    )
                    if (relocatedImage.objectKey != image.objectKey) {
                        completedRelocations += CompletedRelocation(
                            fromKey = image.objectKey,
                            toKey = relocatedImage.objectKey,
                        )
                    }
                    relocatedImage
                }
            }
            mediaImageRepository.saveAll(touchedImages)
        } catch (ex: Exception) {
            completedRelocations.asReversed().forEach { relocation ->
                runCatching { storagePort.moveObject(relocation.toKey, relocation.fromKey) }
            }
            throw ex
        }
    }

    private fun relocateImageIfNeeded(
        image: MediaImage,
        targetType: MediaTargetType,
        targetId: UUID,
        now: Instant,
    ): MediaImage {
        val nextObjectKey = if (requiresRelocation(image, targetType, targetId)) {
            objectKeyFactory.assignedKey(
                targetType = targetType,
                targetId = targetId,
                currentObjectKey = image.objectKey,
                originalFilename = image.originalFilename,
                contentType = image.contentType,
            )
        } else {
            image.objectKey
        }

        if (nextObjectKey != image.objectKey) {
            storagePort.moveObject(image.objectKey, nextObjectKey)
        }

        return image.copy(
            targetType = targetType,
            targetId = targetId,
            objectKey = nextObjectKey,
            publicUrl = storagePort.buildPublicUrl(nextObjectKey),
            status = MediaImageStatus.READY,
            updatedAt = now,
        )
    }

    private fun requiresRelocation(
        image: MediaImage,
        targetType: MediaTargetType,
        targetId: UUID,
    ): Boolean {
        return image.targetType != targetType || image.targetId != targetId
    }

    private fun loadUsageByImageIds(imageIds: Collection<UUID>): Map<UUID, List<MediaUsage>> {
        if (imageIds.isEmpty()) {
            return emptyMap()
        }

        return buildList {
            addAll(
                categoryImageRepository.findAllByImageIds(imageIds).map { image ->
                    MediaUsage(
                        targetType = MediaTargetType.CATEGORY,
                        ownerId = image.categoryId,
                        imageId = image.imageId,
                    )
                }
            )
            addAll(
                productImageRepository.findAllByImageIds(imageIds).map { image ->
                    MediaUsage(
                        targetType = MediaTargetType.PRODUCT,
                        ownerId = image.productId,
                        imageId = image.imageId,
                    )
                }
            )
            addAll(
                productVariantImageRepository.findAllByImageIds(imageIds).map { image ->
                    MediaUsage(
                        targetType = MediaTargetType.VARIANT,
                        ownerId = image.variantId,
                        imageId = image.imageId,
                    )
                }
            )
        }.groupBy { it.imageId }
    }

    private fun loadImageMap(imageIds: Collection<UUID>): Map<UUID, MediaImage> {
        if (imageIds.isEmpty()) {
            return emptyMap()
        }

        return mediaImageRepository.findAllByIds(imageIds).associateBy { it.id }
    }

    private fun resolveImageUrl(image: MediaImage): String {
        return image.publicUrl?.trim()?.takeIf { it.isNotBlank() } ?: storagePort.buildPublicUrl(image.objectKey)
    }

    private fun normalizeImageIds(imageIds: List<UUID>, fieldName: String): List<UUID> {
        val duplicateIds = imageIds.groupBy { it }
            .filterValues { it.size > 1 }
            .keys
        if (duplicateIds.isNotEmpty()) {
            throw IllegalArgumentException("$fieldName contains duplicate image ids: ${duplicateIds.joinToString(", ")}")
        }

        return imageIds
    }
}

private data class MediaUsage(
    val targetType: MediaTargetType,
    val ownerId: UUID,
    val imageId: UUID,
)

private data class MediaOwnerUsage(
    val targetType: MediaTargetType,
    val ownerId: UUID,
)

private data class CompletedRelocation(
    val fromKey: String,
    val toKey: String,
)
