package ru.foodbox.delivery.modules.media.application

import org.junit.jupiter.api.Test
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJob
import ru.foodbox.delivery.modules.media.domain.ImageProcessingJobStatus
import ru.foodbox.delivery.modules.media.domain.MediaImage
import ru.foodbox.delivery.modules.media.domain.MediaImageStatus
import ru.foodbox.delivery.modules.media.domain.MediaTargetType
import ru.foodbox.delivery.modules.media.domain.repository.ImageProcessingJobRepository
import ru.foodbox.delivery.modules.media.domain.repository.MediaImageRepository
import ru.foodbox.delivery.modules.media.domain.storage.CreateDirectUploadRequest
import ru.foodbox.delivery.modules.media.domain.storage.DirectUpload
import ru.foodbox.delivery.modules.media.domain.storage.ObjectStoragePort
import ru.foodbox.delivery.modules.media.domain.storage.StoredObjectMetadata
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImageProcessingServiceTest {

    @Test
    fun `configureCompression sets compression type before quality`() {
        val param = TestImageWriteParam()

        configureCompression(param, 80)

        assertEquals(ImageWriteParam.MODE_EXPLICIT, param.compressionMode)
        assertEquals("Lossy", param.compressionType)
        assertEquals(0.8f, param.compressionQuality)
    }

    @Test
    fun `configureCompression clamps quality to supported range`() {
        val highQualityParam = TestImageWriteParam()
        configureCompression(highQualityParam, 150)

        val lowQualityParam = TestImageWriteParam()
        configureCompression(lowQualityParam, -10)

        assertEquals(1.0f, highQualityParam.compressionQuality)
        assertEquals(0.0f, lowQualityParam.compressionQuality)
    }

    @Test
    fun `extractExifOrientation reads jpeg exif orientation`() {
        assertEquals(6, extractExifOrientation(jpegBytesWithExifOrientation(6)))
    }

    @Test
    fun `normalizeImageOrientation rotates image for orientation 6`() {
        val source = BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB)
        source.setRGB(0, 0, Color.RED.rgb)
        source.setRGB(1, 0, Color.GREEN.rgb)
        source.setRGB(2, 0, Color.BLUE.rgb)
        source.setRGB(0, 1, Color.CYAN.rgb)
        source.setRGB(1, 1, Color.MAGENTA.rgb)
        source.setRGB(2, 1, Color.YELLOW.rgb)

        val normalized = normalizeImageOrientation(source, jpegBytesWithExifOrientation(6))

        assertEquals(2, normalized.width)
        assertEquals(3, normalized.height)
        assertEquals(Color.CYAN.rgb, normalized.getRGB(0, 0))
        assertEquals(Color.RED.rgb, normalized.getRGB(1, 0))
        assertEquals(Color.YELLOW.rgb, normalized.getRGB(0, 2))
        assertEquals(Color.BLUE.rgb, normalized.getRGB(1, 2))
    }

    @Test
    fun `normalizeImageOrientation keeps source when no exif orientation exists`() {
        val source = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)

        val normalized = normalizeImageOrientation(source, byteArrayOf(1, 2, 3, 4))

        assertSame(source, normalized)
    }

    @Test
    fun `processJob uploads thumb and card for jpeg with exif orientation`() {
        val imageId = UUID.randomUUID()
        val originalKey = "products/unassigned/source.jpg"
        val originalImage = BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB).apply {
            setRGB(0, 0, Color.RED.rgb)
            setRGB(1, 0, Color.GREEN.rgb)
            setRGB(2, 0, Color.BLUE.rgb)
            setRGB(0, 1, Color.CYAN.rgb)
            setRGB(1, 1, Color.MAGENTA.rgb)
            setRGB(2, 1, Color.YELLOW.rgb)
        }
        val originalBytes = realJpegBytesWithExifOrientation(originalImage, 6)
        val mediaImage = MediaImage(
            id = imageId,
            targetType = MediaTargetType.PRODUCT,
            targetId = null,
            bucket = "test-bucket",
            objectKey = originalKey,
            originalFilename = "source.jpg",
            contentType = "image/jpeg",
            fileSize = originalBytes.size.toLong(),
            status = MediaImageStatus.PROCESSING,
            publicUrl = "https://cdn.example.com/$originalKey",
            thumbKey = null,
            cardKey = null,
            processingError = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val job = ImageProcessingJob(
            id = UUID.randomUUID(),
            imageId = imageId,
            status = ImageProcessingJobStatus.PROCESSING,
            attempts = 0,
            maxAttempts = 3,
            nextRetryAt = null,
            lastError = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val mediaImageRepository = InMemoryMediaImageRepository(mediaImage)
        val jobRepository = InMemoryJobRepository(job)
        val storagePort = InMemoryObjectStoragePort(
            mutableMapOf(
                originalKey to originalBytes,
            )
        )
        val service = ImageProcessingService(
            mediaImageRepository = mediaImageRepository,
            jobRepository = jobRepository,
            storagePort = storagePort,
            objectKeyFactory = MediaObjectKeyFactory(),
            properties = ImageProcessingProperties().apply {
                thumb = ImageSizeProperties(400, 400, 80)
                card = ImageSizeProperties(800, 800, 85)
            },
        )

        service.processJob(job)

        val processedImage = assertNotNull(mediaImageRepository.findById(imageId))
        assertEquals(MediaImageStatus.READY, processedImage.status)
        assertNotNull(processedImage.thumbKey)
        assertNotNull(processedImage.cardKey)
        assertTrue(storagePort.objects.containsKey(processedImage.thumbKey))
        assertTrue(storagePort.objects.containsKey(processedImage.cardKey))

        val thumbImage = ImageIO.read(ByteArrayInputStream(storagePort.objects.getValue(processedImage.thumbKey!!)))
        val cardImage = ImageIO.read(ByteArrayInputStream(storagePort.objects.getValue(processedImage.cardKey!!)))
        assertNotNull(thumbImage)
        assertNotNull(cardImage)
        assertEquals(2, thumbImage.width)
        assertEquals(3, thumbImage.height)
        assertEquals(2, cardImage.width)
        assertEquals(3, cardImage.height)

        val completedJob = assertNotNull(jobRepository.findById(job.id))
        assertEquals(ImageProcessingJobStatus.COMPLETED, completedJob.status)
        assertEquals(1, completedJob.attempts)
    }

    private class TestImageWriteParam : ImageWriteParam() {
        init {
            canWriteCompressed = true
            compressionTypes = arrayOf("Lossy", "Lossless")
        }
    }

    private fun jpegBytesWithExifOrientation(orientation: Int): ByteArray {
        val tiff = ByteArrayOutputStream().apply {
            write(byteArrayOf('I'.code.toByte(), 'I'.code.toByte()))
            writeLittleEndianShort(42)
            writeLittleEndianInt(8)
            writeLittleEndianShort(1)
            writeLittleEndianShort(0x0112)
            writeLittleEndianShort(3)
            writeLittleEndianInt(1)
            writeLittleEndianShort(orientation)
            writeLittleEndianShort(0)
            writeLittleEndianInt(0)
        }.toByteArray()

        val app1Payload = byteArrayOf(
            'E'.code.toByte(),
            'x'.code.toByte(),
            'i'.code.toByte(),
            'f'.code.toByte(),
            0,
            0,
        ) + tiff
        return ByteArrayOutputStream().apply {
            write(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
            write(byteArrayOf(0xFF.toByte(), 0xE1.toByte()))
            writeBigEndianShort(app1Payload.size + 2)
            write(app1Payload)
            write(byteArrayOf(0xFF.toByte(), 0xD9.toByte()))
        }.toByteArray()
    }

    private fun realJpegBytesWithExifOrientation(source: BufferedImage, orientation: Int): ByteArray {
        val jpegBytes = ByteArrayOutputStream().use { output ->
            ImageIO.write(source, "jpeg", output)
            output.toByteArray()
        }
        val app1Segment = buildExifApp1Segment(orientation)
        return jpegBytes.copyOfRange(0, 2) + app1Segment + jpegBytes.copyOfRange(2, jpegBytes.size)
    }

    private fun buildExifApp1Segment(orientation: Int): ByteArray {
        val tiff = ByteArrayOutputStream().apply {
            write(byteArrayOf('I'.code.toByte(), 'I'.code.toByte()))
            writeLittleEndianShort(42)
            writeLittleEndianInt(8)
            writeLittleEndianShort(1)
            writeLittleEndianShort(0x0112)
            writeLittleEndianShort(3)
            writeLittleEndianInt(1)
            writeLittleEndianShort(orientation)
            writeLittleEndianShort(0)
            writeLittleEndianInt(0)
        }.toByteArray()
        val app1Payload = byteArrayOf(
            'E'.code.toByte(),
            'x'.code.toByte(),
            'i'.code.toByte(),
            'f'.code.toByte(),
            0,
            0,
        ) + tiff
        return ByteArrayOutputStream().apply {
            write(byteArrayOf(0xFF.toByte(), 0xE1.toByte()))
            writeBigEndianShort(app1Payload.size + 2)
            write(app1Payload)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeLittleEndianShort(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeLittleEndianInt(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeBigEndianShort(value: Int) {
        write((value shr 8) and 0xFF)
        write(value and 0xFF)
    }

    private class InMemoryMediaImageRepository(
        mediaImage: MediaImage,
    ) : MediaImageRepository {
        private val images = linkedMapOf(mediaImage.id to mediaImage)

        override fun findById(id: UUID): MediaImage? = images[id]

        override fun findAllByIds(ids: Collection<UUID>): List<MediaImage> {
            return ids.mapNotNull(images::get)
        }

        override fun findAllByTargetTypeAndTargetIdIn(targetType: MediaTargetType, targetIds: Collection<UUID>): List<MediaImage> {
            return images.values.filter { it.targetType == targetType && it.targetId in targetIds }
        }

        override fun save(mediaImage: MediaImage): MediaImage {
            images[mediaImage.id] = mediaImage
            return mediaImage
        }

        override fun saveAll(mediaImages: List<MediaImage>): List<MediaImage> {
            mediaImages.forEach(::save)
            return mediaImages
        }
    }

    private class InMemoryJobRepository(
        job: ImageProcessingJob,
    ) : ImageProcessingJobRepository {
        private val jobs = linkedMapOf(job.id to job)

        override fun save(job: ImageProcessingJob): ImageProcessingJob {
            jobs[job.id] = job
            return job
        }

        override fun findById(id: UUID): ImageProcessingJob? = jobs[id]

        override fun findByImageId(imageId: UUID): ImageProcessingJob? {
            return jobs.values.firstOrNull { it.imageId == imageId }
        }

        override fun claimNextPending(now: Instant, batchSize: Int): List<ImageProcessingJob> {
            return emptyList()
        }
    }

    private class InMemoryObjectStoragePort(
        val objects: MutableMap<String, ByteArray>,
    ) : ObjectStoragePort {
        override fun bucket(): String = "test-bucket"

        override fun createDirectUpload(request: CreateDirectUploadRequest): DirectUpload {
            error("Not used in test")
        }

        override fun getObjectMetadata(objectKey: String): StoredObjectMetadata? {
            val bytes = objects[objectKey] ?: return null
            return StoredObjectMetadata(
                contentType = null,
                contentLength = bytes.size.toLong(),
            )
        }

        override fun getObjectBytes(objectKey: String): ByteArray {
            return objects.getValue(objectKey)
        }

        override fun putObject(objectKey: String, data: ByteArray, contentType: String) {
            objects[objectKey] = data
        }

        override fun moveObject(sourceKey: String, destinationKey: String) {
            val bytes = objects.remove(sourceKey) ?: error("Missing source object $sourceKey")
            objects[destinationKey] = bytes
        }

        override fun buildPublicUrl(objectKey: String): String {
            return "https://cdn.example.com/$objectKey"
        }
    }
}
