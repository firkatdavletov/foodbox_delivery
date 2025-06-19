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
import ru.foodbox.delivery.services.ImageService
import java.nio.file.Files
import java.nio.file.Paths

@RestController
@RequestMapping("/images")
class ImageController(
    private val imageService: ImageService
) {

    @PostMapping("/upload")
    fun uploadImage(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        val imageUrl = imageService.uploadImage(file)
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