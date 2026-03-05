package ru.foodbox.delivery.controllers.admin.s3

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.controllers.admin.s3.body.InitUploadReq
import ru.foodbox.delivery.controllers.admin.s3.body.InitUploadRes
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.UUID

@RestController
class ProductImageUploadController(
    private val presigner: S3Presigner,
    private val s3: S3Client,
    @Value("\${yc.s3.bucket}") private val bucket: String,
) {

    @PostMapping("/admin/products/{productId}/images:init")
    fun init(
        @PathVariable productId: Long,
        @RequestBody req: InitUploadReq
    ): InitUploadRes {
        require(req.sizeBytes in 1..10L * 1024 * 1024) { "Bad size" }
        require(req.contentType in setOf("image/jpeg","image/png","image/webp")) { "Bad type" }

        val imageId = UUID.randomUUID()
        val ext = when (req.contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> "webp"
        }
        val objectKey = "products/$productId/$imageId.$ext"

        val putObj = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(req.contentType)
            .build()

        val presigned = presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObj)
                .build()
        )

        // В БД: создать запись imageId/productId/objectKey/status=UPLOADING

        return InitUploadRes(
            imageId = imageId,
            objectKey = objectKey,
            uploadUrl = presigned.url().toString(),
            requiredHeaders = mapOf("Content-Type" to req.contentType)
        )
    }

    @PostMapping("/admin/products/{productId}/images/{imageId}:complete")
    fun complete(
        @PathVariable productId: Long,
        @PathVariable imageId: UUID,
        @RequestParam objectKey: String
    ) {
        val head = s3.headObject { it.bucket(bucket).key(objectKey) }

        // Сверь size/contentType с тем, что ожидал (из БД)
        // head.contentLength(), head.contentType()

        // В БД: status=READY, сохранить width/height (если считаешь), sort/isPrimary и т.п.
    }
}