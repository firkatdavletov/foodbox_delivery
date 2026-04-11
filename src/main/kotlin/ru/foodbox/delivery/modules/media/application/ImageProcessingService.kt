package ru.foodbox.delivery.modules.media.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJob
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJobStatus
import ru.foodbox.delivery.modules.media.domain.MediaImageStatus
import ru.foodbox.delivery.modules.media.domain.repository.ImageProcessingJobRepository
import ru.foodbox.delivery.modules.media.domain.repository.MediaImageRepository
import ru.foodbox.delivery.modules.media.domain.storage.ObjectStoragePort
import java.awt.geom.AffineTransform
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@Service
class ImageProcessingService(
    private val mediaImageRepository: MediaImageRepository,
    private val jobRepository: ImageProcessingJobRepository,
    private val storagePort: ObjectStoragePort,
    private val objectKeyFactory: MediaObjectKeyFactory,
    private val properties: ImageProcessingProperties,
) {

    private val log = LoggerFactory.getLogger(ImageProcessingService::class.java)

    @Transactional
    fun processJob(job: ImageProcessingJob) {
        val image = mediaImageRepository.findById(job.imageId)
        if (image == null) {
            log.warn("Image {} not found for job {}, marking FAILED", job.imageId, job.id)
            finalizeJobFailed(job, "Image not found")
            return
        }

        try {
            val originalBytes = storagePort.getObjectBytes(image.objectKey)
            val sourceImage = ImageIO.read(ByteArrayInputStream(originalBytes))
                ?: throw IllegalStateException("Failed to decode image: ${image.objectKey}")
            val orientation = runCatching { extractExifOrientation(originalBytes) }
                .onFailure { ex ->
                    log.warn("Failed to extract EXIF orientation for image {}: {}", image.id, ex.message)
                }
                .getOrNull()

            val thumbKey = objectKeyFactory.thumbKey(image.objectKey)
            val cardKey = objectKeyFactory.cardKey(image.objectKey)

            val thumbImage = resizeAndEncodeWebp(
                source = sourceImage,
                orientation = orientation,
                maxWidth = properties.thumb.width,
                maxHeight = properties.thumb.height,
                quality = properties.thumb.quality,
            )
            storagePort.putObject(thumbKey, thumbImage.bytes, thumbImage.contentType)

            val cardImage = resizeAndEncodeWebp(
                source = sourceImage,
                orientation = orientation,
                maxWidth = properties.card.width,
                maxHeight = properties.card.height,
                quality = properties.card.quality,
            )
            storagePort.putObject(cardKey, cardImage.bytes, cardImage.contentType)

            val now = Instant.now()
            mediaImageRepository.save(
                image.copy(
                    thumbKey = thumbKey,
                    cardKey = cardKey,
                    status = MediaImageStatus.READY,
                    processingError = null,
                    updatedAt = now,
                ),
            )

            jobRepository.save(
                job.copy(
                    status = ImageProcessingJobStatus.COMPLETED,
                    attempts = job.attempts + 1,
                    updatedAt = now,
                ),
            )

            log.info("Image {} processed: thumb={}, card={}", image.id, thumbKey, cardKey)
        } catch (ex: Exception) {
            log.error("Failed to process image {} (attempt {})", image.id, job.attempts + 1, ex)
            handleJobFailure(job, image.id, ex)
        }
    }

    private fun handleJobFailure(job: ImageProcessingJob, imageId: java.util.UUID, ex: Exception) {
        val now = Instant.now()
        val nextAttempt = job.attempts + 1
        val errorMessage = "${ex.javaClass.simpleName}: ${ex.message}".take(4000)

        if (nextAttempt >= job.maxAttempts) {
            finalizeJobFailed(job, errorMessage)

            val image = mediaImageRepository.findById(imageId)
            if (image != null) {
                mediaImageRepository.save(
                    image.copy(
                        status = MediaImageStatus.FAILED,
                        processingError = errorMessage,
                        updatedAt = now,
                    ),
                )
            }
        } else {
            val retryAt = now.plusSeconds(properties.retryDelaySeconds * nextAttempt)
            jobRepository.save(
                job.copy(
                    status = ImageProcessingJobStatus.RETRY,
                    attempts = nextAttempt,
                    nextRetryAt = retryAt,
                    lastError = errorMessage,
                    updatedAt = now,
                ),
            )
        }
    }

    private fun finalizeJobFailed(job: ImageProcessingJob, errorMessage: String) {
        jobRepository.save(
            job.copy(
                status = ImageProcessingJobStatus.FAILED,
                attempts = job.attempts + 1,
                lastError = errorMessage,
                updatedAt = Instant.now(),
            ),
        )
    }

    private fun resizeAndEncodeWebp(
        source: BufferedImage,
        orientation: Int?,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int,
    ): EncodedImage {
        val resized = boundResize(source, orientation, maxWidth, maxHeight)
        return encodeToWebp(resized, quality)
    }

    private fun boundResize(
        source: BufferedImage,
        orientation: Int?,
        maxWidth: Int,
        maxHeight: Int,
    ): BufferedImage {
        val srcWidth = source.width
        val srcHeight = source.height
        val normalizedOrientation = orientation?.takeIf { it in 2..8 } ?: 1
        val orientedWidth = if (normalizedOrientation in ORIENTATIONS_SWAPPING_DIMENSIONS) srcHeight else srcWidth
        val orientedHeight = if (normalizedOrientation in ORIENTATIONS_SWAPPING_DIMENSIONS) srcWidth else srcHeight

        if (normalizedOrientation == 1 && srcWidth <= maxWidth && srcHeight <= maxHeight) {
            return toRgb(source)
        }

        val scale = minOf(
            maxWidth.toDouble() / orientedWidth,
            maxHeight.toDouble() / orientedHeight,
            1.0,
        )
        val targetWidth = (orientedWidth * scale).toInt().coerceAtLeast(1)
        val targetHeight = (orientedHeight * scale).toInt().coerceAtLeast(1)

        val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val g = resized.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val transform = buildOrientationTransform(
            orientation = normalizedOrientation,
            sourceWidth = srcWidth,
            sourceHeight = srcHeight,
            scaleX = targetWidth.toDouble() / orientedWidth,
            scaleY = targetHeight.toDouble() / orientedHeight,
        )
        g.drawImage(source, transform, null)
        g.dispose()
        return resized
    }

    private fun toRgb(source: BufferedImage): BufferedImage {
        if (source.type == BufferedImage.TYPE_INT_RGB) {
            return source
        }
        val rgb = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
        val g = rgb.createGraphics()
        g.drawImage(source, 0, 0, null)
        g.dispose()
        return rgb
    }

    private fun encodeToWebp(image: BufferedImage, quality: Int): EncodedImage {
        val writer = ImageIO.getImageWritersByMIMEType(WEBP_CONTENT_TYPE).let { writers ->
            if (writers.hasNext()) writers.next() else null
        }

        if (writer != null) {
            try {
                val output = ByteArrayOutputStream()
                val imageOutput = ImageIO.createImageOutputStream(output)
                    ?: throw IllegalStateException("Failed to create ImageOutputStream for $WEBP_CONTENT_TYPE")
                try {
                    writer.output = imageOutput
                    val param = writer.defaultWriteParam
                    configureCompression(param, quality)
                    writer.write(null, IIOImage(image, null, null), param)
                } finally {
                    imageOutput.close()
                    writer.dispose()
                }
                return EncodedImage(
                    bytes = output.toByteArray(),
                    contentType = WEBP_CONTENT_TYPE,
                )
            } catch (ex: Exception) {
                log.warn("Failed to encode image as WebP, falling back to PNG: {}", ex.message)
            } catch (ex: LinkageError) {
                log.warn("WebP writer is not available, falling back to PNG: {}", ex.message)
            }
        }

        val output = ByteArrayOutputStream()
        ImageIO.write(image, FALLBACK_FORMAT, output)
        return EncodedImage(
            bytes = output.toByteArray(),
            contentType = PNG_CONTENT_TYPE,
        )
    }

    private companion object {
        const val WEBP_CONTENT_TYPE = "image/webp"
        const val PNG_CONTENT_TYPE = "image/png"
        const val FALLBACK_FORMAT = "png"
        val ORIENTATIONS_SWAPPING_DIMENSIONS = setOf(5, 6, 7, 8)
    }

    private data class EncodedImage(
        val bytes: ByteArray,
        val contentType: String,
    )
}

