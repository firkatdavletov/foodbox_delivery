package ru.foodbox.delivery.controllers.admin.s3

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.foodbox.delivery.controllers.admin.s3.body.InitUploadReq
import ru.foodbox.delivery.controllers.admin.s3.body.InitUploadRes
import ru.foodbox.delivery.services.ImageUploadService

@RestController
class ProductImageUploadController(
    private val imageUploadService: ImageUploadService,
) {

    @PostMapping("/admin/products/{productId}/images:init")
    fun init(
        @PathVariable productId: Long,
        @RequestBody req: InitUploadReq
    ): ResponseEntity<InitUploadRes> {
        require(req.sizeBytes in 1..10L * 1024 * 1024) { "Bad size" }
        require(req.contentType in setOf("image/jpeg","image/png","image/webp")) { "Bad type" }

        val body = imageUploadService.saveImage(
            productId = productId,
            variant = "original",
            width = -1,
            height = -1,
            sizeBytes = req.sizeBytes,
            contentType = req.contentType,
            isPrimary = true,
        )

        if (body != null) {
            return ResponseEntity.ok(body)
        } else {
            return ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/admin/products/{productId}/images/{imageId}:complete")
    fun complete(
        @PathVariable productId: Long,
        @PathVariable imageId: Long,
        @RequestParam objectKey: String
    ) {

        // Сверь size/contentType с тем, что ожидал (из БД)
        // head.contentLength(), head.contentType()

        // В БД: status=READY, сохранить width/height (если считаешь), sort/isPrimary и т.п.
        imageUploadService.completeUpload(productId, imageId, objectKey)
    }
}
