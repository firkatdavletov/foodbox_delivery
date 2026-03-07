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
class CategoryImageUploadController(
    private val imageUploadService: ImageUploadService,
) {

    @PostMapping("/admin/categories/{categoryId}/images:init")
    fun init(
        @PathVariable categoryId: Long,
        @RequestBody req: InitUploadReq,
    ): ResponseEntity<InitUploadRes> {
        require(req.sizeBytes in 1..1L * 1024 * 1024) { "Bad size" }
        require(req.contentType in setOf("image/jpeg", "image/png", "image/webp")) { "Bad type" }

        val body = imageUploadService.saveCategoryImage(
            categoryId = categoryId,
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

    @PostMapping("/admin/categories/{categoryId}/images/{imageId}:complete")
    fun complete(
        @PathVariable categoryId: Long,
        @PathVariable imageId: Long,
        @RequestParam objectKey: String,
    ) {
        imageUploadService.completeCategoryUpload(categoryId, imageId, objectKey)
    }
}
