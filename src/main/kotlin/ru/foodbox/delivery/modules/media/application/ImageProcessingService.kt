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

            val thumbKey = objectKeyFactory.thumbKey(image.objectKey)
            val cardKey = objectKeyFactory.cardKey(image.objectKey)

            val thumbBytes = resizeAndEncodeWebp(
                source = sourceImage,
                maxWidth = properties.thumb.width,
                maxHeight = properties.thumb.height,
                quality = properties.thumb.quality,
            )
            storagePort.putObject(thumbKey, thumbBytes, WEBP_CONTENT_TYPE)

            val cardBytes = resizeAndEncodeWebp(
                source = sourceImage,
                maxWidth = properties.card.width,
                maxHeight = properties.card.height,
                quality = properties.card.quality,
            )
            storagePort.putObject(cardKey, cardBytes, WEBP_CONTENT_TYPE)

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
        maxWidth: Int,
        maxHeight: Int,
        quality: Int,
    ): ByteArray {
        val resized = boundResize(source, maxWidth, maxHeight)
        return encodeToWebp(resized, quality)
    }

    private fun boundResize(source: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        val srcWidth = source.width
        val srcHeight = source.height

        if (srcWidth <= maxWidth && srcHeight <= maxHeight) {
            return toRgb(source)
        }

        val scale = minOf(maxWidth.toDouble() / srcWidth, maxHeight.toDouble() / srcHeight)
        val targetWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
        val targetHeight = (srcHeight * scale).toInt().coerceAtLeast(1)

        val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val g = resized.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null)
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

    private fun encodeToWebp(image: BufferedImage, quality: Int): ByteArray {
        val writer = ImageIO.getImageWritersByMIMEType(WEBP_CONTENT_TYPE).let { writers ->
            if (writers.hasNext()) writers.next() else null
        }

        if (writer != null) {
            val output = ByteArrayOutputStream()
            writer.output = ImageIO.createImageOutputStream(output)
            val param = writer.defaultWriteParam
            if (param.canWriteCompressed()) {
                param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                param.compressionQuality = quality / 100f
            }
            writer.write(null, IIOImage(image, null, null), param)
            writer.dispose()
            return output.toByteArray()
        }

        val output = ByteArrayOutputStream()
        ImageIO.write(image, FALLBACK_FORMAT, output)
        return output.toByteArray()
    }

    private companion object {
        const val WEBP_CONTENT_TYPE = "image/webp"
        const val FALLBACK_FORMAT = "png"
    }
}
