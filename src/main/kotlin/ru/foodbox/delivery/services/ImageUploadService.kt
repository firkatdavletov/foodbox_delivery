package ru.foodbox.delivery.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.controllers.admin.s3.body.InitUploadRes
import ru.foodbox.delivery.data.entities.CategoryEntity
import ru.foodbox.delivery.data.entities.ImageEntity
import ru.foodbox.delivery.data.entities.ProductEntity
import ru.foodbox.delivery.data.repository.CategoryRepository
import ru.foodbox.delivery.data.repository.ImageRepository
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.services.model.UploadImageStatus
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Service
class ImageUploadService(
    private val presigner: S3Presigner,
    private val s3: S3Client,
    @Value("\${s3.bucket}") private val bucket: String,
    @Value("\${s3.endpoint}") private val s3Endpoint: String,
    private val imageRepository: ImageRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
) {
    fun saveImage(
        productId: Long,
        variant: String,
        width: Int,
        height: Int,
        sizeBytes: Long,
        contentType: String,
        isPrimary: Boolean,
    ): InitUploadRes? {
        productRepository.findByIdOrNull(productId) ?: return null
        val objectKey = buildObjectKey("products", productId, contentType)
        return initUpload(
            objectKey = objectKey,
            variant = variant,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            contentType = contentType,
            isPrimary = isPrimary,
        )
    }

    fun saveCategoryImage(
        categoryId: Long,
        variant: String,
        width: Int,
        height: Int,
        sizeBytes: Long,
        contentType: String,
        isPrimary: Boolean,
    ): InitUploadRes? {
        categoryRepository.findByIdOrNull(categoryId) ?: return null
        val objectKey = buildObjectKey("categories", categoryId, contentType)
        return initUpload(
            objectKey = objectKey,
            variant = variant,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            contentType = contentType,
            isPrimary = isPrimary,
        )
    }

    private fun initUpload(
        objectKey: String,
        variant: String,
        width: Int,
        height: Int,
        sizeBytes: Long,
        contentType: String,
        isPrimary: Boolean,
    ): InitUploadRes {
        val imageEntity = ImageEntity(
            storageKey = objectKey,
            variant = variant,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            mime = contentType,
            isPrimary = isPrimary,
            status = UploadImageStatus.UPLOADING
        ).apply {
            created = LocalDateTime.now()
            modified = LocalDateTime.now()
        }
        val savedImage = imageRepository.save(imageEntity)

        val putObj = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
            .build()

        val presigned = presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObj)
                .build()
        )

        return InitUploadRes(
            imageId = savedImage.id!!,
            objectKey = objectKey,
            uploadUrl = presigned.url().toString(),
            requiredHeaders = mapOf("Content-Type" to contentType)
        )
    }

    private fun buildObjectKey(
        folder: String,
        entityId: Long,
        contentType: String,
    ): String {
        val ext = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> "webp"
        }
        return "$folder/$entityId/${UUID.randomUUID()}.$ext"
    }

    private fun bindImageToProduct(productEntity: ProductEntity, imageEntity: ImageEntity): ProductEntity {
        productEntity.images.clear()
        productEntity.images.add(imageEntity)
        val savedProduct = productRepository.save(productEntity)
        return savedProduct
    }

    private fun bindImageToCategory(categoryEntity: CategoryEntity, imageEntity: ImageEntity): CategoryEntity {
        categoryEntity.imageUrl = toPublicUrl(imageEntity.storageKey)
        return categoryRepository.save(categoryEntity)
    }

    fun completeUpload(
        productId: Long,
        imageId: Long,
        objectKey: String,
    ) {
        s3.headObject { it.bucket(bucket).key(objectKey) }

        val image = imageRepository.findByIdOrNull(imageId) ?: return
        val productEntity = productRepository.findByIdOrNull(productId) ?: return
        if (image.storageKey.isBlank()) {
            image.storageKey = objectKey
        } else {
            require(image.storageKey == objectKey) { "Image objectKey mismatch" }
        }

        image.status = UploadImageStatus.READY

        val savedImage = imageRepository.save(image)
        bindImageToProduct(productEntity, savedImage)
    }

    fun completeCategoryUpload(
        categoryId: Long,
        imageId: Long,
        objectKey: String,
    ) {
        s3.headObject { it.bucket(bucket).key(objectKey) }

        val image = imageRepository.findByIdOrNull(imageId) ?: return
        val categoryEntity = categoryRepository.findByIdOrNull(categoryId) ?: return
        if (image.storageKey.isBlank()) {
            image.storageKey = objectKey
        } else {
            require(image.storageKey == objectKey) { "Image objectKey mismatch" }
        }

        image.status = UploadImageStatus.READY

        val savedImage = imageRepository.save(image)
        bindImageToCategory(categoryEntity, savedImage)
    }

    @Transactional
    fun failStaleUploads(): Int {
        return imageRepository.markStaleUploadingAsFailed(
            uploading = UploadImageStatus.UPLOADING,
            failed = UploadImageStatus.FAILED,
            createdBefore = LocalDateTime.now().minusHours(1)
        )
    }

    private fun toPublicUrl(storageKey: String?): String? {
        if (storageKey.isNullOrBlank()) return null
        if (storageKey.startsWith("http://") || storageKey.startsWith("https://")) {
            return storageKey
        }
        val endpoint = s3Endpoint.trimEnd('/')
        val bucketName = bucket.trim('/')
        val key = storageKey.trimStart('/')
        return "$endpoint/$bucketName/$key"
    }
}
