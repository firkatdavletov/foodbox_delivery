package ru.foodbox.delivery.modules.media.application

import org.springframework.stereotype.Component
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import java.util.UUID

@Component
class MediaObjectKeyFactory {

    fun newUploadKey(
        targetType: MediaTargetType,
        targetId: UUID?,
        originalFilename: String,
        contentType: String,
    ): String {
        val extension = resolveExtension(originalFilename, contentType)
        return buildObjectKey(
            targetType = targetType,
            ownerSegment = targetId?.toString() ?: UNASSIGNED_SEGMENT,
            fileName = "${UUID.randomUUID()}.$extension",
        )
    }

    fun assignedKey(
        targetType: MediaTargetType,
        targetId: UUID,
        currentObjectKey: String,
        originalFilename: String,
        contentType: String,
    ): String {
        val currentFileName = currentObjectKey
            .substringAfterLast('/')
            .trim()
            .takeIf { it.isNotBlank() }
        val fallbackFileName = "${UUID.randomUUID()}.${resolveExtension(originalFilename, contentType)}"

        return buildObjectKey(
            targetType = targetType,
            ownerSegment = targetId.toString(),
            fileName = currentFileName ?: fallbackFileName,
        )
    }

    private fun buildObjectKey(
        targetType: MediaTargetType,
        ownerSegment: String,
        fileName: String,
    ): String {
        return "${targetPrefix(targetType)}/$ownerSegment/$fileName"
    }

    private fun targetPrefix(targetType: MediaTargetType): String {
        return when (targetType) {
            MediaTargetType.PRODUCT -> "products"
            MediaTargetType.CATEGORY -> "categories"
            MediaTargetType.VARIANT -> "variants"
            MediaTargetType.HERO_BANNER -> "hero-banners"
        }
    }

    private fun resolveExtension(originalFilename: String, contentType: String): String {
        val fromContentType = CONTENT_TYPE_EXTENSIONS[normalizeContentType(contentType)]
        if (fromContentType != null) {
            return fromContentType
        }

        val fromFilename = originalFilename
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.matches(EXTENSION_REGEX) }

        return fromFilename ?: DEFAULT_EXTENSION
    }

    private fun normalizeContentType(contentType: String): String {
        return contentType.substringBefore(';').trim().lowercase()
    }

    fun thumbKey(originalKey: String): String {
        val baseName = originalKey.substringBeforeLast('.')
        return "${baseName}_thumb.webp"
    }

    fun cardKey(originalKey: String): String {
        val baseName = originalKey.substringBeforeLast('.')
        return "${baseName}_card.webp"
    }

    private companion object {
        const val UNASSIGNED_SEGMENT = "unassigned"
        const val DEFAULT_EXTENSION = "bin"
        val EXTENSION_REGEX = Regex("^[a-z0-9]{1,10}$")
        val CONTENT_TYPE_EXTENSIONS = mapOf(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
            "image/gif" to "gif",
        )
    }
}
