package ru.foodbox.delivery.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
class ImageService(
    @Value("\${image.upload-dir}") val uploadDir: String,
    @Value("\${base_url}") val baseUrl: String,
) {

    init {
        val dirPath = Paths.get(uploadDir)
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath)
        }
    }

    fun uploadImage(file: MultipartFile): String {
        val fileName = "${UUID.randomUUID()}.${file.originalFilename?.substringAfterLast(".")}"
        val targetPath = Paths.get(uploadDir, fileName)

        file.inputStream.use { inputStream ->
            Files.copy(inputStream, targetPath)
        }

        return "$baseUrl/images/$fileName"
    }
}