internal fun configureCompression(param: ImageWriteParam, quality: Int) {
    if (!param.canWriteCompressed()) {
        return
    }

    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
    // Luciad WebP writer requires an explicit compression type before quality is set.
    param.compressionTypes?.firstOrNull()?.let { param.compressionType = it }
    param.compressionQuality = quality.coerceIn(0, 100) / 100f
}

internal fun normalizeImageOrientation(source: BufferedImage, originalBytes: ByteArray): BufferedImage {
    val orientation = extractExifOrientation(originalBytes) ?: return source
    if (orientation !in 2..8) {
        return source
    }

    val targetWidth = if (orientation in setOf(5, 6, 7, 8)) source.height else source.width
    val targetHeight = if (orientation in setOf(5, 6, 7, 8)) source.width else source.height
    val targetType = if (source.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
    val normalized = BufferedImage(targetWidth, targetHeight, targetType)

    for (y in 0 until source.height) {
        for (x in 0 until source.width) {
            val rgb = source.getRGB(x, y)
            val (targetX, targetY) = when (orientation) {
                2 -> (source.width - 1 - x) to y
                3 -> (source.width - 1 - x) to (source.height - 1 - y)
                4 -> x to (source.height - 1 - y)
                5 -> y to x
                6 -> (source.height - 1 - y) to x
                7 -> (source.height - 1 - y) to (source.width - 1 - x)
                8 -> y to (source.width - 1 - x)
                else -> x to y
            }
            normalized.setRGB(targetX, targetY, rgb)
        }
    }

    return normalized
}

internal fun buildOrientationTransform(
    orientation: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    scaleX: Double,
    scaleY: Double,
): AffineTransform {
    return when (orientation) {
        2 -> AffineTransform(-scaleX, 0.0, 0.0, scaleY, scaleX * sourceWidth, 0.0)
        3 -> AffineTransform(-scaleX, 0.0, 0.0, -scaleY, scaleX * sourceWidth, scaleY * sourceHeight)
        4 -> AffineTransform(scaleX, 0.0, 0.0, -scaleY, 0.0, scaleY * sourceHeight)
        5 -> AffineTransform(0.0, scaleY, scaleX, 0.0, 0.0, 0.0)
        6 -> AffineTransform(0.0, scaleY, -scaleX, 0.0, scaleX * sourceHeight, 0.0)
        7 -> AffineTransform(0.0, -scaleY, -scaleX, 0.0, scaleX * sourceHeight, scaleY * sourceWidth)
        8 -> AffineTransform(0.0, -scaleY, scaleX, 0.0, 0.0, scaleY * sourceWidth)
        else -> AffineTransform(scaleX, 0.0, 0.0, scaleY, 0.0, 0.0)
    }
}

internal fun extractExifOrientation(bytes: ByteArray): Int? {
    if (bytes.size < 4 || readUnsignedShort(bytes, 0, littleEndian = false) != JPEG_SOI_MARKER) {
        return null
    }

    var offset = 2
    while (offset + 4 <= bytes.size) {
        if (bytes[offset].toInt() and 0xFF != JPEG_MARKER_PREFIX) {
            offset += 1
            continue
        }

        val marker = bytes[offset + 1].toInt() and 0xFF
        offset += 2

        if (marker == JPEG_SOI_SUFFIX || marker == JPEG_TEM_MARKER) {
            continue
        }
        if (marker == JPEG_EOI_SUFFIX || marker == JPEG_SOS_SUFFIX) {
            break
        }
        if (offset + 2 > bytes.size) {
            return null
        }

        val segmentLength = readUnsignedShort(bytes, offset, littleEndian = false)
        if (segmentLength < 2 || offset + segmentLength > bytes.size) {
            return null
        }

        val segmentDataOffset = offset + 2
        val segmentDataLength = segmentLength - 2
        if (marker == JPEG_APP1_MARKER && hasExifHeader(bytes, segmentDataOffset, segmentDataLength)) {
            return readExifOrientationFromTiff(
                bytes = bytes,
                tiffOffset = segmentDataOffset + EXIF_HEADER.size,
                tiffLength = segmentDataLength - EXIF_HEADER.size,
            )
        }

        offset += segmentLength
    }

    return null
}

private fun hasExifHeader(bytes: ByteArray, offset: Int, length: Int): Boolean {
    if (length < EXIF_HEADER.size || offset + EXIF_HEADER.size > bytes.size) {
        return false
    }
    return EXIF_HEADER.indices.all { index -> bytes[offset + index] == EXIF_HEADER[index] }
}

private fun readExifOrientationFromTiff(bytes: ByteArray, tiffOffset: Int, tiffLength: Int): Int? {
    if (tiffLength < 8 || tiffOffset + tiffLength > bytes.size) {
        return null
    }

    val littleEndian = when {
        bytes[tiffOffset] == 'I'.code.toByte() && bytes[tiffOffset + 1] == 'I'.code.toByte() -> true
        bytes[tiffOffset] == 'M'.code.toByte() && bytes[tiffOffset + 1] == 'M'.code.toByte() -> false
        else -> return null
    }

    if (readUnsignedShort(bytes, tiffOffset + 2, littleEndian) != TIFF_MAGIC_NUMBER) {
        return null
    }

    val ifdOffset = readInt(bytes, tiffOffset + 4, littleEndian)
    if (ifdOffset < 8 || ifdOffset + 2 > tiffLength) {
        return null
    }

    val ifdStart = tiffOffset + ifdOffset
    val entryCount = readUnsignedShort(bytes, ifdStart, littleEndian)

    for (index in 0 until entryCount) {
        val entryOffset = ifdStart + 2 + index * TIFF_ENTRY_SIZE
        if (entryOffset + TIFF_ENTRY_SIZE > tiffOffset + tiffLength) {
            return null
        }

        val tag = readUnsignedShort(bytes, entryOffset, littleEndian)
        if (tag != EXIF_ORIENTATION_TAG) {
            continue
        }

        val type = readUnsignedShort(bytes, entryOffset + 2, littleEndian)
        val count = readInt(bytes, entryOffset + 4, littleEndian)
        if (type != TIFF_TYPE_SHORT || count < 1) {
            return null
        }

        return readUnsignedShort(bytes, entryOffset + 8, littleEndian)
    }

    return null
}

private fun readUnsignedShort(bytes: ByteArray, offset: Int, littleEndian: Boolean): Int {
    val first = bytes[offset].toInt() and 0xFF
    val second = bytes[offset + 1].toInt() and 0xFF
    return if (littleEndian) {
        first or (second shl 8)
    } else {
        (first shl 8) or second
    }
}

private fun readInt(bytes: ByteArray, offset: Int, littleEndian: Boolean): Int {
    val b0 = bytes[offset].toInt() and 0xFF
    val b1 = bytes[offset + 1].toInt() and 0xFF
    val b2 = bytes[offset + 2].toInt() and 0xFF
    val b3 = bytes[offset + 3].toInt() and 0xFF
    return if (littleEndian) {
        b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    } else {
        (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }
}

private const val JPEG_MARKER_PREFIX = 0xFF
private const val JPEG_SOI_MARKER = 0xFFD8
private const val JPEG_SOI_SUFFIX = 0xD8
private const val JPEG_EOI_SUFFIX = 0xD9
private const val JPEG_SOS_SUFFIX = 0xDA
private const val JPEG_TEM_MARKER = 0x01
private const val JPEG_APP1_MARKER = 0xE1
private const val TIFF_MAGIC_NUMBER = 42
private const val TIFF_TYPE_SHORT = 3
private const val TIFF_ENTRY_SIZE = 12
private const val EXIF_ORIENTATION_TAG = 0x0112
private val EXIF_HEADER = byteArrayOf(
    'E'.code.toByte(),
    'x'.code.toByte(),
    'i'.code.toByte(),
    'f'.code.toByte(),
    0,
    0,
)
