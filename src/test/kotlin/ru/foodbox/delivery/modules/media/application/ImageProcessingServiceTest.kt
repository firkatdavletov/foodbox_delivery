package ru.foodbox.delivery.modules.media.application

import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageWriteParam
import kotlin.test.assertEquals
import kotlin.test.assertSame

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
}
