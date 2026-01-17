package ru.foodbox.delivery.controllers.images

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import ru.foodbox.delivery.data.repository.CategoryRepository
import ru.foodbox.delivery.data.repository.ProductRepository
import ru.foodbox.delivery.services.CatalogService
import ru.foodbox.delivery.services.ImageService
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/images")
class ImageController(
    private val imageService: ImageService,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
) {

    @PostMapping("/upload")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("id") id: String,
        @RequestParam("target") target: String,
    ): ResponseEntity<String> {
        val imageUrl = imageService.uploadImage(file)

        when (target) {
            "category" -> {
                val category = categoryRepository.findById(id.toLong()).getOrNull()
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
                category.imageUrl = imageUrl
                categoryRepository.save(category)
            }
            "product" -> {
                val product = productRepository.findById(id.toLong()).getOrNull()
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
                product.imageUrl = imageUrl
                productRepository.save(product)
            }
        }

        return ResponseEntity.ok("Image uploaded successfully: $imageUrl")
    }

    @GetMapping("/{fileName}")
    fun getImage(@PathVariable fileName: String): ResponseEntity<ByteArray> {
        val filePath = Paths.get(imageService.uploadDir, fileName)
        val contentType = Files.probeContentType(filePath) ?: "application/octet-stream"
        return if (Files.exists(filePath)) {
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(Files.readAllBytes(filePath))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }
}